# RuoYi-Vue-springboot3 工程实际技术栈

> 2026-08-20 整理：以仓库内 `pom.xml` / `application.yml` / `Dockerfile` 与前端 `package.json` 的**真实版本与配置**为准；与 CLAUDE.md「底座自 v3.1 起不变」一致，但本文落到具体版本号与模块出处，便于后续 D1~D9 编码时按图索骥。
>
> 与 `docs/项目规划_v5_0.md` §2/§11 的关系：v5.0 摘其要点，本文件是**逐字段可核对的版本清单**；版本/配置变更先动本文件、再回写 v5.0。

---

## 1. 后端（RuoYi-Vue-springboot3）

| 维度 | 选型 / 版本 | 出处 |
|---|---|---|
| **JDK** | Java 17（`<java.version>17</java.version>`，maven-compiler-plugin 17） | [pom.xml:18](../RuoYi-Vue-springboot3/pom.xml:18) |
| **构建** | Maven 3.9.x；多模块 reactor 6 个子模块 + 新增 `ruoyi-vqms`（共 7 个） | [pom.xml:176](../RuoYi-Vue-springboot3/pom.xml:176) |
| **运行时容器** | 多阶段：build = `maven:3.9-eclipse-temurin-17`；runtime = `eclipse-temurin:17-jre-alpine`（非 root 用户 `ruoyi` 跑 jar） | [Dockerfile:1-29](../RuoYi-Vue-springboot3/Dockerfile:1-29) |
| **Web 框架** | Spring Boot **3.5.14** + Spring Web (Servlet MVC) + Tomcat 嵌入 | [pom.xml:19](../RuoYi-Vue-springboot3/pom.xml:19)、[ruoyi-framework/pom.xml:22](../RuoYi-Vue-springboot3/ruoyi-framework/pom.xml:22) |
| **AOP** | `spring-boot-starter-aop`（用于多数据源切面 `@DataSource`） | [ruoyi-framework/pom.xml:26](../RuoYi-Vue-springboot3/ruoyi-framework/pom.xml:26) |
| **安全 / 鉴权** | Spring Security 6.x（由 Spring Boot 6.5.10 BOM 管）+ JWT（`io.jsonwebtoken:jjwt 0.9.1`，自定义 header `Authorization`、30 分钟默认） | [ruoyi-common/pom.xml:31](../RuoYi-Vue-springboot3/ruoyi-common/pom.xml:31)、[application.yml:91-98](../RuoYi-Vue-springboot3/ruoyi-admin/src/main/resources/application.yml:91-98) |
| **持久层** | **MyBatis 3.0.5**（mybatis-spring-boot-starter），Mapper XML + `@Mapper` 注解混用 | [pom.xml:20,68](../RuoYi-Vue-springboot3/pom.xml:20) |
| **多数据源** | 自写 `DynamicDataSource` + `DataSourceAspect`（基于 `@DataSource(MASTER/SLAVE)` 注解 + ThreadLocal）；连接池 = **Alibaba Druid 1.2.28** + `druid-spring-boot-3-starter`；Druid 内置 stat-view `/druid/*` | [DruidConfig.java:32](../RuoYi-Vue-springboot3/ruoyi-framework/src/main/java/com/ruoyi/framework/config/DruidConfig.java:32)、[pom.xml:21,48](../RuoYi-Vue-springboot3/pom.xml:21) |
| **分页** | PageHelper 2.1.1（`pagehelper-spring-boot-starter`，方言 `mysql`） | [pom.xml:24,64](../RuoYi-Vue-springboot3/pom.xml:24)、[application.yml:109-113](../RuoYi-Vue-springboot3/ruoyi-admin/src/main/resources/application.yml:109-113) |
| **校验** | `spring-boot-starter-validation`（Jakarta Bean Validation） | [ruoyi-common/pom.xml:43](../RuoYi-Vue-springboot3/ruoyi-common/pom.xml:43) |
| **缓存** | Redis 客户端 = Lettuce（`spring-boot-starter-data-redis`，池 0~8） | [application.yml:67-89](../RuoYi-Vue-springboot3/ruoyi-admin/src/main/resources/application.yml:67-89) |
| **JSON** | Jackson Databind（Spring MVC 默认）+ **Alibaba Fastjson2 2.0.62**（RuoYi 工具类混用） | [ruoyi-common/pom.xml:55,61](../RuoYi-Vue-springboot3/ruoyi-common/pom.xml:55) |
| **工具库** | Apache Commons Lang3 / IO / Pool2；Yauaa 8.1.1（解析 UA） | [ruoyi-common/pom.xml:49,67,97](../RuoYi-Vue-springboot3/ruoyi-common/pom.xml:49) |
| **Excel 导出** | Apache POI 4.1.2 + POI-OOXML（`@Excel` 注解 + RuoYi 工具封装） | [pom.xml:28,102](../RuoYi-Vue-springboot3/pom.xml:28) |
| **验证码** | Kaptcha 2.3.1（数学/字符，可关；vqms.sql 第五节默认关） | [pom.xml:22,130](../RuoYi-Vue-springboot3/pom.xml:22) |
| **定时任务** | Quartz Scheduler（持久化到 MySQL 的 QRTZ_* 表，`sql/quartz.sql` 初始化）+ RuoYi `ruoyi-quartz` 自带 UI | [ruoyi-quartz/pom.xml:18](../RuoYi-Vue-springboot3/ruoyi-quartz/pom.xml:18) |
| **代码生成** | Velocity 模板引擎 + Druid 解析表元数据 → 一键生成 Controller/Service/Mapper/Vue | [ruoyi-generator/pom.xml:18](../RuoYi-Vue-springboot3/ruoyi-generator/pom.xml:18) |
| **API 文档** | springdoc-openapi 2.8.17 + Swagger UI（`/swagger-ui.html`、`/v3/api-docs`） | [pom.xml:32,86](../RuoYi-Vue-springboot3/pom.xml:32) |
| **XSS / 防爬** | 自写 XSS Filter（`excludes=/system/notice`），Referer 白名单 | [application.yml:128-143](../RuoYi-Vue-springboot3/ruoyi-admin/src/main/resources/application.yml:128-143) |
| **跨域** | `ruoyi.framework.config.CorsFilter`（RuoYi 自带，前端 `vite proxy` + Nginx 反代双层） | 行为约定 |
| **后端字符 / 时区** | URI `UTF-8`；MySQL 连接 `serverTimezone=GMT%2B8`（北京时间）；JVM 需配 `-Duser.timezone=Asia/Shanghai`（v5.0 §13） | [application.yml:24](../RuoYi-Vue-springboot3/ruoyi-admin/src/main/resources/application.yml:24) |

