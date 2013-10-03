package com.aegamesi.steamtrade;

import android.content.Context;
import android.util.AttributeSet;

import com.loopj.android.image.SmartImageView;

public class SquareSmartImageView extends SmartImageView {

	public SquareSmartImageView(Context context) {
		super(context);
	}

	public SquareSmartImageView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
}
