import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.teavm.tooling.TeaVMProblemRenderer
import org.teavm.tooling.TeaVMTargetType
import org.teavm.tooling.TeaVMTool
import org.teavm.tooling.TeaVMToolLog
import org.teavm.tooling.sources.DirectorySourceFileProvider
import org.teavm.tooling.sources.JarSourceFileProvider
import org.teavm.vm.TeaVMOptimizationLevel
import java.net.URLClassLoader
import java.util.Properties

buildscript {
    repositories {
        mavenCentral()
        maven("https://dl.bintray.com/konsoletyper/teavm")
    }

    dependencies {
        // TeaVM and tooling
        classpath("org.teavm:teavm-core:${project.properties["teavm_version"]}")
        classpath("org.teavm:teavm-tooling:${project.properties["teavm_version"]}")
        classpath("commons-io:commons-io:2.6")

        // Misc buildscript stuff
        classpath("io.github.java-diff-utils:java-diff-utils:4.0")
        classpath("org.ajoberstar.grgit:grgit-gradle:3.0.0")
    }
}

plugins {
    java
    application
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/konsoletyper/teavm")
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    implementation("com.google.guava:guava:22.0")
    implementation("org.apache.commons:commons-lang3:3.6")
    implementation("it.unimi.dsi:fastutil:8.2.2")

    implementation("org.teavm:teavm-jso:${project.properties["teavm_version"]}")
    implementation("org.teavm:teavm-jso-apis:${project.properties["teavm_version"]}")
    implementation("org.teavm:teavm-platform:${project.properties["teavm_version"]}")
    implementation("org.teavm:teavm-classlib:${project.properties["teavm_version"]}")
}

application {
    mainClassName = "cc.squiddev.cct.Main"
}

