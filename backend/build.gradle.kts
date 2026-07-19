plugins {
    id("org.springframework.boot")
    id("jacoco")
}

dependencies {
    implementation(project(":core"))

    // ─── spring-boot ───────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.hibernate.orm:hibernate-core")
    implementation("org.springframework.session:spring-session-data-redis")

    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    // ─── PostgreSQL Driver ──────────────────────
    runtimeOnly("org.postgresql:postgresql")

    // ─── Flyway ───
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // ─── Logstash (JSON 構造化ログ) ─────────────
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // ─── Lombok ────────────────────────────────
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ─── Test ──────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")

    // ─── Testcontainers ─────────────────────────
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    // ─── データ駆動テスト（testsupport.data）: Excel形式パーサーが使用 ─────
    testImplementation("org.apache.poi:poi:5.4.0")
    testImplementation("org.apache.poi:poi-ooxml:5.4.0")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
}

tasks.withType<Test> {
    finalizedBy(tasks.jacocoTestReport)
}

// ─────────────────────────────────────────────
// JaCoCo: カバレッジレポート設定
//   ./gradlew :backend:test :backend:jacocoTestReport
//   レポート出力先: build/reports/jacoco/test/html/index.html
// ─────────────────────────────────────────────
jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true // CI（SonarQube等）連携用
        html.required = true // ブラウザで確認用
        csv.required = false
    }
    // カバレッジ計測対象から除外するクラス
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/EcApiApplication.class", // エントリポイント
                        "**/config/**", // 設定クラス
                        "**/constant/**", // 定数
                        "**/exception/ErrorResponse.class", // 単純なデータクラス
                    )
                }
            },
        ),
    )
}
