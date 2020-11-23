package com.example.music_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.example.music_app.services.MusicService
import kotlin.concurrent.thread

class MusicPlayActivity : AppCompatActivity() {

    private lateinit var mainActivity : MainActivity

    var musicIx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.music_frag)
        init()
        loadMusic(musicIx)
        configMusicSwitchBtn()
    }


    private fun init(){
        val params = intent.getBundleExtra("params")
        params?.apply {
            mainActivity = get("mainActivity") as MainActivity
            musicIx = getInt("selectedMusic")
        }
    }

    /**
     * 加载页面的音乐文件布局内容和音乐资源
     * @param toReflushMusicIx 需要被加载的音乐文件的索引号
     */
    fun loadMusic(toReflushMusicIx: Int){


        musicIx = toReflushMusicIx
        val music = mainActivity.musicList[musicIx]
        findViewById<ImageView>(R.id.music_cover).setImageResource(music.imageId)
        findViewById<TextView>(R.id.music_name).text = music.name
        findViewById<TextView>(R.id.music_author).text = music.author



        mainActivity.musicPlayBinder.preparePlay()
        configPregressBar()
        findViewById<ImageView>(R.id.play).setImageResource(R.drawable.start)
        mainActivity.musicPlayBinder.startPlay()
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        thread {
            while(mainActivity.musicPlayBinder.mediaPlayer.isPlaying){
                SystemClock.sleep(MusicService.MusicPlayBinder.reflushTime)
                val msg = Message.obtain()
                msg.obj = seekBar
                msg.what = mainActivity.updateProgress
//                Log.e("progress->${Thread.currentThread().id}","${thisView.findViewById<SeekBar>(R.id.seekBar).progress}")
                mainActivity.handler.sendMessage(msg)
            }
            findViewById<ImageView>(R.id.play).setImageResource(R.drawable.stop)
        }
    }


    /**
     * 配置SeekBar的相关属性和布局显示
     * @param mediaPlayer 已经加载了音乐文件的媒体播放者
     * @param view 本碎片的视图
     */
    private fun configPregressBar(){
        val duration = mainActivity.musicPlayBinder.mediaPlayer.duration
        findViewById<TextView>(R.id.duration).text = "%02d:%02d".format(duration/1000/60,(duration/1000)%60)
        findViewById<SeekBar>(R.id.seekBar).max = duration
        findViewById<SeekBar>(R.id.seekBar).progress = 0
    }


    /**
     * 配置音乐的切换按钮，实现上一首和下一首功能
     * @param view 本碎片的视图
     */
    private fun configMusicSwitchBtn(){
        val precious = findViewById<ImageView>(R.id.precious)
        val next = findViewById<ImageView>(R.id.next)

        precious.setOnClickListener{
            if(musicIx>0){
                mainActivity.musicPlayBinder.preciousMusic()
                loadMusic(--musicIx)
            }else{
                Toast.makeText(this,"这是已经第一首歌曲", Toast.LENGTH_SHORT).show()
            }
        }

        next.setOnClickListener{
            if(musicIx<mainActivity.musicList.size){
                mainActivity.musicPlayBinder.nextMusic()
                loadMusic(++musicIx)
            }else{
                Toast.makeText(this,"这是已经是一首歌曲", Toast.LENGTH_SHORT).show()
            }
        }
    }


}