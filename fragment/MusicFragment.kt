package com.example.music.fragment

import android.os.Bundle
import android.os.Message
import android.os.SystemClock.sleep
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.music_app.MainActivity
import com.example.music_app.R
import com.example.music_app.musicList
import com.example.music_app.services.MusicService
import java.lang.Exception
import kotlin.concurrent.thread

/**
 * 音乐播放页的碎片实现类，仅此一个
 * @author along
 * @param musicIx 当类首次被创建时需要加载的音乐资源的索引号
 */
class MusicFragment(var musicIx: Int):Fragment() {

    private lateinit var mainActivity : MainActivity

    private lateinit var thisView:View


    /**
     * 初始化与碎片关联的活动
     */
    private fun init(){
        if(activity != null){
            mainActivity = activity as MainActivity
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        thisView = inflater.inflate(R.layout.music_frag,container,false)
        /*当活动被创建时初始化碎片需要的属性参数*/
        init()

        /*加载音乐*/
        loadMusic(musicIx)

        /*配置下控制音乐播放的按钮*/
        configMusicSwitchBtn()

        return thisView
    }


    /**
     * 加载页面的音乐文件布局内容和音乐资源
     * @param toReflushMusicIx 需要被加载的音乐文件的索引号
     */
    fun loadMusic(toReflushMusicIx: Int){

        musicIx = toReflushMusicIx
        val music = musicList[musicIx]

        /*重新设置音乐显示的相关控件*/
        thisView.apply {
            findViewById<ImageView>(R.id.music_cover).setImageResource(music.imageId)
            findViewById<TextView>(R.id.music_name).text = music.name
            findViewById<TextView>(R.id.music_author).text = music.author
            findViewById<ImageView>(R.id.play).setImageResource(R.drawable.start)
        }


        /*开始准备播放*/
        mainActivity.musicPlayBinder.preparePlay()
        /*配置进度条的属性*/
        configPregressBar()
        /*开始播放*/
        mainActivity.musicPlayBinder.startPlay()

        /*实时刷新进度条*/
        reflushProgress()

    }


    fun reflushProgress(){
        thread {
            while(mainActivity.musicPlayBinder.mediaPlayer.isPlaying){
                sleep(MusicService.MusicPlayBinder.reflushTime)
                val msg = Message.obtain()
                msg.what = mainActivity.updateProgress
//                Log.e("progress->${Thread.currentThread().id}","${thisView.findViewById<SeekBar>(R.id.seekBar).progress}")
                mainActivity.handler.sendMessage(msg)
            }
            thisView.findViewById<ImageView>(R.id.play).setImageResource(R.drawable.stop)
        }
    }


    /**
     * 配置SeekBar的相关属性和布局显示
     * @param mediaPlayer 已经加载了音乐文件的媒体播放者
     * @param view 本碎片的视图
     */
    private fun configPregressBar(){
        val duration = mainActivity.musicPlayBinder.mediaPlayer.duration
        thisView.findViewById<TextView>(R.id.duration).text = "%02d:%02d".format(duration/1000/60,(duration/1000)%60)
        thisView.findViewById<SeekBar>(R.id.seekBar).max = duration
        thisView.findViewById<SeekBar>(R.id.seekBar).progress = 0
    }


    /**
     * 配置音乐的切换按钮，实现上一首和下一首功能
     * @param view 本碎片的视图
     */
    private fun configMusicSwitchBtn(){
        val precious = thisView.findViewById<ImageView>(R.id.precious)
        val next = thisView.findViewById<ImageView>(R.id.next)

        precious.setOnClickListener{
            if(musicIx>0){
                mainActivity.musicPlayBinder.preciousMusic()
                loadMusic(--musicIx)
            }else{
                Toast.makeText(context,"这是已经第一首歌曲",Toast.LENGTH_SHORT).show()
            }
        }

        next.setOnClickListener{
            if(musicIx<musicList.size){
                mainActivity.musicPlayBinder.nextMusic()
                loadMusic(++musicIx)
            }else{
                Toast.makeText(context,"这是已经是一首歌曲",Toast.LENGTH_SHORT).show()
            }
        }
    }
}