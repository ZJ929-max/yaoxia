plugins { kotlin("jvm") }

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

kotlin { jvmToolchain(21) }

dependencies { testImplementation("junit:junit:4.13.2") }

tasks.test { useJUnit() }
