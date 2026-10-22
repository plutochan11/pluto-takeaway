CREATE TABLE shop(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
    CHECK(status in ('OPEN', 'CLOSED'))
);

CREATE TABLE category(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (shop_id)
                     REFERENCES shop(id),
    UNIQUE (shop_id, id),
    CHECK ( sort_order >= 0 )
);

CREATE TABLE product(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNAVAILABLE',
    FOREIGN KEY (shop_id, category_id)
                    REFERENCES category(shop_id, id),
    CHECK ( price >= 0 ),
    CHECK ( status in ('UNAVAILABLE', 'AVAILABLE') )
);