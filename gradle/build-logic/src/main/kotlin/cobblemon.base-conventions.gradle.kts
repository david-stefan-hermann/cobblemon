/*
 *
 *  * Copyright (C) 2023 Cobblemon Contributors
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import utilities.ACCESS_WIDENER

// PT011 (port/26.1.x): architectury-loom 1.14가 MC 26.1.x unobfuscated 미지원(F005 CONFIRMED iter#77).
// fabric-loom 1.15.5로 직접 전환 + architectury-plugin 드롭. P01 strict는 Fabric only이므로 multi-platform shim 불필요.
plugins {
    id("java")
    id("java-library")

    // PT005: org.cadixdev.licenser 0.6.1이 Gradle 9.1.0에서 SOE — apply 제거. 소스에 이미 MPL-2.0 헤더 존재.
    // id("org.cadixdev.licenser")
    // PT011: dev.architectury.loom → net.fabricmc.fabric-loom 1.15.5
    id("net.fabricmc.fabric-loom")
    // PT011: architectury-plugin 드롭
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version
description = rootProject.description

java {
    toolchain {
        // PT003: MC 26.1.x toolchain Java 25 요구(Loom 단정 메시지 + piston-meta javaVersion.majorVersion=25).
        languageVersion.set(JavaLanguageVersion.of((rootProject.property("java_version") as String).toInt()))
    }
}

repositories {
    mavenCentral()
    //JEI
    maven("https://maven.blamejared.com/")
    maven("https://maven.parchmentmc.org")
    maven("https://maven.fabricmc.net/")
}

// PT005: licenser plugin 비활성화에 따라 license { } 블록도 제거 (extension 미존재 시 컴파일 에러 방지)
// license {
//     header(rootProject.file("HEADER"))
// }

// PT011: architectury { minecraft = ...; compileOnly() } 블록 제거
// fabric-loom은 minecraft 의존성으로 dep 표기를 통해 자동 인식.

loom {
    // PT011: silentMojangMappingsLicense()는 archi-loom 확장 — fabric-loom 1.15.5에 미존재. 제거.
    // MC 26.1.x unobfuscated에서는 Mojang Mappings License 출력 자체가 발생하지 않으므로 silencer 불필요.
    accessWidenerPath.set(project(":common").file(ACCESS_WIDENER))
}

dependencies {
    // PT011: net.minecraft:minecraft → com.mojang:minecraft (fabric-example-mod 26.1.2 패턴 — fabric-loom 1.15.5의 표준 표기).
    minecraft("com.mojang:minecraft:${rootProject.property("mc_version")}")
    // PT007: MC 26.1.x는 unobfuscated이므로 piston-meta에 client_mappings 없음 → officialMojangMappings() 호출 금지.
    // PT011: fabric-loom 1.15.5는 mappings() 호출 자체를 생략해도 됨(fabric-example-mod 26.1.2 build.gradle 검증).
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        // PT011: options.release를 java_version property와 일치시킴(MC 26.1.2는 Java 25 target).
        options.release.set((rootProject.property("java_version") as String).toInt())
        options.compilerArgs.add("-Xlint:-processing,-classfile,-serial")
    }

    withType<KotlinCompile> {
        // PT011: Kotlin 2.3.21은 JVM_25 미지원(B003/B004에서 "falling back to JVM_24" 경고). JVM_24를 명시.
        compilerOptions.jvmTarget.set(JvmTarget.JVM_24)
    }

    withType<Jar> {
        from(rootProject.file("LICENSE"))
    }
}
