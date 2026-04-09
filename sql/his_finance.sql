/*
 Navicat Premium Dump SQL

 Source Server         : 192.168.10.36_3306
 Source Server Type    : MySQL
 Source Server Version : 80034 (8.0.34)
 Source Host           : 192.168.10.36:3306
 Source Schema         : his_finance

 Target Server Type    : MySQL
 Target Server Version : 80034 (8.0.34)
 File Encoding         : 65001

 Date: 09/04/2026 10:45:48
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for drug
-- ----------------------------
DROP TABLE IF EXISTS `drug`;
CREATE TABLE `drug`  (
  `drug_id` int NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `drug_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品编码',
  `drug_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品名称',
  `spec` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格',
  `unit` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单位',
  `sale_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '销售价',
  `manufacturer` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生产厂家',
  `drug_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药品类型：西药/中成药/耗材',
  PRIMARY KEY (`drug_id`) USING BTREE,
  UNIQUE INDEX `drug_code`(`drug_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1027 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品信息字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for drug_in
-- ----------------------------
DROP TABLE IF EXISTS `drug_in`;
CREATE TABLE `drug_in`  (
  `in_id` int NOT NULL AUTO_INCREMENT COMMENT '入库ID',
  `drug_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药品编码',
  `in_num` int NULL DEFAULT NULL COMMENT '入库数量',
  `in_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '入库单价',
  `in_time` datetime NULL DEFAULT NULL COMMENT '入库时间',
  `operator` int NULL DEFAULT NULL COMMENT '操作人ID',
  PRIMARY KEY (`in_id`) USING BTREE,
  INDEX `drug_code`(`drug_code` ASC) USING BTREE,
  CONSTRAINT `drug_in_ibfk_1` FOREIGN KEY (`drug_code`) REFERENCES `drug` (`drug_code`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4096 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品入库记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for drug_out
-- ----------------------------
DROP TABLE IF EXISTS `drug_out`;
CREATE TABLE `drug_out`  (
  `out_id` int NOT NULL AUTO_INCREMENT COMMENT '出库ID',
  `drug_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药品编码',
  `out_num` int NULL DEFAULT NULL COMMENT '出库数量',
  `presc_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处方ID',
  `out_time` datetime NULL DEFAULT NULL COMMENT '出库时间',
  PRIMARY KEY (`out_id`) USING BTREE,
  INDEX `drug_code`(`drug_code` ASC) USING BTREE,
  CONSTRAINT `drug_out_ibfk_1` FOREIGN KEY (`drug_code`) REFERENCES `drug` (`drug_code`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4096 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品出库（发药）记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for drug_stock
-- ----------------------------
DROP TABLE IF EXISTS `drug_stock`;
CREATE TABLE `drug_stock`  (
  `stock_id` int NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  `drug_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药品编码',
  `stock_num` int NULL DEFAULT 0 COMMENT '当前库存',
  `update_time` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`stock_id`) USING BTREE,
  INDEX `drug_code`(`drug_code` ASC) USING BTREE,
  CONSTRAINT `drug_stock_ibfk_1` FOREIGN KEY (`drug_code`) REFERENCES `drug` (`drug_code`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1027 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品库存表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fee_bill
-- ----------------------------
DROP TABLE IF EXISTS `fee_bill`;
CREATE TABLE `fee_bill`  (
  `bill_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收费单号',
  `patient_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '患者ID',
  `total_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '应收总金额',
  `pay_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付方式',
  `bill_time` datetime NULL DEFAULT NULL COMMENT '收费时间',
  `operator` int NULL DEFAULT NULL COMMENT '收费员ID',
  PRIMARY KEY (`bill_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门诊收费主单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fee_bill_item
-- ----------------------------
DROP TABLE IF EXISTS `fee_bill_item`;
CREATE TABLE `fee_bill_item`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `bill_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收费单号',
  `item_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目类型：药品/检查/治疗',
  `item_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目ID',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目名称',
  `quantity` int NULL DEFAULT NULL COMMENT '数量',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  `total_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '小计',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `bill_id`(`bill_id` ASC) USING BTREE,
  CONSTRAINT `fee_bill_item_ibfk_1` FOREIGN KEY (`bill_id`) REFERENCES `fee_bill` (`bill_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8192 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收费明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fee_category
-- ----------------------------
DROP TABLE IF EXISTS `fee_category`;
CREATE TABLE `fee_category`  (
  `cate_id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `cate_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  PRIMARY KEY (`cate_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1028 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收费项目大类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fee_item
-- ----------------------------
DROP TABLE IF EXISTS `fee_item`;
CREATE TABLE `fee_item`  (
  `item_id` int NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `item_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目编码',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
  `cate_id` int NULL DEFAULT NULL COMMENT '所属分类ID',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  PRIMARY KEY (`item_id`) USING BTREE,
  UNIQUE INDEX `item_code`(`item_code` ASC) USING BTREE,
  INDEX `cate_id`(`cate_id` ASC) USING BTREE,
  CONSTRAINT `fee_item_ibfk_1` FOREIGN KEY (`cate_id`) REFERENCES `fee_category` (`cate_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4096 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收费项目明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay_type
-- ----------------------------
DROP TABLE IF EXISTS `pay_type`;
CREATE TABLE `pay_type`  (
  `pay_id` int NOT NULL AUTO_INCREMENT COMMENT '支付方式ID',
  `pay_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付方式名称',
  PRIMARY KEY (`pay_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1028 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付方式字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for refund
-- ----------------------------
DROP TABLE IF EXISTS `refund`;
CREATE TABLE `refund`  (
  `refund_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '退费单号',
  `bill_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原收费单号',
  `refund_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '退费金额',
  `refund_time` datetime NULL DEFAULT NULL COMMENT '退费时间',
  `reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退费原因',
  PRIMARY KEY (`refund_id`) USING BTREE,
  INDEX `bill_id`(`bill_id` ASC) USING BTREE,
  CONSTRAINT `refund_ibfk_1` FOREIGN KEY (`bill_id`) REFERENCES `fee_bill` (`bill_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '退费记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for settlement
-- ----------------------------
DROP TABLE IF EXISTS `settlement`;
CREATE TABLE `settlement`  (
  `settle_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '结算ID',
  `bill_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收费单号',
  `pay_id` int NULL DEFAULT NULL COMMENT '支付方式ID',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额',
  `settle_time` datetime NULL DEFAULT NULL COMMENT '结算时间',
  PRIMARY KEY (`settle_id`) USING BTREE,
  INDEX `bill_id`(`bill_id` ASC) USING BTREE,
  INDEX `pay_id`(`pay_id` ASC) USING BTREE,
  CONSTRAINT `settlement_ibfk_1` FOREIGN KEY (`bill_id`) REFERENCES `fee_bill` (`bill_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `settlement_ibfk_2` FOREIGN KEY (`pay_id`) REFERENCES `pay_type` (`pay_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收费结算记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
