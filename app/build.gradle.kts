plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Firebase (google-services 먼저 적용)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)

    // DI
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.songdosamgyeop.order"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.songdosamgyeop.order"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // ✅ CSV 내보내기 사용 (툴바 '내보내기' 기능 ON)
            buildConfigField("boolean", "FEATURE_EXPORT", "true")
            buildConfigField("boolean", "FUNCTIONS_ENABLED", "true")  // 스텁 또는 에뮬레이터
            buildConfigField("boolean", "EMULATOR", "false")
            buildConfigField("String",  "EMULATOR_HOST", "\"\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 필요 시 운영에서 켜도 됨
            buildConfigField("boolean", "FEATURE_EXPORT", "true")
            buildConfigField("boolean", "FUNCTIONS_ENABLED", "true")   // 실서버 호출
            buildConfigField("boolean", "EMULATOR", "false")
            buildConfigField("String",  "EMULATOR_HOST", "\"\"")
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // ✅ desugar 활성화 (java.time 등 최신 API 사용)
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}


dependencies {
    // AndroidX / UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.browser) // ✅ PortOne 커스텀 탭 의존성

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // ✅ viewModels(), SavedState
    implementation(libs.androidx.lifecycle.runtime.ktx)   // ✅ lifecycleScope 등

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.google.material)
    implementation(libs.androidx.navigation.runtime.ktx)
    kapt(libs.hilt.compiler)

    // Timber
    implementation(libs.timber)

    // Firebase (BOM으로 버전 정렬)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging) // ✅ FCM Push

    // ✅ desugar 라이브러리 (compileOptions와 세트)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}