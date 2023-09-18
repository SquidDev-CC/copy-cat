rootProject.name = "copy-cat"

includeBuild("vendor/teavm") {
     dependencySubstitution {
        substitute(module("org.teavm:teavm-jso")).using(project(":jso:core"))
        substitute(module("org.teavm:teavm-jso-apis")).using(project(":jso:apis"))
        substitute(module("org.teavm:teavm-platform")).using(project(":platform"))
        substitute(module("org.teavm:teavm-classlib")).using(project(":classlib"))
        substitute(module("org.teavm:teavm-metaprogramming-api")).using(project(":metaprogramming:api"))
        substitute(module("org.teavm:teavm-cli")).using(project(":tools:cli"))
    }
}
