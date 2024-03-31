plugins {
  java
  groovy
}

repositories {
  mavenCentral()
}

val rootDir = projectDir.parentFile!!

dependencies {
  api(libs.aya.build.util)

  // The following is required for
  // - extracting common parts inside `graalvmNative` block
  // - specifying the plugin version from libs.versions.toml
  implementation(libs.graal.nitools)
}
