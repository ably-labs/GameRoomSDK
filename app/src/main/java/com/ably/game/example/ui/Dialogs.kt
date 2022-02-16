package com.ably.game.example.ui

import android.annotation.SuppressLint
import android.content.Context
import com.ably.game.example.MyGamePlayer
import com.ably.game.room.GamePlayer
import com.ablylabs.multiplayergame.R

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input

fun checkName(context: Context, named: (name: String) -> Unit) {
    val prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    prefs.getString("name", null)?.let {
        named(it)
    } ?: run {
        MaterialDialog(context).show {
            input(hint = context.getString(R.string.your_name)) { dialog, text ->
                prefs.edit().putString("name", text.toString()).apply()
                named(text.toString())
            }
            title(res = R.string.your_name)
            positiveButton(R.string.submit)
        }
    }
}

@SuppressLint("CheckResult")
fun showSendMessageDialog(context: Context, player: GamePlayer, messageProvided: (messageString: String) -> Unit) {
        MaterialDialog(context).show {
            input(hint = context.getString(R.string.sendMessageTo, player.id)) { dialog, text ->
                messageProvided(text.toString())
            }
            title(text = context.getString(R.string.sendMessageTo_title, player.id))
            positiveButton(R.string.submit)
        }
}