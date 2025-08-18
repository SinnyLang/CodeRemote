import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("de.undercouch.download") version "5.5.0"
}


android {
    namespace = "xyz.sl.coderemote"
    compileSdk = 35

    defaultConfig {
        applicationId = "xyz.sl.coderemote"
        minSdk = 29
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.documentfile)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
}

val repoUrl = "https://dl.google.com/dl/android/maven2"
val outputDir = buildDir.resolve("androidx-sources")

// 任务提前注册（配置阶段）
val downloadAllAndroidxSources = tasks.register("downloadAllAndroidxSources") {
    group = "download"
    description = "Download all AndroidX sources"

    doLast {
        println("▶ Download tasks completed.")
        println("🗂 Sources saved in: $outputDir")
    }
}

// 动态扫描 androidx 依赖，创建子下载任务，并将其加入主任务依赖
afterEvaluate {
    val deps = configurations
        .filter { it.isCanBeResolved }
        .flatMap { config ->
            try {
                config.resolvedConfiguration.resolvedArtifacts.toList()
            } catch (_: Exception) {
                emptyList()
            }
        }
        .filter { it.moduleVersion.id.group.startsWith("androidx.") }
        .distinctBy { it.moduleVersion.id }

    deps.forEach { artifact ->
        val group = artifact.moduleVersion.id.group
        val name = artifact.name
        val version = artifact.moduleVersion.id.version
        val groupPath = group.replace('.', '/')
        val jarName = "$name-$version-sources.jar"
        val url = "$repoUrl/$groupPath/$name/$version/$jarName"
        val destFile = outputDir.resolve(jarName)

        val safeTaskName = "downloadAndroidxSource_${name}_${version}".replace(Regex("[^A-Za-z0-9_]"), "_")

        val downloadTask = tasks.register<Download>(safeTaskName) {
            src(url)
            dest(destFile)
            overwrite(false)
            onlyIfModified(true)
        }

        downloadAllAndroidxSources.configure {
            dependsOn(downloadTask)
        }
    }
}

val pullAppPrivateFiles = tasks.register<Exec>("pullAppPrivateFiles") {
    group = "debug"
    description = "Pull DataStore files from emulator to project dir"

    // 包名
    val packageName = android.namespace
    val targetDir = "${projectDir}/build"
    try {
        exec {
            commandLine(
                "adb", "shell", "run-as", packageName,
                "cp", "-r", ".", "/sdcard/${packageName}"
            )
        }
    } catch (e: Exception) {}

    try {
        exec {
            commandLine(
                "adb", "pull", "/sdcard/${packageName}", targetDir
            )
        }
    } catch (e: Exception) {}

}
