plugins {
    id("java")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    maxParallelForks = if (System.getenv("CI") != null) {
        Runtime.getRuntime().availableProcessors()
    } else {
        // https://docs.gradle.org/8.0/userguide/performance.html#execute_tests_in_parallel
        (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    }
}

tasks.test {
    // Use the latest Gradle supported JDK here.
    configureCommon(20, GradleVersion.current().version)
}

listOf(8, 11, 17).forEach {
    if (it != 17) {
        // Gradle 6.0 doesn't support JDK 17.
        testJdkOnGradle(it, "6.0")
    }
    testJdkOnGradle(it, "7.3")
    testJdkOnGradle(it, "7.6")
    testJdkOnGradle(it, GradleVersion.current().version)
}

fun testJdkOnGradle(jdkVersion: Int, gradleVersion: String) {
    tasks.register<Test>("testJdk${jdkVersion}onGradle${gradleVersion}") {
        configureCommon(jdkVersion, gradleVersion)
        classpath = tasks.test.get().classpath
        testClassesDirs = tasks.test.get().testClassesDirs
    }
}

fun Test.configureCommon(jdkVersion: Int, gradleVersion: String) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the test suite on JDK $jdkVersion and Gradle $gradleVersion"

    systemProperty("gradleVersion", gradleVersion)
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(jdkVersion)
    }

    tasks.check {
        dependsOn(this@configureCommon)
    }
}
