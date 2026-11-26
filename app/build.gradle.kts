plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.lovelite"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.lovelite"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
        resourceConfigurations += listOf("vi", "en")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Ký bằng khoá debug để cài thử ngay; hãy thay bằng keystore riêng khi phát hành.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    dependenciesInfo { includeInApk = false; includeInBundle = false }
}
// Không có thư viện ngoài: chỉ dùng Android framework + Kotlin stdlib (R8 cắt gọn).
