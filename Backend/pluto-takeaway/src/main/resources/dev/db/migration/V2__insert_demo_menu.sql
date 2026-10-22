INSERT INTO shop (id, name, status)
VALUES (1, 'Pluto Kitchen', 'OPEN'),
       (2, 'Harbour Cafe', 'OPEN');

INSERT INTO category (id, shop_id, name, sort_order)
VALUES (10, 1, 'Main Courses', 10),
       (20, 2, 'Drinks', 10);

INSERT INTO product (id, shop_id, category_id, name, price, status)
VALUES (100, 1, 10, 'Chicken Rice', 58.00, 'AVAILABLE');

INSERT INTO product (id, shop_id, category_id, name, price, status)
VALUES (200, 2, 20, 'Coffee', 28.00, 'AVAILABLE'),
       (201, 2, 20, 'Lemon Tea', 25.00, 'UNAVAILABLE');