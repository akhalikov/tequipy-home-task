plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.tequipy"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

sourceSets {
    create("testIntegration") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
    create("testPerformance") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

configurations["testIntegrationImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["testIntegrationRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())
configurations["testPerformanceImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["testPerformanceRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.3")
    runtimeOnly("org.postgresql:postgresql")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    "testIntegrationRuntimeOnly"("com.h2database:h2")
    "testIntegrationImplementation"("org.awaitility:awaitility:4.2.1")
    "testPerformanceRuntimeOnly"("com.h2database:h2")
    "testPerformanceImplementation"("org.awaitility:awaitility:4.2.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Test>("testIntegration") {
    description = "Runs integration tests against an in-memory database."
    group = "verification"
    testClassesDirs = sourceSets["testIntegration"].output.classesDirs
    classpath = sourceSets["testIntegration"].runtimeClasspath
    shouldRunAfter(tasks.test)
}

tasks.check { dependsOn("testIntegration") }

tasks.register<Test>("testPerformance") {
    description = "Runs performance tests."
    group = "verification"
    testClassesDirs = sourceSets["testPerformance"].output.classesDirs
    classpath = sourceSets["testPerformance"].runtimeClasspath
    shouldRunAfter("testIntegration")
    testLogging {
        showStandardStreams = true
    }
}
