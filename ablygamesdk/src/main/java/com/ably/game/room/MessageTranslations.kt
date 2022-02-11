package com.ably.game.room

import io.ably.lib.types.Message

internal fun GameMessage.ablyMessage(clientId: String): Message {
    //for now all message content is the same, but this should be allowed to change
    return Message(messageType.toString(), this.messageContent, clientId)
}

internal fun Message.gameMessage(): GameMessage {
    //for now all message content is the same, but this should be allowed to change
    return GameMessage(messageType = MessageType.valueOf(this.name), this.data as String)
}