import org.aya.gradle.CommonTasks

CommonTasks.nativeImageConfig(project)

dependencies {
  api(libs.annotations)
  api(libs.kala.common)

  implementation(libs.gson)
  implementation(libs.jsoup)

  implementation("com.github.houbb:pinyin:0.4.0")

  testImplementation(libs.junit.params)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest)
}
