pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("de.undercouch.download") version "5.5.0"
        id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CodeRemote"
include(":app")
include(":EditCore")
include(":piecetable")

project(":piecetable").projectDir =
    file("third_party/piecetable/lib")
