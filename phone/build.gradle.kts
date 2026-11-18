plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.anishexperiments.anishos.phone"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.anishexperiments.anishos"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
        buildConfigField("String", "SUPABASE_URL", "\"https://nueripsuvrfhylenqumi.supabase.co\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"sb_publishable_u0cOgR-3RY00zTnLrzuhEQ_ocVurxcV\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
// Use the official companion artwork committed at the repository root.
tasks.register<Copy>("installCompanionLauncherIcon") {
    from(rootProject.file("AnishOS-Phone-icon.png"))
    into(layout.projectDirectory.dir("src/main/res/mipmap-nodpi"))
    rename { "anishos_launcher.png" }
}
tasks.named("preBuild").configure { dependsOn("installCompanionLauncherIcon") }

dependencies {
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
