# Pluto Takeaway

A collection-only, multi-shop food ordering learning project. The current implementation provides public shop and menu browsing. Authentication, baskets and orders are planned, not implemented.

## Current stack

- Java 21, Spring Boot 4.1.1, Maven.
- Spring Data JPA with Hibernate; MySQL Connector/J.
- MySQL 8.4 in Docker Compose, published at `127.0.0.1:3307`.
- Flyway schema migrations and development-only demo data.

## Prerequisites

- JDK 21 and Docker Desktop with Docker Compose available and running.
- IntelliJ IDEA, or a Maven 3.9.x installation for terminal commands below.
- Ports 3307 and 8080 available for the database and application respectively.

The checked-in Maven wrapper currently selects Maven 3.10.0. An IDEA 2026.1 import failure was encountered with that version and resolved by selecting Bundled Maven 3.9.11. The terminal examples below use `mvn`, not the wrapper. This does not establish a general Maven 3.10 command-line incompatibility.

## 1. Start MySQL

Run from the repository root:

```bash
cp .env.example .env
```

Edit `.env` and replace both placeholder passwords. If `.env` already exists, retain the existing file instead of overwriting it. `.env` is ignored by Git.

```bash
docker compose up -d mysql
docker compose logs -f mysql
```

Wait for completed initialization and `ready for connections`; use Ctrl+C to stop following logs. Container data is stored in the project-scoped `mysql_data` named volume. The host MySQL installation is not used.

The image initially creates `pluto_takeaway` and user `pluto`. This name may already contain the earlier manual schema exercises. Use the separate migration demonstration database below to avoid that conflict.

## 2. Prepare an empty migration database

For a fresh setup, connect as root using `MYSQL_ROOT_PASSWORD`:

```bash
docker compose exec mysql mysql -u root -p
```

```sql
CREATE DATABASE pluto_takeaway_m1 CHARACTER SET utf8mb4;
GRANT ALL PRIVILEGES ON pluto_takeaway_m1.* TO 'pluto'@'%';
exit;
```

If this database already exists and has successfully migrated, reuse it; do not recreate or manually import the draft schema/seed. A nonempty manually created schema without Flyway history is not the same as an empty migration target.

Database account creation/permissions are setup responsibilities. Flyway in this project creates tables inside an existing database, not the database account itself.

## 3. Run the backend in IDEA

Open `Backend/pluto-takeaway/pom.xml` as a Maven project, or link it with **Add as Maven Project** from the outer workspace. Select JDK 21 and Bundled Maven 3.9.11, then reload Maven dependencies.

Create/run the main class `com.pluto.takeaway.PlutoTakeawayApplication`. In its Run Configuration set:

| Setting | Value |
|---|---|
| Active profiles | `dev` |
| Environment variable `DB_URL` | `jdbc:mysql://127.0.0.1:3307/pluto_takeaway_m1` |
| Environment variable `DB_PASSWORD` | The actual application password from `MYSQL_PASSWORD` in `.env` |

Docker Compose reads `.env`; an application launched by IDEA does not automatically inherit that file. Do not store the password in `application.yaml` or a shared Run Configuration.

Alternative terminal startup: with JDK 21 selected and `DB_PASSWORD` already exported in the shell, run from `Backend/pluto-takeaway`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--spring.datasource.url=jdbc:mysql://127.0.0.1:3307/pluto_takeaway_m1
```

Flyway applies the migrations before Hibernate validates entity mappings:

- `src/main/resources/db/migration/V1__create_shop_menu_tables.sql`: schema.
- `src/main/resources/dev/db/migration/V2__insert_demo_menu.sql`: demo data, selected only by the `dev` profile configuration.

Do not edit applied migration files. Add a new version for subsequent changes. Hibernate uses `ddl-auto: validate`, not schema creation/update.

## 4. Verify

Open these URLs or use [the saved HTTP requests](./Docs/requests.http):

- `http://localhost:8080/api/hello`
- `http://localhost:8080/api/shops`
- `http://localhost:8080/api/shops/1/menu`
- `http://localhost:8080/api/shops/2/menu`

The default dev seed contains two shops, two categories and three products. Shop 1 contains Chicken Rice. Shop 2 contains Coffee and unavailable Lemon Tea. Menus expose unavailable products with their status.

Connect to the migrated database:

```bash
docker compose exec mysql mysql -u pluto -p pluto_takeaway_m1
```

```sql
SELECT version, description, success
FROM flyway_schema_history ORDER BY installed_rank;
```

Restart the backend. V1/V2 should remain recorded once each and the seed should not be inserted again. Empty-shop/ordering fixtures from the earlier exercise are not included in V2; see the demo guide.

## Tests and verification status

From `Backend/pluto-takeaway`, with `DB_URL`, `DB_PASSWORD` and `SPRING_PROFILES_ACTIVE=dev` supplied, `mvn test` runs the current test sources. The generated context-load test is not a complete endpoint integration suite and can connect to the configured database. Use a dedicated test target for automated database tests once that suite is introduced.

The developer reports successful browser checks and a successful Flyway restart. Earlier constraint tests were executed against the original exercise database with recorded output. The documented fresh setup has not yet been independently rerun end to end, and automated M1 API integration coverage is pending. See [Demo Guide](./Docs/Demo%20Guide.md) for evidence and remaining checks.

## Stop and resume

From the repository root, `docker compose stop mysql` stops MySQL and `docker compose up -d mysql` resumes it. `docker compose down` removes the Compose containers/network while retaining the named volume. Adding `-v` deletes the named volume and its database data; it is not needed for ordinary restart.

## Project documents

- [Product scope](./Docs/Pluto%20Takeaway%20Scope%20%26%20Requirements.md)
- [Milestones](./Docs/Pluto%20Takeaway%20Milestones.md)
- [Data model](./Docs/Data%20Model/Data%20Model.md)
- [API contract](./Docs/API%20Contract.md)
- [Demo guide and evidence](./Docs/Demo%20Guide.md)
