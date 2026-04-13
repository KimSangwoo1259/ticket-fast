# 1. 빌드 스테이지
FROM eclipse-temurin:21-jdk-alpine AS build
# FROM 문 뒤에 다시 한번 선언해야 아래에서 쓸 수 있습니다
ARG MODULE_NAME
WORKDIR /app

# 기본 설정 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# [핵심 변경] 특정 모듈만 복사하는 대신, 전체 구조를 복사합니다.
# 그래들이 모든 모듈 폴더를 인식할 수 있게 해줍니다.
COPY . .

# 권한 부여 및 빌드
RUN chmod +x gradlew
# 어떤 모듈을 빌드하는지 로그에 찍어주면 나중에 보기 편합니다
RUN echo "Building module: ${MODULE_NAME}"
RUN ./gradlew :${MODULE_NAME}:bootJar -x test --no-daemon

# 2. 실행 스테이지 (이하 동일)
FROM eclipse-temurin:21-jre-alpine
ARG MODULE_NAME
WORKDIR /app

COPY --from=build /app/${MODULE_NAME}/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]