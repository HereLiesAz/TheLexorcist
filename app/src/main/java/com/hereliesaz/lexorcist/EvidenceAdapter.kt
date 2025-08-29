package com.hereliesaz.lexorcist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hereliesaz.lexorcist.R
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hereliesaz.lexorcist.data.Evidence
import java.text.SimpleDateFormat
import java.util.*

class EvidenceAdapter : ListAdapter<Evidence, EvidenceAdapter.EvidenceViewHolder>(EvidenceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvidenceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evidence, parent, false)
        return EvidenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: EvidenceViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    class EvidenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.content_textview)
        private val dateTextView: TextView = itemView.findViewById(R.id.date_textview)
        private val tagsTextView: TextView = itemView.findViewById(R.id.tags_textview)
        // Assuming there might be another TextView for timestamp if errors on 35 & 36 are distinct
        // For now, will use dateTextView for documentDate and make sure tags are handled.

        fun bind(entry: Evidence) {
            contentTextView.text = entry.sourceDocument // Displays source document as content
            
            // Line 35 equivalent if it was for timestamp (entry.timestamp is Date)
            // if there was a timestampTextView: 
            // timestampTextView.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(entry.timestamp)
            
            // Line 36 equivalent for documentDate (entry.documentDate is Date)
            dateTextView.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(entry.documentDate)
            
            // Line 37 equivalent for tags (entry.tags is List<String>?)
            tagsTextView.text = entry.tags?.joinToString(", ") ?: "No tags"
        }
    }
}

class EvidenceDiffCallback : DiffUtil.ItemCallback<Evidence>() {
    override fun areItemsTheSame(oldItem: Evidence, newItem: Evidence): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Evidence, newItem: Evidence): Boolean {
        return oldItem == newItem
    }
}
