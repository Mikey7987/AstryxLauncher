package git.artdeell.mojo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class StorageMigrationActivity extends AppCompatActivity {
    private static final int PICK_BACKUP = 5101;
    private static final int CREATE_BACKUP = 5102;
    private static final long MAX_UNPACKED_BYTES = 4L * 1024L * 1024L * 1024L;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(getResources().getColor(R.color.background_app));

        TextView title = new TextView(this);
        title.setText("ASTRYX DATA TRANSFER");
        title.setTextColor(getResources().getColor(R.color.astryx_accent));
        title.setTextSize(23);
        title.setTypeface(null, 1);
        root.addView(title);

        TextView info = new TextView(this);
        info.setPadding(0, pad, 0, pad);
        info.setText("Export creates a ZIP of Astryx's app-specific game-data folder. Import extracts a compatible ZIP into Astryx storage. Android does not allow Astryx to read another launcher's private app folder directly. Keep your original launcher and backup until you verify your worlds, instances, accounts and mods.");
        info.setTextColor(getResources().getColor(R.color.secondary_text));
        root.addView(info, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button export = new Button(this);
        export.setText("EXPORT ASTRYX BACKUP");
        root.addView(export);
        export.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, "astryx-backup.zip");
            startActivityForResult(intent, CREATE_BACKUP);
        });

        Button pick = new Button(this);
        pick.setText("IMPORT BACKUP ZIP");
        root.addView(pick);
        pick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, PICK_BACKUP);
        });

        Button done = new Button(this);
        done.setText("DONE");
        root.addView(done);
        done.setOnClickListener(v -> finish());
        setContentView(root);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        new Thread(() -> {
            String result;
            try {
                File root = getExternalFilesDir(null);
                if (root == null) throw new IllegalStateException("Astryx storage is unavailable");
                if (requestCode == PICK_BACKUP) {
                    result = extractBackup(uri, root);
                } else if (requestCode == CREATE_BACKUP) {
                    result = createBackup(uri, root);
                } else {
                    return;
                }
            } catch (Exception e) {
                result = "Transfer failed: " + e.getMessage();
            }
            final String message = result;
            runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
        }, "AstryxDataTransfer").start();
    }

    private String extractBackup(Uri uri, File root) throws Exception {
        String canonicalRoot = root.getCanonicalPath() + File.separator;
        long total = 0;
        int count = 0;
        InputStream raw = getContentResolver().openInputStream(uri);
        if (raw == null) throw new IllegalArgumentException("Could not open the selected ZIP");
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
            ZipEntry entry;
            byte[] buffer = new byte[32768];
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > 100000) throw new IllegalArgumentException("Backup contains too many entries");
                String name = entry.getName();
                if (name == null || name.startsWith("/") || name.contains("\\") || name.matches("^[A-Za-z]:.*")) {
                    throw new IllegalArgumentException("Unsafe path in backup");
                }
                File target = new File(root, name);
                String canonical = target.getCanonicalPath();
                if (!canonical.equals(root.getCanonicalPath()) && !canonical.startsWith(canonicalRoot)) {
                    throw new IllegalArgumentException("Backup contains an unsafe path");
                }
                if (entry.isDirectory()) {
                    if (!target.isDirectory() && !target.mkdirs()) throw new IllegalStateException("Cannot create folder");
                } else {
                    File parent = target.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                        throw new IllegalStateException("Cannot create destination folder");
                    }
                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                            total += read;
                            if (total > MAX_UNPACKED_BYTES) throw new IllegalArgumentException("Backup exceeds 4 GB unpacked limit");
                            out.write(buffer, 0, read);
                        }
                    }
                }
                zip.closeEntry();
            }
        }
        if (count == 0) throw new IllegalArgumentException("The selected ZIP is empty or invalid");
        return "Imported " + count + " backup entries. Restart Astryx before launching the imported game data.";
    }

    private String createBackup(Uri destination, File root) throws Exception {
        String canonicalRoot = root.getCanonicalPath();
        long[] total = {0};
        int[] count = {0};
        java.io.OutputStream raw = getContentResolver().openOutputStream(destination);
        if (raw == null) throw new IllegalStateException("Could not create the backup file");
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(raw))) {
            addDirectoryToZip(root, root, canonicalRoot, zip, total, count);
        }
        if (count[0] == 0) throw new IllegalStateException("Astryx storage is empty; nothing to back up");
        return "Exported " + count[0] + " files (" + (total[0] / (1024 * 1024)) + " MB).";
    }

    private void addDirectoryToZip(File base, File current, String canonicalRoot,
                                   ZipOutputStream zip, long[] total, int[] count) throws Exception {
        File[] children = current.listFiles();
        if (children == null) return;
        byte[] buffer = new byte[32768];
        for (File child : children) {
            String canonical = child.getCanonicalPath();
            if (!canonical.equals(canonicalRoot) && !canonical.startsWith(canonicalRoot + File.separator)) continue;
            if (child.isDirectory()) {
                addDirectoryToZip(base, child, canonicalRoot, zip, total, count);
            } else if (child.isFile()) {
                String name = base.toURI().relativize(child.toURI()).getPath();
                if (name.isEmpty()) continue;
                zip.putNextEntry(new ZipEntry(name));
                try (InputStream in = new BufferedInputStream(new java.io.FileInputStream(child))) {
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        total[0] += read;
                        if (total[0] > MAX_UNPACKED_BYTES) throw new IllegalArgumentException("Backup exceeds 4 GB limit");
                        zip.write(buffer, 0, read);
                    }
                }
                zip.closeEntry();
                count[0]++;
            }
        }
    }
}
