/*
 *
 *  * Copyright (C) 2023 Cobblemon Contributors
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

configurations.all {
    resolutionStrategy {
        force(libs.fabric.loader)
    }
}

plugins {
    id("cobblemon.platform-conventions")
    id("cobblemon.publish-conventions")
}

// PT011 (port/26.1.x): architectury-plugin 드롭. fabric-loom 1.15.5 직접 사용.
// architectury {
//     platformSetupLoomIde()
//     fabric()
// }

val generatedResources = file("src/generated/resources")

sourceSets.main {
    resources {
        srcDir(generatedResources)
    }
}

repositories {
    maven(url = "${rootProject.projectDir}/deps")
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.terraformersmc.com/")
}

dependencies {
    // PT017 (port/26.1.x): fabric-loom 1.15.5 no-remap mode → `namedElements` configuration 미등록.
    // 표준 Gradle Java variant (runtimeElements/apiElements) 사용 — :common project 디폴트 참조.
    implementation(project(":common")) {
        isTransitive = false
    }
    bundle(project(":common")) {
        isTransitive = false
    }
    // PT012 (port/26.1.x): fabric-loom 1.15.5 no-remap mode → mod*/modLocalRuntime configurations 미등록.
    // plain implementation/api/compileOnly/runtimeOnly 사용 (V36 fabric-example-mod 26.1.2 패턴).
    // port/26.2: debug utils, JEI and c2me sit in the dev runtime only and exist as 1.21.1 builds,
    // so fabric-loader refuses to start runClient/runServer with them on the classpath. Restore them
    // once 26.2 builds exist; the JEI integration still compiles against jei-api (compileOnly).
    // runtimeOnly(libs.fabric.debugutils)
    implementation(libs.fabric.loader)
    api(libs.fabric.api)
    api(libs.bundles.fabric)

    compileOnly(libs.bundles.common.integrations.compileOnly) {
        isTransitive = false
    }

    implementation(libs.bundles.fabric.integrations.implementation)
    // runtimeOnly(libs.bundles.fabric.integrations.runtimeOnly)
    runtimeOnly(libs.bundles.mongo)

//    implementation(libs.flywheelFabric)
//    include(libs.flywheelFabric)

    listOf(
        libs.fabric.kotlin,
        libs.bundles.graal,
        libs.bundles.mongo
    ).forEach {
        include(it)
    }

    // port/26.2: the production jar is built by shadowJar from the `bundle` configuration, and this
    // loom setup registers no remapJar, so `include` (jar-in-jar) produces nothing - GraalVM was
    // simply missing from the shipped jar and showdown died on NoClassDefFoundError: HostAccess.
    // Bundling it puts the classes in the jar the way molang is already handled.
    bundle(libs.bundles.graal)

    // Added to make graal available in dev
    runtimeOnly(libs.bundles.graal)

    include(libs.fabric.kotlin)

    listOf(
        libs.molang
    ).forEach {
        bundle(it)
        runtimeOnly(it)
    }

    minecraftServerLibraries(libs.icu4j)

}

tasks {
    // The AW file is needed in :fabric project resources when the game is run.
    val copyAccessWidener by registering(Copy::class) {
        from(loom.accessWidenerPath)
        into(generatedResources)
        // PT005/PT011: licenser plugin 비활성화로 `checkLicenseMain` task 미존재 → dependsOn 제거.
        // dependsOn(checkLicenseMain)
    }

    processResources {
        dependsOn(copyAccessWidener)
        inputs.property("version", rootProject.version)
        inputs.property("fabric_loader_version", libs.fabric.loader.get().version)
        inputs.property("fabric_api_version", libs.fabric.api.get().version)
        inputs.property("minecraft_version", rootProject.property("mc_version").toString())
        inputs.property("java_version", rootProject.property("java_version").toString())

        filesMatching("fabric.mod.json") {
            // PT016: libs.*.get().version → String? — Kotlin DSL strict 타입 매칭으로 `!!` 강제.
            expand(
                "version" to rootProject.version,
                "fabric_loader_version" to (libs.fabric.loader.get().version ?: ""),
                "fabric_api_version" to (libs.fabric.api.get().version ?: ""),
                "minecraft_version" to rootProject.property("mc_version").toString(),
                "java_version" to rootProject.property("java_version").toString()
            )
        }
    }

    sourcesJar {
        dependsOn(copyAccessWidener)
    }
}
