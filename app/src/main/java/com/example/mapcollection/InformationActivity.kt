package com.example.mapcollection

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class InformationActivity : AppCompatActivity() {

    private lateinit var tvLocName: TextView
    private lateinit var btnBack: Button
    private lateinit var btnAskAI: Button
    private lateinit var btnNearbySpots: Button

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var spotName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.card_information)

        tvLocName = findViewById(R.id.tvLocName)
        btnBack = findViewById(R.id.btnBack)
        btnAskAI = findViewById(R.id.button)   // 詢問 AI
        btnNearbySpots = findViewById(R.id.button2) // 介紹附近景點

        // 讀取參數（名稱可用於顯示，AI 還是用座標）
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        spotName = intent.getStringExtra("spotName")
            ?: intent.getStringExtra("EXTRA_SPOT_NAME") // 兼容其他 key

        val hasName = !spotName.isNullOrBlank() && spotName != "null"
        tvLocName.text = if (hasName) spotName else "座標: $latitude, $longitude"

        btnBack.setOnClickListener { finish() }

        btnAskAI.setOnClickListener { showAskAIDialog() }
        btnNearbySpots.setOnClickListener { findNearbyAttractions() }
    }

    private fun showAskAIDialog() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("詢問 AI")
            .setMessage("請輸入你想詢問此景點的問題：")
            .setView(editText)
            .setPositiveButton("送出") { dialog, _ ->
                val question = editText.text.toString()
                if (question.isNotBlank()) {
                    val prompt = "景點座標 ($latitude, $longitude)，問題：$question，請用繁體中文簡明回答。"
                    callGemini(prompt, "AI 的回答")
                }
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun findNearbyAttractions() {
        val prompt =
            "請列出座標 ($latitude, $longitude) 方圓 3 公里內推薦的 5 個景點，每個景點給一行名稱與一句亮點。"
        callGemini(prompt, "附近景點建議")
    }

    private fun callGemini(prompt: String, title: String) {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("AI 思考中...")
            .setCancelable(false)
            .show()

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                loadingDialog.dismiss()
                showResultDialog(title, response.text ?: "無法取得回應，請再試一次。")
            } catch (e: Exception) {
                loadingDialog.dismiss()
                showResultDialog("錯誤", "發生錯誤：${e.localizedMessage}")
            }
        }
    }

    private fun showResultDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("確認", null)
            .show()
    }
}
