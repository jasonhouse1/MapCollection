package com.example.mapcollection

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.mapcollection.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private val posts = mutableListOf<Post>()
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val db = Firebase.firestore
    private var currentEmail: String? = null
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        currentEmail = getSharedPreferences("Account", MODE_PRIVATE)
            .getString("LOGGED_IN_EMAIL", null) ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        sharedPreferences = getSharedPreferences("MapCollection", MODE_PRIVATE)
        loadPostsFromLocal()

        recyclerView = binding.recyclerView

        loadProfileFromLocal()
        fetchProfileFromCloud()

        setupRecyclerView()
        setupFloatingAdd()
        setupEditProfileButton()
        setupBottomNavigation()

        fetchMyPostsFromCloud()
    }

    override fun onResume() {
        super.onResume()
        fetchProfileFromCloud()
        fetchMyPostsFromCloud()
    }

    // ---------------- 個人資料讀取 ----------------
    private fun loadProfileFromLocal() {
        val email = currentEmail ?: return
        val prefs = getSharedPreferences("Profile_$email", MODE_PRIVATE)

        val userName = prefs.getString("userName", "使用者姓名") ?: "使用者姓名"
        val userLabel = prefs.getString("userLabel", "個人化標籤") ?: "個人化標籤"
        val introduction = prefs.getString("introduction", "個人簡介") ?: "個人簡介"
        val photoBase64 = prefs.getString("userPhotoBase64", null)
        val photoUrl = prefs.getString("photoUrl", null)

        updateUserProfileDisplay(userName, userLabel, introduction)

        when {
            !photoUrl.isNullOrEmpty() -> Glide.with(this).load(photoUrl).into(binding.imgProfile)
            !photoBase64.isNullOrEmpty() -> {
                try {
                    val bytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.imgProfile.setImageBitmap(bmp)
                } catch (_: Exception) {}
            }
        }
    }

    private fun fetchProfileFromCloud() {
        val email = currentEmail ?: return
        db.collection("users").document(email).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val userName = doc.getString("userName") ?: "使用者姓名"
                    val userLabel = doc.getString("userLabel") ?: "個人化標籤"
                    val introduction = doc.getString("introduction") ?: "個人簡介"
                    val photoUrl = doc.getString("photoUrl")

                    updateUserProfileDisplay(userName, userLabel, introduction)
                    saveProfileToLocal(userName, userLabel, introduction)

                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this).load(photoUrl).into(binding.imgProfile)

                        getSharedPreferences("Profile_$email", MODE_PRIVATE)
                            .edit()
                            .putString("photoUrl", photoUrl)
                            .remove("userPhotoBase64")
                            .apply()
                    }
                }

                updateStats()
                fetchSocialCounts()
            }
    }

    private fun saveProfileToLocal(userName: String, userLabel: String, introduction: String) {
        val email = currentEmail ?: return
        getSharedPreferences("Profile_$email", MODE_PRIVATE).edit()
            .putString("userName", userName)
            .putString("userLabel", userLabel)
            .putString("introduction", introduction)
            .apply()
    }

    private fun updateUserProfileDisplay(userName: String, userLabel: String, introduction: String) {
        binding.userName.text = userName
        binding.introduction.text = introduction
        renderLabelChips(userLabel)
    }

    private fun renderLabelChips(raw: String) {
        binding.chipGroupLabels.removeAllViews()
        val tokens = raw.replace("，", ",").replace("、", ",")
            .split(',', '#', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        for (t in tokens) {
            val themedCtx = android.view.ContextThemeWrapper(this, R.style.ChipStyle_Label)
            val chip = Chip(themedCtx, null, 0).apply {
                text = t
                isCheckable = false
                isClickable = false
                setEnsureMinTouchTargetSize(false)
            }
            binding.chipGroupLabels.addView(chip)
        }
    }

    // ---------------- 本地貼文讀取 ----------------
    private fun loadPostsFromLocal() {
        val json = sharedPreferences.getString("posts", "[]")
        val type = object : TypeToken<List<Post>>() {}.type
        val savedPosts = gson.fromJson<List<Post>>(json, type) ?: emptyList()
        posts.clear()
        posts.addAll(savedPosts)
    }

    private fun savePostsToLocal() {
        val json = gson.toJson(posts)
        sharedPreferences.edit().putString("posts", json).apply()
    }

    // ---------------- Firestore 讀取貼文 ----------------
    private fun fetchMyPostsFromCloud() {
        val email = currentEmail ?: return
        db.collection("posts")
            .whereEqualTo("ownerEmail", email)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                posts.clear()
                for (doc in snap) {
                    posts.add(
                        Post(
                            doc.id,
                            doc.getString("mapName") ?: "",
                            doc.getString("mapType") ?: "",
                            doc.getTimestamp("createdAt"),
                            doc.getBoolean("isRecommended") ?: false
                        )
                    )
                }

                recyclerView.adapter?.notifyDataSetChanged()
                savePostsToLocal()
                updateStats()
            }
            .addOnFailureListener {
                db.collection("posts")
                    .whereEqualTo("ownerEmail", email)
                    .get()
                    .addOnSuccessListener { snap2 ->
                        posts.clear()
                        for (doc in snap2) {
                            posts.add(
                                Post(
                                    doc.id,
                                    doc.getString("mapName") ?: "",
                                    doc.getString("mapType") ?: "",
                                    doc.getTimestamp("createdAt"),
                                    doc.getBoolean("isRecommended") ?: false
                                )
                            )
                        }
                        posts.sortByDescending { it.createdAt?.seconds ?: 0L }
                        recyclerView.adapter?.notifyDataSetChanged()
                        savePostsToLocal()
                        updateStats()
                    }
            }
    }

    // ---------------- 統計資訊 ----------------
    private fun updateStats() {
        val postCount = posts.size

        // 貼文數
        binding.statPostsValue.text = postCount.toString()

        // 粉絲與追蹤中先清空（fetchSocialCounts 會補上）
        binding.statFollowersValue.text = "—"
        binding.statFollowingValue.text = "—"
    }

    private fun fetchSocialCounts() {
        val email = currentEmail ?: return
        val userDoc = db.collection("users").document(email)

        // 粉絲
        userDoc.collection("followers")
            .count()
            .get(AggregateSource.SERVER)
            .addOnSuccessListener { agg ->
                binding.statFollowersValue.text = agg.count.toString()
            }

        // 追蹤中
        userDoc.collection("following")
            .count()
            .get(AggregateSource.SERVER)
            .addOnSuccessListener { agg ->
                binding.statFollowingValue.text = agg.count.toString()
            }
    }

    // ---------------- RecyclerView 設置 ----------------
    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = PostAdapter(posts) { position ->
            if (position !in posts.indices) return@PostAdapter
            val post = posts[position]
            startActivity(
                Intent(this, MapEditorActivity::class.java)
                    .putExtra("POST_ID", post.docId)
            )
        }
    }

    // ---------------- 新增貼文按鈕 ----------------
    private fun setupFloatingAdd() {
        binding.fabUpload.setOnClickListener {
            startActivity(
                Intent(this, MapEditorActivity::class.java)
                    .putExtra("NEW_POST", true)
            )
        }
    }

    // ---------------- 編輯資料 ----------------
    private fun setupEditProfileButton() {
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java).apply {
                putExtra("currentUserName", binding.userName.text.toString())
                putExtra("currentUserLabel", getCurrentLabelsAsString())
                putExtra("currentIntroduction", binding.introduction.text.toString())
            }
            startActivity(intent)
        }
    }

    private fun getCurrentLabelsAsString(): String {
        val list = mutableListOf<String>()
        for (i in 0 until binding.chipGroupLabels.childCount) {
            val c = binding.chipGroupLabels.getChildAt(i)
            if (c is Chip) list.add(c.text?.toString() ?: "")
        }
        return list.filter { it.isNotEmpty() }.joinToString(",")
    }

    // ---------------- 底部導覽列 ----------------
    private fun setupBottomNavigation() {

        val bottomNav =
            findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomBar)

        bottomNav.setOnItemSelectedListener { item ->
            if (isNavigating) return@setOnItemSelectedListener false
            isNavigating = true

            when (item.itemId) {
                R.id.nav_home ->
                    startActivity(Intent(this, RecommendActivity::class.java))

                R.id.nav_search ->
                    startActivity(Intent(this, SearchActivity::class.java))

                R.id.nav_path ->
                    startActivity(Intent(this, PathActivity::class.java))

                R.id.nav_profile -> { /* 已在本頁 */ }
            }

            bottomNav.postDelayed({ isNavigating = false }, 500)
            true
        }

        bottomNav.selectedItemId = R.id.nav_profile
    }

    // ---------------- 貼文刪除 ----------------
    fun confirmDeletePost(position: Int) {
        if (position !in posts.indices) return
        val title = posts[position].mapName.ifBlank { "未命名地圖" }

        AlertDialog.Builder(this)
            .setTitle("刪除貼文")
            .setMessage("確定要刪除「$title」嗎？此動作無法復原。")
            .setNegativeButton("取消", null)
            .setPositiveButton("刪除") { _, _ -> deletePost(position) }
            .show()
    }

    fun deletePost(position: Int) {
        if (position !in posts.indices) return
        val docId = posts[position].docId
        val email = currentEmail ?: return

        if (docId.isNotEmpty()) {
            db.collection("posts").document(docId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists() && doc.getString("ownerEmail") == email) {
                        db.collection("posts").document(docId).delete()
                    }
                }
        }

        posts.removeAt(position)
        recyclerView.adapter?.notifyDataSetChanged()
        savePostsToLocal()
        updateStats()
        fetchSocialCounts()
    }
}

// ------------------- 資料類別 + Adapter -------------------
data class Post(
    val docId: String = "",
    val mapName: String = "",
    val mapType: String = "",
    val createdAt: Timestamp? = null,
    val isRecommended: Boolean = false
)

class PostAdapter(
    private val posts: List<Post>,
    private val onItemClick: (Int) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val mapNameText: TextView = view.findViewById(R.id.mapNameText)
        val mapTypeText: TextView = view.findViewById(R.id.mapTypeText)
        val btnDelete: android.widget.ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PostViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.card_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.mapNameText.text = post.mapName
        holder.mapTypeText.text = post.mapType

        holder.itemView.setOnClickListener {
            val realPos = holder.bindingAdapterPosition
            if (realPos in posts.indices) onItemClick(realPos)
        }

        holder.btnDelete.setOnClickListener {
            val realPos = holder.bindingAdapterPosition
            (holder.itemView.context as? MainActivity)?.confirmDeletePost(realPos)
        }
    }

    override fun getItemCount() = posts.size
}
