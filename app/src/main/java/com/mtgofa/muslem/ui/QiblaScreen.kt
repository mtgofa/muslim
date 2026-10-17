package com.mtgofa.muslem.ui

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import com.mtgofa.muslem.data.Haptics
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.prayer.arabicDigits
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** The Kaaba, Makkah. */
private const val KAABA_LAT = 21.422487
private const val KAABA_LNG = 39.826206

/** Within this many degrees the phone counts as facing the Qibla. */
private const val ALIGNED_DEG = 4f

/** Initial great-circle bearing from the user to the Kaaba, degrees from true north. */
fun qiblaBearing(lat: Double, lng: Double): Float {
    val p1 = Math.toRadians(lat); val p2 = Math.toRadians(KAABA_LAT)
    val dl = Math.toRadians(KAABA_LNG - lng)
    val y = sin(dl) * cos(p2)
    val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
    return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
}

/** Signed difference a-b folded into (-180, 180]. */
private fun angleDiff(a: Float, b: Float): Float {
    var d = (a - b) % 360f
    if (d > 180f) d -= 360f
    if (d <= -180f) d += 360f
    return d
}

/**
 * Phone heading from true north, from the rotation-vector sensor (or accelerometer + compass),
 * smoothed. Sensors are only registered while the screen is shown.
 */
@Composable
private fun rememberHeading(): Pair<Float?, Int> {
    val ctx = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    // SensorManager accuracy: -1 = not reported yet, 0 unreliable … 3 high
    var accuracy by remember { mutableStateOf(-1) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val declination = GeomagneticField(Settings.lat.toFloat(), Settings.lng.toFloat(), 0f, System.currentTimeMillis()).declination
        val rot = FloatArray(9); val orient = FloatArray(3)
        val grav = FloatArray(3); val mag = FloatArray(3)
        var haveGrav = false; var haveMag = false
        fun publish(azimuthRad: Float) {
            val raw = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + declination + 360f) % 360f
            val prev = heading
            // low-pass that respects the wrap at 0/360
            heading = if (prev == null) raw else (prev + angleDiff(raw, prev) * 0.35f + 360f) % 360f
        }
        val setAccuracy: (Int) -> Unit = { accuracy = it }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rot, e.values)
                        SensorManager.getOrientation(rot, orient); publish(orient[0])
                    }
                    Sensor.TYPE_ACCELEROMETER -> { e.values.copyInto(grav, endIndex = 3); haveGrav = true }
                    Sensor.TYPE_MAGNETIC_FIELD -> { e.values.copyInto(mag, endIndex = 3); haveMag = true }
                }
                if (e.sensor.type != Sensor.TYPE_ROTATION_VECTOR && haveGrav && haveMag &&
                    SensorManager.getRotationMatrix(rot, null, grav, mag)
                ) { SensorManager.getOrientation(rot, orient); publish(orient[0]) }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD || sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    setAccuracy(accuracy)
                }
            }
        }
        val rv = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rv != null) sm.registerListener(listener, rv, SensorManager.SENSOR_DELAY_GAME)
        else {
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        }
        sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        onDispose { sm.unregisterListener(listener) }
    }
    return heading to accuracy
}

