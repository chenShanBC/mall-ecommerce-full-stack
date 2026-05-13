DROP DATABASE IF EXISTS mall_fei;
CREATE DATABASE mall_fei DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE mall_fei;

CREATE TABLE ums_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '鐢ㄦ埛ID',
    mobile VARCHAR(20) NOT NULL COMMENT '鎵嬫満鍙?,
    password_hash VARCHAR(128) NOT NULL COMMENT '瀵嗙爜鍝堝笇',
    nickname VARCHAR(50) NOT NULL COMMENT '鏄电О',
    avatar_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '澶村儚鍦板潃',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '璐︽埛鐘舵€?,
    version INT NOT NULL DEFAULT 0 COMMENT '涔愯閿佺増鏈彿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    UNIQUE KEY uk_ums_user_mobile (mobile)
) COMMENT='鐢ㄦ埛琛?;

CREATE TABLE ums_admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '绠＄悊鍛業D',
    username VARCHAR(50) NOT NULL COMMENT '绠＄悊鍛樿处鍙?,
    password_hash VARCHAR(128) NOT NULL COMMENT '瀵嗙爜鍝堝笇',
    nickname VARCHAR(50) NOT NULL COMMENT '绠＄悊鍛樻樀绉?,
    role_code VARCHAR(50) NOT NULL DEFAULT 'SUPER_ADMIN' COMMENT '瑙掕壊缂栫爜',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '璐︽埛鐘舵€?,
    version INT NOT NULL DEFAULT 0 COMMENT '涔愯閿佺増鏈彿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    UNIQUE KEY uk_ums_admin_username (username)
) COMMENT='绠＄悊鍛樿〃';

CREATE TABLE ums_user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '鍦板潃ID',
    user_id BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '鏀朵欢浜哄鍚?,
    receiver_phone VARCHAR(20) NOT NULL COMMENT '鏀朵欢浜烘墜鏈哄彿',
    province_code VARCHAR(20) DEFAULT '' COMMENT '鐪佺紪鐮?,
    province_name VARCHAR(50) NOT NULL COMMENT '鐪佸悕绉?,
    city_code VARCHAR(20) DEFAULT '' COMMENT '甯傜紪鐮?,
    city_name VARCHAR(50) NOT NULL COMMENT '甯傚悕绉?,
    district_code VARCHAR(20) DEFAULT '' COMMENT '鍖虹紪鐮?,
    district_name VARCHAR(50) NOT NULL COMMENT '鍖哄悕绉?,
    detail_address VARCHAR(255) NOT NULL COMMENT '璇︾粏鍦板潃',
    postal_code VARCHAR(20) NOT NULL DEFAULT '' COMMENT '閭紪',
    is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '鏄惁榛樿鍦板潃',
    version INT NOT NULL DEFAULT 0 COMMENT '涔愯閿佺増鏈彿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    KEY idx_ums_user_address_user_id (user_id)
) COMMENT='鐢ㄦ埛鍦板潃琛?;

CREATE TABLE fms_file_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    biz_type VARCHAR(50) NOT NULL COMMENT '业务类型',
    storage_type VARCHAR(20) NOT NULL COMMENT '存储类型',
    file_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    original_file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    relative_path VARCHAR(255) NOT NULL COMMENT '相对路径',
    access_url VARCHAR(255) NOT NULL COMMENT '访问地址',
    content_type VARCHAR(100) NOT NULL DEFAULT '' COMMENT '文件内容类型',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小字节',
    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    uploader_type VARCHAR(20) NOT NULL COMMENT '上传人类型',
    uploader_account VARCHAR(100) NOT NULL DEFAULT '' COMMENT '上传人账号',
    uploader_nickname VARCHAR(100) NOT NULL DEFAULT '' COMMENT '上传人昵称',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '文件状态',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    KEY idx_fms_file_record_biz_type (biz_type),
    KEY idx_fms_file_record_uploader_id (uploader_id)
) COMMENT='文件记录表';
CREATE TABLE pms_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '绫荤洰ID',
    name VARCHAR(50) NOT NULL COMMENT '绫荤洰鍚嶇О',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '鐖剁被鐩甀D',
    level INT NOT NULL DEFAULT 1 COMMENT '灞傜骇',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '鎺掑簭鍊?,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '绫荤洰鐘舵€?,
    version INT NOT NULL DEFAULT 0 COMMENT '涔愯閿佺増鏈彿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    KEY idx_pms_category_parent_id (parent_id)
) COMMENT='鍟嗗搧绫荤洰琛?;

