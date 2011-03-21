package org.acmelab.andgram;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends Activity {
    private static final String TAG = "ANDGRAM";
    private static final String LOGIN_URL = "https://instagr.am/api/v1/accounts/login/";
    private static final String LOGOUT_URL = "http://instagr.am/api/v1/accounts/logout/";
    private static final String PREFS_NAME = "andgram_prefs";

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
        Log.i(TAG, "Login: " + loginValid.toString());

        if( loginValid ) {
            // call other activity
        }
    }

    public boolean saveLoginInfo( CookieStore cookieStore, String username, String password ) {

        List<Cookie> cookieList = cookieStore.getCookies();
        if( cookieList.size() < 3 ) {
            // save username/password in shared preferences
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.putBoolean("loginValid", false);
            editor.commit();
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

    public void doLogin(View view) {
        StringBuilder cookieString;
        CookieStore cookieStore;

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
            if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                return;
            }

            HttpEntity httpEntity = httpResponse.getEntity();

            cookieStore = httpClient.getCookieStore();
            if( saveLoginInfo(cookieStore, username, password) == true ) {
                Toast.makeText(LoginActivity.this, "Logged in", Toast.LENGTH_SHORT).show();
                // call main activity
            } else {
                Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
            }
        } catch( Exception e ) {
            Log.e(TAG, "HttpPost error: " + e.toString());
            Toast.makeText(LoginActivity.this, "Login failed " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}