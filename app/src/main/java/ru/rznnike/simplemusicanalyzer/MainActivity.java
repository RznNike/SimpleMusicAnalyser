package ru.rznnike.simplemusicanalyzer;

import android.content.pm.PackageManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

public class MainActivity extends AppCompatActivity {
    private View container;
    private AppCompatTextView textViewPath;
    private TextInputEditText editTextBitrate;
    private AppCompatButton buttonSearch;
    private ListView listView;
    private String directoryPath;
    private FilePickerDialog dialog;
    private int bitrateBorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);
        textViewPath = findViewById(R.id.textViewPath);
        editTextBitrate = findViewById(R.id.editTextBitrate);
        buttonSearch = findViewById(R.id.buttonSearch);
        listView = findViewById(R.id.listView);

        textViewPath.setOnClickListener(v -> onTextViewPathClick());
        buttonSearch.setOnClickListener(v -> onButtonSearchClick());
        directoryPath = null;
        bitrateBorder = 0;
    }

    protected void showSnackMessage(String message) {
        Snackbar.make(container, message, Snackbar.LENGTH_SHORT).show();
    }

    private void onTextViewPathClick() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        dialog = new FilePickerDialog(this, properties);
        dialog.setTitle("Выберите файл");
        dialog.setDialogSelectionListener(files -> {
            if ((files != null) && (files.length > 0)) {
                textViewPath.setText(files[0]);
                directoryPath = files[0];
            }
        });
        dialog.show();
    }

    private void onButtonSearchClick() {
        if ((directoryPath != null) && parseBitrate() && (bitrateBorder > 0)) {
            List<String> filePaths = new ArrayList<>();
            File directory = new File(directoryPath);
            getFilePathsRecursive(filePaths, directory);

            buttonSearch.setEnabled(false);
            ListAdapter adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, new String[] {"Идет поиск..."});
            listView.setAdapter(adapter);
            new Thread(() -> checkFilesBitrate(filePaths)).start();
        }
    }

    private boolean parseBitrate() {
        try {
            bitrateBorder = 1000 * Integer.parseInt(Objects.requireNonNull(editTextBitrate.getText()).toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkFilesBitrate(List<String> filePaths) {
        List<String> filteredFilesInfo = new ArrayList<>();

        for (String filePath : filePaths) {
            try {
                if ((filePath.endsWith(".wma")) || filePath.endsWith(".WMA")) {
                    filteredFilesInfo.add(String.format(Locale.getDefault(), "%s\nНЕПОДДЕРЖИВАЕМЫЙ ФОРМАТ",
                            filePath));
                } else if (filePath.endsWith(".mp3")
                        || filePath.endsWith(".MP3")
                        || filePath.endsWith(".m4a")) {
                    MediaExtractor mediaExtractor = new MediaExtractor();
                    mediaExtractor.setDataSource(filePath);
                    MediaFormat mediaFormat = mediaExtractor.getTrackFormat(0);
                    int bitrate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                    int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

                    if (bitrate < bitrateBorder) {
                        filteredFilesInfo.add(String.format(Locale.getDefault(), "%s\nbitrate:%d   sample rate:%.1f",
                                filePath,
                                bitrate / 1000,
                                sampleRate / 1000f));
                    }
                } else {
                    Log.d("UNUSED_FORMATS", filePath.substring(filePath.length() - 5));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("UNUSED_FORMATS_BAD", filePath.substring(filePath.length() - 5));
            }
        }

        if (filteredFilesInfo.size() == 0) {
            filteredFilesInfo.add("Ничего не найдено");
        }
        runOnUiThread(() -> {
            ListAdapter adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, filteredFilesInfo);
            listView.setAdapter(adapter);
            buttonSearch.setEnabled(true);
        });
    }

    private void getFilePathsRecursive(List<String> resultArray, File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    getFilePathsRecursive(resultArray, file);
                } else {
                    resultArray.add(file.getPath());
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (dialog != null)
                    {
                        dialog.show();
                    }
                }
                else {
                    showSnackMessage("Необходимо предоставить разрешение на чтение файлов");
                }
            }
        }
    }
}