CREATE TABLE pms_spu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'SPU ID',
    name VARCHAR(100) NOT NULL COMMENT '鍟嗗搧鍚嶇О',
    category_id BIGINT NOT NULL COMMENT '鎵€灞炵被鐩甀D',
    main_image_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '涓诲浘鍦板潃',
    album_images_json JSON NULL COMMENT '鍥鹃泦JSON',
    description TEXT NULL COMMENT '鍟嗗搧鎻忚堪',
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE' COMMENT '鍟嗗搧鐘舵€?,
    version INT NOT NULL DEFAULT 0 COMMENT '涔愯閿佺増鏈彿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    KEY idx_pms_spu_category_id (category_id)
) COMMENT='鍟嗗搧SPU琛?;

CREATE TABLE pms_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'SKU ID',
    spu_id BIGINT NOT NULL COMMENT '鎵€灞濻PU ID',
    sku_code VARCHAR(50) NOT NULL COMMENT 'SKU缂栫爜',
    sku_name VARCHAR(100) NOT NULL COMMENT 'SKU鍚嶇О',
    spec_json JSON NULL COMMENT '瑙勬牸JSON',
    sale_price_cent BIGINT NOT NULL COMMENT '閿€鍞环鍒?,
    origin_price_cent BIGINT NOT NULL COMMENT '鍘熶环鍒?,
    sales_count INT NOT NULL DEFAULT 0 COMMENT '閿€閲?,
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE' COMMENT 'SKU鐘舵€?,
    version INT NOT NULL DEFAULT 0 COMMENT '涔愯閿佺増鏈彿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    UNIQUE KEY uk_pms_sku_code (sku_code),
    KEY idx_pms_sku_spu_id (spu_id)
) COMMENT='鍟嗗搧SKU琛?;

CREATE TABLE ims_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '搴撳瓨涓婚敭',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '鎬诲簱瀛?,
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '宸查攣搴撳瓨',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '鍙敭搴撳瓨',
    version INT NOT NULL DEFAULT 0 COMMENT '涔愯閿佺増鏈彿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_ims_stock_sku_id (sku_id)
) COMMENT='搴撳瓨琛?;

CREATE TABLE ims_stock_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '搴撳瓨閿佽褰旾D',
    lock_no VARCHAR(64) NOT NULL COMMENT '搴撳瓨閿佸崟鍙?,
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    business_type VARCHAR(32) NOT NULL COMMENT '涓氬姟绫诲瀷',
    business_no VARCHAR(64) NOT NULL COMMENT '涓氬姟鍗曞彿',
    quantity INT NOT NULL COMMENT '閿佸畾鏁伴噺',
    status VARCHAR(20) NOT NULL COMMENT '閿佺姸鎬?,
    lock_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '閿佸畾鏃堕棿',
    release_time DATETIME NULL COMMENT '閲婃斁鏃堕棿',
    deduct_time DATETIME NULL COMMENT '鎵ｅ噺鏃堕棿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_ims_stock_lock_no (lock_no),
    UNIQUE KEY uk_ims_stock_lock_business (business_type, business_no, sku_id)
) COMMENT='搴撳瓨閿佽褰曡〃';

CREATE TABLE cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '璐墿杞﹂」ID',
    user_id BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '璐拱鏁伴噺',
    checked TINYINT(1) NOT NULL DEFAULT 1 COMMENT '鏄惁鍕鹃€?,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    UNIQUE KEY uk_cart_item_user_sku (user_id, sku_id)
) COMMENT='璐墿杞﹂」琛?;

