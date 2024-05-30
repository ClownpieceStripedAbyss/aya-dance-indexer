rootProject.name = "aya-dance-indexer"

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage") repositories {
    mavenCentral()
  }
}

include(
  "pypy-cdn-mirror",
)
