package com.suo.waveform.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class PressedImageView extends ImageView {
    public PressedImageView(Context context) {
        super(context);
    }

    public PressedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PressedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        setAlpha((isPressed() || isFocused() || isSelected()) ? 0.3f : 1.0f);
    }
}
