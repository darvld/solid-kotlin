plugins {
    kotlin("multiplatform") version "1.4.30"
}

group = "com.github.darvld"
version = "1.0.0"

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

    linuxX64("native").apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val nativeMain by getting {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        val nativeTest by getting
    }
}
