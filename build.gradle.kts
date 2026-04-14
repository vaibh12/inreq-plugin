plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.4"
}

group = "com.inreq.plugin"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        instrumentationTools()
        pluginVerifier()
    }
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

kotlin { jvmToolchain(17) }

intellijPlatform {
    pluginConfiguration {
        name = "InReq"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "265.*"
        }
    }
}

tasks {
    wrapper { gradleVersion = "8.14" }

    // Skip buildSearchableOptions — crashes with JDK 25 on IntelliJ 2024.2
    // This task is only needed for JetBrains Marketplace publishing, not for local dev
    named("buildSearchableOptions") {
        enabled = false
    }
}
