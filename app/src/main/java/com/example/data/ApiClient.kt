package com.example.data

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class PairRequest(
    val merchantCode: String,
    val deviceId: String,
    val deviceName: String
)

@JsonClass(generateAdapter = true)
data class PairResponse(
    val success: Boolean,
    val merchantId: String? = null,
    val merchantName: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class TransactionRequest(
    val merchantCode: String,
    val smsText: String,
    val senderHeader: String,
    val timestamp: String,
    val deviceId: String,
    val deviceName: String
)

@JsonClass(generateAdapter = true)
data class TransactionResponse(
    val success: Boolean,
    val transactionId: String? = null,
    val message: String? = null,
    val parsing: ParsingDetails? = null
)

@JsonClass(generateAdapter = true)
data class ParsingDetails(
    val isCredit: Boolean = false,
    val parsedAmount: String? = null,
    val parsedUtr: String? = null,
    val parsedSender: String? = null,
    val confidence: Double? = null,
    val reasoning: List<String>? = null
)

interface OpenPayService {
    @POST
    suspend fun pairDevice(
        @Url url: String,
        @Body request: PairRequest
    ): Response<PairResponse>

    @POST
    suspend fun forwardTransaction(
        @Url url: String,
        @Body request: TransactionRequest
    ): Response<TransactionResponse>
}

object ApiClient {
    private val moshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private fun getRetrofit(baseUrl: String): Retrofit {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun getService(baseUrl: String): OpenPayService {
        return getRetrofit(baseUrl).create(OpenPayService::class.java)
    }
}
