package com.kathline.videocache;


import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.danikula.videocache.HttpProxyCacheServer;
import com.kathline.videocache.cache.PreloadManager;
import com.kathline.videocache.cache.ProxyVideoCacheManager;

public class MainActivity extends AppCompatActivity {

    private VideoView mVideoView;
    private Button button;
    private Button button2;
    private Button button3;
    private Button button4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        //  mVideoView.setVideoPath(file.getAbsolutePath()) //设置视频文件

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //视频加载完成,准备好播放视频的回调
                Log.d("mVideoView", "setOnPreparedListener");
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                //视频播放完成后的回调
                Log.d("mVideoView", "setOnCompletionListener");
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e("mVideoView", "Error (" + what + "," + extra + ")");
                return false;
            }
        });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoView.stopPlayback();
                HttpProxyCacheServer proxy = ProxyVideoCacheManager.getProxy(getApplication());
                String proxyUrl = proxy.getProxyUrl("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"); //设置视
                //mVideoView.setVideoPath("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4") //设置视
                mVideoView.setVideoPath(proxyUrl); //设置视
                mVideoView.start();
            }
        });


        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreloadManager.getInstance(getApplicationContext()).addPreloadTask("http://vfx.mtime.cn/Video/2019/03/18/mp4/190318214226685784.mp4", 100);
                PreloadManager.getInstance(getApplicationContext()).addPreloadTask("https://oss-static.innogx.com/Public/Attachment/File/2021-01-12/5ffd518743241.mp4", 20);
                PreloadManager.getInstance(getApplicationContext()).addPreloadTask("https://vd3.bdstatic.com/mda-kgdqbun4qa1nexm2/v1-cae/1080p/mda-kgdqbun4qa1nexm2.mp4", 20);
                PreloadManager.getInstance(getApplicationContext()).addPreloadTask("https://vd3.bdstatic.com/mda-marvjvfuzzf57m8x/v1-cae/1080p/mda-marvjvfuzzf57m8x.mp4", 20);
                PreloadManager.getInstance(getApplicationContext()).addPreloadTask("https://vd4.bdstatic.com/mda-kg4deg9qaaemq6yx/v1-cae/1080p/mda-kg4deg9qaaemq6yx.mp4", 20);
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoView.stopPlayback();
                HttpProxyCacheServer proxy = ProxyVideoCacheManager.getProxy(getApplication());
                String proxyUrl = proxy.getProxyUrl("http://vfx.mtime.cn/Video/2019/03/18/mp4/190318214226685784.mp4");
                mVideoView.setVideoPath(proxyUrl); //设置视
                mVideoView.start();
            }
        });

        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoView.stopPlayback();
                HttpProxyCacheServer proxy = ProxyVideoCacheManager.getProxy(getApplication());
                String proxyUrl = proxy.getProxyUrl("https://oss-static.innogx.com/Public/Attachment/File/2021-01-12/5ffd518743241.mp4");
                mVideoView.setVideoPath(proxyUrl); //设置视
                mVideoView.start();
            }
        });
    }

    private void initView() {
        mVideoView = (VideoView) findViewById(R.id.mVideoView);
        button = (Button) findViewById(R.id.button);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
    }
}
