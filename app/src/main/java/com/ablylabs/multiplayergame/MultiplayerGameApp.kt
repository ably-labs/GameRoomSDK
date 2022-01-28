package com.ablylabs.multiplayergame

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import com.ablylabs.ablygamesdk.AblyGame


private const val TAG = "MultiplayerGameApp"

class MultiplayerGameApp : Application() {
    private lateinit var _ablyGame: AblyGame
    val ablyGame: AblyGame
        get() = _ablyGame

    override fun onCreate() {
        super.onCreate()
        AblyGame(getString(R.string.ably_key)) {
            _ablyGame = it
            Log.d(TAG, "game is ready ")
        }
        instance = this
    }

    companion object {
        lateinit var instance: MultiplayerGameApp
            private set
    }
}