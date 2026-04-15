# Prompt: pré-deploy Docker e Render (API Bread)

Documento para **você** ou para **colar em um assistente** ao final de uma entrega, garantindo build local, imagem Docker alinhada ao `Dockerfile` e deploy seguro no Render.

---

## Objetivo

1. Validar o projeto com Maven **antes** do build Docker (economiza tempo e pega falhas cedo).
2. Garantir que `docker build` conclui na máquina (ou no CI) com o mesmo `Dockerfile` usado pelo Render.
3. Conferir **migrações Flyway** e **variáveis de ambiente** de produção antes do push.

---

## Frase curta para colar (humano ou IA)

> *No repositório `api-daily-bread`, na raiz: executar `./mvnw clean verify`; em seguida `docker build -t bread-api:local .` e confirmar sucesso. Não alterar migrações Flyway já aplicadas em produção; novas só como `V*__*.sql`. Em produção: `bread.seed-test-user=false`, JWT forte (`BREAD_JWT_SECRET` ou `bread.jwt.secret`), datasource Postgres correto. Depois commit/push na branch do Render e verificar deploy; se o build parecer desatualizado, limpar cache de build no Render e redeployar.*

---

## O que o Dockerfile faz (resumo)

| Etapa | Conteúdo |
|--------|-----------|
| **Build** | Imagem `eclipse-temurin:17-jdk-jammy`, copia `pom.xml`, `src`, `mvnw`, roda `./mvnw clean package -DskipTests`. |
| **Runtime** | Imagem `eclipse-temurin:17-jre-jammy`, usuário não-root `spring`, JAR em `/app/app.jar`. |
| **Porta** | `EXPOSE 8080`; `ENV PORT=8080`. O app usa `${PORT:9090}` no `application.properties` — no Render, defina `PORT` conforme o serviço (geralmente `10000` ou o que o painel indicar). |

Artefato esperado: `target/bread-0.0.1-SNAPSHOT.jar` (versão do `pom.xml`).

---

## Checklist (ordem recomendada)

### 1. Código e banco

- [ ] Tudo que deve ir para produção está commitado; `git status` revisado.
- [ ] Novas alterações de schema apenas via **nova** migração em `src/main/resources/db/migration/` (`V5__...`, etc.).
- [ ] **Nunca** reescrever migrações já aplicadas em ambientes compartilhados.

### 2. Produção — configuração sensível

- [ ] `bread.seed-test-user=false` (ou variável ausente com default desligado no perfil prod, conforme sua estratégia).
- [ ] Segredo JWT forte e não versionado: `BREAD_JWT_SECRET` / `bread.jwt.secret`.
- [ ] Datasource: URL, usuário e senha do **Postgres** (Render ou outro) conferidos no painel.
- [ ] `spring.jpa.hibernate.ddl-auto=validate` em prod (já é o padrão típico; evitar `update` em produção sem critério).

### 3. Build local (Maven)

Na **raiz** do repositório (`api-daily-bread`):

```bash
./mvnw clean verify
```

- **Preferido:** `verify` roda testes e valida o pacote.
- **Rápido (menos seguro):** `./mvnw clean package -DskipTests` — alinhado ao que o **Dockerfile** executa dentro da imagem.

### 4. Imagem Docker (local)

Pré-requisito: **Docker Desktop** (ou daemon) em execução.

```bash
docker build -t bread-api:local .
```

- Falhou: corrigir código, `pom.xml` ou `Dockerfile` antes do push.
- Windows: se aparecer erro de *pipe* `dockerDesktopLinuxEngine`, abra o Docker Desktop e tente de novo.

### 5. Smoke opcional (container + Postgres)

Ajuste host, porta, banco e credenciais. Exemplo com Postgres na máquina host (Windows/Mac: `host.docker.internal`):

```bash
docker run --rm -p 8080:8080 \
  -e PORT=8080 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/nome_do_banco' \
  -e SPRING_DATASOURCE_USERNAME=usuario \
  -e SPRING_DATASOURCE_PASSWORD=senha \
  -e BREAD_JWT_SECRET='seu-secret-minimo-32-chars' \
  -e BREAD_SEED_TEST_USER=false \
  bread-api:local
```

No Render, use as variáveis equivalentes configuradas no serviço (nomes podem seguir o mesmo padrão Spring Boot `SPRING_DATASOURCE_*`).

### 6. Git e Render

- [ ] Push na branch conectada ao serviço Web.
- [ ] Deploy automático concluído ou deploy manual disparado.
- [ ] Pós-deploy: `/actuator/health` (se público), login, rota nova crítica (ex.: `GET /api/v1/reading-progress/dashboard` autenticado).
- [ ] Se a versão parecer antiga: **Clear build cache** no Render e novo deploy.

---

## Comandos mínimos (copiar e colar)

```bash
cd /caminho/para/api-daily-bread
./mvnw clean verify
docker build -t bread-api:local .
```

---

## Problemas frequentes

| Sintoma | Ação |
|---------|------|
| `dockerDesktopLinuxEngine` / daemon inacessível | Iniciar Docker Desktop; aguardar estado “running”. |
| Build Maven ok, Docker falha | Ver log do estágio `RUN ./mvnw`; muitas vezes dependência de rede ou erro de compilação só no Linux. |
| App sobe mas Flyway erro | Ordem de migrações, conflito de versão, ou URL de banco errada no ambiente. |
| Render com código velho | Novo commit, ou limpar cache de build + redeploy. |

---

## Referência

- `Dockerfile` na raiz do projeto.
- Migrações: `src/main/resources/db/migration/`.
- Porta local no `application.properties` usa `${PORT:9090}`; container/Render deve definir `PORT` de forma consistente com o mapeamento do serviço.
