# 1. 빌드 스테이지: Gradle을 사용하여 JAR 파일 생성
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# 빌드 효율을 위해 소스 복사 전 Gradle 래퍼부터 복사 (캐시 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 종속성 미리 다운로드
RUN ./gradlew dependencies --no-daemon

# 전체 소스 복사 및 빌드 (테스트는 제외하여 빌드 속도 향상)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 2. 실행 스테이지: 실행에 필요한 JRE만 포함하여 이미지 경량화
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 가져오기
COPY --from=build /app/build/libs/*.jar app.jar

# JVM 최적화 옵션 (t3.small 메모리 환경 고려)
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

EXPOSE 8080

# 앱 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]