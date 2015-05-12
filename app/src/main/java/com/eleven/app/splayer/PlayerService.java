package com.eleven.app.splayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 播放服务
 * Created by eleven on 15/5/11.
 */
public class PlayerService extends Service {
    private static final String TAG = PlayerService.class.getSimpleName();

    public static final String PLAY = "com.eleven.app.splayer.PLAY";
    public static final String PAUSE = "com.eleven.app.splayer.PAUSE";
    public static final String RESUME = "com.eleven.app.splayer.RESUME";
    public static final String PLAY_NEXT = "com.eleven.app.splayer.PLAY_NEXT";
    public static final String PLAY_PREVIOUS = "com.eleven.app.splayer.PLAY_PREVIOUS";
    public static final String UPDATE_PROGRESS = "com.eleven.app.splayer.UPDATE_PROGRESS";

    public static final String EXTRA_PROGRESS = "com.eleven.app.splayer.EXTRA_PROGRESS";

    public static final int NOTIFICATION_ID = 123456;

    private MediaPlayer mMediaPlayer;
    private IBinder mIBinder = new LocalBinder();

    private Timer mTimer;

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scheduleUpdate();
        return START_NOT_STICKY;
    }

    /**
     * 播放
     */
    public void playMusic() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
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
            MusicPlayData.sCurrentPlayPosition = 0;
            MusicPlayData.sTotalTime = mMediaPlayer.getDuration();
            mMediaPlayer.seekTo(MusicPlayData.sCurrentPlayPosition);
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playNext();
                }
            });
            Intent intent = new Intent();
            intent.setAction(PLAY);
            sendBroadcast(intent);
            scheduleUpdate();
            showNotification();
        }
    }

    /**
     * 暂停
     */
    public void pauseMusic() {
        Log.d(TAG, "实现暂停");
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            MusicPlayState.sCurrentState = MusicPlayState.PLAY_STATE_PAUSE;
            Intent intent = new Intent();
            intent.setAction(PAUSE);
            sendBroadcast(intent);
        }
    }

    /**
     * 恢复
     */
    public void resumeMusic() {
        Log.d(TAG, "实现恢复");
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            MusicPlayState.sCurrentState = MusicPlayState.PLAY_STATE_PLAYING;
            Intent intent = new Intent();
            intent.setAction(RESUME);
            sendBroadcast(intent);
        }
    }

    /**
     * 播放上一首
     */
    public void playNext() {
        int nextIndex = MusicPlayData.sCurrentPlayIndex + 1;
        if (nextIndex > MusicPlayData.sMusicList.size() - 1) {
            return;
        }
        MusicPlayData.sCurrentPlayIndex = nextIndex;
        MusicPlayData.sCurrentPlayPosition = 0;
        MusicPlayData.sIsPlayNew = true;
        playMusic();
    }

    /**
     * 播放上一首
     */
    public void playPrevious() {
        int previousIndex = MusicPlayData.sCurrentPlayIndex - 1;
        if (previousIndex < 0) {
            return;
        }
        MusicPlayData.sCurrentPlayIndex = previousIndex;
        MusicPlayData.sCurrentPlayPosition = 0;
        MusicPlayData.sIsPlayNew = true;
        playMusic();
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    private void scheduleUpdate() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (MusicPlayState.sCurrentState == MusicPlayState.PLAY_STATE_PLAYING) {
                    MusicPlayData.sCurrentPlayPosition = mMediaPlayer.getCurrentPosition();
                    Intent intent = new Intent();
                    intent.setAction(UPDATE_PROGRESS);
                    sendBroadcast(intent);
                }
            }
        }, 0, 1000);
    }

    private void showNotification() {
        String songName = MusicPlayData.sMusicList.get(MusicPlayData.sCurrentPlayIndex).getMusicName();
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_music_play);
        builder.setTicker("SPlayer");
        builder.setContentTitle("SPlayer");
        builder.setContentText(songName);
        builder.setContentIntent(pi);
        Intent actionIntent = new Intent(this, PlayerService.class);
        PendingIntent actionPendingIntent = PendingIntent.getService(this, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.mipmap.ic_music_pause, "暂停", actionPendingIntent);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notifaction);
        views.setCharSequence(R.id.musicName, "setText", songName);

        builder.setContent(views);

        startForeground(NOTIFICATION_ID, builder.build());
    }
}
