package com.example.legalparser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.legalparser.db.FinancialEntry
import java.text.SimpleDateFormat
import java.util.*

class FinancialEntryAdapter : ListAdapter<FinancialEntry, FinancialEntryAdapter.EntryViewHolder>(EntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val amountTextView: TextView = itemView.findViewById(R.id.amount_textview)
        private val documentDateTextView: TextView = itemView.findViewById(R.id.document_date_textview)
        private val sourceTextView: TextView = itemView.findViewById(R.id.source_textview)
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(entry: FinancialEntry) {
            amountTextView.text = entry.amount
            documentDateTextView.text = "Document Date: ${dateFormat.format(Date(entry.documentDate))}"
            sourceTextView.text = "Source: ${entry.sourceDocument}"
        }
    }

    class EntryDiffCallback : DiffUtil.ItemCallback<FinancialEntry>() {
        override fun areItemsTheSame(oldItem: FinancialEntry, newItem: FinancialEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FinancialEntry, newItem: FinancialEntry): Boolean {
            return oldItem == newItem
        }
    }
}
