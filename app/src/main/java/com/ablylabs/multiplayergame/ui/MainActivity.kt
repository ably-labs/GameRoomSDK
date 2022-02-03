package com.ablylabs.multiplayergame.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ablylabs.ablygamesdk.AblyGame
import com.ablylabs.ablygamesdk.GameRoom
import com.ablylabs.ablygamesdk.PresenceAction
import com.ablylabs.multiplayergame.MultiplayerGameApp
import com.ablylabs.multiplayergame.MyGamePlayer
import com.ablylabs.multiplayergame.MyGameRoom
import com.ablylabs.multiplayergame.R
import com.ablylabs.pubcrawler.ui.GameRoomActivity
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlin.random.Random


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    val sampleRooms = listOf(
        MyGameRoom("Volleyball room"),MyGameRoom("Basketball room"),MyGameRoom("Football " +
        "room"),MyGameRoom("Hockey room"),MyGameRoom("Tennis room"))

    private lateinit var recyclerLayoutManager:LinearLayoutManager
    private lateinit var roomsRecyclerView: RecyclerView
    private lateinit var ablyGame:AblyGame
    private var numberOfPlayers = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        recyclerLayoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        roomsRecyclerView = findViewById(R.id.roomsRecyclerView)
        roomsRecyclerView.layoutManager = recyclerLayoutManager
        //add some spacing between items
        val dividerItemDecoration = DividerItemDecoration(
            roomsRecyclerView.context,
            recyclerLayoutManager.orientation
        )
        roomsRecyclerView.addItemDecoration(dividerItemDecoration)
        roomsRecyclerView.adapter = RoomsRecyclerViewAdapter(sampleRooms,this::onRoomTap)

        val numberOfPlayersTextView = findViewById<TextView>(R.id.numberOfPlayersTextView)

        lifecycleScope.launch {
            ablyGame = MultiplayerGameApp.instance.ablyGame
            //also register to changes
            ablyGame.subscribeToPlayerNumberUpdate {
                //you can either update numbers here or pull numberOfPlayers
                when(it){
                    PresenceAction.ENTER -> numberOfPlayers++
                    PresenceAction.LEAVE -> numberOfPlayers--
                }
                numberOfPlayersTextView.text = "${numberOfPlayers} players"
            }
            val enterResult = ablyGame.enter(MyGamePlayer("ikbal${Random.nextInt(1000)}"))
            if (enterResult.isSuccess){
                Log.d(TAG, "Successful entry")
            }else{
                Log.e(TAG, "problem entering: ",enterResult.exceptionOrNull())
            }
            numberOfPlayers = ablyGame.numberOfPlayers()
            numberOfPlayersTextView.text = "${numberOfPlayers} players"

        }
    }

    private fun onRoomTap(gameRoom: GameRoom){
        checkName(this) { name ->
            val who = MyGamePlayer(name)

            Intent(this, GameRoomActivity::class.java).run {
                val gson = Gson()
                putExtra(GameRoomActivity.EXTRA_ROOM_JSON, gson.toJson(gameRoom))
                putExtra(GameRoomActivity.EXTRA_PLAYER_JSON, gson.toJson(who))
                startActivity(this)
            }
        }
    }
}