package com.passheep.sticky_note.data.remote.api

import com.passheep.sticky_note.data.network.ApiEnvelope
import com.passheep.sticky_note.data.remote.dto.DeviceDto
import com.passheep.sticky_note.data.remote.dto.DeviceSyncIntervalResponse
import com.passheep.sticky_note.data.remote.dto.UpdateDeviceSyncIntervalRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface DeviceApiService {

    @GET("/open/v1/devices")
    suspend fun getDevices(): ApiEnvelope<List<DeviceDto>>

    /**
     * 修改设备硬件同步周期（分钟：0/10/30/60/180/720）
     */
    @PUT("/open/v1/devices/{deviceId}/sync-interval")
    suspend fun updateSyncInterval(
        @Path("deviceId") deviceId: String,
        @Body request: UpdateDeviceSyncIntervalRequest,
    ): ApiEnvelope<DeviceSyncIntervalResponse>
}
