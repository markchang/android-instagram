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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.markupartist.android.widget.ActionBar;

public class DashboardActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_home);

        Intent pictureIntent = new Intent(DashboardActivity.this, TakePictureActivity.class);
        pictureIntent.putExtra("action", Utils.UPLOAD_FROM_CAMERA);


        final ActionBar actionBar = (ActionBar) findViewById(R.id.dashboardActionbar);
        final ActionBar.Action takePictureAction = new ActionBar.IntentAction(this,
                pictureIntent, R.drawable.ic_title_camera);
        actionBar.addAction(takePictureAction);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Instagram has asked that I remove this application from the Android Market " +
                            "as I am using unpublished APIs. The app " +
                            "will be going away shortly, and as of now, it no longer functions.\n\n" +
                            "Thanks for your support.")
               .setCancelable(true)
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.cancel();
                   }
               });
        AlertDialog alert = builder.create();
        alert.show();


    }

    public void openPictureIntent(View view) {
        Intent pictureIntent = new Intent(DashboardActivity.this, TakePictureActivity.class);
        pictureIntent.putExtra("action", Utils.UPLOAD_FROM_CAMERA);
        startActivity(pictureIntent);
    }

    public void openGalleryIntent(View view) {
        Intent galleryIntent = new Intent(DashboardActivity.this, TakePictureActivity.class);
        galleryIntent.putExtra("action", Utils.UPLOAD_FROM_GALLERY);
        startActivity(galleryIntent);
    }

    public void openFeedIntent(View view) {
        Intent feedIntent = new Intent(DashboardActivity.this, ImageListActivity.class);
        feedIntent.putExtra("url", Utils.TIMELINE_URL);
        feedIntent.putExtra("title", R.string.feed);
        startActivity(feedIntent);
    }

    public void openPopularIntent(View view) {
        Intent feedIntent = new Intent(DashboardActivity.this, ImageListActivity.class);
        feedIntent.putExtra("url", Utils.POPULAR_URL);
        feedIntent.putExtra("title", R.string.popular);
        startActivity(feedIntent);
    }

    public void openUserfeedIntent(View view) {
        Intent feedIntent = new Intent(DashboardActivity.this, ImageListActivity.class);
        String pk = Utils.getUserPk(getApplicationContext());
        feedIntent.putExtra("url", Utils.createUserTimelineUrl(pk));
        feedIntent.putExtra("title", R.string.userfeed);
        startActivity(feedIntent);
    }

    public void openLoginIntent(View view) {
        Utils.launchCredentials(getApplicationContext());
    }

}