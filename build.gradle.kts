import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    `java-library`

    id("io.papermc.paperweight.userdev") version "1.7.1"

    id("xyz.jpenilla.run-paper") version
            "2.3.0" // Adds runServer and runMojangMappedServer tasks for testing

    id("xyz.jpenilla.resource-factory-bukkit-convention") version
            "1.1.1" // Generates plugin.yml based on the Gradle config

  	id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "citizensRepo"
        url = uri("https://maven.citizensnpcs.co/repo")
    }
}

group = "trident.grimm"

version = "1.0.0-SNAPSHOT"

description = "Battlegrounds"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    compileOnly("trident.grimm:elib:1.0.0-SNAPSHOT")

    compileOnly("com.github.cryptomorin:XSeries:9.7.0")
    compileOnly("org.apache.commons:commons-math3:3.6.1")

    compileOnly("redis.clients:jedis:4.3.1")

    compileOnly("net.kyori:adventure-text-minimessage:4.14.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.14.0")

    compileOnly("net.citizensnpcs:citizens-main:2.0.33-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }

    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
}

tasks {
    compileJava {
        options.release = 21
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
	
	shadowJar {
	   archiveClassifier.set("")
	}

}

tasks.register<Copy>("copyJar") {
    from("build/libs/")

    into("../../../Servers/BG.S1/plugins/update")

    include("**/*.jar")

    println("Jar files have been copied to the plugin update directory.")
}

tasks.named("shadowJar") {
	finalizedBy("copyJar")
}

bukkitPluginYaml {
    main = "trident.grimm.battlegrounds.App"
    load = BukkitPluginYaml.PluginLoadOrder.POSTWORLD
	commands {
		register("kill") {
			description = "Kill yourself"
			aliases = listOf("k")
			usage = "/kill"
		}
	}
    authors.add("Eric")
    apiVersion = "1.20.5"
}
