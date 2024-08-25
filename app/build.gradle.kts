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
    implementation ("pub.devrel:easypermissions:3.0.0")
    //noinspection UseTomlInstead
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0-alpha")
}