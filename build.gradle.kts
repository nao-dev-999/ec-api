plugins {
    id("org.springframework.boot") version "4.0.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.diffplug.spotless") version "8.4.0"
    id("org.sonarqube") version "7.3.1.8318"
    id("io.gatling.gradle") version "3.15.1.2" apply false
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

    // 依存関係解決の再現性を保証する(gradle.lockfileを固定)
    // 依存関係を追加・更新したら ./gradlew dependencies --write-locks で再生成しコミットすること
    dependencyLocking {
        lockAllConfigurations()
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

sonar {
    properties {
        property("sonar.projectKey", "nao-dev-999_ec-api")
        property("sonar.organization", "nao-dev-999")
        // backend/build.gradle.kts の jacocoTestReport 除外設定と揃える
        // (config/constant/例外の単純DTOはビジネスロジックを持たずテスト対象外という方針)
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/EcApiApplication.java",
                "**/config/**",
                "**/constant/**",
                "**/exception/ErrorResponse.java",
            ).joinToString(","),
        )
    }
}

// core モジュールは entity/repository のみで自身の test/jacoco タスクを持たず、
// backend のテスト実行を通じてのみカバレッジが得られる。backend/build.gradle.kts 側で
// jacocoTestReport に core のクラスを含めるよう設定した上で、ここで core プロジェクトの
// カバレッジレポート参照先を backend の集約レポートに向ける（指定しないと core 配下の
// 新規行は「カバレッジ情報なし＝未カバー」として Quality Gate に計上されてしまう）。
project(":core") {
    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                "$rootDir/backend/build/reports/jacoco/test/jacocoTestReport.xml",
            )
        }
    }
}
