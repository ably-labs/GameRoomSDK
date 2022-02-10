package com.ablylabs.pubcrawler.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ably.game.room.GamePlayer
import com.ablylabs.multiplayergame.R
import com.ably.game.example.ui.checkName


class PlayersRecyclerAdapter(
    private val onSayHi: (player: GamePlayer) -> Unit
) : RecyclerView.Adapter<PlayersRecyclerAdapter.ViewHolder>() {
    private val players = mutableListOf<GamePlayer>()
    fun setPlayers(list: List<GamePlayer>) {
        players.clear()
        players.addAll(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.personTextView.text = players[position].id
        checkName(holder.itemView.context) {
            if (players[position].id != it) {
                holder.sayHiButton.visibility = View.VISIBLE
                holder.sayHiButton.setOnClickListener { onSayHi(players[position]) }
            } else {
                holder.sayHiButton.visibility = View.GONE
                holder.personTextView.append(" (you)")
            }
        }
    }

    override fun getItemCount() = players.size
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val personTextView: TextView = view.findViewById(R.id.personTextView)
        val sayHiButton: Button = view.findViewById(R.id.sayHiButton)
    }
}