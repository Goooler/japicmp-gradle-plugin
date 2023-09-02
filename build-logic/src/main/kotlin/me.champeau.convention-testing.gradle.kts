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

val currentGradle: String = GradleVersion.current().version
val allGradle = listOf("7.2", "7.3", "7.6", currentGradle)
val testJdk = providers.gradleProperty("me.champeau.japicmp.javaToolchain.test")
    .getOrElse("8").toInt()

tasks.test {
    // Skip default test.
    onlyIf { false }
}

allGradle.forEach {
    // Gradle 7.2 doesn't support JDK 17.
    if (it == "7.2" && testJdk == 17) return@forEach
    // Gradle 8.3 starts to support JDK 20.
    if (it < "8.3" && testJdk >= 20) return@forEach
    testJdkOnGradle(testJdk, it)
}

fun testJdkOnGradle(jdkVersion: Int, gradleVersion: String) {
    val task = tasks.register<Test>("testJdk${jdkVersion}onGradle${gradleVersion}") {
        configureCommon(jdkVersion, gradleVersion)
        classpath = tasks.test.get().classpath
        testClassesDirs = tasks.test.get().testClassesDirs
    }
    tasks.check {
        dependsOn(task)
    }
}

fun Test.configureCommon(jdkVersion: Int, gradleVersion: String) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the test suite on JDK $jdkVersion and Gradle $gradleVersion"

    systemProperty("gradleVersion", gradleVersion)
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(jdkVersion)
    }
}
