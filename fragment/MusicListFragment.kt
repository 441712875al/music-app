package com.example.music.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.music.adapter.MusicRecycleViewAdapter


import com.example.music.pojo.Music
import com.example.music_app.MainActivity
import com.example.music_app.R
import com.google.android.material.bottomnavigation.BottomNavigationView


class MusicListFragment(val musicList:List<Music>):Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.music_list_fragment,container,false)
        val musicListView = view.findViewById<RecyclerView>(R.id.musicListView)
        musicListView.layoutManager = LinearLayoutManager(context)
        val adapter = MusicRecycleViewAdapter(musicList)
        musicListView.adapter = adapter
        adapter.notifyDataSetChanged()

        if(activity != null){
            val bottomNavigationView = view.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            val mainActivity = activity as MainActivity
            val drawerLayout = mainActivity.findViewById<DrawerLayout>(R.id.drawerLayout)
            bottomNavigationView.itemIconTintList = null
            bottomNavigationView.setOnNavigationItemSelectedListener {
                when(it.itemId){
                    R.id.homePageItem-> Toast.makeText(mainActivity,"你已处于首页", Toast.LENGTH_SHORT).show()
                    R.id.videoItem-> Toast.makeText(mainActivity,"视频内容正在建设", Toast.LENGTH_SHORT).show()
                    R.id.puTongItem-> Toast.makeText(mainActivity,"扑通内容正在建设", Toast.LENGTH_SHORT).show()
                    R.id.userItem->drawerLayout!!.openDrawer(GravityCompat.START)
                }
                true
            }
        }

        return view
    }

}