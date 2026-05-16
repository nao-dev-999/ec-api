plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.4.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// ─────────────────────────────────────────────
// Spotless: コードフォーマット設定
//   ./gradlew spotlessApply  → 自動整形
//   ./gradlew spotlessCheck  → フォーマット確認（CIで使用）
// ─────────────────────────────────────────────
spotless {
    java {
        // Google Java Format でコード整形
        googleJavaFormat("1.25.0")
        // 未使用 import を自動削除
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

repositories {
    mavenCentral()
}

dependencies {
    // ─── Web ───────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")

    // ─── Hibernate / JPA ───────────────────────
    // Spring Boot 4.0.1 では Hibernate 7.1 が同梱される
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Hibernate の型変換・スペシャルタイプをサポート
    implementation("org.hibernate.orm:hibernate-core")

    // ─── Validation ────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ─── PostgreSQL Driver ──────────────────────
    runtimeOnly("org.postgresql:postgresql")

    // flyway
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // ─── Lombok ────────────────────────────────
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ─── Test ──────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ビルド前に Spotless チェックを実行（CI向け）
tasks.named("check") { dependsOn("spotlessCheck") }
