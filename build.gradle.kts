import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.teavm.tooling.RuntimeCopyOperation
import org.teavm.tooling.TeaVMTargetType
import org.teavm.tooling.TeaVMTool
import org.teavm.tooling.TeaVMToolLog
import org.teavm.tooling.sources.DirectorySourceFileProvider
import org.teavm.tooling.sources.JarSourceFileProvider
import org.teavm.vm.TeaVMOptimizationLevel
import java.net.URLClassLoader

buildscript {
    repositories {
        mavenCentral()
        maven("https://dl.bintray.com/konsoletyper/teavm")
    }

    dependencies {
        classpath("org.teavm:teavm-core:${project.properties["teavm_version"]}")
        classpath("org.teavm:teavm-tooling:${project.properties["teavm_version"]}")
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
        outputs.file(File(dir, "classes.raw.js")).withPropertyName("output")

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
                tool.targetFileName = "classes.raw.js"
                tool.mainClass = application.mainClassName
                tool.runtime = RuntimeCopyOperation.MERGED
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

                if (!tool.problemProvider.severeProblems.isEmpty()) throw IllegalStateException("Build failed")
            }
        }
    }

    val bundleTeaVM by registering {
        group = "build"
        description = "Converts the TeaVM file into a AMD module"

        dependsOn(compileTeaVM)
        val dir = File(buildDir, "teaVM")
        inputs.file(File(dir, "classes.raw.js")).withPropertyName("input")
        outputs.file(File(dir, "classes.js")).withPropertyName("output")

        doLast {
            File(dir, "classes.js").bufferedWriter().use { writer ->
                File(dir, "classes.raw.js").reader().use { it.copyTo(writer) }
                writer.write("export default callbacks => {\n");
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

    val compileTypescript by registering(Exec::class) {
        group = "build"
        description = "Converts TypeScript code to Javascript"

        inputs.files(fileTree("src/web/ts")).withPropertyName("sources")
        inputs.file("package.json").withPropertyName("package.json")
        inputs.file("tsconfig.json").withPropertyName("TypeScript config")

        outputs.dir("$buildDir/typescript").withPropertyName("output")

        commandLine(mkCommand("npm run --silent tsc"))
    }

    val rollup by registering(Exec::class) {
        group = "build"
        description = "Combines multiple Javascript files into one"

        dependsOn(bundleTeaVM, compileTypescript)
        inputs.file("$buildDir/teaVM/classes.js").withPropertyName("teaVM")
        inputs.files(fileTree("$buildDir/typescript")).withPropertyName("typescript")
        inputs.file("package.json").withPropertyName("package.json")
        inputs.file("rollup.config.js").withPropertyName("Rollup config")

        outputs.files(fileTree("$buildDir/rollup")).withPropertyName("output")

        commandLine(mkCommand("npm run rollup --silent"))
    }

    val website by registering(Sync::class) {
        group = "build"
        description = "Combines all resource files into one distribution."

        dependsOn(rollup)

        inputs.property("hash", {
            try {
                FileRepositoryBuilder().setWorkTree(File(".")).build()
                    .use { it.resolve("HEAD").name() }
            } catch (ignored: Exception) {
                ignored.printStackTrace()
                "unknown"
            }
        })

        from("$buildDir/rollup") {
            include("*.js")
            // So require.js thinks that files ending in '.js' must be absolute and so leaves off the baseUrl. Rollup
            // will include the ".js" on non-external modules, and so we need to "fix" that.
            // Yes, this is horrible and there's probably better ways to fix it.
            filter { it.replace(".js\'],", "\'],") }
            into("assets")
        }

        from("$buildDir/typescript/loader.js") {
            filter { it.replace("{{version}}", inputs.properties["hash"].toString()) }
            into("assets")
        }

        from("src/web/public") {
            include("*.html")
            filter { it.replace("{{version}}", inputs.properties["hash"].toString()) }
        }

        from("src/web/public") {
            exclude("*.html")
            exclude("assets/font/config.json")
        }

        from("node_modules/gif.js/dist") {
            include("gif.worker.js")
            into("assets")
        }

        into("$buildDir/web")
    }

    val cleanPatches by registering(Delete::class) {
        delete("src/patches")
        description = "Cleans the patch directory"
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
                                writer.write(it)
                                writer.write("\n")
                            }
                        }
                    } else {
                        originalFile.copyTo(modifiedFile)
                    }
                }
            }
        }
    }

    assemble {
        dependsOn(website)
    }
}
