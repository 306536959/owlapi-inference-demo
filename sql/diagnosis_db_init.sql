-- ============================================================
-- 医院诊断库 MySQL 数据库初始化脚本
-- 数据库名: diagnosis_db
-- 说明: 医院门诊/住院诊断、病历、检查检验记录
-- 创建时间: 2026-04-09
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建数据库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `diagnosis_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `diagnosis_db`;

-- ----------------------------
-- 1. 患者表 (diagnosis_patient) - 诊断库的患者表
-- ----------------------------
DROP TABLE IF EXISTS `diagnosis_patient`;
CREATE TABLE `diagnosis_patient` (
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `patient_name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `gender` VARCHAR(10) NOT NULL COMMENT '性别',
  `birth_date` DATE NOT NULL COMMENT '出生日期',
  `id_card` VARCHAR(18) NOT NULL COMMENT '身份证号',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `address` VARCHAR(300) DEFAULT NULL COMMENT '住址',
  `occupation` VARCHAR(100) DEFAULT NULL COMMENT '职业',
  `nation` VARCHAR(50) DEFAULT NULL COMMENT '民族',
  `marital_status` VARCHAR(20) DEFAULT NULL COMMENT '婚姻状况',
  `emergency_contact` VARCHAR(50) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` VARCHAR(20) DEFAULT NULL COMMENT '紧急联系电话',
  `blood_type` VARCHAR(10) DEFAULT NULL COMMENT '血型',
  `rh_factor` VARCHAR(10) DEFAULT NULL COMMENT 'Rh因子',
  `allergy_history` TEXT DEFAULT NULL COMMENT '过敏史',
  `family_history` TEXT DEFAULT NULL COMMENT '家族史',
  `smoking_history` VARCHAR(50) DEFAULT NULL COMMENT '吸烟史',
  `drinking_history` VARCHAR(50) DEFAULT NULL COMMENT '饮酒史',
  `registration_date` DATE NOT NULL COMMENT '建档日期',
  `id_card_front` VARCHAR(200) DEFAULT NULL COMMENT '身份证正面图片路径',
  `id_card_back` VARCHAR(200) DEFAULT NULL COMMENT '身份证背面图片路径',
  `photo` VARCHAR(200) DEFAULT NULL COMMENT '患者照片路径',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0注销 1正常',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`patient_id`),
  UNIQUE KEY `uk_id_card` (`id_card`),
  KEY `idx_name` (`patient_name`),
  KEY `idx_phone` (`phone`),
  KEY `idx_birth` (`birth_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者信息表(诊断库)';

