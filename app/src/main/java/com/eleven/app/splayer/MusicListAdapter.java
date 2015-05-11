package com.eleven.app.splayer;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by eleven on 15/5/8.
 */
public class MusicListAdapter extends BaseAdapter {
    private static final String TAG = MusicListAdapter.class.getSimpleName();

    private ArrayList<MusicData> mMusicDatas;
    private Context mContext;

    private int mSelectedIndex = -1;

    public MusicListAdapter(Context context, ArrayList<MusicData> musicDatas) {
        mContext = context;
        mMusicDatas = musicDatas;
    }

    @Override
    public int getCount() {
        return mMusicDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return mMusicDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.musiclist_item, null, false);
            holder = new ViewHolder();
            holder.title = (TextView)convertView.findViewById(R.id.music_name);
            holder.author = (TextView)convertView.findViewById(R.id.music_author);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.title.setText(mMusicDatas.get(position).getFileName());
        holder.author.setText(mMusicDatas.get(position).getMusicArtist());

        if (position == mSelectedIndex) {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.list_selected));
            holder.title.setSelected(true);
            holder.title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        } else {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.white));
            holder.title.setEllipsize(TextUtils.TruncateAt.END);
        }

        return convertView;
    }

    void setSelectedIndex(int index) {
        mSelectedIndex = index;
    }

    class ViewHolder {
        public TextView title;
        public TextView author;
    }
}
