plugins {
    id("java")
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        bundledPlugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        pluginVerifier()
        zipSigner()
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.tpluscode.agentssync"
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        description = """
            Synchronize, edit, and manage a system-wide AGENTS.md file across projects and machines using JetBrains Settings Sync.
            Includes a full Markdown editor tab, history snapshot tracking with rollback support, and an embedded MCP (Model Context Protocol) tool interface for JetBrains AI Assistant, Junie, AIR, and Claude.
        """.trimIndent()
        vendor {
            name = "Tomasz Pluskiewicz"
            url = "https://github.com/tpluscode"
        }
        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // channels = listOf(providers.gradleProperty("pluginVersion").map { if (it.contains('-')) "eap" else "default" }.get())
    }

    pluginVerification {
        ides {
            ide("IC-2024.2.4")
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
