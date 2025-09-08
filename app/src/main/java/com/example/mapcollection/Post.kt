package com.example.mapcollection

import java.io.Serializable

data class Post(
    val mapName: String = "",
    val mapType: String = "",
    val userName: String? = null,         // 使用者名稱
    val userLabel: String? = null,        // 個人化標籤
    val userPhoto: ByteArray? = null      // 使用者大頭貼（byte array）
) : Serializable
