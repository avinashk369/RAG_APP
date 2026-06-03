plugins {
	java
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.drdoc"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")


	implementation("dev.langchain4j:langchain4j:1.14.1")
	implementation("dev.langchain4j:langchain4j-ollama:1.14.1")
	implementation("dev.langchain4j:langchain4j-qdrant:1.14.1-beta24")
	implementation("org.apache.pdfbox:pdfbox:3.0.5")
	implementation ("com.fasterxml.jackson.core:jackson-databind")
	implementation("io.qdrant:client:1.17.0")

//	implementation("io.grpc:grpc-netty-shaded:1.81.0")
//	implementation("io.grpc:grpc-protobuf:1.81.0")
//	implementation("io.grpc:grpc-stub:1.81.0")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
