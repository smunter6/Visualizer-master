package com.example.mobile.visualizer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;

public class ServiceHandler extends Activity {

    //song list variables
    public ArrayList<Song> songList;
    public ArrayList<Song> artistList;
    public ListView songView;
    public ListView artistView;

    public Intent playIntent;
    //binding
    public boolean musicBound=false;
    public MusicService musicSrv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    //start the service
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            startService(playIntent);
        }
    }

    //bind the service
    @Override
    protected void onResume(){
        super.onResume();
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
    }

    //unbind the service
    @Override
    protected void onPause(){
        super.onPause();
        unbindService(musicConnection);
        musicBound = false;
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    //destroy service
    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicSrv.playSong(true);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };
}
