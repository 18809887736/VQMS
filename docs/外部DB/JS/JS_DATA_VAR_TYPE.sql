/*
 Navicat Premium Data Transfer

 Source Server         : QHeatAvcRtdb
 Source Server Type    : SQLite
 Source Server Version : 3035005 (3.35.5)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3035005 (3.35.5)
 File Encoding         : 65001

 Date: 12/08/2026 15:02:39
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for JS_DATA_VAR_TYPE
-- ----------------------------
DROP TABLE IF EXISTS "JS_DATA_VAR_TYPE";
CREATE TABLE "JS_DATA_VAR_TYPE" (
  "id" SMALLINT,
  "name" VARCHAR(50),
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Records of JS_DATA_VAR_TYPE
-- ----------------------------
INSERT INTO "JS_DATA_VAR_TYPE" VALUES (0, '常量');
INSERT INTO "JS_DATA_VAR_TYPE" VALUES (1, '遥测');
INSERT INTO "JS_DATA_VAR_TYPE" VALUES (2, '遥信');
INSERT INTO "JS_DATA_VAR_TYPE" VALUES (3, '电度');
INSERT INTO "JS_DATA_VAR_TYPE" VALUES (4, '步骤');

PRAGMA foreign_keys = true;
