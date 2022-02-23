_[Ably](https://ably.com) is the platform that powers synchronized digital experiences in realtime. Whether attending an event in a virtual venue, receiving realtime financial information, or monitoring live car performance data – consumers simply expect realtime digital experiences as standard. Ably provides a suite of APIs to build, extend, and deliver powerful digital experiences in realtime for more than 250 million devices across 80 countries each month. Organizations like Bloomberg, HubSpot, Verizon, and Hopin depend on Ably’s platform to offload the growing complexity of business-critical realtime data synchronization at global scale. For more information, see the [Ably documentation](https://ably.com/documentation)._

## Overview

Ably GameRoomSDK provides an easy way to manage multiplayer game room related events such as entering game rooms,
sending messages to other players, registering to presence events and more using  [Ably](https://ably.com/) realtime
network

**Status:** this is an alpha version of the SDK. It means that it contains some functionality and APIs that can
change without notice.

GameRoomSDK has following features
* Create / initiate a game environment that allows SDK to serve client functions.
* Start a game : This starts a game and enables players to enter to / leave from a game.
* Query for current players in the game.
* Observe entries / exits from a game.
* Enter into a game.
* Enter into a room in a game,  leave from a room in a game.
* Listen to presence actions in a room.
* Register to messages from other players in a room.
* Send messages to other players in a room.
* List all players that are in a room.

Ably GameRoomSDK is a pure Kotlin library and APIs are heavily based on [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## Example Apps

This repo comes with an [example app](app/) that is intended to showcase SDK functionality.
Please add following line to your ```local.properties``` file to run example app

- ```ABLY_KEY={YOUR ABLY API KEY}```

## Usage

This library is not yet published on any major publishing platform and only been used with an Android app. But you
should be able to use it any Kotlin project by checking it out and adding it as a dependency module.

You can clone library using
```
git clone https://github.com/ably-labs/GameRoomSDK.git
```
This will clone the multi module Gradle project including SDK and example app. The easiest way is to examine the
example app to see how you can add SDK as a dependencey.

If your project is in the same directory as ```ablygamesdk``` is add following to your ```settings.gradle``` file
```
include ':ablygamesdk'
```
Then in your app level build.gradle file add following

```
implementation project(path: ':ablygamesdk')
```

### AblyGame example usages

Following are some examples of how to use functions that are provided by ```AblyGame```

#### Game environment initialisation
You will need to initialise a game environment in order to activate SDK functionality

```kotlinlang
val ablyGame = AblyGame.Builder(ABLY_API_KEY)
            .scope(GAME_SCOPE)
            .build()

```
You should replace ```ABLY_API_KEY``` with your own. Please login into [Ably dashboard](https://ably.com/login) to retrieve
 your own api key.
 ```GAME_SCOPE``` is coroutine scope that you intend to run your AblyGame instance in. A Typical application scope
 for that matter could be defined as
 ```
 val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
 ```

 #### Start a game

```kotlinlang
ablyGame.start {
            when(it){
                AblyGame.GameState.Started -> //TODO replace this with your action of game started
                AblyGame.GameState.Stopped -> //TODO replace this with your action of game stopped
            }
        }
```
As you can see this method accepts a ```FlowCollector<GameState>``` for you to observe the changes on game state. You
should make sure that the game is started before moving forward with other game functionalities

#### Enter a game

```
someCoroutineScope.launch {
            val enterResult = ablyGame.enter(gamePlayer)
            if (enterResult.isSuccess) {
               //enter successful
            } else {
               //enter failed
            }
        }
```
As you can see, you should invoke ```enter``` inside a coroutine scope.
You must replace ```gamePlayer``` with your own instance of ```GamePlayer```

#### Leave a game

```kotlinlang
someCoroutineScope.launch {
            val leaveResult = ablyGame.leave(gamePlayer)
            if (leaveResult.isSuccess) {
                //leave successful
            } else {
               //leave failed
            }
        }
```
You must replace ```someCoroutineScope``` with scope your are launching the function.
You must replace ```gamePlayer``` with your own instance of ```GamePlayer```

#### Subscribing to game player updates in the game
If you want to observe users entering / leaving a game, you can use the following example code

```kotlinlang
ablyGame.subscribeToGamePlayerUpdates {
            when (it) {
                is PresenceAction.Enter -> {
                    Log.d(TAG, "PresenceAction.Enter ${it.player.id}")
                }
                is PresenceAction.Leave -> {
                    Log.d(TAG, "PresenceAction.Leave ${it.player.id}")
                }
            }
        }
```

#### Get list of all players
You can get a list of all players in the game using the following code block
```
 coroutineScope.launch {
            val players = ablyGame.allPlayers()
       }
```
As ```allPlayers``` is a suspending function, you must call this from a coroutine scope

#### Check if a player is in a game
```
coroutineScope.launch {
            if (ablyGame.isInGame(gamePlayer)) {
               //do something
            }
}
```

### Game room related functions
All game room related functionality is inside ```GameRoomController```. You must get a handle of this to invoke
functionality related to game rooms

To get a handle of ```GameRoomController``` in your ```AblyGame``` instance you can simply use

```
val roomsController = ablyGame.roomsController
```

#### Enter a room
```
someCoroutineScope.launch{
val result = controller.enter(yourPlayer,yourRoom)
            when (result){
                is RoomPresenceResult.Success -> //successful
                is RoomPresenceResult.Failure -> //failure
            }
}
```

You must replace ```yourPlayer``` with your own instance of ```GamePlayer``` and ```yourGame``` with your own
instance of ```GameRoom```

#### Leave a room
```
someCoroutineScope.launch{
val result = controller.leave(yourPlayer,yourRoom)
            when (result){
                is RoomPresenceResult.Success -> //successful
                is RoomPresenceResult.Failure -> //failure
            }
}
```
You must replace ```yourPlayer``` with your own instance of ```GamePlayer``` and ```yourGame``` with your own
instance of ```GameRoom```

#### Send message to a room
You can directly send a message to a room so that all room participants can register to and receive later
```
someCoroutineScope.launch {
            val result = controller.sendMessageToRoom(player, room, GameMessage(messageContent = message))
            when(result){
                is MessageSentResult.Failed -> //message sent failed
                is MessageSentResult.Success -> //message sent is success
            }
        }
```

#### Register to room messages

```
someCoroutineScope.launch {
            controller.registerToRoomMessages(room,MessageType.TEXT).collect{ receivedMessage ->
                // receivedMessage is received as ReceivedMessage
            }
        }
```

#### Register to player messages in the room

If you want to receive messages from all players in room, you can simply use following
```
someCoroutineScope.launch {
            controller.registerToPlayerMessagesInRoom(which, who, MessageType.TEXT).collect { receivedMessage ->
                //received message from receivedMessage.from
            }
        }
```

#### Send message to another player
```
someCoroutineScope.launch {
            val result = controller.sendMessageToPlayer(who, toWhom, GameMessage(messageContent = message))
            when(result){
                is MessageSentResult.Failed -> TODO()
                is MessageSentResult.Success -> TODO()
            }
        }
```

#### Register to presence events in a room
You can observe presence events in a room (currently supporting enter and leave) so you can react to it, for example
notifying other players or updating players in your room.

```
controller.registerToPresenceEvents(which).collect { roomPresenceUpdate ->
         //do something with roomPresenceUpdate
      }
```

#### Query all players in a room
You can query all players in a room like following. Notice that ```allPlayers()``` is a suspending function and needs
 to be called from a coroutine scope.
```
someCoroutineScope.launch{
   val allPlayers = allPlayers(room)
}
```





