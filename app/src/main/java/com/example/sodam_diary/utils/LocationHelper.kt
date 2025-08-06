package com.example.sodam_diary.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 위치 관련 유틸리티 클래스
 */
class LocationHelper(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    /**
     * 현재 위치 정보 가져오기
     */
    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) {
            return null
        }
        
        return try {
            suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()
                
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
                
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    object : CancellationToken() {
                        override fun onCanceledRequested(listener: OnTokenCanceledListener) =
                            cancellationTokenSource.token.onCanceledRequested(listener)
                        
                        override fun isCancellationRequested(): Boolean =
                            cancellationTokenSource.token.isCancellationRequested
                    }
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        // 위치를 먼저 반환하고, 주소는 별도로 비동기 처리
                        val locationData = LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            locationName = null // 주소는 별도 함수로 처리
                        )
                        continuation.resume(locationData)
                    } else {
                        continuation.resume(null)
                    }
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            }
        } catch (e: SecurityException) {
            null
        }
    }
    
    /**
     * 위치 정보에 주소 추가 (별도 호출)
     */
    suspend fun getLocationWithAddress(latitude: Double, longitude: Double): LocationData {
        val addressName = getAddressFromCoordinates(latitude, longitude)
        return LocationData(latitude, longitude, addressName)
    }
    
    /**
     * 좌표를 주소 텍스트로 변환 (비동기)
     */
    private suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            
            suspendCancellableCoroutine { continuation ->
                val geocoder = Geocoder(context, Locale.getDefault())
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    // API 33 이상: 새로운 비동기 방식
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.getAddressLine(0)
                        continuation.resume(address)
                    }
                } else {
                    // API 26-32: 기존 방식을 백그라운드에서 실행
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        val address = addresses?.firstOrNull()?.getAddressLine(0)
                        continuation.resume(address)
                    } catch (e: Exception) {
                        continuation.resume(null)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 위치 권한 확인
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * 위치 정보 데이터 클래스
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val locationName: String?
)