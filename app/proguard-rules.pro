# Repackage classes into the default package to reduce the size of descriptors.
# Excluded from repackaging: classes looked up by name at runtime (Room _Impl, SQLCipher bridge).
-repackageclasses
-keeppackagenames com.jegly.pages.core.database
-keeppackagenames net.zetetic.database.sqlcipher

# ── Room ────────────────────────────────────────────────────────────────────
# Room locates the generated _Impl via Class.forName(dbClass.name + "_Impl").
# Keep all RoomDatabase subclasses (PagesDatabase and the generated _Impl).
-keep class * extends androidx.room.RoomDatabase { *; }
# Keep entity and DAO classes; Room reads field names via generated code but
# obfuscated member names would break any string-based column resolution.
-keep @androidx.room.Entity class ** { *; }
-keep @androidx.room.Dao interface ** { *; }
# Enum stored in BookEntity without an explicit @TypeConverter — Room generates
# code that calls BookFormat.valueOf(name). Keep all enum constants by name.
-keepclassmembers enum com.jegly.pages.core.model.** { *; }

# ── SQLCipher ────────────────────────────────────────────────────────────────
# SupportOpenHelperFactory bridges to the native libsqlcipher.so via JNI;
# its internal method names must survive obfuscation.
-keep class net.zetetic.database.** { *; }

# ── SQLCipher optional TLS provider warnings ─────────────────────────────────
# SQLCipher references BouncyCastle/Conscrypt/OpenJSSE at class-load time but
# none of them are bundled in the APK; suppress the resulting R8 warnings.
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# ── sherpa-onnx (Piper VITS neural TTS) ─────────────────────────────────────
# libsherpa-onnx-jni.so resolves the JNI bridge by the exact class and method
# names compiled into the .so. R8 must not rename or strip any of them.
-keep class com.k2fsa.sherpa.onnx.** { *; }

# ── kotlinx.serialization (Navigation3 nav keys) ─────────────────────────────
# The serialization plugin generates keep rules, but they can be lost when
# combined with -repackageclasses. Repeat the critical subset explicitly.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion *;
    static <1>$serializer *;
}
-if @kotlinx.serialization.Serializable class ** {
    static ** Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep all @Serializable nav key classes themselves (class name used by navigation3).
-keep @kotlinx.serialization.Serializable class ** { *; }
