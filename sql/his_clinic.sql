/*
 Navicat Premium Dump SQL

 Source Server         : 192.168.10.36_3306
 Source Server Type    : MySQL
 Source Server Version : 80034 (8.0.34)
 Source Host           : 192.168.10.36:3306
 Source Schema         : his_clinic

 Target Server Type    : MySQL
 Target Server Version : 80034 (8.0.34)
 File Encoding         : 65001

 Date: 09/04/2026 10:45:32
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dept
-- ----------------------------
DROP TABLE IF EXISTS `dept`;
CREATE TABLE `dept`  (
  `dept_id` int NOT NULL AUTO_INCREMENT COMMENT '科室ID',
  `dept_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科室编码',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科室名称',
  `dept_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '科室类型：门诊/住院/医技',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '科室电话',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标识 0正常 1删除',
  PRIMARY KEY (`dept_id`) USING BTREE,
  UNIQUE INDEX `dept_code`(`dept_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1029 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '科室信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for employee
-- ----------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee`  (
  `emp_id` int NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `emp_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '员工编码',
  `emp_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '员工姓名',
  `dept_id` int NULL DEFAULT NULL COMMENT '所属科室ID',
  `job_title` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职称',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别 1男 2女',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `is_doctor` tinyint NULL DEFAULT 1 COMMENT '是否医生 1是 0否',
  PRIMARY KEY (`emp_id`) USING BTREE,
  UNIQUE INDEX `emp_code`(`emp_code` ASC) USING BTREE,
  INDEX `dept_id`(`dept_id` ASC) USING BTREE,
  CONSTRAINT `employee_ibfk_1` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`dept_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2053 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '员工信息表（医生/护士/医技）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_apply
-- ----------------------------
DROP TABLE IF EXISTS `exam_apply`;
CREATE TABLE `exam_apply`  (
  `apply_id` int NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `register_id` int NULL DEFAULT NULL COMMENT '挂号ID',
  `patient_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '患者ID',
  `doctor_id` int NULL DEFAULT NULL COMMENT '开单医生ID',
  `item_id` int NULL DEFAULT NULL COMMENT '检查项目ID',
  `apply_time` datetime NULL DEFAULT NULL COMMENT '申请时间',
  `pay_status` tinyint NULL DEFAULT 0 COMMENT '支付状态',
  PRIMARY KEY (`apply_id`) USING BTREE,
  INDEX `register_id`(`register_id` ASC) USING BTREE,
  INDEX `patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `item_id`(`item_id` ASC) USING BTREE,
  CONSTRAINT `exam_apply_ibfk_1` FOREIGN KEY (`register_id`) REFERENCES `register` (`register_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `exam_apply_ibfk_2` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `exam_apply_ibfk_3` FOREIGN KEY (`doctor_id`) REFERENCES `employee` (`emp_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `exam_apply_ibfk_4` FOREIGN KEY (`item_id`) REFERENCES `exam_item` (`item_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4096 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '检查申请单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_item
-- ----------------------------
DROP TABLE IF EXISTS `exam_item`;
CREATE TABLE `exam_item`  (
  `item_id` int NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `item_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目编码',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
  `item_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目类型',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '价格',
  PRIMARY KEY (`item_id`) USING BTREE,
  UNIQUE INDEX `item_code`(`item_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1027 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '检查检验项目字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for icd10
-- ----------------------------
DROP TABLE IF EXISTS `icd10`;
CREATE TABLE `icd10`  (
  `icd_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ICD10编码',
  `icd_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '诊断名称',
  `py_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拼音码',
  PRIMARY KEY (`icd_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '疾病诊断字典表(ICD10)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for medical_record
-- ----------------------------
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record`  (
  `record_id` int NOT NULL AUTO_INCREMENT COMMENT '病历ID',
  `register_id` int NULL DEFAULT NULL COMMENT '挂号ID',
  `patient_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '患者ID',
  `doctor_id` int NULL DEFAULT NULL COMMENT '医生ID',
  `chief_complaint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '主诉',
  `present_illness` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '现病史',
  `diagnosis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '诊断结果',
  `icd_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '诊断编码',
  `record_time` datetime NULL DEFAULT NULL COMMENT '书写时间',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `register_id`(`register_id` ASC) USING BTREE,
  INDEX `patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `doctor_id`(`doctor_id` ASC) USING BTREE,
  INDEX `icd_code`(`icd_code` ASC) USING BTREE,
  CONSTRAINT `medical_record_ibfk_1` FOREIGN KEY (`register_id`) REFERENCES `register` (`register_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `medical_record_ibfk_2` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `medical_record_ibfk_3` FOREIGN KEY (`doctor_id`) REFERENCES `employee` (`emp_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `medical_record_ibfk_4` FOREIGN KEY (`icd_code`) REFERENCES `icd10` (`icd_code`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4096 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门诊病历表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for patient
-- ----------------------------
DROP TABLE IF EXISTS `patient`;
CREATE TABLE `patient`  (
  `patient_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '患者唯一ID',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '患者姓名',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别 1男 2女',
  `birth_date` date NULL DEFAULT NULL COMMENT '出生日期',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '家庭住址',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
  PRIMARY KEY (`patient_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '患者主索引表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for prescription
-- ----------------------------
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription`  (
  `presc_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '处方ID',
  `presc_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '处方单号',
  `register_id` int NULL DEFAULT NULL COMMENT '挂号ID',
  `patient_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '患者ID',
  `doctor_id` int NULL DEFAULT NULL COMMENT '医生ID',
  `presc_time` datetime NULL DEFAULT NULL COMMENT '开方时间',
  `total_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '处方总金额',
  `pay_status` tinyint NULL DEFAULT 0 COMMENT '支付状态 0未付 1已付',
  PRIMARY KEY (`presc_id`) USING BTREE,
  UNIQUE INDEX `presc_no`(`presc_no` ASC) USING BTREE,
  INDEX `register_id`(`register_id` ASC) USING BTREE,
  INDEX `patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `doctor_id`(`doctor_id` ASC) USING BTREE,
  CONSTRAINT `prescription_ibfk_1` FOREIGN KEY (`register_id`) REFERENCES `register` (`register_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `prescription_ibfk_2` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `prescription_ibfk_3` FOREIGN KEY (`doctor_id`) REFERENCES `employee` (`emp_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门诊处方主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for prescription_item
-- ----------------------------
DROP TABLE IF EXISTS `prescription_item`;
CREATE TABLE `prescription_item`  (
  `item_id` int NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `presc_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处方ID',
  `drug_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药品编码',
  `drug_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药品名称',
  `spec` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格',
  `unit` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单位',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  `quantity` int NULL DEFAULT NULL COMMENT '数量',
  `total_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '小计金额',
  `drug_usage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用法',
  `frequency` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '频次',
  PRIMARY KEY (`item_id`) USING BTREE,
  INDEX `presc_id`(`presc_id` ASC) USING BTREE,
  CONSTRAINT `prescription_item_ibfk_1` FOREIGN KEY (`presc_id`) REFERENCES `prescription` (`presc_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8192 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方药品明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for register
-- ----------------------------
DROP TABLE IF EXISTS `register`;
CREATE TABLE `register`  (
  `register_id` int NOT NULL AUTO_INCREMENT COMMENT '挂号ID',
  `register_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '挂号单号',
  `patient_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '患者ID',
  `dept_id` int NULL DEFAULT NULL COMMENT '科室ID',
  `doctor_id` int NULL DEFAULT NULL COMMENT '医生ID',
  `register_time` datetime NULL DEFAULT NULL COMMENT '挂号时间',
  `fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '挂号费',
  `pay_status` tinyint NULL DEFAULT 0 COMMENT '支付状态 0未付 1已付',
  `see_status` tinyint NULL DEFAULT 0 COMMENT '就诊状态 0未诊 1已诊',
  PRIMARY KEY (`register_id`) USING BTREE,
  UNIQUE INDEX `register_no`(`register_no` ASC) USING BTREE,
  INDEX `patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `doctor_id`(`doctor_id` ASC) USING BTREE,
  CONSTRAINT `register_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `register_ibfk_2` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`dept_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `register_ibfk_3` FOREIGN KEY (`doctor_id`) REFERENCES `employee` (`emp_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4099 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '门诊挂号表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for treat_item
-- ----------------------------
DROP TABLE IF EXISTS `treat_item`;
CREATE TABLE `treat_item`  (
  `item_id` int NOT NULL AUTO_INCREMENT COMMENT '治疗项目ID',
  `item_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目编码',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '价格',
  PRIMARY KEY (`item_id`) USING BTREE,
  UNIQUE INDEX `item_code`(`item_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1026 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '治疗项目字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for treat_record
-- ----------------------------
DROP TABLE IF EXISTS `treat_record`;
CREATE TABLE `treat_record`  (
  `tid` int NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `register_id` int NULL DEFAULT NULL COMMENT '挂号ID',
  `patient_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '患者ID',
  `item_id` int NULL DEFAULT NULL COMMENT '治疗项目ID',
  `doctor_id` int NULL DEFAULT NULL COMMENT '执行医生ID',
  `fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '治疗费用',
  `create_time` datetime NULL DEFAULT NULL COMMENT '执行时间',
  PRIMARY KEY (`tid`) USING BTREE,
  INDEX `register_id`(`register_id` ASC) USING BTREE,
  INDEX `patient_id`(`patient_id` ASC) USING BTREE,
  INDEX `item_id`(`item_id` ASC) USING BTREE,
  INDEX `doctor_id`(`doctor_id` ASC) USING BTREE,
  CONSTRAINT `treat_record_ibfk_1` FOREIGN KEY (`register_id`) REFERENCES `register` (`register_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `treat_record_ibfk_2` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `treat_record_ibfk_3` FOREIGN KEY (`item_id`) REFERENCES `treat_item` (`item_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `treat_record_ibfk_4` FOREIGN KEY (`doctor_id`) REFERENCES `employee` (`emp_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4096 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '治疗执行记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
