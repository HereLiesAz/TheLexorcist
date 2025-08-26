package com.hereliesaz.lexorcist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaggedDataAdapter(private val data: Map<String, List<String>>) :
    RecyclerView.Adapter<TaggedDataAdapter.TaggedDataViewHolder>() {

    private val items = data.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaggedDataViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tagged_data, parent, false)
        return TaggedDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaggedDataViewHolder, position: Int) {
        val (tag, values) = items[position]
        holder.bind(tag, values)
    }

    override fun getItemCount(): Int = items.size

    class TaggedDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagTextView: TextView = itemView.findViewById(R.id.tag_textview)
        private val dataTextView: TextView = itemView.findViewById(R.id.data_textview)

        fun bind(tag: String, data: List<String>) {
            tagTextView.text = tag
            dataTextView.text = data.joinToString("\n")
        }
    }
}
