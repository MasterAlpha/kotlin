import org.jetbrains.intellij.IntelliJPluginExtension

apply { plugin("kotlin") }

configureIntellijPlugin {
    version = (rootProject.extra["versions.intellij"] as String).replaceFirst("IC-", "IU-")
}

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

val runUltimate by task<JavaExec> {
    dependsOn(":dist", ":prepare:idea-plugin:idea-plugin", ":ideaPlugin", ":ultimate:idea-ultimate-plugin")

    classpath = the<JavaPluginConvention>().sourceSets["main"].runtimeClasspath

    main = "com.intellij.idea.Main"

    afterEvaluate {
        workingDir = project.the<IntelliJPluginExtension>().ideaDependency.classes
    }

    val ideaUltimatePluginDir: File by rootProject.extra

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
            "-Dplugin.path=${ideaUltimatePluginDir.absolutePath}",
            "-Dkotlin.internal.mode.enabled=true",
            "-Didea.additional.classpath=../idea-kotlin-runtime/kotlin-runtime.jar,../idea-kotlin-runtime/kotlin-reflect.jar"
    )

    if (project.hasProperty("noPCE")) {
        jvmArgs("-Didea.ProcessCanceledException=disabled")
    }

    args()
}