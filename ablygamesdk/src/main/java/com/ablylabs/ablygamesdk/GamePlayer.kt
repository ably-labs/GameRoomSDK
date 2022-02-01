package com.ablylabs.ablygamesdk

interface GamePlayer {
    val id:String
    //a simple builder for given id
    companion object{
        fun of(id:String):GamePlayer{
            return object : GamePlayer {
                override val id: String
                    get() = id
            }
        }
    }
}