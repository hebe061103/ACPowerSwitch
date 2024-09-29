plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.zt.acpowerswitch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zt.acpowerswitch"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }
    buildToolsVersion = "34.0.0"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //noinspection UseTomlInstead
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0-alpha")
    //noinspection UseTomlInstead
    implementation ("androidx.appcompat:appcompat:1.7.0")                 //必须 1.0.0 以上
    //noinspection UseTomlInstead
    implementation  ("io.github.scwang90:refresh-layout-kernel:2.1.0")    //核心必须依赖
    //noinspection UseTomlInstead
    implementation  ("io.github.scwang90:refresh-header-radar:2.1.0")   //雷达刷新头
    //noinspection UseTomlInstead
    implementation  ("io.github.scwang90:refresh-footer-ball:2.1.0")    //球脉冲加载
    //noinspection UseTomlInstead
    implementation  ("io.github.scwang90:refresh-header-material:2.1.0") //谷歌刷新头
}