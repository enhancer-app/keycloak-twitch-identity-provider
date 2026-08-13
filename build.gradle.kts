plugins {
    `java-library`
    `maven-publish`
}

group = "io.github.enhancer-app"
version = providers.gradleProperty("version").getOrElse("1.0.0-SNAPSHOT")
description = "Twitch social identity provider for Keycloak"

val keycloakVersion = libs.versions.keycloak.get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // Keycloak supplies these at runtime. The provider JAR remains thin.
    compileOnly(libs.keycloak.core)
    compileOnly(libs.keycloak.common)
    compileOnly(libs.keycloak.server.spi)
    compileOnly(libs.keycloak.server.spi.private)
    compileOnly(libs.keycloak.services)
    compileOnly(libs.jakarta.ws.rs)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jboss.logging)
    compileOnly(libs.httpclient)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito.core)
    testImplementation(libs.keycloak.core)
    testImplementation(libs.keycloak.common)
    testImplementation(libs.keycloak.server.spi)
    testImplementation(libs.keycloak.server.spi.private)
    testImplementation(libs.keycloak.services)
    testImplementation(libs.jakarta.ws.rs)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jboss.logging)
    testImplementation(libs.httpclient)
    testImplementation(libs.resteasy.core)
    testImplementation(libs.testcontainers)
    testRuntimeOnly(libs.slf4j.simple)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.jar {
    archiveFileName.set("keycloak-twitch.jar")
    manifest {
        attributes(
            "Implementation-Title" to "keycloak-twitch-identity-provider",
            "Implementation-Version" to project.version,
            "Keycloak-Target-Version" to keycloakVersion,
        )
    }
}

val withSmoke = providers.gradleProperty("withSmoke").isPresent

tasks.test {
    useJUnitPlatform {
        if (!withSmoke) excludeTags("smoke")
    }
    systemProperty("twitch.keycloakVersion", keycloakVersion)
    systemProperty("twitch.providerJar", tasks.jar.flatMap { it.archiveFile }.get().asFile.absolutePath)
    systemProperty("org.jboss.logging.provider", "jdk")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

if (withSmoke) tasks.test { dependsOn(tasks.jar) }

tasks.register("verify") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all Gradle verification tasks. Equivalent to Maven verify."
    dependsOn(tasks.check)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "keycloak-twitch-identity-provider"
            pom {
                name.set("Keycloak Twitch Identity Provider")
                description.set(project.description)
                url.set("https://github.com/enhancer-app/keycloak-twitch-identity-provider")
                licenses { license { name.set("MIT License"); url.set("https://opensource.org/licenses/MIT") } }
                scm { url.set("https://github.com/enhancer-app/keycloak-twitch-identity-provider") }
            }
        }
    }
}
