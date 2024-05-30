import org.aya.gradle.CommonTasks

val mainClassQName = project.group.toString() + ".Main"
CommonTasks.fatJar(project, mainClassQName)
CommonTasks.nativeImageConfig(project)

plugins {
  id("org.graalvm.buildtools.native")
  application
}

dependencies {
  api(libs.annotations)
  api(libs.kala.common)

  implementation(libs.gson)
  implementation(libs.jsoup)

  implementation(project(":base"))

  testImplementation(libs.junit.params)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest)
}

graalvmNative {
  binaries {
    named("main") {
      imageName.set("pypy-cdn-mirror")
      mainClass.set(mainClassQName)
      debug.set(System.getenv("CI") == null)
    }
  }
  CommonTasks.nativeImageBinaries(
    project, javaToolchains, this,
    true,
    true,
  )
}
