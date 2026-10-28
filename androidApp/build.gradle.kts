plugins { id("com.android.application"); kotlin("android"); kotlin("plugin.compose") }
android {
    namespace = "pe.upeu.ecorecicla"
    compileSdk = 36
    defaultConfig { applicationId = "pe.upeu.ecorecicla"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "0.1"
        buildConfigField("String", "API_URL", "\"${providers.gradleProperty("ecoApiUrl").getOrElse("http://10.0.2.2:8080/api/v1/")}\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":shared"))
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("io.ktor:ktor-client-okhttp:3.3.1")
    implementation("io.insert-koin:koin-android:4.1.1")
    implementation("app.cash.sqldelight:android-driver:2.1.0")
}
