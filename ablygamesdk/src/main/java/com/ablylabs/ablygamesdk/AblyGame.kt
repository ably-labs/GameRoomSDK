package com.ablylabs.ablygamesdk

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.types.AblyException
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.PresenceMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val GLOBAL_CHANNEL_NAME = "global"

enum class PresenceAction { ENTER, LEAVE }

class AblyGame private constructor(apiKey: String, val scope: CoroutineScope, gameOn: (ablyGame: AblyGame) -> Unit) {

    class Builder(private val apiKey: String) {
        private lateinit var _scope: CoroutineScope
        suspend fun build(): AblyGame {
            if (!(this::_scope.isInitialized)){
                throw Exception("scope is not provided")
            }
            return suspendCoroutine { continuation ->
                AblyGame(apiKey,_scope) { game ->
                    continuation.resume(game)
                }
            }
        }

        fun scope(scope: CoroutineScope): Builder {
            this._scope = scope
            return this
        }
    }

    private val ably: AblyRealtime = AblyRealtime(apiKey)
    val roomsController: GameRoomController = GameRoomControllerImpl(ably)

    init {
        ably.connection.on { state ->
            when (state.current) {
                ConnectionState.connected -> gameOn(this)
            }
        }
    }

    //enter game --global
    suspend fun enter(player: GamePlayer): Result<Unit> {
        return suspendCoroutine { continuation ->
            ably.channels[GLOBAL_CHANNEL_NAME].presence.run {
                //unable to use this api without data
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

    suspend fun leave(player: GamePlayer): Result<Unit> {
        return suspendCoroutine { continuation ->
            ably.channels[GLOBAL_CHANNEL_NAME].presence.run {
                leaveClient(player.id, object : CompletionListener {
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

    suspend fun numberOfPlayers(): Int {
        return suspendCoroutine { continuation ->
            continuation.resume(ably.channels[GLOBAL_CHANNEL_NAME].presence.get().size)
        }
    }


    fun subscribeToPlayerNumberUpdate(updated: (action: PresenceAction) -> Unit) {
        val observedActions = EnumSet.of(PresenceMessage.Action.enter, PresenceMessage.Action.leave)
        ably.channels[GLOBAL_CHANNEL_NAME].presence.subscribe(observedActions) {
            when (it.action) {
                PresenceMessage.Action.enter -> updated(PresenceAction.ENTER)
                PresenceMessage.Action.leave -> updated(PresenceAction.LEAVE)
            }
        }

    }
}