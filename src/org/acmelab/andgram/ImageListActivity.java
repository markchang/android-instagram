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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ImageListActivity extends Activity {

    private static final String TAG = "ANDGRAM";

    ActionBar actionBar;
    ListView list;
    LazyAdapter adapter;
    ArrayList<InstagramImage> instagramImageList;
    DefaultHttpClient httpClient = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_list);

        Intent dashboardIntent = new Intent(getApplicationContext(), DashboardActivity.class);
        dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        actionBar = (ActionBar) findViewById(R.id.imageListActionbar);
        actionBar.setTitle(R.string.activity);
        final ActionBar.Action goHomeAction = new ActionBar.IntentAction(this,
                dashboardIntent, R.drawable.ic_title_home_default);
        actionBar.addAction(goHomeAction);
        list=(ListView)findViewById(R.id.list);
        //list.setOnItemClickListener(myClickListener);

        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "Instagram");

        init();
    }

    private void init() {
        // set that list to background downloader
        instagramImageList = new ArrayList<InstagramImage>();
        adapter = new LazyAdapter(this, instagramImageList);
        list.setAdapter(adapter);
        new FetchActivity().execute();
    }


    public AdapterView.OnItemClickListener myClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Log.i(Utils.TAG, "Clicked: " + String.valueOf(i));
            InstagramImage instagramImage = (InstagramImage)adapter.getItem(i);
            String username = Utils.getUsername(getApplicationContext());

            // build dialog
            final CharSequence[] items = {"Like", "Unlike"};

            AlertDialog.Builder builder = new AlertDialog.Builder(ImageListActivity.this);
            builder.setTitle("Pick a color");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        }
    };

    @Override
    public void onDestroy()
    {
        adapter.imageLoader.stopThread();
        list.setAdapter(null);
        super.onDestroy();
    }

    public void clearCache(View view) {
        adapter.imageLoader.clearCache();
        adapter.notifyDataSetChanged();
    }

    private class FetchActivity extends AsyncTask<Void, String, Boolean> {
        protected void onPreExecute() {
            actionBar.setProgressBarVisibility(View.VISIBLE);
        }

        protected void onPostExecute(Boolean result) {
            actionBar.setProgressBarVisibility(View.GONE);
            if(result) {
                adapter.notifyDataSetChanged();
            }
        }

        protected void onProgressUpdate(String toastText) {
            Toast.makeText(ImageListActivity.this, toastText, Toast.LENGTH_SHORT).show();
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
                    HttpGet httpGet = new HttpGet(Utils.TIMELINE_URL);
                    HttpResponse httpResponse = httpClient.execute(httpGet);

                    // test result code
                    if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                        publishProgress("Login failed.");
                        Log.e(TAG, "Login status code bad.");
                        return false;
                    }

                    // test json response
                    HttpEntity httpEntity = httpResponse.getEntity();
                    if( httpEntity != null ) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                        String json = reader.readLine();
                        JSONTokener jsonTokener = new JSONTokener(json);
                        JSONObject jsonObject = new JSONObject(jsonTokener);
                        Log.i(TAG,"JSON: " + jsonObject.toString());

                        String loginStatus = jsonObject.getString("status");

                        if( !loginStatus.equals("ok") ) {
                            publishProgress("Activity feed did not return status ok");
                            Log.e(TAG, "JSON status not ok: " + jsonObject.getString("status"));
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
                                JSONObject bigImage = (JSONObject)imageVersions.get(0);
                                instagramImage.url = bigImage.getString("url");

                                // user
                                JSONObject user = entry.getJSONObject("user");
                                instagramImage.username = user.getString("full_name");

                                // date taken_at
                                Long dateLong = entry.getLong("taken_at");
                                SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy HH:mm");
                                instagramImage.taken_at = formatter.format(new Date(dateLong * 1000L));

                                // comments (and caption)
                                JSONArray comments = entry.getJSONArray("comments");
                                if( comments != null ) {
                                    StringBuilder commentString = new StringBuilder();
                                    for( int c=0; c < comments.length(); c++ ) {
                                        JSONObject comment = comments.getJSONObject(c);
                                        user = comment.getJSONObject("user");
                                        if(c==0) {
                                            instagramImage.caption = "<b>" + user.getString("username") + "</b> " +
                                                    comment.getString("text");
                                        } else {
                                            commentString.append("<b>" + user.getString("username") + "</b> ");
                                            commentString.append(comment.getString("text") + "<br />");
                                        }
                                    }
                                    instagramImage.comments = commentString.toString();
                                }

                                // likers
                                try {
                                    JSONArray liker_ids = entry.getJSONArray("liker_ids");
                                    if( liker_ids != null) {
                                        instagramImage.likers = "<b>Liked by " + liker_ids.length() +
                                                " people</b>";
                                    }
                                } catch( JSONException j ) {}

                                try {
                                    JSONArray likers = entry.getJSONArray("likers");
                                    if( likers != null ) {
                                        StringBuilder likerString = new StringBuilder();
                                        if( likers.length() > 0 ) {
                                            likerString.append("Liked by: <b>");
                                            for( int l=0; l < likers.length(); l++ ) {
                                                JSONObject like = likers.getJSONObject(l);
                                                likerString.append(like.getString("username") + " ");
                                            }
                                            likerString.append("</b>");
                                            instagramImage.likers = likerString.toString();
                                        }
                                    }
                                } catch( JSONException j ) {}

                                instagramImageList.add(instagramImage);
                            }

                            return true;
                        }
                    } else {
                        publishProgress("Improper data returned from Instagram");
                        Log.e(TAG, "instagram returned bad data");
                        return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

    }

}