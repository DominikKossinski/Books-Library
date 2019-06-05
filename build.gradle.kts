import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.1.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    kotlin("jvm") version "1.2.71"
    kotlin("plugin.spring") version "1.2.71"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-jdbc")
    implementation("com.google.code.gson:gson:2.7")
    implementation("commons-validator:commons-validator:1.4.1")
    implementation("mysql:mysql-connector-java:5.1.6")
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("javax.activation:activation:1.1")
    implementation("com.sun.mail:javax.mail:1.5.1")
    implementation("com.sun.mail:smtp:1.6.3")
    implementation("org.hibernate:hibernate-core:5.0.7.Final")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.6"
    }
}
