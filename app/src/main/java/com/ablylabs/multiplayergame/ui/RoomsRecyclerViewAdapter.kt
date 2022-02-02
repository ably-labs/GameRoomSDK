package com.ablylabs.multiplayergame.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ablylabs.multiplayergame.MyGameRoom
import com.ablylabs.multiplayergame.R

class RoomsRecyclerViewAdapter(private val rooms:List<MyGameRoom>): RecyclerView.Adapter<RoomsRecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.roomNameTextView.text = rooms[position].name
    }

    override fun getItemCount(): Int {
        return rooms.size
    }
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomNameTextView: TextView = view.findViewById(R.id.roomNameTextView)
        init {
            // Define click listener for the ViewHolder's View.
            view.setOnClickListener {
                //define on click behaviour
            }
        }
    }
}