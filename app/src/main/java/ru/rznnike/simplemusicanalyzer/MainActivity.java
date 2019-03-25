package ru.rznnike.simplemusicanalyzer;

import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.documentfile.provider.DocumentFile;

public class MainActivity extends AppCompatActivity {
    private static final int READ_REQUEST_CODE = 100;

    private AppCompatTextView textViewPath;
    private TextInputEditText editTextBitrate;
    private AppCompatButton buttonSearch;
    private ListView listView;
    private Uri directoryUri;
    private int bitrateBorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewPath = findViewById(R.id.textViewPath);
        editTextBitrate = findViewById(R.id.editTextBitrate);
        buttonSearch = findViewById(R.id.buttonSearch);
        listView = findViewById(R.id.listView);

        textViewPath.setOnClickListener(v -> onTextViewPathClick());
        buttonSearch.setOnClickListener(v -> onButtonSearchClick());
        directoryUri = null;
        bitrateBorder = 0;
    }

    private void onTextViewPathClick() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if ((requestCode == READ_REQUEST_CODE) && (resultCode == RESULT_OK)) {
            directoryUri = resultData.getData();
            textViewPath.setText(directoryUri.getPath());
        }
    }

    private void onButtonSearchClick() {
        buttonSearch.setEnabled(false);
        ListAdapter adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new String[] {"Идет анализ..."});
        listView.setAdapter(adapter);
        if ((directoryUri != null) && parseBitrate() && (bitrateBorder > 0)) {
            new Thread(() -> {

                List<Uri> filePaths = new ArrayList<>();
                DocumentFile directory = DocumentFile.fromTreeUri(this, directoryUri);
                getFilePathsRecursive(filePaths, directory);

                runOnUiThread(() -> {
                    buttonSearch.setEnabled(false);
                    ListAdapter adapter2 = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, new String[] {"Идет поиск..."});
                    listView.setAdapter(adapter2);
                });
                checkFilesBitrate(filePaths);
            }).start();
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

    private void checkFilesBitrate(List<Uri> fileUris) {
        List<String> filteredFilesInfo = new ArrayList<>();

        for (Uri uri : fileUris) {
            try {
                if ((uri.getPath().endsWith(".wma")) || uri.getPath().endsWith(".WMA")) {
                    filteredFilesInfo.add(String.format(Locale.getDefault(), "%s\nНЕПОДДЕРЖИВАЕМЫЙ ФОРМАТ",
                            uri.getLastPathSegment()));
                } else if (uri.getPath().endsWith(".mp3")
                        || uri.getPath().endsWith(".MP3")
                        || uri.getPath().endsWith(".m4a")) {
                    MediaExtractor mediaExtractor = new MediaExtractor();
                    mediaExtractor.setDataSource(this, uri, null);
                    MediaFormat mediaFormat = mediaExtractor.getTrackFormat(0);
                    int bitrate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                    int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

                    if (bitrate < bitrateBorder) {
                        filteredFilesInfo.add(String.format(Locale.getDefault(), "%s\nbitrate:%d   sample rate:%.1f",
                                uri.getLastPathSegment(),
                                bitrate / 1000,
                                sampleRate / 1000f));
                    }
                } else {
                    Log.d("UNUSED_FORMATS", uri.getPath().substring(uri.getPath().length() - 5));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("UNUSED_FORMATS_BAD", uri.getPath().substring(uri.getPath().length() - 5));
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

    private void getFilePathsRecursive(List<Uri> resultArray, DocumentFile directory) {
        DocumentFile[] files = directory.listFiles();
        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                getFilePathsRecursive(resultArray, file);
            } else {
                resultArray.add(file.getUri());
            }
        }
    }
}
