import java.util.*

plugins {
    java
    distribution
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven("https://squiddev.cc/maven")
        }
        filter {
            includeGroup("org.squiddev")
        }
    }
}

val filteredCCSources by tasks.registering(Sync::class) {
    from("original/CC-Tweaked/projects/core/src/main/java") {
        exclude(
            // Exclude all the Netty-specific code
            "dan200/computercraft/core/apis/http/CheckUrl.java",
            "dan200/computercraft/core/apis/http/NetworkUtils.java",
            "dan200/computercraft/core/apis/http/request/HttpRequest.java",
            "dan200/computercraft/core/apis/http/request/HttpRequestHandler.java",
            "dan200/computercraft/core/apis/http/websocket/NoOriginWebSocketHandshaker.java",
            "dan200/computercraft/core/apis/http/websocket/Websocket.java",
            "dan200/computercraft/core/apis/http/websocket/WebsocketCompressionHandler.java",
            "dan200/computercraft/core/apis/http/websocket/WebsocketHandler.java",
        )
    }

    into(layout.buildDirectory.dir("filtedCCSources"))
}

sourceSets {
    main {
        java.srcDir("original/CC-Tweaked/projects/core-api/src/main/java")
        java.srcDir(filteredCCSources)
        resources.srcDir("original/CC-Tweaked/projects/core/src/main/resources")
    }
    register("builder")
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.checkerframework:checker-qual:3.17.0")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("com.google.errorprone:error_prone_annotations:2.14.0")

    implementation("com.google.guava:guava:22.0")
    implementation("org.apache.commons:commons-lang3:3.6")
    implementation("it.unimi.dsi:fastutil:8.2.2")
    implementation("org.ow2.asm:asm:9.5")
    implementation("io.netty:netty-codec-http:4.1.82.Final")
    implementation("org.squiddev:Cobalt:0.7.3")

    implementation("org.teavm:teavm-jso")
    implementation("org.teavm:teavm-jso-apis")
    implementation("org.teavm:teavm-platform")
    implementation("org.teavm:teavm-classlib")
    implementation("org.teavm:teavm-metaprogramming-api")

    "builderImplementation"("org.teavm:teavm-tooling")
    "builderImplementation"("org.teavm:teavm-metaprogramming-impl")
    "builderImplementation"("org.teavm:teavm-jso-impl")
    "builderImplementation"("org.ow2.asm:asm:9.5")
    "builderImplementation"("org.ow2.asm:asm-commons:9.5")
}

tasks {
    val teaVMOutput = layout.buildDirectory.dir("teaVM")

    val compileTeaVM by registering(JavaExec::class) {
        group = "build"
        description = "Stitch all files together"

        val propertiesFile = projectDir.resolve("original/CC-Tweaked/gradle.properties")

        inputs.files(sourceSets.main.get().runtimeClasspath).withPropertyName("inputClasspath")
        inputs.file(propertiesFile).withPropertyName("inputProperties")
        outputs.dir(teaVMOutput).withPropertyName("output")

        classpath = sourceSets["builder"].runtimeClasspath
        jvmArguments.addAll(provider {
            val properties = Properties().also { propertiesFile.inputStream().use(it::load) }

            val main = sourceSets.main.get()
            listOf(
                "-Dcct.input=${main.output.classesDirs.asPath}",
                "-Dcct.version=${properties["modVersion"]}",
                "-Dcct.classpath=${main.runtimeClasspath.asPath}",
                "-Dcct.output=${teaVMOutput.get().asFile.absolutePath}"
            )
        })
        mainClass.set("cc.tweaked.builder.Builder")
        javaLauncher.set(project.javaToolchains.launcherFor { languageVersion.set(java.toolchain.languageVersion) })
    }

    val genCssTypes by registering(Exec::class) {
        group = "build"
        description = "Generates type stubs for CSS files"

        inputs.file("src/web/ts/styles.css").withPropertyName("styles.css")
        outputs.file("src/web/ts/styles.css.d.ts").withPropertyName("styles.css.d.ts")

        commandLine("node", "tools/setup.js")
    }

    val rollup by registering(Exec::class) {
        group = "build"
        description = "Combines multiple Javascript files into one"

        dependsOn(genCssTypes)
        inputs.files(compileTeaVM).withPropertyName("teaVM")
        inputs.files(fileTree("src/web")).withPropertyName("sources")
        inputs.file("package-lock.json").withPropertyName("package-lock.json")
        inputs.file("rollup.config.js").withPropertyName("Rollup config")

        outputs.files(fileTree(layout.buildDirectory.dir("web"))).withPropertyName("output")
        outputs.files(fileTree(layout.buildDirectory.dir("webMin"))).withPropertyName("outputMin")

        commandLine("npx", "rollup", "-c")
    }

    assemble { dependsOn(rollup) }
    assembleDist { dependsOn(rollup) }
}

gradle.projectsEvaluated {
    tasks.withType(JavaCompile::class) {
        options.compilerArgs.addAll(listOf("-Xlint", "-Xlint:-processing"))
    }
}
