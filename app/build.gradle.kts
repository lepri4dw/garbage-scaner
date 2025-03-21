plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.garbagescaner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.garbagescaner"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/license.txt")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/notice.txt")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/*.proto")
            excludes.add("META-INF/*.kotlin_module")
            excludes.add("META-INF/MANIFEST.MF")

            pickFirsts.add("META-INF/io.netty.versions.properties")
            pickFirsts.add("META-INF/jersey-module-version")
            pickFirsts.add("META-INF/services/javax.annotation.processing.Processor")
        }
    }

    lint {
        disable.add("DuplicateClassesCheck")
    }
}

// Глобальное исключение конфликтующей библиотеки
configurations.all {
    exclude(group = "org.conscrypt", module = "conscrypt-openjdk-uber")
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Google Cloud Text-to-Speech - с исключением конфликтов
    implementation("com.google.cloud:google-cloud-texttospeech:2.20.0") {
        exclude(group = "org.conscrypt", module = "conscrypt-openjdk-uber")
    }
    implementation("com.google.auth:google-auth-library-oauth2-http:1.16.0") {
        exclude(group = "org.conscrypt", module = "conscrypt-openjdk-uber")
    }

    // Используем только одну версию Conscrypt
    implementation("org.conscrypt:conscrypt-android:2.5.2")

    // 2GIS SDK
    implementation("ru.dgis.sdk:sdk-full:12.4.0")

    // Volley для сетевых запросов
    implementation("com.android.volley:volley:1.2.1")

    // GSON для работы с JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // CameraX
    val cameraxVersion = "1.2.3"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Google Cloud Vision API - с исключением конфликтов
    implementation("com.google.cloud:google-cloud-vision:3.7.0") {
        exclude(group = "org.conscrypt", module = "conscrypt-openjdk-uber")
    }
    implementation("com.google.api:gax-grpc:2.22.0") {
        exclude(group = "org.conscrypt", module = "conscrypt-openjdk-uber")
    }

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // gRPC - выбираем одну согласованную версию
    implementation("io.grpc:grpc-okhttp:1.53.0")
    implementation("io.grpc:grpc-android:1.53.0")
    implementation("io.grpc:grpc-protobuf:1.53.0") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    implementation("io.grpc:grpc-stub:1.53.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Glide для работы с изображениями
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // Permissions
    implementation("com.karumi:dexter:6.2.3")
}