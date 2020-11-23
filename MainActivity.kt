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
import kotlin.concurrent.thread
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var  musicListFragment: MusicListFragment

    lateinit var musicFragment: MusicFragment

    lateinit var musicPlayBinder: MusicService.MusicPlayBinder

    private val mainActivity = this

    val musicList = ArrayList<Music>()

    val updateProgress = 0x123

    private var selectMusicIx = 0

    //进度条刷新周期



    /**
     * 消息处理器，用来处理UI更新
     */
    val handler = object : Handler(){
        override fun handleMessage(msg: Message) {
            val seekBar = msg.obj as SeekBar
            when(msg.what){
                updateProgress -> {
//                    Log.e("main -> ","${seekBar.progress}")
                    seekBar.progress += MusicService.MusicPlayBinder.reflushTime.toInt()
                    val time = mainFrag.findViewById<TextView>(R.id.time)
                    val timeTmp = seekBar.progress/1000
                    if(time!=null )
                        time.text = "%02d:%02d".format(timeTmp/60,timeTmp%60)
                }
            }
        }
    }

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
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMusic()
        //配置toolbar

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeAsUpIndicator(R.drawable.home)
            it.setDisplayHomeAsUpEnabled(true)
        }

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

        replaceFragment(R.id.mainFrag, CoverFragment(), false)
        configBroadcastReceiver()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }


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
     * 替换主布局文件中的FrameLayout，实现页面的动态加载功能
     *
     * @param fragmentId 需要被替换FrameLayout的id
     * @param fragment 已经加载了需要被显示的布局文件的Fragment类
     */

    fun replaceFragment(fragmentId:Int, fragment: Fragment, push:Boolean){
        val transition = supportFragmentManager.beginTransaction()
        transition.replace(fragmentId,fragment)
        if(push)
            transition.addToBackStack(null)
        transition.commit()
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
                    val position = it.findViewById<RecyclerView>(R.id.musicListView).getChildLayoutPosition(v)
                    val intent = Intent(this, MusicService::class.java)
                    selectMusicIx = position
                    if(this::musicFragment.isInitialized){
                        unbindService(connection)
                        stopService(intent)
                    }
                    startService(intent)
                    bindService(intent,connection, Context.BIND_AUTO_CREATE)
                }
            }

            R.id.play ->{
                if(!musicPlayBinder.mediaPlayer.isPlaying){
                    musicPlayBinder.startPlay()
                    val seekBar = mainFrag.findViewById<SeekBar>(R.id.seekBar)
                    thread {
                        while(musicPlayBinder.mediaPlayer.isPlaying){
                            sleep(MusicService.MusicPlayBinder.reflushTime)
                            val msg = Message.obtain()
                            msg.what = updateProgress
                            msg.obj = seekBar
                            handler.sendMessage(msg)
                        }
                    }
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
     * 初始化音乐数据
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

    private fun configBroadcastReceiver(){
        val myBroadcastReceiver = MusicSwitchBroacast(this)
        val intentFilter = IntentFilter(MusicSwitchBroacast.START_MUSIC)
        intentFilter.addAction(MusicSwitchBroacast.STOP_MUSIC)
        intentFilter.addAction(MusicSwitchBroacast.PRECIOUS_MUSIC)
        intentFilter.addAction(MusicSwitchBroacast.NEXT_MUSIC)

        registerReceiver(myBroadcastReceiver,intentFilter)
    }


    /**
     * 使用Toast显示提示信息
     * @param msg 需要显示的信息
     */
    private fun showMessage(msg:String){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
    }


}
