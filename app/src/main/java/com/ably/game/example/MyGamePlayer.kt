package com.ably.game.example

import com.ably.game.room.GamePlayer

class MyGamePlayer(private val name: String) : GamePlayer {
    override val id: String
        get() = name
}