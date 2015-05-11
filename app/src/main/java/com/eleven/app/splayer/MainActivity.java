package com.eleven.app.splayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity{

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int AUTO_UPDATE = 0;

    private ListView mMusicListView;
    private ImageButton mPlayButton;
    private ImageButton mPreButton;
    private ImageButton mNextButton;
    private SeekBar mSeekBar;
    private TextView mCurrentPlayTime;
    private TextView mMusicTotalTime;

    // 播放服务
    private PlayerService mPlayerService;

    private Cursor mCursor;

    private MusicListAdapter mAdapter;


    // 服务绑定
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayerService = ((PlayerService.LocalBinder)service).getService();
            initUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlayerService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMusicListView = (ListView)findViewById(R.id.music_list);
        mPlayButton = (ImageButton)findViewById(R.id.play_button);
        mPreButton = (ImageButton)findViewById(R.id.pre_button);
        mNextButton = (ImageButton)findViewById(R.id.next_button);
        mSeekBar = (SeekBar)findViewById(R.id.list_music_seekBar);
        mCurrentPlayTime = (TextView) findViewById(R.id.list_current_play_time);
        mMusicTotalTime = (TextView) findViewById(R.id.list_music_totalTime);

        initMusicList();
        mAdapter = new MusicListAdapter(this, MusicPlayData.sMusicList);
        mMusicListView.setAdapter(mAdapter);

        mMusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MusicPlayData.sIsPlayNew = true;
                MusicPlayData.sCurrentPlayIndex = position;
                MusicPlayData.sCurrentPlayPosition = 0;
                play();
                initUI();
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MusicPlayState.sCurrentState == MusicPlayState.PLAY_STATE_PLAYING) {
                    pause();
                } else {
                    resume();
                }
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MusicPlayData.sCurrentPlayPosition = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mPlayerService != null) {
                    mPlayerService.getMediaPlayer().seekTo(MusicPlayData.sCurrentPlayPosition);
                }
            }
        });

        mPreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        });

        Intent startPlayIntent = new Intent(this, PlayerService.class);
        startService(startPlayIntent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPlayerService == null) {
            Intent bindIntent = new Intent(this, PlayerService.class);
            bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlayerService.PLAY);
        intentFilter.addAction(PlayerService.PAUSE);
        intentFilter.addAction(PlayerService.RESUME);
        intentFilter.addAction(PlayerService.UPDATE_PROGRESS);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    /**
     * 初始化音乐列表
     */
    private void initMusicList() {
        MusicPlayData.sMusicList.clear();
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        // 查询音乐
        mCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATA }, null, null, null);

        if (mCursor == null || mCursor.moveToFirst() == false) {
            Toast.makeText(this, "没有音乐", Toast.LENGTH_LONG).show();
            return;
        }
        int index = 0;
        mCursor.moveToFirst();
        while (mCursor.moveToNext()) {
            MusicData song = new MusicData();
            song.setMusicId(index);
            song.setFileName(mCursor.getString(1));
            song.setMusicName(mCursor.getString(2));
            song.setMusicDuration(mCursor.getInt(3));
            song.setMusicArtist(mCursor.getString(4));
            song.setMusicAlbum(mCursor.getString(5));
            song.setMusicYear(mCursor.getString(6));
            // file type
            if ("audio/mpeg".equals(mCursor.getString(7).trim())) {
                song.setFileType("mp3");
            } else if ("audio/x-ms-wma".equals(mCursor.getString(7).trim())){
                song.setFileType("wma");
            }
            song.setFileType(mCursor.getString(7));
            song.setFileSize(mCursor.getString(8));
            if (mCursor.getString(9) != null) {
                song.setFilePath(mCursor.getString(9));
            }
            index++;
            MusicPlayData.sMusicList.add(song);
        }
        mCursor.close();
        mCursor = null;
    }

    private void play() {
        if (mPlayerService != null) {
            mPlayerService.playMusic();
        }
    }

    private void pause() {
        if (mPlayerService != null) {
            mPlayerService.pauseMusic();
        }
    }

    private void resume() {
        if (mPlayerService != null){
            mPlayerService.resumeMusic();
        }
    }

    private void playPrevious() {
        if (mPlayerService != null) {
            mPlayerService.playPrevious();
        }
    }

    private void playNext() {
        if (mPlayerService != null) {
            mPlayerService.playNext();
        }
    }

    private void initUI() {
        MediaPlayer mediaPlayer = mPlayerService.getMediaPlayer();
        if (mediaPlayer != null && MusicPlayState.sCurrentState == MusicPlayState.PLAY_STATE_PLAYING) {
            mSeekBar.setProgress(MusicPlayData.sCurrentPlayPosition);
            mMusicTotalTime.setText(formatTime(mediaPlayer.getDuration()));
            mCurrentPlayTime.setText(formatTime(MusicPlayData.sCurrentPlayPosition));
            mSeekBar.setMax(mediaPlayer.getDuration());
        } else {
            mSeekBar.setProgress(0);
            mMusicTotalTime.setText("00:00");
            mCurrentPlayTime.setText("00:00");
        }

        updatePlayButton();
        updateList();
    }


    private void updateList() {
        if (MusicPlayData.sCurrentPlayIndex > -1) {
            mAdapter.setSelectedIndex(MusicPlayData.sCurrentPlayIndex);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void updateProgress() {
        mSeekBar.setProgress(MusicPlayData.sCurrentPlayPosition);
        mMusicTotalTime.setText(formatTime(MusicPlayData.sTotalTime));
        mCurrentPlayTime.setText(formatTime(MusicPlayData.sCurrentPlayPosition));
        mSeekBar.setMax(MusicPlayData.sTotalTime);
    }

    private void updatePlayButton() {
        if (MusicPlayState.sCurrentState == MusicPlayState.PLAY_STATE_PLAYING) {
            mPlayButton.setImageResource(R.drawable.selector_pause_button);
        } else {
            mPlayButton.setImageResource(R.drawable.selector_play_button);
        }
    }


    private String formatTime(int time) {
        time /= 1000;
        int hour;
        int minute;
        int second;
        if (time > 3600) {
            hour = time / 3600;
            minute = (time % 3600) / 60;
            second = (time % 3600) % 60;
            return String.format("%02d:%02d:%02d", hour, minute, second);
        } else {
            minute = time / 60;
            second = time % 60;
            return String.format("%02d:%02d", minute, second);
        }
    }

    // 监听service的广播
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(PlayerService.PLAY)) {
                updatePlayButton();
                updateList();
                updateProgress();
            }
            else if (action.equals(PlayerService.PAUSE)) {
                updatePlayButton();
                updateList();
            }
            else if (action.equals(PlayerService.RESUME)) {
                updatePlayButton();
                updateList();
            }
            else if (action.equals(PlayerService.UPDATE_PROGRESS)) {
                updateProgress();
            }
        }
    };
}
