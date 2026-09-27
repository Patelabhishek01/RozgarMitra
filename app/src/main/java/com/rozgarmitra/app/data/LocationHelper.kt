package com.rozgarmitra.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
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
            val hasFine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCoarse = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                return@withContext null
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            // Choose appropriate priority based on granted permission
            val priority = if (hasFine) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }

            var location: Location? = try {
                fusedClient.getCurrentLocation(priority, cts.token).await()
            } catch (e: Exception) {
                null
            }

            // Fallback to lastLocation if fresh location request returned null
            if (location == null) {
                location = try {
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
        if (lat == 0.0 && lng == 0.0) return "Location Not Available"
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

    fun getCoordinatesFromAddress(context: Context, locationName: String): Pair<Double, Double>? {
        if (locationName.isBlank()) return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocationName(locationName, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                Pair(addr.latitude, addr.longitude)
            } else null
        } catch (e: Exception) {
            null
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
