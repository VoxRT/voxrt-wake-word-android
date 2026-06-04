package com.voxrt.sdk.wakeword

/**
 * A single wake-word detection event emitted by
 * [VoxrtWakeWordEngine.processPcm] when the rolling sigmoid score
 * crosses the configured threshold.
 *
 * @property frameIndex 0-based frame index since session start
 *     (or last `reset`). 1 frame = 10 ms at 16 kHz.
 * @property timestampSec Seconds since session start at the detection
 *     frame's start, derived as `frameIndex * 10 ms`.
 * @property score Sigmoid score in `[0, 1]`.
 */
data class WakeWordDetection(
    val frameIndex: Long,
    val timestampSec: Float,
    val score: Float,
)
