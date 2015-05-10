package com.example.mobile.visualizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.example.mobile.visualizer.MusicService.MusicBinder;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.TabHost;
import android.widget.Toast;

public class MainActivity extends ServiceHandler implements MediaPlayerControl {

    //controller
    private MediaController controller;

    //activity and playback pause flags
    private boolean paused=false, playbackPaused=false;

    public static int tab = 0;

    //current duration and position of songs
    private int currDur;
    private int currPos;
    //flag to show controls
    private boolean showControls;
    // Broadcast receiver to determine when music player has been prepared
    private BroadcastReceiver onPrepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            // When music player has been prepared, show controller
            if(showControls) {
                controller.show();
            }
            showControls=false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Visualizer");

        //code for tabs
        final TabHost tabHost = (TabHost) findViewById(R.id.tabHost);

        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("song");
        tabSpec.setContent(R.id.tabSongList);
        tabSpec.setIndicator("Song");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("artist");
        tabSpec.setContent(R.id.tabArtistList);
        tabSpec.setIndicator("Artist");
        tabHost.addTab(tabSpec);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                int i = tabHost.getCurrentTab();
                if (i == 0) {
                    musicSrv.setList(songList);
                    tab = 0;
                } else if (i == 1) {
                    musicSrv.setList(artistList);
                    tab = 1;
                }
            }
        });

        //create list of songs
        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        getSongList();

        artistView = (ListView)findViewById(R.id.artist_list);
        artistList = new ArrayList<Song>();
        artistList.addAll(songList);

        //sort by artist
        Collections.sort(artistList, new Comparator<Song>() {
            @Override
            public int compare(Song a, Song b) {
                return a.getArtist().compareTo(b.getArtist());
            }
        });

        //sort songs alphabetically
        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        SongAdapter artistAdt = new SongAdapter(this, artistList);
        songView.setAdapter(songAdt);
        artistView.setAdapter(artistAdt);
        showControls = false;
        currDur = 0;
        currPos = 0;
        setController();
    }

    //start and bind the service when the activity starts
    @Override
    protected void onStart() {
        super.onStart();
        // Set up receiver for media player onPrepared broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(onPrepareReceiver, new IntentFilter("MEDIA_PLAYER_PREPARED"));
    }

    //user song select
    public void songPicked(View view){
        showControls = true;
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        if(playbackPaused){
            playbackPaused=false;
        }
        musicSrv.playSong(false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                Toast.makeText(getApplicationContext(), "Shuffle", Toast.LENGTH_LONG).show();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
            case R.id.action_viz:
                    //unbindService(musicSrv);
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(onPrepareReceiver);
                    Intent intent = new Intent(getApplicationContext(), VisualizerActivity.class);
                    startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    //method to retrieve song info from device
    public void getSongList(){
        //query external audio
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        //iterate over results if valid
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng()) {
            currPos=musicSrv.getPosn();
        }
        return currPos;
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng()) {
            currDur=musicSrv.getDur();
        }
        return currDur;
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public void pause() {
        showControls = true;
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void start() {
        showControls = true;
        musicSrv.go();
    }

    //set the controller up
    private void setController(){
        if(controller == null){
            controller = new MediaController(this, false){
                @Override
                public void hide(){

                }
            };
        }
        //set previous and next button listeners
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        //set and show
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.LinLayout));
        controller.setEnabled(true);
    }

    private void playNext(){
        showControls = true;
        if(playbackPaused){
            playbackPaused=false;
        }
        musicSrv.playNext();
    }

    private void playPrev(){
        showControls = true;
        if(playbackPaused){
            playbackPaused=false;
        }
        musicSrv.playPrev();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(!paused){
            paused=true;
        }
    }

    @Override
    protected void onResume(){
        showControls = true;
        super.onResume();
        if(paused){
            paused=false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onPrepareReceiver);
    }


    @Override
    protected void onDestroy(){
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus && showControls){
            controller.show(0);
        }
        showControls=false;
    }
}
