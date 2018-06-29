package com.mopub.common;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.mobileads.BaseWebView;
import com.mopub.mobileads.util.WebViews;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.mopub.common.util.Drawables.BACKGROUND;
import static com.mopub.common.util.Drawables.CLOSE;
import static com.mopub.common.util.Drawables.OPEN_IN_BROWSER;
import static com.mopub.common.util.Drawables.UNLEFT_ARROW;
import static com.mopub.common.util.Drawables.UNRIGHT_ARROW;

public class MoPubBrowser extends Activity {
    public static final String DESTINATION_URL_KEY = "URL";
    public static final String DSP_CREATIVE_ID = "mopub-dsp-creative-id";
    public static final int MOPUB_BROWSER_REQUEST_CODE = 1;
    private static final int INNER_LAYOUT_ID = 1;

    private WebView mWebView;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageButton mNativeBrowserButton;
    private ImageButton mCloseButton;

    private boolean mProgressBarAvailable;
    private ProgressBar mProgressBar;

    @NonNull
    public ImageButton getBackButton() {
        return mBackButton;
    }

    @NonNull
    public ImageButton getCloseButton() {
        return mCloseButton;
    }

    @NonNull
    public ImageButton getForwardButton() {
        return mForwardButton;
    }

    @NonNull
    public ImageButton getRefreshButton() {
        return mNativeBrowserButton;
    }

    @NonNull
    public WebView getWebView() {
        return mWebView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(Activity.RESULT_OK);

        // hide title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mProgressBarAvailable = getWindow().requestFeature(Window.FEATURE_PROGRESS);
        if (mProgressBarAvailable) {
            getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
        }

        setContentView(getMoPubBrowserView());

        initializeWebView();
        initializeButtons();
        enableCookies();
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void initializeWebView() {
        WebSettings webSettings = mWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);

        /*
         * Pinch to zoom is apparently not enabled by default on all devices, so
         * declare zoom support explicitly.
         * https://stackoverflow.com/questions/5125851/enable-disable-zoom-in-android-webview
         */
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);

        mWebView.loadUrl(getRequestedUrl());

        // keep track of redirect count https://stackoverflow.com/a/16795181/282502
        mWebView.setWebViewClient(new BrowserWebViewClient(this));
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                if (ActivityCompat.checkSelfPermission(MoPubBrowser.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                    DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (downloadManager != null) {
                        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

                        Uri uri = Uri.parse(url);
                        String scheme = uri.getScheme();

                        if ("http".equals(scheme) || "https".equals(scheme)) {
                            DownloadManager.Request request = new DownloadManager.Request(uri);
                            request.setMimeType(mimetype);
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                            request.setTitle(fileName);

                            // https://stackoverflow.com/a/33501835/282502
                            String cookie = CookieManager.getInstance().getCookie(url);
                            if (!TextUtils.isEmpty(cookie)) {
                                request.addRequestHeader("cookie", cookie);
                            }
                            request.addRequestHeader("User-Agent", userAgent);

                            downloadManager.enqueue(request);

                            Toast.makeText(MoPubBrowser.this, "Downloading file...", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
    }

    private String getRequestedUrl() {
        return getIntent().getStringExtra(DESTINATION_URL_KEY);
    }

    private void initializeButtons() {
        mBackButton.setBackgroundColor(Color.TRANSPARENT);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                }
            }
        });

        mForwardButton.setBackgroundColor(Color.TRANSPARENT);
        mForwardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mWebView.canGoForward()) {
                    mWebView.goForward();
                }
            }
        });

        mNativeBrowserButton.setBackgroundColor(Color.TRANSPARENT);
        mNativeBrowserButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getRequestedUrl())));
                } catch (Throwable e) {
                    MoPubLog.e("Cannot open in native browser", e);
                }
            }
        });

        mCloseButton.setBackgroundColor(Color.TRANSPARENT);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MoPubBrowser.this.finish();
            }
        });
    }

    private void enableCookies() {
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().startSync();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
        mWebView.setWebChromeClient(null);
        WebViews.onPause(mWebView, isFinishing());
    }

    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView webView, int progress) {
                if (progress == 100) {
                    setTitle(webView.getUrl());
                } else {
                    setTitle("Loading...");
                }

                if (mProgressBarAvailable && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    setProgress(progress * 100);
                }
                mProgressBar.setVisibility(progress >= 99 ? View.GONE : View.VISIBLE);
                mProgressBar.setProgress(progress);
            }
        });

        mWebView.onResume();
    }

    @Override
    public void finish() {
        // ZoomButtonController adds buttons to the window's decorview. If they're still visible
        // when finish() is called, they need to be removed or a Window object will be leaked.
        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        decorView.removeAllViews();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
        mWebView = null;
    }

    @SuppressWarnings("ResourceType") // Using XML resources causes issues in Unity
    private View getMoPubBrowserView() {
        LinearLayout moPubBrowserView = new LinearLayout(this);
        LinearLayout.LayoutParams browserLayoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        moPubBrowserView.setLayoutParams(browserLayoutParams);
        moPubBrowserView.setOrientation(LinearLayout.VERTICAL);

        RelativeLayout outerLayout = new RelativeLayout(this);
        LinearLayout.LayoutParams outerLayoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        outerLayout.setLayoutParams(outerLayoutParams);
        moPubBrowserView.addView(outerLayout);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setId(INNER_LAYOUT_ID);
        RelativeLayout.LayoutParams innerLayoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        innerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        innerLayout.setLayoutParams(innerLayoutParams);
        innerLayout.setBackgroundDrawable(BACKGROUND.createDrawable(this));
        outerLayout.addView(innerLayout);

        mBackButton = getButton(UNLEFT_ARROW.createDrawable(this));
        mForwardButton = getButton(UNRIGHT_ARROW.createDrawable(this));
        mNativeBrowserButton = getButton(OPEN_IN_BROWSER.createDrawable(this));
        mCloseButton = getButton(CLOSE.createDrawable(this));

        innerLayout.addView(mBackButton);
        innerLayout.addView(mForwardButton);
        innerLayout.addView(mNativeBrowserButton);
        innerLayout.addView(mCloseButton);

        mWebView = new BaseWebView(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ABOVE, INNER_LAYOUT_ID);
        mWebView.setLayoutParams(layoutParams);
        outerLayout.addView(mWebView);

        // add a progress bar
        mProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mProgressBar.setMax(100);
        outerLayout.addView(mProgressBar, new RelativeLayout.LayoutParams(MATCH_PARENT, Dips.dipsToIntPixels(2, this)));

        return moPubBrowserView;
    }

    private ImageButton getButton(final Drawable drawable) {
        ImageButton imageButton = new ImageButton(this);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1f);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        imageButton.setLayoutParams(layoutParams);

        imageButton.setImageDrawable(drawable);

        return imageButton;
    }

    @Deprecated
    @VisibleForTesting
    void setWebView(WebView webView) {
        mWebView = webView;
    }
}
