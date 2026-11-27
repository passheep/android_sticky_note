package com.passheep.sticky_note.data.remote.api

import com.passheep.sticky_note.data.network.ApiEnvelope
import com.passheep.sticky_note.data.remote.dto.DisplayResponse
import com.passheep.sticky_note.data.remote.dto.DisplayStructuredTextRequest
import com.passheep.sticky_note.data.remote.dto.DisplayTextRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface DisplayApiService {

    @POST("/open/v1/devices/{deviceId}/display/text")
    suspend fun pushText(
        @Path("deviceId") deviceId: String,
        @Body request: DisplayTextRequest,
    ): ApiEnvelope<DisplayResponse>

    @POST("/open/v1/devices/{deviceId}/display/structured-text")
    suspend fun pushStructuredText(
        @Path("deviceId") deviceId: String,
        @Body request: DisplayStructuredTextRequest,
    ): ApiEnvelope<DisplayResponse>

    @Multipart
    @POST("/open/v1/devices/{deviceId}/display/image")
    suspend fun pushImage(
        @Path("deviceId") deviceId: String,
        @Part images: List<MultipartBody.Part>,
        @Part("dither") dither: RequestBody? = null,
        @Part("pageId") pageId: RequestBody? = null,
        @Part("bitDepth") bitDepth: RequestBody? = null,
    ): ApiEnvelope<DisplayResponse>

    @DELETE("/open/v1/devices/{deviceId}/display/pages/{pageId}")
    suspend fun deletePage(
        @Path("deviceId") deviceId: String,
        @Path("pageId") pageId: String,
    ): ApiEnvelope<Unit>
}
