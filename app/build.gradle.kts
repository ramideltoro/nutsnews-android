plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val releaseSigningVariableNames = listOf(
    "NUTSNEWS_UPLOAD_KEYSTORE_PATH",
    "NUTSNEWS_UPLOAD_KEYSTORE_PASSWORD",
    "NUTSNEWS_UPLOAD_KEY_ALIAS",
    "NUTSNEWS_UPLOAD_KEY_PASSWORD",
)
val releaseTaskRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}
val releaseSigningValues = releaseSigningVariableNames.associateWith { variableName ->
    providers.environmentVariable(variableName).orNull?.takeIf(String::isNotBlank)
}
val configuredVersionName = providers.gradleProperty("nutsnewsVersionName")
    .orElse("1.1.1")
    .map { value ->
        check(Regex("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$").matches(value)) {
            "nutsnewsVersionName must be a stable semantic version."
        }
        value
    }
val configuredVersionCode = providers.gradleProperty("nutsnewsVersionCode")
    .orElse("2")
    .map { value ->
        val parsedValue = value.toIntOrNull()
        check(parsedValue != null && parsedValue in 1..2_100_000_000) {
            "nutsnewsVersionCode must be an Android version code from 1 through 2100000000."
        }
        parsedValue
    }

if (releaseTaskRequested) {
    val missingVariables = releaseSigningValues.filterValues { it == null }.keys
    check(missingVariables.isEmpty()) {
        "Release signing requires environment variables: ${missingVariables.joinToString()}"
    }

    val keystorePath = checkNotNull(releaseSigningValues["NUTSNEWS_UPLOAD_KEYSTORE_PATH"])
    check(file(keystorePath).isFile) {
        "Release signing keystore does not exist at the configured path."
    }
}

tasks.withType<Test>().configureEach {
    systemProperty(
        "nutsnews.recordGoldens",
        providers.gradleProperty("nutsnews.recordGoldens").orElse("false").get(),
    )
}

val writeNutsNewsBuildIdentity = tasks.register("writeNutsNewsBuildIdentity") {
    val identityFile = layout.buildDirectory.file("outputs/release/release-identity.json")
    outputs.file(identityFile)
    doLast {
        identityFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                """{"versionName":"${configuredVersionName.get()}","versionCode":${configuredVersionCode.get()}}""" +
                    "\n",
            )
        }
    }
}

android {
    namespace = "com.nutsnews.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nutsnews.app"
        minSdk = 26
        targetSdk = 36
        versionCode = configuredVersionCode.get()
        versionName = configuredVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseTaskRequested) {
                storeFile = file(
                    checkNotNull(releaseSigningValues["NUTSNEWS_UPLOAD_KEYSTORE_PATH"]),
                )
                storePassword = checkNotNull(
                    releaseSigningValues["NUTSNEWS_UPLOAD_KEYSTORE_PASSWORD"],
                )
                keyAlias = checkNotNull(releaseSigningValues["NUTSNEWS_UPLOAD_KEY_ALIAS"])
                keyPassword = checkNotNull(
                    releaseSigningValues["NUTSNEWS_UPLOAD_KEY_PASSWORD"],
                )
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }

        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
}

tasks.configureEach {
    if (name == "bundleRelease") {
        finalizedBy(writeNutsNewsBuildIdentity)
    }
    if (name.contains("release", ignoreCase = true)) {
        notCompatibleWithConfigurationCache(
            "Release signing credentials must never be retained in configuration-cache state.",
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)

    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.window.core)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.coroutines)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    ksp(libs.androidx.room.compiler)

    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.glance.appwidget.testing)
    testImplementation(libs.androidx.glance.testing)
    testImplementation(composeBom)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver.junit4)
    testImplementation(libs.robolectric)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.kotlin.test)
}
