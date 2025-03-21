# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Добавьте эти правила в файл proguard-rules.pro

# Правила для Google Cloud API
-keep class com.google.api.** { *; }
-keep class com.google.cloud.** { *; }
-keep class io.grpc.** { *; }
-keep class com.google.auth.** { *; }

# Правила для Protocol Buffers
-keep class com.google.protobuf.** { *; }

# Правила для Conscrypt
-keep class org.conscrypt.** { *; }

# Правила для предотвращения ошибок с SSL/TLS
-keepnames class javax.net.ssl.SSLContext
-keepnames class javax.net.ssl.SSLSocketFactory
-keepnames class javax.net.ssl.SSLEngine
-keepnames class javax.net.ssl.TrustManager
-keepnames class javax.net.ssl.X509TrustManager
-keepnames class javax.net.ssl.X509KeyManager

# Правила для GSON (если используется)
-keep class com.google.gson.** { *; }