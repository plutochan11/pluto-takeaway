# Pluto Takeaway

A collection-only, multi-shop food ordering learning project. The current implementation provides public shop/menu browsing, Session login/logout and shop-admin reads/name updates restricted by role and ownership. Baskets, orders and full menu management are not implemented.

## Current stack

- Java 21, Spring Boot 4.1.1, Maven.
- Spring Data JPA with Hibernate; MySQL Connector/J.
- MySQL 8.4 in Docker Compose, published at `127.0.0.1:3307`.
- Flyway schema migrations and development-only demo data.
- Spring Security, BCrypt passwords, Session authentication, CSRF protection and Jakarta Validation.

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
- `src/main/resources/dev/db/migration/V2__insert_demo_menu.sql`: demo menu data, selected only by `dev`.
- `src/main/resources/db/migration/V3__create_user_account.sql`: account schema.
- `src/main/resources/dev/db/migration/V4__insert_demo_accounts.sql`: demo accounts, selected only by `dev`.

Production locations contain V1/V3; dev adds V2/V4. Test uses separate V2/V4 fixtures in its independent database. Migration versions need not be contiguous.

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

Restart the backend. V1/V2/V3/V4 should remain recorded once each and the seed should not be inserted again. Empty-shop/ordering fixtures from the earlier exercise are not included in V2; see the demo guide.

## Tests and verification status

From `Backend/pluto-takeaway`, with Java 21, Maven and Docker Desktop available:

```bash
mvn test
```

The test class activates `test`, imports a Spring-managed MySQL Testcontainer and uses MockMvc against real controllers/services/repositories. Service connection details select an independent container; no Compose startup, dev `.env` or `DB_PASSWORD` is required for tests. Flyway applies V1/V3 plus test-only V2/V4 fixtures. The current 43-test suite covers browsing, account/password mapping, real login and session reuse, logout, Session ID rotation, disabled login, role/ownership boundaries, CSRF, persisted shop-name updates, rejected writes, validation, ignored extra JSON fields and error JSON. Mutation tests reset affected fixture data before/after execution; do not enable parallel execution without redesigning this isolation. Tests do not start a listening Tomcat server.

On 24 October 2026 the agent independently ran the suite successfully. On 25 October the developer reported the full `mvn test` suite also passed from a fresh GitHub clone: 8 tests, 0 failures, 0 errors. Maven caches may be reused; this is not a completely clean-machine certification. Manual application startup and Flyway restart were separately verified by the developer. See [Demo Guide](./Docs/Demo%20Guide.md) for provenance and limits.

M1 and the bounded M2 backend scope are complete. M2 adds uniform JSON errors for the documented security, validation and resource-error cases; it does not promise a custom body for every possible framework error. The developer reports 43 tests passing, corroborated by the local Surefire report reviewed on 3 November 2026. The agent independently ran the earlier 40-test suite on 2 November. MockMvc does not verify real Tomcat cookie transport or idle expiration.

See the [M2 API contract](./Docs/M2%20API%20Contract.md) and [demo guide](./Docs/Demo%20Guide.md) for the cookie/CSRF flow. Dev/test accounts `customer_a`, `customer_b`, `admin_a`, `admin_b` use the public fixture password `password`; admins own shops 1 and 2 respectively. Never reuse these demonstration credentials in deployment. Sessions are in memory, use a configured 30-minute inactivity timeout, HttpOnly and SameSite=Lax cookies; Secure cookies over deployment HTTPS remain deployment work.

## Stop and resume

From the repository root, `docker compose stop mysql` stops MySQL and `docker compose up -d mysql` resumes it. `docker compose down` removes the Compose containers/network while retaining the named volume. Adding `-v` deletes the named volume and its database data; it is not needed for ordinary restart.

## Project documents

- [Product scope](./Docs/Pluto%20Takeaway%20Scope%20%26%20Requirements.md)
- [Milestones](./Docs/Pluto%20Takeaway%20Milestones.md)
- [Data model](./Docs/Data%20Model/Data%20Model.md)
- [Public browsing API contract](./Docs/API%20Contract.md)
- [M2 authentication and ownership contract](./Docs/M2%20API%20Contract.md)
- [Architecture decisions](./Docs/Decisions.md)
- [Demo guide and evidence](./Docs/Demo%20Guide.md)
