# FocusFilter — ProGuard rules

# ── Our own code ──────────────────────────────────────────────────────────────
-keep class com.focusfilter.** { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# ── ONNX Runtime (on-device AI inference) ────────────────────────────────────
# Keep all native bindings, JNI entry points, and reflection targets
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# ── Kotlin ────────────────────────────────────────────────────────────────────
-dontwarn kotlin.**
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# ── Kotlin coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
