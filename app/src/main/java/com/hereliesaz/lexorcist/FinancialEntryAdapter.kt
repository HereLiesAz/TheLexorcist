package com.hereliesaz.lexorcist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hereliesaz.lexorcist.R
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hereliesaz.lexorcist.db.FinancialEntry
import java.text.SimpleDateFormat
import java.util.*

class FinancialEntryAdapter : ListAdapter<FinancialEntry, FinancialEntryAdapter.FinancialEntryViewHolder>(FinancialEntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FinancialEntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_financial_entry, parent, false)
        return FinancialEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: FinancialEntryViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    class FinancialEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val amountTextView: TextView = itemView.findViewById(R.id.amount_textview)
        private val dateTextView: TextView = itemView.findViewById(R.id.date_textview)
        private val categoryTextView: TextView = itemView.findViewById(R.id.category_textview)

        fun bind(entry: FinancialEntry) {
            amountTextView.text = entry.amount
            dateTextView.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.documentDate))
            categoryTextView.text = entry.category
        }
    }
}

class FinancialEntryDiffCallback : DiffUtil.ItemCallback<FinancialEntry>() {
    override fun areItemsTheSame(oldItem: FinancialEntry, newItem: FinancialEntry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FinancialEntry, newItem: FinancialEntry): Boolean {
        return oldItem == newItem
    }
}
