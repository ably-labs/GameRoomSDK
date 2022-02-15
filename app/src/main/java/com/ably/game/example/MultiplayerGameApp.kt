package com.ably.game.example

import android.app.Application
import android.util.Log
import com.ably.game.room.AblyGame
import com.ablylabs.multiplayergame.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "MultiplayerGameApp"
class MultiplayerGameApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val ablyGame: AblyGame by lazy {
        AblyGame.Builder(getString(R.string.ably_key))
            .scope(applicationScope)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        applicationScope.launch {
            //observe game state
            ablyGame.start().collect{
                Log.d(TAG, "onCreate: ")
            }
        }
    }

    companion object {
        lateinit var instance: MultiplayerGameApp
            private set
    }
}