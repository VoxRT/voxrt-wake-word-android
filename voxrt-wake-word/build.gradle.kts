// Generated per-release from the VoxRT monorepo. Do not edit by hand.

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

group = "com.github.VoxRT"
version = "0.1.0"

android {
    namespace = "com.voxrt.sdk.wakeword"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Pre-built .so files live under src/main/jniLibs/<abi>/.
    // They are produced from the proprietary VoxRT Rust runtime on the
    // VoxRT side and checked into this repo as the binary half of the
    // distribution. Keep them unmodified at packaging time.
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.add("**/*.so")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Kotlin stdlib comes via the Android Gradle plugin.
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.VoxRT"
                artifactId = "voxrt-wake-word"
                version = "0.1.0"
            }
        }
    }
}
