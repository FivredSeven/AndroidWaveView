package com.suo.waveform.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class PressedButton extends Button {
    public PressedButton(Context context) {
        super(context);
    }

    public PressedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PressedButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        setAlpha((isPressed() || isFocused() || isSelected()) ? 0.3f : 1.0f);
    }
}
