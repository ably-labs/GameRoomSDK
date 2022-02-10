package com.ably.game.room

interface GamePlayer {
    val id:String
}
data class DefaultGamePlayer(override val id: String) : GamePlayer