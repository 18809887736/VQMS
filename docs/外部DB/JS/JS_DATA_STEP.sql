/*
 Navicat Premium Data Transfer

 Source Server         : QHeatAvcRtdb
 Source Server Type    : SQLite
 Source Server Version : 3035005 (3.35.5)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3035005 (3.35.5)
 File Encoding         : 65001

 Date: 12/08/2026 15:02:22
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for JS_DATA_STEP
-- ----------------------------
DROP TABLE IF EXISTS "JS_DATA_STEP";
CREATE TABLE "JS_DATA_STEP" (
  "js_num" INTEGER,
  "step_num" INTEGER,
  "step_code" CHAR(7),
  "gs_num" INTEGER,
  "var_count" INTEGER,
  PRIMARY KEY ("js_num", "step_num"),
  FOREIGN KEY ("js_num") REFERENCES "JS_DATA" ("js_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("gs_num") REFERENCES "GS_DATA" ("gs_num") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of JS_DATA_STEP
-- ----------------------------
INSERT INTO "JS_DATA_STEP" VALUES (0, 0, '累加', 1, 2);
INSERT INTO "JS_DATA_STEP" VALUES (1, 0, '3#投退', 5, 2);
INSERT INTO "JS_DATA_STEP" VALUES (2, 0, '4#投退', 5, 2);
INSERT INTO "JS_DATA_STEP" VALUES (3, 0, 'DCS转发3#', 2, 2);
INSERT INTO "JS_DATA_STEP" VALUES (4, 0, 'DCS转发4#', 2, 2);
INSERT INTO "JS_DATA_STEP" VALUES (5, 0, '3#增磁闭锁', 6, 10);
INSERT INTO "JS_DATA_STEP" VALUES (6, 0, '3#减磁闭锁', 6, 11);
INSERT INTO "JS_DATA_STEP" VALUES (7, 0, '4#增磁闭锁', 6, 10);
INSERT INTO "JS_DATA_STEP" VALUES (8, 0, '4#减磁闭锁', 6, 11);
INSERT INTO "JS_DATA_STEP" VALUES (9, 0, '3#总闭锁', 6, 10);
INSERT INTO "JS_DATA_STEP" VALUES (10, 0, '4#总闭锁', 6, 10);
INSERT INTO "JS_DATA_STEP" VALUES (11, 0, '3#越限', 6, 3);
INSERT INTO "JS_DATA_STEP" VALUES (12, 0, '4#越限', 6, 3);
INSERT INTO "JS_DATA_STEP" VALUES (101, 0, '或', 6, 14);
INSERT INTO "JS_DATA_STEP" VALUES (102, 0, '或', 6, 2);
INSERT INTO "JS_DATA_STEP" VALUES (103, 0, '或', 6, 2);
INSERT INTO "JS_DATA_STEP" VALUES (104, 0, '或', 6, 13);
INSERT INTO "JS_DATA_STEP" VALUES (105, 0, '或', 6, 9);
INSERT INTO "JS_DATA_STEP" VALUES (106, 0, '或', 6, 8);
INSERT INTO "JS_DATA_STEP" VALUES (107, 0, '或', 6, 13);
INSERT INTO "JS_DATA_STEP" VALUES (108, 0, '或', 6, 9);
INSERT INTO "JS_DATA_STEP" VALUES (109, 0, '或', 6, 8);
INSERT INTO "JS_DATA_STEP" VALUES (13, 0, '累加', 1, 2);
INSERT INTO "JS_DATA_STEP" VALUES (15, 0, '转发赋值', 1, 1);
INSERT INTO "JS_DATA_STEP" VALUES (14, 0, '合并远方就地', 6, 2);
INSERT INTO "JS_DATA_STEP" VALUES (16, 0, '减后', 1, 2);
INSERT INTO "JS_DATA_STEP" VALUES (17, 0, '500减后', 1, 2);

PRAGMA foreign_keys = true;
