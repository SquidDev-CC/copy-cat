import org.apache.tools.ant.taskdefs.condition.Os
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

sourceSets {
    main {
        java.srcDir("src/main/javaPatched")
        resources.srcDir("src/main/resourcesPatched")
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
    val classesFile = layout.buildDirectory.file("teaVM/classes.js")

    val compileTeaVM by registering(JavaExec::class) {
        group = "build"
        description = "Stitch all files together"

        inputs.files(sourceSets.main.get().runtimeClasspath).withPropertyName("inputClasspath")
        outputs.file(classesFile).withPropertyName("javascript")

        classpath = sourceSets["builder"].runtimeClasspath
        jvmArguments.addAll(provider {
            val main = sourceSets.main.get()
            listOf(
                "-Dcct.input=${main.output.classesDirs.asPath}",
                "-Dcct.classpath=${main.runtimeClasspath.asPath}",
                "-Dcct.output=${classesFile.get().asFile.absolutePath}"
            )
        })
        mainClass.set("cc.tweaked.builder.Builder")
        javaLauncher.set(project.javaToolchains.launcherFor { languageVersion.set(java.toolchain.languageVersion) })
    }

    /**
     * Build a command which runs a specific program. Gradle daemons may not be started with the correct PATH, and so
     * will not find the required programs.
     */
    fun mkCommand(cmd: String) =
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            listOf("cmd", "/c", cmd)
        } else {
            listOf("sh", "-c", cmd)
        }

    val genCssTypes by registering(Exec::class) {
        group = "build"
        description = "Generates type stubs for CSS files"

        inputs.file("src/web/ts/styles.css").withPropertyName("styles.css")
        outputs.file("src/web/ts/styles.css.d.ts").withPropertyName("styles.css.d.ts")

        commandLine(mkCommand("npm run --silent prepare:setup"))
    }

    val rollup by registering(Exec::class) {
        group = "build"
        description = "Combines multiple Javascript files into one"

        dependsOn(compileTeaVM, genCssTypes)
        inputs.file(classesFile).withPropertyName("teaVM")
        inputs.files(fileTree("src/web")).withPropertyName("sources")
        inputs.file("package-lock.json").withPropertyName("package-lock.json")
        inputs.file("rollup.config.js").withPropertyName("Rollup config")

        outputs.files(fileTree(layout.buildDirectory.dir("web"))).withPropertyName("output")
        outputs.files(fileTree(layout.buildDirectory.dir("webMin"))).withPropertyName("outputMin")

        commandLine(mkCommand("npm run --silent prepare:rollup"))
    }

    val applyPatches by registering {
        group = "patch"
        description = "Applies our patches to the source directories"

        doLast {
            // We load CC:T's properties and use them to substitute in the mod version
            val props = Properties()
            File(projectDir, "original/CC-Tweaked/gradle.properties").inputStream().use { props.load(it) }

            sync {
                from("original/CC-Tweaked/projects/core-api/src/main/java")
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

                into("src/main/javaPatched")
            }

            sync {
                from("original/CC-Tweaked/projects/core/src/main/resources")
                into("src/main/resourcesPatched")
            }

            // Generate Resources.java
            println("Making Resources.java")
            val builder = StringBuilder()
            val resources = fileTree("original/CC-Tweaked/projects/core/src/main/resources/data/computercraft/lua/rom/")
            resources.forEach {
                builder
                    .append("        \"")
                    .append(it.relativeTo(resources.dir).toString().replace('\\', '/'))
                    .append("\",\n")
            }

            val contents = File(projectDir, "src/main/java/cc/tweaked/web/mount/Resources.java.txt").readText()
                .replace("__FILES__", builder.toString())
                .replace("__VERSION__", props["modVersion"].toString())
            File(projectDir, "src/main/java/cc/tweaked/web/mount/Resources.java").writeText(contents)
        }
    }

    assemble { dependsOn(rollup) }
    assembleDist { dependsOn(rollup) }

    val avengers by registering {
        description = "An alias to 'clean'. Should be run before assemble."
        dependsOn(clean)
    }
}

gradle.projectsEvaluated {
    tasks.withType(JavaCompile::class) {
        options.compilerArgs.addAll(listOf("-Xlint", "-Xlint:-processing"))
    }
}
