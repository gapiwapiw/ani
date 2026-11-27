package app.ani.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ani.data.Show
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/** Text with tabular numerals. Sizes 12/14/16/18/22/28, weights 400/500/600. */
@Composable
fun AText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 14,
    weight: Int = 400,
    color: Color = C.Tx,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Start,
    lh: Float = 1.45f,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = FontWeight(weight),
            lineHeight = (size * lh).sp,
            textAlign = align,
            fontFeatureSettings = "tnum",
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Press feedback is a quick opacity change. No ripple, no scale. */
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val a by animateFloatAsState(if (pressed) 0.8f else 1f, motion(Motion.D1), label = "press")
    this
        .alpha(a)
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

fun Modifier.dashedBorder(radius: Dp = 12.dp): Modifier = drawBehind {
    drawRoundRect(
        color = C.S3,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))),
    )
}

@Composable
fun AIcon(icon: ImageVector, tint: Color = C.Tx, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun Hair(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(C.Ln))
}

@Composable
fun Poster(s: Show, modifier: Modifier = Modifier, small: Boolean = false) {
    val shape = RoundedCornerShape(if (small) 6.dp else 12.dp)
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .clip(shape)
            .background(if (s.none) C.S2 else s.bg())
            .then(if (s.none) Modifier.dashedBorder(if (small) 6.dp else 12.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (s.none) {
            AIcon(AniIcons.Film, C.Mut, 28.dp)
        } else {
            AText(s.title.take(1), size = if (small) 18 else 28, weight = 500, color = s.fg())
            if (s.thumb.isNotBlank()) {
                AsyncImage(
                    model = s.thumb, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun PBar(frac: Float, modifier: Modifier = Modifier, fill: Color = C.Ac2) {
    Box(modifier.height(2.dp).clip(RoundedCornerShape(1.dp)).background(C.S3)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f)).background(fill))
    }
}

@Composable
fun ShowCard(s: Show, sub: String, progress: Float?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.tap(onClick = onClick)) {
        Poster(s, Modifier.fillMaxWidth())
        if (progress != null) PBar(progress, Modifier.fillMaxWidth().padding(top = 8.dp))
        AText(s.title, Modifier.padding(top = 8.dp), maxLines = 2, lh = 1.3f)
        AText(sub, Modifier.padding(top = 2.dp), size = 12, color = C.Mut)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AText(title, weight = 500, color = C.Mut)
        if (action != null) AText(action, Modifier.tap(onClick = onAction), size = 12, color = C.Ac2)
    }
}

@Composable
fun Chip(text: String, on: Boolean = false, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) C.S3 else C.S1, motion(Motion.D2), label = "chip")
    Box(
        Modifier.clip(CircleShape).background(bg).tap(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) { AText(text) }
}

/** Sub/Dub style toggle. Pills only here and for chips. */
@Composable
fun Seg(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(CircleShape).background(C.S1).padding(3.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            val bg by animateColorAsState(if (on) C.S3 else Color.Transparent, motion(Motion.D2), label = "seg")
            val fg by animateColorAsState(if (on) C.Tx else C.Mut, motion(Motion.D2), label = "segfg")
            Box(
                Modifier.clip(CircleShape).background(bg).tap { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) { AText(o, weight = 500, color = fg) }
        }
    }
}

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ghost: Boolean = false,
    icon: ImageVector? = null,
) {
    Row(
        modifier.height(44.dp).clip(Shp.R2).background(if (ghost) C.S2 else C.Ac)
            .tap(onClick = onClick).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) AIcon(icon, if (ghost) C.Tx else C.OnAc, 20.dp)
        AText(text, size = 16, weight = 600, color = if (ghost) C.Tx else C.OnAc, maxLines = 1)
    }
}

@Composable
fun IconBtn(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sq: Boolean = false,
    tint: Color = C.Tx,
    bg: Color = Color.Transparent,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier.size(44.dp).clip(Shp.R2)
            .background(if (sq) C.S2 else bg)
            .tap(onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) { AIcon(icon, tint, iconSize) }
}

@Composable
fun Empty(title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 56.dp)) {
        AText(title, size = 16, weight = 500)
        AText(text, Modifier.padding(top = 4.dp), color = C.Mut)
    }
}

enum class DlState { Idle, Run, Paused, Done }

@Composable
fun Ring(state: DlState, pct: Int, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).tap(onClick = onClick)
            .semantics { contentDescription = "Download state: $state" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 2.dp.toPx()
            val tl = Offset(sw / 2, sw / 2)
            val sz = Size(size.width - sw, size.height - sw)
            drawArc(C.S3, 0f, 360f, false, tl, sz, style = Stroke(sw))
            if (state != DlState.Idle) {
                drawArc(
                    color = if (state == DlState.Run) C.Ac2 else C.Mut,
                    startAngle = -90f,
                    sweepAngle = 360f * pct / 100f,
                    useCenter = false,
                    topLeft = tl,
                    size = sz,
                    style = Stroke(sw, cap = StrokeCap.Round),
                )
            }
        }
        val ic = when (state) {
            DlState.Done -> AniIcons.Check
            DlState.Run -> AniIcons.Pause
            DlState.Paused -> AniIcons.Play
            DlState.Idle -> AniIcons.Download
        }
        AIcon(ic, C.Tx, 18.dp)
    }
}

/** Thin slider: 3dp track, 12dp thumb. Value is 0..1. */
@Composable
fun AniSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    track: Color = Color(0x40FFFFFF),
    onDone: () -> Unit = {},
) {
    var w by remember { mutableStateOf(0) }
    val change by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onDone)
    val v = value.coerceIn(0f, 1f)
    Canvas(
        modifier
            .height(32.dp)
            .onSizeChanged { w = it.width }
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    if (w > 0) change((o.x / w).coerceIn(0f, 1f))
                    done()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = { done() }) { c, _ ->
                    c.consume()
                    if (w > 0) change((c.position.x / w).coerceIn(0f, 1f))
                }
            },
    ) {
        val cy = size.height / 2
        val th = 3.dp.toPx()
        val r = CornerRadius(th / 2)
        drawRoundRect(track, Offset(0f, cy - th / 2), Size(size.width, th), r)
        drawRoundRect(C.Ac2, Offset(0f, cy - th / 2), Size(size.width * v, th), r)
        drawCircle(C.Tx, 6.dp.toPx(), Offset(size.width * v, cy))
    }
}

fun Modifier.plainClick(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

fun Modifier.hairBorder(shape: androidx.compose.ui.graphics.Shape): Modifier = border(1.dp, C.Ln, shape)
