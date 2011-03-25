package org.acmelab.andgram;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
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
    public static final int CROP_REQUEST = 2;
    public static final String OUTPUT_DIR = "andgram";
    public static final String OUTPUT_FILE = "andgram.jpg";
    public static final String OUTPUT_FILE_CROPPED = "andgram_cropped.jpg";
    public static final int ID_MAIN = 1;

    public static final String LOGIN_URL = "https://instagr.am/api/v1/accounts/login/";
    public static final String LOGOUT_URL = "http://instagr.am/api/v1/accounts/logout/";
    public static final String UPLOAD_URL = "http://instagr.am/api/v1/media/upload/";
    public static final String CONFIGURE_URL = "https://instagr.am/api/v1/media/configure/";
    public static final String TIMELINE_URL = "http://instagr.am/api/v1/feed/timeline/";


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