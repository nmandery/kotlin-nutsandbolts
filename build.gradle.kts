import java.io.ByteArrayOutputStream

plugins {
    java
    `maven-publish`
    kotlin("jvm") version "1.4.10"
    id("org.jetbrains.dokka") version "0.10.0"
}

fun getCommitFromGit(fallback: String = "unknown"): String {
    return try {
        val commit = ByteArrayOutputStream().use { os ->
            exec {
                commandLine("git", "show", "-s", "--format=%h")
                standardOutput = os
            }
            os.toString("UTF8").lines().firstOrNull() ?: "unknown"
        }
        val isDirty = ByteArrayOutputStream().use { os ->
            exec {
                commandLine("git", "describe", "--dirty", "--always")
                standardOutput = os
            }
            os.toString("UTF8").lines().firstOrNull()?.endsWith("-dirty") ?: false
        }
        if (isDirty) {
            "${commit}_dirty"
        } else {
            commit
        }
    } catch (e: Exception) {
        println("not build from a git repository: ${e.message}")
        fallback
    }
}

group = "net.nmandery"
version = getCommitFromGit()

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    testImplementation("junit", "junit", "4.12")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
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
    classifier = "javadoc"
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
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            artifact(dokkaJar)
        }
    }
}