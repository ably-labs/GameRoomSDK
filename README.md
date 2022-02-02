# GameRoomSDK
A multiplayer game room SDK using Ably as communication platform

Note: Work in progress

This SDK is intended to provide some common functionality / interface to multiplayer games involving rooms/lobbies. The aim is to reduce the engineering effort involving realtime events in this domain.

This SDK currently uses a language of games, but please note that we can change the terminology to be a bit more wider.

# Features 
(Each point should be supported by code sample when SDK is a bit more mature)
* Create / initiate an environment that will allow streaming of realtime events
* Enter / leave a game -> Enter into a multiplayer game intended to notify other players
* Observe enters to / leave from game
* Enter into a room / leave from a room
* Listen to presence updates when in a room
* Register to messages from other players in a room
* Send messages to other players in a room
* List all players within a room

This repo comes with an example app that is intended to showcase SDK functionality.
Please add following line to your ```local.properties``` file to run example app

```ABLY_KEY={YOUR ABLY API KEY}```