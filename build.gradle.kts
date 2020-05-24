plugins {
    java
    kotlin("jvm") version "1.3.72"
}

group = "com.github.nmandery.nutsandbolts"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    testImplementation("junit", "junit", "4.12")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform()
        testLogging {
            // show standard out and standard error of the test JVM(s) on the console
            showStandardStreams = true
            events("passed", "skipped", "failed", "standard_out", "standard_error")
        }
    }
}