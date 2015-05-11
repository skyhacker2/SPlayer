package com.eleven.app.splayer;

import java.util.ArrayList;

/**
 * Created by eleven on 15/5/8.
 */
public class MusicPlayData {
    public static ArrayList<MusicData> sMusicList = new ArrayList<>();

    public static int sCurrentPlayIndex = 0;

    public static int sCurrentPlayPosition = 0;

    public static boolean sIsPlayNew = true;

    public static int sTotalTime = 0;
}
