package com.rozgarmitra.app.presentation.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rozgarmitra.app.data.LocationData
import com.rozgarmitra.app.data.LocationHelper
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocationSelectionDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val userLocation by RozgarRepository.userLocationData.collectAsState()
    val currentRadius by RozgarRepository.searchRadiusKm.collectAsState()
    
    var manualLocationText by remember { mutableStateOf(userLocation?.addressName ?: "") }
    var selectedRadius by remember { mutableStateOf(currentRadius) }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var isSettingsRequired by remember { mutableStateOf(false) }

    fun fetchGpsLocation() {
        isLocating = true
        locationError = null
        isSettingsRequired = false
        scope.launch {
            val locData = LocationHelper.getCurrentLocation(context)
            isLocating = false
            if (locData != null) {
                RozgarRepository.updateUserLocation(locData)
                manualLocationText = locData.addressName
            } else {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val isGpsOn = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                        locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
                if (!isGpsOn) {
                    locationError = "Location (GPS) is disabled on your device. Please turn on Location in settings."
                    isSettingsRequired = true
                } else {
                    locationError = "Could not detect GPS location. Make sure GPS signal is available and try again."
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsLocation()
        } else {
            locationError = "Location permission denied. You can enter location manually below or enable permissions in App Settings."
            isSettingsRequired = true
        }
    }

    fun requestLocation() {
        val finePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (finePerm == PackageManager.PERMISSION_GRANTED || coarsePerm == PackageManager.PERMISSION_GRANTED) {
            fetchGpsLocation()
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    AlertDialog(
        containerColor = DarkSurfaceElevated,
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AccentCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Location & Distance", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Set location and radius to filter nearby job opportunities.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Detect GPS Button
                OutlinedButton(
                    onClick = { requestLocation() },
                    enabled = !isLocating,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = PrimaryBlue),
                    border = BorderStroke(1.dp, PrimaryBlue)
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detecting GPS Location...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Current GPS Location", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                if (locationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(locationError!!, color = WarningRose, fontSize = 12.sp)
                    if (isSettingsRequired) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Location Settings", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual Location Fallback
                Text("Or enter locality / city manually", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualLocationText,
                    onValueChange = { manualLocationText = it },
                    placeholder = { Text("e.g. HSR Layout, Bengaluru", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderStrokeColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Radius Filter Options
                Text("Search Radius", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                val radiusOptions = listOf(
                    0.0 to "All Jobs",
                    5.0 to "5 km",
                    10.0 to "10 km",
                    25.0 to "25 km",
                    50.0 to "50 km"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    radiusOptions.forEach { (radiusVal, label) ->
                        val isSelected = selectedRadius == radiusVal
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRadius = radiusVal },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.25f),
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = BorderStrokeColor,
                                selectedBorderColor = PrimaryBlue
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    RozgarRepository.setSearchRadius(selectedRadius)
                    if (manualLocationText.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            val coords = LocationHelper.getCoordinatesFromAddress(context, manualLocationText.trim())
                            val lat = coords?.first ?: (userLocation?.latitude ?: 0.0)
                            val lng = coords?.second ?: (userLocation?.longitude ?: 0.0)
                            val updatedLoc = LocationData(
                                latitude = lat,
                                longitude = lng,
                                addressName = manualLocationText.trim()
                            )
                            withContext(Dispatchers.Main) {
                                RozgarRepository.updateUserLocation(updatedLoc)
                                RozgarRepository.recalculateJobDistances()
                            }
                        }
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply Filter", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
