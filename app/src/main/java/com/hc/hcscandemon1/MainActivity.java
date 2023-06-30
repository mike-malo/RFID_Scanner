package com.hc.hcscandemon1;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hc.pda.HcPowerCtrl;
import com.nlscan.nlsdk.NLDevice;
import com.nlscan.nlsdk.NLDeviceStream;

public class MainActivity extends AppCompatActivity implements NLDeviceStream.NLUartListener {

    private NLDeviceStream ds = new NLDevice(NLDeviceStream.DevClass.DEV_UART);
    private String path = "/dev/ttyS1";
    private String TAG = "TAG";
    private int baudrate = 9600;
    HcPowerCtrl ctrl = new HcPowerCtrl();
    public static SoundPool mSoundPool;
    public static Vibrator vibrator;
    public static int soundId = 1;
    private WebView webView = null;
    private SharedPreferences sharedPreferences = null;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initSound();

        sharedPreferences = getSharedPreferences("qishi", MODE_PRIVATE);
        String url = sharedPreferences.getString("url", "");
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setWebViewClient(new WebViewClient());
        Toast.makeText(MainActivity.this, url, Toast.LENGTH_LONG).show();
        if(url.length() > 0) {
            webView.loadUrl(url);
        } else {
            showEditor();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_url) {
            showEditor();
            return true;
        }
        if (id == R.id.action_refresh) {
            webView.loadUrl(sharedPreferences.getString("url", ""));
            return true;
        }
        if (id == R.id.action_moni) {
            sendDataToJavascript();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openScan115200();
    }

    private void openScan115200() {
        if (ctrl == null) {
            ctrl = new HcPowerCtrl();
        }
        ctrl.scanTrig(1);
        ctrl.scanPower(1);
        ctrl.scanWakeup(1);
        ctrl.scanPwrdwn(1);
        ctrl.scanTrig(0);
        SystemClock.sleep(100);
        boolean open = ds.open(path, baudrate, this);
        boolean b = ds.setConfig("@SCNTCE1");//设置指令模式
        boolean b1 = ds.setConfig("@232BAD8");//设置115200
        Log.e(TAG, "设置指令模式: " + b );
        Log.e(TAG, "设置115200: " + b1 );
        if (open) {
            ds.close();
            SystemClock.sleep(50);
            boolean open2 = ds.open(path, 115200, this);
            toast("初始化成功！"+open2);
        } else {
            toast("初始化失败！");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("TAG", "onPause: ");
        closeScan();
    }

    private void closeScan() {
        ctrl.scanPower(0);
        ds.stopScan();
        ds.close();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            Log.e(TAG, "onKeyDown: " + keyCode);
            if (keyCode == 288 || keyCode == 286 || keyCode == 287 || keyCode == 290) {
                scanD();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void showEditor() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.editor_title);
        EditText editText = new EditText(MainActivity.this);
        editText.setText(sharedPreferences.getString("url", ""));
        builder.setView(editText);
        builder.setPositiveButton(R.string.editor_btn_save, (dialogInterface, i) -> {
            String url = editText.getText().toString();
            sharedPreferences.edit().putString("url", url).apply();
            webView.loadUrl(url);
        });
        builder.setNegativeButton(R.string.editor_btn_cancel, null);
        builder.show();
    }


    private void scanD() {
//        ctrl.scanTrig(1);
//        ctrl.scanPower(1);
//        ctrl.scanWakeup(1);
//        ctrl.scanPwrdwn(1);
//        ctrl.scanTrig(0);
        ds.startScan();
    }

    private byte[] barcodeBuff = new byte[2 * 1024];

    @Override
    public void actionRecv(byte[] RecvBuff, int len) {
        if (RecvBuff != null) {
            System.arraycopy(RecvBuff, 0, barcodeBuff, 0, len);
            String barcode = new String(barcodeBuff, 0, len);
//            String str = new String(Base64.encode(barcode.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP), StandardCharsets.UTF_8);
            boolean cont = barcode.contains("\r\n");
            Log.d("lodq",barcode);
            String str = barcode.replace("\r\n", "\\r\\n");
//            str.replaceAll("\r", "\\r");
            Log.d("logh", str);
            Log.e(TAG, "actionRecv: " + barcode);
            mSoundPool.play(soundId, 1, 1, 0, 0, (float) 1.0);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("log", String.valueOf(cont));
                    webView.evaluateJavascript("javascript:aaa('" + str + "')", null);
                }
            });
        } else {
            Log.e(TAG, "actionRecv: 返回数据null");
        }
    }
    public void sendDataToJavascript() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.js_title);
        EditText editText = new EditText(MainActivity.this);
        builder.setView(editText);
        builder.setPositiveButton(R.string.js_btn_send, (dialogInterface, i) -> {
            String txt = editText.getText().toString();
//            webView.evaluateJavascript("javascript:aaa('" + txt + "')", null);
        });
        builder.setNegativeButton(R.string.editor_btn_cancel, null);
        builder.show();
    }


    private void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void initSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build();
            mSoundPool = new SoundPool.Builder().setMaxStreams(1)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 10);
        }
        vibrator = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);
        soundId = mSoundPool.load(getApplicationContext(), R.raw.dingdj5, 10);
    }

    public void clear(View view) {

    }

    public void scan(View view) {
        scanD();
    }
}