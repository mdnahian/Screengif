package com.mdislam.screengiff;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;

import pl.droidsonroids.gif.GifImageView;

/**
 * Created by mdislam on 12/30/16.
 */

public class GifViewerActivity extends Activity {


    private boolean ishowing;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gifviewer);

        String filePath = getIntent().getStringExtra("filePath");

        GifImageView gif = (GifImageView) findViewById(R.id.gif);
        gif.setImageURI(Uri.fromFile(new File(filePath)));

        final ImageView backBtn = (ImageView) findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });




        RelativeLayout backBg = (RelativeLayout) findViewById(R.id.backBg);



        new CountDownTimer(3000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                backBtn.setVisibility(View.INVISIBLE);
                ishowing = false;
            }
        }.start();





        backBg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){

                    case MotionEvent.ACTION_DOWN:
                        if(!ishowing) {
                            backBtn.setVisibility(View.VISIBLE);

                            new CountDownTimer(3000, 1000) {

                                public void onTick(long millisUntilFinished) {
                                }

                                public void onFinish() {
                                    backBtn.setVisibility(View.INVISIBLE);
                                    ishowing = false;
                                }
                            }.start();

                            ishowing = true;
                            break;
                        }

                }

                return true;
            }
        });




    }


}
