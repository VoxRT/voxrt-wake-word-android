package com.voxrt.sdk.wakeword

/**
 * 1:1 JNI facade for `libvoxrt_wake_word.so` — every method here maps
 * directly to a `Java_com_voxrt_sdk_wakeword_VoxrtWakeWordNative_*`
 * symbol in `crates/voxrt-sdk/voxrt-wake-word/src/lib.rs::jni_android`.
 *
 * This object is the low-level binding; consumers should use
 * [VoxrtWakeWordEngine] for the idiomatic API (RAII lifecycle, mic
 * helpers, typed detections).
 */
object VoxrtWakeWordNative {
    init {
        System.loadLibrary("voxrt_wake_word")
    }

    external fun voxrtWakeWordVersion(): String?

    /** Build a session. Returns the opaque handle or 0 on error. */
    external fun create(modelBytes: ByteArray): Long

    /** Release a session. Idempotent on 0. */
    external fun destroy(handle: Long)

    /** Wipe accumulated state (FIFOs, pre-emph carry, rolling pool). */
    external fun reset(handle: Long): Int

    /** Sigmoid-space threshold in `[0, 1]`. */
    external fun setThreshold(handle: Long, threshold: Float): Int

    /** Cooldown after a detection, in 10 ms frames. */
    external fun setCooldownFrames(handle: Long, cooldownFrames: Int): Int

    /** Latest sigmoid score in [0, 1]; 0.5 before any frame emitted. */
    external fun currentScore(handle: Long): Float

    /**
     * Push i16 PCM. Returns a triple-packed `FloatArray` of detections:
     *   `[frame_index_f32, timestamp_sec, score, ...]`
     * Empty array if no detection; null on error.
     */
    external fun pushPcmI16(handle: Long, pcm: ShortArray): FloatArray?

    /** Same as [pushPcmI16] for f32 input. */
    external fun pushPcmF32(handle: Long, pcm: FloatArray): FloatArray?

    /**
     * Pin the *calling* thread to a CPU cluster. Native side discovers
     * cluster boundaries at runtime via sysfs — no SoC hardcoding.
     * Modes:
     *   0 = AUTO       — scheduler default (clear any prior pinning)
     *   1 = HIGH_PERF  — pin to the highest-frequency cluster
     *                    (e.g. A73 / X-class). Maximises throughput.
     *   2 = LOW_POWER  — pin to the lowest-frequency cluster
     *                    (A53 / A520). Trades throughput for battery.
     *
     * Must be called from the thread that should be pinned —
     * `sched_setaffinity(pid=0)` affects the caller's tid only.
     * Returns `true` on success, `false` on any failure (the thread
     * keeps running unpinned).
     */
    external fun setCurrentThreadAffinity(mode: Int): Boolean
}
