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
            maven("https://maven.squiddev.cc")
        }
        filter {
            includeGroup("cc.tweaked")
            includeGroup("org.teavm")
        }
    }
}

val filteredCCSources by tasks.registering(Sync::class) {
    from("vendor/CC-Tweaked/projects/core-api/src/main/java")
    from("vendor/CC-Tweaked/projects/core/src/main/java")
    from("vendor/CC-Tweaked/projects/web/src/main/java") {
        exclude("cc/tweaked/web/Main.java")
    }

    into(layout.buildDirectory.dir(name))
}

sourceSets {
    main {
        java.srcDir(filteredCCSources)
        resources.srcDir("vendor/CC-Tweaked/projects/core/src/main/resources")
    }
    register("builder") {
        java.srcDir("vendor/CC-Tweaked/projects/web/src/builder/java")
        resources.srcDir("vendor/CC-Tweaked/projects/web/src/builder/resources")
    }
}

dependencies {
    compileOnly(libs.bundles.annotations)
    implementation(libs.asm)
    implementation(libs.cobalt)
    implementation(libs.fastutil)
    implementation(libs.guava)
    implementation(libs.jzlib)
    implementation(libs.netty.http)
    implementation(libs.netty.proxy)
    implementation(libs.netty.socks)
    implementation(libs.slf4j)
    runtimeOnly(libs.teavm.core) // Contains the TeaVM runtime

    implementation(libs.bundles.teavm.api)

    "builderCompileOnly"(libs.bundles.annotations)
    "builderImplementation"(libs.bundles.teavm.tooling)
    "builderImplementation"(libs.asm)
    "builderImplementation"(libs.asm.commons)
}

tasks {
    val teaVMOutput = layout.buildDirectory.dir("teaVM")

    val compileTeaVM by registering(JavaExec::class) {
        group = "build"
        description = "Stitch all files together"

        val propertiesFile = projectDir.resolve("vendor/CC-Tweaked/gradle.properties")

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
        mainClass.set("cc.tweaked.web.builder.Builder")
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
