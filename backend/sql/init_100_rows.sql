DROP DATABASE IF EXISTS mall_fei;
CREATE DATABASE mall_fei DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE mall_fei;

CREATE TABLE ums_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    mobile VARCHAR(20) NOT NULL COMMENT '手机号',
    password_hash VARCHAR(128) NOT NULL COMMENT '密码哈希',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像地址',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '账户状态',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    UNIQUE KEY uk_ums_user_mobile (mobile)
) COMMENT='用户表';

CREATE TABLE ums_admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    username VARCHAR(50) NOT NULL COMMENT '管理员账号',
    password_hash VARCHAR(128) NOT NULL COMMENT '密码哈希',
    nickname VARCHAR(50) NOT NULL COMMENT '管理员昵称',
    role_code VARCHAR(50) NOT NULL DEFAULT 'SUPER_ADMIN' COMMENT '角色编码',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '账户状态',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    UNIQUE KEY uk_ums_admin_username (username)
) COMMENT='管理员表';

CREATE TABLE ums_user_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地址ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收件人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收件人手机号',
    province_code VARCHAR(20) DEFAULT '' COMMENT '省编码',
    province_name VARCHAR(50) NOT NULL COMMENT '省名称',
    city_code VARCHAR(20) DEFAULT '' COMMENT '市编码',
    city_name VARCHAR(50) NOT NULL COMMENT '市名称',
    district_code VARCHAR(20) DEFAULT '' COMMENT '区编码',
    district_name VARCHAR(50) NOT NULL COMMENT '区名称',
    detail_address VARCHAR(255) NOT NULL COMMENT '详细地址',
    postal_code VARCHAR(20) NOT NULL DEFAULT '' COMMENT '邮编',
    is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    KEY idx_ums_user_address_user_id (user_id)
) COMMENT='用户地址表';

CREATE TABLE pms_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '类目ID',
    name VARCHAR(50) NOT NULL COMMENT '类目名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父类目ID',
    level INT NOT NULL DEFAULT 1 COMMENT '层级',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '类目状态',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    KEY idx_pms_category_parent_id (parent_id)
) COMMENT='商品类目表';

CREATE TABLE pms_spu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'SPU ID',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category_id BIGINT NOT NULL COMMENT '所属类目ID',
    main_image_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '主图地址',
    album_images_json JSON NULL COMMENT '图集JSON',
    description TEXT NULL COMMENT '商品描述',
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE' COMMENT '商品状态',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    KEY idx_pms_spu_category_id (category_id)
) COMMENT='商品SPU表';

CREATE TABLE pms_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'SKU ID',
    spu_id BIGINT NOT NULL COMMENT '所属SPU ID',
    sku_code VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    sku_name VARCHAR(100) NOT NULL COMMENT 'SKU名称',
    spec_json JSON NULL COMMENT '规格JSON',
    sale_price_cent BIGINT NOT NULL COMMENT '销售价分',
    origin_price_cent BIGINT NOT NULL COMMENT '原价分',
    sales_count INT NOT NULL DEFAULT 0 COMMENT '销量',
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE' COMMENT 'SKU状态',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    UNIQUE KEY uk_pms_sku_code (sku_code),
    KEY idx_pms_sku_spu_id (spu_id)
) COMMENT='商品SKU表';

CREATE TABLE ims_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存主键',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '已锁库存',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可售库存',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_ims_stock_sku_id (sku_id)
) COMMENT='库存表';

CREATE TABLE ims_stock_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '库存锁记录ID',
    lock_no VARCHAR(64) NOT NULL COMMENT '库存锁单号',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    business_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    business_no VARCHAR(64) NOT NULL COMMENT '业务单号',
    quantity INT NOT NULL COMMENT '锁定数量',
    status VARCHAR(20) NOT NULL COMMENT '锁状态',
    lock_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '锁定时间',
    release_time DATETIME NULL COMMENT '释放时间',
    deduct_time DATETIME NULL COMMENT '扣减时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_ims_stock_lock_no (lock_no),
    UNIQUE KEY uk_ims_stock_lock_business (business_type, business_no, sku_id)
) COMMENT='库存锁记录表';

CREATE TABLE cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '购物车项ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    checked TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否勾选',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    UNIQUE KEY uk_cart_item_user_sku (user_id, sku_id)
) COMMENT='购物车项表';

