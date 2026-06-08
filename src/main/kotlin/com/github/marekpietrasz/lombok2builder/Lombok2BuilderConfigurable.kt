package com.github.marekpietrasz.lombok2builder

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

/** Settings UI under Settings → Tools → Lombok To Builder. */
class Lombok2BuilderConfigurable : BoundConfigurable("Lombok To Builder") {

    override fun createPanel(): DialogPanel {
        val settings = Lombok2BuilderSettings.getInstance()
        return panel {
            row {
                checkBox("Generate each builder call on a new line")
                    .bindSelected(settings::multiline)
            }
            row {
                comment("When disabled, the whole builder chain is generated on a single line.")
            }
            row {
                checkBox("Skip setting null values")
                    .bindSelected(settings::skipNullValues)
            }
            row {
                comment("Drops <code>.x(null)</code> calls from the generated chain.")
            }
            row("Minimum values to convert:") {
                intTextField(range = 0..999)
                    .bindIntText(settings::minValues)
            }
            row {
                comment(
                    "Constructor calls with fewer than this many (non-null) arguments are not " +
                        "converted. Setter blocks are always converted, regardless of this value.",
                )
            }
        }
    }
}
