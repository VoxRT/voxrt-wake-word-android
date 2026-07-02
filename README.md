# VoxrtWakeWord for Android

Always-on wake-phrase detection on the **VoxRT** custom on-device inference runtime. ~48K-parameter depthwise-separable convnet, 16 kHz mono in, sigmoid-score out + threshold-crossing events. Detects the phrase **"Hey Assistant"**.

- Current version: `v0.1.0`
- Minimum Android: API 26 (Android 8.0)
- ABIs shipped: `arm64-v8a` (NEON-accelerated), `x86_64` (scalar, emulator only)
- License: Apache-2.0 (Kotlin wrapper) · proprietary (compiled runtime, redistribution allowed via this artifact)
- Wake-phrase weights: proprietary in-house (synthetic training data; no upstream license obligations)

---

## What is VoxRT?

VoxRT is a from-scratch inference runtime for on-device speech models. No ONNX Runtime, no PyTorch Mobile, no LiteRT — a custom Rust core sized and tuned for streaming voice workloads on phone-class hardware.

`VoxrtWakeWord` is the wake-word product on that runtime, alongside [`VoxrtSilero`](https://github.com/VoxRT/voxrt-silero-android) (VAD) and [`VoxrtAsr`](https://github.com/VoxRT/voxrt-asr-android) (streaming ASR). All three share the same Rust runtime crate and the same NEON kernel set. The runtime is the product; the models are what it runs.

Custom-phrase wake-word models (your own brand name, multi-phrase detection, language extension) are part of the commercial VoxRT SDK tier. Contact help@voxrt.com.

## Model quality

Test split: 5,240 positive utterances + 6,416 hard-negative utterances (isolated "Hey", isolated "Assistant", competitor wake-words like "Hey Siri", phonetic neighbours, arbitrary speech, non-speech audio). All speakers disjoint from train + val.

- **ROC AUC: 0.9966**
- **Average precision (PR AUC): 0.9899**

| Threshold | Precision | Recall | F1     | FPR    | False positives on test |
| --------- | --------- | ------ | ------ | ------ | ----------------------- |
| 0.5       | 0.864     | 0.995  | 0.925  | 12.8 % | 822 / 6,416             |
| 0.85      | 0.957     | 0.987  | 0.972  | 3.7 %  | 234 / 6,416             |
| **0.9 (default)** | **0.993** | **0.982** | **0.987** | **0.5 %** | **34 / 6,416** |
| 0.95      | 0.997     | 0.769  | 0.868  | 0.2 %  | 12 / 6,416              |

The library ships with `threshold = 0.9` as the default operating point. Lower it via `setThreshold` if your application can tolerate more false positives in exchange for higher recall.

## Performance

Measured at ship time, `arm64-v8a` release builds, post-warmup, RTF = wall-time-per-frame ÷ frame audio duration (lower is better):

| Device                                       | SoC class      | Mode                         | RTF       |
| -------------------------------------------- | -------------- | ---------------------------- | --------- |
| Xiaomi Redmi 9C (SD 662, Cortex-A73)         | midrange-2020  | scheduler default            | **0.021** |
| Xiaomi Redmi 9C (SD 662, Cortex-A73)         | midrange-2020  | `CpuAffinity.HIGH_PERF` pin  | **0.021** |
| Xiaomi Redmi 9C (SD 662, Cortex-A53)         | midrange-2020  | LITTLE cluster (`LOW_POWER`) | 0.071     |

At RTF ≈ 0.02 the wake-word is ~50× faster than realtime on a 5-year-old midrange SoC — well within an always-on power budget. Even on the LITTLE cluster (Cortex-A53), RTF stays at 0.07 — wake-word survives a thermally-throttled phone gracefully. Pin the engine thread to the perf cluster (`CpuAffinity.HIGH_PERF`) on big.LITTLE chips to keep latency stable; the scheduler otherwise migrates the audio thread to a LITTLE core under sustained load.

## How it compares

The on-device wake-word category is dominated by Picovoice Porcupine on the paid side and openWakeWord on the OSS side:

| | **VoxrtWakeWord** | Picovoice Porcupine | openWakeWord |
|---|---|---|---|
| Model file | **~100 KB** (.vxrt) | not published | not published |
| Mobile RTF disclosed | ✅ measured on Snapdragon 662 + iPhone | ❌ Raspberry Pi 5 only (0.6 % CPU; ~1.8 % scaled to SD662) | ❌ Raspberry Pi 3 only |
| Accuracy headline | ROC AUC 0.9966 on "Hey Assistant"; precision 0.993 / recall 0.982 @ default threshold | 2.7 % miss rate averaged across 6 built-in keywords (alexa, computer, jarvis, smart mirror, snowboy, view glass) | varies per pretrained model |
| Native mobile SDK | ✅ Android JitPack + iOS SPM | ✅ Android + iOS + RN + Flutter | ❌ Python-only; community C++ port |
| License | Apache-2.0 wrapper + proprietary runtime + proprietary weights (redistribution allowed as an unmodified part of this SDK, no per-seat fees) | Commercial (Free Plan evaluation-only; production tier opaque, sales-gated) | Apache-2.0 code, **CC-BY-NC-SA** on pretrained weights (non-commercial) |
| Custom phrase / language | Tuned per customer on request (paid engagement) | Via Picovoice Console — paid tier required for commercial deployment | Self-train via Colab + TTS (~1 hour) |

On raw speed and accuracy we're near-tie with Porcupine (their 2.7 % miss rate is a real benchmark; our ~100 KB model is genuinely tiny). The clear differentiators are **license clarity** (no per-seat fees, commercial redistribution allowed as part of this SDK vs Picovoice opaque pricing vs openWakeWord NC-blocked weights), **measured mobile RTF** (no other vendor publishes one for cheap Android), and a **~100 KB** model file.

Full sourced analysis: [voxrt.com](https://voxrt.com).

## Binary footprint

- Kotlin wrapper source: ~6 KB total (4 files)
- `libvoxrt_wake_word.so` per ABI:
  - `arm64-v8a`: ~525 KB stripped
  - `x86_64`: ~593 KB stripped
- Wake-phrase model `voxrt_wake_word.vxrt`: ~100 KB fp16 (downloaded separately)

Net effect on a consuming Android app's APK: roughly 600 KB once the .so + .vxrt + Kotlin wrapper are bundled.

## Install

In `settings.gradle.kts`, add JitPack:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

In your app `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.VoxRT:voxrt-wake-word-android:v0.1.0")
}
```

## Get the wake-phrase model

The model weights are NOT bundled with the library — fetch them once from [`voxrt-wake-word-models`](https://github.com/VoxRT/voxrt-wake-word-models/releases/tag/v0.1.0):

```
https://github.com/VoxRT/voxrt-wake-word-models/releases/download/v0.1.0/voxrt_wake_word.vxrt
```

SHA-256: `9d40bdc132a2ad8e85bd8a28bb49b77c51a7c62f60567222a037e44418510e8f`

You decide where it lives. Two common patterns for an ~100 KB asset:

- **Bundle in app assets** — drop `voxrt_wake_word.vxrt` into `app/src/main/assets/` and load with `VoxrtWakeWordEngine.fromAssetBytes(context.assets, "voxrt_wake_word.vxrt")`. Smallest engineering overhead, works offline from first launch.
- **Download on first run** — fetch into `context.filesDir`, verify the SHA-256, then load with `VoxrtWakeWordEngine.fromBytes(...)`. Lets you swap models without an app update; requires `<uses-permission android:name="android.permission.INTERNET" />` in your manifest.

### Download-on-first-run snippet

```kotlin
private const val MODEL_URL =
    "https://github.com/VoxRT/voxrt-wake-word-models/releases/download/v0.1.0/voxrt_wake_word.vxrt"
private const val MODEL_SHA256 = "9d40bdc132a2ad8e85bd8a28bb49b77c51a7c62f60567222a037e44418510e8f"

fun ensureModel(ctx: Context): ByteArray {
    val cached = java.io.File(ctx.filesDir, "voxrt_wake_word.vxrt")
    if (cached.exists() && sha256(cached.readBytes()) == MODEL_SHA256) {
        return cached.readBytes()
    }
    val conn = (java.net.URL(MODEL_URL).openConnection() as java.net.HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = 15_000
        readTimeout = 60_000
    }
    val bytes = conn.inputStream.use { it.readBytes() }
    conn.disconnect()
    check(sha256(bytes) == MODEL_SHA256) { "model SHA-256 mismatch" }
    cached.writeBytes(bytes)
    return bytes
}

private fun sha256(b: ByteArray): String =
    java.security.MessageDigest.getInstance("SHA-256")
        .digest(b).joinToString("") { "%02x".format(it) }

// Then, off the main thread:
val bytes = ensureModel(context)
val engine = VoxrtWakeWordEngine.fromBytes(bytes)
```

## Quick start

```kotlin
import com.voxrt.sdk.wakeword.VoxrtWakeWordEngine

// 1. Construct the engine. `fromAssetBytes` loads the .vxrt off
//    the AssetManager (mmap-friendly under the hood).
val engine = VoxrtWakeWordEngine.fromAssetBytes(
    context.assets, "voxrt_wake_word.vxrt"
)

// 2. Feed Int16 PCM (mono, 16 kHz) blocks of any size — 100 ms
//    blocks are the recommended pace for AudioRecord callbacks.
//    `processPcm` returns any threshold-crossing detections that
//    occurred during this push; usually empty.
val detections = engine.processPcm(shortArrayOfPcm)
for (d in detections) {
    Log.i("wakeword", "frame=${d.frameIndex} t=${d.timestampSec} score=${d.score}")
}

// 3. When you're done.
engine.close()
```

`processPcm` / `reset` / `close` are **synchronous and stateful** — same shape as `VoxrtAsrStreamingEngine.processPcm` in the companion ASR library. The engine does NOT own a worker thread. You drive it from your own capture thread.

## Live microphone example

The canonical pattern — capture thread owns the `AudioRecord` loop, engine is just a stateful function. Run on a background thread; don't block the UI thread on `processPcm`.

```kotlin
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.voxrt.sdk.wakeword.VoxrtWakeWordEngine

class WakeWordCapture(private val context: Context) {
    private val engine = VoxrtWakeWordEngine.fromAssetBytes(
        context.assets, "voxrt_wake_word.vxrt"
    )

    private val sampleRate = 16_000
    private val blockSamples = 1_600   // 100 ms

    fun runUntilCancelled(onDetection: (Long, Float) -> Unit) {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, blockSamples * 2 * 4),
        )
        val buf = ShortArray(blockSamples)
        rec.startRecording()
        try {
            while (!Thread.currentThread().isInterrupted) {
                val n = rec.read(buf, 0, buf.size, AudioRecord.READ_BLOCKING)
                if (n <= 0) continue
                val block = if (n < blockSamples) buf.copyOf(n) else buf
                for (d in engine.processPcm(block)) {
                    onDetection(d.frameIndex, d.score)
                }
            }
        } finally {
            rec.stop()
            rec.release()
            engine.close()
        }
    }
}
```

> **Permission:** declare `<uses-permission android:name="android.permission.RECORD_AUDIO" />` in your app's `AndroidManifest.xml` and request the runtime permission before instantiating `AudioRecord`.

## Tuning

### Threshold

Default is 0.9 (the chosen operating point on test). Lower for higher recall, raise for stricter precision:

```kotlin
engine.setThreshold(0.85f)   // a bit more recall, ~5 % false-positive rate
engine.setThreshold(0.95f)   // a bit stricter, but loses ~20 % recall
```

### Cooldown

After a detection, the engine suppresses further events for `cooldownFrames` × 10 ms. Default is 100 frames = 1 second — long enough that a single "Hey Assistant" utterance never triggers twice.

```kotlin
engine.setCooldownFrames(200)   // 2 seconds
```

### CPU affinity (advanced)

big.LITTLE chips migrate the audio thread between performance and efficiency clusters under load. On a Snapdragon 662-class device this can swing RTF from 0.021 (A73 cluster) to 0.182 (A53 cluster). Pin the engine's worker thread to a specific cluster:

```kotlin
import com.voxrt.sdk.wakeword.CpuAffinity

// Call from the thread that will drive engine.processPcm — affinity
// applies only to the calling thread.
CpuAffinity.applyToCurrentThread(CpuAffinity.HIGH_PERF)
```

`AUTO` (default) lets the scheduler decide. `HIGH_PERF` pins to the cluster with the highest reported max frequency. `LOW_POWER` pins to the LITTLE cluster (useful for measuring worst-case behaviour).

## API

### `VoxrtWakeWordEngine`

| Method                                              | Returns                       | Purpose |
| --------------------------------------------------- | ----------------------------- | ------- |
| `fromAssetBytes(assets, assetName)` (companion)     | `VoxrtWakeWordEngine`         | Load model from `AssetManager`. |
| `fromBytes(bytes)` (companion)                      | `VoxrtWakeWordEngine`         | Load model from a `ByteArray`. |
| `nativeVersion()` (companion)                       | `String`                      | SDK version baked into the .so. |
| `processPcm(pcm: ShortArray)`                       | `List<WakeWordDetection>`     | Push i16 PCM, get any threshold-crossings emitted during this push. |
| `processPcm(pcm: FloatArray)`                       | `List<WakeWordDetection>`     | Same, for f32 PCM in `[-1, 1]`. |
| `currentScore(): Float`                             | `Float`                       | Latest sigmoid score (0..1); doesn't require a fresh `processPcm`. |
| `reset()`                                           | `Unit`                        | Wipe accumulated state (FIFOs, rolling pool, cooldown, frame counter). |
| `setThreshold(threshold: Float)`                    | `Unit`                        | Sigmoid-space detection threshold (0..1). |
| `setCooldownFrames(cooldownFrames: Int)`            | `Unit`                        | Post-detection cooldown, in 10 ms frames. |
| `close()` (or `use { ... }`)                        | `Unit`                        | Release native handle. |

### `WakeWordDetection`

```kotlin
data class WakeWordDetection(
    val frameIndex: Long,    // 0-based frame index (1 frame = 10 ms)
    val timestampSec: Float, // seconds since engine start (or last reset)
    val score: Float,        // sigmoid score in [0, 1]
)
```

### `CpuAffinity`

```kotlin
enum class CpuAffinity { AUTO, HIGH_PERF, LOW_POWER }

object CpuAffinity {
    fun applyToCurrentThread(mode: CpuAffinity): Boolean
}
```

## License

- **Kotlin wrapper source** (this Gradle module): Apache-2.0. See [`LICENSE`](LICENSE).
- **Compiled runtime** (`libvoxrt_wake_word.so`): proprietary, redistributable under the terms in [`LICENSE-BINARY`](LICENSE-BINARY).
- **Wake-phrase model** (`voxrt_wake_word.vxrt`): proprietary, distributed separately under the [`voxrt-wake-word-models`](https://github.com/VoxRT/voxrt-wake-word-models) license terms.

For commercial integration, custom phrase models, or licensing terms beyond redistribution of the unmodified library, contact help@voxrt.com.
