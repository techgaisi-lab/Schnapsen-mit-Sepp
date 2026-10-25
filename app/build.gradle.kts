import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Signaturschlüssel für den Play Store. Die Datei keystore.properties liegt NUR lokal im
// Projektordner (siehe keystore.properties.example) und ist in .gitignore – sie kommt nie auf GitHub.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasUploadKey = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "at.schnapsenmitsepp.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "at.schnapsenmitsepp.app"
        minSdk = 24
        targetSdk = 37
        // Für jedes Update im Play Store um 1 erhöhen.
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (hasUploadKey) {
            create("upload") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: entfernt ungenutzten Code und verkleinert die App – deutlich schneller auf alten Tablets.
            optimization {
                enable = true
            }
            // Mit eigenem Upload-Schlüssel signieren, sobald keystore.properties vorhanden ist.
            // Ohne die Datei wird zum Ausprobieren mit dem Debug-Schlüssel signiert –
            // so ein Build wird vom Play Store abgelehnt.
            signingConfig = if (hasUploadKey) signingConfigs.getByName("upload") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":schnapsen"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}
