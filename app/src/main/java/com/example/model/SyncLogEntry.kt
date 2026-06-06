package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncLogEntry(
    val sender: String,
    val smsPreview: String,
    val timestamp: String,
    val status: String, // "success", "failed", "missed"
    val transactionId: String? = null
)
