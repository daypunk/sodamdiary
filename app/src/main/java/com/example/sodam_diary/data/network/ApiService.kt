package com.example.sodam_diary.data.network

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
     * @param photo 촬영한 사진 파일
     * @param fileInfo 사용자가 입력한 설명 (STT로 변환된 텍스트, 선택사항)
     * @return 서버로부터 받은 이미지 설명
     */
    @Multipart
    @POST("test/pickerToText/")
    suspend fun analyzePhoto(
        @Part photo: MultipartBody.Part?,
        @Part("file_info") fileInfo: RequestBody?
    ): Response<ApiResponse>
}

/**
 * 서버 응답 데이터 클래스
 */
data class ApiResponse(
    val status: String,
    val message: String,
    val data: ResponseData?
)

/**
 * 응답 데이터 내부 구조
 */
data class ResponseData(
    val file_description: String
)