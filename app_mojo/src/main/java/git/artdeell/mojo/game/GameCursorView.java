package git.artdeell.mojo.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import git.artdeell.mojo.R;
import git.artdeell.mojo.game.platform.Platform;
import git.artdeell.mojo.game.platform.cursor.PlatformCursor;
import git.artdeell.mojo.game.platform.cursor.PlatformCursorImplementor;

import java.io.InputStream;

/** Draws the pointer overlay and optional Astryx custom cursor image. */
public class GameCursorView extends View implements PlatformCursorImplementor {
    private final Paint customCursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Drawable cursorDrawable;
    private boolean noDraw = false;
    private float mouseScale = 1f;
    private Bitmap importedCursor;
    private float importedHotX;
    private float importedHotY;

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GameCursorView(Context context) { this(context, null); }
    public GameCursorView(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        cursorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mouse_pointer);
        if (cursorDrawable != null) cursorDrawable.setBounds(0, 0, 36, 54);

        android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mouseScale = Math.max(0.25f, Math.min(2f, prefs.getInt("astryx_cursor_scale", 100) / 100f));
        importedHotX = Math.max(0f, Math.min(1f, prefs.getInt("astryx_cursor_hot_x", 0) / 100f));
        importedHotY = Math.max(0f, Math.min(1f, prefs.getInt("astryx_cursor_hot_y", 0) / 100f));

        String uriValue = prefs.getString("astryx_cursor_uri", null);
        if (uriValue != null) {
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream stream = context.getContentResolver().openInputStream(Uri.parse(uriValue))) {
                    if (stream != null) BitmapFactory.decodeStream(stream, null, bounds);
                }
                int sample = 1;
                while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sample;
                try (InputStream stream = context.getContentResolver().openInputStream(Uri.parse(uriValue))) {
                    if (stream != null) importedCursor = BitmapFactory.decodeStream(stream, null, options);
                }
            } catch (Exception ignored) {
                importedCursor = null;
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (noDraw) return;
        int dx = (int) (Platform.cursorX * ((GameView) getParent()).cursorRatioX);
        int dy = (int) (Platform.cursorY * ((GameView) getParent()).cursorRatioY);
        canvas.translate(dx, dy);
        PlatformCursor cursor = Platform.getCursor();
        canvas.scale(mouseScale, mouseScale);
        if (importedCursor != null) {
            canvas.drawBitmap(importedCursor,
                    -importedCursor.getWidth() * importedHotX,
                    -importedCursor.getHeight() * importedHotY,
                    customCursorPaint);
        } else if (cursor != null) {
            canvas.drawBitmap(cursor.bitmap, -cursor.hotX, -cursor.hotY, customCursorPaint);
        } else if (cursorDrawable != null) {
            cursorDrawable.draw(canvas);
        }
    }

    @Override public void onCursorPosition() {
        if (!noDraw) post(this::invalidate);
    }

    @Override public void onCursorChanged() { post(this::invalidate); }

    @Override public void onGrabState(boolean isGrabbing) {
        noDraw = isGrabbing;
        invalidate();
    }

    public void setCursorScale(float scale) {
        mouseScale = Math.max(0.25f, Math.min(2f, scale));
        invalidate();
    }
}
