package com.voxrt.sdk.wakeword

/**
 * CPU-cluster pinning for the wake-word inference thread. Mirrors the
 * silero demo's CpuAffinity. The native side discovers cluster
 * boundaries at runtime by reading each CPU's `cpuinfo_max_freq`
 * sysfs — never hardcoded to a specific SoC. On homogeneous chips
 * (all cores same freq) both pinning modes resolve to the full mask
 * and the call is a no-op.
 *
 * Apply via [applyToCurrentThread] from inside the thread that
 * should be pinned (`pthread_setaffinity_np` only affects the
 * calling tid).
 */
enum class CpuAffinity(val nativeMode: Int) {
    /** Scheduler picks freely. The safe default for always-on /
     *  battery-sensitive usage. */
    AUTO(0),

    /** Pin to the highest-frequency cluster (e.g. Cortex-A73 / X-class).
     *  Maximises sustained throughput at higher power cost. Good for
     *  perf measurements on big.LITTLE phones (SD662 etc.) — without
     *  it the kernel happily migrates the audio thread to a LITTLE
     *  core under sustained load, inflating RTF. */
    HIGH_PERF(1),

    /** Pin to the lowest-frequency cluster (e.g. A53 / A55 / A520).
     *  Trades throughput for battery life. */
    LOW_POWER(2);

    companion object {
        /** Apply this affinity to the *calling* thread. Returns `true`
         *  on success, `false` on any native-side failure (the call
         *  is a soft fail — the thread keeps running without
         *  pinning). */
        fun applyToCurrentThread(mode: CpuAffinity): Boolean {
            return VoxrtWakeWordNative.setCurrentThreadAffinity(mode.nativeMode)
        }
    }
}
