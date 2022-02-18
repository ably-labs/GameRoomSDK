package com.ably.game.room

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.types.AblyException
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.PresenceMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val GLOBAL_CHANNEL_NAME = "global"

sealed class PresenceAction {
    data class Enter(val player: GamePlayer) : PresenceAction()
    data class Leave(val player: GamePlayer) : PresenceAction()
}

class AblyGame private constructor(private val apiKey: String, val scope: CoroutineScope) {

    class Builder(private val apiKey: String) {
        private lateinit var _scope: CoroutineScope
        fun build(): AblyGame {
            if (!(this::_scope.isInitialized)) {
                throw Exception("scope is not provided")
            }
            return AblyGame(apiKey, _scope)
        }

        fun scope(scope: CoroutineScope): Builder {
            this._scope = scope
            return this
        }
    }

    private val clientOptions = ClientOptions().apply {
        key = apiKey
        autoConnect = false //we set this to false to start game and setup listener for later
    }
    private val ably: AblyRealtime = AblyRealtime(clientOptions)
    val roomsController: GameRoomController = GameRoomControllerImpl(ably)

    enum class GameState { Idle, Started, Stopped }

    private var gameState = GameState.Idle

    fun start(collector: FlowCollector<GameState>) {
        scope.launch {
            startSuspending().collect(collector)
        }
    }

    private suspend fun startSuspending(): Flow<GameState> {
        return suspendCoroutine { continuation ->
            val flow = callbackFlow {
                ably.connection.on { state ->
                    when (state.current) {
                        ConnectionState.connected -> {
                            gameState = GameState.Started
                            trySend(GameState.Started)
                            System.out.println("Ably Game Started")
                        }
                        ConnectionState.closed -> {
                            gameState = GameState.Stopped
                            trySend(GameState.Stopped)
                            System.out.println("Ably Game stopped")

                        }
                        //other states are currently not of our interests
                    }
                }
                ably.connect()

                awaitClose { cancel() }
            }
            continuation.resume(flow)
        }

    }

    fun stop() {
        if (gameState == GameState.Started) {
            ably.close()
        }
    }

    //enter game --global
    suspend fun enter(player: GamePlayer): Result<Unit> {
        //first make sure the game has started
        if (!isActive()) {
            return Result.failure(IllegalStateException("enter failed as AblyGame is not started"))
        }
        return suspendCoroutine { continuation ->
            ably.channels[GLOBAL_CHANNEL_NAME].presence.run {
                enterClient(player.id, "no data", object : CompletionListener {
                    override fun onSuccess() {
                        scope.launch {
                            continuation.resume(Result.success(Unit))
                        }

                    }

                    override fun onError(reason: ErrorInfo?) {
                        continuation.resume(Result.failure(AblyException.fromErrorInfo(reason)))
                    }
                })

            }
        }
    }

    private fun isActive() = gameState == GameState.Started

    suspend fun leave(player: GamePlayer): Result<Unit> {
        return suspendCoroutine { continuation ->
            ably.channels[GLOBAL_CHANNEL_NAME].presence.run {
                leaveClient(player.id, "no_data", object : CompletionListener {
                    override fun onSuccess() {
                        continuation.resume(Result.success(Unit))
                    }

                    override fun onError(reason: ErrorInfo?) {
                        continuation.resume(Result.failure(AblyException.fromErrorInfo(reason)))
                    }
                })
            }
        }
    }

    suspend fun allPlayers(): List<GamePlayer> {
        return suspendCoroutine { continuation ->
            if (isActive()) {
                val players = ably.channels[GLOBAL_CHANNEL_NAME].presence.get().map { DefaultGamePlayer(it.clientId) }
                continuation.resume(players)
            } else {
                continuation.resume(emptyList())
            }

        }
    }

    fun subscribeToGamePlayerUpdates(collector: FlowCollector<PresenceAction>){
        scope.launch {
            subscibeToUpdatesSuspending().collect(collector)
        }
    }

    private suspend fun subscibeToUpdatesSuspending(): Flow<PresenceAction> {
        return suspendCoroutine { continuation ->
            val flow = callbackFlow {
                val observedActions = EnumSet.of(PresenceMessage.Action.enter, PresenceMessage.Action.leave)
                ably.channels[GLOBAL_CHANNEL_NAME].presence.subscribe(observedActions) {
                    when (it.action) {
                        PresenceMessage.Action.enter -> trySend(PresenceAction.Enter(DefaultGamePlayer(it.clientId)))
                        PresenceMessage.Action.leave -> trySend(PresenceAction.Leave(DefaultGamePlayer(it.clientId)))
                    }
                }
                awaitClose { cancel() }
            }
            continuation.resume(flow)
        }
    }

    suspend fun isInGame(gamePlayer: GamePlayer?): Boolean {
        if (!isActive()) return false
        if (gamePlayer == null) return false
        return allPlayers().find { it.id == gamePlayer.id } != null
    }
}