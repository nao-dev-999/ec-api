# ビルドステージ
# ビルドコンテキストはリポジトリルート（core/backendとGradleマルチモジュールを共有するため）
FROM public.ecr.aws/amazoncorretto/amazoncorretto:25 AS build
WORKDIR /app
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY core ./core
COPY backend ./backend
COPY batch ./batch

RUN yum install -y findutils
RUN chmod +x gradlew
RUN ./gradlew :batch:bootJar --no-daemon -x spotlessCheck

# 実行ステージ
FROM public.ecr.aws/amazoncorretto/amazoncorretto:25 AS runtime
WORKDIR /app
COPY --from=build /app/batch/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
