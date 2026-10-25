plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "at.schnapsenmitsepp.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "at.schnapsenmitsepp.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // R8: entfernt ungenutzten Code und verkleinert die App – deutlich schneller auf alten Tablets.
            optimization {
                enable = true
            }
            // Zum Ausprobieren ohne eigenen Schlüssel mit dem Debug-Schlüssel signieren.
            // Für den Play Store hier den eigenen Release-Schlüssel eintragen.
            signingConfig = signingConfigs.getByName("debug")
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
