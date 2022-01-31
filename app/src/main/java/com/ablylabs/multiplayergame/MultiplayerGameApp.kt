package com.ablylabs.multiplayergame

import android.app.Application
import android.util.Log
import com.ablylabs.ablygamesdk.AblyGame


private const val TAG = "MultiplayerGameApp"

class MultiplayerGameApp : Application() {
    lateinit var ablyGameBuilder: AblyGame.Builder

    override fun onCreate() {
        super.onCreate()
        ablyGameBuilder = AblyGame.Builder(getString(R.string.ably_key))

        instance = this
    }

    companion object {
        lateinit var instance: MultiplayerGameApp
            private set
    }
}