CREATE TABLE oms_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '璁㈠崟ID',
    order_no VARCHAR(64) NOT NULL COMMENT '璁㈠崟鍙?,
    user_id BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    order_status VARCHAR(32) NOT NULL COMMENT '璁㈠崟鐘舵€?,
    total_amount_cent BIGINT NOT NULL COMMENT '璁㈠崟鎬婚噾棰濆垎',
    pay_amount_cent BIGINT NOT NULL COMMENT '瀹炰粯閲戦鍒?,
    freight_amount_cent BIGINT NOT NULL DEFAULT 0 COMMENT '杩愯垂鍒?,
    discount_amount_cent BIGINT NOT NULL DEFAULT 0 COMMENT '浼樻儬閲戦鍒?,
    receiver_name VARCHAR(50) NOT NULL COMMENT '鏀惰揣浜?,
    receiver_phone VARCHAR(20) NOT NULL COMMENT '鏀惰揣鐢佃瘽',
    receiver_province_name VARCHAR(50) NOT NULL COMMENT '鐪佸悕绉?,
    receiver_city_name VARCHAR(50) NOT NULL COMMENT '甯傚悕绉?,
    receiver_district_name VARCHAR(50) NOT NULL COMMENT '鍖哄悕绉?,
    receiver_detail_address VARCHAR(255) NOT NULL COMMENT '璇︾粏鍦板潃',
    remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '璁㈠崟澶囨敞',
    pay_type VARCHAR(30) NOT NULL DEFAULT 'MOCK' COMMENT '鏀粯绫诲瀷',
    paid_at DATETIME NULL COMMENT '鏀粯鏃堕棿',
    cancelled_at DATETIME NULL COMMENT '鍙栨秷鏃堕棿',
    shipped_at DATETIME NULL COMMENT '鍙戣揣鏃堕棿',
    completed_at DATETIME NULL COMMENT '瀹屾垚鏃堕棿',
    version INT NOT NULL DEFAULT 0 COMMENT '涔愯閿佺増鏈彿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    UNIQUE KEY uk_oms_order_order_no (order_no),
    KEY idx_oms_order_user_id (user_id)
) COMMENT='璁㈠崟琛?;

CREATE TABLE oms_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '璁㈠崟椤笽D',
    order_id BIGINT NOT NULL COMMENT '璁㈠崟ID',
    order_no VARCHAR(64) NOT NULL COMMENT '璁㈠崟鍙?,
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    spu_id BIGINT NOT NULL COMMENT 'SPU ID',
    sku_name VARCHAR(100) NOT NULL COMMENT 'SKU鍚嶇О蹇収',
    sku_image_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'SKU鍥剧墖蹇収',
    sale_price_cent BIGINT NOT NULL COMMENT '鎴愪氦鍗曚环鍒?,
    quantity INT NOT NULL COMMENT '璐拱鏁伴噺',
    total_amount_cent BIGINT NOT NULL COMMENT '鎬婚噾棰濆垎',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    KEY idx_oms_order_item_order_id (order_id)
) COMMENT='璁㈠崟椤硅〃';

CREATE TABLE pay_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '鏀粯鍗旾D',
    pay_order_no VARCHAR(64) NOT NULL COMMENT '鏀粯鍗曞彿',
    order_no VARCHAR(64) NOT NULL COMMENT '涓氬姟璁㈠崟鍙?,
    user_id BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    pay_amount_cent BIGINT NOT NULL COMMENT '鏀粯閲戦鍒?,
    pay_status VARCHAR(32) NOT NULL COMMENT '鏀粯鐘舵€?,
    pay_channel VARCHAR(30) NOT NULL DEFAULT 'MOCK' COMMENT '鏀粯娓犻亾',
    transaction_no VARCHAR(100) NOT NULL DEFAULT '' COMMENT '涓夋柟娴佹按鍙?,
    callback_payload TEXT NULL COMMENT '鍥炶皟鎶ユ枃',
    notify_time DATETIME NULL COMMENT '閫氱煡鏃堕棿',
    paid_at DATETIME NULL COMMENT '鏀粯瀹屾垚鏃堕棿',
    idempotent_key VARCHAR(64) NOT NULL DEFAULT '' COMMENT '骞傜瓑閿?,
    version INT NOT NULL DEFAULT 0 COMMENT '涔愯閿佺増鏈彿',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    deleted_at DATETIME NULL COMMENT '鍒犻櫎鏃堕棿',
    UNIQUE KEY uk_pay_order_pay_order_no (pay_order_no),
    UNIQUE KEY uk_pay_order_idempotent_key (idempotent_key),
    KEY idx_pay_order_order_no (order_no)
) COMMENT='鏀粯鍗曡〃';

INSERT INTO ums_user (mobile, password_hash, nickname, avatar_url, status) VALUES
('13800000000', '123456', '绀轰緥鐢ㄦ埛', 'https://via.placeholder.com/120x120.png?text=user', 'ENABLED');

