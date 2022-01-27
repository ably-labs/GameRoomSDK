package com.ablylabs.ablygamesdk

interface AblyGame {
    //enter game --global
    fun enter(player: GamePlayer)
    fun exit(player: GamePlayer)
}