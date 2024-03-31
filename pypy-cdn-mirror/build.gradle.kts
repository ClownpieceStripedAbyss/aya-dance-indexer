import org.aya.gradle.CommonTasks
CommonTasks.fatJar(project, project.group.toString() + ".Main")

dependencies {
  api(libs.annotations)
  api(libs.kala.common)

  implementation(libs.gson)
  implementation(libs.jsoup)

  testImplementation(libs.junit.params)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest)
}
