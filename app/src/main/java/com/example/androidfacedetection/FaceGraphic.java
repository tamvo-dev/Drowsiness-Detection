package com.example.androidfacedetection;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.gms.vision.face.Face;

import java.io.IOException;


import static com.example.androidfacedetection.VideoFaceDetectionActivity.context;
import static com.example.androidfacedetection.VideoFaceDetectionActivity.mediaPlayerOver;

class FaceGraphic extends GraphicOverlay.Graphic {

    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private static Boolean playing = false;

    private static final int mTimeSleep = 2000;
    private static int mCountSleep = 0;
    private float mSleepProbability = 0.3f;
    private static boolean isSleep = false;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        VideoFaceDetectionActivity.mediaPlayer = new MediaPlayer();
        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }
        Paint p = new Paint();
        //VideoFaceDetectionActivity.mediaPlayer = new MediaPlayer();
        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);

        canvas.drawText("Left Eye" + face.getIsLeftEyeOpenProbability(), 0, canvas.getHeight()/4, mIdPaint);
        canvas.drawText("Right Eye" + face.getIsRightEyeOpenProbability(), 0, canvas.getHeight(), mIdPaint);

        if(face.getIsRightEyeOpenProbability()<mSleepProbability && face.getIsLeftEyeOpenProbability()<mSleepProbability){
            p.setColor(Color.RED);
            p.setTextSize(280);
            canvas.drawText("SLEEPY", 0,canvas.getHeight()/2, p);

            try{
                if(playing == false){
                    mCountSleep++;
                    playing = true;
                    Log.e("TAG", mCountSleep + "");
                    if (mCountSleep > 3){
                        showDiaLog();
                        mCountSleep = 0;
                    } else {
                        playAudio();
                    }

                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }

        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);
    }

    private void showDiaLog() throws IOException {

        VideoFaceDetectionActivity.mediaPlayerOver.start();

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_messenger, null, false);
        builder.setView(view);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                mediaPlayerOver.pause();
                playing = false;
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private void checkSleep(){
        if (isSleep == false)
            return;
        try {
            playAudio();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playAudio() throws IOException {

        AssetFileDescriptor as = context.getAssets().openFd("alarm.wav");
        VideoFaceDetectionActivity.mediaPlayer.setDataSource(as.getFileDescriptor(), as.getStartOffset(), as.getLength());
        as.close();
        VideoFaceDetectionActivity.mediaPlayer.prepare();
        VideoFaceDetectionActivity.mediaPlayer.start();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                playing = false;
                if (VideoFaceDetectionActivity.mediaPlayer != null){
                    VideoFaceDetectionActivity.mediaPlayer.reset();
                }
            }
        }, 2500);
    }

}
