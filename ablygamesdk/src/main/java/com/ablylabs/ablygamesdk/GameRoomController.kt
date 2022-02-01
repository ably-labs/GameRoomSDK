package com.ablylabs.ablygamesdk

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ErrorInfo
import kotlinx.coroutines.flow.Flow
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class RoomPresenceResult {
    data class Success(val gameRoom: GameRoom, val player: GamePlayer) : RoomPresenceResult()
    data class Failure(val gameRoom: GameRoom, val player: GamePlayer, val exception:Exception?) : RoomPresenceResult()
}

//make result type names a bit more clear
typealias LeaveRoomResult = RoomPresenceResult
typealias EnterRoomResult = RoomPresenceResult

sealed class MessageSentResult {
    data class Success(val toWhom: GamePlayer) : MessageSentResult()
    data class Failed(val toWhom: GamePlayer, val reason: String) : MessageSentResult()
}

data class ReceivedMessage(val from: GamePlayer, val message: String)

sealed class RoomPresenceUpdate {
    data class Enter(val player: GamePlayer) : RoomPresenceUpdate()
    data class Leave(val player: GamePlayer) : RoomPresenceUpdate()
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

internal const val roomNamespace = "room"
internal const val playerNamespace = "player"
internal fun roomChannel(gameRoom: GameRoom) = "room:${gameRoom.id}"

internal class GameRoomControllerImpl(private val ably: AblyRealtime) : GameRoomController {
    override suspend fun numberOfPeopleInRoom(gameRoom: GameRoom): Int {
        return suspendCoroutine { continuation ->
            continuation.resume(ably.channels[roomChannel(gameRoom)].presence.get().size)
        }
    }

    override suspend fun enter(player: GamePlayer, gameRoom: GameRoom): EnterRoomResult {
        return suspendCoroutine { continuation ->
            ably.channels[roomChannel(gameRoom)].presence.apply {
                enterClient(player.id, "no_data", object : CompletionListener {
                    override fun onSuccess() {
                        continuation.resume(RoomPresenceResult.Success(gameRoom,player))
                    }

                    override fun onError(reason: ErrorInfo?) {
                        continuation.resume(RoomPresenceResult.Failure(gameRoom,player,AblyException.fromErrorInfo(reason)))
                    }
                })

            }
        }
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