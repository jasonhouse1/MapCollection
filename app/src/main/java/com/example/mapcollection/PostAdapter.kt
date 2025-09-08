package com.example.mapcollection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class PostAdapter(
    private val posts: List<Post>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    companion object {
        private const val TAG = "PostAdapter"
        private val executor = Executors.newSingleThreadExecutor()
    }

    private val imageCache = mutableMapOf<Int, Bitmap?>()

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mapNameText: TextView = itemView.findViewById(R.id.mapNameText)
        val mapTypeText: TextView = itemView.findViewById(R.id.mapTypeText)
        val userNameText: TextView = itemView.findViewById(R.id.userNameText)
        val userLabelText: TextView = itemView.findViewById(R.id.userLabelText)
        val imgMap: ImageView = itemView.findViewById(R.id.imgMap)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        if (position !in posts.indices) {
            Log.w(TAG, "無效的位置: $position")
            return
        }

        val post = posts[position]
        holder.mapNameText.text = post.mapName.ifBlank { "未命名地圖" }
        holder.mapTypeText.text = post.mapType.ifBlank { "未分類" }
        holder.userNameText.text = post.userName ?: "使用者"
        holder.userLabelText.text = post.userLabel ?: "探索者"

        holder.btnDelete.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION && currentPosition in posts.indices) {
                onDeleteClick(currentPosition)
            } else {
                Log.w(TAG, "嘗試刪除無效的位置: $currentPosition")
            }
        }

        loadImage(holder, post, position)
    }

    override fun getItemCount(): Int = posts.size

    private fun loadImage(holder: PostViewHolder, post: Post, position: Int) {
        holder.imgMap.setImageResource(R.drawable.map)

        post.userPhoto?.let { photoBytes ->
            if (imageCache.containsKey(position)) {
                imageCache[position]?.let { bitmap ->
                    holder.imgMap.setImageBitmap(bitmap)
                }
                return
            }

            executor.execute {
                try {
                    val bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                    if (bitmap != null) {
                        imageCache[position] = bitmap
                        holder.itemView.post {
                            if (holder.bindingAdapterPosition == position) {
                                holder.imgMap.setImageBitmap(bitmap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "圖片解碼失敗: ${e.message}")
                    imageCache.remove(position)
                }
            }
        }
    }

    fun clearResources() {
        imageCache.values.forEach { it?.recycle() }
        imageCache.clear()
    }

    fun updatePosts(newPosts: List<Post>) {
        clearResources()
        notifyDataSetChanged() // 可改成 DiffUtil 提升效能
    }

    private fun removeImageCache(position: Int) {
        imageCache[position]?.recycle()
        imageCache.remove(position)
    }
}
