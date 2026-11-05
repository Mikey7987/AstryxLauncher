package git.artdeell.mojo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class CursorStudioActivity extends AppCompatActivity {
    private static final int PICK_CURSOR = 4102;
    private static final String PREF_CURSOR_URI = "astryx_cursor_uri";
    private static final String PREF_CURSOR_SCALE = "astryx_cursor_scale";
    private static final String PREF_HOT_X = "astryx_cursor_hot_x";
    private static final String PREF_HOT_Y = "astryx_cursor_hot_y";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Astryx Cursor Studio");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, pad);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText(R.string.astryx_cursor_note);
        note.setTextSize(14);
        root.addView(note, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView scaleLabel = new TextView(this);
        scaleLabel.setText(R.string.astryx_cursor_scale);
        scaleLabel.setPadding(0, pad, 0, 0);
        root.addView(scaleLabel);

        SeekBar scale = new SeekBar(this);
        scale.setMax(200);
        int initial = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(PREF_CURSOR_SCALE, 100);
        scale.setProgress(Math.max(25, Math.min(200, initial)));
        root.addView(scale);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (fromUser) PreferenceManager.getDefaultSharedPreferences(CursorStudioActivity.this)
                        .edit().putInt(PREF_CURSOR_SCALE, Math.max(25, value)).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });


        TextView hotspotTitle = new TextView(this);
        hotspotTitle.setText("Cursor hotspot position");
        hotspotTitle.setPadding(0, pad, 0, 0);
        root.addView(hotspotTitle);

        android.content.SharedPreferences cursorPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        addHotspotSlider(root, "Hotspot X", PREF_HOT_X, cursorPrefs.getInt(PREF_HOT_X, 0));
        addHotspotSlider(root, "Hotspot Y", PREF_HOT_Y, cursorPrefs.getInt(PREF_HOT_Y, 0));

        Button importButton = new Button(this);
        importButton.setText(R.string.astryx_cursor_import);
        root.addView(importButton);
        importButton.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("image/png");
            pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(pick, PICK_CURSOR);
        });

        Button resetButton = new Button(this);
        resetButton.setText(R.string.astryx_cursor_reset);
        root.addView(resetButton);
        resetButton.setOnClickListener(v -> {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .remove(PREF_CURSOR_URI).putInt(PREF_CURSOR_SCALE, 100)
                    .putInt(PREF_HOT_X, 0).putInt(PREF_HOT_Y, 0).apply();
            scale.setProgress(100);
            Toast.makeText(this, "Cursor reset. It will apply next time Minecraft starts.", Toast.LENGTH_LONG).show();
        });

        Button close = new Button(this);
        close.setText(android.R.string.ok);
        root.addView(close);
        close.setOnClickListener(v -> finish());
        setContentView(root);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_CURSOR || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString(PREF_CURSOR_URI, uri.toString()).apply();
            Toast.makeText(this, "Cursor imported. It will apply next time Minecraft starts.", Toast.LENGTH_LONG).show();
        } catch (SecurityException error) {
            Toast.makeText(this, "Could not keep access to that image.", Toast.LENGTH_LONG).show();
        }
    }
}
