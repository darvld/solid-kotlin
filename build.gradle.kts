plugins {
    kotlin("multiplatform") version "1.4.30"
    id("maven-publish")
}

group = "com.github.darvld"
version = "0.0.1"

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    explicitApi()

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        mavenPublication {
            artifactId = "solid-core"
        }
    }

    sourceSets {
        val nativeMain by getting {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        val nativeTest by getting
    }
}
