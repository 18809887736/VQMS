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

 Date: 10/08/2026 16:02:04
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for warn_info
-- ----------------------------
DROP TABLE IF EXISTS `warn_info`;
CREATE TABLE `warn_info`  (
  `warn_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `millisecond` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `warn_type` bigint(20) NULL DEFAULT NULL,
  `obj_num` bigint(20) NULL DEFAULT NULL,
  `warn_info` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