CREATE TABLE oms_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态',
    total_amount_cent BIGINT NOT NULL COMMENT '订单总金额分',
    pay_amount_cent BIGINT NOT NULL COMMENT '实付金额分',
    freight_amount_cent BIGINT NOT NULL DEFAULT 0 COMMENT '运费分',
    discount_amount_cent BIGINT NOT NULL DEFAULT 0 COMMENT '优惠金额分',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货电话',
    receiver_province_name VARCHAR(50) NOT NULL COMMENT '省名称',
    receiver_city_name VARCHAR(50) NOT NULL COMMENT '市名称',
    receiver_district_name VARCHAR(50) NOT NULL COMMENT '区名称',
    receiver_detail_address VARCHAR(255) NOT NULL COMMENT '详细地址',
    remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '订单备注',
    pay_type VARCHAR(30) NOT NULL DEFAULT 'MOCK' COMMENT '支付类型',
    paid_at DATETIME NULL COMMENT '支付时间',
    cancelled_at DATETIME NULL COMMENT '取消时间',
    shipped_at DATETIME NULL COMMENT '发货时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    UNIQUE KEY uk_oms_order_order_no (order_no),
    KEY idx_oms_order_user_id (user_id)
) COMMENT='订单表';

CREATE TABLE oms_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单项ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    spu_id BIGINT NOT NULL COMMENT 'SPU ID',
    sku_name VARCHAR(100) NOT NULL COMMENT 'SKU名称快照',
    sku_image_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'SKU图片快照',
    sale_price_cent BIGINT NOT NULL COMMENT '成交单价分',
    quantity INT NOT NULL COMMENT '购买数量',
    total_amount_cent BIGINT NOT NULL COMMENT '总金额分',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    KEY idx_oms_order_item_order_id (order_id)
) COMMENT='订单项表';

CREATE TABLE pay_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付单ID',
    pay_order_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    pay_amount_cent BIGINT NOT NULL COMMENT '支付金额分',
    pay_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    pay_channel VARCHAR(30) NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道',
    transaction_no VARCHAR(100) NOT NULL DEFAULT '' COMMENT '三方流水号',
    callback_payload TEXT NULL COMMENT '回调报文',
    notify_time DATETIME NULL COMMENT '通知时间',
    paid_at DATETIME NULL COMMENT '支付完成时间',
    idempotent_key VARCHAR(64) NOT NULL DEFAULT '' COMMENT '幂等键',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME NULL COMMENT '删除时间',
    UNIQUE KEY uk_pay_order_pay_order_no (pay_order_no),
    UNIQUE KEY uk_pay_order_idempotent_key (idempotent_key),
    KEY idx_pay_order_order_no (order_no)
) COMMENT='支付单表';


