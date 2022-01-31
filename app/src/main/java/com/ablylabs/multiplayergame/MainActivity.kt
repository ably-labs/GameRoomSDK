package com.ablylabs.multiplayergame

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    val sampleRooms = listOf(MyGameRoom("Volleyball room"),MyGameRoom("Basketball room"),MyGameRoom("Football " +
        "room"),MyGameRoom("Hockey room"),MyGameRoom("Tennis room"))

    private lateinit var recyclerLayoutManager:LinearLayoutManager
    private lateinit var roomsRecyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerLayoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        roomsRecyclerView = findViewById(R.id.roomsRecyclerView)
        roomsRecyclerView.layoutManager = recyclerLayoutManager
        //add some spacing between items
        val dividerItemDecoration = DividerItemDecoration(
            roomsRecyclerView.context,
            recyclerLayoutManager.orientation
        )
        roomsRecyclerView.addItemDecoration(dividerItemDecoration)
        roomsRecyclerView.adapter = RoomsRecyclerViewAdapter(sampleRooms)

        //enter after some dleay
        lifecycleScope.launch {
            delay(1000)

            val result = MultiplayerGameApp.instance.ablyGameBuilder.build().enter(MyGamePlayer("ikbal"))
            Log.d(TAG, "enter result $result ")
        }
    }
}