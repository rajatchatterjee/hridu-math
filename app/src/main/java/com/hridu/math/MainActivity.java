package com.hridu.math;

import android.annotation.SuppressLint;
import android.app.Activity;
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

import java.util.Locale;

/**
 * Full-screen WebView that runs the game bundled from /web.
 * The TV remote's D-pad and OK arrive in the page as arrow / Enter keys;
 * Back is forwarded to window.hriduBack() so it pauses or goes up a screen,
 * and only exits the app from the title screen.
 * Voice-over: Android WebView has no speechSynthesis, so the page talks to
 * the phone/TV's own text-to-speech engine through window.HriduTTS.
 */
public class MainActivity extends Activity {
    private WebView web;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;

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
        web.destroy();
        super.onDestroy();
    }
}
