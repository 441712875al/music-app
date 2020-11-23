package com.example.music_app.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.ImageView
import com.example.music_app.MainActivity
import com.example.music_app.R

class MusicSwitchBroacast(val activity: MainActivity):BroadcastReceiver() {
    companion object{
        @JvmStatic
        val START_MUSIC = "com.example.music_app.START_MUSIC"
        val STOP_MUSIC = "com.example.music_app.STOP_MUSIC"
        val NEXT_MUSIC = "com.example.music_app.NEXT_MUSIC"
        val PRECIOUS_MUSIC = "com.example.music_app.PRECIOUS_MUSIC"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action){
            START_MUSIC -> activity.musicFragment.view?.findViewById<ImageView>(R.id.play)
                ?.callOnClick()

            STOP_MUSIC -> activity.musicFragment.view?.findViewById<ImageView>(R.id.play)
                ?.callOnClick()

            NEXT_MUSIC ->activity.musicFragment.view?.findViewById<ImageView>(R.id.next)
                ?.callOnClick()

            PRECIOUS_MUSIC ->activity.musicFragment.view?.findViewById<ImageView>(R.id.precious)
                ?.callOnClick()
        }
    }
}