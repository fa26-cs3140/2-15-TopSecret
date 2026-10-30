plugins {
    id("java")
    application
    jacoco
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.+") 
    testImplementation("org.mockito:mockito-junit-jupiter:5.+")
    implementation("org.xerial:sqlite-jdbc:3.53.0.0")
}

application {
    mainClass.set("org.example.TopSecret")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "org.example.TopSecret")
    }

    from({
        configurations.runtimeClasspath.get().map {  file -> if (file.isDirectory) file else zipTree(file) }
    })

    archiveFileName.set("TopSecret.jar");

    // Prevents Gradle from crashing if multiple libraries contain the same meta-files (e.g. LICENSE files)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE 

}

// Java 26-compatible coverage; production dependencies are unchanged.
jacoco {
    toolVersion = "0.8.15"
}

val integrationPatterns = listOf(
    "**/*IntegrationTest.class", "**/*Sqlite*Test.class", "**/DatabaseManagerTest.class",
    "**/AgencyInterfaceAuditLogManagerTest.class",
    "**/UserAccountManagerCredentialManagerTest.class",
    "**/AuditLogManagerDatabaseManagerTest.class"
)

val unitTest by tasks.registering(Test::class) {
    description = "Runs isolated unit tests without SQLite/file integration tests."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    exclude(integrationPatterns)
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs persistence and cross-component integration tests separately."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    include(integrationPatterns)
    shouldRunAfter(unitTest)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports { xml.required.set(true) }
}

tasks.register<JacocoReport>("jacocoUnitTestReport") {
    dependsOn(unitTest)
    executionData(layout.buildDirectory.file("jacoco/unitTest.exec"))
    sourceSets(sourceSets.main.get())
    reports { xml.required.set(true); html.required.set(true) }
}
