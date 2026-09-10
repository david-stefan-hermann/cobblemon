enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()

        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
    }

    includeBuild("gradle/build-logic")
}

rootProject.name = "cobblemon"

// PT011 (port/26.1.x): :neoforge 타깃 제거. P01 strict는 Fabric only이고 archi-loom 1.14가 MC 26.1.x unobfuscated 미지원이라 architectury 자체를 드롭하기 위함.
// :common은 architectury 매크로 없이 모듈 분리 효용이 떨어지므로 향후 :fabric으로 흡수 가능. 단계적 변경을 위해 이 iter에서는 :common 유지.
listOf(
    "common",
    "fabric"
).forEach { setupProject(it, file(it)) }

fun setupProject(name: String, projectDirectory: File) = setupProject(name) {
    projectDir = projectDirectory
}

inline fun setupProject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}