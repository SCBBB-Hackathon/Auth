# -------------------------------------------------------------------
# Multi-stage Dockerfile for the sessac-auth Spring Boot app
# - Stage 1: Build with Gradle (using official Gradle + Temurin JDK 17)
# - Stage 2: Run with lightweight Temurin JRE 17
# -------------------------------------------------------------------
# 빌드 도구와 JDK가 포함된 이미지로 애플리케이션을 빌드한다.
FROM gradle:8.7-jdk17 AS builder

# 애플리케이션 소스 전체를 컨테이너로 복사한다.
WORKDIR /app
COPY . .

# 테스트 + 패키징 (필요에 따라 -x test 로 변경 가능)
# 결과물: build/libs/*.jar
RUN gradle clean bootJar -x test

# -------------------------------------------------------------------
# 런타임 이미지는 JRE만 포함된 경량 이미지 사용
# -------------------------------------------------------------------
# alpine 변종이 플랫폼 매칭 문제를 낼 수 있어 일반 JRE 이미지를 사용합니다.
# (필요하면 --platform=linux/amd64 등으로 빌드 시점에 지정하세요)
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드 스테이지에서 만들어진 fat-jar를 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 헬스체크/기본 포트 (Spring Boot 기본 8080)
EXPOSE 8080

# 컨테이너 기동 시 실행할 커맨드
ENTRYPOINT ["java","-jar","/app/app.jar"]
