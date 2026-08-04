/*
 Navicat Premium Data Transfer

 Source Server         : QHeatAvcRtdb
 Source Server Type    : SQLite
 Source Server Version : 3035005 (3.35.5)
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3035005 (3.35.5)
 File Encoding         : 65001

 Date: 04/08/2026 09:36:27
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for ADJUST_LockType
-- ----------------------------
DROP TABLE IF EXISTS "ADJUST_LockType";
CREATE TABLE "ADJUST_LockType" (
  "id" SMALLINT,
  "name" VARCHAR(50),
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Records of ADJUST_LockType
-- ----------------------------
INSERT INTO "ADJUST_LockType" VALUES (1, '总闭锁');
INSERT INTO "ADJUST_LockType" VALUES (2, '闭锁升');
INSERT INTO "ADJUST_LockType" VALUES (3, '闭锁降');

-- ----------------------------
-- Table structure for ADJUST_USER_DEFINE_LOCK
-- ----------------------------
DROP TABLE IF EXISTS "ADJUST_USER_DEFINE_LOCK";
CREATE TABLE "ADJUST_USER_DEFINE_LOCK" (
  "genNum" integer,
  "lockType" integer,
  "lockNum" integer,
  "lockName" wvarchar(50),
  "relatedYxNum" integer,
  "restoreWaitTime" integer,
  "isManRestore" bit NOT NULL,
  PRIMARY KEY ("genNum", "lockType", "lockNum"),
  FOREIGN KEY ("genNum") REFERENCES "adjustpara" ("genNum") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("lockType") REFERENCES "ADJUST_LockType" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of ADJUST_USER_DEFINE_LOCK
-- ----------------------------

-- ----------------------------
-- Table structure for ADJUST_USER_DEFINE_LOCK_CONDITION_YC
-- ----------------------------
DROP TABLE IF EXISTS "ADJUST_USER_DEFINE_LOCK_CONDITION_YC";
CREATE TABLE "ADJUST_USER_DEFINE_LOCK_CONDITION_YC" (
  "genNum" integer,
  "lockType" integer,
  "lockNum" integer,
  "conditionNum" integer,
  "ycNum" integer,
  "checkType" integer,
  "threshold" float,
  "deadArea" float,
  PRIMARY KEY ("genNum", "lockType", "lockNum", "conditionNum"),
  FOREIGN KEY ("ycNum") REFERENCES "YC_DATA" ("yc_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("genNum", "lockType", "lockNum") REFERENCES "ADJUST_USER_DEFINE_LOCK" ("genNum", "lockType", "lockNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of ADJUST_USER_DEFINE_LOCK_CONDITION_YC
-- ----------------------------

-- ----------------------------
-- Table structure for ADJUST_USER_DEFINE_LOCK_CONDITION_YX
-- ----------------------------
DROP TABLE IF EXISTS "ADJUST_USER_DEFINE_LOCK_CONDITION_YX";
CREATE TABLE "ADJUST_USER_DEFINE_LOCK_CONDITION_YX" (
  "genNum" integer,
  "lockType" integer,
  "lockNum" integer,
  "conditionNum" integer,
  "yxNum" integer,
  "threshold" smallint,
  PRIMARY KEY ("genNum", "lockType", "lockNum", "conditionNum"),
  FOREIGN KEY ("yxNum") REFERENCES "YX_DATA" ("yx_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("genNum", "lockType", "lockNum") REFERENCES "ADJUST_USER_DEFINE_LOCK" ("genNum", "lockType", "lockNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of ADJUST_USER_DEFINE_LOCK_CONDITION_YX
-- ----------------------------

-- ----------------------------
-- Table structure for AVC_ADJUST_MODE
-- ----------------------------
DROP TABLE IF EXISTS "AVC_ADJUST_MODE";
CREATE TABLE "AVC_ADJUST_MODE" (
  "id" SMALLINT,
  "name" VARCHAR(50),
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Records of AVC_ADJUST_MODE
-- ----------------------------
INSERT INTO "AVC_ADJUST_MODE" VALUES (0, '全厂');
INSERT INTO "AVC_ADJUST_MODE" VALUES (1, '单机');

-- ----------------------------
-- Table structure for AVC_CONTROL_MODE
-- ----------------------------
DROP TABLE IF EXISTS "AVC_CONTROL_MODE";
CREATE TABLE "AVC_CONTROL_MODE" (
  "id" SMALLINT,
  "name" VARCHAR(50),
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Records of AVC_CONTROL_MODE
-- ----------------------------
INSERT INTO "AVC_CONTROL_MODE" VALUES (0, '闭环');
INSERT INTO "AVC_CONTROL_MODE" VALUES (1, '半闭环');
INSERT INTO "AVC_CONTROL_MODE" VALUES (2, '开环');

-- ----------------------------
-- Table structure for AVC_INFO
-- ----------------------------
DROP TABLE IF EXISTS "AVC_INFO";
CREATE TABLE "AVC_INFO" (
  "AVCName" wvarchar(50),
  "isSaveCurve" BOOLEAN,
  "isSaveHistory" BOOLEAN,
  "isValid31Plan" BOOLEAN,
  "SaveCurveInterval" integer,
  "SaveHistoryInterval" integer,
  "curveValidTime" integer,
  "historyValidTime" integer,
  "startTime" timestamp,
  "ControlMode" integer,
  "AVCStatusYxNum" integer,
  "AdjustMode" integer,
  "CallbackMode" INTEGER DEFAULT 0,
  PRIMARY KEY ("AVCName"),
  CONSTRAINT "fkey0" FOREIGN KEY ("ControlMode") REFERENCES "AVC_CONTROL_MODE" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT "fkey1" FOREIGN KEY ("AdjustMode") REFERENCES "AVC_ADJUST_MODE" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of AVC_INFO
-- ----------------------------
INSERT INTO "AVC_INFO" VALUES ('!B�AVC', 1, 1, 0, 60, 15, 30, 24, -1, 0, 1001, 0, 0);

-- ----------------------------
-- Table structure for AlarmObjectType
-- ----------------------------
DROP TABLE IF EXISTS "AlarmObjectType";
CREATE TABLE "AlarmObjectType" (
  "ObjectType" integer,
  "ObjectTypeName" wvarchar(50),
  PRIMARY KEY ("ObjectType")
);

-- ----------------------------
-- Records of AlarmObjectType
-- ----------------------------
INSERT INTO "AlarmObjectType" VALUES (3, '遥信');
INSERT INTO "AlarmObjectType" VALUES (4, '遥测');

-- ----------------------------
-- Table structure for AlarmPara
-- ----------------------------
DROP TABLE IF EXISTS "AlarmPara";
CREATE TABLE "AlarmPara" (
  "ObjectType" integer,
  "ObjectNum" integer,
  "AlarmType" integer,
  "AlarmGrade" integer,
  "AlarmTime" integer,
  "AlarmCount" integer,
  "OutModuleNum" integer,
  "OutRegAddr" integer,
  PRIMARY KEY ("ObjectType", "ObjectNum", "AlarmType"),
  FOREIGN KEY ("ObjectType", "AlarmType") REFERENCES "AlarmType" ("ObjectType", "AlarmType") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of AlarmPara
-- ----------------------------

-- ----------------------------
-- Table structure for AlarmType
-- ----------------------------
DROP TABLE IF EXISTS "AlarmType";
CREATE TABLE "AlarmType" (
  "ObjectType" integer,
  "AlarmType" integer,
  "AlarmName" wvarchar(50),
  PRIMARY KEY ("ObjectType", "AlarmType")
);

-- ----------------------------
-- Records of AlarmType
-- ----------------------------
INSERT INTO "AlarmType" VALUES (3, 0, '遥信合');
INSERT INTO "AlarmType" VALUES (3, 1, '遥信分');
INSERT INTO "AlarmType" VALUES (4, 0, '遥测');

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

-- ----------------------------
-- Table structure for BUSBAR_DISTRIBUTE
-- ----------------------------
DROP TABLE IF EXISTS "BUSBAR_DISTRIBUTE";
CREATE TABLE "BUSBAR_DISTRIBUTE" (
  "id" integer,
  "name" wvarchar(50),
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Records of BUSBAR_DISTRIBUTE
-- ----------------------------
INSERT INTO "BUSBAR_DISTRIBUTE" VALUES (0, '等功率因数');
INSERT INTO "BUSBAR_DISTRIBUTE" VALUES (1, '等微增率');
INSERT INTO "BUSBAR_DISTRIBUTE" VALUES (2, '平局分配');
INSERT INTO "BUSBAR_DISTRIBUTE" VALUES (3, '与容量成比例');

-- ----------------------------
-- Table structure for BUSBAR_GROUP
-- ----------------------------
DROP TABLE IF EXISTS "BUSBAR_GROUP";
CREATE TABLE "BUSBAR_GROUP" (
  "GroupNum" integer,
  "GroupName" wvarchar(50),
  "MainBarYcNum" integer,
  PRIMARY KEY ("GroupNum")
);

-- ----------------------------
-- Records of BUSBAR_GROUP
-- ----------------------------
INSERT INTO "BUSBAR_GROUP" VALUES (0, '220kv母线组', 3);

-- ----------------------------
-- Table structure for BUSBAR_PLANTYPE
-- ----------------------------
DROP TABLE IF EXISTS "BUSBAR_PLANTYPE";
CREATE TABLE "BUSBAR_PLANTYPE" (
  "id" INTEGER NOT NULL,
  "name" TEXT NOT NULL,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Records of BUSBAR_PLANTYPE
-- ----------------------------
INSERT INTO "BUSBAR_PLANTYPE" VALUES (0, '电压目标');
INSERT INTO "BUSBAR_PLANTYPE" VALUES (1, '电压增量');
INSERT INTO "BUSBAR_PLANTYPE" VALUES (2, '无功增量');

-- ----------------------------
-- Table structure for BUSBAR_VQ
-- ----------------------------
DROP TABLE IF EXISTS "BUSBAR_VQ";
CREATE TABLE "BUSBAR_VQ" (
  "line" integer,
  "V_Value" integer,
  "Q_value" integer,
  PRIMARY KEY ("line", "V_Value"),
  FOREIGN KEY ("line") REFERENCES "BUSBAR" ("busbarNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of BUSBAR_VQ
-- ----------------------------
INSERT INTO "BUSBAR_VQ" VALUES (0, 25, 550);
INSERT INTO "BUSBAR_VQ" VALUES (0, 98, 1500);
INSERT INTO "BUSBAR_VQ" VALUES (0, 176, 2500);
INSERT INTO "BUSBAR_VQ" VALUES (0, 254, 3200);
INSERT INTO "BUSBAR_VQ" VALUES (0, 332, 4000);
INSERT INTO "BUSBAR_VQ" VALUES (0, 410, 4700);
INSERT INTO "BUSBAR_VQ" VALUES (0, 488, 5500);
INSERT INTO "BUSBAR_VQ" VALUES (0, 566, 6500);
INSERT INTO "BUSBAR_VQ" VALUES (0, 644, 7500);
INSERT INTO "BUSBAR_VQ" VALUES (0, 722, 9300);
INSERT INTO "BUSBAR_VQ" VALUES (0, 800, 12000);
INSERT INTO "BUSBAR_VQ" VALUES (1, 25, 550);
INSERT INTO "BUSBAR_VQ" VALUES (1, 98, 1500);
INSERT INTO "BUSBAR_VQ" VALUES (1, 176, 2500);
INSERT INTO "BUSBAR_VQ" VALUES (1, 254, 3200);
INSERT INTO "BUSBAR_VQ" VALUES (1, 332, 4000);
INSERT INTO "BUSBAR_VQ" VALUES (1, 410, 4700);
INSERT INTO "BUSBAR_VQ" VALUES (1, 488, 5500);
INSERT INTO "BUSBAR_VQ" VALUES (1, 566, 6500);
INSERT INTO "BUSBAR_VQ" VALUES (1, 644, 7500);
INSERT INTO "BUSBAR_VQ" VALUES (1, 722, 9300);
INSERT INTO "BUSBAR_VQ" VALUES (1, 800, 12000);

-- ----------------------------
-- Table structure for BUSBAR_VRateParameter
-- ----------------------------
DROP TABLE IF EXISTS "BUSBAR_VRateParameter";
CREATE TABLE "BUSBAR_VRateParameter" (
  "BusbarNum" integer,
  "VString" wvarchar(50),
  "VRateUpLimit" float,
  "VRateDownLimit" float,
  PRIMARY KEY ("BusbarNum")
);

-- ----------------------------
-- Records of BUSBAR_VRateParameter
-- ----------------------------
INSERT INTO "BUSBAR_VRateParameter" VALUES (0, '220KV', 235.0, 232.0);
INSERT INTO "BUSBAR_VRateParameter" VALUES (1, '220KV', 235.0, 232.0);

-- ----------------------------
-- Table structure for BUS_BUS_LINK
-- ----------------------------
DROP TABLE IF EXISTS "BUS_BUS_LINK";
CREATE TABLE "BUS_BUS_LINK" (
  "Link_num" smallint,
  "LeftMxNum" smallint,
  "RightMxNum" smallint,
  PRIMARY KEY ("Link_num"),
  FOREIGN KEY ("LeftMxNum") REFERENCES "BUSBAR" ("busbarNum") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("RightMxNum") REFERENCES "BUSBAR" ("busbarNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of BUS_BUS_LINK
-- ----------------------------
INSERT INTO "BUS_BUS_LINK" VALUES (0, 0, 1);

-- ----------------------------
-- Table structure for BUS_BUS_LINK_LOGIC
-- ----------------------------
DROP TABLE IF EXISTS "BUS_BUS_LINK_LOGIC";
CREATE TABLE "BUS_BUS_LINK_LOGIC" (
  "LinkNum" smallint,
  "LogicNum" smallint,
  "LogicName" wvarchar(50),
  PRIMARY KEY ("LinkNum", "LogicNum"),
  FOREIGN KEY ("LinkNum") REFERENCES "BUS_BUS_LINK" ("Link_num") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of BUS_BUS_LINK_LOGIC
-- ----------------------------
INSERT INTO "BUS_BUS_LINK_LOGIC" VALUES (0, 0, '220kv母联开关系统连接');

-- ----------------------------
-- Table structure for BUS_BUS_LINK_LOGIC_DEV
-- ----------------------------
DROP TABLE IF EXISTS "BUS_BUS_LINK_LOGIC_DEV";
CREATE TABLE "BUS_BUS_LINK_LOGIC_DEV" (
  "LinkNum" smallint,
  "LogicNum" smallint,
  "DevNum" smallint,
  "YxNum" smallint,
  PRIMARY KEY ("LinkNum", "LogicNum", "DevNum"),
  FOREIGN KEY ("LinkNum", "LogicNum") REFERENCES "BUS_BUS_LINK_LOGIC" ("LinkNum", "LogicNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of BUS_BUS_LINK_LOGIC_DEV
-- ----------------------------
INSERT INTO "BUS_BUS_LINK_LOGIC_DEV" VALUES (0, 0, 0, 1);
INSERT INTO "BUS_BUS_LINK_LOGIC_DEV" VALUES (0, 0, 1, 2);
INSERT INTO "BUS_BUS_LINK_LOGIC_DEV" VALUES (0, 0, 2, 3);

-- ----------------------------
-- Table structure for BUS_GEN_LINK
-- ----------------------------
DROP TABLE IF EXISTS "BUS_GEN_LINK";
CREATE TABLE "BUS_GEN_LINK" (
  "MxNum" smallint,
  "LinkNum" smallint,
  "GenNum" integer,
  PRIMARY KEY ("MxNum", "LinkNum"),
  FOREIGN KEY ("MxNum") REFERENCES "BUSBAR" ("busbarNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of BUS_GEN_LINK
-- ----------------------------
INSERT INTO "BUS_GEN_LINK" VALUES (0, 0, 0);
INSERT INTO "BUS_GEN_LINK" VALUES (1, 0, 0);
INSERT INTO "BUS_GEN_LINK" VALUES (0, 1, 1);
INSERT INTO "BUS_GEN_LINK" VALUES (1, 1, 1);

-- ----------------------------
-- Table structure for BUS_GEN_LINK_LOGIC
-- ----------------------------
DROP TABLE IF EXISTS "BUS_GEN_LINK_LOGIC";
CREATE TABLE "BUS_GEN_LINK_LOGIC" (
  "MxNum" smallint,
  "LinkNum" smallint,
  "LogicNum" smallint,
  "LogicName" wvarchar(50),
  PRIMARY KEY ("MxNum", "LinkNum", "LogicNum"),
  FOREIGN KEY ("MxNum", "LinkNum") REFERENCES "BUS_GEN_LINK" ("MxNum", "LinkNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of BUS_GEN_LINK_LOGIC
-- ----------------------------
INSERT INTO "BUS_GEN_LINK_LOGIC" VALUES (0, 0, 0, '220kv正母与1号机组连接逻辑');
INSERT INTO "BUS_GEN_LINK_LOGIC" VALUES (1, 0, 0, '220kv副母与1号机组连接逻辑');
INSERT INTO "BUS_GEN_LINK_LOGIC" VALUES (0, 1, 0, '220kv正母与2号机组连接逻辑');
INSERT INTO "BUS_GEN_LINK_LOGIC" VALUES (1, 1, 0, '220kv副母与1号机组连接逻辑');

-- ----------------------------
-- Table structure for BUS_GEN_LINK_LOGIC_DEV
-- ----------------------------
DROP TABLE IF EXISTS "BUS_GEN_LINK_LOGIC_DEV";
CREATE TABLE "BUS_GEN_LINK_LOGIC_DEV" (
  "MxNum" smallint,
  "LinkNum" smallint,
  "LogicNum" smallint,
  "DevNum" smallint,
  "YxNum" smallint,
  PRIMARY KEY ("MxNum", "LinkNum", "LogicNum", "DevNum"),
  FOREIGN KEY ("MxNum", "LinkNum", "LogicNum") REFERENCES "BUS_GEN_LINK_LOGIC" ("MxNum", "LinkNum", "LogicNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of BUS_GEN_LINK_LOGIC_DEV
-- ----------------------------
INSERT INTO "BUS_GEN_LINK_LOGIC_DEV" VALUES (0, 0, 0, 0, 201);
INSERT INTO "BUS_GEN_LINK_LOGIC_DEV" VALUES (0, 0, 0, 1, 202);
INSERT INTO "BUS_GEN_LINK_LOGIC_DEV" VALUES (1, 0, 0, 0, 201);
INSERT INTO "BUS_GEN_LINK_LOGIC_DEV" VALUES (1, 0, 0, 1, 203);
INSERT INTO "BUS_GEN_LINK_LOGIC_DEV" VALUES (0, 1, 0, 0, 301);
INSERT INTO "BUS_GEN_LINK_LOGIC_DEV" VALUES (0, 1, 0, 1, 302);
INSERT INTO "BUS_GEN_LINK_LOGIC_DEV" VALUES (1, 1, 0, 0, 301);
INSERT INTO "BUS_GEN_LINK_LOGIC_DEV" VALUES (1, 1, 0, 1, 303);

-- ----------------------------
-- Table structure for CHUNNEL_DD
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_DD";
CREATE TABLE "CHUNNEL_DD" (
  "ch_dd_num" integer,
  "chunnel_num" integer,
  "io_num" integer,
  "func_num" integer,
  "path_num" integer,
  "dd_num" integer,
  "coefficient" numeric,
  "offset" numeric,
  "other" numeric,
  PRIMARY KEY ("chunnel_num", "io_num", "func_num", "path_num"),
  FOREIGN KEY ("chunnel_num") REFERENCES "chunnel" ("chunnel_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("dd_num") REFERENCES "DD_DATA" ("dd_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  UNIQUE ("ch_dd_num" ASC),
  UNIQUE ("chunnel_num" ASC, "dd_num" ASC)
);

-- ----------------------------
-- Records of CHUNNEL_DD
-- ----------------------------

-- ----------------------------
-- Table structure for CHUNNEL_MODULE_MODBUS
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_MODULE_MODBUS";
CREATE TABLE "CHUNNEL_MODULE_MODBUS" (
  "ChunnelNum" INTEGER,
  "ModuleAddress" INTEGER,
  "ModuleName" CHAR(7),
  "PortType" INTEGER,
  "HaveYC" BOOLEAN,
  "YCFunctionCode" INTEGER,
  "YCDataType" INTEGER,
  "YCDataOrder" INTEGER,
  "YCStartNum" INTEGER,
  "YCSendInterval" INTEGER,
  "HaveYX" BOOLEAN,
  "YXFunctionCode" INTEGER,
  "YXDataType" INTEGER,
  "YXDataOrder" INTEGER,
  "YXStartNum" INTEGER,
  "YXSendInterval" INTEGER,
  "YKFunctionCode" INTEGER,
  "YTFunctionCode" INTEGER
);

-- ----------------------------
-- Records of CHUNNEL_MODULE_MODBUS
-- ----------------------------
INSERT INTO "CHUNNEL_MODULE_MODBUS" VALUES (0, 1, '1#机组采集', 1, 1, 3, 3, 0, 1098, 100, 0, 3, 1, 1, 528, 100, 5, 16);
INSERT INTO "CHUNNEL_MODULE_MODBUS" VALUES (0, 2, '1#高压采集', 1, 1, 3, 3, 0, 1118, 100, 0, 3, 1, 1, 4, 100, 5, 16);
INSERT INTO "CHUNNEL_MODULE_MODBUS" VALUES (0, 3, '2#机组采集', 1, 1, 3, 3, 0, 1098, 100, 0, 3, 1, 1, 528, 100, 5, 16);
INSERT INTO "CHUNNEL_MODULE_MODBUS" VALUES (0, 4, '2#高压采集', 1, 1, 3, 3, 0, 1118, 100, 0, 3, 1, 1, 528, 100, 5, 16);
INSERT INTO "CHUNNEL_MODULE_MODBUS" VALUES (2, 7, '1#2#PLC', 1, 1, 3, 3, 0, 0, 300, 1, 2, 1, 1, 0, 300, 5, 6);

-- ----------------------------
-- Table structure for CHUNNEL_PROTOCOL_TYPE
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_PROTOCOL_TYPE";
CREATE TABLE "CHUNNEL_PROTOCOL_TYPE" (
  "chunnel_type_id" integer,
  "protocol_type_id" integer,
  "protocol_type_name" wvarchar(255),
  PRIMARY KEY ("chunnel_type_id", "protocol_type_id"),
  FOREIGN KEY ("chunnel_type_id") REFERENCES "CHUNNEL_TYPE" ("chunnel_type_id") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of CHUNNEL_PROTOCOL_TYPE
-- ----------------------------
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 1, 'CDT');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 2, '9702');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 3, '101');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 4, '104');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 5, 'MODBUS');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 6, 'TH');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 7, 'SIM');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 8, 'ML101CAN');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 9, 'DNB');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 11, 'KDB');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 12, '104HH');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 13, 'MODBUSTCP');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 14, '104NR');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 1, 'CDT');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 2, '9702');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 3, '101');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 4, '104');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 5, 'MODBUS');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 6, 'TH');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 7, 'SIM');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 8, 'ML101CAN');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 9, 'DNB');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 10, '102KD');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (2, 11, 'HAOJIN');
INSERT INTO "CHUNNEL_PROTOCOL_TYPE" VALUES (1, 15, 'GPS');

-- ----------------------------
-- Table structure for CHUNNEL_SOE
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_SOE";
CREATE TABLE "CHUNNEL_SOE" (
  "ch_soe_num" integer,
  "chunnel_num" integer,
  "io_num" integer,
  "func_num" integer,
  "path_num" integer,
  "yx_num" integer,
  "other" numeric,
  PRIMARY KEY ("ch_soe_num"),
  FOREIGN KEY ("chunnel_num") REFERENCES "chunnel" ("chunnel_num") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of CHUNNEL_SOE
-- ----------------------------

-- ----------------------------
-- Table structure for CHUNNEL_TYPE
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_TYPE";
CREATE TABLE "CHUNNEL_TYPE" (
  "chunnel_type_id" smallint,
  "chunnel_type_name" wvarchar(50),
  PRIMARY KEY ("chunnel_type_id")
);

-- ----------------------------
-- Records of CHUNNEL_TYPE
-- ----------------------------
INSERT INTO "CHUNNEL_TYPE" VALUES (1, 'GETHER                            ');
INSERT INTO "CHUNNEL_TYPE" VALUES (2, 'TRANSMIT                                          ');

-- ----------------------------
-- Table structure for CHUNNEL_YC
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_YC";
CREATE TABLE "CHUNNEL_YC" (
  "ch_yc_num" INTEGER,
  "chunnel_num" INTEGER,
  "io_num" INTEGER,
  "func_num" INTEGER,
  "path_num" INTEGER,
  "yc_num" INTEGER,
  "coefficient" FLOAT,
  "offset" INTEGER,
  "other" INTEGER
);

-- ----------------------------
-- Records of CHUNNEL_YC
-- ----------------------------
INSERT INTO "CHUNNEL_YC" VALUES (1, 0, 1, 0, 1142, 216, 100.0, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (2, 0, 1, 0, 1146, 217, 100.0, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (3, 0, 1, 0, 1119, 210, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (4, 0, 1, 0, 1099, 213, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (5, 0, 1, 0, 1179, 209, 0.01, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (6, 0, 1, 0, 1162, 218, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (7, 0, 2, 0, 1119, 8, 0.01, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (8, 0, 3, 0, 1142, 316, 100.0, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (9, 0, 3, 0, 1146, 317, 100.0, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (10, 0, 3, 0, 1119, 310, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (11, 0, 3, 0, 1099, 313, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (12, 0, 3, 0, 1179, 309, 0.01, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (13, 0, 3, 0, 1162, 318, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (14, 0, 4, 0, 1119, 14, 0.01, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (15, 2, 7, 3, 0, 208, 0.976800977, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (16, 2, 7, 3, 1, 308, 0.976800977, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (21, 3, 0, 0, 16385, 203, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (22, 3, 0, 0, 16386, 204, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (25, 3, 0, 0, 16387, 303, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (26, 3, 0, 0, 16388, 304, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (33, 3, 0, 0, 16389, 1, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (34, 4, 0, 0, 16385, 203, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (35, 4, 0, 0, 16386, 204, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (36, 4, 0, 0, 16387, 303, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (37, 4, 0, 0, 16388, 304, 0.001, 0, 0);
INSERT INTO "CHUNNEL_YC" VALUES (38, 4, 0, 0, 16389, 1, 1.0, 0, 0);

-- ----------------------------
-- Table structure for CHUNNEL_YK
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_YK";
CREATE TABLE "CHUNNEL_YK" (
  "ch_yk_num" integer,
  "chunnel_num" integer,
  "close_io_num" integer,
  "close_func_num" integer,
  "close_path_num" integer,
  "open_io_num" integer,
  "open_func_num" integer,
  "open_path_num" integer,
  "yk_num" integer,
  "other" numeric,
  PRIMARY KEY ("chunnel_num", "close_io_num", "close_func_num", "close_path_num", "open_io_num", "open_func_num", "open_path_num"),
  FOREIGN KEY ("chunnel_num") REFERENCES "chunnel" ("chunnel_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("yk_num") REFERENCES "YK_DATA" ("yk_num") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of CHUNNEL_YK
-- ----------------------------
INSERT INTO "CHUNNEL_YK" VALUES (0, 2, 7, 5, 12, 7, 5, 12, 0, 0);
INSERT INTO "CHUNNEL_YK" VALUES (2, 2, 7, 5, 14, 7, 5, 14, 2, 0);
INSERT INTO "CHUNNEL_YK" VALUES (3, 2, 7, 5, 15, 7, 5, 15, 3, 0);

-- ----------------------------
-- Table structure for CHUNNEL_YS
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_YS";
CREATE TABLE "CHUNNEL_YS" (
  "ch_ys_num" INTEGER,
  "chunnel_num" INTEGER,
  "io_num" INTEGER,
  "func_num" INTEGER,
  "path_num" INTEGER,
  "ys_num" INTEGER,
  "coefficient" FLOAT,
  "offset" INTEGER,
  "other" INTEGER
);

-- ----------------------------
-- Records of CHUNNEL_YS
-- ----------------------------
INSERT INTO "CHUNNEL_YS" VALUES (1, 2, 7, 6, 10, 101, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (2, 3, 0, 0, 25089, 2, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (3, 2, 7, 6, 18, 11, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (4, 2, 7, 6, 19, 12, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (5, 2, 7, 6, 20, 13, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (6, 2, 7, 6, 21, 14, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (11, 3, 0, 0, 25091, 4, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (7, 3, 0, 0, 25090, 3, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (12, 4, 0, 0, 25089, 2, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (13, 4, 0, 0, 25090, 3, 1.0, 0, 0);
INSERT INTO "CHUNNEL_YS" VALUES (14, 4, 0, 0, 25091, 4, 1.0, 0, 0);

-- ----------------------------
-- Table structure for CHUNNEL_YT
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_YT";
CREATE TABLE "CHUNNEL_YT" (
  "ch_yt_num" integer,
  "chunnel_num" integer,
  "up_io_num" integer,
  "up_func_num" integer,
  "up_path_num" integer,
  "down_io_num" integer,
  "down_func_num" integer,
  "down_path_num" integer,
  "stop_io_num" integer,
  "stop_func_num" integer,
  "stop_path_num" integer,
  "yt_num" integer,
  "other" numeric,
  PRIMARY KEY ("chunnel_num", "up_io_num", "up_func_num", "up_path_num", "down_io_num", "down_func_num", "down_path_num", "stop_io_num", "stop_func_num", "stop_path_num"),
  FOREIGN KEY ("chunnel_num") REFERENCES "chunnel" ("chunnel_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("yt_num") REFERENCES "YT_DATA" ("yt_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  UNIQUE ("ch_yt_num" ASC),
  UNIQUE ("chunnel_num" ASC, "yt_num" ASC)
);

-- ----------------------------
-- Records of CHUNNEL_YT
-- ----------------------------

-- ----------------------------
-- Table structure for CHUNNEL_YX
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_YX";
CREATE TABLE "CHUNNEL_YX" (
  "ch_yx_num" INTEGER,
  "chunnel_num" INTEGER,
  "io_num" INTEGER,
  "func_num" INTEGER,
  "path_num" INTEGER,
  "yx_num" INTEGER,
  "other" INTEGER
);

-- ----------------------------
-- Records of CHUNNEL_YX
-- ----------------------------
INSERT INTO "CHUNNEL_YX" VALUES (1, 2, 7, 2, 0, 204, 0);
INSERT INTO "CHUNNEL_YX" VALUES (2, 2, 7, 2, 1, 205, 0);
INSERT INTO "CHUNNEL_YX" VALUES (3, 2, 7, 2, 2, 304, 0);
INSERT INTO "CHUNNEL_YX" VALUES (4, 2, 7, 2, 3, 305, 0);
INSERT INTO "CHUNNEL_YX" VALUES (5, 2, 7, 2, 4, 1006, 0);
INSERT INTO "CHUNNEL_YX" VALUES (6, 2, 7, 2, 5, 1007, 0);
INSERT INTO "CHUNNEL_YX" VALUES (7, 2, 7, 2, 12, 1200, 0);
INSERT INTO "CHUNNEL_YX" VALUES (8, 2, 7, 2, 14, 1201, 0);
INSERT INTO "CHUNNEL_YX" VALUES (9, 2, 7, 2, 15, 1202, 0);
INSERT INTO "CHUNNEL_YX" VALUES (11, 3, 0, 0, 1, 1001, 0);
INSERT INTO "CHUNNEL_YX" VALUES (12, 3, 0, 0, 2, 2003, 0);
INSERT INTO "CHUNNEL_YX" VALUES (13, 3, 0, 0, 3, 210, 0);
INSERT INTO "CHUNNEL_YX" VALUES (14, 3, 0, 0, 4, 208, 0);
INSERT INTO "CHUNNEL_YX" VALUES (15, 3, 0, 0, 5, 209, 0);
INSERT INTO "CHUNNEL_YX" VALUES (16, 3, 0, 0, 6, 310, 0);
INSERT INTO "CHUNNEL_YX" VALUES (17, 3, 0, 0, 7, 308, 0);
INSERT INTO "CHUNNEL_YX" VALUES (18, 3, 0, 0, 8, 309, 0);
INSERT INTO "CHUNNEL_YX" VALUES (20, 4, 0, 0, 1, 1001, 0);
INSERT INTO "CHUNNEL_YX" VALUES (21, 4, 0, 0, 2, 2003, 0);
INSERT INTO "CHUNNEL_YX" VALUES (22, 4, 0, 0, 3, 210, 0);
INSERT INTO "CHUNNEL_YX" VALUES (23, 4, 0, 0, 4, 208, 0);
INSERT INTO "CHUNNEL_YX" VALUES (24, 4, 0, 0, 5, 209, 0);
INSERT INTO "CHUNNEL_YX" VALUES (25, 4, 0, 0, 6, 310, 0);
INSERT INTO "CHUNNEL_YX" VALUES (26, 4, 0, 0, 7, 308, 0);
INSERT INTO "CHUNNEL_YX" VALUES (27, 4, 0, 0, 8, 309, 0);

-- ----------------------------
-- Table structure for CHUNNEL_YcFunctionCode
-- ----------------------------
DROP TABLE IF EXISTS "CHUNNEL_YcFunctionCode";
CREATE TABLE "CHUNNEL_YcFunctionCode" (
  "ID" INTEGER,
  "name" TEXT,
  PRIMARY KEY ("ID")
);

-- ----------------------------
-- Records of CHUNNEL_YcFunctionCode
-- ----------------------------
INSERT INTO "CHUNNEL_YcFunctionCode" VALUES (0, '无');
INSERT INTO "CHUNNEL_YcFunctionCode" VALUES (9, '整型');
INSERT INTO "CHUNNEL_YcFunctionCode" VALUES (13, '浮点型');

-- ----------------------------
-- Table structure for ControlCurve
-- ----------------------------
DROP TABLE IF EXISTS "ControlCurve";
CREATE TABLE "ControlCurve" (
  "No" integer,
  "genNum" integer,
  "P1_DeltaValue" float,
  "P1_SendDownValue" float,
  "P1_P2_FragmentNum" integer,
  "P2_DeltaValue" float,
  "P2_SendDownValue" float,
  "P2_P3_FragmentNum" integer,
  "P3_DeltaValue" float,
  "P3_SendDownValue" float,
  "P3_P4_FragmentNum" integer,
  "P4_DeltaValue" float,
  "P4_SendDownValue" float,
  "Type" integer
);

-- ----------------------------
-- Records of ControlCurve
-- ----------------------------

-- ----------------------------
-- Table structure for DD_ATTRIBUTE
-- ----------------------------
DROP TABLE IF EXISTS "DD_ATTRIBUTE";
CREATE TABLE "DD_ATTRIBUTE" (
  "attribute_id" float,
  "attribute_name" wvarchar(50),
  PRIMARY KEY ("attribute_id")
);

-- ----------------------------
-- Records of DD_ATTRIBUTE
-- ----------------------------
INSERT INTO "DD_ATTRIBUTE" VALUES (0.0, '其他属性');
INSERT INTO "DD_ATTRIBUTE" VALUES (1.0, '主变电度');
INSERT INTO "DD_ATTRIBUTE" VALUES (2.0, '线路电度');

-- ----------------------------
-- Table structure for DD_DATA
-- ----------------------------
DROP TABLE IF EXISTS "DD_DATA";
CREATE TABLE "DD_DATA" (
  "dd_num" integer,
  "dd_code" wvarchar(50),
  "rtu_num" integer,
  "line_num" integer,
  "define" integer,
  "attribute" integer,
  "dd_data" float,
  "update_time" timestamp,
  PRIMARY KEY ("dd_num"),
  FOREIGN KEY ("attribute") REFERENCES "DD_ATTRIBUTE" ("attribute_id") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of DD_DATA
-- ----------------------------

-- ----------------------------
-- Table structure for DISTRIBUTE_LockType
-- ----------------------------
DROP TABLE IF EXISTS "DISTRIBUTE_LockType";
CREATE TABLE "DISTRIBUTE_LockType" (
  "id" SMALLINT,
  "name" VARCHAR(50),
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Records of DISTRIBUTE_LockType
-- ----------------------------
INSERT INTO "DISTRIBUTE_LockType" VALUES (1, '总闭锁');
INSERT INTO "DISTRIBUTE_LockType" VALUES (2, '闭锁升');
INSERT INTO "DISTRIBUTE_LockType" VALUES (3, '闭锁降');

-- ----------------------------
-- Table structure for DISTRIBUTE_USER_DEFINE_LOCK
-- ----------------------------
DROP TABLE IF EXISTS "DISTRIBUTE_USER_DEFINE_LOCK";
CREATE TABLE "DISTRIBUTE_USER_DEFINE_LOCK" (
  "busNum" integer,
  "lockType" integer,
  "lockNum" integer,
  "lockName" wvarchar(50),
  "relatedYxNum" integer,
  "restoreWaitTime" integer,
  "isManRestore" bit NOT NULL,
  PRIMARY KEY ("busNum", "lockType", "lockNum"),
  FOREIGN KEY ("busNum") REFERENCES "BUSBAR" ("busbarNum") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("lockType") REFERENCES "DISTRIBUTE_LockType" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of DISTRIBUTE_USER_DEFINE_LOCK
-- ----------------------------
INSERT INTO "DISTRIBUTE_USER_DEFINE_LOCK" VALUES (0, 1, 0, '220KV南母通讯故障总闭锁', 10, 0, 0);
INSERT INTO "DISTRIBUTE_USER_DEFINE_LOCK" VALUES (1, 1, 0, '220KV北母线通讯故障总闭锁', 21, 0, 0);

-- ----------------------------
-- Table structure for DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YC
-- ----------------------------
DROP TABLE IF EXISTS "DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YC";
CREATE TABLE "DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YC" (
  "busNum" integer,
  "lockType" integer,
  "lockNum" integer,
  "conditionNum" integer,
  "ycNum" integer,
  "checkType" integer,
  "threshold" float,
  "deadArea" float,
  PRIMARY KEY ("busNum", "lockType", "lockNum", "conditionNum"),
  FOREIGN KEY ("busNum", "lockType", "lockNum") REFERENCES "DISTRIBUTE_USER_DEFINE_LOCK" ("busNum", "lockType", "lockNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YC
-- ----------------------------

-- ----------------------------
-- Table structure for DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YX
-- ----------------------------
DROP TABLE IF EXISTS "DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YX";
CREATE TABLE "DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YX" (
  "busNum" integer,
  "lockType" integer,
  "lockNum" integer,
  "conditionNum" integer,
  "yxNum" integer,
  "threshold" smallint,
  PRIMARY KEY ("busNum", "lockType", "lockNum", "conditionNum"),
  FOREIGN KEY ("yxNum") REFERENCES "YX_DATA" ("yx_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("busNum", "lockType", "lockNum") REFERENCES "DISTRIBUTE_USER_DEFINE_LOCK" ("busNum", "lockType", "lockNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YX
-- ----------------------------
INSERT INTO "DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YX" VALUES (0, 1, 0, 0, 1004, 0);
INSERT INTO "DISTRIBUTE_USER_DEFINE_LOCK_CONDITION_YX" VALUES (1, 1, 0, 0, 1004, 0);

-- ----------------------------
-- Table structure for GENERATOR
-- ----------------------------
DROP TABLE IF EXISTS "GENERATOR";
CREATE TABLE "GENERATOR" (
  "generatorNum" integer,
  "name" wvarchar(50),
  "shortReactance" float,
  "synReactance" float,
  "maxStatorVolt" float,
  "maxStatorCurrent" float,
  "ratingStatorVolt" float,
  "ratingStatorCurrent" float,
  "maxRotorVolt" float,
  "maxRotorCurrent" float,
  "ratingRotorVolt" float,
  "ratingRotorCurrent" float,
  "ratingQPower" float,
  "ratingPPower" float,
  "ratingCos" float,
  "linkReactance" float,
  "maxDelta" float,
  "maxPPower" float,
  "minPPower" float,
  "maxQPower" float,
  "minQPower" float,
  "maxAdjustQ" float,
  "planQYcNum" smallint,
  "pYcNum" smallint,
  "qYcNum" smallint,
  "vLLAvgYcNum" smallint,
  "iAvgYcNum" smallint,
  "fYcNum" smallint,
  "pfYcNum" smallint,
  "rotorCurrentYcNum" smallint,
  "pYcNum_1" smallint,
  "qYcNum_1" smallint,
  "vLLAvgYcNum_1" smallint,
  "iAvgYcNum_1" smallint,
  "fYcNum_1" smallint,
  "pfYcNum_1" smallint,
  "rotorCurrentYcNum_1" smallint,
  "planQExcuteYcNum" smallint,
  "ratingFactoryVoltA" FLOAT,
  "factoryVoltAYcNum" integer,
  "ratingFactoryVoltB" FLOAT,
  "factoryVoltBYcNum" integer,
  "QUpLimitYcNum" smallint,
  "QDownLimitYcNum" smallint,
  PRIMARY KEY ("generatorNum")
);

-- ----------------------------
-- Records of GENERATOR
-- ----------------------------
INSERT INTO "GENERATOR" VALUES (0, '一号机组', 0.25, 2.6, 26.0, 11887.0, 20.0, 10190.0, 308.0, 3000.0, 542.0, 2642.0, 200000.0, 300000.0, 0.85, 0.25, 70.0, 300000.0, 0.0, 200000.0, -100000.0, 30000.0, 202, 216, 217, 210, 213, 209, 218, 208, 216, 217, 210, 213, 209, 218, 208, 201, 6300.0, 219, 6300.0, 220, 203, 204);
INSERT INTO "GENERATOR" VALUES (1, '二号机组', 0.25, 2.6, 26.0, 11887.0, 20.0, 10190.0, 308.0, 3000.0, 542.0, 2642.0, 200000.0, 300000.0, 0.85, 0.25, 70.0, 300000.0, 0.0, 200000.0, -100000.0, 30000.0, 302, 316, 317, 310, 313, 309, 318, 308, 316, 317, 310, 313, 309, 318, 308, 301, 6300.0, 319, 6300.0, 320, 303, 304);

-- ----------------------------
-- Table structure for GENERATOR_P_QLimit
-- ----------------------------
DROP TABLE IF EXISTS "GENERATOR_P_QLimit";
CREATE TABLE "GENERATOR_P_QLimit" (
  "genNum" integer,
  "p" float,
  "qUpLimit" float,
  "qDownLimit" float,
  PRIMARY KEY ("genNum", "p"),
  FOREIGN KEY ("genNum") REFERENCES "GENERATOR" ("generatorNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of GENERATOR_P_QLimit
-- ----------------------------
INSERT INTO "GENERATOR_P_QLimit" VALUES (0, 10000.0, 150000.0, 10000.0);
INSERT INTO "GENERATOR_P_QLimit" VALUES (0, 50000.0, 150000.0, 10000.0);
INSERT INTO "GENERATOR_P_QLimit" VALUES (0, 100000.0, 150000.0, 10000.0);
INSERT INTO "GENERATOR_P_QLimit" VALUES (0, 200000.0, 150000.0, 10000.0);
INSERT INTO "GENERATOR_P_QLimit" VALUES (0, 350000.0, 150000.0, 10000.0);
INSERT INTO "GENERATOR_P_QLimit" VALUES (1, 10000.0, 150000.0, 10000.0);
INSERT INTO "GENERATOR_P_QLimit" VALUES (1, 80000.0, 150000.0, 10000.0);
INSERT INTO "GENERATOR_P_QLimit" VALUES (1, 100000.0, 150000.0, 10000.0);
INSERT INTO "GENERATOR_P_QLimit" VALUES (1, 200000.0, 150000.0, 10000.0);
INSERT INTO "GENERATOR_P_QLimit" VALUES (1, 350000.0, 150000.0, 10000.0);

-- ----------------------------
-- Table structure for GENERATOR_QTime
-- ----------------------------
DROP TABLE IF EXISTS "GENERATOR_QTime";
CREATE TABLE "GENERATOR_QTime" (
  "genNum" INTEGER,
  "Q_Value" INTEGER,
  "Time_Value" INTEGER,
  "Verify_Value" INTEGER,
  "Precent" INTEGER
);

-- ----------------------------
-- Records of GENERATOR_QTime
-- ----------------------------
INSERT INTO "GENERATOR_QTime" VALUES (1, 600, 10, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 1140, 19, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 1680, 28, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 2220, 37, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 2760, 46, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 3300, 55, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 3840, 64, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 4380, 73, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 4920, 82, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 5460, 91, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (1, 6000, 100, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 600, 10, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 1140, 19, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 1680, 28, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 2220, 37, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 2760, 46, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 3300, 55, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 3840, 64, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 4380, 73, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 4920, 82, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 5460, 91, 0, 0);
INSERT INTO "GENERATOR_QTime" VALUES (0, 6000, 100, 0, 0);

-- ----------------------------
-- Table structure for GENERATOR_VTime
-- ----------------------------
DROP TABLE IF EXISTS "GENERATOR_VTime";
CREATE TABLE "GENERATOR_VTime" (
  "genNum" INTEGER,
  "V_Value" INTEGER,
  "Time_Value" INTEGER,
  PRIMARY KEY ("genNum", "V_Value"),
  FOREIGN KEY ("genNum") REFERENCES "GENERATOR" ("generatorNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of GENERATOR_VTime
-- ----------------------------
INSERT INTO "GENERATOR_VTime" VALUES (0, 80, 1000);
INSERT INTO "GENERATOR_VTime" VALUES (0, 90, 1142);
INSERT INTO "GENERATOR_VTime" VALUES (0, 100, 1285);
INSERT INTO "GENERATOR_VTime" VALUES (0, 110, 1428);
INSERT INTO "GENERATOR_VTime" VALUES (0, 120, 1571);
INSERT INTO "GENERATOR_VTime" VALUES (0, 130, 1714);
INSERT INTO "GENERATOR_VTime" VALUES (0, 140, 1857);
INSERT INTO "GENERATOR_VTime" VALUES (0, 150, 2000);
INSERT INTO "GENERATOR_VTime" VALUES (1, 80, 1000);
INSERT INTO "GENERATOR_VTime" VALUES (1, 90, 1142);
INSERT INTO "GENERATOR_VTime" VALUES (1, 100, 1285);
INSERT INTO "GENERATOR_VTime" VALUES (1, 110, 1428);
INSERT INTO "GENERATOR_VTime" VALUES (1, 120, 1571);
INSERT INTO "GENERATOR_VTime" VALUES (1, 130, 1714);
INSERT INTO "GENERATOR_VTime" VALUES (1, 140, 1857);
INSERT INTO "GENERATOR_VTime" VALUES (1, 150, 2000);

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

-- ----------------------------
-- Table structure for Graph
-- ----------------------------
DROP TABLE IF EXISTS "Graph";
CREATE TABLE "Graph" (
  "GraphNum" integer,
  "GraphName" wvarchar(50),
  "GroupNum" integer,
  PRIMARY KEY ("GraphNum"),
  FOREIGN KEY ("GroupNum") REFERENCES "Graph_Group" ("GroupNum") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of Graph
-- ----------------------------
INSERT INTO "Graph" VALUES (0, '3号机组信息.qet', 0);
INSERT INTO "Graph" VALUES (1, '4号机组信息.qet', 1);
INSERT INTO "Graph" VALUES (2, '220Kv副母线.qet', 1);
INSERT INTO "Graph" VALUES (5, '3号机组.qet', 2);
INSERT INTO "Graph" VALUES (6, '4号机组.qet', 2);

-- ----------------------------
-- Table structure for Graph_Group
-- ----------------------------
DROP TABLE IF EXISTS "Graph_Group";
CREATE TABLE "Graph_Group" (
  "GroupNum" integer,
  "GroupName" wvarchar(50),
  PRIMARY KEY ("GroupNum")
);

-- ----------------------------
-- Records of Graph_Group
-- ----------------------------
INSERT INTO "Graph_Group" VALUES (0, '主系统图');
INSERT INTO "Graph_Group" VALUES (1, '母线');
INSERT INTO "Graph_Group" VALUES (2, '机组');

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

-- ----------------------------
-- Table structure for JS_DATA_STEP_VAR
-- ----------------------------
DROP TABLE IF EXISTS "JS_DATA_STEP_VAR";
CREATE TABLE "JS_DATA_STEP_VAR" (
  "js_num" INTEGER,
  "step_num" INTEGER,
  "var_no" INTEGER,
  "var_type" INTEGER,
  "var_num" INTEGER
);

-- ----------------------------
-- Records of JS_DATA_STEP_VAR
-- ----------------------------
INSERT INTO "JS_DATA_STEP_VAR" VALUES (0, 0, 0, 1, 217);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (0, 0, 1, 1, 317);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (1, 0, 0, 2, 204);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (1, 0, 1, 2, 205);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (2, 0, 0, 2, 304);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (2, 0, 1, 2, 305);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (3, 0, 0, 1, 1007);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (3, 0, 1, 1, 1003);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (4, 0, 0, 1, 1007);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (4, 0, 1, 1, 1003);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 0, 2, 217);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 1, 2, 219);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 2, 2, 220);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 3, 2, 221);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 4, 2, 222);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 5, 2, 223);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 6, 2, 224);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 7, 2, 225);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 0, 2, 218);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 1, 2, 226);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 2, 2, 227);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 3, 2, 228);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 4, 2, 229);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 5, 2, 234);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 6, 2, 235);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 0, 2, 317);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 1, 2, 319);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 2, 2, 320);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 3, 2, 321);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 4, 2, 322);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 5, 2, 323);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 6, 2, 324);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 7, 2, 325);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 0, 2, 318);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 1, 2, 326);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 2, 2, 327);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 3, 2, 328);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 4, 2, 329);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 5, 2, 334);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 6, 2, 335);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 0, 2, 211);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 1, 2, 212);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 2, 2, 213);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 3, 2, 214);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 4, 2, 215);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 5, 2, 216);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 6, 2, 230);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 7, 2, 231);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 8, 2, 232);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (9, 0, 9, 2, 233);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 0, 2, 311);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 1, 2, 312);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 2, 2, 313);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 3, 2, 314);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 4, 2, 315);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 5, 2, 316);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 6, 2, 330);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 7, 2, 331);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 8, 2, 332);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (10, 0, 9, 2, 333);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (11, 0, 0, 2, 208);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (11, 0, 1, 2, 209);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (12, 0, 0, 2, 308);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (12, 0, 1, 2, 309);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 0, 2, 4);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 1, 2, 5);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 2, 2, 6);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 3, 2, 9);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 4, 2, 10);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 5, 2, 11);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 6, 2, 14);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 7, 2, 15);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 8, 2, 16);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 9, 2, 17);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 10, 2, 20);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 11, 2, 21);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 12, 2, 22);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (101, 0, 13, 2, 25);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (102, 0, 0, 2, 7);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (102, 0, 1, 2, 18);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (103, 0, 0, 2, 8);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (103, 0, 1, 2, 19);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 0, 2, 206);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 1, 2, 207);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 2, 2, 211);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 3, 2, 212);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 4, 2, 213);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 5, 2, 214);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 6, 2, 215);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 7, 2, 216);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 8, 2, 230);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 9, 2, 231);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 10, 2, 232);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 11, 2, 233);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (104, 0, 12, 2, 236);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (105, 0, 0, 2, 208);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (105, 0, 1, 2, 217);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (105, 0, 2, 2, 219);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (105, 0, 3, 2, 220);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (105, 0, 4, 2, 221);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (105, 0, 5, 2, 222);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (105, 0, 6, 2, 223);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (105, 0, 7, 2, 224);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (105, 0, 8, 2, 224);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (106, 0, 0, 2, 209);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (106, 0, 1, 2, 218);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (106, 0, 2, 2, 226);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (106, 0, 3, 2, 227);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (106, 0, 4, 2, 228);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (106, 0, 5, 2, 229);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (106, 0, 6, 2, 234);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (106, 0, 7, 2, 235);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 0, 2, 306);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 1, 2, 307);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 2, 2, 311);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 3, 2, 312);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 4, 2, 313);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 5, 2, 314);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 6, 2, 315);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 7, 2, 316);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 8, 2, 330);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 9, 2, 331);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 10, 2, 332);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 11, 2, 333);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (107, 0, 12, 2, 336);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (108, 0, 0, 2, 308);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (108, 0, 1, 2, 317);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (108, 0, 2, 2, 319);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (108, 0, 3, 2, 320);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (108, 0, 4, 2, 321);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (108, 0, 5, 2, 322);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (108, 0, 6, 2, 323);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (108, 0, 7, 2, 324);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (108, 0, 8, 2, 325);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (109, 0, 0, 2, 309);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (109, 0, 1, 2, 318);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (109, 0, 2, 2, 326);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (109, 0, 3, 2, 327);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (109, 0, 4, 2, 328);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (109, 0, 5, 2, 329);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (109, 0, 6, 2, 334);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (109, 0, 7, 2, 335);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 7, 2, 239);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (13, 0, 0, 1, 217);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (13, 0, 1, 1, 317);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (14, 0, 0, 2, 12);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (14, 0, 1, 2, 23);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (15, 0, 0, 2, 1002);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 7, 2, 339);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (16, 0, 0, 1, 1);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (16, 0, 1, 1, 1005);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (17, 0, 0, 1, 1);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (17, 0, 1, 1, 1006);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 8, 2, 1103);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 8, 2, 1103);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 8, 2, 1104);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 8, 2, 1104);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (5, 0, 9, 2, 240);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 9, 2, 241);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (6, 0, 10, 2, 242);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (7, 0, 9, 2, 340);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 9, 2, 341);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (8, 0, 10, 2, 342);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (11, 0, 2, 2, 1103);
INSERT INTO "JS_DATA_STEP_VAR" VALUES (12, 0, 2, 2, 1104);

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

-- ----------------------------
-- Table structure for JS_TABLE
-- ----------------------------
DROP TABLE IF EXISTS "JS_TABLE";
CREATE TABLE "JS_TABLE" (
  "num" integer,
  "name" wvarchar(50),
  PRIMARY KEY ("num")
);

-- ----------------------------
-- Records of JS_TABLE
-- ----------------------------

-- ----------------------------
-- Table structure for JS_TABLE_DATA
-- ----------------------------
DROP TABLE IF EXISTS "JS_TABLE_DATA";
CREATE TABLE "JS_TABLE_DATA" (
  "num" float,
  "key" float,
  "value" float,
  PRIMARY KEY ("num", "key"),
  FOREIGN KEY ("num") REFERENCES "JS_TABLE" ("num") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of JS_TABLE_DATA
-- ----------------------------

-- ----------------------------
-- Table structure for LINE
-- ----------------------------
DROP TABLE IF EXISTS "LINE";
CREATE TABLE "LINE" (
  "rtu_id" integer,
  "line_id" integer,
  "line_name" wvarchar(50),
  PRIMARY KEY ("rtu_id", "line_id"),
  FOREIGN KEY ("rtu_id") REFERENCES "RTU" ("rtu_id") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of LINE
-- ----------------------------
INSERT INTO "LINE" VALUES (1, 1, '1号母线');
INSERT INTO "LINE" VALUES (1, 2, '2号母线');
INSERT INTO "LINE" VALUES (2, 1, '1号机组');
INSERT INTO "LINE" VALUES (2, 2, '2号机组');

-- ----------------------------
-- Table structure for MODULE
-- ----------------------------
DROP TABLE IF EXISTS "MODULE";
CREATE TABLE "MODULE" (
  "module_num" smallint,
  "chunnel_num" smallint,
  "address" smallint,
  "name" wvarchar(50),
  "type" smallint,
  "relating_yx_num" smallint,
  PRIMARY KEY ("module_num"),
  FOREIGN KEY ("chunnel_num") REFERENCES "chunnel" ("chunnel_num") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("type") REFERENCES "MODULE_TYPE" ("type_num") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of MODULE
-- ----------------------------

-- ----------------------------
-- Table structure for MODULE_TYPE
-- ----------------------------
DROP TABLE IF EXISTS "MODULE_TYPE";
CREATE TABLE "MODULE_TYPE" (
  "type_num" smallint,
  "type_name" wvarchar(50),
  PRIMARY KEY ("type_num")
);

-- ----------------------------
-- Records of MODULE_TYPE
-- ----------------------------
INSERT INTO "MODULE_TYPE" VALUES (0, '通用MODBUS');
INSERT INTO "MODULE_TYPE" VALUES (1, '美兰尼尔Can');
INSERT INTO "MODULE_TYPE" VALUES (2, 'TWDIO');

-- ----------------------------
-- Table structure for MONITOR
-- ----------------------------
DROP TABLE IF EXISTS "MONITOR";
CREATE TABLE "MONITOR" (
  "MonitorNum" INTEGER,
  "MonitorName" CHAR(4),
  "PYcNum" INTEGER,
  "QYcNum" INTEGER,
  "QUpLimitYcNum" INTEGER,
  "QDownLimitYcNum" INTEGER,
  "QIsRunningYXNum" INTEGER,
  "QsingleMaxDelta" INTEGER,
  "QsingleMinDelta" INTEGER,
  "QMaxPlan" INTEGER,
  "QMinPlan" INTEGER,
  "QAdjustMode" INTEGER,
  "QAdjustType" INTEGER,
  "QAdjustWaitTime" INTEGER,
  "QGenAdjustMode" INTEGER,
  "QIsCalculateLimit" BOOLEAN,
  "QDistributeWay" INTEGER,
  "YsNum" INTEGER
);

-- ----------------------------
-- Records of MONITOR
-- ----------------------------
INSERT INTO "MONITOR" VALUES (0, '东气机群', 209, 202, 210, 211, 110, 7000, 50, 31000, -31000, 0, 0, 40, 1, 'False', 1, 2);

-- ----------------------------
-- Table structure for Output_ObjectType
-- ----------------------------
DROP TABLE IF EXISTS "Output_ObjectType";
CREATE TABLE "Output_ObjectType" (
  "ObjectType" integer,
  "ObjectTypeName" wvarchar(50),
  PRIMARY KEY ("ObjectType")
);

-- ----------------------------
-- Records of Output_ObjectType
-- ----------------------------
INSERT INTO "Output_ObjectType" VALUES (3, '遥信');
INSERT INTO "Output_ObjectType" VALUES (4, '遥测');

-- ----------------------------
-- Table structure for Output_Para
-- ----------------------------
DROP TABLE IF EXISTS "Output_Para";
CREATE TABLE "Output_Para" (
  "ObjectType" integer,
  "ObjectNum" integer,
  "OutputType" integer,
  "RelateNum" integer,
  "timeout" INTEGER NOT NULL,
  PRIMARY KEY ("ObjectType", "ObjectNum")
);

-- ----------------------------
-- Records of Output_Para
-- ----------------------------
INSERT INTO "Output_Para" VALUES (3, 1002, 0, 0, 3000);
INSERT INTO "Output_Para" VALUES (3, 1101, 0, 2, 3000);
INSERT INTO "Output_Para" VALUES (3, 1102, 0, 3, 3000);
INSERT INTO "Output_Para" VALUES (4, 1001, 0, 101, 3000);

-- ----------------------------
-- Table structure for Output_Type
-- ----------------------------
DROP TABLE IF EXISTS "Output_Type";
CREATE TABLE "Output_Type" (
  "ObjectType" integer,
  "OutputType" integer,
  "Name" wvarchar(50),
  PRIMARY KEY ("ObjectType", "OutputType")
);

-- ----------------------------
-- Records of Output_Type
-- ----------------------------
INSERT INTO "Output_Type" VALUES (3, 0, '遥信合');
INSERT INTO "Output_Type" VALUES (3, 1, '遥信分');
INSERT INTO "Output_Type" VALUES (4, 0, '遥测');

-- ----------------------------
-- Table structure for PARAMETER
-- ----------------------------
DROP TABLE IF EXISTS "PARAMETER";
CREATE TABLE "PARAMETER" (
  "parameter_type" integer NOT NULL,
  "parameter_type_value" INTEGER NOT NULL,
  "parameter_id" INTEGER NOT NULL,
  "parameter_value" double NOT NULL DEFAULT -1,
  PRIMARY KEY ("parameter_type", "parameter_type_value", "parameter_id"),
  FOREIGN KEY ("parameter_id") REFERENCES "PARAMETER_ID_NAME" ("parameter_id") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("parameter_type") REFERENCES "PARAMETER_TYPE" ("parameter_type_num") ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- ----------------------------
-- Records of PARAMETER
-- ----------------------------
INSERT INTO "PARAMETER" VALUES (0, 3, 0, 0.0);

-- ----------------------------
-- Table structure for PARAMETER_ID_NAME
-- ----------------------------
DROP TABLE IF EXISTS "PARAMETER_ID_NAME";
CREATE TABLE "PARAMETER_ID_NAME" (
  "parameter_id" INTEGER NOT NULL,
  "parameter_type" wvarchar(50) NOT NULL,
  PRIMARY KEY ("parameter_id")
);

-- ----------------------------
-- Records of PARAMETER_ID_NAME
-- ----------------------------
INSERT INTO "PARAMETER_ID_NAME" VALUES (0, '是否给系统校时，1表示校时');

-- ----------------------------
-- Table structure for PARAMETER_TYPE
-- ----------------------------
DROP TABLE IF EXISTS "PARAMETER_TYPE";
CREATE TABLE "PARAMETER_TYPE" (
  "parameter_type_num" INTEGER NOT NULL,
  "parameter_type_name" wvarchar(50) NOT NULL,
  PRIMARY KEY ("parameter_type_num")
);

-- ----------------------------
-- Records of PARAMETER_TYPE
-- ----------------------------
INSERT INTO "PARAMETER_TYPE" VALUES (0, 'chunnel');
INSERT INTO "PARAMETER_TYPE" VALUES (1, 'generator');

-- ----------------------------
-- Table structure for PLAN_CURVE
-- ----------------------------
DROP TABLE IF EXISTS "PLAN_CURVE";
CREATE TABLE "PLAN_CURVE" (
  "curveNum" integer,
  "num" integer,
  "start_time_hour" integer,
  "start_time_minute" integer,
  "system_v" float,
  "system_v_up" float,
  "system_v_down" float,
  "system_v_up_nextday" float,
  "system_v_down_nextday" float,
  "Coefficient" float,
  "Datatime" TIME,
  PRIMARY KEY ("curveNum", "num")
);

-- ----------------------------
-- Records of PLAN_CURVE
-- ----------------------------
INSERT INTO "PLAN_CURVE" VALUES (1, 18, 4, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 19, 4, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 20, 4, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 21, 5, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 22, 5, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 23, 5, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 24, 5, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 25, 6, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 26, 6, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 27, 6, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 28, 6, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 29, 7, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 30, 7, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 31, 7, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 32, 7, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 33, 8, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 34, 8, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 35, 8, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 36, 8, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 37, 9, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 38, 9, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 39, 9, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 40, 9, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 41, 10, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 42, 10, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 43, 10, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 44, 10, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 45, 11, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 46, 11, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 47, 11, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 48, 11, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 49, 12, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 50, 12, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 51, 12, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 52, 12, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 53, 13, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 54, 13, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 55, 13, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 56, 13, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 57, 14, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 58, 14, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 59, 14, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 60, 14, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 61, 15, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 62, 15, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 63, 15, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 64, 15, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 65, 16, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 66, 16, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 67, 16, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 68, 16, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 69, 17, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 70, 17, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 71, 17, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 72, 17, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 73, 18, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 74, 18, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 75, 18, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 76, 18, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 77, 19, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 78, 19, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 79, 19, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 80, 19, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 81, 20, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 82, 20, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 83, 20, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 84, 20, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 85, 21, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 86, 21, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 87, 21, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 88, 21, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 89, 22, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 90, 22, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 91, 22, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 92, 22, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 93, 23, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 94, 23, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 95, 23, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 96, 23, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 1, 0, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 2, 0, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 3, 0, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 4, 0, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 5, 1, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 6, 1, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 7, 1, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 8, 1, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 9, 2, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 10, 2, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 11, 2, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 12, 2, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 13, 3, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 14, 3, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 15, 3, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 16, 3, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 17, 4, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 18, 4, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 19, 4, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 20, 4, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 21, 5, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 22, 5, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 23, 5, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 24, 5, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 25, 6, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 26, 6, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 27, 6, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 28, 6, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 29, 7, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 30, 7, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 31, 7, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 32, 7, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 33, 8, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 34, 8, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 35, 8, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 36, 8, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 37, 9, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 38, 9, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 39, 9, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 40, 9, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 41, 10, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 42, 10, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 43, 10, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 44, 10, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 45, 11, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 46, 11, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 47, 11, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 48, 11, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 49, 12, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 50, 12, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 51, 12, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 52, 12, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 53, 13, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 54, 13, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 55, 13, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 56, 13, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 57, 14, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 58, 14, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 59, 14, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 60, 14, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 61, 15, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 62, 15, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 63, 15, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 64, 15, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 65, 16, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 66, 16, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 67, 16, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 68, 16, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 69, 17, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 70, 17, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 71, 17, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 72, 17, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 73, 18, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 74, 18, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 75, 18, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 76, 18, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 77, 19, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 78, 19, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 79, 19, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 80, 19, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 81, 20, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 82, 20, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 83, 20, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 84, 20, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 85, 21, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 86, 21, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 87, 21, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 88, 21, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 89, 22, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 90, 22, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 91, 22, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 92, 22, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 93, 23, 0, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 94, 23, 15, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 95, 23, 30, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (0, 96, 23, 45, 230.0, 233.0, 231.0, 233.0, 231.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 1, 0, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 2, 0, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 3, 0, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 4, 0, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 5, 1, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 6, 1, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 7, 1, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 8, 1, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 9, 2, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 10, 2, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 11, 2, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 12, 2, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 13, 3, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 14, 3, 15, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 15, 3, 30, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 16, 3, 45, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (1, 17, 4, 0, 510.0, 511.0, 509.0, 511.0, 509.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 1, 0, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 2, 0, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 3, 0, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 4, 0, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 5, 1, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 6, 1, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 7, 1, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 8, 1, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 9, 2, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 10, 2, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 11, 2, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 12, 2, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 13, 3, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 14, 3, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 15, 3, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 16, 3, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 17, 4, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 18, 4, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 19, 4, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 20, 4, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 21, 5, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 22, 5, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 23, 5, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 24, 5, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 25, 6, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 26, 6, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 27, 6, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 28, 6, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 29, 7, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 30, 7, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 31, 7, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 32, 7, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 33, 8, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 34, 8, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 35, 8, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 36, 8, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 37, 9, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 38, 9, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 39, 9, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 40, 9, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 41, 10, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 42, 10, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 43, 10, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 44, 10, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 45, 11, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 46, 11, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 47, 11, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 48, 11, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 49, 12, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 50, 12, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 51, 12, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 52, 12, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 53, 13, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 54, 13, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 55, 13, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 56, 13, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 57, 14, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 58, 14, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 59, 14, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 60, 14, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 61, 15, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 62, 15, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 63, 15, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 64, 15, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 65, 16, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 66, 16, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 67, 16, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 68, 16, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 69, 17, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 70, 17, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 71, 17, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 72, 17, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 73, 18, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 74, 18, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 75, 18, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 76, 18, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 77, 19, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 78, 19, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 79, 19, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 80, 19, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 81, 20, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 82, 20, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 83, 20, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 84, 20, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 85, 21, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 86, 21, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 87, 21, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 88, 21, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 89, 22, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 90, 22, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 91, 22, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 92, 22, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 93, 23, 0, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 94, 23, 15, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 95, 23, 30, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);
INSERT INTO "PLAN_CURVE" VALUES (2, 96, 23, 45, 230.0, 233.0, 232.0, 233.0, 232.0, 0.1, NULL);

-- ----------------------------
-- Table structure for PLAN_CURVE_31DAY
-- ----------------------------
DROP TABLE IF EXISTS "PLAN_CURVE_31DAY";
CREATE TABLE "PLAN_CURVE_31DAY" (
  "curveNum" integer,
  "dayNum" integer,
  "num" integer,
  "up_limit" float,
  "down_limit" float,
  "system_v" float,
  "start_time_hour" integer,
  "start_time_minute" integer,
  "Coefficient" float,
  PRIMARY KEY ("curveNum", "dayNum", "num")
);

-- ----------------------------
-- Records of PLAN_CURVE_31DAY
-- ----------------------------
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 35, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 36, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 37, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 38, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 39, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 40, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 41, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 42, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 43, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 44, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 45, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 46, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 47, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 48, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 49, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 50, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 51, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 52, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 53, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 54, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 55, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 56, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 57, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 58, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 59, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 60, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 61, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 62, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 63, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 64, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 65, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 66, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 67, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 68, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 69, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 70, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 71, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 72, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 73, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 74, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 75, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 76, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 77, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 78, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 79, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 80, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 81, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 82, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 83, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 84, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 85, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 86, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 87, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 88, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 89, 512.0, 508.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 90, 512.0, 508.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 91, 512.0, 508.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 92, 512.0, 508.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 93, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 94, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 95, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 1, 96, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 1, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 2, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 3, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 4, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 5, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 6, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 7, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 8, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 9, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 10, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 11, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 12, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 13, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 14, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 15, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 16, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 17, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 18, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 19, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 20, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 21, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 22, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 23, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 24, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 25, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 26, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 27, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 28, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 29, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 30, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 31, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 32, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 33, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 34, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 35, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 36, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 37, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 38, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 39, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 40, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 41, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 42, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 43, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 44, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 45, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 46, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 47, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 48, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 49, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 50, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 51, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 52, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 53, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 54, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 55, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 56, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 57, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 58, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 59, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 60, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 61, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 62, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 63, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 64, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 65, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 66, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 67, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 68, 513.0, 509.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 69, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 70, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 71, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 72, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 73, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 74, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 75, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 76, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 77, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (1, 2, 78, 514.0, 510.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 1, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 2, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 3, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 4, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 5, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 6, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 7, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 8, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 9, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 10, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 11, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 12, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 13, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 14, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 15, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 16, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 17, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 18, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 19, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 20, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 21, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 22, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 23, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 24, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 25, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 26, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 27, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 28, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 29, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 30, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 31, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 32, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 33, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 34, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 35, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 36, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 37, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 38, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 39, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 40, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 41, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 42, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 43, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 44, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 45, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 46, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 47, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 48, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 49, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 50, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 51, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 52, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 53, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 54, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 55, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 56, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 57, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 58, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 59, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 60, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 61, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 62, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 63, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 64, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 65, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 66, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 67, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 68, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 69, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 70, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 71, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 72, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 73, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 74, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 75, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 76, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 77, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 78, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 79, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 80, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 81, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 82, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 83, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 84, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 85, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 86, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 87, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 88, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 89, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 90, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 91, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 92, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 93, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 94, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 95, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 1, 96, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 1, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 2, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 3, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 4, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 5, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 6, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 7, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 8, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 9, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 10, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 11, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 12, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 13, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 14, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 15, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 16, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 17, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 18, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 19, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 20, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 21, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 22, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 23, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 24, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 25, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 26, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 27, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 28, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 29, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 30, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 31, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 32, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 33, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 34, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 35, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 36, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 37, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 38, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 39, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 40, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 41, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 42, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 43, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 44, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 45, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 46, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 47, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 48, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 49, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 50, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 51, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 52, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 53, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 54, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 55, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 56, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 57, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 58, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 59, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 60, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 61, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 62, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 63, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 64, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 65, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 66, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 67, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 68, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 69, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 70, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 71, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 72, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 73, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 74, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 75, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 76, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 77, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 78, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 79, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 80, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 81, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 82, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 83, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 84, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 85, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 86, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 87, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 88, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 89, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 90, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 91, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 92, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 93, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 94, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 95, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 2, 96, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 1, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 2, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 3, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 4, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 5, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 6, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 7, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 8, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 9, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 10, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 11, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 12, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 13, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 14, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 15, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 16, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 17, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 18, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 19, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 20, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 21, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 22, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 23, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 24, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 25, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 26, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 27, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 28, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 29, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 30, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 31, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 32, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 33, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 34, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 35, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 36, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 37, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 38, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 39, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 40, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 41, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 42, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 43, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 44, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 45, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 46, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 47, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 48, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 49, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 50, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 51, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 52, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 53, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 54, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 55, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 56, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 57, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 58, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 59, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 60, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 61, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 62, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 63, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 64, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 65, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 66, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 67, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 68, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 69, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 70, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 71, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 72, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 73, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 74, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 75, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 76, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 77, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 78, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 79, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 80, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 81, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 82, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 83, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 84, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 85, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 86, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 87, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 88, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 89, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 90, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 91, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 92, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 93, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 94, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 95, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 3, 96, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 1, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 2, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 3, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 4, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 5, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 6, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 7, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 8, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 9, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 10, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 11, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 12, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 13, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 14, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 15, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 16, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 17, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 18, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 19, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 20, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 21, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 22, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 23, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 24, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 25, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 26, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 27, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 28, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 29, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 30, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 31, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 32, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 33, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 34, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 35, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 36, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 37, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 38, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 39, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 40, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 41, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 42, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 43, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 44, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 45, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 46, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 47, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 48, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 49, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 50, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 51, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 52, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 53, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 54, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 55, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 56, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 57, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 58, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 59, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 60, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 61, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 62, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 63, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 64, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 65, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 66, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 67, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 68, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 69, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 70, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 71, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 72, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 73, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 74, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 75, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 76, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 77, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 78, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 79, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 80, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 81, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 82, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 83, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 84, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 85, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 86, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 87, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 88, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 89, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 90, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 91, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 92, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 93, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 94, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 95, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 4, 96, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 1, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 2, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 3, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 4, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 5, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 6, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 7, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 8, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 9, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 10, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 11, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 12, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 13, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 14, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 15, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 16, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 17, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 18, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 19, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 20, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 21, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 22, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 23, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 24, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 25, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 26, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 27, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 28, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 29, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 30, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 31, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 32, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 33, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 34, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 35, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 36, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 37, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 38, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 39, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 40, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 41, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 42, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 43, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 44, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 45, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 46, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 47, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 48, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 49, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 50, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 51, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 52, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 53, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 54, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 55, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 56, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 57, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 58, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 59, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 60, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 61, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 62, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 63, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 64, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 65, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 66, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 67, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 68, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 69, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 70, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 71, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 72, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 73, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 74, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 75, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 76, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 77, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 78, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 79, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 80, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 81, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 82, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 83, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 84, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 85, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 86, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 87, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 88, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 89, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 90, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 91, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 92, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 93, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 94, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 95, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 5, 96, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 1, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 2, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 3, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 4, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 5, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 6, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 7, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 8, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 9, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 10, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 11, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 12, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 13, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 14, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 15, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 16, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 17, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 18, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 19, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 20, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 21, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 22, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 23, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 24, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 25, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 26, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 27, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 28, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 29, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 30, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 31, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 32, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 33, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 34, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 35, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 36, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 37, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 38, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 39, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 40, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 41, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 42, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 43, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 44, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 45, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 46, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 47, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 48, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 49, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 50, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 51, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 52, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 53, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 54, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 55, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 56, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 57, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 58, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 59, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 60, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 61, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 62, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 63, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 64, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 65, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 66, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 67, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 68, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 69, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 70, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 71, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 72, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 73, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 74, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 75, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 76, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 77, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 78, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 79, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 80, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 81, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 82, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 83, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 84, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 85, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 86, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 87, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 88, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 89, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 90, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 91, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 92, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 93, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 94, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 95, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 6, 96, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 1, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 2, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 3, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 4, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 5, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 6, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 7, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 8, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 9, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 10, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 11, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 12, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 13, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 14, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 15, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 16, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 17, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 18, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 19, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 20, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 21, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 22, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 23, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 24, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 25, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 26, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 27, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 28, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 29, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 30, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 31, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 32, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 33, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 34, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 35, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 36, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 37, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 38, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 39, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 40, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 41, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 42, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 43, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 44, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 45, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 46, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 47, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 48, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 49, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 50, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 51, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 52, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 53, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 54, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 55, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 56, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 57, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 58, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 59, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 60, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 61, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 62, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 63, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 64, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 65, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 66, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 67, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 68, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 69, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 70, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 71, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 72, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 73, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 74, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 75, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 76, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 77, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 78, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 79, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 80, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 81, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 82, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 83, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 84, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 85, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 86, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 87, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 88, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 89, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 90, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 91, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 92, 232.0, 228.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 93, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 94, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 95, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 7, 96, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 1, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 2, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 3, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 4, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 5, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 6, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 7, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 8, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 9, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 10, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 11, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 12, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 13, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 14, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 15, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 16, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 17, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 18, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 19, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 20, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 21, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 22, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 23, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 24, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 25, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 26, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 27, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 28, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 29, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 30, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 31, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 32, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 33, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 34, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 35, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 36, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 37, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 38, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 39, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 40, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 41, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 42, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 43, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 44, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 45, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 46, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 47, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 48, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 49, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 50, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 51, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 52, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 53, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 54, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 55, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 56, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 57, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 58, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 59, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 60, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 61, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 62, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 63, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 64, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 65, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 66, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 67, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 68, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 69, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 70, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 71, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 72, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 73, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 74, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 75, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 76, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 77, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 78, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 79, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 80, 234.0, 230.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 81, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 82, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 83, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 84, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 85, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 86, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 87, 233.0, 229.0, 0.0, 0, 0, 0.0);
INSERT INTO "PLAN_CURVE_31DAY" VALUES (0, 8, 88, 233.0, 229.0, 0.0, 0, 0, 0.0);

PRAGMA foreign_keys = true;
