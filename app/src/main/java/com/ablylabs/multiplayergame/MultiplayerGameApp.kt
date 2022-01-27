package com.ablylabs.multiplayergame

import android.app.Application
import com.ablylabs.ablygamesdk.AblyGame

class MultiplayerGameApp : Application() {
    private lateinit var _ablyGame: AblyGame
    val ablyGame: AblyGame
        get() = _ablyGame

    override fun onCreate() {
        super.onCreate()
        _ablyGame = AblyGame(getString(R.string.ably_key))
        instance = this
    }
    companion object {
        lateinit var instance: MultiplayerGameApp
            private set
    }
}