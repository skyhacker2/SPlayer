package com.eleven.app.splayer;

import android.database.Cursor;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Handler;
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

    private Cursor mCursor;

    private MusicListAdapter mAdapter;
    private MediaPlayer mMediaPlayer;

    private Timer mTimer;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AUTO_UPDATE:
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        int position = mMediaPlayer.getCurrentPosition();
                        MusicPlayData.sCurrentPlayPosition = position;
                        mSeekBar.setProgress(position);
                        mCurrentPlayTime.setText(formatTime(position));
                    }
            }
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

        scheduleUpdate();

        mMusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MusicPlayData.sIsPlayNew = true;
                MusicPlayData.sCurrentPlayIndex = position;
                MusicPlayData.sCurrentPlayPosition = 0;
                play();
                setDurationAndCurrentTime();
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    updatePlayButton();
                } else {
                    resume();
                    updatePlayButton();
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
                if (mMediaPlayer != null) {
                    mMediaPlayer.seekTo(MusicPlayData.sCurrentPlayPosition);
                }
                sendUpdateMessage(AUTO_UPDATE);
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


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
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
        if (mMediaPlayer != null) {
            Log.d(TAG, "播放前先把之前的释放");
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (MusicPlayData.sMusicList.size() > 0) {
            mMediaPlayer = new MediaPlayer();
            if (MusicPlayData.sIsPlayNew) {
                mMediaPlayer.reset();
                try {
                    mMediaPlayer.setDataSource(MusicPlayData.sMusicList.get(MusicPlayData.sCurrentPlayIndex).getFilePath());
                    mMediaPlayer.prepare();
                    MusicPlayData.sIsPlayNew = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 设置播放模式为正在播放
            MusicPlayState.sCurrentState = MusicPlayState.PLAY_STATE_PLAYING;

            mMediaPlayer.seekTo(MusicPlayData.sCurrentPlayPosition);;
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playNext();
                }
            });
        }
    }

    private void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    private void resume() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    private void playPrevious() {
        int previousIndex = MusicPlayData.sCurrentPlayIndex - 1;
        if (previousIndex < 0) {
            return;
        }
        MusicPlayData.sCurrentPlayIndex = previousIndex;
        MusicPlayData.sCurrentPlayPosition = 0;
        MusicPlayData.sIsPlayNew = true;
        play();
        setDurationAndCurrentTime();
    }

    private void playNext() {
        int nextIndex = MusicPlayData.sCurrentPlayIndex + 1;
        if (nextIndex > MusicPlayData.sMusicList.size() - 1) {
            return;
        }
        MusicPlayData.sCurrentPlayIndex = nextIndex;
        MusicPlayData.sCurrentPlayPosition = 0;
        MusicPlayData.sIsPlayNew = true;
        play();
        setDurationAndCurrentTime();
    }

    private void initSeekAndText() {
        mSeekBar.setProgress(0);
        mCurrentPlayTime.setText("00:00");
        mMusicTotalTime.setText("00:00");
    }

    private void updatePlayButton() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mMediaPlayer.isPlaying()) {
            mPlayButton.setImageResource(R.drawable.selector_pause_button);
        } else {
            mPlayButton.setImageResource(R.drawable.selector_play_button);
        }
    }

    private void updateListViewSelect() {
        if (MusicPlayData.sCurrentPlayIndex > -1) {
            mAdapter.setSelectedIndex(MusicPlayData.sCurrentPlayIndex);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void setDurationAndCurrentTime() {
        int index = MusicPlayData.sCurrentPlayIndex;
        if (index >=0 && index < MusicPlayData.sMusicList.size()) {
            updateListViewSelect();
            if (mMediaPlayer != null) {
                mMusicTotalTime.setText(formatTime(mMediaPlayer.getDuration()));
                MusicPlayData.sTotalTime = mMediaPlayer.getDuration();
                mSeekBar.setMax(mMediaPlayer.getDuration());
                mSeekBar.setProgress(0);
            }
        } else {
            initSeekAndText();
        }
        updatePlayButton();
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

    private void sendUpdateMessage(int type) {
        Message msg = Message.obtain();
        msg.what = type;
        mHandler.sendMessage(msg);
    }

    /**
     * 启动定时器
     */
    private void scheduleUpdate() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendUpdateMessage(AUTO_UPDATE);
            }
        }, 0, 1000);
    }
}
