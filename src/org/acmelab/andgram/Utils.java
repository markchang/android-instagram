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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static final String TAG = "ANDGRAM";
    public static final String PREFS_NAME = "andgram_prefs";
    public static final int CAMERA_PIC_REQUEST = 1;
    public static final String OUTPUT_DIR = "andgram";
    public static final String OUTPUT_FILE = "andgram.jpg";
    public static final String OUTPUT_FILE_CROPPED = "andgram_cropped.jpg";
    public static final int ID_MAIN = 1;
    public static final int IMAGE_WIDTH = 612;
    public static final int IMAGE_HEIGHT = 612;
    public static final int IMAGE_BORDER = 24;
    public static final int IMAGE_CORNER_RADIUS = 35;
    public static final int IMAGE_JPEG_COMPRESSION_QUALITY = 75;

    public static final String LOGIN_URL = "https://instagr.am/api/v1/accounts/login/";
    public static final String LOGOUT_URL = "http://instagr.am/api/v1/accounts/logout/";
    public static final String UPLOAD_URL = "http://instagr.am/api/v1/media/upload/";
    public static final String CONFIGURE_URL = "https://instagr.am/api/v1/media/configure/";
    public static final String TIMELINE_URL = "http://instagr.am/api/v1/feed/timeline/";


    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if( cm.getActiveNetworkInfo() == null ) return false;
        return cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }

    public static void launchCredentials(Context ctx) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putBoolean("loginValid", false);
        editor.commit();

        Intent loginIntent = new Intent(ctx, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(loginIntent);
    }

    public static String getUsername(Context ctx) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        Boolean loginValid = sharedPreferences.getBoolean("loginValid",false);

        if( loginValid ) {
            return sharedPreferences.getString("username",null);
        } else {
            return null;
        }
    }



    public static boolean doLogin(Context ctx, DefaultHttpClient httpClient) {
        Log.i(TAG, "Doing login");

        // gather login info
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        Boolean loginValid = sharedPreferences.getBoolean("loginValid",false);

        if( !loginValid ) {
            launchCredentials(ctx);
            return false;
        }

        String username = sharedPreferences.getString("username","");
        String password = sharedPreferences.getString("password","");

        // create POST
        HttpPost httpPost = new HttpPost(LOGIN_URL);
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("username", username));
        postParams.add(new BasicNameValuePair("password", password));
        postParams.add(new BasicNameValuePair("device_id", "0000"));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpPost);

            // test result code
            if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                Log.i(TAG, "Login HTTP status fail");
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
                }
            }

        } catch( IOException e ) {
            Log.e(TAG, "HttpPost error: " + e.toString());
            return false;
        } catch( JSONException e ) {
            Log.e(TAG, "JSON parse error: " + e.toString());
            return false;
        }

        return true;
    }

}