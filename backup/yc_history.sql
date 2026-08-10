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

 Date: 10/08/2026 10:46:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for yc_history
-- ----------------------------
DROP TABLE IF EXISTS `yc_history`;
CREATE TABLE `yc_history`  (
  `yc_num` bigint(20) NULL DEFAULT NULL,
  `yc_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `yc_data` double NULL DEFAULT NULL,
  UNIQUE INDEX `yc_history_num_time_index`(`yc_num`, `yc_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
