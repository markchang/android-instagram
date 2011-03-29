/*
 * Copyright 2011, Mark L. Chang <mark.chang@gmail.com>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Mark L. Chang ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MARK L. CHANG OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Mark L. Chang.
 *
 * -----------------------
 * Code here for image listview handling was based heavily upon work done by
 * Fedor Vlasov (http://www.fedorvlasov.com/), originally posted here:
 * http://stackoverflow.com/questions/541966/android-how-do-i-do-a-lazy-load-of-images-in-listview/3068012#3068012
 *
 * No copyright was associated with the LazyList.zip source, so it was used and modified.
 */

package org.acmelab.andgram;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
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
        return instagramImageArrayList.get(position);
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
        View vi = convertView;
        ViewHolder holder;
        if(convertView == null){
            vi = inflater.inflate(R.layout.image_list_item, null);
            holder = new ViewHolder();
            holder.image = (ImageView)vi.findViewById(R.id.image);
            holder.username = (TextView)vi.findViewById(R.id.username);
            holder.comments = (TextView)vi.findViewById(R.id.comments);
            holder.caption = (TextView)vi.findViewById(R.id.caption);
            vi.setTag(holder);
        }
        else
            holder = (ViewHolder)vi.getTag();

        InstagramImage image = instagramImageArrayList.get(position);

        holder.image.setTag(image.url);
        holder.username.setText(Html.fromHtml("<b>" + image.username + "</b>") +
            " taken " + image.taken_at);
        holder.caption.setText(Html.fromHtml("<b>" + image.username + "</b> " + image.caption));

        // comments hold likes and comments
        StringBuilder likerString = new StringBuilder();
        if( image.liker_list != null ) {
            if( image.liker_list.size() > 0 ) {
                likerString.append("Liked by <b>");
                for( String liker : image.liker_list ) {
                    likerString.append(" " + liker);
                }
                likerString.append("</b><br />");
            }
        }

        // iterate over comments
        if( image.comment_list != null ) {
            if( image.comment_list.size() > 0 ) {
                for( Comment comment : image.comment_list ) {
                    likerString.append("<b>" + comment.username + "</b> ");
                    likerString.append(comment.comment + "<br />");
                }
            }
        }

        holder.comments.setText(Html.fromHtml(likerString.toString()));

        imageLoader.DisplayImage(image.url, activity, holder.image);

        return vi;
    }
}