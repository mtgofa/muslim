package com.mtgofa.muslem.ui

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
private fun rememberHeading(): Pair<Float?, Boolean> {
    val ctx = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    var unreliable by remember { mutableStateOf(false) }
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
            heading = if (prev == null) raw else (prev + angleDiff(raw, prev) * 0.15f + 360f) % 360f
        }
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
                    unreliable = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }
        val rv = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rv != null) sm.registerListener(listener, rv, SensorManager.SENSOR_DELAY_UI)
        else {
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }
        sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        onDispose { sm.unregisterListener(listener) }
    }
    return heading to unreliable
}

@Composable
fun QiblaScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val qibla = remember { qiblaBearing(Settings.lat, Settings.lng) }
    val (heading, unreliable) = rememberHeading()
    // where the Qibla is relative to the top of the phone: 0 = straight ahead
    val rel = heading?.let { angleDiff(qibla, it) }
    val aligned = rel != null && abs(rel) <= ALIGNED_DEG

    // one buzz each time the phone comes onto the Qibla
    LaunchedEffect(aligned) {
        if (aligned) {
            ctx.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    val ring by animateColorAsState(if (aligned) Color(0xFF1DB954) else Color(0xFFDDE5E0), label = "ring")
    val face by animateColorAsState(if (aligned) Color(0xFFE6F8EC) else Color.White, label = "face")
    val arrow by animateColorAsState(if (aligned) Color(0xFF1DB954) else Palette.Emerald, label = "arrow")
    var arrowAngle by remember { mutableFloatStateOf(0f) }
    if (rel != null) arrowAngle = rel

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TopBar("القبلة", onBack)
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                heading == null -> "جاري تشغيل البوصلة…"
                aligned -> "أنت في اتجاه القبلة"
                rel!! > 0 -> "أدر الهاتف يميناً"
                else -> "أدر الهاتف يساراً"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = if (aligned) Color(0xFF138A3F) else Palette.Ink,
        )
        Text(
            "القبلة على ${arabicDigits(qibla.toInt())}° من الشمال · ${Settings.locationName}",
            style = MaterialTheme.typography.bodyMedium, color = Palette.Muted,
        )
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2 - 8.dp.toPx()
                drawCircle(face, radius = r, center = c)
                drawCircle(ring, radius = r, center = c, style = Stroke(10.dp.toPx()))
                // a fixed mark at the top: the direction the phone points
                val top = Path().apply {
                    moveTo(c.x, c.y - r - 2.dp.toPx())
                    lineTo(c.x - 10.dp.toPx(), c.y - r - 20.dp.toPx())
                    lineTo(c.x + 10.dp.toPx(), c.y - r - 20.dp.toPx())
                    close()
                }
                drawPath(top, ring)
                // the arrow toward the Qibla, with the Kaaba at its tip
                rotate(arrowAngle, c) {
                    val tip = Offset(c.x, c.y - r * 0.62f)
                    drawLine(arrow, c, tip, strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
                    val head = Path().apply {
                        moveTo(c.x, tip.y - 18.dp.toPx())
                        lineTo(c.x - 16.dp.toPx(), tip.y + 6.dp.toPx())
                        lineTo(c.x + 16.dp.toPx(), tip.y + 6.dp.toPx())
                        close()
                    }
                    drawPath(head, arrow)
                    val k = 26.dp.toPx()
                    val kTop = Offset(c.x - k / 2, c.y - r * 0.9f - k / 2)
                    drawRect(Color(0xFF1C1C1C), topLeft = kTop, size = Size(k, k))
                    drawRect(Color(0xFFC49A45), topLeft = Offset(kTop.x, kTop.y + k * 0.28f), size = Size(k, k * 0.12f))
                }
                drawCircle(arrow, radius = 9.dp.toPx(), center = c)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (rel != null) Text(
            if (aligned) "✓" else "${arabicDigits(abs(rel).toInt())}°",
            fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = Plex,
            color = if (aligned) Color(0xFF138A3F) else Palette.Ink,
        )
        Spacer(Modifier.weight(1f))
        if (unreliable) Text(
            "البوصلة تحتاج معايرة: حرّك الهاتف على شكل رقم ٨ في الهواء، وابتعد عن المعادن والمغناطيس.",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFF9A6B12), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
        )
        Text(
            "ضع الهاتف مسطّحاً، ثم أدره حتى يصبح السهم للأعلى.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 28.dp),
        )
    }
}
