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
 */

package org.acmelab.andgram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.markupartist.android.widget.ActionBar;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImageGridActivity extends Activity {

    private String sourceUrl;

    GridView grid;
    LazyGridAdapter adapter;
    ArrayList<InstagramImage> instagramImageList;
    DefaultHttpClient httpClient = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_grid);

        Bundle extras = getIntent().getExtras();
        sourceUrl = extras.getString("url");
        grid = (GridView)findViewById(R.id.gridview);

        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "Instagram");

        init();
    }

    private void init() {
        // set that list to background downloader
        instagramImageList = new ArrayList<InstagramImage>();
        adapter = new LazyGridAdapter(this, instagramImageList);
        grid.setAdapter(adapter);
        new FetchActivity().execute();
    }

    private void refresh() {
        instagramImageList.clear();
        adapter.notifyDataSetChanged();
        new FetchActivity().execute();
    }


    @Override
    public void onDestroy()
    {
        adapter.imageLoader.stopThread();
        grid.setAdapter(null);
        super.onDestroy();
    }

    public void clearCache(View view) {
        adapter.imageLoader.clearCache();
        adapter.notifyDataSetChanged();
    }

    private class FetchActivity extends AsyncTask<Void, String, Boolean> {
        protected void onPreExecute() {
        }

        protected void onPostExecute(Boolean result) {
            if(result) {
                adapter.notifyDataSetChanged();
            }
        }

        protected void onProgressUpdate(String toastText) {
            Toast.makeText(ImageGridActivity.this, toastText, Toast.LENGTH_SHORT).show();
        }

        protected Boolean doInBackground(Void... voids) {
            Log.i(Utils.TAG, "Image fetch");

            if( Utils.isOnline(getApplicationContext()) == false ) {
                publishProgress("No connection to Internet.\nTry again later");
                Log.i(Utils.TAG, "No internet, didn't load Activity Feed");
                return false;
            }

            if( !Utils.doLogin(getApplicationContext(), httpClient) ) {
                publishProgress("Login failed");
                return false;
            } else {
                try {
                    HttpGet httpGet = new HttpGet(sourceUrl);
                    HttpResponse httpResponse = httpClient.execute(httpGet);

                    // test result code
                    if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                        publishProgress("Login failed.");
                        Log.e(Utils.TAG, "Login status code bad.");
                        return false;
                    }

                    // test json response
                    HttpEntity httpEntity = httpResponse.getEntity();
                    if( httpEntity != null ) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                        String json = reader.readLine();
                        JSONTokener jsonTokener = new JSONTokener(json);
                        JSONObject jsonObject = new JSONObject(jsonTokener);
                        Log.i(Utils.TAG,"JSON: " + jsonObject.toString());

                        String loginStatus = jsonObject.getString("status");

                        if( !loginStatus.equals("ok") ) {
                            publishProgress("Activity feed did not return status ok");
                            Log.e(Utils.TAG, "JSON status not ok: " + jsonObject.getString("status"));
                            return false;
                        } else {
                            // parse the activity feed
                            Log.i(Utils.TAG, "Getting images from activity feed");
                            JSONArray items = jsonObject.getJSONArray("items");

                            // get image URLs and commentary
                            for( int i=0; i< items.length(); i++ ) {
                                // create a new instance
                                InstagramImage instagramImage = new InstagramImage();

                                // image
                                JSONObject entry = (JSONObject)items.get(i);
                                JSONArray imageVersions = entry.getJSONArray("image_versions");
                                JSONObject bigImage = (JSONObject)imageVersions.get(2);
                                instagramImage.url = bigImage.getString("url");
                                instagramImage.pk = ((Long)entry.getLong("pk")).toString();

                                // user
                                JSONObject user = entry.getJSONObject("user");
                                instagramImage.username = user.getString("username");

                                // date taken_at
                                Long dateLong = entry.getLong("taken_at");
                                SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy HH:mm");
                                instagramImage.taken_at = formatter.format(new Date(dateLong * 1000L));

                                // comments (and caption)
                                JSONArray comments = entry.getJSONArray("comments");
                                if( comments != null ) {
                                    ArrayList<Comment> commentList = new ArrayList<Comment>();
                                    for( int c=0; c < comments.length(); c++ ) {
                                        JSONObject comment = comments.getJSONObject(c);
                                        user = comment.getJSONObject("user");
                                        if(c==0) {
                                            instagramImage.caption = comment.getString("text");
                                        } else {
                                            commentList.add(new Comment(user.getString("username"),
                                                    comment.getString("text")));
                                        }
                                    }
                                    instagramImage.comment_list = commentList;
                                }

                                // likers
                                try {
                                    JSONArray liker_ids = entry.getJSONArray("liker_ids");
                                    if( liker_ids != null) {
                                        if( liker_ids.length() > 0 ) {
                                            ArrayList<String> likerList = new ArrayList<String>();
                                            likerList.add(Integer.toString(liker_ids.length()));
                                            instagramImage.liker_list = likerList;
                                            instagramImage.liker_list_is_count = true;
                                        }
                                    }
                                } catch( JSONException j ) {}

                                try {
                                    JSONArray likers = entry.getJSONArray("likers");
                                    if( likers != null ) {
                                        ArrayList<String> likerList = new ArrayList<String>();
                                        StringBuilder likerString = new StringBuilder();
                                        if( likers.length() > 0 ) {
                                            likerString.append("Liked by: <b>");
                                            for( int l=0; l < likers.length(); l++ ) {
                                                JSONObject like = likers.getJSONObject(l);
                                                likerString.append(like.getString("username") + " ");
                                                likerList.add(like.getString("username"));
                                            }
                                            likerString.append("</b>");
                                            instagramImage.liker_list = likerList;
                                            instagramImage.liker_list_is_count = false;
                                        }
                                    }
                                } catch( JSONException j ) {}

                                instagramImageList.add(instagramImage);
                            }

                            return true;
                        }
                    } else {
                        publishProgress("Improper data returned from Instagram");
                        Log.e(Utils.TAG, "instagram returned bad data");
                        return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
    }

    private class RefreshAction implements ActionBar.Action {

        public int getDrawable() {
            return R.drawable.ic_title_refresh;
        }

        public void performAction(View view) {
            refresh();
        }
    }
}