@Composable
fun QiblaScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val qibla = remember { qiblaBearing(Settings.lat, Settings.lng) }
    val (heading, accuracy) = rememberHeading()
    // low only once the sensor has said so; some phones never report accuracy at all
    val unreliable = accuracy in 0..SensorManager.SENSOR_STATUS_ACCURACY_LOW
    // where the Qibla is relative to the top of the phone: 0 = straight ahead
    val rel = heading?.let { angleDiff(qibla, it) }

    // The compass needs a few seconds after it starts before its direction can be trusted.
    // Until then the dial is dimmed and nothing is announced. It counts as settled once it has
    // run for 2.5 s, the heading has held within 4° for 2 s, and the sensor reports at least
    // medium accuracy (or never reports any). After 12 s we show it anyway, with a
    // calibration hint, rather than wait forever.
    var settled by remember { mutableStateOf(false) }
    var steadiness by remember { mutableStateOf(0f) }
    // the loop below outlives this composition: read live values, not the first ones
    val liveHeading by androidx.compose.runtime.rememberUpdatedState(heading)
    val liveAccuracy by androidx.compose.runtime.rememberUpdatedState(accuracy)
    val recent = remember { ArrayDeque<Float>() }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (!settled) {
            kotlinx.coroutines.delay(250)
            val h = liveHeading ?: continue
            recent.addLast(h); if (recent.size > 8) recent.removeFirst()
            val spread = recent.maxOf { abs(angleDiff(it, h)) }
            if (spread >= 4f) { recent.clear(); recent.addLast(h) }
            val goodSensor = liveAccuracy < 0 || liveAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
            val elapsed = System.currentTimeMillis() - start
            steadiness = (recent.size / 8f).coerceAtMost(if (goodSensor) 1f else 0.85f)
            if (elapsed > 2500 && recent.size == 8 && goodSensor) settled = true
            if (elapsed > 12_000) settled = true
        }
    }
    val aligned = settled && rel != null && abs(rel) <= ALIGNED_DEG

    // one firm buzz each time the phone comes onto the Qibla
    LaunchedEffect(aligned) { if (aligned) Haptics.pattern(ctx, 0, 70, 60, 70) }

    // the dial turns with the phone; unwrap so it never spins the long way round 0/360
    val unwrapped = remember { FloatArray(1) }
    if (heading != null) unwrapped[0] += angleDiff(-heading, unwrapped[0] % 360f)
    val dial by animateFloatAsState(unwrapped[0], spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), label = "dial")

    val glow by animateColorAsState(if (aligned) Color(0xFF3DDC84) else Color(0xFFE2C27A), label = "glow")

    val bg = Brush.verticalGradient(listOf(Color(0xFF0B3A2C), Color(0xFF14664F)))
    Column(Modifier.fillMaxSize().background(bg), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع", tint = Color.White) }
            Text("القبلة", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                heading == null || !settled -> "لحظات… جاري ضبط البوصلة"
                aligned -> "أنت في اتجاه القبلة"
                rel!! > 0 -> "أدر الهاتف يميناً"
                else -> "أدر الهاتف يساراً"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = if (aligned) Color(0xFF7CF2A8) else Color.White,
        )
        Text(
            "القبلة على ${arabicDigits(qibla.toInt())}° من الشمال · ${Settings.locationName}",
            style = MaterialTheme.typography.bodyMedium, color = Color(0xB3FFFFFF),
        )
        Spacer(Modifier.weight(1f))
        val measurer = rememberTextMeasurer()
        Box(Modifier.size(340.dp), contentAlignment = Alignment.Center) {
            val dim by animateFloatAsState(if (settled) 1f else 0.3f, label = "dim")
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = dim }) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2 - 18.dp.toPx()
                val gold = Color(0xFFE2C27A); val goldDeep = Color(0xFFB8893A)
                // soft halo, brighter when aligned
                drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), c, r * 1.25f), r * 1.25f, c)
                // fixed pointer at the top: where the phone is facing
                val pointer = Path().apply {
                    moveTo(c.x, c.y - r + 2.dp.toPx())
                    lineTo(c.x - 12.dp.toPx(), c.y - r - 16.dp.toPx())
                    lineTo(c.x + 12.dp.toPx(), c.y - r - 16.dp.toPx())
                    close()
                }
                // dial body
                drawCircle(Brush.sweepGradient(listOf(gold, goldDeep, gold, goldDeep, gold), c), r, c)
                drawCircle(Brush.radialGradient(listOf(Color(0xFF1C6E55), Color(0xFF0A2F24)), c, r), r - 7.dp.toPx(), c)
                drawCircle(gold.copy(alpha = 0.5f), r - 12.dp.toPx(), c, style = Stroke(1.dp.toPx()))
                rotate(dial, c) {
                    // ticks every 5 degrees, longer every 30
                    for (i in 0 until 72) {
                        val a = Math.toRadians(i * 5.0 - 90)
                        val long = i % 6 == 0
                        val r1 = r - (if (long) 30.dp else 22.dp).toPx()
                        val r2 = r - 15.dp.toPx()
                        drawLine(
                            if (long) gold else Color(0x66FFFFFF),
                            Offset(c.x + (r1 * cos(a)).toFloat(), c.y + (r1 * sin(a)).toFloat()),
                            Offset(c.x + (r2 * cos(a)).toFloat(), c.y + (r2 * sin(a)).toFloat()),
                            strokeWidth = (if (long) 2.dp else 1.dp).toPx(), cap = StrokeCap.Round,
                        )
                    }
                    // eight-point star rosette, faint
                    for (rot in floatArrayOf(0f, 45f)) rotate(rot, c) {
                        val k = r * 0.5f
                        drawRect(gold.copy(alpha = 0.12f), Offset(c.x - k, c.y - k), Size(k * 2, k * 2), style = Stroke(1.dp.toPx()))
                    }
                    // the four directions, kept upright
                    val labels = listOf("ش" to 0f, "ق" to 90f, "ج" to 180f, "غ" to 270f)
                    for ((t, deg) in labels) {
                        val a = Math.toRadians(deg - 90.0)
                        val rr = r - 48.dp.toPx()
                        val pos = Offset(c.x + (rr * cos(a)).toFloat(), c.y + (rr * sin(a)).toFloat())
                        val layout = measurer.measure(t, TextStyle(color = if (deg == 0f) Color(0xFFFF8A80) else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = Plex))
                        rotate(-dial, pos) {
                            drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
                        }
                    }
                    // a large arrow from the centre to the Kaaba, both turned toward the Qibla
                    rotate(qibla, c) {
                        val tip = c.y - r + 44.dp.toPx()
                        val tail = c.y + r * 0.42f
                        val headH = 46.dp.toPx(); val headW = 30.dp.toPx(); val shaft = 9.dp.toPx()
                        val arrow = Path().apply {
                            moveTo(c.x, tip)
                            lineTo(c.x + headW, tip + headH)
                            lineTo(c.x + shaft, tip + headH * 0.82f)
                            lineTo(c.x + shaft, tail)
                            lineTo(c.x - shaft, tail)
                            lineTo(c.x - shaft, tip + headH * 0.82f)
                            lineTo(c.x - headW, tip + headH)
                            close()
                        }
                        drawPath(arrow, Brush.verticalGradient(listOf(glow, goldDeep), tip, tail))
                        drawPath(arrow, Color(0x55000000), style = Stroke(1.dp.toPx()))
                        // a split highlight down the left half for some depth
                        val half = Path().apply {
                            moveTo(c.x, tip); lineTo(c.x - headW, tip + headH); lineTo(c.x - shaft, tip + headH * 0.82f)
                            lineTo(c.x - shaft, tail); lineTo(c.x, tail); close()
                        }
                        drawPath(half, Color(0x26FFFFFF))
                        kaaba(Offset(c.x, c.y - r - 4.dp.toPx()), 40.dp.toPx(), glow)
                    }
                }
                drawPath(pointer, glow)
            }
        }
        Spacer(Modifier.height(20.dp))
        if (!settled) {
            Text("جاري التأكد من الاتجاه…", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.LinearProgressIndicator(
                { steadiness }, Modifier.width(200.dp).height(6.dp).clip(CircleShape),
                color = Color(0xFFE2C27A), trackColor = Color(0x33FFFFFF),
            )
            Text(
                if (unreliable) "حرّك الهاتف في الهواء على شكل رقم 8 عدة مرات" else "ثبّت الهاتف مسطّحاً ولا تحرّكه لحظات",
                style = MaterialTheme.typography.bodySmall, color = Color(0xB3FFFFFF), modifier = Modifier.padding(top = 10.dp),
            )
        } else {
            if (rel != null) Text(
                if (aligned) "✓" else "${arabicDigits(abs(rel).toInt())}°",
                fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = Plex,
                color = if (aligned) Color(0xFF7CF2A8) else Color.White,
            )
            val (label, tint) = when {
                accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "دقة البوصلة: عالية" to Color(0xFF7CF2A8)
                accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "دقة البوصلة: متوسطة" to Color(0xFFF2D59B)
                unreliable -> "دقة البوصلة: منخفضة" to Color(0xFFFF9E80)
                else -> "البوصلة جاهزة" to Color(0xFF7CF2A8)
            }
            Text(
                label, style = MaterialTheme.typography.labelLarge, color = tint,
                modifier = Modifier.padding(top = 6.dp).clip(CircleShape).background(Color(0x26FFFFFF)).padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        if (unreliable && settled) Text(
            "البوصلة تحتاج معايرة: حرّك الهاتف على شكل رقم 8، وابتعد عن المعادن والمغناطيس.",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFFF2D59B), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
        )
        Text(
            "أدر الهاتف حتى تصل الكعبة إلى العلامة الذهبية في الأعلى.",
            style = MaterialTheme.typography.bodySmall, color = Color(0xB3FFFFFF), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 32.dp).padding(bottom = 20.dp),
        )
    }
}

