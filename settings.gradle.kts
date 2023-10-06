rootProject.name = "copy-cat"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("vendor/CC-Tweaked/gradle/libs.versions.toml"))
        }
    }
}
