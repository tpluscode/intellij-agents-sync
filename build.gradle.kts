plugins {
    id("java")
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

group = "com.github.tpluscode.agentssync"
version = providers.gradleProperty("pluginVersion").getOrElse("0.0.0")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2024.2.4")
        bundledPlugin("org.intellij.plugins.markdown")
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
        name = "Agents Sync"
        version = providers.gradleProperty("pluginVersion").getOrElse("0.0.0")
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
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
