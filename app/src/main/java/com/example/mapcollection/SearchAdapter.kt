package com.example.mapcollection

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(
    private val searchList: List<Post>
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mapNameText: TextView = itemView.findViewById(R.id.mapNameText)
        val mapTypeText: TextView = itemView.findViewById(R.id.mapTypeText)
        val mapDescriptionText: TextView = itemView.findViewById(R.id.mapDescriptionText)
        val mapCoordinateText: TextView = itemView.findViewById(R.id.mapCoordinateText)
        val userNameText: TextView = itemView.findViewById(R.id.userNameText)
        val userLabelText: TextView = itemView.findViewById(R.id.userLabelText)
        val imgUserPhoto: ImageView = itemView.findViewById(R.id.imgUserPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_search, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val post = searchList[position]

        holder.mapNameText.text = post.mapName
        holder.mapTypeText.text = post.mapType
        holder.mapDescriptionText.text = post.description.ifEmpty { "（尚無說明）" }
        holder.mapCoordinateText.text = post.getFormattedLatLng()
        holder.userNameText.text = post.userName.ifEmpty { "使用者姓名" }
        holder.userLabelText.text = post.userLabel.ifEmpty { "個人化標籤" }

        if (post.userPhoto != null) {
            val bitmap = BitmapFactory.decodeByteArray(post.userPhoto, 0, post.userPhoto.size)
            holder.imgUserPhoto.setImageBitmap(bitmap)
        } else {
            holder.imgUserPhoto.setImageResource(R.drawable.default_user) // 若無圖片，使用預設圖示
        }
    }

    override fun getItemCount(): Int = searchList.size
}
