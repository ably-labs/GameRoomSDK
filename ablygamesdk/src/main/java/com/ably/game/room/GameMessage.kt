package com.ably.game.room

import io.ably.lib.types.Message

enum class MessageType { TEXT, REQUEST }
data class GameMessage(val messageType: MessageType = MessageType.TEXT, val messageContent: String)