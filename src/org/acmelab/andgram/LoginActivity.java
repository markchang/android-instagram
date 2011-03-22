package org.acmelab.andgram;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
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
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends Activity {
    private static final String TAG = "ANDGRAM";
    private static final String LOGIN_URL = "https://instagr.am/api/v1/accounts/login/";
    private static final String LOGOUT_URL = "http://instagr.am/api/v1/accounts/logout/";
    public static final String PREFS_NAME = "andgram_prefs";

    EditText txtPassword = null;
    EditText txtUsername = null;
    DefaultHttpClient httpClient = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        txtPassword = (EditText)findViewById(R.id.txtPassword);
        txtUsername = (EditText)findViewById(R.id.txtUsername);
        httpClient = new DefaultHttpClient();

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Boolean loginValid = sharedPreferences.getBoolean("loginValid",false);

        if( loginValid ) {
            openMainActivity();
        }
    }

    public void openMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
    }



    public boolean saveLoginInfo( CookieStore cookieStore, String username, String password ) {

        List<Cookie> cookieList = cookieStore.getCookies();
        if( cookieList.size() < 3 )  {
            clearCookies();
            return false;
        }
        else {
            // save username/password in shared preferences
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("loginValid", true);
            editor.putString("username", username);
            editor.putString("password", password);
            editor.commit();
            return true;
        }

    }

    public void clearCookies() {
        // clear username/password in shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putBoolean("loginValid", false);
        editor.commit();
    }

    public void doLogin(View view) {
        CookieStore cookieStore;

        // clear cookies
        clearCookies();

        // gather login info
        String password = txtPassword.getText().toString();
        String username = txtUsername.getText().toString();

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
                Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Login HTTP status fail");
                return;
            }

            // test json response
            // should look like
            /*
            {"logged_in_user":
            {"username": "blah",
              "pk": 9999999,
              "profile_pic_url": "http://images.instagram.com/profiles/anonymousUser.jpg",
              "full_name": "blah"},
            "status": "ok"}
            */
            HttpEntity httpEntity = httpResponse.getEntity();
            if( httpEntity != null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                String json = reader.readLine();
                JSONTokener jsonTokener = new JSONTokener(json);
                JSONObject jsonObject = new JSONObject(jsonTokener);
                Log.i(TAG,"JSON: " + jsonObject.toString());

                String loginStatus = jsonObject.getString("status");

                if( loginStatus.equals("\"ok\"") ) {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "JSON status not ok: " + jsonObject.getString("status"));
                    return;
                }
            }

            // stash cookies
            cookieStore = httpClient.getCookieStore();

            if( saveLoginInfo(cookieStore, username, password) == true ) {
                Toast.makeText(LoginActivity.this, "Logged in", Toast.LENGTH_SHORT).show();
                openMainActivity();
            } else {
                Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Cookie error");
            }
        } catch( IOException e ) {
            Log.e(TAG, "HttpPost error: " + e.toString());
            Toast.makeText(LoginActivity.this, "Login failed "
                    + e.toString(), Toast.LENGTH_LONG).show();
        } catch( JSONException e ) {
            Log.e(TAG, "JSON parse error: " + e.toString());
            Toast.makeText(LoginActivity.this, "Result from instagr.am was unexpected: "
                    + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}