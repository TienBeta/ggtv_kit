plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python") version "16.1.0"
    id("kotlin-parcelize")
}

android {
    namespace = "co.vulcanlabs.ggtv_kit"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64")
        }
    }
    chaquopy {
        defaultConfig {
            version = "3.12"
            buildPython("/opt/homebrew/bin/python3")
            pip {
                install("nest_asyncio")
                install("androidtvremote2==0.2.3")
                install("protobuf>=3.20.0")
                install("zeroconf>=0.39.0")
                install("cryptography>=3.4.8")
                install("ifaddr>=0.1.7")
            }
            pyc {
                src = false
            }
        }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-process:2.9.2")
    implementation("androidx.annotation:annotation:1.9.1")

    // For logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Expose minimal dependencies
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.14.5")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")

}