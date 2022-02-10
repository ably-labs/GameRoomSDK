package com.ably.game.example

import android.app.Application
import com.ably.game.room.AblyGame
import com.ablylabs.multiplayergame.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


private const val TAG = "MultiplayerGameApp"

class MultiplayerGameApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var ablyGame: AblyGame

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            ablyGame = AblyGame.Builder(getString(R.string.ably_key))
                .scope(applicationScope)
                .build()
        }
        instance = this
    }

    companion object {
        lateinit var instance: MultiplayerGameApp
            private set
    }
}