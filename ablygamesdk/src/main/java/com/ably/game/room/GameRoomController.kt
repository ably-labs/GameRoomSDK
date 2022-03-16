package com.ably.game.room

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ErrorInfo
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
    data class Success(val toWhom: GamePlayer? = null, val toWhere: GameRoom? = null) : MessageSentResult()
    data class Failed(val toWhom: GamePlayer? = null, val toWhere: GameRoom? = null, val exception: Exception?) :
        MessageSentResult()
}

data class ReceivedMessage(val from: GamePlayer, val message: GameMessage)

sealed class RoomPresenceUpdate {
    data class Enter(val player: GamePlayer) : RoomPresenceUpdate()
    data class Leave(val player: GamePlayer) : RoomPresenceUpdate()
}

interface GameRoomController {
    suspend fun numberOfPeopleInRoom(gameRoom: GameRoom): Int
    suspend fun enter(player: GamePlayer, gameRoom: GameRoom): EnterRoomResult
    suspend fun leave(player: GamePlayer, gameRoom: GameRoom): LeaveRoomResult

    suspend fun sendMessageToPlayer(
        from: GamePlayer,
        to: GamePlayer,
        message: GameMessage
    ): MessageSentResult

    suspend fun sendMessageToRoom(
        from: GamePlayer,
        to: GameRoom,
        message: GameMessage
    ): MessageSentResult

    fun registerToPlayerMessagesInRoom(
        room: GameRoom,
        receiver: GamePlayer,
        messageType: MessageType
    ): Flow<ReceivedMessage>

    //register to messages sent to room
    fun registerToRoomMessages(
        room: GameRoom,
        messageType: MessageType
    ): Flow<ReceivedMessage>

    suspend fun unregisterFromPlayerMessagesInRoom(room: GameRoom, receiver: GamePlayer): Result<Unit>
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

    override suspend fun sendMessageToRoom(from: GamePlayer, to: GameRoom, message: GameMessage): MessageSentResult {
        val ablyMessage = message.ablyMessage(from.id)
        val channelId = roomChannel(to)
        println("Sending message over $channelId")
        return suspendCoroutine { continuation ->
            ably.channels[channelId]
                .publish(ablyMessage, object : CompletionListener {
                    override fun onSuccess() {
                        continuation.resume(MessageSentResult.Success(toWhere = to))
                    }

                    override fun onError(reason: ErrorInfo?) {
                        continuation.resume(
                            MessageSentResult.Failed(
                                toWhere = to,
                                exception = AblyException
                                    .fromErrorInfo
                                        (reason)
                            )
                        )
                    }
                })
        }
    }

    override suspend fun sendMessageToPlayer(from: GamePlayer, to: GamePlayer, message: GameMessage):
            MessageSentResult {
        val ablyMessage = message.ablyMessage(from.id)
        val channelId = unidirectionalPlayerChannel(from, to)
        println("Sending message over $channelId")
        return suspendCoroutine { continuation ->
            ably.channels[channelId]
                .publish(ablyMessage, object : CompletionListener {
                    override fun onSuccess() {
                        continuation.resume(MessageSentResult.Success(to))
                    }

                    override fun onError(reason: ErrorInfo?) {
                        continuation.resume(
                            MessageSentResult.Failed(
                                toWhom = to, exception = AblyException
                                    .fromErrorInfo(reason)
                            )
                        )
                    }
                })
        }

    }

    override fun registerToRoomMessages(room: GameRoom, messageType: MessageType): Flow<ReceivedMessage> {
        return callbackFlow {
            val channelId = roomChannel(room)
            System.out.println("Registering to channel $channelId")
            ably.channels[channelId].subscribe(messageType.toString()) { message ->
                println("Messaged received from ${message.clientId}")
                trySend(ReceivedMessage(DefaultGamePlayer(message.clientId), message.gameMessage()))
            }
            awaitClose { cancel() }
        }
    }

    override fun registerToPlayerMessagesInRoom(
        room: GameRoom,
        receiver: GamePlayer,
        messageType: MessageType
    ): Flow<ReceivedMessage> {
        return callbackFlow {
            val allPlayers = allPlayers(room)
            allPlayers.filter { player -> receiver.id != player.id } //do not create a channel between self-self
                .forEach { from ->
                    val channelId = unidirectionalPlayerChannel(from, receiver)
                    System.out.println("Registering to channel $channelId")
                    ably.channels[channelId].subscribe(messageType.toString()) { message ->
                        println("Messaged received from ${from.id}")
                        trySend(ReceivedMessage(from, message.gameMessage()))
                    }
                }
            awaitClose { cancel() }

        }

    }

    override suspend fun unregisterFromPlayerMessagesInRoom(room: GameRoom, receiver: GamePlayer): Result<Unit> {
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