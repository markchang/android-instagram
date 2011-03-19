package org.acmelab.andgram;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class MainActivity extends Activity
{
    private static final int CAMERA_PIC_REQUEST = 1337;
    EditText txtPassword = null;
    EditText txtUsername = null;
    ImageView imageView = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        txtPassword = (EditText)findViewById(R.id.txtUsername);
        txtUsername = (EditText)findViewById(R.id.txtUsername);
        imageView = (ImageView)findViewById(R.id.imageView);
    }

    public void takePicture(View view) {
        Log.i("ANDGRAM", "Picture button pressed");
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    public void doLogin(View view) {
        Log.i("ANDGRAM", "Login button pressed");
        String password = txtPassword.getText().toString();
        String username = txtUsername.getText().toString();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_PIC_REQUEST) {
            Log.i("ANDGRAM", "Camera returned");
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(thumbnail);
        }
    }

}
