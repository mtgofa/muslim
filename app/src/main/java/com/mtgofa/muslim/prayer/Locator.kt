package com.mtgofa.muslim.prayer

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** One-shot device location without Play Services. */
object Locator {

    @SuppressLint("MissingPermission")
    suspend fun current(ctx: Context): Location? {
        val lm = ctx.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        // a recent cached fix is good enough for prayer times (they change ~1 min per 15 km)
        val cached = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (cached != null && System.currentTimeMillis() - cached.time < 6 * 3600_000L) return cached
        for (p in providers.filter { it != LocationManager.PASSIVE_PROVIDER }.sortedBy { if (it == LocationManager.NETWORK_PROVIDER) 0 else 1 }) {
            val fix = withTimeoutOrNull(20_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val cancel = CancellationSignal()
                    cont.invokeOnCancellation { cancel.cancel() }
                    LocationManagerCompat.getCurrentLocation(lm, p, cancel, ContextCompat.getMainExecutor(ctx)) { cont.resume(it) }
                }
            }
            if (fix != null) return fix
        }
        return cached
    }

    /** Best-effort place name ("المعادي، القاهرة"); null when no geocoder is available. */
    @Suppress("DEPRECATION")
    suspend fun placeName(ctx: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            val a = Geocoder(ctx, Locale("ar")).getFromLocation(lat, lng, 1)?.firstOrNull() ?: return@runCatching null
            listOfNotNull(a.subLocality ?: a.locality ?: a.subAdminArea, a.adminArea)
                .distinct().joinToString("، ").ifBlank { null }
        }.getOrNull()
    }

    /** Nearest built-in city, used to name a GPS fix when the geocoder is unavailable. */
    fun nearestCity(lat: Double, lng: Double): City = Cities.all.minBy {
        val r = FloatArray(1); Location.distanceBetween(lat, lng, it.lat, it.lng, r); r[0]
    }
}
