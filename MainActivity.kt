package com.example.music_app

import android.content.*
import android.media.MediaPlayer
import android.os.*
import android.os.SystemClock.sleep
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.music.fragment.CoverFragment
import com.example.music.fragment.MusicFragment
import com.example.music.fragment.MusicListFragment
import com.example.music.pojo.Music
import com.example.music_app.broadcasts.MusicSwitchBroacast
import com.example.music_app.services.MusicService
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.music_frag.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var  musicListFragment: MusicListFragment

    lateinit var musicFragment: MusicFragment

    lateinit var musicPlayBinder: MusicService.MusicPlayBinder

    lateinit var myBroadcastReceiver : MusicSwitchBroacast


    val updateProgress = 0x123

    private var selectMusicIx = 0


    /**
     * 消息处理器，用来处理进度条更新和歌词更新
     */
    val handler = object : Handler(){

        override fun handleMessage(msg: Message) {
            when(msg.what){
                updateProgress -> {
//                    Log.e("main -> ","${seekBar.progress}")
                    val currPos = musicPlayBinder.mediaPlayer.currentPosition
                    musicFragment.seekBar.progress = currPos

                    /*更新歌词*/
                    musicPlayBinder.lrcInfo.infos?.let {
                        val lrcTxt = it.get(currPos/1000)
                        if(lrcTxt!=null && !lrcTxt.equals(musicFragment.lrcTxt.text)){
                            musicFragment.lrcTxt.text = lrcTxt
//                            Log.e("MainActivity#handleMessage -> ", "${lrcTxt}")
                        }
                    }

                    /*更新时间*/
                    val timeTmp = musicFragment.seekBar.progress/1000
                    if(musicFragment.time!=null ){
                        musicFragment.time.text = "%02d:%02d".format(timeTmp/60,timeTmp%60)
                    }


                }
            }
        }
    }




    /**
     * service 连接，定义了连接后的处理事件，包括初始化音乐播放处理的Binder以及
     * 新建一个音乐播放的碎片musicFragment，然后将它推到主页面
     */
    private val connection = object : ServiceConnection{


        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicPlayBinder = service as MusicService.MusicPlayBinder
            musicPlayBinder.init(musicList,selectMusicIx,assets)

//            val intent = Intent(mainActivity,MusicPlayActivity::class.java)
//            val bundle = Bundle()
//            bundle["mainActivity"]
            musicFragment = MusicFragment(selectMusicIx)
            replaceFragment(R.id.mainFrag,musicFragment,true)
        }


        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
            Log.e("MusicService->","onServiceDisconnected()")
        }

    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("MainActivity ->","${this} onCreate()")
        setContentView(R.layout.activity_main)

        replaceFragment(R.id.mainFrag, CoverFragment(), false)

        initMusic()

        configToolbar()

        configNavigationView()

        configBroadcastReceiver()
    }



    /**
     * 初始化音乐数据，包括实例化音乐列表碎片musicListFragment和musicList
     */
    private fun initMusic(){

        val musicNames = resources.getStringArray(R.array.music)
        val musicAuthors = resources.getStringArray(R.array.singer)
        val musicImages = resources.obtainTypedArray(R.array.music_picture)
        val music_res = resources.getStringArray(R.array.music_lyrics)

        for(i in musicNames.indices){
            musicList.add(Music(musicNames[i],musicAuthors[i],musicImages.getResourceId(i,R.drawable.music),music_res[i]))
        }

        musicListFragment = MusicListFragment(musicList)

    }



    /**
     * 配置toolbar
     */
    private fun configToolbar(){
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeAsUpIndicator(R.drawable.home)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }




    /**
     * 配置侧边栏
     */
    private fun configNavigationView(){

        navigationView.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.themeItem-> Snackbar.make(drawerLayout,"个性化正在建设",Snackbar.LENGTH_SHORT).show()
                R.id.helpItem->Snackbar.make(drawerLayout,"帮助与反馈信息正在建设",Snackbar.LENGTH_SHORT).show()
                R.id.closeInTimeItem->Snackbar.make(drawerLayout,"定时关闭正在建设",Snackbar.LENGTH_SHORT).show()
                R.id.teenagerModelItem->Snackbar.make(drawerLayout,"青少年模式正在建设",Snackbar.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawers()
            true
        }


    }


    /**
     * 注册广播,监听通知栏中的点击事件
     */
    private fun configBroadcastReceiver(){
        myBroadcastReceiver = MusicSwitchBroacast(this)
        val intentFilter = IntentFilter(MusicSwitchBroacast.START_MUSIC)
        intentFilter.addAction(MusicSwitchBroacast.STOP_MUSIC)
        intentFilter.addAction(MusicSwitchBroacast.PRECIOUS_MUSIC)
        intentFilter.addAction(MusicSwitchBroacast.NEXT_MUSIC)

        registerReceiver(myBroadcastReceiver,intentFilter)
    }


    /**
     * 替换主布局文件中的FrameLayout，实现页面的动态加载功能
     *
     * @param fragmentId 需要被替换FrameLayout的id
     * @param fragment 已经加载了需要被显示的布局文件的Fragment类
     * @param push 是否将操作加入返回栈
     */
    fun replaceFragment(fragmentId:Int, fragment: Fragment, push:Boolean){
        val transition = supportFragmentManager.beginTransaction()
        transition.replace(fragmentId,fragment)

        /*是否将这次的操作加入栈中*/
        if(push)
            transition.addToBackStack(null)
        transition.commit()
    }



