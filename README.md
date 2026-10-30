# QueryPilot — AI SQL Assistant

**Quick start:** [Local MySQL with Docker Compose + Maven Wrapper](docs/local-setup.md) · [Postman API examples](docs/api-examples.md)

QueryPilot is a Java/Spring Boot REST API that turns a natural-language question into SQL using DeepSeek, checks the generated statement, executes it against MySQL, and returns rows as JSON.

It is a backend learning project. The current implementation aims to allow SELECT queries, but its string-based validation is not a complete SQL security boundary. Use a dedicated read-only database account and a non-sensitive demo database.

## What is implemented

- Questions submitted through `POST /api/query`.
- Request constraints: a nonblank question, at most 500 characters.
- Dynamic discovery of base-table names, column names, and data types from the connected MySQL database.
- DeepSeek chat-completions integration with the configured `deepseek-chat` model.
- SQL normalization, SELECT-prefix checks, multiple-statement rejection, and keyword checks.
- Query execution through `JdbcTemplate`.
- JSON responses containing the question, generated SQL, rows, row count, elapsed time, and a message.
- A plain-text root endpoint confirming that the HTTP application is running.

## Technology and source guide

| Component | Implementation |
| --- | --- |
| Java / build | Java 21, Maven Wrapper (Maven 3.9.9) |
| Framework | Spring Boot 3.5.5 |
| HTTP and validation | Spring Web, Jakarta Bean Validation |
| Database | MySQL Connector/J, Spring JDBC / JdbcTemplate |
| Dependencies | Spring Data JPA and Spring Boot Test are declared; no entity classes are committed; unit tests use JUnit 5 and Mockito |
| AI client | Spring RestClient and Jackson |
| Entry point | [QueryPilotApplication.java](src/main/java/org/querypilot/QueryPilotApplication.java) |
| API | [QueryController.java](src/main/java/org/querypilot/controller/QueryController.java), [HomeController.java](src/main/java/org/querypilot/controller/HomeController.java) |
| Orchestration | [QueryService.java](src/main/java/org/querypilot/service/QueryService.java) |
| Schema discovery | [DatabaseSchemaService.java](src/main/java/org/querypilot/service/DatabaseSchemaService.java) |
| AI request/response handling | [LlmService.java](src/main/java/org/querypilot/llm/LlmService.java) |
| SQL checks / execution | [SqlValidator.java](src/main/java/org/querypilot/validator/SqlValidator.java), [QueryExecutionService.java](src/main/java/org/querypilot/service/QueryExecutionService.java) |
| Configuration | [application.properties](src/main/resources/application.properties), [pom.xml](pom.xml) |

## Request flow

1. The controller accepts and validates the question.
2. The schema service queries `INFORMATION_SCHEMA` for the currently selected database.
3. The AI service sends the question and schema to DeepSeek, asking for a SELECT statement or `INVALID_QUERY`.
4. The validator normalizes the generated SQL and applies string-based checks.
5. The execution service performs another set of checks and calls `JdbcTemplate.queryForList`.
6. The service returns the rows and response metadata.

Schema discovery includes base tables and column types. It does not collect foreign-key relationships, constraints, or sample rows. The AI request contains the user's question and discovered schema; this implementation does not send query result rows back to the AI service.

## Local setup

### 1. Prerequisites and clone

You need JDK 21, a running MySQL server, and your own DeepSeek API key with API access.

```sh
git clone https://github.com/53Husain/QueryPilot-AI-SQL-Assistant.git
cd QueryPilot-AI-SQL-Assistant
java -version
./mvnw -version
```

### 2. Prepare a demo database

The repository's [schema.sql](src/main/resources/schema.sql) is empty. There are no entity classes. The optional Compose setup seeds a synthetic employees table through `dev/mysql/`; the manual setup below remains available.

Using a database administrator account, create your own demo database and tables. The following is **optional sample data for this README**, not an existing repository dataset. Run it only in a fresh demo database:

```sql
CREATE DATABASE querypilot_db;
USE querypilot_db;

CREATE TABLE employees (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    salary DECIMAL(10, 2) NOT NULL
);

INSERT INTO employees (id, name, department, salary) VALUES
    (1, 'Asha', 'Engineering', 65000.00),
    (2, 'Rohan', 'Support', 42000.00);
```

