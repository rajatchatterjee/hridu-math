package com.hridu.math;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Full-screen WebView that runs the game bundled from /web.
 * The TV remote's D-pad and OK arrive in the page as arrow / Enter keys;
 * Back is forwarded to window.hriduBack() so it pauses or goes up a screen,
 * and only exits the app from the title screen.
 * Voice-over: Android WebView has no speechSynthesis, so the page talks to
 * the phone/TV's own text-to-speech engine through window.HriduTTS.
 * Updates: window.HriduApp checks GitHub's latest Release and installs it
 * with Android's PackageInstaller (no Downloader needed after the first time).
 */
public class MainActivity extends Activity {
    private WebView web;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;

    private static final String RELEASES_API = "https://api.github.com/repos/rajatchatterjee/hridu-math/releases/latest";
    private static final String ACTION_INSTALL_STATUS = "com.hridu.math.INSTALL_STATUS";
    private volatile String updateUrl = null;
    private BroadcastReceiver installReceiver;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // saves progress (localStorage)
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        web.setBackgroundColor(Color.rgb(0x16, 0x20, 0x3A));
        web.setWebViewClient(new WebViewClient());
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);

        startTts();
        web.addJavascriptInterface(new TtsBridge(), "HriduTTS");
        web.addJavascriptInterface(new AppBridge(), "HriduApp");
        registerInstallReceiver();

        setContentView(web);
        hideSystemUi();
        web.loadUrl("file:///android_asset/index.html");
        web.requestFocus();
    }

    private void startTts() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) return;
            int r = tts.setLanguage(new Locale("en", "IN"));   // Indian English if installed
            if (r < 0) tts.setLanguage(Locale.US);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { }
                @Override public void onDone(String id) { js("window.hriduVoiceDone && hriduVoiceDone('" + id + "')"); }
                @Override public void onError(String id) { js("window.hriduVoiceError && hriduVoiceError('" + id + "')"); }
            });
            ttsReady = true;
        });
    }

    private void js(String code) {
        runOnUiThread(() -> { if (web != null) web.evaluateJavascript(code, null); });
    }

    /** Called from the game's JavaScript. */
    private class TtsBridge {
        @JavascriptInterface
        public boolean speak(String text, double pitch, double rate, String id) {
            if (!ttsReady) return false;
            tts.setPitch((float) pitch);
            tts.setSpeechRate((float) rate);
            return tts.speak(text, TextToSpeech.QUEUE_FLUSH, new Bundle(), id) == TextToSpeech.SUCCESS;
        }

        @JavascriptInterface
        public void stop() {
            if (ttsReady) tts.stop();
        }
    }

    /* ---------------- self-update ---------------- */

    private int currentVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private void status(String state, String detail) {
        js("window.hriduUpdateStatus && hriduUpdateStatus(" + JSONObject.quote(state) + "," + JSONObject.quote(detail == null ? "" : detail) + ")");
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toString("UTF-8");
    }

    private void checkForUpdate() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(RELEASES_API).openConnection();
            c.setRequestProperty("Accept", "application/vnd.github+json");
            c.setRequestProperty("User-Agent", "hridu-great-rescue");
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            if (c.getResponseCode() != 200) return;
            JSONObject rel = new JSONObject(readAll(c.getInputStream()));
            String tag = rel.optString("tag_name", "");            // e.g. "build-9"
            int latest = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
            JSONArray assets = rel.optJSONArray("assets");
            String url = null;
            for (int i = 0; assets != null && i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                if (a.optString("name").endsWith(".apk")) url = a.optString("browser_download_url");
            }
            if (url == null) return;
            updateUrl = url;
            js("window.hriduUpdateInfo && hriduUpdateInfo(" + latest + "," + currentVersion() + ")");
        } catch (Exception ignored) {
            // offline or GitHub unreachable: just play the installed version
        }
    }

    private void installUpdate() {
        String url = updateUrl;
        if (url == null) { status("failed", "No update found yet. Check the internet and try again."); return; }
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            status("permission", "");
            runOnUiThread(() -> {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    status("failed", "Turn on 'Install unknown apps' for Hridu's Great Rescue in the TV's Settings, then try again.");
                }
            });
            return;
        }
        PackageInstaller.Session session = null;
        try {
            status("downloading", "0");
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "hridu-great-rescue");
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            int total = c.getContentLength();
            PackageInstaller pi = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(getPackageName());
            int id = pi.createSession(params);
            session = pi.openSession(id);
            try (InputStream in = c.getInputStream(); OutputStream out = session.openWrite("update.apk", 0, total > 0 ? total : -1)) {
                byte[] buf = new byte[16384];
                int n, done = 0, lastPct = -1;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    done += n;
                    if (total > 0) {
                        int pct = (int) (done * 100L / total);
                        if (pct != lastPct && pct % 5 == 0) { lastPct = pct; status("downloading", String.valueOf(pct)); }
                    }
                }
                session.fsync(out);
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            Intent cb = new Intent(ACTION_INSTALL_STATUS).setPackage(getPackageName());
            PendingIntent pending = PendingIntent.getBroadcast(this, id, cb, flags);
            status("installing", "");
            session.commit(pending.getIntentSender());
            session.close();
        } catch (Exception e) {
            if (session != null) session.abandon();
            status("failed", "Download failed. Check the internet and try again.");
        }
    }

    private void registerInstallReceiver() {
        installReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                int st = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                    if (confirm != null) {
                        confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try { startActivity(confirm); } catch (Exception e) { status("failed", "Couldn't open the installer."); }
                    }
                } else if (st == PackageInstaller.STATUS_SUCCESS) {
                    status("done", "");
                } else {
                    String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                    status("failed", "Update didn't install" + (msg != null ? " (" + msg + ")" : "") + ".");
                }
            }
        };
        IntentFilter f = new IntentFilter(ACTION_INSTALL_STATUS);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(installReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(installReceiver, f);
    }

    /** Called from the game's JavaScript (TV/phone app only). */
    private class AppBridge {
        @JavascriptInterface
        public int versionCode() { return currentVersion(); }

        @JavascriptInterface
        public void checkUpdate() { new Thread(MainActivity.this::checkForUpdate).start(); }

        @JavascriptInterface
        public void installUpdate() { new Thread(MainActivity.this::installUpdate).start(); }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
            web.requestFocus();
        }
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.hriduBack ? window.hriduBack() : false)", value -> {
            if (!"true".equals(value)) finish();
        });
    }

    @Override
    protected void onPause() {
        if (tts != null) tts.stop();
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        if (installReceiver != null) unregisterReceiver(installReceiver);
        web.destroy();
        super.onDestroy();
    }
}
