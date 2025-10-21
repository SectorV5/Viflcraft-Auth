import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
    id("net.kyori.blossom").version("1.3.1")
    id("java-library")
    id("xyz.kyngs.libby.plugin").version("1.2.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    // mavenLocal()
    maven { url = uri("https://repo.opencollab.dev/maven-snapshots/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/") }
    maven { url = uri("https://repo.kyngs.xyz/public/") }
    maven { url = uri("https://mvn.exceptionflug.de/repository/exceptionflug-public/") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://repo.alessiodp.com/releases/") }
    maven { url = uri("https://jitpack.io/") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
}

blossom {
    replaceToken("@version@", version)
}

tasks.withType<ShadowJar> {
    archiveFileName.set("ViflcraftAuth.jar")

    dependencies {
        exclude(dependency("org.slf4j:.*:.*"))
        exclude(dependency("org.checkerframework:.*:.*"))
        exclude(dependency("com.google.errorprone:.*:.*"))
        exclude(dependency("com.google.protobuf:.*:.*"))
    }

    relocate("co.aikar.acf", "win.sectorfive.viflcraftauth.lib.acf")
    relocate("com.github.benmanes.caffeine", "win.sectorfive.viflcraftauth.lib.caffeine")
    relocate("com.typesafe.config", "win.sectorfive.viflcraftauth.lib.hocon")
    relocate("com.zaxxer.hikari", "win.sectorfive.viflcraftauth.lib.hikari")
    relocate("org.mariadb", "win.sectorfive.viflcraftauth.lib.mariadb")
    relocate("org.bstats", "win.sectorfive.viflcraftauth.lib.metrics")
    relocate("org.intellij", "win.sectorfive.viflcraftauth.lib.intellij")
    relocate("org.jetbrains", "win.sectorfive.viflcraftauth.lib.jetbrains")
    relocate("io.leangen.geantyref", "win.sectorfive.viflcraftauth.lib.reflect")
    relocate("org.spongepowered.configurate", "win.sectorfive.viflcraftauth.lib.configurate")
    relocate("net.byteflux.libby", "win.sectorfive.viflcraftauth.lib.libby")
    relocate("org.postgresql", "win.sectorfive.viflcraftauth.lib.postgresql")
    relocate("com.github.retrooper.packetevents", "win.sectorfive.viflcraftauth.lib.packetevents.api")
    relocate("io.github.retrooper.packetevents", "win.sectorfive.viflcraftauth.lib.packetevents.platform")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<Jar> {
    from("../LICENSE.txt")
}

libby {
    excludeDependency("org.slf4j:.*:.*")
    excludeDependency("org.checkerframework:.*:.*")
    excludeDependency("com.google.errorprone:.*:.*")
    excludeDependency("com.google.protobuf:.*:.*")

    // Often redeploys the same version, so calculating checksum causes false flags
    noChecksumDependency("com.github.retrooper.packetevents:.*:.*")
}

configurations.all {
    // I hate this, but it needs to be done as bungeecord does not support newer versions of adventure, and packetevents includes it
    resolutionStrategy {
        force("net.kyori:adventure-text-minimessage:4.14.0")
        force("net.kyori:adventure-text-serializer-gson:4.14.0")
        force("net.kyori:adventure-text-serializer-legacy:4.14.0")
        force("net.kyori:adventure-text-serializer-json:4.14.0")
        force("net.kyori:adventure-api:4.14.0")
        force("net.kyori:adventure-nbt:4.14.0")
        force("net.kyori:adventure-key:4.14.0")
    }
}

dependencies {
    //API
    implementation(project(":API"))

    //Velocity
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-proxy:3.2.0-SNAPSHOT-277")

    //MySQL
    libby("org.mariadb.jdbc:mariadb-java-client:3.5.1")
    libby("com.zaxxer:HikariCP:6.2.1")

    //SQLite
    libby("org.xerial:sqlite-jdbc:3.47.1.0")

    //PostgreSQL
    libby("org.postgresql:postgresql:42.7.5")

    //ACF
    libby("com.github.kyngs.commands:acf-velocity:7d5bf7cac0")
    libby("com.github.kyngs.commands:acf-bungee:7d5bf7cac0")
    libby("com.github.kyngs.commands:acf-paper:7d5bf7cac0")

    //Utils
    libby("com.github.ben-manes.caffeine:caffeine:3.2.0")
    libby("org.spongepowered:configurate-hocon:4.1.2")
    libby("at.favre.lib:bcrypt:0.10.2")
    compileOnly("dev.simplix:protocolize-api:2.4.2")
    libby("org.bouncycastle:bcprov-jdk18on:1.80")
    libby("org.apache.commons:commons-email:1.6.0")
    // DO NOT UPGRADE TO 4.15.0 OR ABOVE BEFORE TESTING WATERFALL AND BUNGEECORD COMPATIBILITY!!!
    libby("net.kyori:adventure-text-minimessage:4.14.0")
    libby("com.github.kyngs:LegacyMessage:0.2.0")

    //Geyser
    compileOnly("org.geysermc.floodgate:api:2.2.0-SNAPSHOT")
    //LuckPerms
    compileOnly("net.luckperms:api:5.4")

    //Bungeecord
    compileOnly("net.md-5:bungeecord-api:1.21-R0.3")
    compileOnly("com.github.ProxioDev.ValioBungee:RedisBungee-Bungee:0.12.5")
    libby("net.kyori:adventure-platform-bungeecord:4.1.2")

    //BStats
    libby("org.bstats:bstats-velocity:3.0.2")
    libby("org.bstats:bstats-bungeecord:3.0.2")
    libby("org.bstats:bstats-bukkit:3.0.2")

    //Paper
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    //compileOnly "com.comphenix.protocol:ProtocolLib:5.1.0"
    libby("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("io.netty:netty-transport:4.1.108.Final")
    compileOnly("com.mojang:datafixerupper:5.0.28") //I hate this so much
    compileOnly("org.apache.logging.log4j:log4j-core:2.23.1")

    //Libby
    implementation("xyz.kyngs.libby:libby-bukkit:1.6.0")
    implementation("xyz.kyngs.libby:libby-velocity:1.6.0")
    implementation("xyz.kyngs.libby:libby-bungee:1.6.0")
    implementation("xyz.kyngs.libby:libby-paper:1.6.0")

    //NanoLimboPlugin
    compileOnly("com.github.bivashy.NanoLimboPlugin:api:1.0.8")
}

tasks.withType<ProcessResources> {
    outputs.upToDateWhen { false }
    filesMatching("plugin.yml") {
        expand(mapOf("version" to version))
    }
    filesMatching("bungee.yml") {
        expand(mapOf("version" to version))
    }
    filesMatching("paper-plugin.yml") {
        expand(mapOf("version" to version))
    }
}