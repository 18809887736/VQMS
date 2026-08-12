/*
 Navicat Premium Data Transfer

 Source Server         : QHeatAvcRtdb
 Source Server Type    : SQLite
 Source Server Version : 3035005 (3.35.5)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3035005 (3.35.5)
 File Encoding         : 65001

 Date: 12/08/2026 15:02:05
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for GS_DATA
-- ----------------------------
DROP TABLE IF EXISTS "GS_DATA";
CREATE TABLE "GS_DATA" (
  "gs_num" integer,
  "gs_name" wvarchar(50),
  "var_count" integer,
  "gs_description" wvarchar(100),
  PRIMARY KEY ("gs_num")
);

-- ----------------------------
-- Records of GS_DATA
-- ----------------------------
INSERT INTO "GS_DATA" VALUES (0, '内蒙古计划电压', 2, '变量0为当前母线电压值，变量1为增量命令');
INSERT INTO "GS_DATA" VALUES (1, '累加', 0, '所有变量相加运算，若只有一个变量为赋值运算');
INSERT INTO "GS_DATA" VALUES (2, '累乘', 0, '所有变量相乘运算，若只有一个变量为赋值运算');
INSERT INTO "GS_DATA" VALUES (3, '符号取反', 1, '第一个变量正数变负数，负数变正数，其他变量忽略');
INSERT INTO "GS_DATA" VALUES (4, '倒数', 1, '第一个变量取倒数，其他变量忽略');
INSERT INTO "GS_DATA" VALUES (5, '与', 0, '逻辑与操作');
INSERT INTO "GS_DATA" VALUES (6, '或', 0, '逻辑或操作');
INSERT INTO "GS_DATA" VALUES (7, '非', 1, '逻辑非操作');
INSERT INTO "GS_DATA" VALUES (8, '同或', 2, '逻辑同或操作');
INSERT INTO "GS_DATA" VALUES (9, '异或', 2, '逻辑异或操作');
INSERT INTO "GS_DATA" VALUES (10, '逻辑大于', 2, '逻辑运算，第一个操作数大于第二个数返回true，否则返回false');
INSERT INTO "GS_DATA" VALUES (11, '逻辑小于', 0, '逻辑运算，第一个操作数小于第二个数返回true，否则返回false');
INSERT INTO "GS_DATA" VALUES (12, '逻辑相等', 0, '逻辑运算，第一个操作数等于第二个数返回true，否则返回false');
INSERT INTO "GS_DATA" VALUES (13, '逻辑大于等于', 0, '逻辑运算，第一个操作数大于等于第二个数返回true，否则返回false');
INSERT INTO "GS_DATA" VALUES (14, '逻辑小于等于', 0, '逻辑运算，第一个操作数小于等于第二个数返回true，否则返回false');
INSERT INTO "GS_DATA" VALUES (15, 'sin', 1, 'sin（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (16, 'cos', 1, 'cos（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (17, 'tg', 1, 'tg（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (18, 'ctg', 1, 'ctg（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (19, 'arcsin', 1, 'arcsin（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (20, 'arccos', 1, 'arccos（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (21, 'arctg', 1, 'arctg（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (22, 'arcctg', 1, 'arcctg（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (23, 'Extraction', 1, 'Extraction（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (24, 'Power', 1, 'Power（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (25, 'sinangle', 1, 'sinangle（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (26, 'cosangle', 1, 'cosangle（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (27, 'tgangle', 1, 'tgangle（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (28, 'ctgangle', 1, 'ctgangle（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (29, 'arcsinangle', 1, 'arcsinangle（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (30, 'arccosangle', 1, 'arccosangle（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (31, 'arctgangle', 1, 'arctgangle（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (32, 'arcctgangle', 1, 'arcctgangle（θ）,弧度');
INSERT INTO "GS_DATA" VALUES (33, 'IfElse', 3, '三目运算符X?A:B');
INSERT INTO "GS_DATA" VALUES (34, 'CurrentTime', 0, '当前时间');
INSERT INTO "GS_DATA" VALUES (35, 'Year', 1, 'Year（TimeValue）');
INSERT INTO "GS_DATA" VALUES (36, 'Month', 1, 'Month（TimeValue）');
INSERT INTO "GS_DATA" VALUES (37, 'Day', 1, 'Day（TimeValue）');
INSERT INTO "GS_DATA" VALUES (38, 'Hour', 1, 'Hour（TimeValue）');
INSERT INTO "GS_DATA" VALUES (39, 'Minute', 1, 'Minute（TimeValue）');
INSERT INTO "GS_DATA" VALUES (40, 'Second', 1, 'Second（TimeValue）');
INSERT INTO "GS_DATA" VALUES (41, 'Table', 2, '查表（TableNum，KeyValue）');
INSERT INTO "GS_DATA" VALUES (42, 'Max', 0, 'Max(YcNum,second);second<3600');
INSERT INTO "GS_DATA" VALUES (43, 'Min', 0, 'Min(YcNum,second);second<3600');
INSERT INTO "GS_DATA" VALUES (44, 'Average', 0, 'Average(YcNum,second);second<3600');

PRAGMA foreign_keys = true;
