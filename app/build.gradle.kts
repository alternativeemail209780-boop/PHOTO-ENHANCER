plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "local.enhancer"
    compileSdk = 34
    defaultConfig { applicationId = "local.enhancer.photo"; minSdk = 29; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    androidResources { noCompress += "onnx" }
}
dependencies { implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0") }
