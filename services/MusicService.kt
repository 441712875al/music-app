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
import com.example.music_app.pojo.LrcInfo
import com.example.music_app.utils.LrcParser
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
        val remoteViews = configRemoteViews()


        /*是否需要创建通知渠道*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel("my_service","前台通知", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }


        /*创建点击通知栏的意图，回到主活动*/
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this,0,intent,0)


        /*创建通知栏*/
        val notification = NotificationCompat.Builder(this,"my_service")
            .setContentTitle("网抑晕音乐")
            .setContentText("内容")
            .setSmallIcon(R.mipmap.music_app_icon)
            .setLargeIcon(BitmapFactory.decodeResource(resources,R.drawable.avatar))
            .setContent(remoteViews)
            .setContentIntent(pi)
            .build()
        startForeground(MusicPlayBinder.notificationID,notification)

        musicPlayBinder = MusicPlayBinder(manager,notification)
    }



    /**
     * 注册通知栏的音乐切换的监听器
     * @return 返回一个加载好通知栏视图资源的RemoteViews对象
     */
    private fun configRemoteViews() : RemoteViews{

        val remoteViews = RemoteViews(packageName, R.layout.music_notification)

        /*创建音乐切换的意图*/
        val intentTStart = Intent(MusicSwitchBroacast.START_MUSIC)
        val intentStop = Intent(MusicSwitchBroacast.STOP_MUSIC)
        val intentNext = Intent(MusicSwitchBroacast.NEXT_MUSIC)
        val intentPrecious = Intent(MusicSwitchBroacast.PRECIOUS_MUSIC)

        /*实例化广播*/
        val piStart = PendingIntent.getBroadcast(this,0,intentTStart,0)
        val piStop = PendingIntent.getBroadcast(this,0,intentStop,0)
        val piNext = PendingIntent.getBroadcast(this,0,intentNext,0)
        val piPrecious = PendingIntent.getBroadcast(this,0,intentPrecious,0)

        /*创建监听器，实现当点击通知栏的音乐切换按钮后发送广播*/
        remoteViews.setOnClickPendingIntent(R.id.notifyStart,piStart)
        remoteViews.setOnClickPendingIntent(R.id.notifyStop,piStop)
        remoteViews.setOnClickPendingIntent(R.id.notifyPrecious,piPrecious)
        remoteViews.setOnClickPendingIntent(R.id.notifyNext,piNext)
        return remoteViews
    }



    override fun onDestroy() {
        super.onDestroy()
        Log.e("MusicService ->","onDestroy()")
    }



    /**
     * 处理音乐播放的Binder
     * @author along
     * @param notificationManager 通知管理者
     * @param notification 需要创建的通知体
     */
    class MusicPlayBinder(val notificationManager: NotificationManager,
    val notification: Notification) : Binder(){

        var mediaPlayer = MediaPlayer()

        private val lrcParser = LrcParser()

        companion object{
            @JvmStatic
            var reflushTime = 1L
            @JvmStatic
            val notificationID = 1000
        }


        lateinit var musicList : List<Music>

        lateinit var assetsManager:AssetManager

        lateinit var lrcInfo: LrcInfo

        var musicIx = 0


        /**
         * 初始化参数
         * @param musicList 模拟音乐数据库
         * @param musicIx 当前选择的音乐id
         * @param assetManager 资源管理器，用来加载asset文件夹的资源
         */
        fun init(musicList:List<Music>,musicIx:Int,assetManager: AssetManager){
            this.musicList = musicList
            this.musicIx = musicIx
            this.assetsManager = assetManager
        }


        /**
         * 做一些准备播放的工作，如销毁前一个播放的mediaPlayer（如果存在的话）、
         * 加载MP3和lrc文件、重新定义通知栏的内容
         */
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
            lrcInfo = lrcParser.parser(assetsManager.open("${musicList[musicIx].resName}.lrc"))

            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(fileDescriptor.fileDescriptor,fileDescriptor.startOffset,fileDescriptor.length)
            mediaPlayer.prepare()
            val remoteViews = notification.contentView

            remoteViews.setTextViewText(R.id.notifyMusicName,musicList[musicIx].name)
            remoteViews.setTextViewText(R.id.notifyMusicAuthor,musicList[musicIx].author)
            remoteViews.setImageViewResource(R.id.notifyCover,musicList[musicIx].imageId)
            notificationManager.notify(notificationID,notification)
        }




        /**
         * 开始播放音乐
         */
        fun startPlay(){
            mediaPlayer.start()
        }



        /**
         * 停止播放音乐
         */
        fun stopPlay(){
            mediaPlayer.pause()
        }



        /**
         * 切换下一首音乐
         */
        fun nextMusic(){
            if(musicIx<musicList.size){
                ++musicIx
                preparePlay()
                startPlay()
            }
        }



        /**
         * 切换上一首音乐
         */
        fun preciousMusic(){
            if(musicIx>0){
                --musicIx
                preparePlay()
                startPlay()
            }
        }


    }
}
