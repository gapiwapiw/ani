package app.ani.ui

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Colors from the design spec. Accent only for the Watch button, progress and active states. */
object C {
    val Bg = Color(0xFF151413)
    val S1 = Color(0xFF1C1B19)
    val S2 = Color(0xFF242220)
    val S3 = Color(0xFF2E2C29)
    val Ln = Color(0x14FFF5EB)
    val Tx = Color(0xFFE6E1DA)
    val Mut = Color(0xFF9A948B)
    val Ac = Color(0xFFB34A43)
    val Ac2 = Color(0xFFD67A70)
    val OnAc = Color(0xFFFFF3EE)
    val Logo = Color(0xFFC0524A)
}

object Shp {
    val R1 = RoundedCornerShape(6.dp)
    val R2 = RoundedCornerShape(12.dp)
    val R3 = RoundedCornerShape(20.dp)
}

object Motion {
    const val D1 = 120
    const val D2 = 200
    const val D3 = 300
    val Ease = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
}

val LocalReducedMotion = compositionLocalOf { false }

@Composable
fun <T> motion(ms: Int): FiniteAnimationSpec<T> {
    val reduced = LocalReducedMotion.current
    return tween(durationMillis = if (reduced) 1 else ms, easing = Motion.Ease)
}

@Composable
fun AniTheme(content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val reduced = remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(LocalReducedMotion provides reduced, content = content)
}
