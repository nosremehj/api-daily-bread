# Prompt: pré-deploy Docker e VPS (API Bread)

Documento para **você** ou para **colar em um assistente** ao final de uma entrega: validar build (Maven + imagem), subir **PostgreSQL** na VPS (via `docker-compose.yml`), rodar a API com perfil **`prod`** e checagens pós-deploy.

---

## Objetivo

1. Validar o projeto com Maven **antes** do build Docker (falhas aparecem mais cedo).
2. Garantir que `docker build` conclui com o `Dockerfile` da raiz (local ou CI).
3. Na VPS: Postgres persistente (Compose), API com **`SPRING_PROFILES_ACTIVE=prod`**, segredos fortes, sem usuário de teste automático.
4. Conferir **migrações Flyway** antes de apontar produção para banco novo ou existente.

---

## Frase curta para colar (humano ou IA)

> *No repositório `api-daily-bread`, na raiz: `./mvnw clean verify`; depois `docker build -t bread-api:local .` e confirmar sucesso. Não reescrever migrações Flyway já aplicadas; novas só como `V*__*.sql`. Em produção na VPS: subir Postgres com `docker compose up -d`, definir `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_*`, `BREAD_JWT_SECRET` (≥32 caracteres), garantir `bread.seed-test-user=false` (já no perfil prod). Opcional: Nginx na frente (HTTPS) e CORS com a origem do front. Depois health check e uma rota autenticada.*

---

## Arquitetura esperada na VPS

| Componente | Forma típica |
|------------|----------------|
| **PostgreSQL** | `docker-compose.yml` na raiz (serviço `postgres`, volume `bread_pgdata`). |
| **API** | JAR no host (`java -jar` + systemd) **ou** container da imagem do `Dockerfile` na mesma rede Docker do Postgres. |
| **HTTPS / domínio** | Nginx (ou Caddy) como reverse proxy para `127.0.0.1:PORT` da API. |
| **Firewall** | Liberar 22 (SSH), 80/443 (proxy); **não** expor5432 publicamente se o app só roda na mesma máquina. |

---

## O que o Dockerfile faz (resumo)

| Etapa | Conteúdo |
|--------|-----------|
| **Build** | `eclipse-temurin:17-jdk-jammy`, copia `pom.xml`, `src`, `mvnw`, roda `./mvnw clean package -DskipTests`. |
| **Runtime** | `eclipse-temurin:17-jre-jammy`, usuário não-root `spring`, JAR em `/app/app.jar`. |
| **Porta** | `EXPOSE 8080`; `ENV PORT=8080`. O app usa `server.port=${PORT:9090}` — na VPS, defina `PORT` igual ao que o proxy encaminha (ex.: `8080`). |

Artefato: `target/bread-0.0.1-SNAPSHOT.jar` (versão no `pom.xml`).

---

## Variáveis de ambiente (produção / VPS)

Obrigatório ou altamente recomendado:

| Variável | Função |
|----------|--------|
| `SPRING_PROFILES_ACTIVE` | `prod` — ativa `application-prod.properties` (Postgres, sem H2, sem seed de usuário teste). |
| `SPRING_DATASOURCE_URL` | Ex.: `jdbc:postgresql://localhost:5432/bread` (host `localhost` se a API está no host e o Postgres publicou a porta 5432; ou nome do **serviço** `postgres` se a API roda em container na mesma rede Compose). |
| `SPRING_DATASOURCE_USERNAME` | Alinhado a `POSTGRES_USER` (padrão local no Compose: `bread`). |
| `SPRING_DATASOURCE_PASSWORD` | Alinhado a `POSTGRES_PASSWORD` — **troque o default** `bread_change_me` em produção. |
| `BREAD_JWT_SECRET` | Segredo HS256 forte (mínimo ~32 caracteres); não versionar. |

Já coberto pelo perfil **prod** (não precisa repetir salvo override):

- `bread.seed-test-user=false`
- Dialect PostgreSQL, H2 console desligado

---

## Checklist (ordem recomendada)

### 1. Código e banco

- [ ] Tudo que deve ir para produção está commitado; `git status` revisado.
- [ ] Alterações de schema só com **nova** migração em `src/main/resources/db/migration/` (`V5__...`, etc.).
- [ ] **Nunca** reescrever migrações já aplicadas em bancos compartilhados.

