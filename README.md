# Bread (daily-bread)

API em **Spring Boot** para importar e consultar **planos de leitura bíblica** a partir de arquivos **PDF**. O sistema extrai o texto do PDF, interpreta dias e leituras (livro + faixa de capítulos), persiste o arquivo e os dias no banco e expõe endpoints REST para importação e listagem.

## Requisitos

- **Java 17**
- **Maven** (ou use o wrapper `./mvnw` / `mvnw.cmd` na raiz do projeto)

## Como executar

Na pasta do projeto:

```bash
./mvnw spring-boot:run
```

No Windows (CMD/PowerShell):

```bash
mvnw.cmd spring-boot:run
```

A aplicação sobe em **`http://localhost:9090`** (porta definida em `application.properties`).

## Build

```bash
./mvnw clean package
```

O JAR executável fica em `target/bread-0.0.1-SNAPSHOT.jar`:

```bash
java -jar target/bread-0.0.1-SNAPSHOT.jar
```

## Banco de dados (desenvolvimento)

Em desenvolvimento o projeto usa **H2 em memória** com **Flyway** para criar o schema. A console web do H2 está habilitada:

| Configuração | Valor |
|--------------|--------|
| URL no navegador | `http://localhost:9090/h2-console` |
| JDBC URL (no formulário) | `jdbc:h2:mem:breaddb` |
| Usuário | `sa` |
| Senha | `123456` |

Os dados em memória são perdidos ao encerrar a aplicação.

## API REST — planos de leitura

Base: `http://localhost:9090/api/v1/reading-plans`

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `POST` | `/` | Importa um PDF (`multipart/form-data`, campo **`file`**) |
| `GET` | `/` | Lista resumos dos planos importados |
| `GET` | `/{id}` | Retorna o plano com todos os dias |

**Exemplo (curl) — importar:**

```bash
curl -X POST http://localhost:9090/api/v1/reading-plans -F "file=@caminho/do/plano.pdf"
```

O PDF precisa ter **texto selecionável** (não basta imagem escaneada). O parser espera linhas no padrão `dia livro capítulo-capítulo` (por exemplo: `1 Gênesis 1-3`).

## Documentação interativa da API

Com **SpringDoc OpenAPI**, a UI costuma estar em:

- `http://localhost:9090/swagger-ui.html`

(Se o caminho mudar na sua versão do SpringDoc, consulte `springdoc` nos logs ou em `application.properties`.)

## Organização do código (módulo `readingplan`)

Pacote base: `com.daily.bread.readingplan`

| Pacote | Função |
|--------|--------|
| `model` | Entidades JPA |
| `repository` | Spring Data JPA |
| `services` | Regras de negócio e leitura/parse do PDF |
| `controllers` | REST |
| `exceptions` | Exceções e tratamento HTTP (`@RestControllerAdvice`) |
| `response` | DTOs de resposta da API |

## Stack principal

- Spring Boot 4.x
- Spring Web, Data JPA, Validation, Actuator
- Flyway, H2 (dev), driver PostgreSQL (para evolução futura)
- Apache PDFBox (extração de texto de PDFs)
- SpringDoc OpenAPI (Swagger UI)

## Licença

Defina a licença do repositório conforme a política do seu time ou projeto.
