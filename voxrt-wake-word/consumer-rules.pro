# ProGuard / R8 rules consumed by apps that depend on voxrt-wake-word.
#
# Wake-word v0.1.0 uses RegisterNatives (ADR-0016 Tier B anti-RE)
# instead of name-based JNI exports. That changes the failure modes
# we have to defend against in `isMinifyEnabled = true` consumer
# builds:
#
#   1. R8 renames `com.voxrt.sdk.wakeword.VoxrtWakeWordNative` to
#      `com.voxrt.sdk.wakeword.a`. JNI_OnLoad inside libvoxrt_wake_word.so
#      then does `env.find_class("com/voxrt/sdk/wakeword/VoxrtWakeWordNative")`
#      and fails — every native call throws `UnsatisfiedLinkError`.
#
#   2. R8 renames an `external fun create(...)` to `external fun a(...)`.
#      RegisterNatives binds by exact method name + signature, so the
#      bind silently misses and the first native call throws.
#
#   3. R8 strips a `native <methods>` because the Kotlin bytecode marks
#      it as an instance method on the singleton receiver (Kotlin
#      `object`), which static-analysers can mistake for unreachable
#      code. Same outcome — silent failure.
#
# `VoxrtWakeWordNative` is a Kotlin `object`, so in JVM bytecode it is:
#
#   public final class com.voxrt.sdk.wakeword.VoxrtWakeWordNative {
#       public static final com.voxrt.sdk.wakeword.VoxrtWakeWordNative INSTANCE;
#       public final native long create(byte[]);
#       public final native int pushPcmI16(long, short[]);
#       ...etc
#   }
#
# We must keep the class name AND its members AND the INSTANCE field.

-keep class com.voxrt.sdk.wakeword.VoxrtWakeWordNative {
    public static ** INSTANCE;
    public static <fields>;
    public <methods>;
    native <methods>;
}

# Defence in depth: any class that has native methods should keep
# them under their original names so JNI resolution still works.
# Default Android proguard rules already include this, but pinning
# it here makes the contract explicit and survives a consumer who
# overrides the default rule set.
-keepclasseswithmembernames class * {
    native <methods>;
}

# `VoxrtWakeWordEngine` is the public entry point consumers touch.
# Keep its public API surface (fromBytes / fromAssetBytes / processPcm
# / reset / currentScore / setThreshold / setCooldownFrames / close /
# nativeVersion) so call sites still resolve after R8.
-keep class com.voxrt.sdk.wakeword.VoxrtWakeWordEngine {
    public *;
}

# CpuAffinity exposes static enum + helper. Keep the enum values so
# clients passing `CpuAffinity.AUTO` / `HIGH_PERF` / `LOW_POWER` still
# match what JNI sees.
-keep class com.voxrt.sdk.wakeword.CpuAffinity { *; }

# WakeWordDetection is a data class returned from processPcm. Keep
# its fields so consumers can read frameIndex / timestampSec / score
# off events at runtime.
-keep class com.voxrt.sdk.wakeword.WakeWordDetection { *; }
