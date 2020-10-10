package io.github.lz233.mediaclassifiedbyplaylist;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private OkHttpClient client = new OkHttpClient();
    private Headers neteaseHeaders = new Headers.Builder()
            .add("User-Agent", "NeteaseMusic/5.4.1.1533213048(122);Dalvik/2.1.0 (Linux; U; Android 8.1.0; Redmi Note 5 MIUI/8.8.2)")
            .add("Cookie", "buildver=1533213048; resolution=2030x1080; osver=8.1.0; appver=5.4.1; versioncode=122; mobilename=RedmiNote5; os=android; channel=netease")
            .add("X-Real-IP", "112.88.230.176")
            .add("Content-Type", "application/x-www-form-urlencoded; charset=utf-8").build();

    private LinearLayout mainLinearLayout;
    private LinearLayout aboutLinearLayout;
    private AppCompatButton coolapkAppCompatButton;
    private AppCompatButton githubAppCompatButton;
    private TextInputEditText linkTextInputEditText;
    private AppCompatTextView musicFolderAppCompatTextView;
    private AppCompatButton musicFolderAppCompatButton;
    private AppCompatTextView destinationFolderAppCompatTextView;
    private AppCompatButton destinationFolderAppCompatButton;
    private SwitchMaterial deleteOriginalFileSwitchMaterial;
    private AppCompatButton startAppCompatButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        Toolbar toolbar = findViewById(R.id.toolbar);
        mainLinearLayout = findViewById(R.id.mainLinearLayout);
        aboutLinearLayout = findViewById(R.id.aboutLinearLayout);
        coolapkAppCompatButton = findViewById(R.id.coolapkAppCompatButton);
        githubAppCompatButton = findViewById(R.id.githubAppCompatButton);
        linkTextInputEditText = findViewById(R.id.linkTextInputEditText);
        musicFolderAppCompatTextView = findViewById(R.id.musicFolderAppCompatTextView);
        musicFolderAppCompatButton = findViewById(R.id.musicFolderAppCompatButton);
        destinationFolderAppCompatTextView = findViewById(R.id.destinationFolderAppCompatTextView);
        destinationFolderAppCompatButton = findViewById(R.id.destinationFolderAppCompatButton);
        deleteOriginalFileSwitchMaterial = findViewById(R.id.deleteOriginalFileSwitchMaterial);
        startAppCompatButton = findViewById(R.id.startAppCompatButton);
        progressBar = findViewById(R.id.progressBar);
        //
        setSupportActionBar(toolbar);
        sharedPreferences = getSharedPreferences("setting", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        mainLinearLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        //状态栏icon
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (mode == Configuration.UI_MODE_NIGHT_NO) {
            this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        linkTextInputEditText.setText(sharedPreferences.getString("playListUri", ""));
        musicFolderAppCompatTextView.setText(DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("musicFolderUriTree", ""))).getName());
        destinationFolderAppCompatTextView.setText(DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("destinationFolderUriTree", ""))).getName());
        deleteOriginalFileSwitchMaterial.setChecked(sharedPreferences.getBoolean("deleteOriginalFile",false));
        //
        coolapkAppCompatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        linkTextInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                editor.putString("playListUri", editable.toString());
                editor.apply();
            }
        });
        musicFolderAppCompatButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, 0);
        });
        destinationFolderAppCompatButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, 1);
        });
        deleteOriginalFileSwitchMaterial.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b){
                editor.putBoolean("deleteOriginalFile",true);
            }else {
                editor.putBoolean("deleteOriginalFile",false);
            }
            editor.apply();
        });
        startAppCompatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (linkTextInputEditText.getEditableText().toString().equals("") | sharedPreferences.getString("musicFolderUriTree", "").equals("") | sharedPreferences.getString("destinationFolderUriTree", "").equals("")) {
                    Snackbar.make(view, R.string.error_tips, BaseTransientBottomBar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(view, R.string.progress_tips, BaseTransientBottomBar.LENGTH_SHORT).show();
                    startAppCompatButton.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    new Thread(() -> {
                        try {
                            String playListUrl = linkTextInputEditText.getEditableText().toString();
                            String playListId = playListUrl.substring(playListUrl.indexOf("?id=") + 4, playListUrl.indexOf("&userid="));
                            //Snackbar.make(view,playListId, BaseTransientBottomBar.LENGTH_SHORT).show();
                            Request request1 = new Request.Builder().url("https://music.163.com/api/playlist/detail?id=" + playListId + "&offset=0&total=true&limit=200").headers(neteaseHeaders).build();
                            Response response1 = client.newCall(request1).execute();
                            JSONObject jsonObject1 = new JSONObject(response1.body().string()).getJSONObject("result");
                            progressBar.post(() -> {
                                progressBar.setIndeterminate(false);
                                progressBar.setMax(jsonObject1.optInt("trackCount"));
                            });
                            JSONArray trackArrary = jsonObject1.optJSONArray("tracks");
                            final int takeFlags = getIntent().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            Uri musicFolderUriTree = Uri.parse(sharedPreferences.getString("musicFolderUriTree", ""));
                            Uri destinationFolderUriTree = Uri.parse(sharedPreferences.getString("destinationFolderUriTree", ""));
                            getContentResolver().takePersistableUriPermission(musicFolderUriTree, takeFlags);
                            getContentResolver().takePersistableUriPermission(destinationFolderUriTree, takeFlags);
                            DocumentFile musicFolderRoot = DocumentFile.fromTreeUri(MainActivity.this, musicFolderUriTree);
                            DocumentFile destinationFolderRoot = DocumentFile.fromTreeUri(MainActivity.this, destinationFolderUriTree);
                            DocumentFile playListFolderRoot;
                            try {
                                if (destinationFolderRoot.findFile(jsonObject1.optString("name")).isDirectory()) {
                                    playListFolderRoot = destinationFolderRoot.findFile(jsonObject1.optString("name"));
                                } else {
                                    playListFolderRoot = destinationFolderRoot.createDirectory(jsonObject1.optString("name"));
                                }
                            } catch (Exception e) {
                                playListFolderRoot = destinationFolderRoot.createDirectory(jsonObject1.optString("name"));
                            }

                            for (int i = 0; i < trackArrary.length(); i++) {
                                progressBar.setProgress(i+1);
                                boolean isFlac = false;
                                JSONObject songJSONObject = trackArrary.optJSONObject(i);
                                String songName = getSongName(songJSONObject);
                                DocumentFile musicFileMp3 = musicFolderRoot.findFile(songName + ".mp3");
                                DocumentFile musicFileFlac = musicFolderRoot.findFile(songName + ".flac");
                                if (musicFileMp3 == null) isFlac = true;
                                if ((musicFileMp3 == null) & (musicFileFlac == null)) {
                                    //DocumentFile musicFile2 = playListFolderRoot.createFile("")
                                } else {
                                    String musicName = isFlac ? musicFileFlac.getName() : musicFileMp3.getName();
                                    Log.i("musicName", musicName);
                                    try {
                                        playListFolderRoot.findFile(musicName).isFile();
                                    }catch (Exception e){
                                        DocumentFile musicFile = playListFolderRoot.createFile(isFlac?"audio/x-flac":"audio/mpeg",musicName);
                                        InputStream inputStream = getContentResolver().openInputStream(isFlac ? musicFileFlac.getUri() : musicFileMp3.getUri());
                                        OutputStream outputStream = getContentResolver().openOutputStream(musicFile.getUri());
                                        copyFile(inputStream,outputStream);
                                        if(sharedPreferences.getBoolean("deleteOriginalFile",false)) {
                                            if (isFlac) {
                                                musicFileFlac.delete();
                                            } else {
                                                musicFileMp3.delete();
                                            }
                                        }
                                    }
                                }
                            }
                            progressBar.post(() -> {
                                progressBar.setVisibility(View.GONE);
                                startAppCompatButton.setVisibility(View.VISIBLE);
                                progressBar.setIndeterminate(true);
                            });
                            Snackbar.make(view, R.string.done_tips, BaseTransientBottomBar.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        });
    }

    private void copyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buff = new byte[1024];
        int len;
        while ((len=inputStream.read(buff))!=-1){
            outputStream.write(buff,0,len);
        }
        inputStream.close();
        outputStream.close();
    }

    private String getSongName(JSONObject songJSONObject) {
        JSONArray artistsJSONArray = songJSONObject.optJSONArray("artists");
        String result = "";
        StringBuilder artists = new StringBuilder();
        for (int i = 0; i < artistsJSONArray.length(); i++) {
            artists.append(artistsJSONArray.optJSONObject(i).optString("name") + " ");
        }
        result = artists.toString() + "- " + songJSONObject.optString("name");
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case 0:
                    Uri musicFolderUriTree = resultData.getData();
                    if (musicFolderUriTree != null) {
                        editor.putString("musicFolderUriTree", musicFolderUriTree.toString());
                        editor.apply();
                        //final int takeFlags = getIntent().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        //getContentResolver().takePersistableUriPermission(uriTree, takeFlags);
                        // 创建所选目录的DocumentFile，可以使用它进行文件操作
                        DocumentFile musicFolderRoot = DocumentFile.fromTreeUri(this, musicFolderUriTree);
                        musicFolderAppCompatTextView.setText(musicFolderRoot.getName());
                        // 比如使用它创建文件夹
                        //DocumentFile[] rootList = root.listFiles();
                        //shotOneTextViaSAF(root);
                    }
                    break;
                case 1:
                    Uri destinationFolderUriTree = resultData.getData();
                    if (destinationFolderUriTree != null) {
                        editor.putString("destinationFolderUriTree", destinationFolderUriTree.toString());
                        editor.apply();
                        DocumentFile destinationFolderRoot = DocumentFile.fromTreeUri(this, destinationFolderUriTree);
                        destinationFolderAppCompatTextView.setText(destinationFolderRoot.getName());
                    }
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}