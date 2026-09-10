
import utilities.isSnapshot
import utilities.version
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
 *
 *  * Copyright (C) 2023 Cobblemon Contributors
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

plugins {
    // PT011: 적용 순서 변경 — fabric-loom을 base-conventions보다 먼저 적용해 configuration 등록을 보장.
    id("net.fabricmc.fabric-loom")
    id("net.nemerosa.versioning")

    id("cobblemon.base-conventions")
    // PT011: :common은 P01 strict에서 Maven publish 불필요. architectury 드롭 후 remapJar 미등록 → publish-conventions 제거.
    // id("cobblemon.publish-conventions")

    id("net.kyori.blossom")
    id("org.jetbrains.gradle.plugin.idea-ext")
}

// PT011 (port/26.1.x): architectury-plugin 드롭. P01 strict는 Fabric only.
// architectury { common("neoforge", "fabric") }

repositories {
    maven(url = "${rootProject.projectDir}/deps")
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.neoforged.net/releases")
    maven(url = "https://maven.gegy.dev")
    mavenLocal()
}

// PT012 (port/26.1.x): fabric-loom 1.15.5 `net.fabricmc.fabric-loom` plugin id는 LoomNoRemapGradlePlugin.
// → disableObfuscation=true로 mod* configurations(modImplementation/modApi/modCompileOnly) 미등록.
// fabric-example-mod 26.1.2 패턴대로 plain implementation/api/compileOnly 사용 (V36 검증).
// Ref: https://github.com/FabricMC/fabric-loom/blob/1.15/src/main/java/net/fabricmc/loom/LoomNoRemapGradlePlugin.java
dependencies {
    implementation(libs.bundles.kotlin)
    implementation(libs.fabric.loader)
    api(libs.molang)

    // Integrations
    compileOnlyApi(libs.jei.api)
    compileOnly(libs.bundles.common.integrations.compileOnly) {
        isTransitive = false
    }
    // port/26.2: the "api-mojmap" capability split no longer exists on 26.2 builds since MC ships
    // unobfuscated (no more intermediary/named distinction to disambiguate).
    compileOnly(libs.lambDynamicLights)

    // Showdown
    compileOnly(libs.graal.core)

    // Data Storage
    compileOnly(libs.bundles.mongo)

    // Unit Testing
    testImplementation(libs.bundles.unitTesting)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        setEvents(listOf("failed"))
        setExceptionFormat("full")
    }
}

sourceSets {
    main {
        blossom {
            kotlinSources {
                fun generateLicenseHeader() : String {
                    val builder = StringBuilder()
                    builder.append("/*\n")
                    rootProject.file("HEADER").forEachLine {
                        if(it.isEmpty()) {
                            builder.append(" *").append("\n")
                        } else {
                            builder.append(" * ").append(it).append("\n")
                        }
                    }

                    return builder.append(" */").append("\n").toString()
                }

                property("license", generateLicenseHeader())
                property("modid", "cobblemon")
                property("version", project.version())
                property("isSnapshot", if(rootProject.isSnapshot()) "true" else "false")
                property("gitCommit", versioning.info.commit)
                property("branch", versioning.info.branch)
                System.getProperty("buildNumber")?.let { property("buildNumber", it) }
                property("timestamp", OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss")) + " UTC")
            }
        }
    }
}
