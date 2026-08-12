/*
 Navicat Premium Data Transfer

 Source Server         : QHeatAvcRtdb
 Source Server Type    : SQLite
 Source Server Version : 3035005 (3.35.5)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3035005 (3.35.5)
 File Encoding         : 65001

 Date: 12/08/2026 15:02:15
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for JS_DATA
-- ----------------------------
DROP TABLE IF EXISTS "JS_DATA";
CREATE TABLE "JS_DATA" (
  "js_num" INTEGER,
  "js_code" CHAR(11),
  "step_count" INTEGER,
  "result_type" INTEGER,
  "relate_num" INTEGER,
  PRIMARY KEY ("js_num")
);

-- ----------------------------
-- Records of JS_DATA
-- ----------------------------
INSERT INTO "JS_DATA" VALUES (0, '正母线总无功', 1, 1, 11);
INSERT INTO "JS_DATA" VALUES (1, '3#机组投退', 1, 2, 210);
INSERT INTO "JS_DATA" VALUES (2, '4#机组投退', 1, 2, 310);
INSERT INTO "JS_DATA" VALUES (3, '转发DCS3#', 1, 1, 1001);
INSERT INTO "JS_DATA" VALUES (4, '转发DCS4#', 1, 1, 1002);
INSERT INTO "JS_DATA" VALUES (5, '3#增磁闭锁', 1, 2, 208);
INSERT INTO "JS_DATA" VALUES (6, '3#减磁闭锁', 1, 2, 209);
INSERT INTO "JS_DATA" VALUES (7, '4#增磁闭锁', 1, 2, 308);
INSERT INTO "JS_DATA" VALUES (8, '4#减磁闭锁', 1, 2, 309);
INSERT INTO "JS_DATA" VALUES (9, '3#总闭锁', 1, 2, 1103);
INSERT INTO "JS_DATA" VALUES (10, '4#总闭锁', 1, 2, 1104);
INSERT INTO "JS_DATA" VALUES (11, '3#越限', 1, 2, 1101);
INSERT INTO "JS_DATA" VALUES (12, '4#越限', 1, 2, 1102);
INSERT INTO "JS_DATA" VALUES (13, '副母线总无功', 1, 1, 17);
INSERT INTO "JS_DATA" VALUES (14, '远方/就地合并', 1, 2, 2003);
INSERT INTO "JS_DATA" VALUES (16, '220KV减210', 1, 1, 1007);
INSERT INTO "JS_DATA" VALUES (17, '500KV减500', 1, 1, 1008);
INSERT INTO "JS_DATA" VALUES (101, '母线双向闭锁总信号', 1, 2, 3001);
INSERT INTO "JS_DATA" VALUES (102, '母线上闭锁总信号', 1, 2, 3002);
INSERT INTO "JS_DATA" VALUES (103, '母线下闭锁总信号', 1, 2, 3003);
INSERT INTO "JS_DATA" VALUES (104, '三号机组双向闭锁总信号', 1, 2, 3004);
INSERT INTO "JS_DATA" VALUES (105, '三号机组上闭锁总信号', 1, 2, 3005);
INSERT INTO "JS_DATA" VALUES (106, '三号机组下闭锁总信号', 1, 2, 3006);
INSERT INTO "JS_DATA" VALUES (107, '四号机组双向闭锁总信号', 1, 2, 3007);
INSERT INTO "JS_DATA" VALUES (108, '四号机组上闭锁总信号', 1, 2, 3008);
INSERT INTO "JS_DATA" VALUES (109, '四号机组下闭锁总信号', 1, 2, 3009);

PRAGMA foreign_keys = true;
