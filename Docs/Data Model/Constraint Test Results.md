# Constraint Test Results

Executed: 20 October 2026 (Asia/Shanghai).

Target: Docker Compose service `mysql`, MySQL **8.4.8**, database `pluto_takeaway`.

Script: [constraint-tests.sql](./constraint-tests.sql). Raw evidence: [constraint-test-output.txt](./constraint-test-output.txt).

## Results

| Test | Scenario | Actual outcome |
|---|---|---|
| T01 | Shop default status / generated ID | PASS: CLOSED and positive ID |
| T02 | Category default sort_order | PASS: 0 |
| T03 | Product default status / exact price | PASS: UNAVAILABLE and 58.00 |
| T04 | Negative price | PASS: ERROR 3819, product_chk_1 |
| T05 | Invalid shop status | PASS: ERROR 3819, shop_chk_1 |
| T06 | NULL shop name | PASS: ERROR 1048 |
| T07 | Negative category sort_order | PASS: ERROR 3819, category_chk_1 |
| T08 | Invalid product status | PASS: ERROR 3819, product_chk_2 |
| T09 | Cross-shop category reference | PASS: ERROR 1452, composite foreign key |
| T10 | Nonexistent shop reference | PASS: ERROR 1452, category foreign key |
| T11 | Rollback preserves original row counts | PASS |

All 11 cases met their expected outcomes. Seven deliberately invalid INSERT statements returned the expected MySQL errors at the expected script line numbers. The three valid/default cases and final rollback count check returned PASS. The runner checked these outcomes explicitly; mysql --force exit status alone was not used as proof.

## Data handling

All test INSERT statements ran in one transaction ending in ROLLBACK. Original table row counts were unchanged. AUTO_INCREMENT counters may advance despite rollback, so gaps in generated IDs are expected. Existing seed data was not reinserted or edited. Only the seed SQL file was reformatted to name columns explicitly and terminate its final statement.

## Re-run

From the project root, run:

```bash
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --user="$MYSQL_USER" --database="$MYSQL_DATABASE" --force --batch --verbose' < "Docs/Data Model/constraint-tests.sql"
```

The shell inside the container reads the configured credentials; this command does not print their values. Expected ERROR lines are part of this learning script. --force is required to continue after them and reach ROLLBACK. Read the actual outcomes, rather than treating every ERROR line as a failed test.

## Limits

These cases cover initial schema constraints and defaults. They do not validate API authorisation, all input rules or checkout concurrency. NOT NULL alone does not reject an empty string. Status case-sensitivity depends on the column collation and was not tested here. Money overprecision validation remains future application work.
