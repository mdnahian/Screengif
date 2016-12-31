package com.mdislam.screengiff.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.mdislam.screengiff.R;

/**
 * Created by mdislam on 12/30/16.
 */

public class GifCropView extends View {

    private String mDrawable;

    public GifCropView (Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        TypedArray a = context.obtainStyledAttributes(attributeSet, R.styleable.GifCropView, 0, 0);

        try {
            mDrawable = a.getString(R.styleable.GifCropView_src);
        } finally {
            a.recycle();
        }




    }




    public String getDrawable(){
        return mDrawable;
    }



    public void setDrawable(String mDrawable){
        this.mDrawable = mDrawable;
        invalidate();
        requestLayout();
    }





}
