plugins {
    id("java-library")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    // entity/repository が使う JPA・Spring Data のアノテーションのみ。
    // Web/Security/Redis 等、backend/batch 固有の関心事はここに持ち込まない。
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
