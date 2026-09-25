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
}

application {
    mainClass.set("org.example.TopSecret")
}

tasks.test {
    useJUnitPlatform()
}
