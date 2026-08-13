-- VQMS AVC 合成数据测试库 schema（从 backup/*.sql 复用三表 DDL）
-- 独立库 vqms_avc_test，与真实 qheatavchisdb / 主库 ry_vqms 隔离
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `his_curve_sv`;
CREATE TABLE `his_curve_sv`  (
  `save_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `busbar_num` bigint(20) NULL DEFAULT NULL,
  `high_SV` decimal(10, 0) NULL DEFAULT NULL,
  `low_SV` decimal(10, 0) NULL DEFAULT NULL,
  `average_SV` decimal(10, 0) NULL DEFAULT NULL,
  `plan_SV` decimal(10, 0) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `warn_info`;
CREATE TABLE `warn_info`  (
  `warn_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `millisecond` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `warn_type` bigint(20) NULL DEFAULT NULL,
  `obj_num` bigint(20) NULL DEFAULT NULL,
  `warn_info` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `yc_history`;
CREATE TABLE `yc_history`  (
  `yc_num` bigint(20) NULL DEFAULT NULL,
  `yc_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `yc_data` double NULL DEFAULT NULL,
  UNIQUE INDEX `yc_history_num_time_index`(`yc_num`, `yc_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


SET FOREIGN_KEY_CHECKS = 1;