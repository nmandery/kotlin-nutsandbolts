import java.io.ByteArrayOutputStream

plugins {
    java
    `maven-publish`
    kotlin("jvm") version "1.4.20"
    id("org.jetbrains.dokka") version "0.10.0"
}

fun getVersionFromGit(fallback: String = "unknown"): String {
    return try {
        ByteArrayOutputStream().use { os ->
            exec {
                commandLine("git", "describe", "--dirty", "--always")
                standardOutput = os
            }
            os.toString("UTF8").lines().firstOrNull() ?: "unknown"
        }
            .run {
                if ("^v[0-9].".toRegex().containsMatchIn(this)) {
                    this.trimStart('v')
                } else {
                    this
                }
            }
            // replace "-" so this is not understood as a classifier of the java package
            .replace("-", "_")
    } catch (e: Exception) {
        println("not build from a git repository")
        fallback
    }.also {
        println("${project.name} version: $it")
    }
}

group = "net.nmandery"
version = getVersionFromGit()

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    testImplementation("junit", "junit", "4.12")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
    testImplementation(kotlin("reflect"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    }
    test {
        useJUnitPlatform()
        testLogging {
            // show standard out and standard error of the test JVM(s) on the console
            showStandardStreams = true
            events("passed", "skipped", "failed", "standard_out", "standard_error")
        }
    }
    dokka {
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

// from https://docs.github.com/en/free-pro-team@latest/packages/using-github-packages-with-your-projects-ecosystem/configuring-gradle-for-use-with-github-packages#example-using-gradle-groovy-for-a-single-package-in-a-repository
// and https://guides.gradle.org/building-kotlin-jvm-libraries/#step_4_publish_to_local_repo
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/nmandery/kotlin-nutsandbolts")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GH_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GH_TOKEN")
            }
        }
        mavenLocal()
    }
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            artifact(dokkaJar)
        }
    }
}