/** The Kaaba in a slight 3/4 view: black cloth, gold band, door, centred at [top]+size/2. */
private fun DrawScope.kaaba(top: Offset, s: Float, halo: Color) {
    val c = Offset(top.x, top.y + s * 0.6f)
    drawCircle(halo.copy(alpha = 0.18f), s * 0.95f, c)
    val front = Rect(c.x - s * 0.42f, c.y - s * 0.3f, c.x + s * 0.18f, c.y + s * 0.4f)
    val side = Path().apply {
        moveTo(front.right, front.top); lineTo(front.right + s * 0.24f, front.top - s * 0.12f)
        lineTo(front.right + s * 0.24f, front.bottom - s * 0.12f); lineTo(front.right, front.bottom); close()
    }
    val roof = Path().apply {
        moveTo(front.left, front.top); lineTo(front.left + s * 0.24f, front.top - s * 0.12f)
        lineTo(front.right + s * 0.24f, front.top - s * 0.12f); lineTo(front.right, front.top); close()
    }
    drawRect(Color(0xFF151515), front.topLeft, front.size)
    drawPath(side, Color(0xFF2A2A2A)); drawPath(roof, Color(0xFF3A3A3A))
    val band = front.top + front.height * 0.24f
    drawRect(Color(0xFFD4A84A), Offset(front.left, band), Size(front.width, s * 0.07f))
    val sideBand = Path().apply {
        moveTo(front.right, band); lineTo(front.right + s * 0.24f, band - s * 0.12f)
        lineTo(front.right + s * 0.24f, band - s * 0.05f); lineTo(front.right, band + s * 0.07f); close()
    }
    drawPath(sideBand, Color(0xFFB88E34))
    drawRect(Color(0xFFD4A84A), Offset(front.right - s * 0.2f, front.bottom - s * 0.26f), Size(s * 0.11f, s * 0.22f))
}
