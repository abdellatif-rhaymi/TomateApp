pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Si nécessaire pour d'autres dépendances
        // maven { url "https://jitpack.io" }
    }
}

rootProject.name = "TomatosApp"
include(":app")