_[Ably](https://ably.com) is the platform that powers synchronized digital experiences in realtime. Whether attending an event in a virtual venue, receiving realtime financial information, or monitoring live car performance data – consumers simply expect realtime digital experiences as standard. Ably provides a suite of APIs to build, extend, and deliver powerful digital experiences in realtime for more than 250 million devices across 80 countries each month. Organizations like Bloomberg, HubSpot, Verizon, and Hopin depend on Ably’s platform to offload the growing complexity of business-critical realtime data synchronization at global scale. For more information, see the [Ably documentation](https://ably.com/documentation)._

## Overview

Ably GameRoomSDK provides an easy way to manage multiplayer game room related events such as entering game rooms,
sending messages to other players, register to presence events and more using  [Ably](https://ably.com/) realtime network

**Status:** this is an alpha version of the SDK. That means that it contains some functionality and APIs that can
change without notice.  The latest release of the SDKs is available in the [Releases section](https://github.com/ably/ably-asset-tracking-android/releases) of this repository.

GameRoomSDK contains following features
* Create / initiate a game environment that allows SDK to serve client functions.
* Start a game : This starts a game and enables players to enter to / leave from a game
* Query for current players in the game
* Observe entries / exits from a game
* Enter into a game
* Enter into a room in a game,  leave from a room
* Listen to presence actions in a room
* Register to messages from other players in a room
* Send messages to other players in a room
* List all players within a room

Ably GameRoomSDK is a pure Kotlin library and APIs are heavily based on [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## Example Apps

This repo comes with an [example app](app/) that is intended to showcase SDK functionality.
Please add following line to your ```local.properties``` file to run example app

- ```ABLY_KEY={YOUR ABLY API KEY}```

## Usage

This library is not yet published on any major publishing platform and only been tested with Android apps. But you
should be able to use it any Kotlin project by adding checking it out and adding it as a dependency module

//TODO add some more info about how to setup the library

## Game environment initialisation
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
 ```val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)```

