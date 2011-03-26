package org.acmelab.andgram;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class LazyAdapter extends BaseAdapter {

    private Activity activity;
    private ArrayList<InstagramImage> instagramImageArrayList;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader;

    public LazyAdapter(Activity a, ArrayList<InstagramImage> i) {
        activity = a;
        instagramImageArrayList = i;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader = new ImageLoader(activity.getApplicationContext());
    }

    public int getCount() {
        return instagramImageArrayList.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public static class ViewHolder{
        public TextView username;
        public TextView comments;
        public TextView caption;
        public ImageView image;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        ViewHolder holder;
        if(convertView==null){
            vi = inflater.inflate(R.layout.image_list_item, null);
            holder = new ViewHolder();
            holder.image = (ImageView)vi.findViewById(R.id.image);
            holder.username = (TextView)vi.findViewById(R.id.username);
            holder.comments = (TextView)vi.findViewById(R.id.comments);
            holder.caption = (TextView)vi.findViewById(R.id.caption);
            vi.setTag(holder);
        }
        else
            holder=(ViewHolder)vi.getTag();

        holder.image.setTag(instagramImageArrayList.get(position).getUrl());
        holder.username.setText(instagramImageArrayList.get(position).getUsername());
        holder.caption.setText(instagramImageArrayList.get(position).getCaption());
        holder.comments.setText(instagramImageArrayList.get(position).getComments());

        imageLoader.DisplayImage(instagramImageArrayList.get(position).getUrl(), activity, holder.image);

        return vi;
    }
}