rootProject.name = "Tilo.SpatialIndex"

pluginManagement {
    plugins {
        kotlin("multiplatform") version "2.3.0"
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
