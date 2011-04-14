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
 *
 * -----------------------
 * Code here for image listview handling was based heavily upon work done by
 * Fedor Vlasov (http://www.fedorvlasov.com/), originally posted here:
 * http://stackoverflow.com/questions/541966/android-how-do-i-do-a-lazy-load-of-images-in-listview/3068012#3068012
 *
 * No copyright was associated with the LazyList.zip source, so it was used and modified.
 */

package org.acmelab.andgram;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

public class ImageLoader {

    // the simplest in-memory cache implementation.
    // This should be replaced with something like SoftReference or
    // BitmapOptions.inPurgeable(since 1.6)
    private HashMap<String, SoftReference<Bitmap>> cache=new HashMap<String, SoftReference<Bitmap>>();

    final int stub_id = R.drawable.stub;
    private File cacheDir;

    public ImageLoader(Context context){
        // Make the background thread low priority. This way it will not affect the UI performance
        photoLoaderThread.setPriority(Thread.NORM_PRIORITY-1);

        //Find the dir to save cached images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),Utils.OUTPUT_DIR);
        else
            cacheDir=context.getCacheDir();
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }

    public void DisplayImage(String url, Activity activity, ImageView imageView)
    {
        Log.i(Utils.TAG, "Displaying image: " + url);
        if(cache.containsKey(url)) {
            Log.i(Utils.TAG, "Found cached image " + url);
            SoftReference<Bitmap> softRef = cache.get(url);
            Bitmap bitmap = softRef.get();
            if( bitmap == null ) {
                Log.i(Utils.TAG, "But re-queuing GC'ed image: " + url);
                // maybe? : cache.remove(softRef);
                queuePhoto(url, activity, imageView);
                imageView.setImageResource(stub_id);
            } else {
                Log.i(Utils.TAG, "Setting image bitmap");
                imageView.setImageBitmap(softRef.get());
                if( softRef.get() == null ) {
                    Log.e(Utils.TAG, "Null bitmap: " + url);
                }
            }
        }
        else
        {
            Log.i(Utils.TAG, "Not in cache, queueing "  + url);
            queuePhoto(url, activity, imageView);
            imageView.setImageResource(stub_id);
        }
    }

    private void queuePhoto(String url, Activity activity, ImageView imageView)
    {
        Log.i(Utils.TAG, "Queueing: " + url);

        // This ImageView may be used for other images before.
        // So there may be some old tasks in the queue. We need to discard them.
        photosQueue.Clean(imageView);
        PhotoToLoad p=new PhotoToLoad(url, imageView);
        synchronized( photosQueue.photosToLoad ){
            photosQueue.photosToLoad.push(p);
            photosQueue.photosToLoad.notifyAll();
        }

        //start thread if it's not started yet
        if(photoLoaderThread.getState()==Thread.State.NEW)
            photoLoaderThread.start();
    }

    private Bitmap getBitmap(String url)
    {
        // I identify images by hashcode. Not a perfect solution, good for the demo.
        Log.i(Utils.TAG, "Getting image in thread: " + url);
        String filename=String.valueOf(url.hashCode());
        File f=new File(cacheDir, filename);

        //from SD cache
        Bitmap b = decodeFile(f);
        if(b != null) {
            Log.i(Utils.TAG, "Found in cache." );
            return b;
        }


        //from web
        try {
            Log.i(Utils.TAG, "Downloading from web");
            Bitmap bitmap = null;
            InputStream is = new URL(url).openStream();
            OutputStream os = new FileOutputStream(f);
            Utils.CopyStream(is, os);
            os.close();
            is.close();
            bitmap = decodeFile(f);
            return bitmap;
        } catch (Exception ex){
           ex.printStackTrace();
           return null;
        }
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {
            Log.i(Utils.TAG, "Decoding image: " + f.toString());
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            Log.i(Utils.TAG, "decoded a real bitmap!");
            return(b);
        } catch (FileNotFoundException e) {
            Log.i(Utils.TAG, "File not found");
            return null;
        } catch (Exception ex) {
            Log.e(Utils.TAG, "Some other shit failed in decodeFile");
            ex.printStackTrace();
            return null;
        }
    }

    // Task for the queue
    private class PhotoToLoad
    {
        public String url;
        public ImageView imageView;
        public PhotoToLoad(String u, ImageView i){
            url = u;
            imageView = i;
        }
    }

    PhotosQueue photosQueue = new PhotosQueue();

    public void stopThread()
    {
        photoLoaderThread.interrupt();
    }

    // stores list of photos to download
    class PhotosQueue
    {
        private Stack<PhotoToLoad> photosToLoad=new Stack<PhotoToLoad>();

        //removes all instances of this ImageView
        public void Clean(ImageView image)
        {
            for(int j=0 ;j<photosToLoad.size();){
                if(photosToLoad.get(j).imageView == image)
                    photosToLoad.remove(j);
                else
                    ++j;
            }
        }
    }

    class PhotosLoader extends Thread {
        public void run() {
            try {
                while(true)
                {
                    //thread waits until there are any images to load in the queue
                    if(photosQueue.photosToLoad.size() == 0) {
                        synchronized(photosQueue.photosToLoad){
                            photosQueue.photosToLoad.wait();
                        }
                    }
                    if(photosQueue.photosToLoad.size() != 0)
                    {
                        PhotoToLoad photoToLoad;
                        synchronized(photosQueue.photosToLoad){
                            photoToLoad=photosQueue.photosToLoad.pop();
                        }
                        Bitmap bmp = getBitmap(photoToLoad.url);
                        if(bmp == null) {
                            Log.e(Utils.TAG, "Bitmap loaded as null!");
                        } else {
                            cache.put(photoToLoad.url, new SoftReference<Bitmap>(bmp));
                            Object tag = photoToLoad.imageView.getTag();
                            if(tag != null && ((String)tag).equals(photoToLoad.url)){
                                BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad.imageView);
                                Activity a = (Activity)photoToLoad.imageView.getContext();
                                a.runOnUiThread(bd);
                            } else {
                                Log.i(Utils.TAG, "Got image w/o tag!");
                            }
                        }
                    }
                    if(Thread.interrupted())
                        break;
                }
            } catch (InterruptedException e) {
                //allow thread to exit
            }
        }
    }

    PhotosLoader photoLoaderThread = new PhotosLoader();

    // Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        ImageView imageView;

        public BitmapDisplayer(Bitmap b, ImageView i) {
            bitmap = b;
            imageView = i;
        }

        public void run()
        {
            if(bitmap != null)
                imageView.setImageBitmap(bitmap);
            else
                imageView.setImageResource(stub_id);
        }
    }

    public void clearCache() {
        // clear memory cache
        cache.clear();

        //clear SD cache
        File[] files=cacheDir.listFiles();
        for(File f:files)
            f.delete();
    }

}
