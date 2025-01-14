dependencies {
    implementation((project(":domain")))
    implementation((project(":application")))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-config")
    implementation("org.springframework.security:spring-security-oauth2-jose")
}