Configure a separate application account with SELECT permission on the demo tables, rather than connecting the application as an administrator. Ensure it can discover those tables through MySQL metadata. Account creation and permission setup are separate from the application's code.

### 3. Set environment variables

The committed configuration reads `DB_PASSWORD` and `DEEPSEEK_API_KEY`. Its database username defaults to `root`; override that with your dedicated application account.

**PowerShell:**

```powershell
$env:DB_PASSWORD = 'YOUR_DATABASE_PASSWORD'
$env:DEEPSEEK_API_KEY = 'YOUR_DEEPSEEK_API_KEY'
$env:SPRING_DATASOURCE_USERNAME = 'YOUR_READ_ONLY_DATABASE_USER'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'none'
```

**Bash:**

```bash
export DB_PASSWORD='YOUR_DATABASE_PASSWORD'
export DEEPSEEK_API_KEY='YOUR_DEEPSEEK_API_KEY'
export SPRING_DATASOURCE_USERNAME='YOUR_READ_ONLY_DATABASE_USER'
export SPRING_JPA_HIBERNATE_DDL_AUTO='none'
```

Replace the uppercase example values locally. Do not commit real passwords or API keys.

[.env.example](.env.example) is a reference only: **the default profile does not load `.env`; the opt-in `local` profile does**. See the local setup guide for the profile-specific configuration import. Set variables in the terminal that starts Maven, or in your IDE's run configuration.

| Setting | Committed value / purpose |
| --- | --- |
| HTTP port | `8080`; override with `SERVER_PORT` |
| Database URL | `jdbc:mysql://localhost:3306/querypilot_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| Database URL override | `SPRING_DATASOURCE_URL` |
| Database password | `DB_PASSWORD` |
| AI API key | `DEEPSEEK_API_KEY` |
| AI URL | `https://api.deepseek.com/chat/completions`; override with `DEEPSEEK_API_URL` |
| AI model | `deepseek-chat`; override with `DEEPSEEK_MODEL` |
| Hibernate DDL | Committed as `update`; the example above overrides it to `none` for a manually prepared, read-only database |

The committed connection URL is for local development and disables SSL. Configure an appropriate connection separately for a remote database.

### 4. Run

```sh
./mvnw spring-boot:run
```

Or package and launch:

```sh
./mvnw clean package
java -jar target/querypilot-0.0.1-SNAPSHOT.jar
```

The environment variables must also be available to the Java process when launching the JAR.

Open `http://localhost:8080/`. The root controller returns:

```text
QueryPilot AI SQL Assistant is running!
```

This is a plain-text endpoint, not a frontend or a full database/AI health check.

## API usage

### Ask a question

`POST http://localhost:8080/api/query`

Header: `Content-Type: application/json`

```json
{
  "question": "Show the names and salaries of employees earning more than 50000."
}
```

In Postman, choose POST, set the URL above, select **Body → raw → JSON**, and paste the request.

**Bash/curl:**

```bash
curl -X POST http://localhost:8080/api/query \
  -H 'Content-Type: application/json' \
  -d '{"question":"Show the names and salaries of employees earning more than 50000."}'
```

**PowerShell:**

```powershell
$body = @{ question = 'Show the names and salaries of employees earning more than 50000.' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/query' -ContentType 'application/json' -Body $body
```

### Response fields

The response shape comes from [QueryResponse.java](src/main/java/org/querypilot/dto/QueryResponse.java):

| Field | Meaning |
| --- | --- |
| `question` | Submitted question |
| `sql` | Normalized generated SQL, or null for `INVALID_QUERY` |
| `data` | Array of row objects |
| `rowCount` | Number of rows returned |
| `executionTimeMs` | Service elapsed time, including schema discovery and the AI call—not just SQL execution |
| `message` | Success, unsupported-question, or SQL-validation message |

**Illustrative response, not a recorded API result:** the AI may produce different SQL, and timing will vary.

```json
{
  "question": "Show the names and salaries of employees earning more than 50000.",
  "sql": "SELECT name, salary FROM employees WHERE salary > 50000",
  "data": [
    { "name": "Asha", "salary": 65000.00 }
  ],
  "rowCount": 1,
  "executionTimeMs": 1200,
  "message": "Query executed successfully"
}
```

