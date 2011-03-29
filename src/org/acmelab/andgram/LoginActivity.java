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
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends Activity {

    private static final String TAG = Utils.TAG;
    EditText txtPassword = null;
    EditText txtUsername = null;
    DefaultHttpClient httpClient = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        txtPassword = (EditText)findViewById(R.id.txtPassword);
        txtUsername = (EditText)findViewById(R.id.txtUsername);
        httpClient = new DefaultHttpClient();

        SharedPreferences sharedPreferences = getSharedPreferences(Utils.PREFS_NAME, MODE_PRIVATE);
        Boolean loginValid = sharedPreferences.getBoolean("loginValid",false);

        if( loginValid ) {
            finish();
            openMainActivity();
        }
    }

    public void openMainActivity() {
        // Intent mainIntent = new Intent(LoginActivity.this, TakePictureActivity.class);
        Intent mainIntent = new Intent(LoginActivity.this, DashboardActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainIntent);
    }



    public boolean saveLoginInfo( CookieStore cookieStore, String username, String password ) {

        List<Cookie> cookieList = cookieStore.getCookies();
        if( cookieList.size() < 3 )  {
            clearCookies();
            return false;
        }
        else {
            SharedPreferences sharedPreferences = getSharedPreferences(Utils.PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("loginValid", true);
            editor.putString("username", username);
            editor.putString("password", password);

            // find user_id (pk)
            for( Cookie c : cookieList ) {
                if( c.getName().equals("ds_user_id") ) {
                    editor.putString("pk", c.getValue());
                }
            }

            // save username/password in shared preferences
            editor.commit();
            return true;
        }

    }

    public void clearCookies() {
        // clear username/password in shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences(Utils.PREFS_NAME, MODE_PRIVATE);
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

        if( Utils.isOnline(getApplicationContext()) == false ) {
            Toast.makeText(LoginActivity.this,
                    "No connection to Internet.\nTry again later.",
                    Toast.LENGTH_SHORT).show();
            Log.i(Utils.TAG, "No internet, failed Login");
            return;
        }

        // create POST
        HttpPost httpPost = new HttpPost(Utils.LOGIN_URL);
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
            HttpEntity httpEntity = httpResponse.getEntity();
            if( httpEntity != null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                String json = reader.readLine();
                JSONTokener jsonTokener = new JSONTokener(json);
                JSONObject jsonObject = new JSONObject(jsonTokener);
                Log.i(TAG,"JSON: " + jsonObject.toString());

                String loginStatus = jsonObject.getString("status");

                if( !loginStatus.equals("ok") ) {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "JSON status not ok: " + jsonObject.getString("status"));
                    return;
                }
            }

            // save login info so that we can reuse them later
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