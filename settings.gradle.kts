rootProject.name = "pypy-cdn-mirror"

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage") repositories {
    mavenCentral()
  }
}

include(
  "pypy-cdn-mirror",
)