Other questions for the optional demo table include “How many employees are there?” and “Show the average salary by department.” These are suggested prompts, not guaranteed AI outputs.

### Error and rejection behavior

| Condition | Current handling |
| --- | --- |
| AI returns `INVALID_QUERY` | Normal HTTP 200 response with null SQL, empty data, and an explanatory message |
| First SQL validator rejects the statement | Normal HTTP 200 response with empty data and a validation message |
| Execution checks throw `IllegalArgumentException` | HTTP 400 with timestamp, status, error, and message |
| AI request, database, or other exception reaches the generic handler | HTTP 500 with a generic message |
| Request fails Bean Validation | DTO constraints exist, but there is no dedicated validation-exception handler; the broad exception handler can map these errors to HTTP 500 |

Do not interpret HTTP 200 alone as successful SQL execution; inspect `message` and the response fields. See [GlobalExceptionHandler.java](src/main/java/org/querypilot/exception/GlobalExceptionHandler.java).

## Validation limits and current boundaries

- SQL filtering uses prefixes, substrings, and regular expressions—not a SQL parser. It does not establish that every accepted SELECT is safe or inexpensive.
- The validator does not enforce a table/column allowlist, permission checks, result limits, or query timeouts. The prompt's instruction to use only the discovered schema is not an enforcement mechanism.
- Normalization collapses whitespace globally, including inside quoted literals. Keyword checks can reject legitimate strings or identifiers.
- There is no authentication, authorization, or rate limiting. The query controller declares unrestricted `@CrossOrigin`.
- Generated SQL is executed immediately after checks, without a separate preview/approval step.
- The schema is rediscovered on each request. AI calls have no explicit retry or timeout policy in the application code.
- Query results are materialized in memory; no pagination or maximum-row policy is implemented.
- No frontend or screenshots are committed. A Postman collection is available in `docs/`. Unit tests and a Java 21 CI workflow are included; CI also checks local MySQL with a mocked AI provider; live provider coverage is still missing.
- No license file is present; this README does not assert an open-source license.

## Automated tests

Run with JDK 21 and Maven:

```sh
./mvnw clean verify
```

The [JUnit 5 / Mockito tests](src/test/java/org/querypilot) cover SQL normalization and rejection, orchestration with discovered schema, successful and empty results, and failure paths. Rejected queries are checked to ensure they never reach the executor or JDBC. Database and AI collaborators are mocked: tests require no API keys, MySQL server, or paid provider calls.

[GitHub Actions](.github/workflows/java.yml) compiles, tests, and packages the application on Java 21 for pushes and pull requests, and uploads Surefire reports. These are bounded unit tests, not proof that the keyword filters are a complete security boundary or that live MySQL/DeepSeek integration works.

## Verification and troubleshooting

This README was checked against the source, configuration, DTOs, and Maven project file. The MySQL/DeepSeek workflow was **not run during this documentation update**. Example SQL output and JSON data above are illustrative, and no benchmark or model-accuracy claim is made.

| Symptom | Check |
| --- | --- |
| Java release/build error | Both Maven and Java must use JDK 21 |
| Missing configuration value | Export the variables before starting the process; `.env` is not automatically loaded |
| Cannot connect to MySQL | Database existence, URL, local server, account host permissions, and password |
| Empty database / unavailable schema | Create at least one base table and check metadata visibility for the application account |
| AI generation failure | API key, provider access, outbound connectivity, configured URL/model |
| HTTP 200 but no data | Read `message`: the question may be unsupported, the SQL rejected, or the query may legitimately match no rows |

## Next improvements

These are proposed work, not implemented features:

1. Expand coverage with HTTP, MySQL, and AI-provider integration tests.
2. Map request-validation errors to a consistent HTTP 400 response.
3. Replace keyword filtering with structured SQL validation and explicit allowed operations/tables.
4. Add query timeouts, row limits, authentication, and rate limiting.
5. Add a generation-only preview mode and verified API examples.

Built by [Husain Lalji](https://www.linkedin.com/in/husain-lalji) · [GitHub](https://github.com/53Husain)
