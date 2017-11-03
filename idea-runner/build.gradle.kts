import org.jetbrains.intellij.IntelliJPluginExtension

apply { plugin("kotlin") }

configureIntellijPlugin()

dependencies {
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-maven"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-jvm"))

    runtimeOnly(files(toolsJar()))
}

afterEvaluate {
    dependencies {
        compile(intellij())
    }
}

afterEvaluate {
    tasks.findByName("runIde")?.let { tasks.remove(it) }

    task<JavaExec>("runIde") {
        dependsOn(":dist", ":prepare:idea-plugin:idea-plugin", ":ideaPlugin")

        classpath = the<JavaPluginConvention>().sourceSets["main"].runtimeClasspath

        main = "com.intellij.idea.Main"

        workingDir = project.the<IntelliJPluginExtension>().ideaDependency.classes

        val ideaPluginDir: File by rootProject.extra

        jvmArgs(
                "-Xmx1250m",
                "-XX:ReservedCodeCacheSize=240m",
                "-XX:+HeapDumpOnOutOfMemoryError",
                "-ea",
                "-Didea.is.internal=true",
                "-Didea.debug.mode=true",
                "-Didea.system.path=../system-idea",
                "-Didea.config.path=../config-idea",
                "-Dapple.laf.useScreenMenuBar=true",
                "-Dapple.awt.graphics.UseQuartz=true",
                "-Dsun.io.useCanonCaches=false",
                "-Dplugin.path=${ideaPluginDir.absolutePath}",
                "-Dkotlin.internal.mode.enabled=true",
                "-Didea.additional.classpath=../idea-kotlin-runtime/kotlin-runtime.jar,../idea-kotlin-runtime/kotlin-reflect.jar"
        )

        if (project.hasProperty("noPCE")) {
            jvmArgs("-Didea.ProcessCanceledException=disabled")
        }

        args()
    }
}
