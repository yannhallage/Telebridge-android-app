plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Ajoute ceci pour Firebase (class path)
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2") // ta version AGP
        classpath("com.google.gms:google-services:4.4.0")  // ✅ plugin Google
    }
}

