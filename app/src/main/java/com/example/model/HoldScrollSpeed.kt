package com.example.model

/**
 * Speed configuration for hold-to-scroll gesture in vertical continuous reading.
 */
enum class HoldScrollSpeed(val displayName: String, val speedFactor: Float) {
    SLOW("Slow", 0.5f),
    NORMAL("Normal", 1.0f),
    FAST("Fast", 2.0f);

    companion object {
        fun fromString(value: String?): HoldScrollSpeed {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
        }
    }
}