DELIMITER $$
CREATE PROCEDURE seed_mall_fei()
BEGIN
DECLARE i INT DEFAULT 1;DECLARE q INT;DECLARE price BIGINT;DECLARE freight BIGINT;DECLARE disc BIGINT;DECLARE total BIGINT;DECLARE pay BIGINT;DECLARE os VARCHAR(32);DECLARE ps VARCHAR(32);DECLARE prov VARCHAR(20);DECLARE city VARCHAR(20);DECLARE dist VARCHAR(20);DECLARE cat VARCHAR(20);DECLARE brand VARCHAR(20);DECLARE goods VARCHAR(30);
WHILE i<=100 DO
SET q=i%3+1;SET price=CASE i%8 WHEN 0 THEN 69900 WHEN 1 THEN 15900 WHEN 2 THEN 299900 WHEN 3 THEN 8900 WHEN 4 THEN 45900 WHEN 5 THEN 129900 WHEN 6 THEN 39900 ELSE 23900 END+i*37;SET freight=IF(i%7=0,800,0);SET disc=IF(i%9=0,300,0);SET total=price*q+freight;SET pay=total-disc;SET os=CASE WHEN i<=18 THEN 'PENDING_PAYMENT' WHEN i<=43 THEN 'PAID' WHEN i<=66 THEN 'SHIPPED' WHEN i<=88 THEN 'COMPLETED' ELSE 'CANCELLED' END;SET ps=CASE WHEN i<=18 THEN 'PENDING' WHEN i<=88 THEN 'SUCCESS' ELSE 'CLOSED' END;SET prov=ELT(i%8+1,'北京市','上海市','广东省','浙江省','江苏省','四川省','湖北省','福建省');SET city=ELT(i%8+1,'北京市','上海市','深圳市','杭州市','南京市','成都市','武汉市','厦门市');SET dist=ELT(i%8+1,'朝阳区','浦东新区','南山区','西湖区','玄武区','武侯区','江汉区','思明区');SET cat=ELT(i%10+1,'手机通讯','电脑办公','家用电器','个护清洁','食品生鲜','运动户外','服饰内衣','母婴用品','图书文创','家居厨具');SET brand=ELT(i%12+1,'松禾','星麦','鹿岛','青橙','北庭','云岚','山也','沐白','岚森','初合','鲸选','有栖');SET goods=ELT(i%12+1,'无线耳机','保温杯','空气炸锅','护眼台灯','跑步鞋','双肩包','电动牙刷','乳胶枕','牛奶礼盒','机械键盘','防晒霜','行李箱');
INSERT INTO ums_user VALUES(i,CONCAT('138',LPAD(i-1,8,'0')),'123456',CONCAT(ELT(i%10+1,'林','陈','王','李','赵','周','吴','郑','许','何'),ELT(i%10+1,'一诺','子涵','明远','若溪','嘉言','思齐','雨桐','景行','书瑶','知夏')),CONCAT('https://img.mallfei.local/avatar/',i,'.png'),IF(i%33=0,'DISABLED','ENABLED'),0,DATE_ADD('2025-10-01 09:00:00',INTERVAL i DAY),DATE_ADD('2025-10-01 09:00:00',INTERVAL i DAY),NULL);
INSERT INTO ums_admin VALUES(i,IF(i=1,'admin',CONCAT('ops',LPAD(i,3,'0'))),'123456',CONCAT(ELT(i%6+1,'运营','客服','财务','仓储','采购','店长'),LPAD(i,2,'0')),IF(i<=6,'SUPER_ADMIN',ELT(i%4+1,'PRODUCT','ORDER','CUSTOMER','FINANCE')),IF(i%40=0,'DISABLED','ENABLED'),0,NOW(),NOW(),NULL);
INSERT INTO ums_user_address VALUES(i,i,CONCAT(ELT(i%10+1,'林','陈','王','李','赵','周','吴','郑','许','何'),ELT(i%8+1,'先生','女士','老师','同学','经理','主管','小姐','师傅')),CONCAT('139',LPAD(i-1,8,'0')),CONCAT(LPAD(i%34+1,2,'0'),'0000'),prov,CONCAT(LPAD(i%34+1,2,'0'),'0100'),city,CONCAT(LPAD(i%34+1,2,'0'),'0105'),dist,CONCAT(ELT(i%8+1,'望京','张江','科技园','文三路','新街口','天府大道','江汉路','环岛路'),' ',100+i,' 号 ',ELT(i%5+1,'1栋','2栋','A座','B座','南门')),LPAD(100000+i,6,'0'),1,0,NOW(),NOW(),NULL);
INSERT INTO pms_category VALUES(i,IF(i<=10,cat,CONCAT(ELT(i%10+1,'电脑办公','家用电器','个护清洁','食品生鲜','运动户外','服饰内衣','母婴用品','图书文创','家居厨具','手机通讯'),'-',ELT(MOD(FLOOR((i-11)/10),5)+1,'热卖','新品','精选','礼赠','清仓'))),IF(i<=10,0,i%10+1),IF(i<=10,1,2),i,IF(i%25=0,'DISABLED','ENABLED'),0,NOW(),NOW(),NULL);
INSERT INTO pms_spu VALUES(i,CONCAT(brand,' ',goods,IF(i%4=0,' 家庭装',IF(i%4=1,' 轻享版',IF(i%4=2,' 标准款',' Pro')))),i,CONCAT('https://img.mallfei.local/product/spu/',i,'.jpg'),JSON_ARRAY(CONCAT('https://img.mallfei.local/product/spu/',i,'-1.jpg'),CONCAT('https://img.mallfei.local/product/spu/',i,'-2.jpg')),CONCAT('精选',brand,goods,'，适合日常使用，品质稳定。'),IF(i%17=0,'OFFLINE','ONLINE'),0,NOW(),NOW(),NULL);
INSERT INTO pms_sku VALUES(i,i,CONCAT('MF',DATE_FORMAT(NOW(),'%y%m'),LPAD(i,5,'0')),CONCAT(brand,' ',goods,' ',ELT(i%6+1,'曜石黑','珍珠白','海盐蓝','松雾绿','暖沙色','樱花粉')),JSON_OBJECT('颜色',ELT(i%6+1,'曜石黑','珍珠白','海盐蓝','松雾绿','暖沙色','樱花粉'),'规格',ELT(i%5+1,'基础版','升级版','家庭装','便携款','礼盒装')),price,price+IF(i%4=0,5000,3000),i*7%380,IF(i%17=0,'OFFLINE','ONLINE'),0,NOW(),NOW(),NULL);
INSERT INTO ims_stock VALUES(i,i,120+i%80,IF(i<=25,i%6,0),120+i%80-IF(i<=25,i%6,0),0,NOW(),NOW());
INSERT INTO ims_stock_lock VALUES(i,CONCAT('SL',DATE_FORMAT(NOW(),'%Y%m%d'),LPAD(i,6,'0')),i,IF(i%2=0,'ORDER_CREATE','PAY_CONFIRM'),CONCAT('BO',DATE_FORMAT(NOW(),'%Y%m%d'),LPAD(i,6,'0')),q,CASE WHEN i<=34 THEN 'LOCKED' WHEN i<=67 THEN 'RELEASED' ELSE 'DEDUCTED' END,DATE_ADD('2026-01-10 10:00:00',INTERVAL i MINUTE),IF(i>34 AND i<=67,DATE_ADD('2026-01-10 11:00:00',INTERVAL i MINUTE),NULL),IF(i>67,DATE_ADD('2026-01-10 12:00:00',INTERVAL i MINUTE),NULL),NOW(),NOW());
INSERT INTO cart_item VALUES(i,i,i,q,IF(i%5=0,0,1),NOW(),NOW(),NULL);
INSERT INTO oms_order VALUES(i,CONCAT('MF',DATE_FORMAT(DATE_ADD('2026-01-01',INTERVAL i DAY),'%Y%m%d'),LPAD(i,6,'0')),i,os,total,pay,freight,disc,CONCAT(ELT(i%10+1,'林','陈','王','李','赵','周','吴','郑','许','何'),ELT(i%8+1,'先生','女士','老师','同学','经理','主管','小姐','师傅')),CONCAT('139',LPAD(i-1,8,'0')),prov,city,dist,CONCAT(ELT(i%8+1,'望京','张江','科技园','文三路','新街口','天府大道','江汉路','环岛路'),' ',100+i,' 号'),IF(i%11=0,'工作日白天配送',''),'MOCK',IF(i>18 AND i<=88,NOW(),NULL),IF(i>88,NOW(),NULL),IF(i>43 AND i<=88,NOW(),NULL),IF(i>66 AND i<=88,NOW(),NULL),0,NOW(),NOW(),NULL);
INSERT INTO oms_order_item VALUES(i,i,CONCAT('MF',DATE_FORMAT(DATE_ADD('2026-01-01',INTERVAL i DAY),'%Y%m%d'),LPAD(i,6,'0')),i,i,CONCAT(brand,' ',goods),CONCAT('https://img.mallfei.local/product/sku/',i,'.jpg'),price,q,price*q,NOW(),NOW(),NULL);
INSERT INTO pay_order VALUES(i,CONCAT('P',DATE_FORMAT(DATE_ADD('2026-01-01',INTERVAL i DAY),'%Y%m%d'),LPAD(i,6,'0')),CONCAT('MF',DATE_FORMAT(DATE_ADD('2026-01-01',INTERVAL i DAY),'%Y%m%d'),LPAD(i,6,'0')),i,pay,ps,ELT(i%3+1,'MOCK','WECHAT','ALIPAY'),IF(i<=18 OR i>88,'',CONCAT('TN',DATE_FORMAT(NOW(),'%Y%m%d'),LPAD(i,8,'0'))),IF(i<=18 OR i>88,NULL,JSON_OBJECT('channel',ELT(i%3+1,'MOCK','WECHAT','ALIPAY'),'status','SUCCESS')),IF(i<=18 OR i>88,NULL,NOW()),IF(i<=18 OR i>88,NULL,NOW()),CONCAT('PAY:',i,':',DATE_FORMAT(NOW(),'%Y%m%d')),0,NOW(),NOW(),NULL);
SET i=i+1;
END WHILE;
END$$
DELIMITER ;
CALL seed_mall_fei();
DROP PROCEDURE seed_mall_fei;
SELECT 'ums_user' table_name,COUNT(*) row_count FROM ums_user UNION ALL SELECT 'ums_admin',COUNT(*) FROM ums_admin UNION ALL SELECT 'ums_user_address',COUNT(*) FROM ums_user_address UNION ALL SELECT 'pms_category',COUNT(*) FROM pms_category UNION ALL SELECT 'pms_spu',COUNT(*) FROM pms_spu UNION ALL SELECT 'pms_sku',COUNT(*) FROM pms_sku UNION ALL SELECT 'ims_stock',COUNT(*) FROM ims_stock UNION ALL SELECT 'ims_stock_lock',COUNT(*) FROM ims_stock_lock UNION ALL SELECT 'cart_item',COUNT(*) FROM cart_item UNION ALL SELECT 'oms_order',COUNT(*) FROM oms_order UNION ALL SELECT 'oms_order_item',COUNT(*) FROM oms_order_item UNION ALL SELECT 'pay_order',COUNT(*) FROM pay_order;
