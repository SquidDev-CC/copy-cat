import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.net.URLClassLoader
import java.util.Properties

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        // Misc buildscript stuff
        classpath("io.github.java-diff-utils:java-diff-utils:4.0")
        classpath("org.ajoberstar.grgit:grgit-gradle:3.0.0")
    }
}

plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
    maven("https://squiddev.cc/maven")
}

val teavmCli by configurations.creating {
    extendsFrom(configurations.runtimeOnly.get())
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.checkerframework:checker-qual:3.17.0")

    implementation("com.google.guava:guava:22.0")
    implementation("org.apache.commons:commons-lang3:3.6")
    implementation("it.unimi.dsi:fastutil:8.2.2")
    implementation("org.ow2.asm:asm:8.0.1")

    implementation("org.teavm:teavm-jso:${project.properties["teavm_version"]}")
    implementation("org.teavm:teavm-jso-apis:${project.properties["teavm_version"]}")
    implementation("org.teavm:teavm-platform:${project.properties["teavm_version"]}")
    implementation("org.teavm:teavm-classlib:${project.properties["teavm_version"]}")

    teavmCli("org.teavm:teavm-cli:${project.properties["teavm_version"]}")
}

application {
    mainClass.set("cc.squiddev.cct.Main")
}

tasks {
    val compileTeaVM by registering(JavaExec::class) {
        group = "build"
        description = "Converts Java code to Javascript using TeaVM"

        inputs.files(project.configurations.getByName("runtimeOnly").allArtifacts.files).withPropertyName("jars")

        val dir = File(buildDir, "teaVM")
        outputs.file(File(dir, "classes.js")).withPropertyName("output")

        classpath = teavmCli + sourceSets.main.get().runtimeClasspath
        mainClass.set("org.teavm.cli.TeaVMRunner")
        args(listOf("-O2", "--minify", "--targetdir", dir.absolutePath, application.mainClass.get()))
        javaLauncher.set(project.javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        })
    }

    val bundleTeaVM by registering {
        group = "build"
        description = "Converts the TeaVM file into a AMD module"

        dependsOn(compileTeaVM)
        inputs.file(File(buildDir, "teaVM/classes.js")).withPropertyName("input")
        outputs.file(File(buildDir, "javascript/classes.js")).withPropertyName("output")

        doLast {
            File(buildDir, "javascript/classes.js").bufferedWriter().use { writer ->
                File(buildDir, "teaVM/classes.js").reader().use { it.copyTo(writer) }

                // Export a simple function which sets the callbacks and then boots the VM
                writer.write("export default callbacks => {\n")
                writer.write("  window.copycatCallbacks = callbacks;\n")
                writer.write("  main();\n");
                writer.write("};\n");
            }
        }
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

        dependsOn(bundleTeaVM, genCssTypes)
        inputs.file(File(buildDir, "javascript/classes.js")).withPropertyName("teaVM")
        inputs.files(fileTree("src/web/ts")).withPropertyName("sources")
        inputs.file("package-lock.json").withPropertyName("package-lock.json")
        inputs.file("rollup.config.js").withPropertyName("Rollup config")

        outputs.files(fileTree(File(buildDir, "rollup"))).withPropertyName("output")

        commandLine(mkCommand("npm run --silent prepare:rollup"))
    }

    val website by registering(Sync::class) {
        group = "build"
        description = "Combines all resource files into one distribution."

        dependsOn(rollup)
        inputs.files(fileTree(File(buildDir, "rollup"))).withPropertyName("rollup")

        /** Replace various template strings within our files. */
        fun replaceTemplate(x: String) = x
            .replace("{{version}}", inputs.properties["hash"].toString());

        inputs.property("hash", {
            try {
                FileRepositoryBuilder().setWorkTree(projectDir).build()
                    .use { it.resolve("HEAD").name() }
            } catch (e: Exception) {
                project.logger.warn("Cannot get current commit ($e)")
                "unknown"
            }
        })

        from("$buildDir/rollup");

        from("src/web/public") {
            filter { replaceTemplate(it) }
        }

        into("$buildDir/web")
    }

    val minify by registering {
        group = "build"
        description = "Minifies the JS and CSS files"

        val js = fileTree("$buildDir/web") { include("**/*.js") }
        val css = fileTree("$buildDir/web") { include("**/*.css") }
        val dest = file("$buildDir/minified")

        inputs.files(js).withPropertyName("js")
        inputs.files(css).withPropertyName("css")
        inputs.file("package-lock.json").withPropertyName("package-lock.json")
        outputs.files(fileTree(dest))
        dependsOn(website)

        doLast {
            // Clean the directory a little bit.
            fileTree(dest).forEach { it.delete() }

            js.forEach { original ->
                val relative = original.relativeTo(js.dir)
                val renamed = dest.resolve(relative)
                renamed.parentFile.mkdirs()

                exec {
                    commandLine(mkCommand("npm run --silent prepare:terser -- --output \"$renamed\" \"$original\""))
                }
            }

            css.forEach { original ->
                val relative = original.relativeTo(js.dir)
                val renamed = dest.resolve(relative)
                renamed.parentFile.mkdirs()

                exec {
                    commandLine(mkCommand("npm run --silent prepare:uglifycss -- --output \"$renamed\" \"$original\""))
                }
            }
        }
    }

    val websiteMinified by registering(Sync::class) {
        group = "build"
        description = "Produces a minified website"

        dependsOn(website, minify)

        from("$buildDir/web") { exclude("**/*.css", "**/*.js") }
        from("$buildDir/minified") { include("**/*.css", "**/*.js") }
        into("$buildDir/webMin")
    }

    val cleanPatches by registering(Delete::class) {
        group = "patch"
        description = "Cleans the patch directory"

        delete("src/patches")
    }

    val cleanSources by registering(Delete::class) {
        group = "patch"
        description = "Cleans the modified sources"

        delete("src/main/java/dan200/computercraft/")
        delete("src/main/resources/data/computercraft")
        delete(fileTree("src/main/java/org/squiddev/cobalt/") { exclude(".editorconfig") })
    }

    val makePatches by registering {
        group = "patch"
        description = "Diffs a canonical directory against our currently modified version"
        dependsOn(cleanPatches)

        doLast {
            listOf(
                Pair("CC-Tweaked", "dan200/computercraft"),
                Pair("Cobalt", "org/squiddev/cobalt")
            ).forEach { (project, packageName) ->
                val patches = File(projectDir, "src/patches/java/$packageName")
                val modified = File(projectDir, "src/main/java/$packageName")
                val original = File(projectDir, "original/$project/src/main/java/$packageName")
                if (!modified.isDirectory) throw IllegalArgumentException("$modified is not a directory or does not exist")
                if (!original.isDirectory) throw IllegalArgumentException("$original is not a directory or does not exist")

                modified.walk()
                    .filter { it.name != ".editorconfig" && it.isFile }
                    .forEach { modifiedFile ->
                        val relativeFile = modifiedFile.relativeTo(modified)

                        val originalFile = original.resolve(relativeFile)
                        val originalContents = originalFile.readLines()

                        val filename = relativeFile.name
                        val diff = DiffUtils.diff(originalContents, modifiedFile.readLines())
                        val patch = UnifiedDiffUtils.generateUnifiedDiff(filename, filename, originalContents, diff, 3)
                        if (!patch.isEmpty()) {
                            val patchFile = File(patches, "$relativeFile.patch")
                            patchFile.parentFile.mkdirs()
                            patchFile.bufferedWriter().use { writer ->
                                patch.forEach {
                                    writer.write(it)
                                    writer.write("\n")
                                }
                            }
                        }
                    }
            }
        }
    }

    val applyPatches by registering {
        group = "patch"
        description = "Applies our patches to the source directories"
        dependsOn(cleanSources)

        doLast {
            listOf(
                fileTree("original/CC-Tweaked/src/main") {
                    include("resources/data/computercraft/lua/bios.lua")
                    include("resources/data/computercraft/lua/rom/**")

                    // We need some stuff from the api and shared, but don't want to have to cope with
                    // adding Minecraft as a dependency. This gets a little ugly.
                    exclude("java/dan200/computercraft/**/package-info.java")
                    include("java/dan200/computercraft/api/lua/**")
                    exclude("java/dan200/computercraft/api/lua/GenericSource.java")
                    include("java/dan200/computercraft/api/filesystem/**")
                    include("java/dan200/computercraft/api/peripheral/**")
                    exclude("java/dan200/computercraft/api/peripheral/GenericPeripheral.java")
                    exclude("java/dan200/computercraft/api/peripheral/IPeripheralProvider.java")
                    exclude("java/dan200/computercraft/api/peripheral/IPeripheralTile.java")

                    // Core is pretty simple.
                    include("java/dan200/computercraft/core/**")
                    exclude("java/dan200/computercraft/core/tracking/ComputerMBean.java")
                    exclude("java/dan200/computercraft/core/computer/ComputerThread.java")
                    // We exclude the actual asm generation stuff, but need some of the interfaces
                    exclude("java/dan200/computercraft/core/asm/DeclaringClassLoader.java")
                    exclude("java/dan200/computercraft/core/asm/Generator.java")
                    exclude("java/dan200/computercraft/core/asm/GenericMethod.java")
                    exclude("java/dan200/computercraft/core/asm/Reflect.java")
                    // We just exclude some FS stuff, as it's a bit of a faff to deal with.
                    exclude("java/dan200/computercraft/core/filesystem/ComboMount.java")
                    exclude("java/dan200/computercraft/core/filesystem/EmptyMount.java")
                    exclude("java/dan200/computercraft/core/filesystem/FileMount.java")
                    exclude("java/dan200/computercraft/core/filesystem/JarMount.java")
                    exclude("java/dan200/computercraft/core/filesystem/SubMount.java")
                    exclude("java/dan200/computercraft/core/filesystem/ResourceMount.java")
                    // Also exclude all the Netty-specific code
                    exclude("java/dan200/computercraft/core/apis/http/CheckUrl.java")
                    exclude("java/dan200/computercraft/core/apis/http/NetworkUtils.java")
                    exclude("java/dan200/computercraft/core/apis/http/request/HttpRequestHandler.java")
                    exclude("java/dan200/computercraft/core/apis/http/websocket/WebsocketHandler.java")
                    exclude("java/dan200/computercraft/core/apis/http/websocket/WebsocketCompressionHandler.java")

                    exclude("java/dan200/computercraft/core/apis/http/options/AddressRuleConfig.java")

                    include("java/dan200/computercraft/shared/util/Colour.java")
                    include("java/dan200/computercraft/shared/util/LuaUtil.java")
                    include("java/dan200/computercraft/shared/util/IoUtil.java")
                    include("java/dan200/computercraft/shared/util/Palette.java")
                    include("java/dan200/computercraft/shared/util/StringUtil.java")
                    include("java/dan200/computercraft/shared/util/ThreadUtils.java")
                    include("java/dan200/computercraft/ComputerCraft.java")
                },
                fileTree("original/Cobalt/src/main") {
                    include("java/**")
                }
            ).forEach { files ->
                val patches = File(projectDir, "src/patches/")
                val modified = File(projectDir, "src/main/")
                val original = files.dir

                // We load CC:T's properties and use them to substitute in the mod version
                val props = Properties()
                File(projectDir, "original/CC-Tweaked/gradle.properties").inputStream().use { props.load(it) }

                var failed = false
                files.forEach { originalFile ->
                    val relativeFile = originalFile.relativeTo(original)
                    val modifiedFile = modified.resolve(relativeFile)
                    val patchFile = File(patches, "$relativeFile.patch")
                    modifiedFile.parentFile.mkdirs()
                    if (patchFile.exists()) {
                        println("Patching $relativeFile")
                        val patch = UnifiedDiffUtils.parseUnifiedDiff(patchFile.readLines())
                        val modifiedContents = try {
                            DiffUtils.patch(originalFile.readLines(), patch)
                        } catch (e: Exception) {
                            println("Failed to apply patch. This should be manually fixed")
                            failed = true
                            originalFile.readLines()
                        }
                        modifiedFile.bufferedWriter().use { writer ->
                            modifiedContents.forEach {
                                if (modifiedFile.name == "ComputerCraft.java" && it.startsWith("    static final String VERSION = \"")) {
                                    // Update the version number within ComputerCraft.java. Yes, ugly, but easier than
                                    // the alternatives.
                                    writer.write("    static final String VERSION = \"${props["mod_version"]}\";")
                                } else {
                                    writer.write(it)
                                }
                                writer.write("\n")
                            }
                        }
                    } else {
                        originalFile.copyTo(modifiedFile)
                    }
                }

                if (failed) throw IllegalStateException("Failed to apply patches")
            }

            // Generate Resources.java
            println("Making Resources.java")
            val builder = StringBuilder()
            val resources = fileTree("original/CC-Tweaked/src/main/resources/data/computercraft/lua/rom/")
            resources.forEach { builder.append("        \"").append(it.relativeTo(resources.dir).toString().replace('\\', '/')).append("\",\n") }

            val contents = File(projectDir, "src/main/java/cc/squiddev/cct/mount/Resources.java.txt").readText().replace("__FILES__", builder.toString())
            File(projectDir, "src/main/java/cc/squiddev/cct/mount/Resources.java").writeText(contents)

        }
    }

    assemble {
        dependsOn(website)
    }

    assembleDist {
        dependsOn(websiteMinified)
    }

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
