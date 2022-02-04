package com.ablylabs.ablygamesdk

interface GamePlayer {
    val id:String
}
data class DefaultGamePlayer(override val id: String) :GamePlayer