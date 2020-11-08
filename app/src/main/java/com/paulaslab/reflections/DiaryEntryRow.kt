package com.paulaslab.reflections

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class DiaryEntryRow(val context: Context, val data: JournallingStore, val playVideo: (Int) -> Unit) : RecyclerView.Adapter<DiaryEntryRow.ViewHolder>() {
    private var mInflater: LayoutInflater = LayoutInflater.from(context)
    private var goodData = data.listGoodEntries()

    fun updateIt() {
        goodData = data.listGoodEntries()
        notifyDataSetChanged()
    }

    // stores and recycles views as they are scrolled off screen
    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var onClick: () -> Unit = {}
        var myTextView: TextView

        override fun onClick(view: View?) {
            onClick()
        }

        init {
            myTextView = itemView.findViewById<TextView>(R.id.diary_entry_label)
            itemView.setOnClickListener(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater!!.inflate(R.layout.diary_entry_row, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val id = goodData[position]
        val humor = data.extractHumor(id)?.getScore()
        holder.myTextView.setText("Diary entry: $id")
        val l = holder.myTextView.parent as LinearLayout
        l.setBackgroundColor(
            Color.argb(100, 0,
                kotlin.math.max(humor!! * 20, 0f).toInt(),
                kotlin.math.max((-1f) * (humor!!) * 20f, 0f).toInt() ))
        holder.onClick = { playVideo(id) }
    }

    override fun getItemCount(): Int = goodData.size
}