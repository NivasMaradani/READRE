package com.example.model

/**
 * Action triggered by tapping the left or right side in paged reading mode.
 */
enum class TapAction(val displayName: String) {
    NEXT_PAGE("Next Page"),
    PREVIOUS_PAGE("Previous Page");

    companion object {
        fun fromString(value: String?, default: TapAction = NEXT_PAGE): TapAction {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: default
        }
    }
}
