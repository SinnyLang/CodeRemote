// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
//    id("de.undercouch.download") version "5.5.0"
}


//val outputDir = buildDir.resolve("androidx-sources")
//val repoUrl = "https://dl.google.com/dl/android/maven2"
//
//val downloadTasks = mutableListOf<TaskProvider<Download>>()
//val dependenciesToDownload = listOf(
////    "androidx.core:core:1.13.1",
////    "androidx.appcompat:appcompat:1.6.1",
////    "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2",
//    "androidx.activity:activity:1.10.1"
//)


//dependenciesToDownload.forEach { dep ->
//    val (groupId, artifactId, version) = dep.split(":")
//    val groupPath = groupId.replace('.', '/')
//    val jarName = "$artifactId-$version-sources.jar"
//    val url = "$repoUrl/$groupPath/$artifactId/$version/$jarName"
//    val destinationFile = outputDir.resolve(jarName)
//
//    tasks.register("downloadSource_$artifactId", Download::class.java) {
//        src(url)
//        dest(destinationFile)
//        onlyIfModified(true)  // ✅ 使用 Groovy 风格的 setter 方法调用，兼容一切
//        overwrite(false)
//    }
//}
//
//tasks.register("downloadAllSources") {
//    group = "androidx"
//    description = "Download all androidx sources JARs"
//    dependsOn(tasks.matching { it.name.startsWith("downloadSource_") })
//}