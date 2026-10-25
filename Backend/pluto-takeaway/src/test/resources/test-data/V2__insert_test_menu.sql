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

insert into shop(id, name, status)
values (9001, 'Empty Shop Test', 'OPEN'),
       (9002, 'Menu Ordering Test', 'OPEN');

insert into category(id, shop_id, name, sort_order)
values (9101, 9002,'Desserts', 20),
       (9102, 9002,'Drinks', 10),
       (9103, 9002,'Main Courses', 10);

insert into product(id, shop_id, category_id, name, price, status)
values (9201, 9002, 9102, 'Test Coffee', 28.00, 'AVAILABLE');
