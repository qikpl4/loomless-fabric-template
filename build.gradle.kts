import groovy.json.JsonSlurper
import java.net.URI

plugins {
  id("java")
}

repositories {
  mavenCentral()

  maven {
    url = uri("https://maven.fabricmc.net")
  }

  maven {
    url = uri("https://libraries.minecraft.net")
  }  

  ivy {
    url = uri("https://piston-data.mojang.com/v1/objects")
    
    patternLayout {
      artifact("[module]/client.jar")
    }

    metadataSources {
      artifact()
    }
  }
}

dependencies {
  val fabricLoader = providers.gradleProperty("fabric-loader").get()
  val minecraftVersion = providers.gradleProperty("minecraft").get()

  implementation("net.fabricmc:fabric-loader:$fabricLoader")

  // since we're avoiding Loom, we have to pay the price
  val manifest = JsonSlurper().parse(URI("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").toURL()) as Map<*, *>
  val versions = manifest["versions"] as List<Map<*, *>>
  val versionMap = versions.find { it["id"] == minecraftVersion }
      ?: throw IllegalStateException("Minecraft version $minecraftVersion not found in manifest")
  val versionUrl = versionMap["url"] as? String
      ?: throw IllegalStateException("Version URL not found for $minecraftVersion")

  val versionData = JsonSlurper().parse(URI(versionUrl).toURL()) as Map<*, *>
  val libraries = versionData["libraries"] as List<Map<*, *>>  

  libraries.forEach { lib ->  
    val libName = lib["name"] as? String
    if (libName != null) {
      implementation(libName)
    }
  }

  val downloads = versionData["downloads"] as? Map<*, *>
  val client = downloads?.get("client") as? Map<*, *>
  val clientUrl = client?.get("url") as? String
  if (clientUrl != null) {
    val sha = clientUrl.split("/")[5]
    implementation(":$sha:")
  }
}
