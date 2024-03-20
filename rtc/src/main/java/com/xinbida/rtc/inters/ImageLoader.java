package com.xinbida.rtc.inters;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.Nullable;

/**
 * 5/7/21 3:30 PM
 * 图片加载
 */
public interface ImageLoader {
    public void onImageLoader(@Nullable Context context, String uid, ImageView imageView);
}