### 2. VPS — Postgres (Compose)

Na raiz do projeto na VPS (ou copie só o `docker-compose.yml`):

```bash
docker compose up -d
```

- Ajuste `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` via ambiente ou arquivo `.env` no mesmo diretório (não commitar segredos).
- Primeira subida: volume vazio → Flyway cria o schema ao iniciar a API.

### 3. Build local (Maven)

```bash
./mvnw clean verify
```

- **Rápido (alinhado ao Dockerfile):** `./mvnw clean package -DskipTests`.

### 4. Imagem Docker da API

```bash
docker build -t bread-api:local .
```

- Falhou: ver o estágio `RUN ./mvnw` no log.

### 5. Subir a API na VPS (escolha um fluxo)

**A — JAR no host (sem container da API)**

```bash
./mvnw clean package -DskipTests   # na máquina de build; copiar o JAR para a VPS
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bread
export SPRING_DATASOURCE_USERNAME=bread
export SPRING_DATASOURCE_PASSWORD='<senha forte>'
export BREAD_JWT_SECRET='<mínimo 32 caracteres aleatórios>'
export PORT=8080
java -jar bread-0.0.1-SNAPSHOT.jar
```

Recomendado encapsular isso em **systemd** (`Environment=` / `EnvironmentFile=`).

**B — API em container, Postgres no Compose**

- Coloque API e `postgres` na **mesma rede** Docker.
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bread` (hostname = nome do serviço no `docker-compose.yml`).
- Exponha a porta da API só em `127.0.0.1` se usar Nginx no host.

**Smoke manual (API em container, Postgres no host com porta5432 publicada):**

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e PORT=8080 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/bread' \
  -e SPRING_DATASOURCE_USERNAME=bread \
  -e SPRING_DATASOURCE_PASSWORD='senha' \
  -e BREAD_JWT_SECRET='seu-secret-minimo-32-chars' \
  bread-api:local
```

*(Em Linux, troque `host.docker.internal` por IP do bridge ou `--add-host` conforme seu setup.)*

### 6. Proxy, CORS e pós-deploy

- [ ] Nginx (ou similar): `proxy_pass` para `http://127.0.0.1:8080` (ou a `PORT` usada).
- [ ] **CORS:** incluir no código a origem do front em produção (`CorsConfig`); localhost/Vercel sozinhos não cobrem o domínio da VPS.
- [ ] Pós-deploy: `/actuator/health`, fluxo de login, rota crítica autenticada (ex.: progresso de leitura).

---

## Comandos mínimos (máquina de desenvolvimento)

```bash
cd /caminho/para/api-daily-bread
./mvnw clean verify
docker build -t bread-api:local .
```

---

## Problemas frequentes

| Sintoma | Ação |
|---------|------|
| `dockerDesktopLinuxEngine` / daemon inacessível | Iniciar Docker Desktop; aguardar “running”. |
| Build Maven ok, Docker falha | Log do estágio `RUN ./mvnw`; rede ou diferença Linux vs Windows. |
| App sobe mas Flyway erro | URL/credenciais do Postgres; ordem de migrações; banco já parcialmente migrado. |
| `Connection refused` ao Postgres | Compose não está no ar; host errado (`localhost` vs `postgres`); porta5432 bloqueada. |
| JWT inválido após deploy | `BREAD_JWT_SECRET` diferente do ambiente anterior → tokens antigos invalidam; esperado. |

---

## Referência rápida no repositório

- `Dockerfile` — imagem da API.
- `docker-compose.yml` — **somente PostgreSQL** (produção na VPS).
- `src/main/resources/application-prod.properties` — datasource e perfil prod.
- `src/main/resources/application-dev.properties` — H2 local.
- Migrações: `src/main/resources/db/migration/`.

---

## Plataformas gerenciadas (legado)

Se ainda existir deploy no **Render** (ou similar): variáveis equivalentes (`SPRING_DATASOURCE_*`, `PORT` conforme o painel, `BREAD_JWT_SECRET`). Em caso de build “antigo”, use limpeza de cache de build + redeploy no painel. Fluxo principal deste documento é **VPS + Compose + perfil prod**.
