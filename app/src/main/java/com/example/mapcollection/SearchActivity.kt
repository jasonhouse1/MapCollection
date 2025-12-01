package com.example.mapcollection

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapcollection.model.SearchItem
import com.example.mapcollection.ui.search.SearchAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var etQuery: EditText
    private lateinit var rvResults: RecyclerView
    private lateinit var adapter: SearchAdapter

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etQuery = findViewById(R.id.etQuery)
        rvResults = findViewById(R.id.rvResults)

        adapter = SearchAdapter { item ->
            startActivity(
                Intent(this, PublicMapViewerActivity::class.java)
                    .putExtra("POST_ID", item.id)
                    .putExtra("MAP_TITLE", item.title)
                    .putExtra("MAP_TYPE", item.subtitle.removePrefix("分類："))
            )
        }
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = adapter

        setupBottomNav()

        etQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE
            ) {
                performSearch(etQuery.text.toString())
                true
            } else false
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomBar)
        bottomNav.selectedItemId = R.id.nav_search
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, RecommendActivity::class.java))
                    true
                }
                R.id.nav_search -> true
                R.id.nav_path -> {
                    startActivity(Intent(this, PathActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    
    private fun performSearch(rawQuery: String) {
        val q = rawQuery.trim()
        if (q.isEmpty()) {
            adapter.submitList(emptyList())
            return
        }
        val qL = q.lowercase(Locale.getDefault())
        val qContainsMapWord = qL.contains("地圖")

        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(300)
            .get()
            .addOnSuccessListener { snap ->
                data class Row(
                    val id: String,
                    val name: String,
                    val type: String,
                    val createdAt: Timestamp?
                )
                data class Weighted(
                    val item: SearchItem,
                    val score: Int,
                    val posBoost: Int,
                    val createdAtMillis: Long
                )

                val rows = snap.documents.map { d ->
                    Row(
                        id = d.id,
                        name = d.getString("mapName") ?: "",
                        type = d.getString("mapType") ?: "",
                        createdAt = d.getTimestamp("createdAt")
                    )
                }

                val results = rows.mapNotNull { r ->
                    val nameL = r.name.lowercase(Locale.getDefault())
                    val typeL = r.type.lowercase(Locale.getDefault())

                    val fullHitName = nameL.contains(qL)
                    val fullHitType = typeL.contains(qL)
                    val hasFull = fullHitName || fullHitType

                    val containsMapInItem = nameL.contains("地圖") || typeL.contains("地圖")
                    val mapOnlyMatch = qContainsMapWord && !hasFull && containsMapInItem

                    if (!hasFull && !(qContainsMapWord && mapOnlyMatch)) {
                        return@mapNotNull null
                    }

                    var score = 0
                    var posBoost = 0

                    if (fullHitName) {
                        score += 200
                        posBoost += 100 - nameL.indexOf(qL).coerceAtMost(100)
                    }
                    if (fullHitType) {
                        score += 180
                        posBoost += 60 - typeL.indexOf(qL).coerceAtMost(60)
                    }

                    if (mapOnlyMatch) {
                        val hitInName = nameL.contains("地圖")
                        val idx = if (hitInName) nameL.indexOf("地圖") else typeL.indexOf("地圖")
                        score += if (hitInName) 60 else 50
                        posBoost += 20 - idx.coerceAtMost(20)
                    }

                    val createdAtMillis = r.createdAt?.toDate()?.time ?: 0L

                    Weighted(
                        item = SearchItem(
                            id = r.id,
                            title = r.name.ifBlank { "(未命名地圖)" },
                            subtitle = "分類：${r.type.ifBlank { "未分類" }}"
                        ),
                        score = score,
                        posBoost = posBoost,
                        createdAtMillis = createdAtMillis
                    )
                }.sortedWith(
                    compareByDescending<Weighted> { it.score }
                        .thenByDescending { it.posBoost }
                        .thenByDescending { it.createdAtMillis }
                ).map { it.item }

                adapter.submitList(results)
            }
    }
}
