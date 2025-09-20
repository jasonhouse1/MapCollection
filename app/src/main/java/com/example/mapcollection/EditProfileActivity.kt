package com.example.mapcollection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mapcollection.databinding.ActivityEditprofileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditprofileBinding
    private var imageUri: Uri? = null

    // 建立圖片選擇器
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                binding.imgUserPhoto.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 點擊頭像 → 開啟圖片選擇
        binding.imgUserPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 點擊儲存按鈕
        binding.btnSave.setOnClickListener {
            val name = binding.edUserName.text.toString().trim()
            val label = binding.edUserLabel.text.toString().trim()
            val intro = binding.edIntroduction.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "請輸入使用者名稱", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 假設未連接 Firebase，這裡先顯示結果
            val message = buildString {
                append("名稱: $name\n")
                append("標籤: $label\n")
                append("簡介: $intro\n")
                append(if (imageUri != null) "頭像已選擇" else "尚未選擇頭像")
            }

            Toast.makeText(this, "已儲存變更\n$message", Toast.LENGTH_LONG).show()
        }
    }
}
