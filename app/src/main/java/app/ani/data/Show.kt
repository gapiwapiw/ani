package app.ani.data

import androidx.compose.ui.graphics.Color

/** What the cards and the show page draw. Built from a [ShowGroup] plus optional [ShowMeta]. */
data class Show(
    val key: String,
    val title: String,
    val thumb: String,
    val hue: Float,
    val year: String = "",
    val rating: String = "",
    val desc: String = "",
    val none: Boolean = false,
) {
    fun bg(): Color = Color.hsl(hue, 0.14f, 0.21f)
    fun fg(): Color = Color.hsl(hue, 0.14f, 0.48f)
}

fun ShowGroup.toShow(meta: ShowMeta? = null): Show = Show(
    key = key,
    title = name,
    thumb = thumb,
    hue = (key.hashCode().mod(360)).toFloat(),
    year = meta?.year.orEmpty(),
    rating = meta?.rating.orEmpty(),
    desc = meta?.desc.orEmpty(),
    none = thumb.isBlank() && key.isBlank(),
)
