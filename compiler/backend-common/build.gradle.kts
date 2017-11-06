
apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:descriptor.loader.java"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:cli-common"))
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../ir/backend.common/src")
    }
    "test" {}
}
