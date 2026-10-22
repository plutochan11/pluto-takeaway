insert into shop(id, name, status)
values (9001, 'Empty Shop Test', 'OPEN'),
       (9002, 'Menu Ordering Test', 'OPEN');

insert into category(id, shop_id, name, sort_order)
values (9101, 9002,'Desserts', 20),
       (9102, 9002,'Drinks', 10),
       (9103, 9002,'Main Courses', 10);

insert into product(id, shop_id, category_id, name, price, status)
values (9201, 9002, 9102, 'Test Coffee', 28.00, 'AVAILABLE');
