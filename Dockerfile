# ビルドステージ
FROM public.ecr.aws/amazoncorretto/amazoncorretto:25 AS build
WORKDIR /app
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN yum install -y findutils
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon -x spotlessCheck

# 実行ステージ
FROM public.ecr.aws/amazoncorretto/amazoncorretto:25 AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]