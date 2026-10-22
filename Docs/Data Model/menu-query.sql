-- Shop 1: retrieve all products, including unavailable ones.
-- This is a learning query, not yet the customer menu API contract.
SELECT
    p.name AS product_name,
    p.price AS price,
    c.name AS category_name,
    s.name AS shop_name
FROM product p
JOIN category c
    ON p.shop_id = c.shop_id
    AND p.category_id = c.id
JOIN shop s
    ON c.shop_id = s.id
WHERE s.id = 1
ORDER BY c.sort_order ASC, c.id ASC, p.id ASC;
