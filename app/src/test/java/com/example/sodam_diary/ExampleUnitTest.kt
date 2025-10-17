 package com.example.sodam_diary

 import com.example.sodam_diary.data.network.NetworkClient
 import kotlinx.coroutines.runBlocking
 import okhttp3.MediaType.Companion.toMediaTypeOrNull
 import okhttp3.RequestBody.Companion.toRequestBody
 import org.junit.Test

 import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
 class ExampleUnitTest {
     @Test
     fun testApi() = runBlocking {
         try {
             val fileInfo = "기분이 좋다".toRequestBody("text/plain".toMediaTypeOrNull())

             val response = NetworkClient.apiService.analyzePhoto(
                 file = null,
                 fileInfo = fileInfo
             )

             if (response.isSuccessful) {
                 println("status: ${response.body()?.status}")
                 println("message: ${response.body()?.message}")
                 println("성공: ${response.body()?.data?.refined_caption}")
             } else {
                 println("실패: ${response.code()}")
             }
         } catch (e: Exception) {
             println("예외(서버 닫힌 경우?): ${e.message}")
         }
     }
 }