package org.acmelab.andgram;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ImageListActivity extends Activity {

    private static final String TAG = "ANDGRAM";

    ListView list;
    LazyAdapter adapter;
    ArrayList<InstagramImage> instagramImageList;
    DefaultHttpClient httpClient = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_list);

        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "Instagram");

        list=(ListView)findViewById(R.id.list);
        fetchActivity();
        displayActivity();
    }

    public boolean fetchActivity() {
        Log.i(Utils.TAG, "Image fetch");

        if( Utils.isOnline(getApplicationContext()) == false ) {
            Toast.makeText(ImageListActivity.this,
                    "No connection to Internet.\nTry again later.",
                    Toast.LENGTH_SHORT).show();
            Log.i(Utils.TAG, "No internet, didn't load Activity Feed");
            return false;
        }

        instagramImageList = new ArrayList<InstagramImage>();

        if( !Utils.doLogin(getApplicationContext(), httpClient) ) {
            Toast.makeText(ImageListActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
        } else {
            try {
                HttpGet httpGet = new HttpGet(Utils.TIMELINE_URL);
                HttpResponse httpResponse = httpClient.execute(httpGet);

                // test result code
                if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                    Toast.makeText(ImageListActivity.this, "Login failed.", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(ImageListActivity.this,
                                "Activity feed did not return status ok",
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "JSON status not ok: " + jsonObject.getString("status"));

                        return false;
                    } else {
                        // parse the activity feed
                        Log.i(Utils.TAG, "Getting images from activity feed");
                        JSONArray items = jsonObject.getJSONArray("items");

                        // get image URLs and commentary
                        JSONObject entry;
                        JSONArray imageVersions;
                        JSONObject bigImage;
                        JSONObject user;
                        JSONArray comments;
                        JSONObject comment;
                        StringBuilder commentString;


                        for( int i=0; i< items.length(); i++ ) {
                            // create a new instance
                            InstagramImage instagramImage = new InstagramImage();

                            // image
                            entry = (JSONObject)items.get(i);
                            imageVersions = entry.getJSONArray("image_versions");
                            bigImage = (JSONObject)imageVersions.get(0);
                            instagramImage.setUrl(bigImage.getString("url"));

                            // user
                            user = entry.getJSONObject("user");
                            instagramImage.setUsername(user.getString("full_name"));

                            // comments (and caption)
                            comments = entry.getJSONArray("comments");
                            if( comments != null ) {
                                commentString = new StringBuilder();
                                for( int c=0; c < comments.length(); c++ ) {
                                    comment = comments.getJSONObject(c);
                                    if(c==0) {
                                        instagramImage.setCaption(comment.getString("text"));
                                    } else {
                                        user = comment.getJSONObject("user");
                                        commentString.append("\n"+user.getString("full_name") + ": ");
                                        commentString.append(comment.getString("text"));
                                    }
                                }
                                instagramImage.setComments(commentString.toString());
                            }


                            instagramImageList.add(instagramImage);
                        }

                        return true;
                    }
                } else {
                    Toast.makeText(ImageListActivity.this,
                            "Improper data returned from Instagram.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "instagram returned bad data");
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void displayActivity() {
        adapter=new LazyAdapter(this, instagramImageList);
        list.setAdapter(adapter);
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

}