//    override fun onDestroy() {
//        super.onDestroy()
//
//        musicPlayBinder.stopPlay()
//        val intent = Intent(this,MusicService::class.java)
//        unbindService(connection)
//        stopService(intent)
//        Log.e("MainActivity ->","onDestroy()")
//    }


    /**
     * 创建主菜单
     * @param menu
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }



    /**
     * 主菜单选择处理方法
     * @param item 选择的项
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.aboutItem -> Snackbar.make(drawerLayout,"关于信息正在建设",Snackbar.LENGTH_SHORT).show()
            R.id.exitItem -> AlertDialog.Builder(this).apply {
                setMessage("确认退出${resources.getString(R.string.app_name)}?")
                setPositiveButton("确认"){dialog, which -> exitProcess(0)  }
                setNegativeButton("取消"){dialog, which ->  }
                show()
            }
            android.R.id.home -> drawerLayout!!.openDrawer(GravityCompat.START)
        }
        return super.onOptionsItemSelected(item)
    }




    /**
     * 拦截被点击的UI控件，并作出相应的响应
     *
     * @param v 点击的UI控件
     */
     fun onClick(v: View?) {
        when(v?.id){
            R.id.cover -> {
                replaceFragment(R.id.mainFrag,musicListFragment,false)
            }


            R.id.music_item ->{
                musicListFragment.view?.let{
                    if(this::musicFragment.isInitialized){
                        /*停止上一首歌曲播放*/
                        musicPlayBinder.stopPlay()
                        sleep(MusicService.MusicPlayBinder.reflushTime)

                        unbindService(connection)
                        stopService(intent)
                    }

                    val position = it.findViewById<RecyclerView>(R.id.musicListView).getChildLayoutPosition(v)
                    val intent = Intent(this, MusicService::class.java)
                    /*更新selectMusicIx，在服务与活动连接时需要使用它渲染页面*/
                    selectMusicIx = position
                    /*开启并绑定服务*/
                    startService(intent)
                    bindService(intent,connection, Context.BIND_AUTO_CREATE)
                }
            }

            R.id.play ->{
                if(!musicPlayBinder.mediaPlayer.isPlaying){
                    musicPlayBinder.startPlay()
                    musicFragment.reflushProgress()
                    v.findViewById<ImageView>(R.id.play).setImageResource(R.drawable.start)
                }else{
                    musicPlayBinder.stopPlay()
                    v.findViewById<ImageView>(R.id.play).setImageResource(R.drawable.stop)
                }
            }


            R.id.loveItem ->{
                showMessage("已加入喜欢")
            }

            R.id.downloadItem ->{
                showMessage("版权原因，歌曲不允许下载")
            }

            R.id.singItem ->{
                showMessage("该功能正在建设，敬请期待")
            }

            R.id.listItem ->{
                showMessage("该功能正在建设，敬请期待")
            }

            R.id.remarkItem ->{
                showMessage("该功能正在建设，敬请期待")
            }
        }
    }




    /**
     * 使用Toast显示提示信息
     * @param msg 需要显示的信息
     */
    private fun showMessage(msg:String){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
    }


}
