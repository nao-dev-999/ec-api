plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.4.0"
    jacoco
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // ─── spring-boot ───────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
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

    // ─── Lombok ────────────────────────────────
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ─── Test ──────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}

// ─────────────────────────────────────────────
// JaCoCo: カバレッジレポート設定
//   ./gradlew test jacocoTestReport
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
                        "**/entity/**", // JPAエンティティ
                        "**/constant/**", // 定数
                        "**/exception/ErrorResponse.class", // 単純なデータクラス
                    )
                }
            },
        ),
    )
}

// ─────────────────────────────────────────────
// Spotless: コードフォーマット設定
//   ./gradlew spotlessApply  → 自動整形
//   ./gradlew spotlessCheck  → フォーマット確認（CIで使用）
// ─────────────────────────────────────────────
spotless {
    java {
        // Google Java Format でコード整形
        googleJavaFormat("1.25.0").aosp()
        // import を自動削除
        removeUnusedImports()
        // 末尾空白を削除
        trimTrailingWhitespace()
        // ファイル末尾に改行を追加
        endWithNewline()
        // ライセンスヘッダー（任意）
        // licenseHeader("/* (C) 2025 Example Corp */")
    }
    // Kotlin DSL ビルドファイルも整形対象
    kotlinGradle {
        ktlint("1.5.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ビルド前に Spotless チェックを実行（CI向け）
tasks.named("check") { dependsOn("spotlessCheck") }
