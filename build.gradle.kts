plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.8.0"
}

group = "ru.bradyden"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
}

dependencyLocking {
    lockAllConfigurations()
}

spotless {
    java {
        target("src/**/*.java")
        removeUnusedImports()
        googleJavaFormat().aosp()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("resolveAndLockAll") {
    notCompatibleWithConfigurationCache("Resolves every dependency configuration")
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks) {
            "Run this task with --write-locks"
        }
    }
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
    }
}
