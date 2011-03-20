package org.acmelab.andgram;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
{
    private static final int CAMERA_PIC_REQUEST = 1337;
    private static final String TAG = "ANDGRAM";
    private static final String OUTPUT_DIR = "andgram";

    private static final String LOGIN_URL = "https://instagr.am/api/v1/accounts/login/";
    private static final String LOGOUT_URL = "http://instagr.am/api/v1/accounts/logout/";
    private static final String UPLOAD_URL = "http://instagr.am/api/v1/media/upload/";
    private static final String CONFIGURE_URL = "https://instagr.am/api/v1/media/configure/";

    EditText txtPassword = null;
    EditText txtUsername = null;
    ImageView imageView = null;

    private DefaultHttpClient httpClient = null;
    private Uri imageUri;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "Instagram");

        txtPassword = (EditText)findViewById(R.id.txtPassword);
        txtUsername = (EditText)findViewById(R.id.txtUsername);
        imageView = (ImageView)findViewById(R.id.imageView);

        // create the output dir for us
        File outputDirectory = new File(Environment.getExternalStorageDirectory(), OUTPUT_DIR);
        outputDirectory.mkdirs();
    }

    public void takePicture(View view) {
        Log.i(TAG, "Taking picture");
        Long timeInMilliseconds = System.currentTimeMillis()/1000;
        String timeInSeconds = timeInMilliseconds.toString();
        File outputFile = new File(Environment.getExternalStorageDirectory(), OUTPUT_DIR + "/" + timeInSeconds + ".jpg");
        imageUri = Uri.fromFile(outputFile);
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    public CookieStore doLogin(View view) {
        Log.i(TAG, "Login button pressed");
        String password = txtPassword.getText().toString();
        String username = txtUsername.getText().toString();
        CookieStore cookieStore = null;

        HttpPost httpPost = new HttpPost(LOGIN_URL);
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("username", username));
        postParams.add(new BasicNameValuePair("password", password));
        postParams.add(new BasicNameValuePair("device_id", "0000"));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            Log.i(TAG, "Login form get: " + httpResponse.getStatusLine());

            Log.i(TAG, "Post logon cookies:");
            cookieStore = httpClient.getCookieStore();
            List<Cookie> cookieList = cookieStore.getCookies();
            if (cookieList.isEmpty()) {
                Log.i(TAG, "None");
            } else {
                for (int i = 0; i < cookieList.size(); i++) {
                    Log.i(TAG, "- " + cookieList.get(i).toString());
                }
                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
            }
        } catch( Exception e ) {
            Log.e(TAG, "HttpPost error: " + e.toString());
            Toast.makeText(this, "doLogin failed " + e.toString(), Toast.LENGTH_LONG).show();
        }

        return cookieStore;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_PIC_REQUEST && resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "Camera returned");
            getContentResolver().notifyChange(imageUri, null);
            ContentResolver contentResolver = getContentResolver();
            Bitmap imageBitmap;
            try {
                imageBitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
                imageView.setImageBitmap(imageBitmap);
                imageView.invalidate();
                Log.i(TAG, "Image: " + imageUri.toString());
                Toast.makeText(MainActivity.this, imageUri.toString(), Toast.LENGTH_LONG).show();;
            } catch ( Exception e ) {
                Toast.makeText(MainActivity.this, "Camera error", Toast.LENGTH_LONG).show();
                Log.e(TAG, e.toString() );
            }
        }
    }

}
