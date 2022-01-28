package com.ablylabs.multiplayergame

import com.ablylabs.ablygamesdk.GamePlayer

class MyGamePlayer(private val name: String) :GamePlayer {
    override val id: String
        get() = name
}