import org.apache.tools.ant.taskdefs.condition.Os
import org.aya.gradle.BuildUtil

plugins {
  java
  `jacoco-report-aggregation`
  idea
  `java-library`
  `maven-publish`
  signing
  alias(libs.plugins.jlink) apply false
}

var projectVersion: String by rootProject.ext
var currentPlatform: String by rootProject.ext
var supportedPlatforms: List<String> by rootProject.ext
var javaVersion: Int by rootProject.ext

projectVersion = libs.versions.project.get()
javaVersion = libs.versions.java.get().toInt()

// Workaround that `libs` is not available in `jacoco {}` block
var jacocoVersion = libs.versions.jacoco.get()

// Platforms we build jlink-ed aya for:
// The "current" means the "current platform", as it is unnecessary to detect what the current system is,
// as calling jlink with default arguments will build for the current platform.
currentPlatform = "current"

// In case we are in CI, or we are debugging CI locally, we build for all platforms
fun buildAllPlatforms(): Boolean {
  if (System.getenv("CI") != null) return true
  if (System.getProperty("user.name").contains("kiva")
    && project.rootDir.resolve(".git/HEAD").readLines().joinToString().contains("refs/heads/ci")) return true
  return false
}
supportedPlatforms = if (!buildAllPlatforms()) listOf(currentPlatform) else listOf(
  "windows-aarch64",
  "windows-x64",
  "linux-aarch64",
  "linux-x64",
  "linux-riscv64",
  "macos-aarch64",
  "macos-x64",
)

allprojects {
  group = "moe.kiva"
  version = projectVersion
}

val useJacoco = listOf("base", "mirror")

/** gradle.properties or environmental variables */
fun propOrEnv(name: String): String =
  if (hasProperty(name)) property(name).toString()
  else (System.getenv(name) ?: "")

subprojects {
  val proj = this@subprojects
  val isSnapshot = proj.version.toString().endsWith("SNAPSHOT")
  val isRelease = !isSnapshot

  apply {
    plugin("java")
    plugin("idea")
    if (name in useJacoco) plugin("jacoco")
    plugin("maven-publish")
    plugin("java-library")
    plugin("signing")
  }

  java {
    withSourcesJar()
    if (isRelease) withJavadocJar()
    JavaVersion.toVersion(javaVersion).let {
      sourceCompatibility = it
      targetCompatibility = it
    }
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
  }

  if (name in useJacoco) jacoco {
    toolVersion = jacocoVersion
  }

  idea.module {
    outputDir = file("out/production")
    testOutputDir = file("out/test")
  }

  tasks.withType<JavaCompile>().configureEach {
    modularity.inferModulePath.set(true)

    options.apply {
      encoding = "UTF-8"
      isDeprecation = true
      release.set(javaVersion)
      compilerArgs.addAll(listOf("-Xlint:unchecked", "--enable-preview"))
    }

    doLast {
      val root = destinationDirectory.asFile.get()
      // skip for test sources
      if (root.endsWith("test")) return@doLast
      val tree = fileTree(root)
      tree.include("**/*.class")
      tree.include("module-info.class")
      tree.forEach {
        BuildUtil.stripPreview(
          root.toPath(), it.toPath(),
          false, false,
          "java/lang/RuntimeException",
        )
      }
    }
  }

  tasks.withType<Javadoc>().configureEach {
    val options = options as StandardJavadocDocletOptions
    options.modulePath = tasks.compileJava.get().classpath.toList()
    options.addBooleanOption("-enable-preview", true)
    options.addStringOption("-source", javaVersion.toString())
    options.addStringOption("Xdoclint:none", "-quiet")
    options.encoding("UTF-8")
    options.tags(
      "apiNote:a:API Note:",
      "implSpec:a:Implementation Requirements:",
      "implNote:a:Implementation Note:",
    )
  }

  artifacts {
    add("archives", tasks.named("sourcesJar"))
    if (isRelease) add("archives", tasks.named("javadocJar"))
  }

  if (name in useJacoco) tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
      xml.required.set(true)
      csv.required.set(false)
      html.required.set(false)
    }
  }

  tasks.withType<Test>().configureEach {
    jvmArgs = listOf("--enable-preview")
    useJUnitPlatform()
    enableAssertions = true
    reports.junitXml.mergeReruns.set(true)
  }

  tasks.withType<JavaExec>().configureEach {
    jvmArgs = listOf("--enable-preview")
    enableAssertions = true
  }

  // Don't know why this manual dependency setting is needed, but it is.
  // I believe the jar task should depends on its dependencies' jar tasks automatically, but it doesn't.
  // In the past 2 years, the task without this line has been working fine, but today it suddenly broke.
  tasks.withType<Jar>().configureEach {
    proj.configurations.implementation.get().dependencies.withType<ProjectDependency>().forEach {
      val itProj = it.dependencyProject
      val itJar = itProj.tasks.named("jar")
      println("Adding task dependency :${itProj.name}:${itJar.name} to task :${proj.name}:$name")
      dependsOn(itJar)
    }
  }

  val ossrhUsername = propOrEnv("ossrhUsername")
  val ossrhPassword = propOrEnv("ossrhPassword")

  if (ossrhUsername.isNotEmpty()) publishing.repositories.maven {
    val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
    val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    url = if (isRelease) releasesRepoUrl else snapshotsRepoUrl
    name = "MavenCentral"
    credentials {
      username = ossrhUsername
      password = ossrhPassword
    }
  }

  // Gradle module metadata contains Gradle JVM version, disable it
  tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
  }

  publishing.publications {
    create<MavenPublication>("maven") {
      val githubUrl = "https://github.com/ClownpieceStripedAbyss/aya-dance-indexer"
      groupId = proj.group.toString()
      version = proj.version.toString()
      artifactId = proj.name
      from(components["java"])
      pom {
        description.set("~ To be described")
        name.set(proj.name)
        url.set(githubUrl)
        licenses {
          license {
            name.set("MIT")
            url.set("$githubUrl/blob/master/LICENSE")
          }
        }
        developers {
          fun dev(i: String, n: String, u: String) = developer {
            id.set(i)
            name.set(n)
            email.set(u)
          }
          
        }
        scm {
          connection.set("scm:git:$githubUrl")
          url.set(githubUrl)
        }
      }
    }
  }

  if (hasProperty("signing.keyId") && isRelease) signing {
    if (!hasProperty("signing.useBuiltinGpg")) useGpgCmd()
    sign(publishing.publications["maven"])
  }
}

apply { plugin("jacoco-report-aggregation") }
dependencies { useJacoco.forEach { jacocoAggregation(project(":$it")) { isTransitive = false } } }

val ccr = tasks["testCodeCoverageReport"]
tasks.register("githubActions") {
  group = "verification"
  dependsOn(ccr)
}

if (Os.isFamily(Os.FAMILY_WINDOWS)) tasks.register("showCCR") {
  dependsOn(ccr)
  doLast { exec { commandLine("cmd", "/c", "explorer", "build\\reports\\jacoco\\testCodeCoverageReport\\html\\index.html") } }
}
