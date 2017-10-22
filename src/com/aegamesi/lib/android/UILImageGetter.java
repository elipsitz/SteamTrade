package com.aegamesi.lib.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;


public class UILImageGetter implements Html.ImageGetter {
	Context context;
	TextView textView;
	float pixelsToDp;

	public UILImageGetter(View t, Context context) {
		this.context = context;
		this.textView = (TextView) t;

		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		pixelsToDp = ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
	}

	@Override
	public Drawable getDrawable(String source) {
		int size = (int) (18.0f * pixelsToDp);

		ContainerDrawable container = new ContainerDrawable(Color.WHITE, size, size);

		Log.d("UILImageGetter", "Loading...");
		ImageLoader.getInstance().loadImage(source, new SimpleListener(container));
		return container;
	}

	private class SimpleListener extends SimpleImageLoadingListener {
		ContainerDrawable containerDrawable;

		public SimpleListener(ContainerDrawable downloader) {
			super();
			containerDrawable = downloader;
		}

		@Override
		public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
			Log.d("UILImageGetter", "Done loading " + imageUri);

			containerDrawable.setBitmap(loadedImage);

			textView.getParent().requestLayout();
			textView.invalidate();
			textView.invalidateDrawable(containerDrawable);
			textView.setText(null);
			textView.setText(textView.getText());
		}
	}

	private class ContainerDrawable extends ColorDrawable {
		private Drawable innerDrawable = null;

		public ContainerDrawable(int w, int h, int color) {
			super(color);

			setBounds(0, 0, w, h);
		}

		private void setBitmap(Bitmap bitmap) {
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();

			int newWidth = (int) (width * pixelsToDp);
			int newHeight = (int) (height * pixelsToDp);

			if (width > textView.getWidth()) {
				newWidth = textView.getWidth();
				newHeight = (newWidth * height) / width;
			}

			innerDrawable = new BitmapDrawable(context.getResources(), bitmap);
			innerDrawable.setFilterBitmap(false);
			innerDrawable.setBounds(0, 0, newWidth, newHeight);
			setBounds(0, 0, newWidth, newHeight);
		}

		@Override
		public void draw(Canvas canvas) {
			if (innerDrawable != null) {
				innerDrawable.draw(canvas);
			} else {
				super.draw(canvas);
			}
		}
	}
}