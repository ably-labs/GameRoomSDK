package com.ablylabs.pubcrawler.ui

import android.util.Log
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.ably.game.room.EnterRoomResult
import com.ably.game.room.GameMessage
import com.ably.game.room.GamePlayer
import com.ably.game.room.GameRoom
import com.ably.game.room.GameRoomController
import com.ably.game.room.LeaveRoomResult
import com.ably.game.room.MessageSentResult
import com.ably.game.room.MessageType
import com.ably.game.room.ReceivedMessage
import com.ably.game.room.RoomPresenceResult
import com.ably.game.room.RoomPresenceUpdate

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "GameRoomViewModel"

class GameRoomViewModel(private val controller: GameRoomController) : ViewModel() {
    //following needs to be transformed from flows, they need not to be exposed like this
    private val _presenceActions = MutableLiveData<RoomPresenceUpdate>()
    val presenceActions: LiveData<RoomPresenceUpdate> = _presenceActions

    //
    //following needs to be transformed from flows, they need not to be exposed like this
    private val _receivedUserMessages = MutableLiveData<ReceivedMessage>()
    val receivedUserMessages: LiveData<ReceivedMessage> = _receivedUserMessages

    private val _receivedRoomMessages = MutableLiveData<ReceivedMessage>()
    val receivedRoomMessages: LiveData<ReceivedMessage> = _receivedRoomMessages

    private val _leaveResult = MutableLiveData<LeaveRoomResult>()
    val leaveResult: LiveData<LeaveRoomResult> = _leaveResult

    private val _enterResult = MutableLiveData<EnterRoomResult>()
    val enterResult: LiveData<EnterRoomResult> = _enterResult

    private val _messageSentResult = MutableLiveData<MessageSentResult>()
    val messageSentResult: LiveData<MessageSentResult> = _messageSentResult

    private val _allPlayers = MutableLiveData<List<GamePlayer>>()
    val allPlayers: LiveData<List<GamePlayer>> = _allPlayers

    private var actionJob: Job? = null

    fun leaveRoom(who: GamePlayer, which: GameRoom) {
        viewModelScope.launch {
            _leaveResult.value = controller.leave(who, which)
            //also manually unsubscribe
            controller.unregisterFromPlayerMessagesInRoom(which, who)

            if (_leaveResult.value is LeaveRoomResult) {
                _allPlayers.value = controller.allPlayers(which)
            }
        }
    }

    fun enterRoom(who: GamePlayer, which: GameRoom) {
        Log.d(TAG, "enterRoom: before launch")
        viewModelScope.launch {
            Log.d(TAG, "enterRoom: entering")
            _enterResult.value = controller.enter(player = who, gameRoom = which)
            Log.d(TAG, "enterRoom: enter result :${enterResult.value}")
            _allPlayers.value = controller.allPlayers(which)
            Log.d(TAG, "enterRoom: all players ${_allPlayers.value?.size}")
            if (_enterResult.value is RoomPresenceResult.Success) {
                actionJob = launch { buildActionFlowFor(which, who) }
                launch { buildPresenceFlow(which, who) }
            }
        }
    }

    private suspend fun buildPresenceFlow(
        which: GameRoom,
        who: GamePlayer
    ) {
        controller.registerToPresenceEvents(which).collect {
            Log.d(TAG, "buildPresenceFlow: presence flow collect")
            _presenceActions.value = it
            //also refresh users again
            _allPlayers.value = controller.allPlayers(which)
            //rebuild action flow
            viewModelScope.launch {
                //first cancel the first job
                actionJob?.cancel()
                actionJob = launch {
                    buildActionFlowFor(which, who)
                }
            }
        }
    }

    private suspend fun buildActionFlowFor(
        which: GameRoom,
        who: GamePlayer
    ) {
        viewModelScope.launch {
            controller.registerToPlayerMessagesInRoom(which, who, MessageType.TEXT).collect {
                _receivedUserMessages.value = it
            }
        }
        viewModelScope.launch {
            controller.registerToRoomMessages(which,MessageType.TEXT).collect{
                _receivedRoomMessages.value = it
            }
        }
    }

    fun sendTextMessage(who: GamePlayer, toWhom: GamePlayer, message: String) {
        viewModelScope.launch {
            _messageSentResult.value =
                controller.sendMessageToPlayer(who, toWhom, GameMessage(messageContent = message))
        }
    }

    fun sendMessageToRoom(player: GamePlayer, room: GameRoom, message: String) {
        viewModelScope.launch {
            // _messageSentResult.value =
            controller.sendMessageToRoom(player, room, GameMessage(messageContent = message))
        }
    }
}

class GameRoomViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val controller: GameRoomController
) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel> create(
        key: String, modelClass: Class<T>, handle: SavedStateHandle
    ): T {
        return GameRoomViewModel(controller) as T
    }
}