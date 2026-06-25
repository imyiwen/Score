# 基础镜像（Java17，适配SpringBoot 3.x）
FROM eclipse-temurin:17-jre

# 容器内创建工作目录
WORKDIR /app

# 把本地jar包复制到容器里
COPY score-0.0.1-SNAPSHOT.jar /app/app.jar

# 启动命令（固定）
ENTRYPOINT ["java", "-jar", "app.jar"]