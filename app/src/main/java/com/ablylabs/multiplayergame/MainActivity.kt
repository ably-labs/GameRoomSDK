package com.ablylabs.multiplayergame

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.enterButton).setOnClickListener {
            lifecycleScope.launch {
                val result = MultiplayerGameApp.instance.ablyGame.enter(MyGamePlayer("ikbal"))
                Log.d(TAG, "enter result $result ")
            }
        }
    }
}