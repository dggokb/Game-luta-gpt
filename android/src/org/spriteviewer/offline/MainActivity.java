package org.spriteviewer.offline;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Offline viewer only. No network permission, native JS bridge or file picker. */
public final class MainActivity extends Activity {
    private WebView viewer;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff10151d);
        getWindow().setNavigationBarColor(0xff10151d);
        viewer = new WebView(this);
        viewer.setBackgroundColor(0xff10151d);
        WebSettings settings = viewer.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setBlockNetworkLoads(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        viewer.setWebChromeClient(new WebChromeClient());
        viewer.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !request.getUrl().toString().startsWith("file:///android_asset/");
            }
        });
        setContentView(viewer);
        viewer.loadUrl("file:///android_asset/index.html");
    }

    @Override public void onBackPressed() {
        if (viewer.canGoBack()) viewer.goBack();
        else super.onBackPressed();
    }

    @Override protected void onPause() {
        viewer.evaluateJavascript("if(typeof playing!=='undefined'){playing=false;document.getElementById('play').textContent='Reproduzir';}", null);
        viewer.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (viewer != null) viewer.onResume();
    }

    @Override protected void onDestroy() {
        viewer.destroy();
        super.onDestroy();
    }
}
