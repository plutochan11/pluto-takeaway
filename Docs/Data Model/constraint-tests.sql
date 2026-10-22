-- Manual learning script: seven statements below intentionally fail.
-- Run in ONE connection, using the mysql client with --force so it continues
-- after expected errors and reaches ROLLBACK. Do not run as a schema migration.
-- Requires the existing shop/category/product tables and InnoDB transactions.
-- Test rows are rolled back. AUTO_INCREMENT counters may still advance.

SELECT VERSION() AS mysql_version, DATABASE() AS database_name;
SET @shops_before = (SELECT COUNT(*) FROM shop);
SET @categories_before = (SELECT COUNT(*) FROM category);
SET @products_before = (SELECT COUNT(*) FROM product);

START TRANSACTION;

INSERT INTO shop (name) VALUES ('__constraint_test_shop_A');
SET @shop_a = LAST_INSERT_ID();
INSERT INTO shop (name) VALUES ('__constraint_test_shop_B');
SET @shop_b = LAST_INSERT_ID();
INSERT INTO category (shop_id, name)
VALUES (@shop_a, '__constraint_test_category_A');
SET @category_a = LAST_INSERT_ID();
INSERT INTO category (shop_id, name)
VALUES (@shop_b, '__constraint_test_category_B');
SET @category_b = LAST_INSERT_ID();

SELECT 'T01 default shop status' AS test,
       IF(status = 'CLOSED' AND id > 0, 'PASS', 'FAIL') AS result
FROM shop WHERE id = @shop_a;

SELECT 'T02 default category sort order' AS test,
       IF(sort_order = 0, 'PASS', 'FAIL') AS result
FROM category WHERE id = @category_a;

INSERT INTO product (shop_id, category_id, name, price)
VALUES (@shop_a, @category_a, '__constraint_test_product', 58.00);
SET @product_id = LAST_INSERT_ID();
SELECT 'T03 default product status and valid price' AS test,
       IF(status = 'UNAVAILABLE' AND price = 58.00, 'PASS', 'FAIL') AS result
FROM product WHERE id = @product_id;

SELECT 'T04 negative price: expect ERROR 3819' AS test;
INSERT INTO product (shop_id, category_id, name, price)
VALUES (@shop_a, @category_a, '__constraint_test_negative_price', -1.00);

SELECT 'T05 invalid shop status: expect ERROR 3819' AS test;
INSERT INTO shop (name, status)
VALUES ('__constraint_test_invalid_shop_status', 'UNKNOWN');

SELECT 'T06 NULL shop name: expect ERROR 1048' AS test;
INSERT INTO shop (name) VALUES (NULL);

SELECT 'T07 negative category order: expect ERROR 3819' AS test;
INSERT INTO category (shop_id, name, sort_order)
VALUES (@shop_a, '__constraint_test_negative_sort', -1);

SELECT 'T08 invalid product status: expect ERROR 3819' AS test;
INSERT INTO product (shop_id, category_id, name, price, status)
VALUES (@shop_a, @category_a, '__constraint_test_invalid_product_status', 10.00, 'UNKNOWN');

SELECT 'T09 cross-shop category: expect ERROR 1452' AS test;
INSERT INTO product (shop_id, category_id, name, price)
VALUES (@shop_a, @category_b, '__constraint_test_cross_shop', 10.00);

-- AUTO_INCREMENT produces positive IDs; -1 is unused in the current dataset.
SELECT 'T10 nonexistent shop: expect ERROR 1452' AS test;
INSERT INTO category (shop_id, name)
VALUES (-1, '__constraint_test_missing_shop');

ROLLBACK;

SELECT 'T11 rollback preserves original row counts' AS test,
       IF((SELECT COUNT(*) FROM shop) = @shops_before
          AND (SELECT COUNT(*) FROM category) = @categories_before
          AND (SELECT COUNT(*) FROM product) = @products_before,
          'PASS', 'FAIL') AS result;