-- ----------------------------
-- 2. 门诊病历表 (outpatient_record)
-- ----------------------------
DROP TABLE IF EXISTS `outpatient_record`;
CREATE TABLE `outpatient_record` (
  `record_id` VARCHAR(20) NOT NULL COMMENT '病历ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `visit_no` VARCHAR(20) NOT NULL COMMENT '就诊号',
  `visit_date` DATETIME NOT NULL COMMENT '就诊日期',
  `dept_id` VARCHAR(20) NOT NULL COMMENT '科室ID',
  `doctor_id` VARCHAR(20) NOT NULL COMMENT '医生ID',
  `chief_complaint` TEXT NOT NULL COMMENT '主诉',
  `present_illness` TEXT DEFAULT NULL COMMENT '现病史',
  `past_history` TEXT DEFAULT NULL COMMENT '既往史',
  `personal_history` TEXT DEFAULT NULL COMMENT '个人史',
  `family_history` TEXT DEFAULT NULL COMMENT '家族史',
  `marriage_history` TEXT DEFAULT NULL COMMENT '婚姻史',
  `menstrual_history` TEXT DEFAULT NULL COMMENT '月经史',
  `physical_examination` TEXT DEFAULT NULL COMMENT '体格检查',
  `preliminary_diagnosis` TEXT DEFAULT NULL COMMENT '初步诊断',
  `final_diagnosis` TEXT DEFAULT NULL COMMENT '最终诊断',
  `diagnosis_codes` VARCHAR(200) DEFAULT NULL COMMENT '诊断代码(ICD-10)',
  `treatment_plan` TEXT DEFAULT NULL COMMENT '治疗方案',
  `advice` TEXT DEFAULT NULL COMMENT '医嘱',
  `visit_type` VARCHAR(20) DEFAULT NULL COMMENT '就诊类型: 初诊/复诊',
  `is_first_visit` TINYINT DEFAULT 1 COMMENT '是否首诊',
  `attending_flag` TINYINT DEFAULT 0 COMMENT '是否科主任查房',
  `record_status` VARCHAR(20) DEFAULT '已归档' COMMENT '病历状态',
  `medical_notes` TEXT DEFAULT NULL COMMENT '医疗备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_visit_date` (`visit_date`),
  KEY `idx_dept` (`dept_id`),
  KEY `idx_doctor` (`doctor_id`),
  KEY `idx_visit_no` (`visit_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门诊病历表';

-- ----------------------------
-- 3. 诊断记录表 (diagnosis)
-- ----------------------------
DROP TABLE IF EXISTS `diagnosis`;
CREATE TABLE `diagnosis` (
  `diagnosis_id` VARCHAR(20) NOT NULL COMMENT '诊断ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `record_id` VARCHAR(20) DEFAULT NULL COMMENT '关联病历ID',
  `visit_no` VARCHAR(20) DEFAULT NULL COMMENT '就诊号',
  `diagnosis_type` VARCHAR(20) NOT NULL COMMENT '诊断类型: 西医/中医',
  `diagnosis_level` VARCHAR(20) NOT NULL COMMENT '诊断级别: 主要诊断/次要诊断/并发症',
  `diagnosis_code` VARCHAR(50) DEFAULT NULL COMMENT 'ICD-10编码',
  `diagnosis_name` VARCHAR(200) NOT NULL COMMENT '诊断名称',
  `diagnosis_subname` VARCHAR(200) DEFAULT NULL COMMENT '诊断亚型',
  `diagnosis_date` DATETIME NOT NULL COMMENT '诊断日期',
  `dept_id` VARCHAR(20) DEFAULT NULL COMMENT '诊断科室',
  `doctor_id` VARCHAR(20) NOT NULL COMMENT '诊断医生',
  `disease_type` VARCHAR(50) DEFAULT NULL COMMENT '疾病分类',
  `disease_severity` VARCHAR(20) DEFAULT NULL COMMENT '疾病严重程度',
  `disease_stage` VARCHAR(50) DEFAULT NULL COMMENT '疾病分期',
  `pathology_diagnosis` TEXT DEFAULT NULL COMMENT '病理诊断',
  `diagnosis_basis` TEXT DEFAULT NULL COMMENT '诊断依据',
  `differential_diagnosis` TEXT DEFAULT NULL COMMENT '鉴别诊断',
  `is_confirmed` TINYINT DEFAULT 1 COMMENT '是否确诊',
  `onset_date` DATE DEFAULT NULL COMMENT '发病日期',
  `admission_date` DATE DEFAULT NULL COMMENT '入院日期',
  `discharge_date` DATE DEFAULT NULL COMMENT '出院日期',
  `treatment_days` INT DEFAULT NULL COMMENT '治疗天数',
  `outcome` VARCHAR(50) DEFAULT NULL COMMENT '治疗结果',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0撤销 1有效',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`diagnosis_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_diagnosis_date` (`diagnosis_date`),
  KEY `idx_diagnosis_code` (`diagnosis_code`),
  KEY `idx_doctor` (`doctor_id`),
  KEY `idx_visit_no` (`visit_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诊断记录表';

-- ----------------------------
-- 4. 诊断详情表 (diagnosis_detail)
-- ----------------------------
DROP TABLE IF EXISTS `diagnosis_detail`;
CREATE TABLE `diagnosis_detail` (
  `detail_id` VARCHAR(20) NOT NULL COMMENT '详情ID',
  `diagnosis_id` VARCHAR(20) NOT NULL COMMENT '诊断ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `symptom_name` VARCHAR(100) NOT NULL COMMENT '症状名称',
  `symptom_duration` VARCHAR(50) DEFAULT NULL COMMENT '症状持续时间',
  `symptom_description` TEXT DEFAULT NULL COMMENT '症状描述',
  `symptom_severity` VARCHAR(20) DEFAULT NULL COMMENT '严重程度',
  `body_part` VARCHAR(100) DEFAULT NULL COMMENT '身体部位',
  `onset_pattern` VARCHAR(50) DEFAULT NULL COMMENT '发作形式',
  `related_factor` TEXT DEFAULT NULL COMMENT '相关因素',
  `physical_sign` VARCHAR(200) DEFAULT NULL COMMENT '体征',
  `exam_result` TEXT DEFAULT NULL COMMENT '检查结果',
  `treatment_response` TEXT DEFAULT NULL COMMENT '治疗反应',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`detail_id`),
  KEY `idx_diagnosis` (`diagnosis_id`),
  KEY `idx_patient` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诊断详情表';

-- ----------------------------
-- 5. 检查申请单表 (exam_request)
-- ----------------------------
DROP TABLE IF EXISTS `exam_request`;
CREATE TABLE `exam_request` (
  `request_id` VARCHAR(20) NOT NULL COMMENT '申请单ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `record_id` VARCHAR(20) DEFAULT NULL COMMENT '关联病历ID',
  `visit_no` VARCHAR(20) DEFAULT NULL COMMENT '就诊号',
  `request_date` DATETIME NOT NULL COMMENT '申请日期',
  `dept_id` VARCHAR(20) NOT NULL COMMENT '申请科室',
  `doctor_id` VARCHAR(20) NOT NULL COMMENT '申请医生',
  `exam_dept_id` VARCHAR(20) DEFAULT NULL COMMENT '检查科室',
  `exam_item_code` VARCHAR(50) NOT NULL COMMENT '检查项目代码',
  `exam_item_name` VARCHAR(200) NOT NULL COMMENT '检查项目名称',
  `exam_category` VARCHAR(50) DEFAULT NULL COMMENT '检查类别',
  `clinical_diagnosis` VARCHAR(200) DEFAULT NULL COMMENT '临床诊断',
  `exam_reason` TEXT DEFAULT NULL COMMENT '检查目的',
  `specimen_type` VARCHAR(50) DEFAULT NULL COMMENT '标本类型',
  `urgent_flag` TINYINT DEFAULT 0 COMMENT '是否加急',
  `fee_item_id` VARCHAR(20) DEFAULT NULL COMMENT '收费项目ID',
  `fee_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '费用金额',
  `payment_status` VARCHAR(20) DEFAULT '待缴费' COMMENT '缴费状态',
  `exam_status` VARCHAR(20) DEFAULT '待检查' COMMENT '检查状态',
  `appointment_date` DATETIME DEFAULT NULL COMMENT '预约时间',
  `exam_date` DATETIME DEFAULT NULL COMMENT '检查时间',
  `report_date` DATETIME DEFAULT NULL COMMENT '报告时间',
  `report_doctor_id` VARCHAR(20) DEFAULT NULL COMMENT '报告医生',
  `report_content` TEXT DEFAULT NULL COMMENT '报告内容',
  `report_conclusion` TEXT DEFAULT NULL COMMENT '报告结论',
  `report_attachment` VARCHAR(500) DEFAULT NULL COMMENT '报告附件路径',
  `reviewer_id` VARCHAR(20) DEFAULT NULL COMMENT '审核医生',
  `review_date` DATETIME DEFAULT NULL COMMENT '审核时间',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`request_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_request_date` (`request_date`),
  KEY `idx_exam_item` (`exam_item_code`),
  KEY `idx_status` (`exam_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查申请单表';

-- ----------------------------
-- 6. 检验记录表 (lab_test)
-- ----------------------------
DROP TABLE IF EXISTS `lab_test`;
CREATE TABLE `lab_test` (
  `test_id` VARCHAR(20) NOT NULL COMMENT '检验ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `record_id` VARCHAR(20) DEFAULT NULL COMMENT '关联病历ID',
  `visit_no` VARCHAR(20) DEFAULT NULL COMMENT '就诊号',
  `request_id` VARCHAR(20) DEFAULT NULL COMMENT '关联申请ID',
  `request_date` DATETIME DEFAULT NULL COMMENT '申请日期',
  `dept_id` VARCHAR(20) NOT NULL COMMENT '申请科室',
  `doctor_id` VARCHAR(20) DEFAULT NULL COMMENT '申请医生',
  `test_category` VARCHAR(50) NOT NULL COMMENT '检验类别',
  `test_item_code` VARCHAR(50) NOT NULL COMMENT '检验项目代码',
  `test_item_name` VARCHAR(200) NOT NULL COMMENT '检验项目名称',
  `specimen_type` VARCHAR(50) NOT NULL COMMENT '标本类型',
  `specimen_no` VARCHAR(50) DEFAULT NULL COMMENT '标本编号',
  `collect_time` DATETIME DEFAULT NULL COMMENT '采集时间',
  `receive_time` DATETIME DEFAULT NULL COMMENT '接收时间',
  `report_time` DATETIME DEFAULT NULL COMMENT '报告时间',
  `test_result` VARCHAR(200) DEFAULT NULL COMMENT '检验结果',
  `result_value` VARCHAR(100) DEFAULT NULL COMMENT '结果数值',
  `unit` VARCHAR(50) DEFAULT NULL COMMENT '单位',
  `reference_value` VARCHAR(100) DEFAULT NULL COMMENT '参考值范围',
  `result_flag` VARCHAR(10) DEFAULT NULL COMMENT '结果标志: H/L/N',
  `instrument` VARCHAR(100) DEFAULT NULL COMMENT '检测仪器',
  `reagent` VARCHAR(100) DEFAULT NULL COMMENT '检测试剂',
  `tester_id` VARCHAR(20) DEFAULT NULL COMMENT '检验者',
  `reviewer_id` VARCHAR(20) DEFAULT NULL COMMENT '审核者',
  `report_status` VARCHAR(20) DEFAULT '待审核' COMMENT '报告状态',
  `critical_value_alert` TINYINT DEFAULT 0 COMMENT '危急值预警',
  `clinical_meaning` TEXT DEFAULT NULL COMMENT '临床意义',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`test_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_request` (`request_id`),
  KEY `idx_test_item` (`test_item_code`),
  KEY `idx_collect_time` (`collect_time`),
  KEY `idx_report_time` (`report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验记录表';

-- ----------------------------
-- 7. 住院记录表 (hospitalization)
-- ----------------------------
DROP TABLE IF EXISTS `hospitalization`;
CREATE TABLE `hospitalization` (
  `hospital_id` VARCHAR(20) NOT NULL COMMENT '住院ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `inpatient_no` VARCHAR(20) NOT NULL COMMENT '住院号',
  `admission_date` DATETIME NOT NULL COMMENT '入院日期',
  `dept_id` VARCHAR(20) NOT NULL COMMENT '入院科室',
  `ward_id` VARCHAR(20) DEFAULT NULL COMMENT '病区ID',
  `bed_no` VARCHAR(20) DEFAULT NULL COMMENT '床位号',
  `admission_diagnosis` VARCHAR(200) NOT NULL COMMENT '入院诊断',
  `admission_diagnosis_code` VARCHAR(50) DEFAULT NULL COMMENT '入院诊断代码',
  `attending_doctor_id` VARCHAR(20) NOT NULL COMMENT '主治医生',
  `chief_doctor_id` VARCHAR(20) DEFAULT NULL COMMENT '主任医生',
  `resident_doctor_id` VARCHAR(20) DEFAULT NULL COMMENT '住院医生',
  `nurse_id` VARCHAR(20) DEFAULT NULL COMMENT '责任护士',
  `admission_way` VARCHAR(50) DEFAULT NULL COMMENT '入院方式',
  `admission_condition` VARCHAR(50) DEFAULT NULL COMMENT '入院情况',
  `discharge_date` DATETIME DEFAULT NULL COMMENT '出院日期',
  `discharge_dept_id` VARCHAR(20) DEFAULT NULL COMMENT '出院科室',
  `discharge_diagnosis` VARCHAR(200) DEFAULT NULL COMMENT '出院诊断',
  `discharge_diagnosis_code` VARCHAR(50) DEFAULT NULL COMMENT '出院诊断代码',
  `operation_count` INT DEFAULT 0 COMMENT '手术次数',
  `hospital_days` INT DEFAULT NULL COMMENT '住院天数',
  `treatment_outcome` VARCHAR(50) DEFAULT NULL COMMENT '治疗结果',
  `discharge_way` VARCHAR(50) DEFAULT NULL COMMENT '出院方式',
  `medical_expense` DECIMAL(12,2) DEFAULT NULL COMMENT '医疗费用',
  `bed_fee` DECIMAL(10,2) DEFAULT NULL COMMENT '床位费',
  `nursing_fee` DECIMAL(10,2) DEFAULT NULL COMMENT '护理费',
  `drug_fee` DECIMAL(10,2) DEFAULT NULL COMMENT '药费',
  `exam_fee` DECIMAL(10,2) DEFAULT NULL COMMENT '检查费',
  `operation_fee` DECIMAL(10,2) DEFAULT NULL COMMENT '手术费',
  `other_fee` DECIMAL(10,2) DEFAULT NULL COMMENT '其他费用',
  `insurance_payment` DECIMAL(12,2) DEFAULT NULL COMMENT '医保支付',
  `self_payment` DECIMAL(12,2) DEFAULT NULL COMMENT '自付金额',
  `hospital_summary` TEXT DEFAULT NULL COMMENT '出院小结',
  `follow_up_plan` TEXT DEFAULT NULL COMMENT '随访计划',
  `admission_notes` TEXT DEFAULT NULL COMMENT '入院备注',
  `status` VARCHAR(20) DEFAULT '住院中' COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`hospital_id`),
  UNIQUE KEY `uk_inpatient_no` (`inpatient_no`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_admission_date` (`admission_date`),
  KEY `idx_dept` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='住院记录表';

-- ----------------------------
-- 8. 病程记录表 (clinical_progress)
-- ----------------------------
DROP TABLE IF EXISTS `clinical_progress`;
CREATE TABLE `clinical_progress` (
  `progress_id` VARCHAR(20) NOT NULL COMMENT '病程ID',
  `hospital_id` VARCHAR(20) NOT NULL COMMENT '住院ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `record_date` DATETIME NOT NULL COMMENT '记录日期',
  `record_type` VARCHAR(50) NOT NULL COMMENT '记录类型: 日常病程/上级查房/疑难病例/手术前讨论',
  `record_title` VARCHAR(200) DEFAULT NULL COMMENT '记录标题',
  `chief_complaint` TEXT DEFAULT NULL COMMENT '主诉',
  `current_condition` TEXT DEFAULT NULL COMMENT '目前情况',
  `vital_signs` TEXT DEFAULT NULL COMMENT '生命体征',
  `physical_examination` TEXT DEFAULT NULL COMMENT '体格检查',
  `auxiliary_exam` TEXT DEFAULT NULL COMMENT '辅助检查',
  `diagnosis_changes` TEXT DEFAULT NULL COMMENT '诊断变更',
  `treatment_plan` TEXT DEFAULT NULL COMMENT '处理意见',
  `doctor_id` VARCHAR(20) NOT NULL COMMENT '记录医生',
  `doctor_title` VARCHAR(50) DEFAULT NULL COMMENT '医生职称',
  `is_chief_visit` TINYINT DEFAULT 0 COMMENT '是否主任查房',
  `next_visit_date` DATE DEFAULT NULL COMMENT '下次查房日期',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`progress_id`),
  KEY `idx_hospital` (`hospital_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_record_date` (`record_date`),
  KEY `idx_record_type` (`record_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病程记录表';

-- ----------------------------
-- 9. 手术记录表 (operation_record)
-- ----------------------------
DROP TABLE IF EXISTS `operation_record`;
CREATE TABLE `operation_record` (
  `operation_id` VARCHAR(20) NOT NULL COMMENT '手术ID',
  `hospital_id` VARCHAR(20) DEFAULT NULL COMMENT '住院ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `record_id` VARCHAR(20) DEFAULT NULL COMMENT '关联病历ID',
  `operation_no` VARCHAR(50) NOT NULL COMMENT '手术编号',
  `operation_date` DATETIME NOT NULL COMMENT '手术日期',
  `operation_name` VARCHAR(200) NOT NULL COMMENT '手术名称',
  `operation_code` VARCHAR(50) DEFAULT NULL COMMENT '手术代码',
  `operation_level` VARCHAR(20) DEFAULT NULL COMMENT '手术等级',
  `operation_type` VARCHAR(50) DEFAULT NULL COMMENT '手术类型',
  `emergency_level` VARCHAR(20) DEFAULT '择期' COMMENT '紧急程度',
  `dept_id` VARCHAR(20) NOT NULL COMMENT '手术科室',
  `operation_room` VARCHAR(50) DEFAULT NULL COMMENT '手术室',
  `surgeon_id` VARCHAR(20) NOT NULL COMMENT '主刀医生',
  `first_assistant_id` VARCHAR(20) DEFAULT NULL COMMENT '第一助手',
  `second_assistant_id` VARCHAR(20) DEFAULT NULL COMMENT '第二助手',
  `anesthesiologist_id` VARCHAR(20) DEFAULT NULL COMMENT '麻醉医生',
  `nurse_id` VARCHAR(20) DEFAULT NULL COMMENT '手术护士',
  `anesthesia_method` VARCHAR(50) DEFAULT NULL COMMENT '麻醉方式',
  `pre_diagnosis` VARCHAR(200) DEFAULT NULL COMMENT '术前诊断',
  `post_diagnosis` VARCHAR(200) DEFAULT NULL COMMENT '术后诊断',
  `operation_position` TEXT DEFAULT NULL COMMENT '手术体位',
  `incision_type` VARCHAR(100) DEFAULT NULL COMMENT '切口类型',
  `operation_procedure` TEXT NOT NULL COMMENT '手术经过',
  `finding` TEXT DEFAULT NULL COMMENT '术中所见',
  `resection_ extent` VARCHAR(200) DEFAULT NULL COMMENT '切除范围',
  `blood_loss` VARCHAR(50) DEFAULT NULL COMMENT '出血量',
  `transfusion` VARCHAR(100) DEFAULT NULL COMMENT '输血情况',
  `anesthesia_begin` DATETIME DEFAULT NULL COMMENT '麻醉开始时间',
  `operation_begin` DATETIME DEFAULT NULL COMMENT '手术开始时间',
  `operation_end` DATETIME DEFAULT NULL COMMENT '手术结束时间',
  `anesthesia_end` DATETIME DEFAULT NULL COMMENT '麻醉结束时间',
  `operation_duration` INT DEFAULT NULL COMMENT '手术时长(分钟)',
  `intraoperative_event` TEXT DEFAULT NULL COMMENT '术中事件',
  `postoperative_plan` TEXT DEFAULT NULL COMMENT '术后计划',
  `complication` VARCHAR(200) DEFAULT NULL COMMENT '并发症',
  `implants` TEXT DEFAULT NULL COMMENT '植入物',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`operation_id`),
  KEY `idx_hospital` (`hospital_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_operation_date` (`operation_date`),
  KEY `idx_surgeon` (`surgeon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手术记录表';

-- ----------------------------
-- 10. 治疗记录表 (treatment_record)
-- ----------------------------
DROP TABLE IF EXISTS `treatment_record`;
CREATE TABLE `treatment_record` (
  `treatment_id` VARCHAR(20) NOT NULL COMMENT '治疗ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `hospital_id` VARCHAR(20) DEFAULT NULL COMMENT '住院ID',
  `record_id` VARCHAR(20) DEFAULT NULL COMMENT '关联病历ID',
  `visit_no` VARCHAR(20) DEFAULT NULL COMMENT '就诊号',
  `treatment_date` DATETIME NOT NULL COMMENT '治疗日期',
  `treatment_type` VARCHAR(50) NOT NULL COMMENT '治疗类型',
  `treatment_item_code` VARCHAR(50) DEFAULT NULL COMMENT '治疗项目代码',
  `treatment_item_name` VARCHAR(200) NOT NULL COMMENT '治疗项目名称',
  `treatment_content` TEXT DEFAULT NULL COMMENT '治疗内容',
  `treatment_dosage` VARCHAR(100) DEFAULT NULL COMMENT '治疗剂量',
  `treatment_duration` VARCHAR(100) DEFAULT NULL COMMENT '治疗时长',
  `treatment_times` INT DEFAULT 1 COMMENT '治疗次数',
  `dept_id` VARCHAR(20) DEFAULT NULL COMMENT '执行科室',
  `doctor_id` VARCHAR(20) DEFAULT NULL COMMENT '开单医生',
  `executer_id` VARCHAR(20) DEFAULT NULL COMMENT '执行人',
  `execution_time` DATETIME DEFAULT NULL COMMENT '执行时间',
  `treatment_response` TEXT DEFAULT NULL COMMENT '治疗反应',
  `next_treatment_date` DATETIME DEFAULT NULL COMMENT '下次治疗时间',
  `treatment_result` VARCHAR(50) DEFAULT NULL COMMENT '治疗结果',
  `fee_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '费用金额',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`treatment_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_hospital` (`hospital_id`),
  KEY `idx_treatment_date` (`treatment_date`),
  KEY `idx_treatment_type` (`treatment_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='治疗记录表';

-- ----------------------------
-- 11. 会诊记录表 (consultation)
-- ----------------------------
DROP TABLE IF EXISTS `consultation`;
CREATE TABLE `consultation` (
  `consultation_id` VARCHAR(20) NOT NULL COMMENT '会诊ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `hospital_id` VARCHAR(20) DEFAULT NULL COMMENT '住院ID',
  `record_id` VARCHAR(20) DEFAULT NULL COMMENT '关联病历ID',
  `consultation_no` VARCHAR(50) NOT NULL COMMENT '会诊编号',
  `request_dept_id` VARCHAR(20) NOT NULL COMMENT '申请科室',
  `request_doctor_id` VARCHAR(20) NOT NULL COMMENT '申请医生',
  `request_date` DATETIME NOT NULL COMMENT '申请日期',
  `consulted_dept_id` VARCHAR(20) NOT NULL COMMENT '被邀科室',
  `consulted_doctor_id` VARCHAR(20) DEFAULT NULL COMMENT '会诊医生',
  `consultation_type` VARCHAR(50) NOT NULL COMMENT '会诊类型: 普通会诊/急会诊/多学科会诊',
  `consultation_date` DATETIME DEFAULT NULL COMMENT '会诊日期',
  `clinical_diagnosis` VARCHAR(200) DEFAULT NULL COMMENT '临床诊断',
  `consultation_purpose` TEXT NOT NULL COMMENT '会诊目的',
  `patient_condition` TEXT DEFAULT NULL COMMENT '患者情况',
  `examination_results` TEXT DEFAULT NULL COMMENT '检查结果',
  `consultation_opinion` TEXT DEFAULT NULL COMMENT '会诊意见',
  `treatment_suggestion` TEXT DEFAULT NULL COMMENT '治疗建议',
  `follow_up_plan` TEXT DEFAULT NULL COMMENT '随访计划',
  `consultation_status` VARCHAR(20) DEFAULT '待会诊' COMMENT '会诊状态',
  `response_time` INT DEFAULT NULL COMMENT '响应时间(小时)',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`consultation_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_hospital` (`hospital_id`),
  KEY `idx_request_date` (`request_date`),
  KEY `idx_status` (`consultation_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会诊记录表';

-- ----------------------------
-- 12. 疾病编码表 (disease_code)
-- ----------------------------
DROP TABLE IF EXISTS `disease_code`;
CREATE TABLE `disease_code` (
  `code_id` VARCHAR(20) NOT NULL COMMENT '编码ID',
  `icd_code` VARCHAR(50) NOT NULL COMMENT 'ICD-10编码',
  `disease_name` VARCHAR(200) NOT NULL COMMENT '疾病名称',
  `disease_type` VARCHAR(50) DEFAULT NULL COMMENT '疾病分类',
  `disease_category` VARCHAR(100) DEFAULT NULL COMMENT '疾病大类',
  `related_dept` VARCHAR(200) DEFAULT NULL COMMENT '相关科室',
  `disease_description` TEXT DEFAULT NULL COMMENT '疾病描述',
  `inclusion_criteria` TEXT DEFAULT NULL COMMENT '纳入标准',
  `exclusion_criteria` TEXT DEFAULT NULL COMMENT '排除标准',
  `treatment_principle` TEXT DEFAULT NULL COMMENT '治疗原则',
  `prognosis_info` TEXT DEFAULT NULL COMMENT '预后信息',
  `is_chronic` TINYINT DEFAULT 0 COMMENT '是否慢性病',
  `is_infectious` TINYINT DEFAULT 0 COMMENT '是否传染病',
  `is_serious` TINYINT DEFAULT 0 COMMENT '是否重大疾病',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`code_id`),
  UNIQUE KEY `uk_icd_code` (`icd_code`),
  KEY `idx_disease_name` (`disease_name`),
  KEY `idx_disease_type` (`disease_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='疾病编码表';

-- ----------------------------
-- 13. ICD-10手术编码表 (operation_code)
-- ----------------------------
DROP TABLE IF EXISTS `operation_code`;
CREATE TABLE `operation_code` (
  `code_id` VARCHAR(20) NOT NULL COMMENT '编码ID',
  `icd9_code` VARCHAR(50) NOT NULL COMMENT 'ICD-9-CM-3编码',
  `operation_name` VARCHAR(300) NOT NULL COMMENT '手术名称',
  `operation_level` VARCHAR(20) DEFAULT NULL COMMENT '手术等级',
  `operation_category` VARCHAR(100) DEFAULT NULL COMMENT '手术分类',
  `body_system` VARCHAR(100) DEFAULT NULL COMMENT '人体系统',
  `anesthesia_type` VARCHAR(50) DEFAULT NULL COMMENT '常用麻醉方式',
  `operation_incision` VARCHAR(50) DEFAULT NULL COMMENT '切口等级',
  `is_minimally_invasive` TINYINT DEFAULT 0 COMMENT '是否微创手术',
  `operation_duration_avg` INT DEFAULT NULL COMMENT '平均手术时长(分钟)',
  `blood_loss_avg` VARCHAR(50) DEFAULT NULL COMMENT '平均出血量',
  `related_diseases` TEXT DEFAULT NULL COMMENT '相关疾病',
  `complication_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '并发症发生率(%)',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`code_id`),
  UNIQUE KEY `uk_icd9_code` (`icd9_code`),
  KEY `idx_operation_name` (`operation_name`),
  KEY `idx_operation_level` (`operation_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ICD-10手术编码表';

-- ----------------------------
-- 14. 随访记录表 (follow_up)
-- ----------------------------
DROP TABLE IF EXISTS `follow_up`;
CREATE TABLE `follow_up` (
  `follow_up_id` VARCHAR(20) NOT NULL COMMENT '随访ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `diagnosis_id` VARCHAR(20) DEFAULT NULL COMMENT '关联诊断ID',
  `hospital_id` VARCHAR(20) DEFAULT NULL COMMENT '关联住院ID',
  `visit_no` VARCHAR(20) DEFAULT NULL COMMENT '关联就诊号',
  `follow_up_no` VARCHAR(50) NOT NULL COMMENT '随访编号',
  `follow_up_type` VARCHAR(50) NOT NULL COMMENT '随访类型: 门诊随访/电话随访/入户随访',
  `follow_up_date` DATETIME NOT NULL COMMENT '随访日期',
  `dept_id` VARCHAR(20) DEFAULT NULL COMMENT '随访科室',
  `doctor_id` VARCHAR(20) DEFAULT NULL COMMENT '随访医生',
  `follow_up_reason` TEXT DEFAULT NULL COMMENT '随访原因',
  `current_symptoms` TEXT DEFAULT NULL COMMENT '目前症状',
  `medication_compliance` VARCHAR(50) DEFAULT NULL COMMENT '用药依从性',
  `side_effects` TEXT DEFAULT NULL COMMENT '不良反应',
  `lifestyle_status` TEXT DEFAULT NULL COMMENT '生活方式状况',
  `vital_signs` TEXT DEFAULT NULL COMMENT '生命体征',
  `physical_examination` TEXT DEFAULT NULL COMMENT '体格检查',
  `lab_results` TEXT DEFAULT NULL COMMENT '检验结果',
  `imaging_results` TEXT DEFAULT NULL COMMENT '影像学结果',
  `disease_control_status` VARCHAR(50) DEFAULT NULL COMMENT '疾病控制状况',
  `treatment_effect` VARCHAR(50) DEFAULT NULL COMMENT '治疗效果',
  `next_follow_up_date` DATE DEFAULT NULL COMMENT '下次随访日期',
  `next_follow_up_type` VARCHAR(50) DEFAULT NULL COMMENT '下次随访方式',
  `follow_up_notes` TEXT DEFAULT NULL COMMENT '随访备注',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`follow_up_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_follow_up_date` (`follow_up_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访记录表';

-- ----------------------------
-- 15. 电子病历模板表 (emr_template)
-- ----------------------------
DROP TABLE IF EXISTS `emr_template`;
CREATE TABLE `emr_template` (
  `template_id` VARCHAR(20) NOT NULL COMMENT '模板ID',
  `template_name` VARCHAR(200) NOT NULL COMMENT '模板名称',
  `template_type` VARCHAR(50) NOT NULL COMMENT '模板类型',
  `template_category` VARCHAR(100) DEFAULT NULL COMMENT '模板分类',
  `dept_id` VARCHAR(20) DEFAULT NULL COMMENT '适用科室',
  `applicable_disease` VARCHAR(200) DEFAULT NULL COMMENT '适用疾病',
  `template_content` TEXT NOT NULL COMMENT '模板内容',
  `field_definitions` TEXT DEFAULT NULL COMMENT '字段定义(JSON)',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认模板',
  `usage_count` INT DEFAULT 0 COMMENT '使用次数',
  `author_id` VARCHAR(20) DEFAULT NULL COMMENT '创建人',
  `approval_status` VARCHAR(20) DEFAULT '待审核' COMMENT '审核状态',
  `effective_date` DATE DEFAULT NULL COMMENT '生效日期',
  `expiry_date` DATE DEFAULT NULL COMMENT '失效日期',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_id`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_dept` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子病历模板表';

-- ----------------------------
-- 16. 病历质控记录表 (quality_control)
-- ----------------------------
DROP TABLE IF EXISTS `quality_control`;
CREATE TABLE `quality_control` (
  `qc_id` VARCHAR(20) NOT NULL COMMENT '质控ID',
  `patient_id` VARCHAR(20) NOT NULL COMMENT '患者ID',
  `hospital_id` VARCHAR(20) DEFAULT NULL COMMENT '住院ID',
  `record_id` VARCHAR(20) DEFAULT NULL COMMENT '关联病历ID',
  `qc_no` VARCHAR(50) NOT NULL COMMENT '质控编号',
  `qc_type` VARCHAR(50) NOT NULL COMMENT '质控类型',
  `qc_date` DATETIME NOT NULL COMMENT '质控日期',
  `qc_dept_id` VARCHAR(20) DEFAULT NULL COMMENT '质控科室',
  `qc_doctor_id` VARCHAR(20) NOT NULL COMMENT '质控医生',
  `record_completeness` DECIMAL(5,2) DEFAULT NULL COMMENT '病历完整性(%)',
  `diagnosis_accuracy` DECIMAL(5,2) DEFAULT NULL COMMENT '诊断准确率(%)',
  `treatment_standardization` DECIMAL(5,2) DEFAULT NULL COMMENT '治疗规范性(%)',
  `informed_consent` TINYINT DEFAULT NULL COMMENT '知情同意书是否完善',
  `emr_timeliness` TINYINT DEFAULT NULL COMMENT '病历时效性',
  `qc_items` TEXT DEFAULT NULL COMMENT '质控项目明细',
  `defects_found` TEXT DEFAULT NULL COMMENT '发现的缺陷',
  `defect_level` VARCHAR(20) DEFAULT NULL COMMENT '缺陷等级',
  `correction_request` TEXT DEFAULT NULL COMMENT '整改要求',
  `correction_status` VARCHAR(20) DEFAULT '待整改' COMMENT '整改状态',
  `correction_deadline` DATE DEFAULT NULL COMMENT '整改期限',
  `correction_result` TEXT DEFAULT NULL COMMENT '整改结果',
  `correction_date` DATE DEFAULT NULL COMMENT '整改日期',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`qc_id`),
  KEY `idx_patient` (`patient_id`),
  KEY `idx_hospital` (`hospital_id`),
  KEY `idx_qc_date` (`qc_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病历质控记录表';

-- ============================================================
-- 插入测试数据
-- ============================================================

-- 患者数据
INSERT INTO `diagnosis_patient` (`patient_id`, `patient_name`, `gender`, `birth_date`, `id_card`, `phone`, `address`, `occupation`, `blood_type`, `allergy_history`, `smoking_history`, `drinking_history`, `registration_date`, `status`) VALUES
('PAT001', '李伟', '男', '1965-05-20', '110101196505201234', '13900001001', '北京市朝阳区建国路88号', '工程师', 'A型', '青霉素过敏', '已戒烟5年', '偶尔饮酒', '2018-03-15', 1),
('PAT002', '王芳', '女', '1972-08-15', '110101197208151567', '13900001003', '北京市海淀区中关村大街1号', '教师', 'O型', '无', '不吸烟', '偶尔饮酒', '2019-06-20', 1),
('PAT003', '张三', '男', '1958-12-03', '110101195812032891', '13900001005', '北京市东城区王府井大街50号', '退休', 'B型', '磺胺类药物过敏', '吸烟30年', '经常饮酒', '2017-01-10', 1),
('PAT004', '李梅', '女', '1985-03-28', '110101198503281234', '13900001007', '北京市西城区西单大街30号', '会计', 'AB型', '无', '不吸烟', '不饮酒', '2020-02-14', 1),
('PAT005', '赵军', '男', '1970-07-22', '110101197007221567', '13900001009', '北京市丰台区南三环路88号', '商人', 'A型', '对海鲜过敏', '已戒烟', '经常饮酒', '2018-11-05', 1),
('PAT006', '陈红', '女', '1990-01-15', '110101199001152891', '13900001011', '北京市石景山区石景山路100号', '护士', 'O型', '无', '不吸烟', '偶尔饮酒', '2021-05-08', 1),
('PAT007', '刘强', '男', '1955-09-10', '110101195509101234', '13900001013', '北京市通州区新华大街88号', '退休', 'B型', '头孢类药物过敏', '已戒烟10年', '偶尔饮酒', '2016-08-22', 1),
('PAT008', '孙丽', '女', '1988-04-05', '110101198804052345', '13900001015', '北京市昌平区回龙观大街50号', '工程师', 'A型', '无', '不吸烟', '不饮酒', '2022-01-18', 1);

-- 门诊病历数据
INSERT INTO `outpatient_record` (`record_id`, `patient_id`, `visit_no`, `visit_date`, `dept_id`, `doctor_id`, `chief_complaint`, `present_illness`, `past_history`, `physical_examination`, `preliminary_diagnosis`, `diagnosis_codes`, `visit_type`, `is_first_visit`) VALUES
('REC001', 'PAT001', 'V20260401001', '2026-04-01 09:30:00', 'DEPT001', 'DOC001', '反复胸闷、心悸1周，加重2天', '患者1周前无明显诱因出现胸闷，伴心悸，多于活动后出现，休息后可缓解。2天前症状加重，稍活动即感胸闷。', '高血压病史5年，服用降压药控制尚可。糖尿病史3年。否认冠心病史。', 'BP:145/90mmHg, P:82次/分, 心肺(-), 双下肢无水肿', '冠心病 稳定性心绞痛', 'I25.103', '复诊', 0),
('REC002', 'PAT002', 'V20260401002', '2026-04-01 10:15:00', 'DEPT004', 'DOC005', '多饮、多尿、多食3个月，加重伴体重下降2周', '患者3个月前出现多饮、多尿、多食，体重无明显变化。2周前症状加重，体重下降约3公斤。', '无特殊病史。', 'BP:120/80mmHg, BMI:26.5, 甲状腺(-)', '2型糖尿病', 'E11.901', '初诊', 1),
('REC003', 'PAT003', 'V20260402001', '2026-04-02 08:45:00', 'DEPT001', 'DOC002', '心悸、胸闷1个月余', '患者1个月前开始出现心悸，多于夜间发作，伴胸闷，持续数分钟至数小时不等，可自行缓解。', '高血压病史10年，服用降压药。脑梗死病史2年，无后遗症。长期服用华法林。', 'BP:150/95mmHg, P:95次/分，心律不齐，第一心音强弱不等', '心房颤动', 'I48.x01', '初诊', 1),
('REC004', 'PAT004', 'V20260402002', '2026-04-02 14:20:00', 'DEPT003', 'DOC004', '上腹部饱胀、嗳气3个月', '患者3个月前开始出现上腹部饱胀感，伴嗳气，餐后加重，无反酸、烧心，无恶心、呕吐。', '否认消化性溃疡病史。', 'BP:115/75mmHg, 心肺(-), 腹软, 上腹部轻压痛, 无反跳痛', '慢性胃炎', 'K29.501', '初诊', 1),
('REC005', 'PAT005', 'V20260403001', '2026-04-03 09:00:00', 'DEPT002', 'DOC003', '反复咳嗽、咳痰10年，加重伴气促1周', '患者10年前开始出现反复咳嗽、咳痰，多于冬春季节发作。1周前受凉后症状加重，咳黄脓痰，伴气促。', '吸烟史30年。否认高血压、糖尿病史。', 'BP:130/85mmHg, SpO2:92%, 口唇紫绀, 桶状胸, 双肺呼吸音低', '慢性阻塞性肺疾病急性加重期', 'J44.101', '复诊', 0),
('REC006', 'PAT006', 'V20260403002', '2026-04-03 15:45:00', 'DEPT008', 'DOC008', '发热、咽痛、咳嗽2天', '患者2天前受凉后出现发热，体温最高38.5℃，伴咽痛、干咳。', '体健。', 'BP:110/70mmHg, T:38.2℃, 咽红肿, 双肺呼吸音粗', '急性上呼吸道感染', 'J06.901', '初诊', 1),
('REC007', 'PAT001', 'V20260403003', '2026-04-03 10:30:00', 'DEPT004', 'DOC005', '体检发现血糖升高3天', '患者3天前体检发现空腹血糖8.5mmol/L，无明显口干、多饮症状。', '有冠心病史。', 'BP:140/88mmHg, BMI:27.0', '2型糖尿病伴并发症', 'E11.901', '初诊', 1),
('REC008', 'PAT007', 'V20260404001', '2026-04-04 08:30:00', 'DEPT005', 'DOC006', '左侧肢体无力、麻木2小时', '患者2小时前睡醒后出现左侧肢体无力、麻木，伴言语不清。', '高血压病史15年，服用降压药。糖尿病史5年。', 'BP:180/110mmHg, 神志清, 左侧肢体肌力3级, 病理征(+)', '急性脑梗死', 'I63.902', '初诊', 1);

-- 诊断记录数据
INSERT INTO `diagnosis` (`diagnosis_id`, `patient_id`, `record_id`, `visit_no`, `diagnosis_type`, `diagnosis_level`, `diagnosis_code`, `diagnosis_name`, `diagnosis_date`, `dept_id`, `doctor_id`, `disease_severity`, `is_confirmed`, `treatment_days`, `outcome`, `status`) VALUES
('DIA001', 'PAT001', 'REC001', 'V20260401001', '西医', '主要诊断', 'I25.103', '冠状动脉粥样硬化性心脏病', '2026-04-01 09:30:00', 'DEPT001', 'DOC001', '中等', 1, NULL, NULL, 1),
('DIA002', 'PAT001', 'REC001', 'V20260401001', '西医', '次要诊断', 'I10.x00', '高血压病', '2026-04-01 09:30:00', 'DEPT001', 'DOC001', '中等', 1, NULL, NULL, 1),
('DIA003', 'PAT002', 'REC002', 'V20260401002', '西医', '主要诊断', 'E11.901', '2型糖尿病', '2026-04-01 10:15:00', 'DEPT004', 'DOC005', '轻度', 1, NULL, NULL, 1),
('DIA004', 'PAT003', 'REC003', 'V20260402001', '西医', '主要诊断', 'I48.x01', '心房颤动', '2026-04-02 08:45:00', 'DEPT001', 'DOC002', '中等', 1, NULL, NULL, 1),
('DIA005', 'PAT003', 'REC003', 'V20260402001', '西医', '次要诊断', 'I10.x00', '高血压病', '2026-04-02 08:45:00', 'DEPT001', 'DOC002', '中等', 1, NULL, NULL, 1),
('DIA006', 'PAT003', 'REC003', 'V20260402001', '西医', '次要诊断', 'I63.902', '脑梗死个人史', '2026-04-02 08:45:00', 'DEPT001', 'DOC002', '中等', 1, NULL, NULL, 1),
('DIA007', 'PAT004', 'REC004', 'V20260402002', '西医', '主要诊断', 'K29.501', '慢性胃炎', '2026-04-02 14:20:00', 'DEPT003', 'DOC004', '轻度', 1, NULL, NULL, 1),
('DIA008', 'PAT005', 'REC005', 'V20260403001', '西医', '主要诊断', 'J44.101', '慢性阻塞性肺疾病急性加重期', '2026-04-03 09:00:00', 'DEPT002', 'DOC003', '重度', 1, NULL, NULL, 1),
('DIA009', 'PAT005', 'REC005', 'V20260403001', '西医', '次要诊断', 'J18.901', '肺部感染', '2026-04-03 09:00:00', 'DEPT002', 'DOC003', '中等', 1, NULL, NULL, 1),
('DIA010', 'PAT006', 'REC006', 'V20260403002', '西医', '主要诊断', 'J06.901', '急性上呼吸道感染', '2026-04-03 15:45:00', 'DEPT008', 'DOC008', '轻度', 1, NULL, NULL, 1),
('DIA011', 'PAT001', 'REC007', 'V20260403003', '西医', '主要诊断', 'E11.901', '2型糖尿病伴有并发症', '2026-04-03 10:30:00', 'DEPT004', 'DOC005', '中等', 1, NULL, NULL, 1),
('DIA012', 'PAT001', 'REC007', 'V20260403003', '西医', '次要诊断', 'E11.200x001', '糖尿病性周围血管病', '2026-04-03 10:30:00', 'DEPT004', 'DOC005', '中等', 1, NULL, NULL, 1),
('DIA013', 'PAT007', 'REC008', 'V20260404001', '西医', '主要诊断', 'I63.902', '急性脑梗死', '2026-04-04 08:30:00', 'DEPT005', 'DOC006', '重度', 1, NULL, NULL, 1),
('DIA014', 'PAT007', 'REC008', 'V20260404001', '西医', '次要诊断', 'I10.x00', '高血压病', '2026-04-04 08:30:00', 'DEPT005', 'DOC006', '中等', 1, NULL, NULL, 1),
('DIA015', 'PAT007', 'REC008', 'V20260404001', '西医', '次要诊断', 'E11.901', '2型糖尿病', '2026-04-04 08:30:00', 'DEPT005', 'DOC006', '中等', 1, NULL, NULL, 1);

-- 诊断详情数据
INSERT INTO `diagnosis_detail` (`detail_id`, `diagnosis_id`, `patient_id`, `symptom_name`, `symptom_duration`, `symptom_description`, `symptom_severity`, `body_part`) VALUES
('DD001', 'DIA001', 'PAT001', '胸闷', '1周', '反复胸闷，多于活动后出现', '中等', '胸部'),
('DD002', 'DIA001', 'PAT001', '心悸', '1周', '心跳加快感，休息后可缓解', '轻度', '胸部'),
('DD003', 'DIA004', 'PAT003', '心悸', '1月余', '心悸，多于夜间发作', '中等', '胸部'),
('DD004', 'DIA004', 'PAT003', '胸闷', '1月余', '胸闷，持续数分钟至数小时', '中等', '胸部'),
('DD005', 'DIA008', 'PAT005', '咳嗽', '10年', '反复咳嗽、咳痰，冬春季节加重', '重度', '呼吸系统'),
('DD006', 'DIA008', 'PAT005', '气促', '1周', '受凉后加重，咳黄脓痰', '重度', '呼吸系统'),
('DD007', 'DIA013', 'PAT007', '左侧肢体无力', '2小时', '睡醒后出现左侧肢体无力、麻木', '重度', '左侧肢体'),
('DD008', 'DIA013', 'PAT007', '言语不清', '2小时', '伴有言语不清', '中等', '面部/言语');

-- 检查申请单数据
INSERT INTO `exam_request` (`request_id`, `patient_id`, `record_id`, `visit_no`, `request_date`, `dept_id`, `doctor_id`, `exam_item_code`, `exam_item_name`, `exam_category`, `clinical_diagnosis`, `urgent_flag`, `exam_status`, `exam_date`, `report_content`, `report_conclusion`) VALUES
('EXAM001', 'PAT001', 'REC001', 'V20260401001', '2026-04-01 09:35:00', 'DEPT001', 'DOC001', 'ECG001', '常规心电图', '心电图', '冠心病 稳定性心绞痛', 0, '已完成', '2026-04-01 10:00:00', '窦性心律，ST-T段改变（II、III、aVF导联ST段压低0.05-0.1mV）', '心电图异常，请结合临床'),
('EXAM002', 'PAT001', 'REC001', 'V20260401001', '2026-04-01 09:36:00', 'DEPT001', 'DOC001', 'ECHO001', '心脏彩色多普勒超声', '超声检查', '冠心病', 0, '已完成', '2026-04-01 11:00:00', '左室壁运动幅度减低，左室舒张功能减低，EF 55%', '左室舒张功能减低'),
('EXAM003', 'PAT002', 'REC002', 'V20260401002', '2026-04-01 10:20:00', 'DEPT004', 'DOC005', 'LAB001', '空腹血糖', '生化检验', '2型糖尿病', 0, '已完成', '2026-04-01 10:30:00', '空腹血糖: 8.5 mmol/L', '空腹血糖偏高'),
('EXAM004', 'PAT002', 'REC002', 'V20260401002', '2026-04-01 10:21:00', 'DEPT004', 'DOC005', 'LAB002', '糖化血红蛋白', '生化检验', '2型糖尿病', 0, '已完成', '2026-04-01 10:35:00', 'HbA1c: 7.8%', '血糖控制欠佳'),
('EXAM005', 'PAT003', 'REC003', 'V20260402001', '2026-04-02 08:50:00', 'DEPT001', 'DOC002', 'ECG001', '常规心电图', '心电图', '心房颤动', 0, '已完成', '2026-04-02 09:00:00', '心房颤动，心室率82次/分', '心房颤动'),
('EXAM006', 'PAT003', 'REC003', 'V20260402001', '2026-04-02 08:51:00', 'DEPT001', 'DOC002', 'LAB003', '凝血功能', '凝血检验', '心房颤动', 0, '已完成', '2026-04-02 09:15:00', 'PT: 18.2秒, INR: 1.8', 'INR偏高'),
('EXAM007', 'PAT005', 'REC005', 'V20260403001', '2026-04-03 09:05:00', 'DEPT002', 'DOC003', 'CXR001', '胸部正侧位片', '放射检查', '慢阻肺急性加重期', 1, '已完成', '2026-04-03 09:30:00', '双肺纹理增粗紊乱，透亮度增加，双下肺可见斑片影', '慢阻肺伴肺部感染'),
('EXAM008', 'PAT007', 'REC008', 'V20260404001', '2026-04-04 08:35:00', 'DEPT005', 'DOC006', 'CT001', '颅脑CT平扫', 'CT检查', '急性脑梗死', 1, '已完成', '2026-04-04 09:00:00', '右侧基底节区可见低密度灶，边界不清', '右侧基底节区脑梗死');

-- 检验记录数据
INSERT INTO `lab_test` (`test_id`, `patient_id`, `record_id`, `visit_no`, `request_date`, `dept_id`, `doctor_id`, `test_category`, `test_item_code`, `test_item_name`, `specimen_type`, `specimen_no`, `collect_time`, `test_result`, `result_value`, `unit`, `reference_value`, `result_flag`) VALUES
('LABT001', 'PAT001', 'REC001', 'V20260401001', '2026-04-01 09:35:00', 'DEPT001', 'DOC001', '生化检验', 'BNP001', 'B型钠尿肽(BNP)', '血液', 'SP20260401001', '2026-04-01 10:00:00', 'BNP', '128', 'pg/mL', '0-100', 'H'),
('LABT002', 'PAT002', 'REC002', 'V20260401002', '2026-04-01 10:20:00', 'DEPT004', 'DOC005', '生化检验', 'GLU001', '空腹血糖', '血液', 'SP20260401002', '2026-04-01 10:30:00', '空腹血糖', '8.5', 'mmol/L', '3.9-6.1', 'H'),
('LABT003', 'PAT002', 'REC002', 'V20260401002', '2026-04-01 10:20:00', 'DEPT004', 'DOC005', '特殊检验', 'HBA001', '糖化血红蛋白', '血液', 'SP20260401003', '2026-04-01 10:30:00', 'HbA1c', '7.8', '%', '4.0-6.0', 'H'),
('LABT004', 'PAT003', 'REC003', 'V20260402001', '2026-04-02 08:50:00', 'DEPT001', 'DOC002', '凝血检验', 'PT001', '凝血酶原时间(PT)', '血液', 'SP20260402001', '2026-04-02 09:10:00', 'PT', '18.2', '秒', '11.0-15.0', 'H'),
('LABT005', 'PAT003', 'REC003', 'V20260402001', '2026-04-02 08:50:00', 'DEPT001', 'DOC002', '凝血检验', 'INR001', '国际标准化比值(INR)', '血液', 'SP20260402002', '2026-04-02 09:10:00', 'INR', '1.8', '', '0.8-1.5', 'H'),
('LABT006', 'PAT005', 'REC005', 'V20260403001', '2026-04-03 09:05:00', 'DEPT002', 'DOC003', '血常规', 'WBC001', '白细胞计数', '血液', 'SP20260403001', '2026-04-03 09:20:00', 'WBC', '11.2', '×10^9/L', '3.5-9.5', 'H'),
('LABT007', 'PAT007', 'REC008', 'V20260404001', '2026-04-04 08:35:00', 'DEPT005', 'DOC006', '血常规', 'PLT001', '血小板计数', '血液', 'SP20260404001', '2026-04-04 08:50:00', 'PLT', '185', '×10^9/L', '125-350', 'N');

-- 住院记录数据
INSERT INTO `hospitalization` (`hospital_id`, `patient_id`, `inpatient_no`, `admission_date`, `dept_id`, `ward_id`, `bed_no`, `admission_diagnosis`, `admission_diagnosis_code`, `attending_doctor_id`, `discharge_date`, `discharge_diagnosis`, `hospital_days`, `treatment_outcome`, `medical_expense`, `status`) VALUES
('HOS001', 'PAT005', 'IP20260001', '2026-04-03 11:00:00', 'DEPT002', 'WARD02', '12', '慢性阻塞性肺疾病急性加重期', 'J44.101', 'DOC003', '2026-04-10 10:00:00', '慢性阻塞性肺疾病急性加重期', 7, '好转', 12800.00, '已出院'),
('HOS002', 'PAT007', 'IP20260002', '2026-04-04 09:00:00', 'DEPT005', 'WARD05', '8', '急性脑梗死', 'I63.902', 'DOC006', NULL, '急性脑梗死', 3, NULL, NULL, '住院中');

-- 手术记录数据
INSERT INTO `operation_record` (`operation_id`, `hospital_id`, `patient_id`, `record_id`, `operation_no`, `operation_date`, `operation_name`, `operation_code`, `operation_level`, `operation_type`, `emergency_level`, `dept_id`, `surgeon_id`, `anesthesia_method`, `pre_diagnosis`, `post_diagnosis`, `operation_procedure`, `blood_loss`, `operation_begin`, `operation_end`, `operation_duration`) VALUES
('OP001', 'HOS001', 'PAT005', 'REC005', 'OP20260405001', '2026-04-05 14:00:00', '纤维支气管镜检查', '31.1x01', '四级', '诊断性手术', '择期', 'DEPT002', 'DOC003', '局麻', '慢阻肺伴肺部感染', '慢阻肺伴肺部感染', '常规消毒铺巾，局部麻醉后经鼻腔插入支气管镜，依次观察气管、支气管，可见右下叶背段支气管开口处有脓性分泌物，予以吸除，并行支气管肺泡灌洗...', '10ml', '2026-04-05 14:00:00', '2026-04-05 14:30:00', 30);

-- 疾病编码数据
INSERT INTO `disease_code` (`code_id`, `icd_code`, `disease_name`, `disease_type`, `disease_category`, `related_dept`, `is_chronic`, `is_infectious`) VALUES
('DIS001', 'I25.103', '冠状动脉粥样硬化性心脏病', '心血管疾病', '循环系统疾病', '心血管内科', 1, 0),
('DIS002', 'I48.x01', '心房颤动', '心律失常', '循环系统疾病', '心血管内科', 1, 0),
('DIS003', 'E11.901', '2型糖尿病', '内分泌疾病', '内分泌、营养和代谢疾病', '内分泌科', 1, 0),
('DIS004', 'K29.501', '慢性胃炎', '消化系统疾病', '消化系统疾病', '消化内科', 1, 0),
('DIS005', 'J44.101', '慢性阻塞性肺疾病急性加重期', '呼吸系统疾病', '呼吸系统疾病', '呼吸内科', 1, 0),
('DIS006', 'J06.901', '急性上呼吸道感染', '呼吸系统疾病', '呼吸系统疾病', '儿科/呼吸内科', 0, 0),
('DIS007', 'I63.902', '急性脑梗死', '脑血管疾病', '循环系统疾病', '神经内科', 0, 0),
('DIS008', 'I10.x00', '高血压病', '心血管疾病', '循环系统疾病', '心血管内科', 1, 0),
('DIS009', 'E11.200x001', '糖尿病性周围血管病', '内分泌疾病', '内分泌、营养和代谢疾病', '内分泌科/血管外科', 1, 0),
('DIS010', 'J18.901', '肺部感染', '呼吸系统疾病', '呼吸系统疾病', '呼吸内科', 0, 0);

-- ICD-10手术编码数据
INSERT INTO `operation_code` (`code_id`, `icd9_code`, `operation_name`, `operation_level`, `operation_category`, `body_system`, `anesthesia_type`, `is_minimally_invasive`) VALUES
('OPC001', '31.1x01', '纤维支气管镜检查', '四级', '诊断性操作', '呼吸系统', '局麻', 0),
('OPC002', '45.2x00', '电子胃镜检查', '四级', '诊断性操作', '消化系统', '局麻', 0),
('OPC003', '51.2301', '腹腔镜胆囊切除术', '四级', '切除性手术', '肝胆系统', '全麻', 1),
('OPC004', '00.5101', '经皮冠状动脉支架植入术', '四级', '介入手术', '心血管系统', '局麻', 1),
('OPC005', '38.1201', '经股动脉插管全脑血管造影术', '四级', '介入手术', '神经系统', '局麻', 1);

-- 随访记录数据
INSERT INTO `follow_up` (`follow_up_id`, `patient_id`, `diagnosis_id`, `hospital_id`, `follow_up_no`, `follow_up_type`, `follow_up_date`, `dept_id`, `doctor_id`, `current_symptoms`, `medication_compliance`, `disease_control_status`, `next_follow_up_date`) VALUES
('FUP001', 'PAT001', 'DIA001', NULL, 'FU20260001', '门诊随访', '2026-04-08 10:00:00', 'DEPT001', 'DOC001', '偶有胸闷，休息后好转', '规律服药', '控制良好', '2026-05-08'),
('FUP002', 'PAT002', 'DIA003', NULL, 'FU20260002', '电话随访', '2026-04-10 15:00:00', 'DEPT004', 'DOC005', '多饮多尿症状减轻', '规律服药', '控制欠佳', '2026-04-17'),
('FUP003', 'PAT005', 'DIA008', 'HOS001', 'FU20260003', '门诊随访', '2026-04-17 09:00:00', 'DEPT002', 'DOC003', '咳嗽咳痰明显减少，无明显气促', '规律服药', '控制良好', '2026-05-17');

-- 会诊记录数据
INSERT INTO `consultation` (`consultation_id`, `patient_id`, `hospital_id`, `record_id`, `consultation_no`, `request_dept_id`, `request_doctor_id`, `request_date`, `consulted_dept_id`, `consulted_doctor_id`, `consultation_type`, `consultation_date`, `consultation_purpose`, `consultation_opinion`, `treatment_suggestion`, `consultation_status`) VALUES
('CON001', 'PAT005', 'HOS001', 'REC005', 'CON20260001', 'DEPT002', 'DOC003', '2026-04-04 10:00:00', 'DEPT001', 'DOC001', '普通会诊', '2026-04-04 14:00:00', '患者有高血压病史，请协助评估心脏功能', '患者心脏彩超示左室舒张功能减低，建议加用改善心肌重构药物', '加用ACEI类药物，注意监测血压', '已完成'),
('CON002', 'PAT007', 'HOS002', 'REC008', 'CON20260002', 'DEPT005', 'DOC006', '2026-04-04 09:30:00', 'DEPT001', 'DOC002', '急会诊', '2026-04-04 10:00:00', '急性脑梗死患者，请评估抗凝治疗方案', '患者合并心房颤动，建议抗凝治疗', '可考虑使用NOAC抗凝，定期监测凝血功能', '已完成');

-- 治疗记录数据
INSERT INTO `treatment_record` (`treatment_id`, `patient_id`, `hospital_id`, `record_id`, `visit_no`, `treatment_date`, `treatment_type`, `treatment_item_name`, `treatment_content`, `treatment_duration`, `dept_id`, `execution_time`, `treatment_result`) VALUES
('TRT001', 'PAT005', 'HOS001', 'REC005', NULL, '2026-04-03 14:00:00', '药物治疗', '注射用头孢哌酮舒巴坦', '3g, 静脉滴注, q12h', '30分钟', 'DEPT002', '2026-04-03 14:00:00', '进行中'),
('TRT002', 'PAT005', 'HOS001', 'REC005', NULL, '2026-04-03 09:00:00', '氧疗', '持续低流量吸氧', '2L/min', '持续', 'DEPT002', '2026-04-03 09:00:00', '进行中'),
('TRT003', 'PAT007', 'HOS002', 'REC008', NULL, '2026-04-04 10:00:00', '药物治疗', '阿替普酶溶栓治疗', '50mg, 静脉溶栓', '60分钟', 'DEPT005', '2026-04-04 10:00:00', '已完成'),
('TRT004', 'PAT007', 'HOS002', 'REC008', NULL, '2026-04-04 11:00:00', '监测治疗', '重症监护', '心电监护、血压监测', '持续', 'DEPT005', '2026-04-04 11:00:00', '进行中');

-- 病程记录数据
INSERT INTO `clinical_progress` (`progress_id`, `hospital_id`, `patient_id`, `record_date`, `record_type`, `record_title`, `current_condition`, `vital_signs`, `diagnosis_changes`, `treatment_plan`, `doctor_id`, `is_chief_visit`) VALUES
('CP001', 'HOS001', 'PAT005', '2026-04-04 08:00:00', '日常病程', '入院第1天病程记录', '患者仍有咳嗽、咳黄脓痰，轻度气促。', 'T:37.8℃, P:88次/分, R:22次/分, BP:130/85mmHg, SpO2:94%', '诊断同前', '继续抗感染、化痰、平喘治疗', 'DOC003', 0),
('CP002', 'HOS001', 'PAT005', '2026-04-04 15:00:00', '上级查房', '主任查房记录', '患者症状有所改善，咳嗽减轻。', 'T:37.2℃, P:82次/分, R:20次/分, BP:125/80mmHg, SpO2:96%', '诊断明确', '继续当前治疗方案，预约支气管镜检查', 'DOC003', 1),
('CP003', 'HOS002', 'PAT007', '2026-04-04 12:00:00', '日常病程', '入院当日病程记录', '患者溶栓后肢体无力症状有所改善。', 'T:36.8℃, P:78次/分, R:18次/分, BP:150/95mmHg, 神志清, 左侧肢体肌力4级', '诊断: 急性脑梗死', '继续抗血小板聚集、营养神经治疗，请心内科会诊协助治疗房颤', 'DOC006', 0);

-- 电子病历模板数据
INSERT INTO `emr_template` (`template_id`, `template_name`, `template_type`, `template_category`, `dept_id`, `template_content`, `is_default`, `author_id`, `approval_status`) VALUES
('TPL001', '内科入院记录模板', '入院记录', '住院病历', 'DEPT001', '主诉：现病史：既往史：个人史：家族史：体格检查：辅助检查：入院诊断：', 1, 'DOC001', '已审核'),
('TPL002', '门诊病历模板', '门诊病历', '门诊病历', NULL, '主诉：现病史：既往史：体格检查：诊断：处理：', 1, 'DOC001', '已审核'),
('TPL003', '病程记录模板', '病程记录', '住院病历', NULL, '日期：目前情况：体格检查：辅助检查：诊断变化：处理意见：', 1, 'DOC001', '已审核'),
('TPL004', '手术记录模板', '手术记录', '住院病历', 'DEPT006', '手术日期：手术名称：手术医师：麻醉方式：术前诊断：术后诊断：手术经过：', 1, 'DOC007', '已审核');

-- 病历质控记录数据
INSERT INTO `quality_control` (`qc_id`, `patient_id`, `hospital_id`, `record_id`, `qc_no`, `qc_type`, `qc_date`, `qc_doctor_id`, `record_completeness`, `diagnosis_accuracy`, `treatment_standardization`, `defects_found`, `defect_level`, `correction_status`) VALUES
('QC001', 'PAT005', 'HOS001', 'REC005', 'QC20260001', '运行病历质控', '2026-04-05 10:00:00', 'DOC001', 95.00, 100.00, 98.00, '入院记录缺少过敏史描述', '一般缺陷', '已完成整改'),
('QC002', 'PAT007', 'HOS002', 'REC008', 'QC20260002', '运行病历质控', '2026-04-05 11:00:00', 'DOC001', 92.00, 100.00, 95.00, '溶栓知情同意书签名不规范', '严重缺陷', '待整改');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 完成
-- ============================================================
