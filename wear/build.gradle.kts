plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.anishexperiments.anishos"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.anishexperiments.anishos"
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
// Use the official Wear artwork committed at the repository root.
tasks.register<Copy>("installWearLauncherIcon") {
    from(rootProject.file("AnishOS-wear-icon.png"))
    into(layout.projectDirectory.dir("src/main/res/mipmap-nodpi"))
    rename { "anishos_launcher.png" }
}
tasks.named("preBuild").configure { dependsOn("installWearLauncherIcon") }

dependencies {
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
}
