/*
 Navicat Premium Data Transfer

 Source Server         : 10.0.0.9
 Source Server Type    : MySQL
 Source Server Version : 50744 (5.7.44)
 Source Host           : 10.0.0.9:3306
 Source Schema         : qheatavchisdb

 Target Server Type    : MySQL
 Target Server Version : 50744 (5.7.44)
 File Encoding         : 65001

 Date: 10/08/2026 10:46:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for his_curve_sv
-- ----------------------------
DROP TABLE IF EXISTS `his_curve_sv`;
CREATE TABLE `his_curve_sv`  (
  `save_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `busbar_num` bigint(20) NULL DEFAULT NULL,
  `high_SV` decimal(10, 0) NULL DEFAULT NULL,
  `low_SV` decimal(10, 0) NULL DEFAULT NULL,
  `average_SV` decimal(10, 0) NULL DEFAULT NULL,
  `plan_SV` decimal(10, 0) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
