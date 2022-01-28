package com.ablylabs.multiplayergame

import com.ablylabs.ablygamesdk.GameRoom

class MyGameRoom(val name:String):GameRoom {
    override val id: String
        get() = name
}