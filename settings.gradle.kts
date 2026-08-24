pluginManagement {
    repositories {
        // 1. 腾讯云 Maven 公共库（优先，替代 mavenCentral）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 2. Google 仓库（Android 插件及 androidx/com.android/com.google 依赖）
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 腾讯云 Maven 公共库（优先）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // Google 仓库（Android 依赖）
        google()
        mavenCentral()
        // JitPack：用于直接引用 GitHub 上的 JVM/Android 库
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ACPowerSwitch"
include(":app")