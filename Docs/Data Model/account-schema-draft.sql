CREATE TABLE user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK ( role in ('CUSTOMER', 'SHOP_ADMIN') ),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    shop_id BIGINT,

    FOREIGN KEY (shop_id)
        REFERENCES shop(id),
    CHECK (
        (role = 'CUSTOMER' AND shop_id IS NULL)
        OR
        (role = 'SHOP_ADMIN' AND shop_id IS NOT NULL)
    )
)