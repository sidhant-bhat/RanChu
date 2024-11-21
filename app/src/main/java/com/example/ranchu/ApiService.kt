import com.example.ranchu.PredictionResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

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
