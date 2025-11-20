plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)

}

android {
    namespace = "com.example.mobile_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mobile_android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            /* ========================================
             * Base URL 설정 가이드
             * ========================================
             *
             * 1. 에뮬레이터에서 테스트:
             *    buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000\"")
             *    - 10.0.2.2는 에뮬레이터에서 호스트 PC의 localhost를 가리킴
             *
             * 2. 실제 디바이스에서 테스트 (USB 연결):
             *    buildConfigField("String", "BASE_URL", "\"http://localhost:8000\"")
             *    - 터미널에서 실행: adb reverse tcp:8000 tcp:8000
             *    - 디바이스의 localhost:8000이 PC의 localhost:8000으로 포워딩됨
             *
             * 3. 실제 디바이스에서 테스트 (같은 Wi-Fi 네트워크):
             *    buildConfigField("String", "BASE_URL", "\"http://192.168.0.10:8000\"")
             *    - PC의 실제 IP 주소로 변경 (cmd에서 ipconfig로 확인)
             *
             * 4. 개발 서버 연결:
             *    buildConfigField("String", "BASE_URL", "\"https://dev.yourserver.com\"")
             *
             * ======================================== */

            // 기본 설정: 에뮬레이터용
//            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000\"")

            // 실제 디바이스 USB 연결 시 아래 주석 해제하고 위 줄 주석 처리
             buildConfigField("String", "BASE_URL", "\"http://localhost:8000\"")

            // Wi-Fi 연결 시 PC IP로 변경
            // buildConfigField("String", "BASE_URL", "\"http://192.168.0.10:8000\"")

            isDebuggable = true
        }
        release {
            /* ========================================
             * 운영 서버 URL 설정
             * ======================================== */
            buildConfigField("String", "BASE_URL", "\"https://api.yourserver.com\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true  // BuildConfig 활성화
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation("androidx.navigation:navigation-fragment:2.9.5")
    implementation("androidx.navigation:navigation-ui:2.9.5")
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
// EncryptedSharedPreferences
}
