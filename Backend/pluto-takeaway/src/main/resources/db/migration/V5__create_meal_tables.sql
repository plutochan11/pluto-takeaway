-- ADR-002/003/004: meals reuse same-shop categories; options may reference
-- same-shop products or represent a labelled zero-supplement choice.
ALTER TABLE product
    ADD CONSTRAINT uq_product_shop_id UNIQUE (shop_id, id);

CREATE TABLE meal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNAVAILABLE',
    CONSTRAINT uq_meal_shop_id UNIQUE (shop_id, id),
    CONSTRAINT fk_meal_category FOREIGN KEY (shop_id, category_id)
        REFERENCES category (shop_id, id),
    CONSTRAINT chk_meal_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_meal_base_price CHECK (base_price >= 0),
    CONSTRAINT chk_meal_status CHECK (status IN ('AVAILABLE', 'UNAVAILABLE'))
);

CREATE TABLE meal_option_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    meal_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_meal_group_shop_id UNIQUE (shop_id, id),
    CONSTRAINT fk_meal_group_meal FOREIGN KEY (shop_id, meal_id)
        REFERENCES meal (shop_id, id),
    CONSTRAINT chk_meal_group_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_meal_group_sort CHECK (sort_order >= 0)
);

CREATE TABLE meal_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    product_id BIGINT,
    label VARCHAR(100),
    supplement DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNAVAILABLE',
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_meal_option_group FOREIGN KEY (shop_id, group_id)
        REFERENCES meal_option_group (shop_id, id),
    CONSTRAINT fk_meal_option_product FOREIGN KEY (shop_id, product_id)
        REFERENCES product (shop_id, id),
    CONSTRAINT chk_meal_option_shape CHECK (
        (product_id IS NOT NULL AND label IS NULL)
        OR
        (product_id IS NULL AND label IS NOT NULL
            AND CHAR_LENGTH(TRIM(label)) > 0 AND supplement = 0)
    ),
    CONSTRAINT chk_meal_option_supplement CHECK (supplement >= 0),
    CONSTRAINT chk_meal_option_status CHECK (status IN ('AVAILABLE', 'UNAVAILABLE')),
    CONSTRAINT chk_meal_option_sort CHECK (sort_order >= 0)
);
