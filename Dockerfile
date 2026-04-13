# 1. 빌드 스테이지
FROM eclipse-temurin:21-jdk-alpine AS build
# 빌드할 때 모듈 이름을 주입받습니다 (예: ticket-gateway)
ARG MODULE_NAME
WORKDIR /app

# Gradle 래퍼 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# MSA 구조: 공통 모듈과 빌드 대상 모듈만 복사하여 효율화
COPY ticket-common ticket-common
COPY ${MODULE_NAME} ${MODULE_NAME}

# 권한 부여 및 특정 모듈만 빌드
RUN chmod +x gradlew
RUN ./gradlew :${MODULE_NAME}:bootJar -x test --no-daemon

# 2. 실행 스테이지
FROM eclipse-temurin:21-jre-alpine
ARG MODULE_NAME
WORKDIR /app

# 빌드 스테이지에서 생성된 특정 모듈의 JAR만 가져오기
COPY --from=build /app/${MODULE_NAME}/build/libs/*.jar app.jar

# t3.small 메모리 최적화 옵션
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]