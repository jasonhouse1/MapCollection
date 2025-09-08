package com.example.mapcollection

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val DEFAULT_USER_NAME = "使用者姓名"
        private const val DEFAULT_USER_LABEL = "個人化標籤"
    }

    private lateinit var recyclerView: RecyclerView
    private val posts = mutableListOf<Post>()
    private lateinit var mapsActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var editProfileLauncher: ActivityResultLauncher<Intent>
    private var editingPosition: Int? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    private lateinit var userNameText: TextView
    private lateinit var userLabelText: TextView
    private lateinit var imgProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            initializeComponents()
            setupActivityLaunchers()
        } catch (e: Exception) {
            Log.e(TAG, "初始化失敗", e)
            Toast.makeText(this, "應用初始化失敗", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeComponents() {
        sharedPreferences = getSharedPreferences("MapCollection", MODE_PRIVATE)
        loadPosts()
        setupRecyclerView()
        setupNavigationButtons()
        setupFloatingButton()
        setupEditProfileButton()
        loadUserProfile()
    }

    private fun setupActivityLaunchers() {
        mapsActivityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result -> handleMapActivityResult(result.resultCode, result.data) }

        editProfileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result -> handleEditProfileResult(result.resultCode, result.data) }
    }

    private fun handleMapActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val newPost = Post(
                mapName = data.getStringExtra("mapName").orEmpty().ifBlank { "未命名地圖" },
                mapType = data.getStringExtra("mapType").orEmpty(),
                description = data.getStringExtra("description").orEmpty(),
                latitude = data.getDoubleExtra("latitude", 0.0),
                longitude = data.getDoubleExtra("longitude", 0.0)
            )

            if (validatePost(newPost)) {
                editingPosition?.let { position ->
                    if (position in posts.indices) {
                        posts[position] = newPost
                        recyclerView.adapter?.notifyItemChanged(position)
                    }
                    editingPosition = null
                } ?: run {
                    posts.add(newPost)
                    recyclerView.adapter?.notifyItemInserted(posts.size - 1)
                }
                savePosts()
            }
        }
    }

    private fun validatePost(post: Post): Boolean {
        return when {
            post.mapName.isBlank() -> {
                Toast.makeText(this, "地圖名稱不能為空", Toast.LENGTH_SHORT).show()
                false
            }
            post.latitude !in -90.0..90.0 -> {
                Toast.makeText(this, "緯度超出範圍 (-90 ~ 90)", Toast.LENGTH_SHORT).show()
                false
            }
            post.longitude !in -180.0..180.0 -> {
                Toast.makeText(this, "經度超出範圍 (-180 ~ 180)", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun handleEditProfileResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val userName = data.getStringExtra("userName").orEmpty()
            val userLabel = data.getStringExtra("userLabel").orEmpty()
            val userPhoto = data.getByteArrayExtra("userPhoto")

            saveUserProfile(userName, userLabel)
            if (userPhoto != null) saveUserPhoto(userPhoto)
            updateUserProfileDisplay(userName, userLabel)
            loadUserPhoto()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = PostAdapter(posts,
            onItemClick = { position ->
                val post = posts[position]
                val intent = Intent(this, MapsActivity::class.java).apply {
                    putExtra("mapName", post.mapName)
                    putExtra("mapType", post.mapType)
                    putExtra("description", post.description)
                    putExtra("latitude", post.latitude)
                    putExtra("longitude", post.longitude)
                }
                editingPosition = position
                mapsActivityLauncher.launch(intent)
            },
            onDeleteClick = { position -> deletePost(position) }
        )
    }

    private fun setupNavigationButtons() {
        findViewById<ImageButton>(R.id.btnRecommend).setOnClickListener {
            startActivity(Intent(this, RecommendActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnPath).setOnClickListener {
            startActivity(Intent(this, PathActivity::class.java))
        }
    }

    private fun setupFloatingButton() {
        findViewById<FloatingActionButton>(R.id.floatingActionButton).setOnClickListener {
            mapsActivityLauncher.launch(Intent(this, MapsActivity::class.java))
        }
    }

    private fun setupEditProfileButton() {
        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java).apply {
                putExtra("currentUserName", userNameText.text.toString())
                putExtra("currentUserLabel", userLabelText.text.toString())
            }
            editProfileLauncher.launch(intent)
        }
    }

    private fun loadPosts() {
        val json = sharedPreferences.getString("posts", "[]") ?: "[]"
        val type = object : TypeToken<List<Post>>() {}.type
        posts.clear()
        posts.addAll(gson.fromJson(json, type))
    }

    private fun savePosts() {
        val json = gson.toJson(posts)
        sharedPreferences.edit().putString("posts", json).apply()
    }

    private fun loadUserProfile() {
        userNameText = findViewById(R.id.userName)
        userLabelText = findViewById(R.id.userLabel)
        imgProfile = findViewById(R.id.imgProfile)

        val name = sharedPreferences.getString("userName", DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
        val label = sharedPreferences.getString("userLabel", DEFAULT_USER_LABEL) ?: DEFAULT_USER_LABEL

        updateUserProfileDisplay(name, label)
        loadUserPhoto()
    }

    private fun updateUserProfileDisplay(name: String, label: String) {
        userNameText.text = name
        userLabelText.text = label
    }

    private fun saveUserProfile(name: String, label: String) {
        sharedPreferences.edit()
            .putString("userName", name)
            .putString("userLabel", label)
            .apply()
    }

    private fun saveUserPhoto(photoBytes: ByteArray) {
        sharedPreferences.edit()
            .putString("userPhoto", android.util.Base64.encodeToString(photoBytes, android.util.Base64.DEFAULT))
            .apply()
    }

    private fun loadUserPhoto() {
        val photoBase64 = sharedPreferences.getString("userPhoto", null)
        photoBase64?.let {
            try {
                val bytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imgProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "頭像載入失敗", e)
            }
        }
    }

    private fun deletePost(position: Int) {
        if (position in posts.indices) {
            if (editingPosition == position) {
                editingPosition = null
            } else if (editingPosition != null && editingPosition!! > position) {
                editingPosition = editingPosition!! - 1
            }
            posts.removeAt(position)
            recyclerView.adapter?.notifyItemRemoved(position)
            savePosts()
        }
    }
}
