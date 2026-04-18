DROP DATABASE IF EXISTS mall_fei;
CREATE DATABASE mall_fei DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE mall_fei;

CREATE TABLE ums_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mobile VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar VARCHAR(255) DEFAULT '',
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE ums_admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE ums_user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    is_default TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE pms_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE pms_spu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    main_image VARCHAR(255) DEFAULT '',
    album_images TEXT,
    description TEXT,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE pms_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    spu_id BIGINT NOT NULL,
    sku_code VARCHAR(50) NOT NULL UNIQUE,
    sku_name VARCHAR(100) NOT NULL,
    spec_json VARCHAR(500) DEFAULT '{}',
    sale_price DECIMAL(10, 2) NOT NULL,
    origin_price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    lock_stock INT NOT NULL DEFAULT 0,
    sales INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    count INT NOT NULL DEFAULT 1,
    checked TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE oms_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(30) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    pay_amount DECIMAL(10, 2) NOT NULL,
    freight_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    receiver_address VARCHAR(255) NOT NULL,
    remark VARCHAR(255) DEFAULT '',
    pay_type VARCHAR(30) DEFAULT 'MOCK',
    pay_time DATETIME NULL,
    cancel_time DATETIME NULL,
    finish_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE oms_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(50) NOT NULL,
    sku_id BIGINT NOT NULL,
    spu_id BIGINT NOT NULL,
    sku_name VARCHAR(100) NOT NULL,
    sku_image VARCHAR(255) DEFAULT '',
    sale_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE pay_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pay_no VARCHAR(50) NOT NULL UNIQUE,
    order_no VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    pay_amount DECIMAL(10, 2) NOT NULL,
    pay_status VARCHAR(30) NOT NULL,
    pay_channel VARCHAR(30) NOT NULL DEFAULT 'MOCK',
    transaction_no VARCHAR(100) DEFAULT '',
    callback_content TEXT,
    pay_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

INSERT INTO ums_user (mobile, password, nickname) VALUES
('13800000000', '123456', '示例用户');

INSERT INTO ums_admin (username, password, nickname, role_code) VALUES
('admin', '123456', '系统管理员', 'SUPER_ADMIN');

INSERT INTO ums_user_address (user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default) VALUES
(1, '示例用户', '13800000000', '北京市', '北京市', '朝阳区', '望京街道 1 号', 1);

INSERT INTO pms_category (name, parent_id, sort) VALUES
('精选推荐', 0, 1),
('手机数码', 0, 2),
('服饰箱包', 0, 3);

INSERT INTO pms_spu (name, category_id, main_image, album_images, description, status) VALUES
('MVP 示例商品', 1, 'https://via.placeholder.com/300x300.png?text=mallFei', '["https://via.placeholder.com/300x300.png?text=mallFei"]', '这是一个用于 MVP 演示的默认商品。', 1);

INSERT INTO pms_sku (spu_id, sku_code, sku_name, spec_json, sale_price, origin_price, stock, lock_stock, sales, status) VALUES
(1, 'SKU-0001', 'MVP 示例商品 默认规格', '{"颜色":"默认","容量":"标准"}', 99.90, 129.90, 100, 0, 10, 1);

INSERT INTO cart_item (user_id, sku_id, count, checked) VALUES
(1, 1, 1, 1);

INSERT INTO oms_order (order_no, user_id, order_status, total_amount, pay_amount, receiver_name, receiver_phone, receiver_address, remark) VALUES
('MVP202604180001', 1, 'UNPAID', 99.90, 99.90, '示例用户', '13800000000', '北京市朝阳区望京街道 1 号', '演示订单');

INSERT INTO oms_order_item (order_id, order_no, sku_id, spu_id, sku_name, sku_image, sale_price, quantity, total_amount) VALUES
(1, 'MVP202604180001', 1, 1, 'MVP 示例商品 默认规格', 'https://via.placeholder.com/300x300.png?text=mallFei', 99.90, 1, 99.90);

INSERT INTO pay_order (pay_no, order_no, user_id, pay_amount, pay_status, pay_channel, transaction_no) VALUES
('PAY202604180001', 'MVP202604180001', 1, 99.90, 'UNPAID', 'MOCK', '');
