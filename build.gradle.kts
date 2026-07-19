plugins {
    id("org.springframework.boot") version "4.0.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.diffplug.spotless") version "8.4.0"
}

allprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.diffplug.spotless")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            events("passed", "failed", "skipped")
        }
        // ルートスイート終了時にテスト実行件数・OK数・NG数のサマリーを表示
        addTestListener(
            object : TestListener {
                override fun beforeSuite(suite: TestDescriptor) {}

                override fun beforeTest(testDescriptor: TestDescriptor) {}

                override fun afterTest(
                    testDescriptor: TestDescriptor,
                    result: TestResult,
                ) {}

                override fun afterSuite(
                    suite: TestDescriptor,
                    result: TestResult,
                ) {
                    if (suite.parent == null) {
                        println(
                            "\nテスト結果: 実行=${result.testCount} OK=${result.successfulTestCount} NG=${result.failedTestCount} スキップ=${result.skippedTestCount}",
                        )
                    }
                }
            },
        )
    }

    // ─────────────────────────────────────────────
    // Spotless: コードフォーマット設定（各モジュール共通）
    //   ./gradlew spotlessApply  → 自動整形
    //   ./gradlew spotlessCheck  → フォーマット確認（CIで使用）
    // ─────────────────────────────────────────────
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            // Google Java Format でコード整形
            googleJavaFormat("1.28.0").aosp()
            // import を自動削除
            removeUnusedImports()
            // 末尾空白を削除
            trimTrailingWhitespace()
            // ファイル末尾に改行を追加
            endWithNewline()
        }
    }

    // ビルド前に Spotless チェックを実行（CI向け）
    tasks.named("check") { dependsOn("spotlessCheck") }
}

// ─────────────────────────────────────────────
// Spotless: ルート直下のビルドスクリプト（*.gradle.kts）の整形
// ─────────────────────────────────────────────
spotless {
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")
        ktlint("1.5.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
