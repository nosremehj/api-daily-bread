# Prompt: padrão de organização do backend (Spring Boot — projeto Bread)

Use este texto como **regra fixa** ao implementar novas funcionalidades ou ao pedir código para assistentes/IA. O objetivo é **não misturar** responsabilidades e **não** criar pacotes paralelos que depois exijam reorganização.

---

## Pacote raiz da aplicação

- **Base:** `com.daily.bread`
- **Classe principal:** `com.daily.bread.BreadApplication` (única `@SpringBootApplication` na raiz)
- **Configuração transversal** (CORS, beans globais, etc.): `com.daily.bread.config`

Não criar novas classes “soltas” em `com.daily.bread` além do `BreadApplication` e do que for explicitamente cross-cutting em `config`.

---

## Módulos por funcionalidade (feature)

Cada domínio ou feature (ex.: leitura de planos, usuários, notificações) vive em **`com.daily.bread.<nomeDoModulo>`**, em **minúsculas**, sem abreviações obscuras.

Exemplo existente: **`com.daily.bread.readingplan`**

Dentro de **cada** módulo, use **sempre** esta estrutura de pacotes (nomes exatos):

| Pacote | Responsabilidade |
|--------|------------------|
| `model` | Entidades JPA (`@Entity`), apenas persistência e relacionamentos. |
| `repository` | Interfaces Spring Data (`JpaRepository`, etc.). |
| `services` | Regras de negócio, orquestração, transações (`@Service`), parsers, integrações. |
| `controllers` | REST: `@RestController`, mapeamento HTTP, validação de entrada mínima. |
| `exceptions` | Exceções do domínio + `@RestControllerAdvice` que trata erros **desse módulo** (HTTP 4xx/5xx). |
| `response` | Records/DTOs de **saída** da API (JSON). Nomes sufixo `Response` quando fizer sentido. |

**Regras:**

- **Não** colocar entidades JPA em `services` ou `controllers` como retorno público da API; expor `response` (ou DTOs de entrada em `request` se no futuro existir).
- **Não** importar `controllers` em `services` ou `repository`.
- **Não** colocar DTOs de API em `model` (model = persistência).
- **Não** criar um pacote genérico `dto` na raiz do módulo; manter **`response`** (e, se necessário depois, **`request`**) para contratos HTTP.

---

## Nomenclatura sugerida

- Entidades: substantivo singular (`ReadingPlan`, `ReadingPlanDay`).
- Repositórios: `<Entidade>Repository`.
- Serviços: `<Contexto>Service` ou `<Verbo><Contexto>Service` quando for um caso de uso claro.
- Controllers: `<Contexto>Controller` + prefixo de path REST versionado (`/api/v1/...`).
- Exceções: `<Contexto><Motivo>Exception` (ex.: `ReadingPlanNotFoundException`).
- Handlers: `<Contexto>ExceptionHandler` no pacote `exceptions`.

---

## Tratamento de erros (`exceptions`)

- Exceções específicas do módulo ficam em **`com.daily.bread.<modulo>.exceptions`**.
- Um `@RestControllerAdvice` por módulo (ou consolidado por `basePackages = "com.daily.bread.<modulo>.controllers"`) para não acoplar handler a imports desnecessários.
- **Não** colocar handlers de API em `config` salvo tratamento global realmente genérico (ex.: 500 genérico), para manter coesão com o módulo.

---

## Persistência e migrações

- Scripts Flyway: `src/main/resources/db/migration/`, nomeados `V<n>__descricao.sql`.
- Tabelas alinhadas ao `model`; após criar entidades novas, **nova migration** — não alterar migrations antigas já aplicadas em ambientes compartilhados.

---

## Testes

- Espelhar o pacote do código: `src/test/java/com/daily/bread/<modulo>/services/...`.
- Testes de integração do controller podem ficar em `.../controllers` ou `.../integration` dentro do módulo, desde que consistente.

---

## Checklist antes de considerar a feature “pronta”

- [ ] Código novo está **inteiro** sob `com.daily.bread.<modulo>` com os subpacotes acima.
- [ ] Não há dependência de **service** → **controller**.
- [ ] Respostas HTTP usam tipos em **`response`**, não entidades expostas sem necessidade.
- [ ] Exceções de negócio mapeadas para status HTTP adequados no handler do módulo.
- [ ] Migração Flyway quando houver mudança de schema.

---

## Exemplo de árvore (módulo `readingplan`)

```
com.daily.bread.readingplan
├── model
├── repository
├── services
├── controllers
├── exceptions
└── response
```

Replique **a mesma forma** para `com.daily.bread.usuario`, `com.daily.bread.notificacao`, etc.

---

## Frase curta para colar em prompts de implementação

> *Implemente no projeto Spring Boot `bread`, pacote base `com.daily.bread`. Coloque a feature no módulo `com.daily.bread.<nome>`, usando obrigatoriamente os subpacotes `model`, `repository`, `services`, `controllers`, `exceptions` e `response`, conforme a documentação em `docs/PROMPT_PADRAO_ORGANIZACAO_BACKEND.md`. Não misture entidades JPA com DTOs de API; controllers finos; regras de negócio em services. Migrações Flyway em `db/migration` quando houver alteração de banco.*
