package com.rozgarmitra.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val addressName: String
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationData? = withContext(Dispatchers.IO) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            
            val location: Location? = try {
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            } catch (e: Exception) {
                try {
                    fusedClient.lastLocation.await()
                } catch (ex: Exception) {
                    null
                }
            }

            if (location != null) {
                val addressName = getAddressFromCoordinates(context, location.latitude, location.longitude)
                LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    addressName = addressName
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAddressFromCoordinates(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val locality = addr.locality ?: addr.subLocality ?: addr.subAdminArea ?: addr.adminArea
                val feature = addr.featureName ?: addr.thoroughfare
                when {
                    !locality.isNullOrBlank() && !feature.isNullOrBlank() && feature != locality -> "$feature, $locality"
                    !locality.isNullOrBlank() -> locality
                    else -> addr.getAddressLine(0) ?: "${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)}"
                }
            } else {
                "${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)}"
            }
        } catch (e: Exception) {
            "${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)}"
        }
    }

    fun calculateDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        if (lat1 == 0.0 && lng1 == 0.0) return 0.0
        if (lat2 == 0.0 && lng2 == 0.0) return 0.0
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return (results[0] / 1000.0)
    }
}
