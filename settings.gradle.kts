rootProject.name = "aya-dance-indexer"

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage") repositories {
    mavenCentral()
  }
}

include(
  "base",
  "mirror",
)
