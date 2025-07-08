buildscript {
    repositories {
        google()
        mavenCentral()

    }
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
        classpath("com.google.gms:google-services:4.4.2")

    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}