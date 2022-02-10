package com.ably.game.example

import com.ably.game.room.GameRoom

class MyGameRoom(val name:String): GameRoom {
    override val id: String
        get() = name
}