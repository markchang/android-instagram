package org.acmelab.andgram;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import com.google.android.maps.MapView;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ImageListActivity extends Activity {

    private static final String TAG = "ANDGRAM";

    ListView list;
    LazyAdapter adapter;
    ArrayList<String> imageList;
    ArrayList<String> imageInfoList;
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
        imageList = new ArrayList<String>();
        imageInfoList = new ArrayList<String>();

        if( !Utils.doLogin(getApplicationContext(), httpClient) ) {
            Toast.makeText(ImageListActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
        } else {
            try {
                HttpGet httpGet = new HttpGet(Utils.TIMELINE_URL);
                HttpResponse httpResponse = httpClient.execute(httpGet);

                // test result code
                if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                    Log.e(TAG, "Timeline fetch fail");
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

                        for( int i=0; i< items.length(); i++ ) {
                            // image
                            entry = (JSONObject)items.get(i);
                            imageVersions = entry.getJSONArray("image_versions");
                            bigImage = (JSONObject)imageVersions.get(0);
                            imageList.add(bigImage.getString("url"));

                            // user
                            user = entry.getJSONObject("user");
                            imageInfoList.add(user.getString("full_name"));
                            Log.i(Utils.TAG, "User " + imageInfoList.get(i) + " -> " +
                                    imageList.get(i));
                        }

                        return true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void displayActivity() {
        adapter=new LazyAdapter(this,
                imageList.toArray(new String[imageList.size()]),
                imageInfoList.toArray(new String[imageInfoList.size()])
        );
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