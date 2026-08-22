# ---- build stage：编译 ruoyi-admin 可执行 jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只拷 pom.xml 利用 Docker 层缓存，依赖不变时不用每次重新下载
COPY pom.xml .
COPY ruoyi-common/pom.xml ruoyi-common/pom.xml
COPY ruoyi-framework/pom.xml ruoyi-framework/pom.xml
COPY ruoyi-system/pom.xml ruoyi-system/pom.xml
COPY ruoyi-quartz/pom.xml ruoyi-quartz/pom.xml
COPY ruoyi-generator/pom.xml ruoyi-generator/pom.xml
COPY ruoyi-admin/pom.xml ruoyi-admin/pom.xml
COPY ruoyi-vqms/pom.xml ruoyi-vqms/pom.xml
RUN mvn -q -B dependency:go-offline

COPY . .
RUN mvn -q -B clean package -DskipTests

# ---- runtime stage：只带 JRE，体积更小 ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S ruoyi && adduser -S ruoyi -G ruoyi \
    && mkdir -p /home/ruoyi/uploadPath /home/ruoyi/logs \
    && chown -R ruoyi:ruoyi /home/ruoyi

COPY --from=build /build/ruoyi-admin/target/ruoyi-admin.jar /app/app.jar

USER ruoyi
EXPOSE 7000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