tasks {
    val compileTeaVM by registering {
        group = "build"
        description = "Converts Java code to Javascript using TeaVM"

        inputs.files(project.configurations.getByName("runtime").allArtifacts.files).withPropertyName("jars")

        val dir = File(buildDir, "teaVM")
        outputs.file(File(dir, "classes.js")).withPropertyName("output")

        doLast {
            val log = object : TeaVMToolLog {
                override fun info(text: String?) = project.logger.info(text)
                override fun debug(text: String?) = project.logger.debug(text)
                override fun warning(text: String?) = project.logger.warn(text)
                override fun error(text: String?) = project.logger.error(text)
                override fun info(text: String?, e: Throwable?) = project.logger.info(text)
                override fun debug(text: String?, e: Throwable?) = project.logger.debug(text, e)
                override fun warning(text: String?, e: Throwable?) = project.logger.warn(text, e)
                override fun error(text: String?, e: Throwable?) = project.logger.error(text, e)
            }

            fun getSource(f: File) = if (f.isFile && f.absolutePath.endsWith(".jar")) {
                JarSourceFileProvider(f)
            } else {
                DirectorySourceFileProvider(f)
            }

            val runtime = project.configurations.getByName("runtimeClasspath")
            val dependencies = runtime.resolve().map { it.toURI().toURL() }
            val artifacts = runtime.allArtifacts.files.map { it.toURI().toURL() }

            URLClassLoader((dependencies + artifacts).toTypedArray(), log.javaClass.classLoader).use { classloader ->
                val tool = TeaVMTool()
                tool.classLoader = classloader
                tool.targetDirectory = dir
                tool.targetFileName = "classes.js"
                tool.mainClass = application.mainClassName
                tool.isMinifying = true
                tool.optimizationLevel = TeaVMOptimizationLevel.ADVANCED
                tool.log = log
                tool.targetType = TeaVMTargetType.JAVASCRIPT

                project.convention
                    .getPlugin(JavaPluginConvention::class.java)
                    .sourceSets
                    .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                    .allSource
                    .forEach { tool.addSourceFileProvider(getSource(it)) }

                tool.generate()

                TeaVMProblemRenderer.describeProblems(tool.getDependencyInfo().getCallGraph(), tool.getProblemProvider(), log)
                if (tool.problemProvider.severeProblems.isNotEmpty()) throw IllegalStateException("Build failed")
            }
        }
    }

    val bundleTeaVM by registering {
        group = "build"
        description = "Converts the TeaVM file into a AMD module"

        dependsOn(compileTeaVM)
        inputs.file(File("$buildDir/teaVM/classes.js")).withPropertyName("input")
        outputs.file(File("$buildDir/javascript/classes.js")).withPropertyName("output")

        doLast {
            File("$buildDir/javascript/classes.js").bufferedWriter().use { writer ->
                File("$buildDir/teaVM/classes.js").reader().use { it.copyTo(writer) }

                // Export a simple function which sets the callbacks and then boots the VM
                writer.write("export default callbacks => {\n")
                writer.write("  window.callbacks = callbacks;\n")
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

    val compileTypescript by registering(Exec::class) {
        group = "build"
        description = "Converts TypeScript code to Javascript"

        dependsOn(genCssTypes)
        inputs.files(fileTree("src/web/ts")).withPropertyName("sources")
        inputs.file("package.json").withPropertyName("package.json")
        inputs.file("tsconfig.json").withPropertyName("TypeScript config")

        outputs.dir("$buildDir/javascript").withPropertyName("output")

        commandLine(mkCommand("npm run --silent prepare:tsc"))
    }

    val copyCss by registering(Copy::class) {
        group = "build"
        description = "Copies CSS files into the rollup directory."

        inputs.file("src/web/ts/styles.css").withPropertyName("styles.css")
        outputs.files(fileTree("$buildDir/rollup")).withPropertyName("output")

        from("src/web/ts") {
            include("styles.css")
        }

        into("$buildDir/javascript/")
    }

    val rollup by registering(Exec::class) {
        group = "build"
        description = "Combines multiple Javascript files into one"

        dependsOn(bundleTeaVM, compileTypescript, copyCss)
        inputs.files(fileTree("$buildDir/javascript")).withPropertyName("sources")
        inputs.file("package.json").withPropertyName("package.json")
        inputs.file("rollup.config.js").withPropertyName("Rollup config")

        outputs.files(fileTree("$buildDir/rollup")).withPropertyName("output")

        commandLine(mkCommand("npm run --silent prepare:rollup"))
    }

    val website by registering(Sync::class) {
        group = "build"
        description = "Combines all resource files into one distribution."

        dependsOn(rollup)

        /** Replace various template strings within our files. */
        fun replaceTemplate(x: String) = x
            .replace("{{version}}", inputs.properties["hash"].toString())
            .replace("{{monaco}}", "https://cdn.jsdelivr.net/npm/monaco-editor@0.20.0")

        inputs.property("hash", {
            try {
                FileRepositoryBuilder().setWorkTree(File(".")).build()
                    .use { it.resolve("HEAD").name() }
            } catch (e: Exception) {
                project.logger.warn("Cannot get current commit ($e)")
                "unknown"
            }
        })

        from("$buildDir/rollup");
        from("node_modules/requirejs/require.js");

        from("$buildDir/javascript/loader.js") {
            filter { replaceTemplate(it) }
        }

        from("src/web/public") {
            include("*.html")
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
        inputs.file("package.json").withPropertyName("package.json")
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
        delete("src/main/resources/assets/computercraft")
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
                val patches = File("src/patches/java/$packageName")
                val modified = File("src/main/java/$packageName")
                val original = File("original/$project/src/main/java/$packageName")
                if (!modified.isDirectory) throw IllegalArgumentException("$modified is not a directory or does not exist")
                if (!original.isDirectory) throw IllegalArgumentException("$original is not a directory or does not exist")

                modified.walk()
                    .filter { it.name != ".editorconfig" && it.isFile }
                    .forEach { modifiedFile ->
                        val relativeFile = modifiedFile.relativeTo(modified)

                        val originalFile = original.resolve(relativeFile)
                        val originalContents = originalFile.readLines()

                        val diff = DiffUtils.diff(originalContents, modifiedFile.readLines())
                        val patch = UnifiedDiffUtils.generateUnifiedDiff(originalFile.toString(), modifiedFile.toString(), originalContents, diff, 3)
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
                    include("resources/assets/computercraft/lua/bios.lua")
                    include("resources/assets/computercraft/lua/rom/**")

                    // We need some stuff from the api and shared, but don't want to have to cope with
                    // adding Minecraft as a dependency. This gets a little ugly.
                    exclude("java/dan200/computercraft/**/package-info.java")
                    include("java/dan200/computercraft/api/lua/**")
                    include("java/dan200/computercraft/api/filesystem/**")
                    include("java/dan200/computercraft/api/peripheral/**")
                    exclude("java/dan200/computercraft/api/peripheral/IPeripheralTile.java")
                    exclude("java/dan200/computercraft/api/peripheral/IPeripheralProvider.java")

                    // Core is pretty simple.
                    include("java/dan200/computercraft/core/**")
                    // We just exclude some FS stuff, as it's a bit of a faff to deal with.
                    exclude("java/dan200/computercraft/core/filesystem/ComboMount.java")
                    exclude("java/dan200/computercraft/core/filesystem/EmptyMount.java")
                    exclude("java/dan200/computercraft/core/filesystem/FileMount.java")
                    exclude("java/dan200/computercraft/core/filesystem/JarMount.java")
                    exclude("java/dan200/computercraft/core/filesystem/SubMount.java")
                    // Also exclude all the Netty-specific code
                    exclude("java/dan200/computercraft/core/apis/http/CheckUrl.java")
                    exclude("java/dan200/computercraft/core/apis/http/NetworkUtils.java")
                    exclude("java/dan200/computercraft/core/apis/http/request/HttpRequestHandler.java")
                    exclude("java/dan200/computercraft/core/apis/http/websocket/WebsocketHandler.java")

                    include("java/dan200/computercraft/shared/util/Colour.java")
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
                val patches = File("src/patches/")
                val modified = File("src/main/")
                val original = files.dir

                // We load CC:T's properties and use them to substitute in the mod version
                val props = Properties()
                File("original/CC-Tweaked/gradle.properties").inputStream().use { props.load(it) }

                files.forEach { originalFile ->
                    val relativeFile = originalFile.relativeTo(original)
                    val modifiedFile = modified.resolve(relativeFile)
                    val patchFile = File(patches, "$relativeFile.patch")

                    modifiedFile.parentFile.mkdirs()
                    if (patchFile.exists()) {
                        println("Patching $relativeFile")
                        val patch = UnifiedDiffUtils.parseUnifiedDiff(patchFile.readLines())
                        val modifiedContents = DiffUtils.patch(originalFile.readLines(), patch)
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
            }

            // Generate Resources.java
            println("Making Resources.java")
            val builder = StringBuilder()
            val resources = fileTree("original/CC-Tweaked/src/main/resources/assets/computercraft/lua/rom/")
            resources.forEach { builder.append("        \"").append(it.relativeTo(resources.dir).toString().replace('\\', '/')).append("\",\n") }

            val contents = File("src/main/java/cc/squiddev/cct/mount/Resources.java.txt").readText().replace("__FILES__", builder.toString())
            File("src/main/java/cc/squiddev/cct/mount/Resources.java").writeText(contents)

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