INSERT INTO ums_admin (username, password_hash, nickname, role_code, status) VALUES
('admin', '123456', '绯荤粺绠＄悊鍛?, 'SUPER_ADMIN', 'ENABLED');

INSERT INTO ums_user_address (user_id, receiver_name, receiver_phone, province_name, city_name, district_name, detail_address, is_default) VALUES
(1, '绀轰緥鐢ㄦ埛', '13800000000', '鍖椾含甯?, '鍖椾含甯?, '鏈濋槼鍖?, '鏈涗含琛楅亾 1 鍙?, 1);

INSERT INTO pms_category (name, parent_id, level, sort_order, status) VALUES
('绮鹃€夋帹鑽?, 0, 1, 1, 'ENABLED'),
('鎵嬫満鏁扮爜', 0, 1, 2, 'ENABLED'),
('鏈嶉グ绠卞寘', 0, 1, 3, 'ENABLED'),
('鎵嬫満', 2, 2, 1, 'ENABLED');

INSERT INTO pms_spu (name, category_id, main_image_url, album_images_json, description, status) VALUES
('MVP 绀轰緥鍟嗗搧', 1, 'https://via.placeholder.com/300x300.png?text=mallFei', JSON_ARRAY('https://via.placeholder.com/300x300.png?text=mallFei'), '杩欐槸涓€涓敤浜?MVP 婕旂ず鐨勯粯璁ゅ晢鍝併€?, 'ONLINE'),
('绗簩涓ず渚嬪晢鍝?, 4, 'https://via.placeholder.com/300x300.png?text=phone', JSON_ARRAY('https://via.placeholder.com/300x300.png?text=phone'), '杩欐槸涓€涓敤浜庡晢鍝佸垪琛ㄥ睍绀虹殑绗簩涓晢鍝併€?, 'ONLINE');

INSERT INTO pms_sku (spu_id, sku_code, sku_name, spec_json, sale_price_cent, origin_price_cent, sales_count, status) VALUES
(1, 'SKU-0001', 'MVP 绀轰緥鍟嗗搧 榛樿瑙勬牸', JSON_OBJECT('棰滆壊', '榛樿', '瀹归噺', '鏍囧噯'), 9990, 12990, 10, 'ONLINE'),
(1, 'SKU-0001-PLUS', 'MVP 绀轰緥鍟嗗搧 鍗囩骇瑙勬牸', JSON_OBJECT('棰滆壊', '榛樿', '瀹归噺', '鍗囩骇鐗?), 12990, 15990, 1, 'ONLINE'),
(2, 'SKU-0002', '绗簩涓ず渚嬪晢鍝?榛戣壊 256G', JSON_OBJECT('棰滆壊', '榛戣壊', '瀹归噺', '256G'), 499900, 569900, 3, 'ONLINE');

INSERT INTO ims_stock (sku_id, total_stock, locked_stock, available_stock) VALUES
(1, 100, 0, 100),
(2, 50, 0, 50),
(3, 20, 0, 20);

INSERT INTO cart_item (user_id, sku_id, quantity, checked) VALUES
(1, 1, 1, 1);

INSERT INTO oms_order (order_no, user_id, order_status, total_amount_cent, pay_amount_cent, receiver_name, receiver_phone, receiver_province_name, receiver_city_name, receiver_district_name, receiver_detail_address, remark) VALUES
('MVP202604180001', 1, 'PENDING_PAYMENT', 9990, 9990, '绀轰緥鐢ㄦ埛', '13800000000', '鍖椾含甯?, '鍖椾含甯?, '鏈濋槼鍖?, '鏈涗含琛楅亾 1 鍙?, '婕旂ず璁㈠崟');

INSERT INTO oms_order_item (order_id, order_no, sku_id, spu_id, sku_name, sku_image_url, sale_price_cent, quantity, total_amount_cent) VALUES
(1, 'MVP202604180001', 1, 1, 'MVP 绀轰緥鍟嗗搧 榛樿瑙勬牸', 'https://via.placeholder.com/300x300.png?text=mallFei', 9990, 1, 9990);

INSERT INTO pay_order (pay_order_no, order_no, user_id, pay_amount_cent, pay_status, pay_channel, transaction_no, idempotent_key) VALUES
('PAY202604180001', 'MVP202604180001', 1, 9990, 'PENDING', 'MOCK', '', 'PAY:MOCK:MVP202604180001');

