package com.example.sodam_diary.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        Log.d("LocationHelper", "📍 위치 정보 요청 시작")
        if (!hasLocationPermission()) {
            Log.w("LocationHelper", "❌ 위치 권한 없음")
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
                        Log.d("LocationHelper", "🎯 GPS 위치 획득 - lat: ${location.latitude}, lng: ${location.longitude}")
                        // 위치를 가져온 후 주소도 함께 처리
                        CoroutineScope(Dispatchers.IO).launch {
                            val addressName = getAddressFromCoordinates(location.latitude, location.longitude)
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                locationName = addressName
                            )
                            Log.d("LocationHelper", "🏠 주소 변환 완료 - 주소: $addressName")
                            continuation.resume(locationData)
                        }
                    } else {
                        Log.w("LocationHelper", "❌ GPS 위치 정보 없음, lastLocation 시도")
                        // 폴백: 마지막으로 알려진 위치 사용
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (lastLoc != null) {
                                    Log.d("LocationHelper", "🛰️ lastLocation 사용 - lat: ${lastLoc.latitude}, lng: ${lastLoc.longitude}")
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val addressName = getAddressFromCoordinates(lastLoc.latitude, lastLoc.longitude)
                                        val locationData = LocationData(
                                            latitude = lastLoc.latitude,
                                            longitude = lastLoc.longitude,
                                            locationName = addressName
                                        )
                                        Log.d("LocationHelper", "🏠 주소 변환 완료(last) - 주소: $addressName")
                                        continuation.resume(locationData)
                                    }
                                } else {
                                    Log.w("LocationHelper", "❌ lastLocation 도 사용 불가")
                                    continuation.resume(null)
                                }
                            }
                            .addOnFailureListener { lastEx ->
                                Log.e("LocationHelper", "❌ lastLocation 획득 실패", lastEx)
                                continuation.resume(null)
                            }
                    }
                }.addOnFailureListener { exception ->
                    Log.e("LocationHelper", "❌ 위치 정보 획득 실패", exception)
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
                        val formatted = addresses.firstOrNull()?.let { buildDisplayName(it) }
                        continuation.resume(formatted)
                    }
                } else {
                    // API 26-32: 기존 방식을 백그라운드에서 실행
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        val formatted = addresses?.firstOrNull()?.let { buildDisplayName(it) }
                        continuation.resume(formatted)
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
     * 주소에서 표시용 문자열 구성: 한국이면 '시/도 + 구', 그 외는 '도시/행정구' 우선
     */
    private fun buildDisplayName(address: Address): String? {
        val countryCode = address.countryCode
        val adminArea = address.adminArea // 예: 서울특별시, 경기도
        val subAdmin = address.subAdminArea // 예: 수원시, district equivalents
        val locality = address.locality // 예: 서울, City
        val subLocality = address.subLocality // 예: 강남구, 동대문구

        return if (countryCode.equals("KR", ignoreCase = true)) {
            // 한국: 시/도 + 구 우선, 없으면 시/군/구, 마지막으로 adminArea만
            when {
                !adminArea.isNullOrBlank() && !subLocality.isNullOrBlank() -> "$adminArea $subLocality"
                !adminArea.isNullOrBlank() && !subAdmin.isNullOrBlank() -> "$adminArea $subAdmin"
                !locality.isNullOrBlank() && !subLocality.isNullOrBlank() -> "$locality $subLocality"
                !adminArea.isNullOrBlank() -> adminArea
                else -> locality ?: subAdmin ?: address.getAddressLine(0)
            }
        } else {
            // 해외: City(locality) > subAdminArea > adminArea 순
            when {
                !locality.isNullOrBlank() && !subLocality.isNullOrBlank() -> "$locality $subLocality"
                !locality.isNullOrBlank() -> locality
                !subAdmin.isNullOrBlank() -> subAdmin
                !adminArea.isNullOrBlank() -> adminArea
                else -> address.getAddressLine(0)
            }
        }
    }
    
    /**
     * 위치 권한 확인
     */
    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
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