import org.aya.gradle.CommonTasks

val mainClassQName = project.group.toString() + ".Main"
CommonTasks.fatJar(project, mainClassQName)
CommonTasks.nativeImageConfig(project)
application.mainClass.set(mainClassQName)

plugins {
  id("org.graalvm.buildtools.native")
  application
}

dependencies {
  api(libs.annotations)
  api(libs.kala.common)

  implementation(libs.gson)
  implementation(libs.jsoup)
  implementation(libs.picocli.runtime)
  annotationProcessor(libs.picocli.codegen)

  implementation(project(":base"))

  testImplementation(libs.junit.params)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest)
}
