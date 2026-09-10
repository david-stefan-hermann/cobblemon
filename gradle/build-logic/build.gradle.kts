plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.architectury.dev/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.terraformersmc.com/")
}

dependencies {
    implementation(libs.kotlin)

    // PT005: licenser는 plugin apply에서 비활성화되었지만 build-logic deps에는 잔존(영향 없음).
    implementation(libs.licenser)
    implementation(libs.shadow)
    // PT011: architectury-loom → fabric-loom 1.15.5 직접 사용. architectury-plugin 드롭.
    implementation(libs.fabric.loom)

    implementation(libs.blossom)
    implementation(libs.ideaExt)
    implementation(libs.versioning)
}