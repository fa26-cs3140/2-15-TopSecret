plugins {
    id("java")
    application
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

    // Prevents Gradle from crashing if multiple libraries contain the same meta-files (e.g. LICENSE files)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE 

}
