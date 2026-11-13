package com.gainforge.app.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gainforge.app.data.Profile

val Tnum = TextStyle(fontFeatureSettings = "tnum")

/** Soft blue light blobs behind everything, so translucent glass cards have something to sit on. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .drawBehind {
                fun blob(c: Color, x: Float, y: Float, r: Float) {
                    val center = Offset(size.width * x, size.height * y)
                    val rad = size.width * r
                    drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center, rad), rad, center)
                }
                blob(p.accent.copy(alpha = if (p.dark) .40f else .24f), .05f, .04f, 1.05f)
                blob(p.accent.copy(alpha = if (p.dark) .22f else .14f), 1f, .48f, .85f)
                blob(Color(0xFF64D2FF).copy(alpha = if (p.dark) .16f else .16f), .1f, 1f, .95f)
            },
        content = content
    )
}

@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(26.dp), highlighted: Boolean = false, solid: Boolean = false): Modifier {
    val p = LocalPalette.current
    val fill: Brush = if (solid) SolidColor(p.bar) else Brush.verticalGradient(listOf(p.glassTop, p.glassBottom))
    val edge = Brush.linearGradient(
        listOf(
            p.accent.copy(alpha = if (highlighted) .95f else .45f),
            p.border,
            p.accent.copy(alpha = if (highlighted) .6f else .16f)
        )
    )
    return this.clip(shape).background(fill).border(if (highlighted) 1.5.dp else 1.dp, edge, shape)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(highlighted = highlighted)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun ScreenColumn(state: LazyListState = rememberLazyListState(), content: LazyListScope.() -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        state = state,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 10.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

// ---------- Text ----------

@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, color = LocalPalette.current.text)

@Composable
fun Title(text: String, modifier: Modifier = Modifier, size: TextUnit = 19.sp) =
    Text(text, modifier, fontSize = size, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp, color = LocalPalette.current.text)

@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color? = null) =
    Text(text.uppercase(), modifier, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp, color = color ?: LocalPalette.current.subtext)

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color? = null, weight: FontWeight = FontWeight.Normal) =
    Text(text, modifier, fontSize = 15.sp, fontWeight = weight, color = color ?: LocalPalette.current.text, lineHeight = 21.sp)

@Composable
fun Sub(text: String, modifier: Modifier = Modifier, color: Color? = null) =
    Text(text, modifier, fontSize = 13.sp, color = color ?: LocalPalette.current.subtext, lineHeight = 18.sp)

// ---------- Buttons ----------

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, color: Color? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    val c = color ?: p.accent
    Row(
        modifier
            .height(52.dp)
            .clip(CircleShape)
            .background(if (enabled) Brush.verticalGradient(listOf(c.copy(alpha = .85f), c)) else SolidColor(p.field))
            .border(1.dp, Color.White.copy(alpha = if (enabled) .3f else 0f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) Color.White else p.subtext, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) Color.White else p.subtext, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    val c = color ?: p.accent
    Row(
        modifier
            .height(48.dp)
            .glass(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = c, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
fun RoundButton(text: String, filled: Color?, modifier: Modifier = Modifier, size: Dp = 78.dp, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(filled?.copy(alpha = .22f) ?: p.field)
            .border(1.5.dp, filled ?: p.divider, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = filled ?: p.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

// ---------- Selection ----------

@Composable
fun SelectChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val bg by animateColorAsState(if (selected) p.accent else p.field, label = "chip")
    Box(
        modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) p.accent else p.divider, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp)
    ) {
        Text(text, color = if (selected) Color.White else p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(options: List<String>, isSelected: (String) -> Boolean, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> SelectChip(o, isSelected(o)) { onToggle(o) } }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(p.field)
            .border(1.dp, p.divider, CircleShape)
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, s ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) p.accent else Color.Transparent, label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(s, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else p.text, maxLines = 1)
            }
        }
    }
}

@Composable
fun CheckDot(checked: Boolean, size: Dp = 26.dp, onClick: (() -> Unit)? = null) {
    val p = LocalPalette.current
    val bg by animateColorAsState(if (checked) p.accent else Color.Transparent, label = "check")
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.5.dp, if (checked) p.accent else p.subtext.copy(alpha = .5f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(size * 0.64f))
    }
}

@Composable
fun OptionCard(title: String, detail: String? = null, selected: Boolean, emoji: String? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(20.dp), highlighted = selected)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = p.text)
            if (detail != null) Text(detail, fontSize = 13.sp, color = p.subtext, lineHeight = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        CheckDot(selected)
    }
}

// ---------- Inputs ----------

@Composable
fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    center: Boolean = false
) {
    val p = LocalPalette.current
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(p.field)
            .border(1.dp, p.divider, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            val align = if (center) TextAlign.Center else TextAlign.Start
            if (value.isEmpty()) Text(placeholder, color = p.subtext.copy(alpha = .7f), fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = align)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(color = p.text, fontSize = 16.sp, textAlign = align),
                cursorBrush = SolidColor(p.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (suffix != null) {
            Spacer(Modifier.width(6.dp))
            Text(suffix, color = p.subtext, fontSize = 15.sp)
        }
    }
}

@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Body(label, Modifier.weight(1f))
        Box(Modifier.size(36.dp).clip(CircleShape).background(p.field).clickable(onClick = onMinus), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Remove, "Less", tint = p.accent, modifier = Modifier.size(18.dp))
        }
        Text(value, Modifier.widthIn(min = 72.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = p.text, style = Tnum)
        Box(Modifier.size(36.dp).clip(CircleShape).background(p.field).clickable(onClick = onPlus), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, "More", tint = p.accent, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------- Visuals ----------

@Composable
fun Ring(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 12.dp,
    color: Color = LocalPalette.current.accent,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val p = LocalPalette.current
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, label = "ring")
    val value = if (animate) animated else target
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val inset = s / 2
            val sz = Size(size.width - s, size.height - s)
            drawArc(p.field, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(s))
            drawArc(color, -90f, 360f * value, false, Offset(inset, inset), sz, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun Bar(progress: Float, modifier: Modifier = Modifier, color: Color = LocalPalette.current.accent, height: Dp = 8.dp) {
    val p = LocalPalette.current
    val v by animateFloatAsState(progress.coerceIn(0f, 1f), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(p.field)) {
        Box(Modifier.fillMaxWidth(v).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(color.copy(alpha = .7f), color))))
    }
}

@Composable
fun MacroBar(label: String, value: Int, target: Int, unit: String, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Body(label, Modifier.weight(1f), weight = FontWeight.Medium)
            Text("$value / $target $unit", fontSize = 14.sp, color = LocalPalette.current.subtext, style = Tnum)
        }
        Spacer(Modifier.height(6.dp))
        Bar(if (target > 0) value / target.toFloat() else 0f, color = color)
    }
}

@Composable
fun Tag(text: String, color: Color = LocalPalette.current.accent) {
    Box(Modifier.clip(CircleShape).background(color.copy(alpha = .15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    val p = LocalPalette.current
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(p.field).padding(14.dp)) {
        Caption(label)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = p.text, style = Tnum)
        if (sub != null) Sub(sub)
    }
}

@Composable
fun Avatar(profile: Profile, size: Dp, modifier: Modifier = Modifier) {
    val accent = Color(profile.accent)
    val bmp = remember(profile.photoPath) {
        profile.photoPath?.let { path -> runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = .45f))))
            .border(2.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .85f), accent)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(profile.emoji, fontSize = (size.value * 0.46f).sp)
    }
}
