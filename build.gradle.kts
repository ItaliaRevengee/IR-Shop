import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "com.italiarevenge"
version = "1.0.0"
description = "Premium Minecraft server shop plugin for PaperMC 1.21.11+"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper API — provided at runtime
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Vault — provided by Vault plugin at runtime
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // PlaceholderAPI — optional soft dependency
    compileOnly("me.clip:placeholderapi:2.11.6")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
    }

    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        mergeServiceFiles()
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        exclude("META-INF/versions/*/module-info.class")
    }

    build {
        dependsOn("shadowJar")
    }

    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        exclude("paper-plugin.yml")
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
