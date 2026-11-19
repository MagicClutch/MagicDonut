import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.0"
    id("net.fabricmc.fabric-loom") version "1.17.13"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()

    runs {
        named("client") {
            // Log into the dev client with your own Microsoft account (DevAuth Neo) so you can join real servers.
            property("devauth.enabled", "1")
            property("devauth.account", "main")
        }
    }

    mods {
        register("magicdonututils") {
            sourceSet("main")
            sourceSet("client")
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

repositories {
    mavenCentral()
    google()
    // Transitive dependencies of OneConfig
    maven("https://maven.terraformersmc.com/releases/") { content { includeGroup("com.terraformersmc") } }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    maven("https://repo.hypixel.net/repository/Hypixel/") { content { includeGroup("net.hypixel") } }
    maven("https://maven.bawnorton.com/releases") { content { includeGroupAndSubgroups("com.github.bawnorton") } }
    maven("https://repo.polyfrost.org/releases") {
        name = "Polyfrost"
        content {
            includeGroup("org.polyfrost")
            includeGroup("org.polyfrost.oneconfig")
        }
    }
}

dependencies {
    // Minecraft and Fabric
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // OneConfig: settings library and screen. Players install it as a separate mod; in dev it runs from the classpath.
    val oneConfig = "org.polyfrost.oneconfig:${project.property("minecraft_version")}-fabric:${project.property("oneconfig_version")}"
    compileOnly(oneConfig)
    localRuntime(oneConfig)

    // Dev only: not part of the mod, never shipped to players.
    localRuntime("net.litetex.mcm:dev-auth-neo:${project.property("devauth_neo_version")}")

}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    inputs.property("oneconfig_version", project.property("oneconfig_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version")!!,
            "loader_version" to project.property("loader_version")!!,
            "kotlin_loader_version" to project.property("kotlin_loader_version")!!,
            "oneconfig_version" to project.property("oneconfig_version")!!
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
