/*
 Navicat Premium Data Transfer

 Source Server         : QHeatAvcRtdb
 Source Server Type    : SQLite
 Source Server Version : 3035005 (3.35.5)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3035005 (3.35.5)
 File Encoding         : 65001

 Date: 12/08/2026 15:06:48
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for BUSBAR
-- ----------------------------
DROP TABLE IF EXISTS "BUSBAR";
CREATE TABLE "BUSBAR" (
  "busbarNum" SMALLINT,
  "busbarName" WVARCHAR(50),
  "TargetMAX" INTEGER,
  "TargetMIN" INTEGER,
  "UpLimit" INTEGER,
  "DownLimit" INTEGER,
  "COSLimit" FLOAT,
  "planVYcNum" SMALLINT,
  "planType" SMALLINT,
  "realVYcNum" SMALLINT,
  "realVYcNum_1" SMALLINT,
  "IsRunningYxNum" SMALLINT,
  "DoubleYC" SMALLINT,
  "IsDoubleYC" BOOLEAN,
  "planVExcuteYcNum" SMALLINT,
  "WorkStatusYXNum" SMALLINT,
  "singleMaxVoltage" DOUBLE,
  "vUpUpLimit" DOUBLE,
  "vDownDownLimit" DOUBLE,
  "distributeMethod" INTEGER,
  "IsBusDoubleVLock" BOOLEAN,
  "IsBusDoublePTLock" BOOLEAN,
  "lock_slc_pc_yx_num" INTEGER,
  "lock_jhyx_yx_num" INTEGER,
  "lock_double_pt_yx_num" INTEGER,
  "plan_vq_way" SMALLINT,
  "vGrade" INTEGER,
  "IsUseLocalCurve" BOOLEAN,
  "IsUseQLimit" BOOLEAN,
  "planQUplimitYcNum" INTEGER,
  "planQDownlimitYcNum" INTEGER,
  "workStatusChangeSleep" INTEGER,
  "busDoublePTLimit" FLOAT,
  "localPlanCurveNum" INTEGER,
  "IsMainBusBar" BOOLEAN,
  "GroupNum" INTEGER,
  "GroupSubNum" INTEGER,
  "PlanVReferenceYcNum" INTEGER,
  "remoteLockYxNum" INTEGER,
  "maxCommandInterval" INTEGER,
  "lock_v_up_up_yx_num" INTEGER,
  "lock_v_down_down_yx_num" INTEGER,
  "lock_total_q_yx_num" INTEGER,
  "lock_comm_yx_num" INTEGER,
  "lock_data_error_yx_num" INTEGER,
  "MaxCount" INTEGER DEFAULT 40,
  "CallBackYxNum" INTEGER DEFAULT (-1),
  PRIMARY KEY ("busbarNum"),
  CONSTRAINT "fkey0" FOREIGN KEY ("planType") REFERENCES "BUSBAR_PLANTYPE" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT "fkey1" FOREIGN KEY ("distributeMethod") REFERENCES "BUSBAR_DISTRIBUTE" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of BUSBAR
-- ----------------------------
INSERT INTO "BUSBAR" VALUES (0, '220Kv东母线', 245, 220, 300, 300, 11.0, 6, 0, 8, 8, 13, 0, 0, 1, 12, 2000.0, 240.0, 220.0, 0, 0, 0, 4, 5, 6, 0, 1, 0, 0, 4, 5, 0, 50.0, 0, 1, 0, 0, 8, 14, 1200, 7, 8, 9, 10, 11, 20, -1);
INSERT INTO "BUSBAR" VALUES (1, '220Kv西母线', 245, 220, 300, 300, 17.0, 6, 0, 14, 14, 24, 0, 0, 1, 23, 2000.0, 240.0, 220.0, 0, 0, 0, 15, 16, 17, 0, 1, 0, 0, 19, 20, 0, 50.0, 0, 0, 0, 1, 14, 25, 1200, 18, 19, 20, 21, 22, 20, -1);

PRAGMA foreign_keys = true;
