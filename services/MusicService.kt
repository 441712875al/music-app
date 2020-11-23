package com.example.music_app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock.sleep
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.music.pojo.Music
import com.example.music_app.MainActivity
import com.example.music_app.R
import com.example.music_app.broadcasts.MusicSwitchBroacast
import java.lang.Exception

class MusicService : Service() {

    private lateinit var musicPlayBinder : MusicPlayBinder

    override fun onBind(intent: Intent): IBinder {
        return musicPlayBinder
    }

    override fun onCreate() {
        super.onCreate()
        Log.e("MusicService -> ","onCreate()")
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val remoteViews = RemoteViews(packageName, R.layout.music_notification)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel("my_service","前台通知", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this,0,intent,0)

        val intentTStart = Intent(MusicSwitchBroacast.START_MUSIC)
        val intentStop = Intent(MusicSwitchBroacast.STOP_MUSIC)
        val intentNext = Intent(MusicSwitchBroacast.NEXT_MUSIC)
        val intentPrecious = Intent(MusicSwitchBroacast.PRECIOUS_MUSIC)

        val piStart = PendingIntent.getBroadcast(this,0,intentTStart,0)
        val piStop = PendingIntent.getBroadcast(this,0,intentStop,0)
        val piNext = PendingIntent.getBroadcast(this,0,intentNext,0)
        val piPrecious = PendingIntent.getBroadcast(this,0,intentPrecious,0)

        remoteViews.setOnClickPendingIntent(R.id.notifyStart,piStart)
        remoteViews.setOnClickPendingIntent(R.id.notifyStop,piStop)
        remoteViews.setOnClickPendingIntent(R.id.notifyPrecious,piPrecious)
        remoteViews.setOnClickPendingIntent(R.id.notifyNext,piNext)


        val notification = NotificationCompat.Builder(this,"my_service")
            .setContentTitle("网抑晕音乐")
            .setContentText("内容")
            .setSmallIcon(R.drawable.home)
            .setLargeIcon(BitmapFactory.decodeResource(resources,R.drawable.avatar))
            .setContent(remoteViews)
            .setContentIntent(pi)
            .build()
        startForeground(MusicPlayBinder.notificationID,notification)

        musicPlayBinder = MusicPlayBinder(manager,notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("MusicService ->","onDestroy()")
    }


    class MusicPlayBinder(val notificationManager: NotificationManager,
    val notification: Notification) : Binder(){
        //加载音乐
        var mediaPlayer = MediaPlayer()

        companion object{
            @JvmStatic
            var reflushTime = 1L
            @JvmStatic
            val notificationID = 1000
        }


        lateinit var musicList : List<Music>

        lateinit var assetsManager:AssetManager

        var musicIx = 0

        fun init(musicList:List<Music>,musicIx:Int,assetManager: AssetManager){
            this.musicList = musicList
            this.musicIx = musicIx
            this.assetsManager = assetManager
        }


        fun preparePlay(){
            try{
                if(mediaPlayer.isPlaying){
                    mediaPlayer.stop()
                }
                sleep(reflushTime)
            }catch (e: Exception){
                //什么也不做
            }
            val fileDescriptor = assetsManager.openFd("${musicList[musicIx].resName}.mp3")

            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(fileDescriptor.fileDescriptor,fileDescriptor.startOffset,fileDescriptor.length)
            mediaPlayer.prepare()
            val remoteViews = notification.contentView

            remoteViews.setTextViewText(R.id.notifyMusicName,musicList[musicIx].name)
            remoteViews.setTextViewText(R.id.notifyMusicAuthor,musicList[musicIx].author)
            remoteViews.setImageViewResource(R.id.notifyCover,musicList[musicIx].imageId)
            notificationManager.notify(notificationID,notification)
        }

        fun startPlay(){
            mediaPlayer.start()
        }

        fun stopPlay(){
            mediaPlayer.pause()
        }

        fun nextMusic(){
            if(musicIx<musicList.size){
                ++musicIx
                preparePlay()
                startPlay()
            }
        }


        fun preciousMusic(){
            if(musicIx>0){
                --musicIx
                preparePlay()
                startPlay()
            }
        }
    }
}
