// ApiService.kt
package com.example.ranchu

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


// Make sure you have this interface defined
//interface ApiService {
//    @Multipart
//    @POST("upload") // Update this endpoint as per your API
//    suspend fun uploadFiles(
//        @Part files: List<MultipartBody.Part>
//    ): Response<ResponseBody>
//}

interface ApiService {
    @Multipart
    @POST("predict")
    suspend fun uploadFiles(
        @Part file_access: MultipartBody.Part,
        @Part network_traffic: MultipartBody.Part,
        @Part system_performance: MultipartBody.Part,
        @Part user_behavior: MultipartBody.Part
    ): Response<PredictionResponse>
}