## 2. VQMS 自建模块（ruoyi-vqms，2026-08-20 新增，§12.1 D1 落地范围）

| 维度 | 选型 / 当前状态 | 出处 |
|---|---|---|
| **包名** | `com.ruoyi.vqms.source`（D1 子包；`statistics/`、`ingestion/` 属搁置轨待填） | 设计：D1 [HisCurveSvReader.java](../RuoYi-Vue-springboot3/ruoyi-vqms/src/main/java/com/ruoyi/vqms/source/reader/HisCurveSvReader.java) |
| **依赖** | `ruoyi-common`（拿 `@DataSource`、`DataSourceType`）+ `ruoyi-framework`（拿 `JdbcTemplate`）+ `mybatis-spring-boot-starter`（为后续 D7/D8 Mapper 预留） | [ruoyi-vqms/pom.xml](../RuoYi-Vue-springboot3/ruoyi-vqms/pom.xml) |
| **D1 实现** | `Mysql57CurveReader implements HisCurveSvReader`：读 `his_curve_sv` / `yc_history` / `warn_info`；坏行跳过+日志；不映射 `average_SV`/`plan_SV`；reader 对外返回原始时间戳 | [Mysql57CurveReader.java](../RuoYi-Vue-springboot3/ruoyi-vqms/src/main/java/com/ruoyi/vqms/source/reader/Mysql57CurveReader.java) |
| **未挂载点** | ① `@MapperScan("com.ruoyi.vqms")` 启动类未加；② `spring.datasource.druid.slave` 未配；③ slave 开关未开 | 跟踪到 D1 完成标准再补 |

## 3. 前端（RuoYi-Vue3）

| 维度 | 选型 / 版本 | 出处 |
|---|---|---|
| **框架** | **Vue 3.5.26** + Vite 6.4.1（`<script setup>` 语法）+ Vue Router 4.6.4 + **Pinia 3.0.4**（状态） | [package.json:32-34](../RuoYi-Vue3/package.json:32) |
| **UI 库** | Element Plus 2.13.1 + `@element-plus/icons-vue 2.3.2` | [package.json:18,24](../RuoYi-Vue3/package.json:18) |
| **图表** | ECharts 5.6.0（电压曲线 / 合格率趋势 / 分档柱状图） | [package.json:23](../RuoYi-Vue3/package.json:23) |
| **工具库** | Axios 1.13.2（封装 `request.ts`，拦截 JWT / 通用错误）、js-cookie、jsencrypt（登录密码 RSA）、clipboard、nprogress（顶部进度条）、fuse.js（菜单搜索）、file-saver（导出）、vue-cropper（头像裁剪） | [package.json:21-35](../RuoYi-Vue3/package.json:21) |
| **样式** | SCSS（`sass-embedded 1.97.2`） + 自写 `vqms.scss` 主题（品牌已去） | [package.json:39](../RuoYi-Vue3/package.json:39) |
| **构建优化** | `unplugin-auto-import 0.18.6`（自动按需引入）、`vite-plugin-compression 0.5.1`（gzip）、`vite-plugin-svg-icons 2.0.1`（SVG 雪碧图）、`unplugin-vue-setup-extend-plus`（`defineOptions`） | [package.json:40-44](../RuoYi-Vue3/package.json:40) |
| **富文本** | Quill 2.0.2（公告编辑；via `overrides` + `resolutions` 锁版） | [package.json:47](../RuoYi-Vue3/package.json:47) |

## 4. 部署（4 个容器）

详见 [项目规划_v5_0.md](项目规划_v5_0.md) §11；技术栈层面 Docker Compose + Nginx + 上述 RuoYi 镜像（7000:7000 → 8080:80）。
