package com.ably.game.example.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ably.game.room.AblyGame
import com.ably.game.room.GameRoom
import com.ably.game.room.PresenceAction
import com.ably.game.example.MultiplayerGameApp
import com.ably.game.example.MyGamePlayer
import com.ably.game.example.MyGameRoom
import com.ablylabs.multiplayergame.R
import com.ablylabs.pubcrawler.ui.GameRoomActivity
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    val sampleRooms = listOf(
        MyGameRoom("Volleyball room"), MyGameRoom("Basketball room"), MyGameRoom(
            "Football " +
                    "room"
        ), MyGameRoom("Hockey room"), MyGameRoom("Tennis room")
    )

    private lateinit var recyclerLayoutManager: LinearLayoutManager
    private lateinit var roomsRecyclerView: RecyclerView
    private lateinit var numberOfPlayersTextView: TextView
    private lateinit var ablyGame: AblyGame
    private lateinit var gamePlayer: MyGamePlayer
    private lateinit var enterButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        enterButton = findViewById(R.id.enterGameButton)

        recyclerLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        roomsRecyclerView = findViewById(R.id.roomsRecyclerView)
        roomsRecyclerView.layoutManager = recyclerLayoutManager
        //add some spacing between items
        val dividerItemDecoration = DividerItemDecoration(
            roomsRecyclerView.context,
            recyclerLayoutManager.orientation
        )
        roomsRecyclerView.addItemDecoration(dividerItemDecoration)
        roomsRecyclerView.adapter = RoomsRecyclerViewAdapter(sampleRooms, this::onRoomTap)

        numberOfPlayersTextView = findViewById(R.id.numberOfPlayersTextView)

        lifecycleScope.launch {
            delay(1000)
            ablyGame = MultiplayerGameApp.instance.ablyGame
            setupEnterButton()
            subscribeToGameEvents()

            updateNumberOfPlayers()
        }
    }

    private fun updateNumberOfPlayers() {
        lifecycleScope.launch {
            val numberOfPlayers = ablyGame.numberOfPlayers()
            numberOfPlayersTextView.text = "${numberOfPlayers} players"
        }
    }

    private fun setupEnterButton() {
        enterButton.setOnClickListener {
            val context = this
            lifecycleScope.launch {
                if (ablyGame.isInGame(gamePlayer)) {
                    leaveGame()
                } else {
                    checkName(context) { name ->
                        gamePlayer = MyGamePlayer(name)
                        enterGame()
                    }
                }
            }

        }
    }

    private fun leaveGame() {
        lifecycleScope.launch {
            enterButton.isEnabled = false
            val leaveResult = MultiplayerGameApp.instance.ablyGame.leave(gamePlayer)
            enterButton.isEnabled = true
            if (leaveResult.isSuccess) {
                Log.d(TAG, "Successful leave")
            } else {
                Toast.makeText(this@MainActivity, leaveResult.exceptionOrNull().toString(), Toast.LENGTH_SHORT).show()
            }
            updateEnterButton()
        }
    }

    private fun enterGame() {
        lifecycleScope.launch {
            //also register to changes
            enterButton.isEnabled = false
            val enterResult = ablyGame.enter(gamePlayer)
            enterButton.isEnabled = true
            if (enterResult.isSuccess) {
                Log.d(TAG, "Successful entry")
            } else {
                Toast.makeText(this@MainActivity, enterResult.exceptionOrNull().toString(), Toast.LENGTH_SHORT).show()
            }
            updateEnterButton()


        }
    }

    private fun subscribeToGameEvents() {
        ablyGame.subscribeToPlayerNumberUpdate {
            //you can either update numbers here or pull numberOfPlayers
            when (it) {
                is PresenceAction.Enter -> {
                    Log.d(TAG, "PresenceAction.Enter ${it.player.id}")
                }
                is PresenceAction.Leave -> {
                    Log.d(TAG, "PresenceAction.Leave ${it.player.id}")
                }
            }
            updateNumberOfPlayers()
        }
    }

    private fun updateEnterButton() {
        lifecycleScope.launch {
            enterButton.text = if (ablyGame.isInGame(gamePlayer)) getString(R.string.leave_game)
            else getString(R.string.enter_game)
        }
    }

    private fun onRoomTap(gameRoom: GameRoom) {
        Intent(this, GameRoomActivity::class.java).run {
            val gson = Gson()
            putExtra(GameRoomActivity.EXTRA_ROOM_JSON, gson.toJson(gameRoom))
            putExtra(GameRoomActivity.EXTRA_PLAYER_JSON, gson.toJson(gamePlayer))
            startActivity(this)
        }

    }
}