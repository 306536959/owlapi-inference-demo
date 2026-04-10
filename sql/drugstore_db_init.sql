-- ============================================================
-- 医院药检库 MySQL 数据库初始化脚本
-- 数据库名: drugstore_db
-- 说明: 医院药品管理、处方、药检相关数据
-- 创建时间: 2026-04-09
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建数据库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `drugstore_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `drugstore_db`;

-- ----------------------------
-- 1. 科室表 (department)
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
  `dept_id` VARCHAR(20) NOT NULL COMMENT '科室编码',
  `dept_name` VARCHAR(100) NOT NULL COMMENT '科室名称',
  `dept_type` VARCHAR(20) DEFAULT NULL COMMENT '科室类型: 内科/外科/儿科/妇产科等',
  `parent_dept_id` VARCHAR(20) DEFAULT NULL COMMENT '上级科室',
  `dept_location` VARCHAR(200) DEFAULT NULL COMMENT '科室位置',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `head_doctor_id` VARCHAR(20) DEFAULT NULL COMMENT '科室主任工号',
  `bed_count` INT DEFAULT 0 COMMENT '床位数',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0停用 1启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`dept_id`),
  KEY `idx_dept_type` (`dept_type`),
  KEY `idx_parent` (`parent_dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室信息表';

-- ----------------------------
-- 2. 药品表 (drug)
-- ----------------------------
DROP TABLE IF EXISTS `drug`;
CREATE TABLE `drug` (
  `drug_id` VARCHAR(20) NOT NULL COMMENT '药品编码',
  `drug_name` VARCHAR(200) NOT NULL COMMENT '药品名称',
  `generic_name` VARCHAR(200) DEFAULT NULL COMMENT '通用名',
  `drug_type` VARCHAR(50) NOT NULL COMMENT '药品类型: 西药/中成药/中药饮片',
  `specification` VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `manufacturer` VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
  `approval_number` VARCHAR(50) DEFAULT NULL COMMENT '批准文号',
  `storage_condition` VARCHAR(100) DEFAULT NULL COMMENT '储存条件',
  `validity_months` INT DEFAULT 24 COMMENT '有效期(月)',
  `inventory_quantity` INT DEFAULT 0 COMMENT '库存数量',
  `min_stock` INT DEFAULT 0 COMMENT '最低库存',
  `max_stock` INT DEFAULT 0 COMMENT '最高库存',
  `reimbursement_type` VARCHAR(20) DEFAULT NULL COMMENT '医保报销类型: 甲类/乙类/丙类',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0停用 1启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`drug_id`),
  KEY `idx_drug_name` (`drug_name`),
  KEY `idx_generic_name` (`generic_name`),
  KEY `idx_drug_type` (`drug_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品信息表';

-- ----------------------------
-- 3. 药品成分表 (drug_ingredient)
-- ----------------------------
DROP TABLE IF EXISTS `drug_ingredient`;
CREATE TABLE `drug_ingredient` (
  `ingredient_id` VARCHAR(20) NOT NULL COMMENT '成分ID',
  `ingredient_name` VARCHAR(100) NOT NULL COMMENT '成分名称',
  `ingredient_type` VARCHAR(50) DEFAULT NULL COMMENT '成分类型: 活性成分/辅料/添加剂',
  `cas_number` VARCHAR(50) DEFAULT NULL COMMENT 'CAS号',
  `molecular_formula` VARCHAR(100) DEFAULT NULL COMMENT '分子式',
  `description` TEXT COMMENT '说明',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ingredient_id`),
  KEY `idx_ingredient_name` (`ingredient_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品成分表';

-- ----------------------------
-- 4. 药品-成分关联表 (drug_ingredient_rel)
-- ----------------------------
DROP TABLE IF EXISTS `drug_ingredient_rel`;
CREATE TABLE `drug_ingredient_rel` (
  `rel_id` INT AUTO_INCREMENT NOT NULL COMMENT '关联ID',
  `drug_id` VARCHAR(20) NOT NULL COMMENT '药品编码',
  `ingredient_id` VARCHAR(20) NOT NULL COMMENT '成分ID',
  `content` VARCHAR(100) DEFAULT NULL COMMENT '含量',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否活性成分',
  PRIMARY KEY (`rel_id`),
  KEY `idx_drug` (`drug_id`),
  KEY `idx_ingredient` (`ingredient_id`),
  CONSTRAINT `fk_drug_ing_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug` (`drug_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_drug_ing_ing` FOREIGN KEY (`ingredient_id`) REFERENCES `drug_ingredient` (`ingredient_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品成分关联表';

-- ----------------------------
-- 5. 药品相互作用表 (drug_interaction)
-- ----------------------------
DROP TABLE IF EXISTS `drug_interaction`;
CREATE TABLE `drug_interaction` (
  `interaction_id` VARCHAR(20) NOT NULL COMMENT '相互作用ID',
  `drug1_id` VARCHAR(20) NOT NULL COMMENT '药品1编码',
  `drug2_id` VARCHAR(20) NOT NULL COMMENT '药品2编码',
  `interaction_level` VARCHAR(20) NOT NULL COMMENT '作用级别: 严重/中等/轻微',
  `description` TEXT NOT NULL COMMENT '相互作用描述',
  `mechanism` TEXT COMMENT '作用机制',
  `clinical_advice` TEXT COMMENT '临床建议',
  `literature_source` VARCHAR(200) DEFAULT NULL COMMENT '文献来源',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`interaction_id`),
  KEY `idx_drug1` (`drug1_id`),
  KEY `idx_drug2` (`drug2_id`),
  KEY `idx_level` (`interaction_level`),
  CONSTRAINT `fk_int_drug1` FOREIGN KEY (`drug1_id`) REFERENCES `drug` (`drug_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_int_drug2` FOREIGN KEY (`drug2_id`) REFERENCES `drug` (`drug_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品相互作用表';

-- ----------------------------
-- 6. 药品禁忌表 (drug_contraindication)
-- ----------------------------
DROP TABLE IF EXISTS `drug_contraindication`;
CREATE TABLE `drug_contraindication` (
  `contraindication_id` VARCHAR(20) NOT NULL COMMENT '禁忌ID',
  `drug_id` VARCHAR(20) NOT NULL COMMENT '药品编码',
  `contraindication_type` VARCHAR(50) NOT NULL COMMENT '禁忌类型: 禁用/慎用/忌用',
  `condition_code` VARCHAR(50) DEFAULT NULL COMMENT '禁忌条件代码',
  `condition_desc` VARCHAR(200) DEFAULT NULL COMMENT '禁忌条件描述',
  `severity` VARCHAR(20) DEFAULT NULL COMMENT '严重程度',
  `description` TEXT COMMENT '详细说明',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`contraindication_id`),
  KEY `idx_drug` (`drug_id`),
  CONSTRAINT `fk_contra_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug` (`drug_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品禁忌表';

-- ----------------------------
-- 7. 医生表 (doctor)
-- ----------------------------
DROP TABLE IF EXISTS `doctor`;
CREATE TABLE `doctor` (
  `doctor_id` VARCHAR(20) NOT NULL COMMENT '工号',
  `doctor_name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `birth_date` DATE DEFAULT NULL COMMENT '出生日期',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `dept_id` VARCHAR(20) DEFAULT NULL COMMENT '所属科室',
  `title` VARCHAR(50) DEFAULT NULL COMMENT '职称: 主任医师/副主任医师/主治医师/住院医师',
  `specialty` VARCHAR(200) DEFAULT NULL COMMENT '专长',
  `qualification_no` VARCHAR(50) DEFAULT NULL COMMENT '执业资格证号',
  `employment_date` DATE DEFAULT NULL COMMENT '入职日期',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0离职 1在职',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`doctor_id`),
  KEY `idx_dept` (`dept_id`),
  KEY `idx_title` (`title`),
  CONSTRAINT `fk_doc_dept` FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生信息表';

-- ----------------------------
-- 8. 患者表 (patient)
-- ----------------------------
DROP TABLE IF EXISTS `patient`;
CREATE TABLE `patient` (
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `patient_name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `gender` VARCHAR(10) NOT NULL COMMENT '性别',
  `birth_date` DATE NOT NULL COMMENT '出生日期',
  `id_card` VARCHAR(18) NOT NULL COMMENT '身份证号',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `address` VARCHAR(300) DEFAULT NULL COMMENT '住址',
  `emergency_contact` VARCHAR(50) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` VARCHAR(20) DEFAULT NULL COMMENT '紧急联系电话',
  `blood_type` VARCHAR(10) DEFAULT NULL COMMENT '血型',
  `allergy_history` TEXT DEFAULT NULL COMMENT '过敏史',
  `medical_history` TEXT DEFAULT NULL COMMENT '既往病史',
  `insurance_type` VARCHAR(50) DEFAULT NULL COMMENT '医保类型',
  `insurance_no` VARCHAR(50) DEFAULT NULL COMMENT '医保卡号',
  `registration_date` DATE NOT NULL COMMENT '建档日期',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0注销 1正常',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`patient_id`),
  KEY `idx_name` (`patient_name`),
  KEY `idx_id_card` (`id_card`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者信息表';

-- ----------------------------
-- 9. 门诊处方表 (prescription)
-- ----------------------------
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription` (
  `prescription_id` VARCHAR(20) NOT NULL COMMENT '处方编号',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `doctor_id` VARCHAR(20) NOT NULL COMMENT '开方医生',
  `dept_id` VARCHAR(20) DEFAULT NULL COMMENT '科室',
  `diagnosis_id` VARCHAR(20) DEFAULT NULL COMMENT '关联诊断ID',
  `prescription_type` VARCHAR(20) NOT NULL COMMENT '处方类型: 西药/中药/中成药',
  `prescription_date` DATETIME NOT NULL COMMENT '开方日期',
  `diagnosis_main` VARCHAR(200) DEFAULT NULL COMMENT '主诊断',
  `diagnosis_sub` VARCHAR(200) DEFAULT NULL COMMENT '副诊断',
  `total_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '处方总金额',
  `payment_type` VARCHAR(20) DEFAULT NULL COMMENT '支付方式',
  `dispensing_status` VARCHAR(20) DEFAULT '待发药' COMMENT '发药状态: 待发药/已发药/已退药',
  `dispenser_id` VARCHAR(20) DEFAULT NULL COMMENT '发药人',
  `dispensing_time` DATETIME DEFAULT NULL COMMENT '发药时间',
  `pharmacist_advice` TEXT DEFAULT NULL COMMENT '药师建议',
  `validity_days` INT DEFAULT 3 COMMENT '有效期(天)',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0作废 1有效',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`prescription_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_doctor` (`doctor_id`),
  KEY `idx_date` (`prescription_date`),
  KEY `idx_diagnosis` (`diagnosis_id`),
  CONSTRAINT `fk_pres_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pres_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`doctor_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门诊处方表';

-- ----------------------------
-- 10. 处方明细表 (prescription_detail)
-- ----------------------------
DROP TABLE IF EXISTS `prescription_detail`;
CREATE TABLE `prescription_detail` (
  `detail_id` INT AUTO_INCREMENT NOT NULL COMMENT '明细ID',
  `prescription_id` VARCHAR(20) NOT NULL COMMENT '处方编号',
  `drug_id` VARCHAR(20) NOT NULL COMMENT '药品编码',
  `drug_name` VARCHAR(200) NOT NULL COMMENT '药品名称(冗余)',
  `quantity` DECIMAL(10,2) NOT NULL COMMENT '数量',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `specification` VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '金额',
  `dosage` VARCHAR(100) DEFAULT NULL COMMENT '用法用量',
  `frequency` VARCHAR(50) DEFAULT NULL COMMENT '频率: 每日几次',
  `administration_route` VARCHAR(50) DEFAULT NULL COMMENT '给药途径',
  `duration` VARCHAR(50) DEFAULT NULL COMMENT '疗程',
  `medication_days` INT DEFAULT NULL COMMENT '服药天数',
  `is_bundled` TINYINT DEFAULT 0 COMMENT '是否拆零',
  `batch_no` VARCHAR(50) DEFAULT NULL COMMENT '批号',
  `line_no` INT DEFAULT NULL COMMENT '处方笺行号',
  `notes` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0取消 1正常',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_prescription` (`prescription_id`),
  KEY `idx_drug` (`drug_id`),
  CONSTRAINT `fk_presd_pres` FOREIGN KEY (`prescription_id`) REFERENCES `prescription` (`prescription_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_presd_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug` (`drug_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方明细表';

-- ----------------------------
-- 11. 药品库存表 (drug_stock)
-- ----------------------------
DROP TABLE IF EXISTS `drug_stock`;
CREATE TABLE `drug_stock` (
  `stock_id` VARCHAR(20) NOT NULL COMMENT '库存ID',
  `drug_id` VARCHAR(20) NOT NULL COMMENT '药品编码',
  `warehouse_id` VARCHAR(20) DEFAULT NULL COMMENT '仓库ID',
  `batch_no` VARCHAR(50) NOT NULL COMMENT '批号',
  `production_date` DATE DEFAULT NULL COMMENT '生产日期',
  `expiry_date` DATE DEFAULT NULL COMMENT '有效期至',
  `quantity` INT NOT NULL COMMENT '库存数量',
  `unit_cost` DECIMAL(10,2) DEFAULT 0 COMMENT '单位成本',
  `supplier_id` VARCHAR(20) DEFAULT NULL COMMENT '供应商ID',
  `received_date` DATE DEFAULT NULL COMMENT '入库日期',
  `storage_location` VARCHAR(100) DEFAULT NULL COMMENT '存放位置',
  `quality_status` VARCHAR(20) DEFAULT '合格' COMMENT '质量状态',
  `last_check_date` DATE DEFAULT NULL COMMENT '最近盘点日期',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`stock_id`),
  KEY `idx_drug` (`drug_id`),
  KEY `idx_batch` (`batch_no`),
  KEY `idx_expiry` (`expiry_date`),
  CONSTRAINT `fk_stock_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug` (`drug_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品库存表';

-- ----------------------------
-- 12. 药检记录表 (drug_test)
-- ----------------------------
DROP TABLE IF EXISTS `drug_test`;
CREATE TABLE `drug_test` (
  `test_id` VARCHAR(20) NOT NULL COMMENT '药检ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `prescription_id` VARCHAR(20) DEFAULT NULL COMMENT '关联处方ID',
  `test_type` VARCHAR(50) NOT NULL COMMENT '检验类型',
  `test_item` VARCHAR(100) NOT NULL COMMENT '检验项目',
  `specimen_type` VARCHAR(50) DEFAULT NULL COMMENT '标本类型: 血液/尿液/其他',
  `specimen_no` VARCHAR(50) DEFAULT NULL COMMENT '标本编号',
  `collect_time` DATETIME DEFAULT NULL COMMENT '采集时间',
  `receive_time` DATETIME DEFAULT NULL COMMENT '接收时间',
  `report_time` DATETIME DEFAULT NULL COMMENT '报告时间',
  `test_result` VARCHAR(200) DEFAULT NULL COMMENT '检验结果',
  `reference_value` VARCHAR(100) DEFAULT NULL COMMENT '参考值',
  `unit` VARCHAR(50) DEFAULT NULL COMMENT '单位',
  `result_flag` VARCHAR(10) DEFAULT NULL COMMENT '结果标志: H高/L低/N正常',
  `instrument` VARCHAR(100) DEFAULT NULL COMMENT '检测仪器',
  `reagent` VARCHAR(100) DEFAULT NULL COMMENT '检测试剂',
  `tester_id` VARCHAR(20) DEFAULT NULL COMMENT '检验员',
  `reviewer_id` VARCHAR(20) DEFAULT NULL COMMENT '审核员',
  `conclusion` TEXT DEFAULT NULL COMMENT '结论',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`test_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_prescription` (`prescription_id`),
  KEY `idx_test_type` (`test_type`),
  KEY `idx_test_date` (`collect_time`),
  CONSTRAINT `fk_test_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_test_pres` FOREIGN KEY (`prescription_id`) REFERENCES `prescription` (`prescription_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药检记录表';

-- ----------------------------
-- 13. 药品不良反应表 (adverse_drug_reaction)
-- ----------------------------
DROP TABLE IF EXISTS `adverse_drug_reaction`;
CREATE TABLE `adverse_drug_reaction` (
  `adr_id` VARCHAR(20) NOT NULL COMMENT '不良反应ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `drug_id` VARCHAR(20) NOT NULL COMMENT '药品编码',
  `prescription_id` VARCHAR(20) DEFAULT NULL COMMENT '关联处方',
  `occurrence_time` DATETIME NOT NULL COMMENT '发生时间',
  `severity_level` VARCHAR(20) NOT NULL COMMENT '严重程度: 轻度/中度/重度/严重',
  `reaction_type` VARCHAR(100) DEFAULT NULL COMMENT '反应类型',
  `reaction_description` TEXT NOT NULL COMMENT '反应描述',
  `outcome` VARCHAR(50) DEFAULT NULL COMMENT '转归: 痊愈/好转/未好转/后遗症/死亡',
  `handling_measures` TEXT DEFAULT NULL COMMENT '处理措施',
  `causality_assessment` VARCHAR(50) DEFAULT NULL COMMENT '因果关系评价',
  `reporter_id` VARCHAR(20) DEFAULT NULL COMMENT '报告人',
  `report_date` DATE DEFAULT NULL COMMENT '报告日期',
  `review_status` VARCHAR(20) DEFAULT '待审核' COMMENT '审核状态',
  `reviewer_id` VARCHAR(20) DEFAULT NULL COMMENT '审核人',
  `review_date` DATE DEFAULT NULL COMMENT '审核日期',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`adr_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_drug` (`drug_id`),
  KEY `idx_report_date` (`report_date`),
  CONSTRAINT `fk_adr_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_adr_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug` (`drug_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品不良反应表';

-- ----------------------------
-- 14. 药品供应商表 (drug_supplier)
-- ----------------------------
DROP TABLE IF EXISTS `drug_supplier`;
CREATE TABLE `drug_supplier` (
  `supplier_id` VARCHAR(20) NOT NULL COMMENT '供应商ID',
  `supplier_name` VARCHAR(200) NOT NULL COMMENT '供应商名称',
  `supplier_type` VARCHAR(50) DEFAULT NULL COMMENT '供应商类型',
  `license_no` VARCHAR(50) DEFAULT NULL COMMENT '许可证号',
  `legal_person` VARCHAR(50) DEFAULT NULL COMMENT '法人代表',
  `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `fax` VARCHAR(20) DEFAULT NULL COMMENT '传真',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `address` VARCHAR(300) DEFAULT NULL COMMENT '地址',
  `bank` VARCHAR(100) DEFAULT NULL COMMENT '开户银行',
  `bank_account` VARCHAR(50) DEFAULT NULL COMMENT '银行账号',
  `rating` INT DEFAULT 5 COMMENT '评级(1-5星)',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0停用 1启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`),
  KEY `idx_supplier_name` (`supplier_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品供应商表';

-- ----------------------------
-- 15. 药品调价记录表 (drug_price_adjust)
-- ----------------------------
DROP TABLE IF EXISTS `drug_price_adjust`;
CREATE TABLE `drug_price_adjust` (
  `adjust_id` VARCHAR(20) NOT NULL COMMENT '调价ID',
  `drug_id` VARCHAR(20) NOT NULL COMMENT '药品编码',
  `old_price` DECIMAL(10,2) NOT NULL COMMENT '原价格',
  `new_price` DECIMAL(10,2) NOT NULL COMMENT '新价格',
  `adjust_ratio` DECIMAL(10,4) DEFAULT NULL COMMENT '调整幅度',
  `adjust_reason` VARCHAR(200) DEFAULT NULL COMMENT '调价原因',
  `effective_date` DATETIME NOT NULL COMMENT '生效时间',
  `applier_id` VARCHAR(20) DEFAULT NULL COMMENT '申请人',
  `approver_id` VARCHAR(20) DEFAULT NULL COMMENT '审批人',
  `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `approval_status` VARCHAR(20) DEFAULT '待审批' COMMENT '审批状态',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`adjust_id`),
  KEY `idx_drug` (`drug_id`),
  KEY `idx_effective` (`effective_date`),
  CONSTRAINT `fk_price_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug` (`drug_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品调价记录表';

-- ----------------------------
-- 16. 用药指导表 (medication_guide)
-- ----------------------------
DROP TABLE IF EXISTS `medication_guide`;
CREATE TABLE `medication_guide` (
  `guide_id` VARCHAR(20) NOT NULL COMMENT '指导ID',
  `prescription_id` VARCHAR(20) NOT NULL COMMENT '处方编号',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `guide_content` TEXT NOT NULL COMMENT '用药指导内容',
  `dietary_advice` TEXT DEFAULT NULL COMMENT '饮食建议',
  `warning_info` TEXT DEFAULT NULL COMMENT '警示信息',
  `follow_up_date` DATE DEFAULT NULL COMMENT '随访日期',
  `guide_doctor_id` VARCHAR(20) DEFAULT NULL COMMENT '指导医生',
  `guide_time` DATETIME DEFAULT NULL COMMENT '指导时间',
  `patient_feedback` TEXT DEFAULT NULL COMMENT '患者反馈',
  `feedback_time` DATETIME DEFAULT NULL COMMENT '反馈时间',
  `effect_evaluation` VARCHAR(100) DEFAULT NULL COMMENT '疗效评价',
  `next_guide_plan` TEXT DEFAULT NULL COMMENT '下次指导计划',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`guide_id`),
  KEY `idx_prescription` (`prescription_id`),
  KEY `idx_patient` (`patient_id`),
  CONSTRAINT `fk_guide_pres` FOREIGN KEY (`prescription_id`) REFERENCES `prescription` (`prescription_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_guide_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药指导表';

-- ============================================================
-- 插入测试数据
-- ============================================================

-- 科室数据
INSERT INTO `department` (`dept_id`, `dept_name`, `dept_type`, `parent_dept_id`, `dept_location`, `phone`, `bed_count`, `status`) VALUES
('DEPT001', '心血管内科', '内科', NULL, '门诊楼3层东侧', '010-88880001', 60, 1),
('DEPT002', '呼吸内科', '内科', NULL, '门诊楼3层西侧', '010-88880002', 45, 1),
('DEPT003', '消化内科', '内科', NULL, '门诊楼4层东侧', '010-88880003', 40, 1),
('DEPT004', '内分泌科', '内科', NULL, '门诊楼4层西侧', '010-88880004', 35, 1),
('DEPT005', '神经内科', '内科', NULL, '门诊楼5层东侧', '010-88880005', 50, 1),
('DEPT006', '普外科', '外科', NULL, '门诊楼2层东侧', '010-88880006', 80, 1),
('DEPT007', '骨科', '外科', NULL, '门诊楼2层西侧', '010-88880007', 55, 1),
('DEPT008', '儿科', '儿科', NULL, '门诊楼6层', '010-88880008', 70, 1),
('DEPT009', '妇产科', '妇产科', NULL, '住院楼5层', '010-88880009', 90, 1),
('DEPT010', '药剂科', '医技', NULL, '门诊楼1层', '010-88880010', 0, 1);

-- 药品数据
INSERT INTO `drug` (`drug_id`, `drug_name`, `generic_name`, `drug_type`, `specification`, `unit`, `price`, `manufacturer`, `approval_number`, `validity_months`, `inventory_quantity`, `reimbursement_type`, `status`) VALUES
('DRUG001', '阿司匹林肠溶片', '阿司匹林', '西药', '100mg*30片', '盒', 8.50, '拜耳医药保健有限公司', '国药准字H20050059', 36, 5000, '甲类', 1),
('DRUG002', '硝苯地平缓释片', '硝苯地平', '西药', '20mg*30片', '盒', 25.80, '拜耳医药保健有限公司', '国药准字H10940079', 24, 3000, '甲类', 1),
('DRUG003', '阿托伐他汀钙片', '阿托伐他汀', '西药', '20mg*7片', '盒', 42.60, '辉瑞制药有限公司', '国药准字J20170029', 24, 2000, '乙类', 1),
('DRUG004', '盐酸二甲双胍片', '二甲双胍', '西药', '0.5g*48片', '盒', 18.90, '中美上海施贵宝制药有限公司', '国药准字H20023370', 36, 4500, '甲类', 1),
('DRUG005', '氯沙坦钾片', '氯沙坦', '西药', '50mg*7片', '盒', 38.50, '杭州默沙东制药有限公司', '国药准字J20180001', 24, 2500, '乙类', 1),
('DRUG006', '奥美拉唑肠溶胶囊', '奥美拉唑', '西药', '20mg*14粒', '盒', 22.30, '阿斯利康制药有限公司', '国药准字H20046379', 24, 3500, '甲类', 1),
('DRUG007', '头孢克肟分散片', '头孢克肟', '西药', '0.1g*6片', '盒', 35.60, '石药集团欧意药业有限公司', '国药准字H20040486', 24, 1800, '乙类', 1),
('DRUG008', '复方氨酚烷胺片', '对乙酰氨基酚', '西药', '12片', '盒', 6.80, '海南康力制药有限公司', '国药准字H46020638', 24, 6000, '甲类', 1),
('DRUG009', '布洛芬混悬液', '布洛芬', '西药', '100ml:2g', '瓶', 18.50, '上海强生制药有限公司', '国药准字H19991011', 24, 2000, '甲类', 1),
('DRUG010', '硫酸沙丁胺醇吸入气雾剂', '沙丁胺醇', '西药', '100μg*200揿', '支', 38.20, '葛兰素史克制药有限公司', '国药准字H20090514', 24, 800, '甲类', 1),
('DRUG011', '银杏叶片', '银杏叶提取物', '中成药', '9.6mg*80片', '盒', 28.50, '贵州益佰制药股份有限公司', '国药准字Z52020225', 24, 2500, '乙类', 1),
('DRUG012', '复方丹参滴丸', '丹参三七冰片', '中成药', '27mg*180粒', '瓶', 29.80, '天士力医药集团股份有限公司', '国药准字Z10950111', 24, 3000, '甲类', 1),
('DRUG013', '华法林钠片', '华法林', '西药', '3mg*100片', '瓶', 32.50, '芬兰奥利安集团', '国药准字J20130108', 36, 500, '甲类', 1),
('DRUG014', '左甲状腺素钠片', '左甲状腺素', '西药', '50μg*100片', '盒', 28.00, 'Merck KGaA Germany', '国药准字J20160079', 36, 1500, '甲类', 1),
('DRUG015', '泮托拉唑钠肠溶片', '泮托拉唑', '西药', '40mg*14片', '盒', 36.80, '武田药品工业株式会社', '国药准字H20150035', 24, 2200, '乙类', 1);

-- 药品成分数据
INSERT INTO `drug_ingredient` (`ingredient_id`, `ingredient_name`, `ingredient_type`, `cas_number`, `molecular_formula`) VALUES
('ING001', '阿司匹林', '活性成分', '50-78-2', 'C9H8O4'),
('ING002', '硝苯地平', '活性成分', '21829-25-4', 'C17H18N2O6'),
('ING003', '阿托伐他汀钙', '活性成分', '134523-00-9', 'C66H68CaF2N4O10'),
('ING004', '盐酸二甲双胍', '活性成分', '1115-70-4', 'C4H12ClN5'),
('ING005', '氯沙坦钾', '活性成分', '124750-99-8', 'C22H23ClKN6O'),
('ING006', '奥美拉唑', '活性成分', '73590-58-6', 'C17H19N3O3S'),
('ING007', '头孢克肟', '活性成分', '79350-37-1', 'C16H15N5O7S2'),
('ING008', '对乙酰氨基酚', '活性成分', '103-90-2', 'C8H9NO2'),
('ING009', '布洛芬', '活性成分', '15687-27-1', 'C13H18O2'),
('ING010', '硫酸沙丁胺醇', '活性成分', '51022-70-9', 'C13H23NO3'),
('ING011', '银杏叶提取物', '活性成分', '20333-39-5', '混合物的总黄酮醇苷'),
('ING012', '丹参酮IIA', '活性成分', '40181-00-6', 'C19H18O3'),
('ING013', '华法林钠', '活性成分', '129-06-6', 'C19H16O4'),
('ING014', '左甲状腺素钠', '活性成分', '55-03-8', 'C15H11I4NNaO4'),
('ING015', '泮托拉唑钠', '活性成分', '154637-70-8', 'C16H16F2N3NaO4S');

-- 药品-成分关联
INSERT INTO `drug_ingredient_rel` (`drug_id`, `ingredient_id`, `content`, `is_active`) VALUES
('DRUG001', 'ING001', '100mg', 1),
('DRUG002', 'ING002', '20mg', 1),
('DRUG003', 'ING003', '20mg', 1),
('DRUG004', 'ING004', '0.5g', 1),
('DRUG005', 'ING005', '50mg', 1),
('DRUG006', 'ING006', '20mg', 1),
('DRUG007', 'ING007', '0.1g', 1),
('DRUG008', 'ING008', '250mg', 1),
('DRUG009', 'ING009', '20mg/ml', 1),
('DRUG010', 'ING010', '100μg/揿', 1),
('DRUG011', 'ING011', '9.6mg', 1),
('DRUG012', 'ING012', '每丸含丹参三七冰片', 1),
('DRUG013', 'ING013', '3mg', 1),
('DRUG014', 'ING014', '50μg', 1),
('DRUG015', 'ING006', '40mg', 1);

-- 药品相互作用
INSERT INTO `drug_interaction` (`interaction_id`, `drug1_id`, `drug2_id`, `interaction_level`, `description`, `clinical_advice`) VALUES
('INT001', 'DRUG001', 'DRUG013', '严重', '阿司匹林增强华法林的抗凝作用', '避免合用，如需合用应密切监测INR'),
('INT002', 'DRUG001', 'DRUG004', '中等', '两者联用可能增加低血糖风险', '密切监测血糖'),
('INT003', 'DRUG002', 'DRUG005', '轻微', '两者联用可能增强降压效果', '注意血压监测'),
('INT004', 'DRUG002', 'DRUG013', '严重', '硝苯地平可能增强华法林作用', '密切监测凝血功能'),
('INT005', 'DRUG003', 'DRUG004', '中等', '两者联用需注意肌病风险', '定期监测肌酸激酶'),
('INT006', 'DRUG006', 'DRUG007', '轻微', '奥美拉唑可能降低头孢克肟吸收', '间隔2小时服用'),
('INT007', 'DRUG001', 'DRUG002', '中等', '两者联用可能增加出血风险', '密切观察出血倾向');

-- 药品禁忌
INSERT INTO `drug_contraindication` (`contraindication_id`, `drug_id`, `contraindication_type`, `condition_desc`, `description`) VALUES
('CONT001', 'DRUG001', '禁用', '活动性消化道溃疡', '活动性消化道溃疡患者禁用'),
('CONT002', 'DRUG001', '禁用', '对阿司匹林过敏', '对阿司匹林或其他水杨酸类药物过敏者禁用'),
('CONT003', 'DRUG013', '禁用', '出血倾向', '有出血倾向或凝血功能障碍者禁用'),
('CONT004', 'DRUG004', '禁用', '严重肝肾功能不全', '严重肝肾功能不全者禁用'),
('CONT005', 'DRUG004', '慎用', '碘造影剂检查前后', '使用碘造影剂检查前后48小时禁用');

-- 医生数据
INSERT INTO `doctor` (`doctor_id`, `doctor_name`, `gender`, `birth_date`, `phone`, `dept_id`, `title`, `specialty`, `employment_date`, `status`) VALUES
('DOC001', '张明华', '男', '1975-03-15', '13800001001', 'DEPT001', '主任医师', '冠心病介入治疗、高血压', '2000-07-01', 1),
('DOC002', '李秀英', '女', '1978-08-22', '13800001002', 'DEPT001', '副主任医师', '心律失常、心力衰竭', '2002-09-01', 1),
('DOC003', '王建国', '男', '1980-05-10', '13800001003', 'DEPT002', '主任医师', '慢阻肺、哮喘', '2005-07-01', 1),
('DOC004', '刘芳', '女', '1982-11-20', '13800001004', 'DEPT003', '副主任医师', '胃肠道疾病、幽门螺杆菌', '2008-08-01', 1),
('DOC005', '陈志强', '男', '1970-06-18', '13800001005', 'DEPT004', '主任医师', '糖尿病、甲状腺疾病', '1998-03-01', 1),
('DOC006', '赵雪梅', '女', '1985-09-25', '13800001006', 'DEPT005', '主治医师', '脑血管病、帕金森病', '2012-07-01', 1),
('DOC007', '孙伟民', '男', '1976-12-08', '13800001007', 'DEPT006', '主任医师', '胃肠外科、腹腔镜手术', '2001-06-01', 1),
('DOC008', '周丽娟', '女', '1988-02-14', '13800001008', 'DEPT008', '主治医师', '儿童呼吸道疾病', '2015-08-01', 1);

-- 患者数据
INSERT INTO `patient` (`patient_id`, `patient_name`, `gender`, `birth_date`, `id_card`, `phone`, `address`, `emergency_contact`, `emergency_phone`, `blood_type`, `allergy_history`, `insurance_type`, `registration_date`, `status`) VALUES
('PAT001', '李伟', '男', '1965-05-20', '110101196505201234', '13900001001', '北京市朝阳区建国路88号', '李强', '13900001002', 'A型', '青霉素过敏', '职工医保', '2018-03-15', 1),
('PAT002', '王芳', '女', '1972-08-15', '110101197208151567', '13900001003', '北京市海淀区中关村大街1号', '张华', '13900001004', 'O型', '无', '居民医保', '2019-06-20', 1),
('PAT003', '张三', '男', '1958-12-03', '110101195812032891', '13900001005', '北京市东城区王府井大街50号', '张小明', '13900001006', 'B型', '磺胺类药物过敏', '职工医保', '2017-01-10', 1),
('PAT004', '李梅', '女', '1985-03-28', '110101198503281234', '13900001007', '北京市西城区西单大街30号', '李大明', '13900001008', 'AB型', '无', '居民医保', '2020-02-14', 1),
('PAT005', '赵军', '男', '1970-07-22', '110101197007221567', '13900001009', '北京市丰台区南三环路88号', '赵丽', '13900001010', 'A型', '对海鲜过敏', '职工医保', '2018-11-05', 1),
('PAT006', '陈红', '女', '1990-01-15', '110101199001152891', '13900001011', '北京市石景山区石景山路100号', '陈刚', '13900001012', 'O型', '无', '居民医保', '2021-05-08', 1),
('PAT007', '刘强', '男', '1955-09-10', '110101195509101234', '13900001013', '北京市通州区新华大街88号', '刘小强', '13900001014', 'B型', '头孢类药物过敏', '职工医保', '2016-08-22', 1),
('PAT008', '孙丽', '女', '1988-04-05', '110101198804052345', '13900001015', '北京市昌平区回龙观大街50号', '孙明', '13900001016', 'A型', '无', '居民医保', '2022-01-18', 1);

-- 处方数据
INSERT INTO `prescription` (`prescription_id`, `patient_id`, `doctor_id`, `dept_id`, `prescription_type`, `prescription_date`, `diagnosis_main`, `total_amount`, `payment_type`, `dispensing_status`, `status`) VALUES
('PRE001', 'PAT001', 'DOC001', 'DEPT001', '西药', '2026-04-01 09:30:00', '冠状动脉粥样硬化性心脏病', 89.90, '医保', '已发药', 1),
('PRE002', 'PAT002', 'DOC005', 'DEPT004', '西药', '2026-04-01 10:15:00', '2型糖尿病', 61.40, '医保', '已发药', 1),
('PRE003', 'PAT003', 'DOC002', 'DEPT001', '西药', '2026-04-02 08:45:00', '心房颤动', 71.00, '医保', '已发药', 1),
('PRE004', 'PAT004', 'DOC004', 'DEPT003', '西药', '2026-04-02 14:20:00', '慢性胃炎', 59.10, '自费', '待发药', 1),
('PRE005', 'PAT005', 'DOC003', 'DEPT002', '西药', '2026-04-03 09:00:00', '慢性阻塞性肺疾病', 106.50, '医保', '已发药', 1),
('PRE006', 'PAT001', 'DOC005', 'DEPT004', '西药', '2026-04-03 10:30:00', '2型糖尿病伴有并发症', 42.60, '医保', '已发药', 1),
('PRE007', 'PAT006', 'DOC008', 'DEPT008', '西药', '2026-04-03 15:45:00', '急性上呼吸道感染', 25.30, '自费', '已发药', 1),
('PRE008', 'PAT007', 'DOC006', 'DEPT005', '西药', '2026-04-04 08:30:00', '脑梗死恢复期', 95.80, '医保', '待发药', 1);

-- 处方明细数据
INSERT INTO `prescription_detail` (`prescription_id`, `drug_id`, `drug_name`, `quantity`, `unit`, `specification`, `unit_price`, `total_price`, `dosage`, `frequency`, `administration_route`, `duration`, `status`) VALUES
-- PRE001 的明细
('PRE001', 'DRUG001', '阿司匹林肠溶片', 3, '盒', '100mg*30片', 8.50, 25.50, '100mg', '每日一次', '口服', '长期', 1),
('PRE001', 'DRUG002', '硝苯地平缓释片', 2, '盒', '20mg*30片', 25.80, 51.60, '20mg', '每日两次', '口服', '30天', 1),
('PRE001', 'DRUG003', '阿托伐他汀钙片', 1, '盒', '20mg*7片', 42.60, 42.60, '20mg', '每晚一次', '口服', '7天', 1),
-- PRE002 的明细
('PRE002', 'DRUG004', '盐酸二甲双胍片', 3, '盒', '0.5g*48片', 18.90, 56.70, '0.5g', '每日三次', '口服', '30天', 1),
('PRE002', 'DRUG008', '复方氨酚烷胺片', 1, '盒', '12片', 6.80, 6.80, '1片', '发热时服用', '口服', '必要时', 1),
-- PRE003 的明细
('PRE003', 'DRUG013', '华法林钠片', 1, '瓶', '3mg*100片', 32.50, 32.50, '3mg', '每日一次', '口服', '30天', 1),
('PRE003', 'DRUG001', '阿司匹林肠溶片', 3, '盒', '100mg*30片', 8.50, 25.50, '100mg', '每日一次', '口服', '长期', 1),
('PRE003', 'DRUG011', '银杏叶片', 1, '盒', '9.6mg*80片', 28.50, 28.50, '9.6mg', '每日三次', '口服', '30天', 1),
-- PRE004 的明细
('PRE004', 'DRUG006', '奥美拉唑肠溶胶囊', 1, '盒', '20mg*14粒', 22.30, 22.30, '20mg', '每日一次', '口服', '14天', 1),
('PRE004', 'DRUG008', '复方氨酚烷胺片', 2, '盒', '12片', 6.80, 13.60, '1片', '每日两次', '口服', '3天', 1),
-- PRE005 的明细
('PRE005', 'DRUG010', '硫酸沙丁胺醇吸入气雾剂', 1, '支', '100μg*200揿', 38.20, 38.20, '100μg', '每日四次', '吸入', '30天', 1),
('PRE005', 'DRUG007', '头孢克肟分散片', 3, '盒', '0.1g*6片', 35.60, 106.80, '0.2g', '每日两次', '口服', '5天', 1),
-- PRE006 的明细
('PRE006', 'DRUG004', '盐酸二甲双胍片', 2, '盒', '0.5g*48片', 18.90, 37.80, '0.5g', '每日三次', '口服', '30天', 1),
('PRE006', 'DRUG003', '阿托伐他汀钙片', 1, '盒', '20mg*7片', 42.60, 42.60, '20mg', '每晚一次', '口服', '7天', 1),
-- PRE007 的明细
('PRE007', 'DRUG009', '布洛芬混悬液', 1, '瓶', '100ml:2g', 18.50, 18.50, '5ml', '体温>38.5℃时', '口服', '必要时', 1),
('PRE007', 'DRUG008', '复方氨酚烷胺片', 1, '盒', '12片', 6.80, 6.80, '1片', '每日两次', '口服', '3天', 1),
-- PRE008 的明细
('PRE008', 'DRUG001', '阿司匹林肠溶片', 3, '盒', '100mg*30片', 8.50, 25.50, '100mg', '每日一次', '口服', '长期', 1),
('PRE008', 'DRUG005', '氯沙坦钾片', 2, '盒', '50mg*7片', 38.50, 77.00, '50mg', '每日一次', '口服', '14天', 1),
('PRE008', 'DRUG012', '复方丹参滴丸', 1, '瓶', '27mg*180粒', 29.80, 29.80, '270mg', '每日三次', '口服', '30天', 1);

-- 药品库存数据
INSERT INTO `drug_stock` (`stock_id`, `drug_id`, `batch_no`, `production_date`, `expiry_date`, `quantity`, `unit_cost`, `received_date`, `quality_status`) VALUES
('STK001', 'DRUG001', 'B20260301', '2026-03-01', '2029-03-01', 5000, 5.50, '2026-03-05', '合格'),
('STK002', 'DRUG002', 'B20260215', '2026-02-15', '2028-02-15', 3000, 18.00, '2026-02-20', '合格'),
('STK003', 'DRUG003', 'B20260110', '2026-01-10', '2028-01-10', 2000, 32.50, '2026-01-15', '合格'),
('STK004', 'DRUG004', 'B20260320', '2026-03-20', '2029-03-20', 4500, 12.00, '2026-03-25', '合格'),
('STK005', 'DRUG005', 'B20260201', '2026-02-01', '2028-02-01', 2500, 28.00, '2026-02-10', '合格'),
('STK006', 'DRUG006', 'B20260315', '2026-03-15', '2028-03-15', 3500, 15.80, '2026-03-20', '合格'),
('STK007', 'DRUG007', 'B20260120', '2026-01-20', '2028-01-20', 1800, 26.00, '2026-01-25', '合格'),
('STK008', 'DRUG008', 'B20260401', '2026-04-01', '2028-04-01', 6000, 4.20, '2026-04-05', '合格'),
('STK009', 'DRUG009', 'B20260228', '2026-02-28', '2028-02-28', 2000, 12.50, '2026-03-05', '合格'),
('STK010', 'DRUG010', 'B20260115', '2026-01-15', '2028-01-15', 800, 28.00, '2026-01-20', '合格');

-- 药检记录数据
INSERT INTO `drug_test` (`test_id`, `patient_id`, `prescription_id`, `test_type`, `test_item`, `specimen_type`, `specimen_no`, `collect_time`, `test_result`, `reference_value`, `unit`, `result_flag`, `conclusion`, `status`) VALUES
('TEST001', 'PAT001', 'PRE001', '生化检验', '凝血酶原时间(PT)', '血液', 'SP20260401001', '2026-04-01 10:00:00', '14.5', '11.0-15.0', '秒', 'N', '凝血功能正常', 1),
('TEST002', 'PAT001', 'PRE001', '生化检验', '国际标准化比值(INR)', '血液', 'SP20260401002', '2026-04-01 10:00:00', '1.3', '0.8-1.5', '', 'N', 'INR在治疗范围内', 1),
('TEST003', 'PAT002', 'PRE002', '生化检验', '空腹血糖(FPG)', '血液', 'SP20260401003', '2026-04-01 11:00:00', '8.5', '3.9-6.1', 'mmol/L', 'H', '空腹血糖偏高', 1),
('TEST004', 'PAT002', 'PRE002', '生化检验', '糖化血红蛋白(HbA1c)', '血液', 'SP20260401004', '2026-04-01 11:00:00', '7.8', '4.0-6.0', '%', 'H', '血糖控制欠佳', 1),
('TEST005', 'PAT003', 'PRE003', '生化检验', '凝血酶原时间(PT)', '血液', 'SP20260402001', '2026-04-02 09:00:00', '18.2', '11.0-15.0', '秒', 'H', '凝血时间延长', 1),
('TEST006', 'PAT003', 'PRE003', '生化检验', '国际标准化比值(INR)', '血液', 'SP20260402002', '2026-04-02 09:00:00', '1.8', '0.8-1.5', '', 'H', 'INR偏高，需调整剂量', 1),
('TEST007', 'PAT005', 'PRE005', '生化检验', '血常规-白细胞(WBC)', '血液', 'SP20260403001', '2026-04-03 09:30:00', '11.2', '3.5-9.5', '×10^9/L', 'H', '白细胞偏高，提示感染', 1),
('TEST008', 'PAT001', 'PRE006', '生化检验', '空腹血糖(FPG)', '血液', 'SP20260403002', '2026-04-03 11:00:00', '7.2', '3.9-6.1', 'mmol/L', 'H', '空腹血糖偏高', 1);

-- 药品不良反应数据
INSERT INTO `adverse_drug_reaction` (`adr_id`, `patient_id`, `drug_id`, `prescription_id`, `occurrence_time`, `severity_level`, `reaction_type`, `reaction_description`, `outcome`, `handling_measures`, `report_date`) VALUES
('ADR001', 'PAT001', 'DRUG001', 'PRE001', '2026-04-02 08:00:00', '轻度', '胃肠道反应', '服用阿司匹林后出现轻微上腹部不适', '好转', '餐后服用，继续观察', '2026-04-02'),
('ADR002', 'PAT003', 'DRUG013', 'PRE003', '2026-04-03 10:00:00', '中度', '出血倾向', '刷牙时牙龈出血，大便隐血试验阳性', '痊愈', '暂停华法林，调整剂量', '2026-04-03'),
('ADR003', 'PAT004', 'DRUG006', 'PRE004', '2026-04-02 20:00:00', '轻度', '神经系统', '服用奥美拉唑后出现头痛，持续约2小时', '痊愈', '无需特殊处理，可继续用药', '2026-04-03');

-- 药品供应商数据
INSERT INTO `drug_supplier` (`supplier_id`, `supplier_name`, `supplier_type`, `license_no`, `contact_person`, `phone`, `address`, `rating`, `status`) VALUES
('SUP001', '北京华润医药商业集团有限公司', '批发企业', '京AA0100001', '张经理', '010-88886666', '北京市朝阳区东大桥路8号', 5, 1),
('SUP002', '国药控股股份有限公司', '批发企业', '沪AA3100001', '李经理', '021-88889999', '上海市长宁区中山西路1000号', 5, 1),
('SUP003', '上海医药集团股份有限公司', '批发企业', '沪AA3100002', '王经理', '021-88887777', '上海市黄浦区福州路221号', 4, 1),
('SUP004', '九州通医药集团股份有限公司', '批发企业', '鄂AA4200001', '赵经理', '027-88885555', '武汉市汉阳区龙阳大道特8号', 4, 1);

-- 用药指导数据
INSERT INTO `medication_guide` (`guide_id`, `prescription_id`, `patient_id`, `guide_content`, `dietary_advice`, `follow_up_date`, `guide_doctor_id`, `guide_time`, `created_at`) VALUES
('GUIDE001', 'PRE001', 'PAT001', '阿司匹林需长期服用，注意观察有无黑便、牙龈出血等出血倾向。硝苯地平缓释片请勿咬碎，整片吞服。', '避免食用葡萄柚，以免影响药物代谢', '2026-05-01', 'DOC001', '2026-04-01 10:00:00', '2026-04-01 10:00:00'),
('GUIDE002', 'PRE002', 'PAT002', '二甲双胍应随餐服用，以减少胃肠道反应。定期监测血糖，如有低血糖症状及时处理。', '控制饮食总热量，减少高糖高脂食物摄入', '2026-05-01', 'DOC005', '2026-04-01 11:00:00', '2026-04-01 11:00:00'),
('GUIDE003', 'PRE003', 'PAT003', '华法林需定期监测凝血功能(INR)，一般目标范围2.0-3.0。避免突然改变维生素K摄入量。', '保持稳定的绿叶蔬菜摄入量，不要突然大幅增减', '2026-04-16', 'DOC002', '2026-04-02 09:30:00', '2026-04-02 09:30:00');

-- 药品调价记录
INSERT INTO `drug_price_adjust` (`adjust_id`, `drug_id`, `old_price`, `new_price`, `adjust_reason`, `effective_date`, `approval_status`) VALUES
('ADJ001', 'DRUG001', 9.00, 8.50, '国家集采降价', '2026-01-01 00:00:00', '已审批'),
('ADJ002', 'DRUG004', 22.00, 18.90, '国家集采降价', '2026-01-01 00:00:00', '已审批'),
('ADJ003', 'DRUG006', 28.00, 22.30, '药品阳光采购', '2026-03-01 00:00:00', '已审批');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 完成
-- ============================================================
