package com.ablylabs.ablygamesdk

import kotlinx.coroutines.flow.Flow

sealed class EnterRoomResult {
    object Success : EnterRoomResult()
    data class Failed(val reason: String) : EnterRoomResult()
}

sealed class LeaveRoomResult {
    object Success : LeaveRoomResult()
    data class Failed(val reason: String) : LeaveRoomResult()
}

sealed class MessageSentResult {
    data class Success(val toWhom: GamePlayer) : MessageSentResult()
    data class Failed(val toWhom: GamePlayer, val reason: String) : MessageSentResult()
}

data class ReceivedMessage(val from: GamePlayer, val message: String)

sealed class RoomPresenceUpdate{
    data class Enter(val player:GamePlayer):RoomPresenceUpdate()
    data class Leave(val player:GamePlayer):RoomPresenceUpdate()
}

interface GameRoomController {
    suspend fun numberOfPeopleInRoom(gameRoom: GameRoom): Int
    suspend fun enter(player: GamePlayer, gameRoom: GameRoom): EnterRoomResult
    suspend fun leave(player: GamePlayer, gameRoom: GameRoom): LeaveRoomResult
    suspend fun sendTextMessage(
        from: GamePlayer,
        to: GamePlayer,
        messageText: String
    ): MessageSentResult

    suspend fun registerToTextMessage(room: GameRoom, receiver: GamePlayer): Flow<ReceivedMessage>
    suspend fun allPlayers(inWhich: GameRoom): List<GamePlayer>
    suspend fun registerToPresenceEvents(gameRoom: GameRoom): Flow<RoomPresenceUpdate>
    suspend fun unregisterFromPresenceEvents(room: GameRoom)
}
internal class GameRoomControllerImpl:GameRoomController{
    override suspend fun numberOfPeopleInRoom(gameRoom: GameRoom): Int {
        TODO("Not yet implemented")
    }

    override suspend fun enter(player: GamePlayer, gameRoom: GameRoom): EnterRoomResult {
        TODO("Not yet implemented")
    }

    override suspend fun leave(player: GamePlayer, gameRoom: GameRoom): LeaveRoomResult {
        TODO("Not yet implemented")
    }

    override suspend fun sendTextMessage(from: GamePlayer, to: GamePlayer, messageText: String): MessageSentResult {
        TODO("Not yet implemented")
    }

    override suspend fun registerToTextMessage(room: GameRoom, receiver: GamePlayer): Flow<ReceivedMessage> {
        TODO("Not yet implemented")
    }

    override suspend fun allPlayers(inWhich: GameRoom): List<GamePlayer> {
        TODO("Not yet implemented")
    }

    override suspend fun registerToPresenceEvents(gameRoom: GameRoom): Flow<RoomPresenceUpdate> {
        TODO("Not yet implemented")
    }

    override suspend fun unregisterFromPresenceEvents(room: GameRoom) {
        TODO("Not yet implemented")
    }

}