import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

val signingPropertiesEnvironmentVariable = "ROPESKILL_SIGNING_PROPERTIES"
val signingPropertiesFile =
    providers.environmentVariable(signingPropertiesEnvironmentVariable).orNull?.let { path ->
        File(path).canonicalFile.also { file ->
            require(file.isFile) {
                "$signingPropertiesEnvironmentVariable must point to an existing properties file."
            }
            require(!file.toPath().startsWith(rootProject.projectDir.canonicalFile.toPath())) {
                "Production signing properties must be stored outside the repository."
            }
        }
    }
val releaseSigningProperties =
    signingPropertiesFile?.let { file ->
        Properties().apply {
            file.inputStream().use(::load)
        }
    }

fun requiredSigningProperty(name: String): String =
    requireNotNull(releaseSigningProperties?.getProperty(name)?.takeIf(String::isNotBlank)) {
        "Missing required production signing property: $name"
    }

android {
    namespace = "com.ropeskill.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ropeskill.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["appLabel"] = "RopeSkill"
        buildConfigField("boolean", "T757_LIVE_ENABLED", "false")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".diagnostic"
            versionNameSuffix = "-diagnostic"
            manifestPlaceholders["appLabel"] = "RopeSkill Diagnostic"
        }
        create("t758") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".diagnostic.t758"
            versionNameSuffix = "-t758"
            manifestPlaceholders["appLabel"] = "RopeSkill T758"
            matchingFallbacks += listOf("debug")
        }
        create("t759") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".diagnostic.t759"
            versionNameSuffix = "-t759"
            manifestPlaceholders["appLabel"] = "RopeSkill T759"
            matchingFallbacks += listOf("debug")
        }
        create("t757Live") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".diagnostic.t757live"
            versionNameSuffix = "-t757-live"
            manifestPlaceholders["appLabel"] = "RopeSkill T757 Live"
            buildConfigField("boolean", "T757_LIVE_ENABLED", "true")
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            if (releaseSigningProperties != null) {
                signingConfig =
                    signingConfigs.create("productionRelease") {
                        val configuredStoreFile = File(requiredSigningProperty("storeFile"))
                        val resolvedStoreFile =
                            if (configuredStoreFile.isAbsolute) {
                                configuredStoreFile.canonicalFile
                            } else {
                                signingPropertiesFile
                                    ?.parentFile
                                    ?.resolve(configuredStoreFile)
                                    ?.canonicalFile
                                    ?: error("Cannot resolve the production keystore path.")
                            }

                        require(resolvedStoreFile.isFile) {
                            "The configured production keystore does not exist."
                        }
                        require(
                            !resolvedStoreFile
                                .toPath()
                                .startsWith(rootProject.projectDir.canonicalFile.toPath()),
                        ) {
                            "The production keystore must be stored outside the repository."
                        }

                        storeFile = resolvedStoreFile
                        storePassword = requiredSigningProperty("storePassword")
                        keyAlias = requiredSigningProperty("keyAlias")
                        keyPassword = requiredSigningProperty("keyPassword")
                    }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

val validateProductionSigning =
    tasks.register("validateProductionSigning") {
        group = "verification"
        description = "Checks that external production signing credentials are configured safely."

        doLast {
            require(releaseSigningProperties != null) {
                "Set $signingPropertiesEnvironmentVariable to an external signing properties file."
            }
            println("Production signing configuration is complete and stored outside the repository.")
        }
    }

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    mustRunAfter(validateProductionSigning)
}

tasks.register("packageProductionRelease") {
    group = "build"
    description = "Tests and builds signed production APK and AAB artifacts."
    dependsOn(
        validateProductionSigning,
        "testDebugUnitTest",
        "lintRelease",
        "assembleRelease",
        "bundleRelease",
    )
}
