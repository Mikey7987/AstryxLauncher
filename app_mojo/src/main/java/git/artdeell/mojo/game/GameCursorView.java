package git.artdeell.mojo.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.preference.PreferenceManager;
import java.io.InputStream;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import git.artdeell.mojo.game.platform.Platform;
import git.artdeell.mojo.game.platform.cursor.PlatformCursor;
import git.artdeell.mojo.game.platform.cursor.PlatformCursorImplementor;

import git.artdeell.mojo.R;

/**
 * A view that draws the platform cursor on the screen
 */
public class GameCursorView extends View implements PlatformCursorImplementor {
    private final Paint customCursorPaint = new Paint();
    private final Drawable cursorDrawable;
    private boolean noDraw = false;
    private float mouseScale = 1f;
    private Bitmap importedCursor;

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GameCursorView(Context context) {
        this(context, null);
    }

    public GameCursorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameCursorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        cursorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mouse_pointer);
        assert cursorDrawable != null;
        cursorDrawable.setBounds(0, 0, 36, 54);
        android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mouseScale = Math.max(0.25f, Math.min(2f, prefs.getInt("astryx_cursor_scale", 100) / 100f));
        String uriValue = prefs.getString("astryx_cursor_uri", null);
        if (uriValue != null) {
            try (InputStream stream = context.getContentResolver().openInputStream(Uri.parse(uriValue))) {
                if (stream != null) importedCursor = BitmapFactory.decodeStream(stream);
            } catch (Exception ignored) { importedCursor = null; }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (noDraw) return;
        // Scale coordinates back to the full unresized screen size
        int dx = (int) (Platform.cursorX * ((GameView)getParent()).cursorRatioX);
        int dy = (int) (Platform.cursorY * ((GameView)getParent()).cursorRatioY);
        canvas.translate(dx, dy);
        PlatformCursor cursor = Platform.getCursor();
        canvas.scale(mouseScale, mouseScale);
        if (cursor == null) {
            cursorDrawable.draw(canvas);
        } else {
            canvas.drawBitmap(cursor.bitmap, -cursor.hotX, -cursor.hotY, customCursorPaint);
        }
    }

    @Override
    public void onCursorPosition() {
        if (!noDraw) post(this::invalidate);
    }

    @Override
    public void onCursorChanged() {
        post(this::invalidate);
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        noDraw = isGrabbing;
        invalidate();
    }

    public void setCursorScale(float scale) {
        this.mouseScale = scale;
    }
}
