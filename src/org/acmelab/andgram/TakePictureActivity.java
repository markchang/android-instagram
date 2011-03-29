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
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.*;
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
import android.widget.*;
import com.markupartist.android.widget.ActionBar;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TakePictureActivity extends Activity
{
    private static final String TAG = Utils.TAG;

    EditText txtCaption  = null;
    ImageView imageView = null;
    Button uploadButton = null;

    private DefaultHttpClient httpClient = null;
    private Uri imageUri = null;
    private Uri croppedImageUri = null;
    private boolean imageReady = false;
    private ActionBar actionBar;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_picture);

        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter("http.useragent", "Instagram");

        txtCaption  = (EditText)findViewById(R.id.txtCaption);
        imageView = (ImageView)findViewById(R.id.imageView);
        uploadButton = (Button)findViewById(R.id.btnUpload);

        // create the output dir for us
        File outputDirectory = new File(Environment.getExternalStorageDirectory(), Utils.OUTPUT_DIR);
        outputDirectory.mkdirs();

        Intent dashboardIntent = new Intent(getApplicationContext(), DashboardActivity.class);
        dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

        actionBar = (ActionBar) findViewById(R.id.pictureActionbar);
        actionBar.setTitle(R.string.take_picture);
        final ActionBar.Action goHomeAction = new ActionBar.IntentAction(this,
                dashboardIntent, R.drawable.ic_title_home);
        actionBar.addAction(goHomeAction);

        // start camera picture taking intent
        takePicture(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.take_picture_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch( item.getItemId() ) {
            case R.id.clear:
                doClear();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void takePicture(View view) {
        Log.i(TAG, "Taking picture");
        File outputFile = new File(Environment.getExternalStorageDirectory(), Utils.OUTPUT_DIR + "/" + Utils.OUTPUT_FILE);
        imageUri = Uri.fromFile(outputFile);
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, Utils.CAMERA_PIC_REQUEST);
    }

    public void doClear() {
        Log.i(TAG, "Clear image");
        if( imageReady ) {
            imageReady = false;
            imageUri = null;
            croppedImageUri = null;

            findViewById(R.id.captionRow).setVisibility(View.INVISIBLE);
            findViewById(R.id.btnUpload).setEnabled(false);
            ((BitmapDrawable)imageView.getDrawable()).getBitmap().recycle();
            imageView.setImageBitmap(null);
        }
    }

    public void startUpload(View view) {
        if( Utils.isOnline(getApplicationContext()) == false ) {
            Toast.makeText(TakePictureActivity.this,
                "No connection to Internet.\nTry again later.",
                Toast.LENGTH_SHORT);
            Log.i(Utils.TAG, "No internet, didn't start upload.");
            return;
        }

        Log.i(TAG, "Starting async upload");
        if( !Utils.doLogin(getApplicationContext(), httpClient) ) {
            Toast.makeText(TakePictureActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
        } else {
            new UploadPhotoTask().execute();
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
            File imageFile = new File(croppedImageUri.getPath());
            FileBody partFile = new FileBody(imageFile);
            StringBody partTime = new StringBody(timeInSeconds);
            multipartEntity.addPart("photo", partFile );
            multipartEntity.addPart("device_timestamp", partTime);
        } catch ( Exception e ) {
            Log.e(TAG,"Error creating mulitpart form: " + e.toString());
            returnMap.put("result", "Error creating mulitpart form: " + e.toString());
            return returnMap;
        }

        // upload
        try {
            HttpPost httpPost = new HttpPost(Utils.UPLOAD_URL);
            httpPost.setEntity(multipartEntity);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            Log.i(TAG, "Upload status: " + httpResponse.getStatusLine());

            // test result code
            if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                Log.e(TAG, "Login HTTP status fail: " + httpResponse.getStatusLine().getStatusCode());
                returnMap.put("result", "HTTP status error: " + httpResponse.getStatusLine().getStatusCode() );
                return returnMap;
            }

            // test json response
            // should look like
            /*
            {"status": "ok"}
            */
            if( httpEntity != null ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), "UTF-8"));
                String json = reader.readLine();
                JSONTokener jsonTokener = new JSONTokener(json);
                JSONObject jsonObject = new JSONObject(jsonTokener);
                Log.i(TAG,"JSON: " + jsonObject.toString());

                String loginStatus = jsonObject.getString("status");

                if( !loginStatus.equals("ok") ) {
                    Log.e(TAG, "JSON status not ok: " + jsonObject.getString("status"));
                    returnMap.put("result", "JSON status not ok: " + jsonObject.getString("status") );
                    return returnMap;
                }
            }
        } catch( Exception e ) {
            Log.e(TAG, "HttpPost exception: " + e.toString());
            returnMap.put("result", "HttpPost exception: " + e.toString());
            return returnMap;
        }

        // configure / comment
        try {
            HttpPost httpPost = new HttpPost(Utils.CONFIGURE_URL);
            String partComment = txtCaption.getText().toString();
            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
            postParams.add(new BasicNameValuePair("device_timestamp", timeInSeconds));
            postParams.add(new BasicNameValuePair("caption", partComment));
            httpPost.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();

            // test result code
            if( httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                Log.e(TAG, "Upload comment fail: " + httpResponse.getStatusLine().getStatusCode());
                returnMap.put("result", "Upload comment fail: " + httpResponse.getStatusLine().getStatusCode() );
                return returnMap;
            }

            returnMap.put("result", "ok");
            return returnMap;
        } catch( Exception e ) {
            Log.e(TAG, "HttpPost comment error: " + e.toString());
            returnMap.put("result", "HttpPost comment error: " + e.toString());
            return returnMap;
        }
    }

    private void doImageResizeAndRound() {
        Bitmap croppedBitmap;
        Bitmap roundedBitmap;

        StringBuilder imageFileName = new StringBuilder();
        StringBuilder croppedImageFileName = new StringBuilder();
        imageFileName.append(Environment.getExternalStorageDirectory() + "/" + Utils.OUTPUT_DIR + "/" + Utils.OUTPUT_FILE);
        croppedImageFileName.append(Environment.getExternalStorageDirectory() + "/" + Utils.OUTPUT_DIR + "/" + Utils.OUTPUT_FILE_CROPPED);

        // Get the source image's dimensions
        Bitmap srcBitmap = BitmapFactory.decodeFile(imageFileName.toString());

        int srcWidth = srcBitmap.getWidth();
        int srcHeight = srcBitmap.getHeight();
        int desiredWidth = Utils.IMAGE_WIDTH;

        int startX = srcWidth/2 - desiredWidth/2;
        int startY = srcHeight/2 - desiredWidth/2;

        if( desiredWidth <= srcWidth && desiredWidth <= srcHeight ) {
            Log.i(Utils.TAG,"Cropping and resizing image");
            croppedBitmap = Bitmap.createBitmap(srcBitmap, startX, startY, desiredWidth, desiredWidth);
            roundedBitmap = getRoundedCornerBitmap(croppedBitmap);

            // Save
            try {
                File outputFile = new File(Environment.getExternalStorageDirectory(), Utils.OUTPUT_DIR + "/" + Utils.OUTPUT_FILE_CROPPED);
                croppedImageUri = Uri.fromFile(outputFile);

                FileOutputStream out = new FileOutputStream(croppedImageFileName.toString());
                roundedBitmap.compress(Bitmap.CompressFormat.JPEG,
                        Utils.IMAGE_JPEG_COMPRESSION_QUALITY, out);
                roundedBitmap.recycle();
                srcBitmap.recycle();
                Log.i(TAG,"Processed image, now returning");
            } catch( Exception e ) {

            }
        } else {
            Log.i(Utils.TAG, "Small image, leaving alone");
            croppedImageUri = imageUri;
        }

    }

    private Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
            bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(Utils.IMAGE_BORDER, Utils.IMAGE_BORDER, Utils.IMAGE_WIDTH-Utils.IMAGE_BORDER,
                Utils.IMAGE_HEIGHT-Utils.IMAGE_BORDER);
        final RectF rectF = new RectF(rect);
        final float roundPx = Utils.IMAGE_CORNER_RADIUS;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;

    }

    private void showCroppedImage() {
        getContentResolver().notifyChange(croppedImageUri, null);
        ContentResolver contentResolver = getContentResolver();
        Bitmap imageBitmap;
        LinearLayout captionRow = (LinearLayout)findViewById(R.id.captionRow);
        try {
            captionRow.setVisibility(View.VISIBLE);

            Drawable toRecycle =  imageView.getDrawable();
            if( toRecycle != null ) {
                Bitmap bitmapToRecycle = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                if( bitmapToRecycle != null ) {
                    ((BitmapDrawable)imageView.getDrawable()).getBitmap().recycle();
                }
            }
            imageBitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, croppedImageUri);
            imageView.setImageBitmap(imageBitmap);
            Log.i(TAG, "Image: " + croppedImageUri.toString());
            imageReady = true;

            // turn on upload button
            uploadButton.setEnabled(true);

        } catch ( Exception e ) {
            Toast.makeText(TakePictureActivity.this, "Camera error", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Camera error: " + e.toString() );
            doClear();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( resultCode == Activity.RESULT_OK ) {
            switch( requestCode ) {
                case Utils.CAMERA_PIC_REQUEST:
                    Log.i(TAG, "Camera returned");
                    getContentResolver().notifyChange(imageUri, null);
                    doImageResizeAndRound();
                    showCroppedImage();
                    break;
                default:

            }
        }
    }

    private class UploadPhotoTask extends AsyncTask<Void, Void, Map<String, String>> {

        protected void onPreExecute() {
            Toast.makeText(TakePictureActivity.this, "Uploading", Toast.LENGTH_SHORT).show();
        }

        protected Map<String,String> doInBackground(Void... voids) {
            return doUpload();
        }

        protected void onPostExecute(Map<String,String> resultMap) {
            Toast.makeText(TakePictureActivity.this, resultMap.get("result"), Toast.LENGTH_SHORT).show();
        }
    }

    public void onDestroy() {
        doClear();
        super.onDestroy();
    }
}
