package org.acmelab.andgram;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity
{
    private static final int CAMERA_PIC_REQUEST = 1337;
    private static final String TAG = "ANDGRAM";
    private static final String OUTPUT_DIR = "andgram";
    private static final int ID_MAIN = 1;

    private static final String UPLOAD_URL = "http://instagr.am/api/v1/media/upload/";
    private static final String CONFIGURE_URL = "https://instagr.am/api/v1/media/configure/";

    EditText txtCaption  = null;
    ImageView imageView = null;
    Button uploadButton = null;

    private DefaultHttpClient httpClient = null;
    private Uri imageUri = null;
    private boolean imageReady = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "Instagram");

        txtCaption  = (EditText)findViewById(R.id.txtCaption);
        imageView = (ImageView)findViewById(R.id.imageView);
        uploadButton = (Button)findViewById(R.id.btnUpload);


        // create the output dir for us
        File outputDirectory = new File(Environment.getExternalStorageDirectory(), OUTPUT_DIR);
        outputDirectory.mkdirs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch( item.getItemId() ) {
            case R.id.refresh:
                return true;
            case R.id.preferences:
                return true;
            case R.id.credentials:
                launchCredentials();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void launchCredentials() {
        SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putBoolean("loginValid", false);
        editor.commit();

        Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(loginIntent);
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

    public void doClear(View view) {
        Log.i(TAG, "Clear");
        if( imageReady ) {
            imageReady = false;
            ((BitmapDrawable)imageView.getDrawable()).getBitmap().recycle();
            imageView.setImageBitmap(null);
        }
    }

    public Map<String, String> doUpload() {
        Log.i(TAG, "Upload");
        Long timeInMilliseconds = System.currentTimeMillis()/1000;
        String timeInSeconds = timeInMilliseconds.toString();
        MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        Map returnMap = new HashMap<String, String>();

        // check for cookies
        if( httpClient.getCookieStore() == null ) {
            returnMap.put("result", "Not logged in");
            return returnMap;
        }

        try {
            // create multipart data
            File imageFile = new File(imageUri.getPath());
            FileBody partFile = new FileBody(imageFile);
            StringBody partTime = new StringBody(timeInSeconds);
            multipartEntity.addPart("photo", partFile );
            multipartEntity.addPart("device_timestamp", partTime);
        } catch ( Exception e ) {
            Log.e(TAG,"Error creating mulitpart form: " + e.toString());
            //Toast.makeText(MainActivity.this, "Create multipart failed " + e.toString(), Toast.LENGTH_LONG).show();
            returnMap.put("result", "Error creating mulitpart form: " + e.toString());
            return returnMap;
        }

        // upload
        try {
            HttpPost httpPost = new HttpPost(UPLOAD_URL);
            httpPost.setEntity(multipartEntity);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            Log.i(TAG, "Upload response: " + httpResponse.getStatusLine());
        } catch( Exception e ) {
            Log.e(TAG, "HttpPost error: " + e.toString());
            //Toast.makeText(MainActivity.this, "Upload failed " + e.toString(), Toast.LENGTH_LONG).show();
            returnMap.put("result", "Upload failed " + e.toString());
            return returnMap;
        }

        // configure / comment
        try {
            HttpPost httpPost = new HttpPost(CONFIGURE_URL);

            String partComment = txtCaption.getText().toString();

            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
            postParams.add(new BasicNameValuePair("device_timestamp", timeInSeconds));
            postParams.add(new BasicNameValuePair("caption", partComment));
            httpPost.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            Log.i(TAG, "Configure response: " + httpResponse.getStatusLine());
            //Toast.makeText(MainActivity.this, "Upload complete", Toast.LENGTH_SHORT).show();
            returnMap.put("result", httpResponse.getStatusLine().toString());
            return returnMap;
        } catch( Exception e ) {
            Log.e(TAG, "HttpPost error: " + e.toString());
            //Toast.makeText(MainActivity.this, "Configure failed " + e.toString(), Toast.LENGTH_LONG).show();
            returnMap.put("result", "HttpPost error: " + e.toString());
            return returnMap;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_PIC_REQUEST && resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "Camera returned");
            getContentResolver().notifyChange(imageUri, null);
            ContentResolver contentResolver = getContentResolver();
            Bitmap imageBitmap;
            try {
                Drawable toRecycle =  imageView.getDrawable();
                if( toRecycle != null ) {
                    Bitmap bitmapToRecycle = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                    if( bitmapToRecycle != null ) {
                        ((BitmapDrawable)imageView.getDrawable()).getBitmap().recycle();
                    }
                }
                imageBitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
                imageView.setImageBitmap(imageBitmap);
                Log.i(TAG, "Image: " + imageUri.toString());
                Toast.makeText(MainActivity.this, imageUri.toString(), Toast.LENGTH_LONG).show();
                imageReady = true;

                // turn on upload button
                if( httpClient.getCookieStore() != null ) uploadButton.setEnabled(true);
            } catch ( Exception e ) {
                Toast.makeText(MainActivity.this, "Camera error", Toast.LENGTH_LONG).show();
                Log.e(TAG, e.toString() );
                imageReady = false;
                imageUri = null;
            }
        }
    }

    private class UploadPhoto extends AsyncTask<Void, Void, Map<String, String>> {

        protected void onPreExecute() {
            Toast.makeText(MainActivity.this, "Upoading in the background", Toast.LENGTH_SHORT).show();
        }

        protected Map<String,String> doInBackground(Void... voids) {
            return doUpload();
        }

        protected void onPostExecute(Map<String,String> resultMap) {
            Toast.makeText(MainActivity.this, resultMap.get("result"), Toast.LENGTH_SHORT).show();
        }
    }

}
