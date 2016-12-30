package com.mdislam.screengiff;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.icu.text.SimpleDateFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;

import pl.droidsonroids.gif.GifImageView;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MainActivity extends AppCompatActivity {

    private boolean isRecording = false;
    private FloatingActionButton fab;

    private ProgressBar spinner;

    private LinearLayout empty;


    private GridView gifsview;
    private GifsAdapter gifsAdapter;


    private int count;


    private File file;


    private ArrayList<File> gifs;


    private boolean fabstate = true;

    private boolean isSelecting;


    private boolean initalLoad = true;



    private static final String TAG = "MainActivity";
    private static final int PERMISSION_CODE = 1;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 480;
    private static final int DISPLAY_HEIGHT = 640;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        spinner = (ProgressBar)findViewById(R.id.progressBar1);


        empty = (LinearLayout) findViewById(R.id.empty);


        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onFailure() {}

                @Override
                public void onSuccess() {}

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            Log.d("Crash", e.getMessage());
        }


        gifs = new ArrayList<>();



        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mMediaRecorder = new MediaRecorder();


        mProjectionManager = (MediaProjectionManager) getSystemService (Context.MEDIA_PROJECTION_SERVICE);


        gifsview = (GridView) findViewById(R.id.gifs);


        gifsview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);


        gifsview.setMultiChoiceModeListener(new GridView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
                final int checkedCount = gifsview.getCheckedItemCount();
                actionMode.setTitle(checkedCount + " Selected");
                gifsAdapter.toggleSelection(i);
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getMenuInflater().inflate(R.menu.menu_main, menu);

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                getSupportActionBar().hide();
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.delete:
                        SparseBooleanArray selected = gifsAdapter.getSelectedIds();

                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                File selecteditem = gifsAdapter.getItem(selected.keyAt(i));
                                // Remove selected items following the ids
                                gifsAdapter.remove(selecteditem);
                            }
                        }

                        actionMode.finish();
                        return true;
                    default:
                        return false;

                }

            }


            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                gifsAdapter.removeSelection();
                getSupportActionBar().show();

                for (int i=0; i< gifsAdapter.getCount(); i++){
                    View view = gifsview.getChildAt(i - gifsview.getFirstVisiblePosition());

                    if(view == null){
                        return;
                    }


                    GifImageView gif = (GifImageView) view.findViewById(R.id.gif);
                    ImageView check = (ImageView) view.findViewById(R.id.check);

                    check.setVisibility(View.GONE);
                    gif.setForeground(new ColorDrawable(ContextCompat.getColor(MainActivity.this, R.color.colorNone)));

                }

                if(gifsAdapter.getCount() == 0){
                    empty.setVisibility(View.VISIBLE);
                }


                isSelecting = false;

            }
        });



        gifsAdapter = new GifsAdapter();




        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(checkPermissions()){
                    if (isRecording) {
                        stopRecording();
                    } else {
                        Log.d("Crash", "Started...");
                        startRecording();
                    }

                    onToggleScreenShare();
                }


            }
        });




        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(fabstate){
                    CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(64, 64, 64, 64);
                    params.gravity = Gravity.BOTTOM|Gravity.END;
                    fab.setLayoutParams(params);
                    fabstate = false;
                } else {
                    CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(64, 64, 64, 64);
                    params.gravity = Gravity.BOTTOM|Gravity.CENTER;
                    fab.setLayoutParams(params);
                    fabstate = true;
                }

                return true;
            }
        });



        mMediaProjectionCallback = new MediaProjectionCallback();

        checkPermissions();

        spinner.setVisibility(View.GONE);




    }



    private void getAllGifs(){

        gifs.clear();

        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        Log.d("Crash", dir.getPath());

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                if (child.endsWith(".gif"))
                    gifs.add(new File(dir, child));
            }
        }


        if (gifs.size() > 0) {
            Collections.sort(gifs, new Comparator<File>() {
                @Override
                public int compare(File object1, File object2) {
                    return (int) ((object1.lastModified() > object2.lastModified()) ? object1.lastModified() : object2.lastModified());
                }
            });

            Collections.reverse(gifs);

            empty.setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.VISIBLE);
        }


        deleteAllMP4Files();

        gifsview.setAdapter(gifsAdapter);

    }





    private class GifsAdapter extends ArrayAdapter<File>{

        private SparseBooleanArray mSelectedItemsIds;


        public GifsAdapter() {
            super(MainActivity.this, R.layout.gif, gifs);

            mSelectedItemsIds = new SparseBooleanArray();
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            final File file = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.gif, parent, false);
            }


            final GifImageView gif = (GifImageView) convertView.findViewById(R.id.gif);
            gif.setImageURI(Uri.fromFile(file));

            final ImageView check = (ImageView) convertView.findViewById(R.id.check);


            ImageView share = (ImageView) convertView.findViewById(R.id.share);

            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    spinner.setVisibility(View.VISIBLE);

                    final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/gif");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    startActivity(Intent.createChooser(shareIntent, "Share image using"));

                    spinner.setVisibility(View.GONE);
                }
            });



            gif.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isSelecting){
                        if(gifsview.isItemChecked(position)){
                            gifsview.setItemChecked(position, false);

                            check.setVisibility(View.GONE);
                            gif.setForeground(new ColorDrawable(ContextCompat.getColor(MainActivity.this, R.color.colorNone)));

                        } else {
                            gifsview.setItemChecked(position, true);

                            check.setVisibility(View.VISIBLE);
                            gif.setForeground(new ColorDrawable(ContextCompat.getColor(MainActivity.this, R.color.colorDark)));

                        }
                    } else {

                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setDataAndType(FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName()+".provider", file), "image/*");
                        } else {
                            intent.setDataAndType(Uri.fromFile(file), "image/*");
                        }
                        startActivity(intent);

                    }
                }
            });




            gif.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(!isSelecting) {
                        gifsview.setItemChecked(position, true);

                        check.setVisibility(View.VISIBLE);
                        gif.setForeground(new ColorDrawable(ContextCompat.getColor(MainActivity.this, R.color.colorDark)));

                        isSelecting = true;
                    }

                    return true;
                }
            });


            return convertView;

        }


        @Override
        public void remove(File object) {
            object.delete();
            gifs.remove(object);
            notifyDataSetChanged();
        }


        public void removeSelection() {
            mSelectedItemsIds = new SparseBooleanArray();
            notifyDataSetChanged();
        }


        public void toggleSelection(int position) {
            selectView(position, !mSelectedItemsIds.get(position));
        }

        public void selectView(int position, boolean value) {
            if (value)
                mSelectedItemsIds.put(position, value);
            else
                mSelectedItemsIds.delete(position);
            notifyDataSetChanged();
        }

        public int getSelectedCount() {
            return mSelectedItemsIds.size();
        }

        public SparseBooleanArray getSelectedIds() {
            return mSelectedItemsIds;
        }


    }









    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 101: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if(initalLoad){
                        deleteAllMP4Files();
                        getAllGifs();

                        initRecorder();
                        prepareRecorder();

                        initalLoad = false;
                    }


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }






















    private boolean checkPermissions(){


        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO) + ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_PHONE_STATE)) {

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE}, 101);

                return false;
            } else {

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE}, 101);


                return false;

            }


        } else {

            if(initalLoad){
                deleteAllMP4Files();
                getAllGifs();

                initRecorder();
                prepareRecorder();

                initalLoad = false;
            }

            return true;
        }

    }


    private String generateRandom(){
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }


    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = generateRandom();

        try {
            timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        } catch (Error e){
            Log.d("Crash", "simple data format");
        }

        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".gif");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }







    public void onToggleScreenShare() {
        if (isRecording) {
            Log.d("Crash", "Sharing...");
            shareScreen();
        } else {
            stopScreen();
        }
    }



    public void stopScreen(){

        try {

            if (mMediaProjection != null) {
                mMediaProjection.stop();
                mMediaProjection = null;
            }
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.v(TAG, "Recording Stopped");
            stopScreenSharing();

            initRecorder();
            prepareRecorder();


            convertToGif();
        } catch (Exception e){

        }
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        if(isRecording) {
            stopScreen();
        }
    }





    private void convertToGif() {

        final File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));


        if (dir.isDirectory()) {
            final String[] children = dir.list();

            count = 0;

            for (final String child : children) {
                if (child.endsWith(".mp4")){

                    String[] cmd = {"-i", new File(dir, child).getPath(), getOutputMediaFile(MEDIA_TYPE_IMAGE).getPath()};

                    FFmpeg ffmpeg = FFmpeg.getInstance(MainActivity.this);
                    try {
                        // to execute "ffmpeg -version" command you just need to pass "-version"
                        ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                            @Override
                            public void onStart() {
                                spinner.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onProgress(String message) {
                                Log.d("Crash", message);
                            }

                            @Override
                            public void onFailure(String message) {
                                Log.d("Crash", message);

                                count++;

                                if(count == children.length){
                                    failedToConvertGif();
                                }

                            }

                            @Override
                            public void onSuccess(String message) {
                                Log.d("Crash", message);


                                getAllGifs();
                                spinner.setVisibility(View.GONE);

                            }

                            @Override
                            public void onFinish() {
                                spinner.setVisibility(View.GONE);
                            }
                        });
                    } catch (FFmpegCommandAlreadyRunningException e) {
                        Log.d("Crash", e.getMessage());
                        failedToConvertGif();
                    }


                }

            }

        }




    }




    private void failedToConvertGif(){
        spinner.setVisibility(View.GONE);

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Could not Record Gif");
        alertDialog.setIcon(R.drawable.icon);
        alertDialog.setMessage("Screengif may be too long. Please try again.");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();

    }





    private void deleteAllMP4Files (){

        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                if (child.endsWith(".mp4"))
                    new File(dir, child).delete();
            }
        }

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }

        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }


        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();

        Log.d("Crash", "Recording...");

    }






    private void shareScreen() {

        if (mMediaProjection == null) {
            Log.d("Crash", "Projecting...");
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
            return;
        }

        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        //mMediaRecorder.release();
    }






    private VirtualDisplay createVirtualDisplay() {

        Log.d("Crash", "Creating...");

        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }





    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (isRecording) {
                isRecording = false;
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
                initRecorder();
                prepareRecorder();
            }
            mMediaProjection = null;
            stopScreenSharing();
            Log.i(TAG, "MediaProjection Stopped");
        }
    }







    private void prepareRecorder() {
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }






    private void initRecorder() {

        try {

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(3000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);

            String filePath = getOutputMediaFile(MEDIA_TYPE_VIDEO).getPath();
            mMediaRecorder.setOutputFile(filePath);

            file = new File(filePath);

            if (!file.exists() || file.length() != 0) {
                file.delete();
            }

        } catch (Exception e){}

    }












    private void startRecording() {
        fab.setImageDrawable(getResources().getDrawable(R.drawable.stop));
        isRecording = true;
    }

    private void stopRecording() {
        fab.setImageDrawable(getResources().getDrawable(R.drawable.record));
        isRecording = false;
    }



}
