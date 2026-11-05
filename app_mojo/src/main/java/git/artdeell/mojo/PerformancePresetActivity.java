package git.artdeell.mojo;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class PerformancePresetActivity extends AppCompatActivity {
    private android.content.SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(getResources().getColor(R.color.background_app));

        TextView title = new TextView(this);
        title.setText("ASTRYX PERFORMANCE");
        title.setTextColor(getResources().getColor(R.color.astryx_accent));
        title.setTextSize(23);
        title.setTypeface(null, 1);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("Choose a launcher profile. These profiles adjust Minecraft RAM allocation, render scale and Android sustained-performance preference. They do not change in-game render distance or install performance mods.");
        description.setTextColor(getResources().getColor(R.color.secondary_text));
        description.setPadding(0, pad / 2, 0, pad);
        root.addView(description);

        addProfile(root, "BATTERY / COOL", "1536 MB RAM • 65% render scale • standard power mode",
                1536, 65, false, true);
        addProfile(root, "BALANCED", "2048 MB RAM • 80% render scale • standard power mode",
                2048, 80, false, true);
        addProfile(root, "PERFORMANCE", "3072 MB RAM • 85% render scale • sustained performance",
                3072, 85, true, false);

        TextView note = new TextView(this);
        note.setText("Changes are saved to Astryx preferences. Restart Astryx before testing a different profile. Actual FPS depends on Minecraft version, renderer, mods, thermal limits and world complexity.");
        note.setTextColor(getResources().getColor(R.color.secondary_text));
        note.setPadding(0, pad, 0, pad);
        root.addView(note);

        Button close = new Button(this);
        close.setText("DONE");
        root.addView(close);
        close.setOnClickListener(v -> finish());
        setContentView(root);
    }

    private void addProfile(LinearLayout root, String name, String details,
                            int ram, int scale, boolean sustained, boolean vsync) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(name + "\n" + details);
        button.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (10 * getResources().getDisplayMetrics().density);
        root.addView(button, lp);
        button.setOnClickListener(v -> {
            prefs.edit()
                    .putInt("allocation", ram)
                    .putInt("resolutionRatio", scale)
                    .putBoolean("sustainedPerformance", sustained)
                    .putBoolean("force_vsync", vsync)
                    .apply();
            Toast.makeText(this, name + " profile saved. Restart Astryx to apply.", Toast.LENGTH_LONG).show();
        });
    }
}
