INSERT INTO user_account(id, username, password_hash, role, enabled, shop_id)
VALUES (101, 'customer_a', '{bcrypt}$2a$10$tz61SqR74lsh3aH.WML6AOVxY9H42kNR/Oe6yt4qsTXBrbfzbCCqW','CUSTOMER', TRUE, NULL),
       (102, 'customer_b', '{bcrypt}$2a$10$j6QNNvLuTj0ov8azEe89NuLaN1kMTbQiU4u3bhqIExy1N6h86kIWO','CUSTOMER', TRUE, NULL),
       (103, 'admin_a', '{bcrypt}$2a$10$N.j6uuChGHwEwzHpaxEaJOAhAhx1NxTuHZfZTaIyiyLj803oX2QWO','SHOP_ADMIN', TRUE, 1),
       (104, 'admin_b', '{bcrypt}$2a$10$y2L8v5BdJvcrCwrY5DK52ehIvaUp/yNPmIXTAWNJNNtVkvuh5BeEu','SHOP_ADMIN', TRUE, 2);