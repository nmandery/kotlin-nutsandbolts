# Kotlin nuts & bolts

Personal utility library for kotlin to share reusable code.

This library mostly targets the JVM and attempts to pull in as few additional dependencies as possible.

## Usage via jitpack 

This library can be pulled into projects using jitpack - service which builds libraries on the fly directly from the github repository. The service can be directly referenced from a maven pom.xml or gradles build.gradle - see the instructions on the site.

One short example for gradle:

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
dependencies {
    implementation 'com.github.nmandery:kotlin-nutsandbolts:GIT_TAG'
}
```

## License

Apache 2.0