package com.example.android.bluetoothchat.ChatKit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.Random;

public class DialogAvatar extends View {
    Paint fontPaint;
    String symbol;
    Random rnd;

    public DialogAvatar(Context context, char symbol) {
        super(context);
        if(symbol > 96 && symbol < 123)
            symbol -= 32;
        this.symbol = "" + symbol;
        rnd = new Random();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        double height = canvas.getHeight() * 0.8;
        fontPaint = new Paint();
        fontPaint.setTextSize((int) height);
        fontPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        fontPaint.setStrokeWidth(15);
        fontPaint.setColor(Color.WHITE);

        canvas.translate(125, 390);
        canvas.drawColor(Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
        canvas.drawText(symbol, 0, 0, fontPaint);

    }
}

