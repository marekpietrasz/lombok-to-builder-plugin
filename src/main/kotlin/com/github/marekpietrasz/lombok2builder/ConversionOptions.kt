package com.github.marekpietrasz.lombok2builder

/** Snapshot of the settings that influence how a builder chain is generated. */
data class ConversionOptions(
    val multiline: Boolean,
    val skipNullValues: Boolean,
    val minValues: Int,
) {
    companion object {
        fun fromSettings(): ConversionOptions {
            val settings = Lombok2BuilderSettings.getInstance()
            return ConversionOptions(
                multiline = settings.multiline,
                skipNullValues = settings.skipNullValues,
                minValues = settings.minValues,
            )
        }
    }
}
