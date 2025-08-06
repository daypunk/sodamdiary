package com.example.sodam_diary.data.network

import com.example.sodam_diary.data.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 서버와의 통신을 위한 API 인터페이스
 */
interface ApiService {
    
    /**
     * 사진과 사용자 설명을 서버에 전송하여 이미지 설명을 받음
     */
    @Multipart
    @POST("test/pickerToText/")
    suspend fun analyzePhoto(
        @Part photo: MultipartBody.Part?,
        @Part("file_info") fileInfo: RequestBody?
    ): Response<ApiResponse>
}