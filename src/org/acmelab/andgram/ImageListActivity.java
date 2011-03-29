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

public class ImageListActivity extends Activity {

    private static final String TAG = "ANDGRAM";
    private String sourceUrl;

    ActionBar actionBar;
    ListView list;
    LazyAdapter adapter;
    ArrayList<InstagramImage> instagramImageList;
    DefaultHttpClient httpClient = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_list);

        Bundle extras = getIntent().getExtras();
        sourceUrl = extras.getString("url");
        int titleId = extras.getInt("title");
        String title = getResources().getString(titleId);

        Intent dashboardIntent = new Intent(getApplicationContext(), DashboardActivity.class);
        dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        actionBar = (ActionBar) findViewById(R.id.imageListActionbar);
        actionBar.setTitle(title);
        actionBar.addAction(new RefreshAction());
        final ActionBar.Action goHomeAction = new ActionBar.IntentAction(this,
                dashboardIntent, R.drawable.ic_title_home);
        actionBar.addAction(goHomeAction);

        list=(ListView)findViewById(R.id.list);
        list.setOnItemClickListener(itemClickListener);

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

    private void refresh() {
        instagramImageList.clear();
        adapter.notifyDataSetChanged();
        new FetchActivity().execute();
    }


    public AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Log.i(Utils.TAG, "Clicked: " + String.valueOf(i));

            final InstagramImage instagramImage = (InstagramImage)adapter.getItem(i);
            final String username = Utils.getUsername(getApplicationContext());

            // build dialog
            List<String> dialogItems = new ArrayList<String>();

            // 0: like/unlike
            if( instagramImage.liker_list != null ) {
                if( instagramImage.liker_list.contains(username) ) {
                    dialogItems.add("Unlike");
                } else {
                    dialogItems.add("Like");
                }
            } else {
                dialogItems.add("Like");
            }

            // 1: comment
            dialogItems.add("Comment");

            // 2: share
            if( instagramImage.username.equals(username) ) {
                dialogItems.add("Share");
            }

            // 3: delete
            if( instagramImage.username.equals(username) ) {
                dialogItems.add("Delete");
            }

            final CharSequence[] items = dialogItems.toArray(new String[dialogItems.size()]);

            AlertDialog.Builder builder = new AlertDialog.Builder(ImageListActivity.this);
            builder.setTitle("Choose your action");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item) {
                        case 0:
                            likeUnlike(instagramImage, username);
                            break;
                        case 1:
                            showCommentDialog(instagramImage, username);
                            break;
                        case 2:
                            showShareDialog(instagramImage);
                            break;
                        case 3:
                            showDeleteDialog(instagramImage);
                            break;
                        default:
                            break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        }
    };

    public void showShareDialog(InstagramImage image) {
        final InstagramImage finalImage = image;

        // get the permalink
        String url = Utils.createPermalinkUrl(finalImage.pk);
        String jsonResponse = Utils.doRestfulGet(httpClient, url, getApplicationContext());
        if( jsonResponse != null ) {
            try {
                JSONTokener jsonTokener = new JSONTokener(jsonResponse);
                JSONObject jsonObject = new JSONObject(jsonTokener);
                String permalink = jsonObject.getString("permalink");
                if( permalink != null ) {
                    // shoot the intent
                    // will default to "messaging / sms" if nothing else is installed
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    //Text seems to be necessary for Facebook and Twitter
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, permalink);
                    startActivity(Intent.createChooser(sharingIntent,"Share using"));
                }
            } catch (JSONException j) {
                Log.e(TAG, "JSON parse error: " + j.toString());
                Toast.makeText(getApplicationContext(),
                        "There was an error communicating with Instagram",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    "Failed to get permalink for the image", Toast.LENGTH_SHORT).show();
        }

    }



    public void showDeleteDialog(InstagramImage image) {
        final InstagramImage finalImage = image;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this image?")
               .setCancelable(false)
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       String url = Utils.createDeleteUrl(finalImage.pk);
                       String jsonResponse = Utils.doRestfulGet(httpClient, url, getApplicationContext());
                       if( jsonResponse != null ) {
                           instagramImageList.remove(finalImage);
                           adapter.notifyDataSetChanged();
                       } else {
                           Toast.makeText(getApplicationContext(),
                                   "Delete failed", Toast.LENGTH_SHORT).show();
                       }
                   }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                   }
               });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void showCommentDialog(InstagramImage image, String username) {
        final InstagramImage finalImage = image;
        final String finalUsername = username;

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Comment");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String comment = input.getText().toString();
                postComment(comment, finalImage, finalUsername);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void postComment(String comment, InstagramImage image, String username) {
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("comment_text", comment));
        String jsonResponse = Utils.doRestulPut(httpClient,
                Utils.createCommentUrl(image.pk),
                postParams,
                this);
        if( jsonResponse != null ) {
            image.comment_list.add(new Comment(username,comment));
            Toast.makeText(this,
                    "Comment successful", Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this,
                    "Comment failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void likeUnlike(InstagramImage image, String username) {
        String url;

        if( image.liker_list != null ) {
            if( image.liker_list.contains(Utils.getUsername(getApplicationContext()))) {
                unlike(image,username);
            } else {
                like(image, username);
            }
        } else {
            like(image, username);
        }

    }

    public void like(InstagramImage image, String username) {
        String url = Utils.createLikeUrl(image.pk);
        String jsonResponse = Utils.doRestfulGet(httpClient, url, ImageListActivity.this);
        if( jsonResponse != null ) {
            if( image.liker_list == null ) image.liker_list = new ArrayList<String>();
            image.liker_list.add(username);
            adapter.notifyDataSetChanged();
        }
    }

    public void unlike(InstagramImage image, String username) {
        String url = Utils.createUnlikeUrl(image.pk);
        String jsonResponse = Utils.doRestfulGet(httpClient, url, ImageListActivity.this);
        if( jsonResponse != null ) {
            if( image.liker_list == null ) image.liker_list = new ArrayList<String>();
            image.liker_list.remove(username);
            adapter.notifyDataSetChanged();
        }
    }

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
                    HttpGet httpGet = new HttpGet(sourceUrl);
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
                                instagramImage.pk = ((Long)entry.getLong("pk")).toString();

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

    private class RefreshAction implements ActionBar.Action {

        public int getDrawable() {
            return R.drawable.ic_title_refresh;
        }

        public void performAction(View view) {
            refresh();
        }
    }
}