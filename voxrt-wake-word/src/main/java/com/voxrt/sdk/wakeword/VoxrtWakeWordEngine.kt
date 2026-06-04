package com.voxrt.sdk.wakeword

import android.content.res.AssetManager
import java.io.Closeable
import java.io.IOException

/**
 * Idiomatic Kotlin wrapper around the wake-word native library.
 *
 * Lifecycle:
 *   - Construct with a `.vxrt v2` byte array (see [fromAssetBytes]
 *     for the common case).
 *   - Drive via [processPcm]; each call returns zero or more
 *     [WakeWordDetection] events.
 *   - Read [currentScore] any time for a continuous-signal UI.
 *   - Close via [close] (or auto via `use { ... }`) — releases the
 *     native handle. Calling any method after close is a no-op /
 *     returns null.
 *
 * Threading: not thread-safe. Drive from one capture thread; gate
 * UI updates by posting to the main looper as needed.
 */
class VoxrtWakeWordEngine private constructor(
    initialHandle: Long,
) : Closeable {

    @Volatile private var handle: Long = initialHandle

    init {
        require(handle != 0L) { "voxrt_wake_word_create returned 0 (model load failed)" }
    }

    /** Sigmoid-space threshold (0..1). Default 0.9 (v6 deploy
     *  operating point — precision 0.993 / recall 0.982 on test). */
    fun setThreshold(threshold: Float) {
        if (handle != 0L) VoxrtWakeWordNative.setThreshold(handle, threshold)
    }

    /** Cooldown after a detection, in 10 ms frames. Default 100 = 1 s. */
    fun setCooldownFrames(cooldownFrames: Int) {
        if (handle != 0L) VoxrtWakeWordNative.setCooldownFrames(handle, cooldownFrames)
    }

    /** Latest sigmoid score (0..1). 0.5 before any frame is emitted. */
    fun currentScore(): Float =
        if (handle == 0L) 0.5f else VoxrtWakeWordNative.currentScore(handle)

    /** Wipe FIFOs + pool + cooldown — equivalent to a fresh session. */
    fun reset() {
        if (handle != 0L) VoxrtWakeWordNative.reset(handle)
    }

    /** Push i16 PCM (mono, 16 kHz). Returns any threshold-crossing events. */
    fun processPcm(pcm: ShortArray): List<WakeWordDetection> {
        if (handle == 0L) return emptyList()
        val flat = VoxrtWakeWordNative.pushPcmI16(handle, pcm) ?: return emptyList()
        return decode(flat)
    }

    /** Push f32 PCM (mono, 16 kHz, range [-1, 1]). */
    fun processPcm(pcm: FloatArray): List<WakeWordDetection> {
        if (handle == 0L) return emptyList()
        val flat = VoxrtWakeWordNative.pushPcmF32(handle, pcm) ?: return emptyList()
        return decode(flat)
    }

    override fun close() {
        val h = handle
        if (h != 0L) {
            handle = 0L
            VoxrtWakeWordNative.destroy(h)
        }
    }

    private fun decode(flat: FloatArray): List<WakeWordDetection> {
        if (flat.isEmpty()) return emptyList()
        require(flat.size % 3 == 0) { "malformed detection array, size=${flat.size}" }
        val n = flat.size / 3
        val out = ArrayList<WakeWordDetection>(n)
        for (i in 0 until n) {
            val base = i * 3
            out.add(
                WakeWordDetection(
                    frameIndex = flat[base].toLong(),
                    timestampSec = flat[base + 1],
                    score = flat[base + 2],
                )
            )
        }
        return out
    }

    companion object {
        /** Load a `.vxrt` model bundled as an APK asset. */
        @Throws(IOException::class)
        fun fromAssetBytes(assets: AssetManager, assetName: String): VoxrtWakeWordEngine {
            val bytes = assets.open(assetName).use { it.readBytes() }
            val handle = VoxrtWakeWordNative.create(bytes)
            if (handle == 0L) {
                throw IOException("voxrt_wake_word_create failed for asset '$assetName'")
            }
            return VoxrtWakeWordEngine(handle)
        }

        /** Load from raw bytes already in memory. */
        fun fromBytes(bytes: ByteArray): VoxrtWakeWordEngine {
            val handle = VoxrtWakeWordNative.create(bytes)
            require(handle != 0L) { "voxrt_wake_word_create failed" }
            return VoxrtWakeWordEngine(handle)
        }

        /** NUL-stripped SDK version string (`CARGO_PKG_VERSION`). */
        fun nativeVersion(): String = VoxrtWakeWordNative.voxrtWakeWordVersion() ?: "unknown"
    }
}
