plugins { kotlin("multiplatform"); kotlin("plugin.serialization"); id("com.android.library"); id("app.cash.sqldelight") }
kotlin {
    androidTarget()
    jvm()
    jvmToolchain(21)
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("io.ktor:ktor-client-core:3.3.1")
            implementation("io.ktor:ktor-client-content-negotiation:3.3.1")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.1")
            implementation("io.insert-koin:koin-core:4.1.1")
            implementation("app.cash.sqldelight:runtime:2.1.0")
            implementation("app.cash.sqldelight:coroutines-extensions:2.1.0")
            implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4")
        }
        commonTest.dependencies { implementation(kotlin("test")); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2"); implementation("io.ktor:ktor-client-mock:3.3.1") }
        jvmTest.dependencies { implementation("app.cash.sqldelight:sqlite-driver:2.1.0") }
        androidUnitTest.dependencies { implementation("app.cash.sqldelight:sqlite-driver:2.1.0") }
    }
}
android { namespace = "pe.upeu.ecorecicla.shared"; compileSdk = 36; defaultConfig { minSdk = 26 }; compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 } }
sqldelight { databases { create("EcoDatabase") { packageName.set("pe.upeu.ecorecicla.db") } } }
