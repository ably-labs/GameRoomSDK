package com.ablylabs.ablygamesdk

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.ChannelBase
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.Message
import io.ably.lib.types.PresenceMessage
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.Exception
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class RoomPresenceResult {
    data class Success(val gameRoom: GameRoom, val player: GamePlayer) : RoomPresenceResult()
    data class Failure(val gameRoom: GameRoom, val player: GamePlayer, val exception: Exception?) : RoomPresenceResult()
}

//make result type names a bit more clear
typealias LeaveRoomResult = RoomPresenceResult
typealias EnterRoomResult = RoomPresenceResult

sealed class MessageSentResult {
    data class Success(val toWhom: GamePlayer) : MessageSentResult()
    data class Failed(val toWhom: GamePlayer, val exception: Exception?) : MessageSentResult()
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

    //consider migrating message
    suspend fun sendMessageToPlayer(
        from: GamePlayer,
        to: GamePlayer,
        message: String
    ): MessageSentResult

    suspend fun registerToRoomMessages(room: GameRoom, receiver: GamePlayer): Flow<ReceivedMessage>
    suspend fun unregisterFromRoomMessages(room: GameRoom, receiver: GamePlayer): Result<Unit>
    suspend fun allPlayers(inWhich: GameRoom): List<GamePlayer>
    suspend fun registerToPresenceEvents(gameRoom: GameRoom): Flow<RoomPresenceUpdate>
    suspend fun unregisterFromPresenceEvents(room: GameRoom)
}

internal const val roomNamespace = "room"
internal const val playerNamespace = "player"
internal fun roomChannel(gameRoom: GameRoom) = "$roomNamespace:${gameRoom.id}"
internal fun unidirectionalPlayerChannel(player1: GamePlayer, player2: GamePlayer): String {
    return "$playerNamespace:${player1.id}-${player2.id}"
}

internal class GameRoomControllerImpl(
    private val ably: AblyRealtime
) : GameRoomController {
    override suspend fun numberOfPeopleInRoom(gameRoom: GameRoom): Int {
        return allPlayers(gameRoom).size
    }

    override suspend fun enter(player: GamePlayer, gameRoom: GameRoom): EnterRoomResult {
        println("enter thread before suspendCoroutine ${Thread.currentThread()}")
        return suspendCoroutine { continuation ->
            ably.channels[roomChannel(gameRoom)].presence.run {
                println("enter thread where enter called ${Thread.currentThread()}")
                enterClient(player.id, "no_data", object : CompletionListener {
                    override fun onSuccess() {
                        println("enter thread onSuccess ${Thread.currentThread()}")
                        continuation.resume(RoomPresenceResult.Success(gameRoom, player))
                    }

                    override fun onError(reason: ErrorInfo?) {
                        println("enter thread onError ${Thread.currentThread()}")
                        continuation.resume(
                            RoomPresenceResult.Failure(
                                gameRoom,
                                player,
                                AblyException.fromErrorInfo(reason)
                            )
                        )

                    }
                })

            }
        }
    }

    override suspend fun leave(player: GamePlayer, gameRoom: GameRoom): LeaveRoomResult {
        return suspendCoroutine { continuation ->
            ably.channels[roomChannel(gameRoom)].presence.run {
                leaveClient(player.id, "no_data", object : CompletionListener {
                    override fun onSuccess() {
                        continuation.resume(RoomPresenceResult.Success(gameRoom, player))
                    }

                    override fun onError(reason: ErrorInfo?) {
                        continuation.resume(
                            RoomPresenceResult.Failure(
                                gameRoom,
                                player,
                                AblyException.fromErrorInfo(reason)
                            )
                        )
                    }
                })

            }
        }
    }

    override suspend fun sendMessageToPlayer(from: GamePlayer, to: GamePlayer, messageText: String):
            MessageSentResult {
        //this message name should be derived from message when migrated
        val message = Message("text", messageText, from.id)
        //this should be a bidirectional channel between two players
        val channelId = unidirectionalPlayerChannel(from, to)
        return suspendCoroutine { continuation ->
            ably.channels[channelId]
                .publish(message, object : CompletionListener {
                    override fun onSuccess() {
                        continuation.resume(MessageSentResult.Success(to))
                    }

                    override fun onError(reason: ErrorInfo?) {
                        continuation.resume(MessageSentResult.Failed(to, AblyException.fromErrorInfo(reason)))
                    }
                })
        }

    }

    override suspend fun registerToRoomMessages(room: GameRoom, receiver: GamePlayer): Flow<ReceivedMessage> {
        return suspendCoroutine { continuation ->
            val flow = callbackFlow {
                allPlayers(room)
                    .filter { receiver != it } //do not create a channel between self-self
                    .forEach { from ->
                        val channelId = unidirectionalPlayerChannel(from, receiver)
                        System.out.println("Registering to channel $channelId")
                        ably.channels[channelId].subscribe("text") { message ->
                            println("Messaged received from ${from.id}")
                            trySend(ReceivedMessage(from, message.data as String))
                        }
                    }
                awaitClose { cancel() }
            }
            continuation.resume(flow)
        }

    }

    override suspend fun unregisterFromRoomMessages(room: GameRoom, receiver: GamePlayer): Result<Unit> {
        val allPlayers = allPlayers(room)
        return suspendCoroutine { continuation ->
                allPlayers.filter { receiver != it } //do not create a channel between self-self
                .forEach { from ->
                    val channelId = unidirectionalPlayerChannel(from, receiver)
                    System.out.println("Unregistering from $channelId")
                    ably.channels[channelId].unsubscribe()
                    continuation.resume(Result.success(Unit))
                }
        }
    }

    override suspend fun allPlayers(room: GameRoom): List<GamePlayer> {
        return suspendCoroutine { continuation ->
            val presenceMessages = ably.channels[roomChannel(room)].presence.get()
            presenceMessages?.let {
                continuation.resume(it.toList().map { DefaultGamePlayer(it.clientId) })
            }
        }
    }

    override suspend fun registerToPresenceEvents(gameRoom: GameRoom): Flow<RoomPresenceUpdate> {
        return suspendCoroutine { continuation ->
            val flow = callbackFlow {
                val observedActions = EnumSet.of(PresenceMessage.Action.enter, PresenceMessage.Action.leave)
                ably.channels[roomChannel(gameRoom)].presence.subscribe(observedActions) {
                    when (it.action) {
                        PresenceMessage.Action.enter -> trySend(RoomPresenceUpdate.Enter(DefaultGamePlayer(it.clientId)))
                        PresenceMessage.Action.leave -> trySend(RoomPresenceUpdate.Leave(DefaultGamePlayer(it.clientId)))
                    }
                }
                awaitClose { cancel() }
            }
            continuation.resume(flow)
        }
    }

    override suspend fun unregisterFromPresenceEvents(room: GameRoom) {
        ably.channels[room.id].presence.unsubscribe()
    }

}