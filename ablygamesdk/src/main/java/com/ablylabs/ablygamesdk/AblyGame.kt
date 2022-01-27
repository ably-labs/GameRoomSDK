package com.ablylabs.ablygamesdk

import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ErrorInfo
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val GLOBAL_CHANNEL_NAME = "global"
class AblyGame(private val apiKey:String){
    private val ably = AblyRealtime(apiKey)

    //enter game --global
    suspend fun enter(player: GamePlayer):Result<Unit>{
        return suspendCoroutine {continuation->
            ably.channels[GLOBAL_CHANNEL_NAME].presence.run {
                enterClient(player.id, null, object : CompletionListener {
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
    suspend fun leave(player: GamePlayer):Result<Unit>{
        return suspendCoroutine {continuation->
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
}