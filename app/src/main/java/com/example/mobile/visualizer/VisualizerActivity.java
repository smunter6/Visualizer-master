package com.example.mobile.visualizer;

import android.app.Activity;
import android.os.Bundle;

public class VisualizerActivity extends ServiceHandler {
    private VisualizerView mVisualizerView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualizer);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // We need to link the visualizer view to the media player so that
        // it displays something
        mVisualizerView = (VisualizerView) findViewById(R.id.visualizerView);
        //mVisualizerView.link(musicSrv.getPlayer());
    }
}
