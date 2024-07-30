///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    java
    id("com.diffplug.spotless") version "6.25.0"
}
buildscript {
    repositories {
        mavenLocal()
        maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
        mavenCentral()
    }
    dependencies {
        classpath("org.eclipse.keyple:keyple-gradle:0.2.+") { isChanging = true }
    }
}
apply(plugin = "org.eclipse.keyple")

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
repositories {
    mavenLocal()
    maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
}
dependencies {
    // Begin Keyple configuration (generated by 'https://keyple.org/components/overview/configuration-wizard/')
    implementation("org.eclipse.keypop:keypop-reader-java-api:2.0.1")
    implementation("org.eclipse.keypop:keypop-calypso-card-java-api:2.1.0")
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.1")
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.4.0")
    implementation("org.eclipse.keyple:keyple-service-java-lib:3.2.3")
    implementation("org.eclipse.keyple:keyple-card-calypso-java-lib:3.1.2")
    implementation("org.eclipse.keyple:keyple-plugin-pcsc-java-lib:2.2.1")
    // End Keyple configuration
    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("com.google.code.gson:gson:2.8.5")
}

val javaSourceLevel: String by project
val javaTargetLevel: String by project
java {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
    println("Compiling Java $sourceCompatibility to Java $targetCompatibility.")
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
tasks {
    spotless {
        java {
            target("src/**/*.java")
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
            importOrder("java", "javax", "org", "com", "")
            removeUnusedImports()
            googleJavaFormat()
        }
    }
    register("fatJarAnalyze", Jar::class.java) {
        archiveClassifier.set("Analyze-fat")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "org.calypsonet.tool.calypso.card.Tool_AnalyzeCardFileStructure")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
    register("fatJarCheck", Jar::class.java) {
        archiveClassifier.set("Check-fat")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "org.calypsonet.tool.calypso.card.Tool_CheckCardFileStructure")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
}

afterEvaluate {
    tasks.named("build").configure {
        dependsOn(tasks.named("fatJarAnalyze"), tasks.named("fatJarCheck"))
    }
}
