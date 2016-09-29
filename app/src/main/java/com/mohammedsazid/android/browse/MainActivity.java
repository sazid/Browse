package com.mohammedsazid.android.browse;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements TextView.OnEditorActionListener, PopupMenu.OnMenuItemClickListener {

    private static final long UI_HIDE_DELAY = TimeUnit.SECONDS.toMillis(3);

    private List<String> autoCompleteList = new ArrayList<>();
    private Handler handler = new Handler();
    private Runnable uiHiderRunnable;

    private VideoEnabledWebView webView;
    private View nonVideoLayout;
    private ViewGroup videoLayout;
    private View loadingView;
    private AutoCompleteTextView addressBarEt;
    private TextView titleTv;
    private ImageView iconIv;
    private ProgressBar progressBar;
    private ImageButton menuButton;
    private View bottomBar;

    private VideoEnabledWebChromeClient webChromeClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupWebView();
        setupAutoCompleteList();
        setupAddressBar();
        prepareUiHiderRunnable();
        checkAndLaunchUrlFromIntent(getIntent());
    }

    private void bindViews() {
        webView = (VideoEnabledWebView) findViewById(R.id.browse_webView);
        nonVideoLayout = findViewById(R.id.nonVideoLayout);
        videoLayout = (ViewGroup) findViewById(R.id.videoLayout);
        addressBarEt = (AutoCompleteTextView) findViewById(R.id.addressBar_et);
        titleTv = (TextView) findViewById(R.id.title_tv);
        iconIv = (ImageView) findViewById(R.id.icon_iv);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        menuButton = (ImageButton) findViewById(R.id.menu_button);
        bottomBar = findViewById(R.id.bottom_bar);

        loadingView = getLayoutInflater().inflate(R.layout.view_loading_video, null);
    }

    private void prepareUiHiderRunnable() {
        uiHiderRunnable = new Runnable() {
            @Override
            public void run() {
                if (webView.hasFocus()) {
                    bottomBar.animate()
                            .setDuration(200)
                            .translationY(bottomBar.getMeasuredHeight())
                            .start();
                } else {
                    handler.postDelayed(uiHiderRunnable, UI_HIDE_DELAY);
                }
            }
        };
    }

    private void hideUi() {
        handler.removeCallbacks(uiHiderRunnable);
        handler.postDelayed(uiHiderRunnable, UI_HIDE_DELAY);
    }

    private void showUi() {
        if (bottomBar.getTranslationY() != 0) {
            bottomBar.animate()
                    .setDuration(200)
                    .translationY(0)
                    .start();
        }

        hideUi();
    }

    private void checkAndLaunchUrlFromIntent(Intent intent) {
        if (intent != null
                && intent.getAction() != null
                && intent.getData() != null
                && intent.getAction().equals(Intent.ACTION_VIEW)) {
            loadWebPage(getIntent().getDataString());
        } else if (TextUtils.isEmpty(webView.getUrl())) {
            // TODO: Use SharedPreferences for storing user's home page
//            webView.loadUrl("http://saved.io/");
        }
    }

    private void setupAutoCompleteList() {
        autoCompleteList.add("animeheaven.eu");
        autoCompleteList.add("gmail.com");
        autoCompleteList.add("webtoons.com");
        autoCompleteList.add("google.com");
        autoCompleteList.add("facebook.com");
        autoCompleteList.add("twitter.com");
        autoCompleteList.add("youtube.com");
        autoCompleteList.add("keep.google.com");
        autoCompleteList.add("saved.io");
    }

    private void setupAddressBar() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, autoCompleteList);
        addressBarEt.setAdapter(adapter);

        addressBarEt.setOnEditorActionListener(this);
        addressBarEt.setFocusable(true);
        addressBarEt.setFocusableInTouchMode(true);
        addressBarEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    addressBarEt.selectAll();
                }
            }
        });
    }

    private void setupWebView() {
//        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setAppCachePath(getCacheDir().getAbsolutePath()
                + File.pathSeparator + "appCache" + File.pathSeparator);
        webView.getSettings().setDatabaseEnabled(true);
        webView.setSaveEnabled(true);

        webChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, loadingView, webView) {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }

                super.onProgressChanged(view, newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                setTaskTitleAndIcon(title, null);
                addressBarEt.setText(webView.getUrl());
                if (bottomBar.getVisibility() != View.VISIBLE) {
                    bottomBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
                if (icon != null) {
                    iconIv.setImageBitmap(icon);
                } else {
                    iconIv.setImageResource(R.drawable.ic_default);
                }
                setTaskTitleAndIcon(titleTv.getText().toString(), icon);
            }
        };

        webChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback() {
            @Override
            public void toggledFullscreen(boolean fullscreen) {
                // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
                if (fullscreen) {
                    WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                    getWindow().setAttributes(attrs);
                    hideSystemUI();
//                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                } else {
                    WindowManager.LayoutParams attrs = getWindow().getAttributes();
                    attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                    attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                    getWindow().setAttributes(attrs);
                    showSystemUI();
//                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }
            }
        });

        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                webView.requestFocus();
                hideKeyboard();
                return false;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
//                Intent i = new Intent(Intent.ACTION_VIEW);
//                i.setData(Uri.parse(url));
//                startActivity(i);

                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

                try {
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), "Downloading file...",
                            Toast.LENGTH_LONG).show();
                } catch (SecurityException e) {
                    Toast.makeText(getApplicationContext(), "Please enable STORAGE permission!",
                            Toast.LENGTH_LONG).show();
                    openAppDetailsIntent(MainActivity.this, getPackageName());
                }
            }
        });

        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                showUi();
                return false;
            }
        });

        webView.setWebChromeClient(webChromeClient);
        webView.setWebViewClient(new InsideWebViewClient(this));
    }

    public static void openAppDetailsIntent(Context context, String packageName) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:" + packageName));
        context.startActivity(intent);
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTaskTitleAndIcon(String title, @Nullable Bitmap icon) {
        titleTv.setText(title);
        setTitle(title);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription;

            if (icon != null) {
                taskDescription = new ActivityManager.TaskDescription("B: " + title, icon);
                setTaskDescription(taskDescription);
            } else {
                taskDescription = new ActivityManager.TaskDescription("B: " + title);
                setTaskDescription(taskDescription);
            }

            setTaskDescription(taskDescription);
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            loadWebPage(textView.getText().toString());
            hideKeyboard();
        }

        return true;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }

    private void loadWebPage(String queryOrUrl) {
        queryOrUrl = queryOrUrl.trim();
        boolean isUrl = Patterns.WEB_URL.matcher(queryOrUrl).matches();
        if (isUrl) {
            Uri url = Uri.parse(queryOrUrl);
            if (TextUtils.isEmpty(url.getScheme())) {
                url = url.buildUpon()
                        .scheme("http")
                        .build();
            }

            webView.loadUrl(url.toString());
            return;
        }

        Uri searchQuery = Uri.parse("https://www.google.com/search")
                .buildUpon()
                .appendQueryParameter("q", queryOrUrl)
                .build();
        webView.loadUrl(searchQuery.toString());
    }

    @Override
    public void onBackPressed() {
        if (!webChromeClient.onBackPressed()) {
            if (webView.canGoBack()) {
                webView.stopLoading();
                webView.goBack();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    public void onMenuButtonClick(View v) {
//        webView.stopLoading();
        showMainMenu(menuButton);
    }

    private void showMainMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater()
                .inflate(R.menu.menu_main, popup.getMenu());
        popup.setOnMenuItemClickListener(this);

        // force shows popup menu icons
//        try {
//            Field mFieldPopup = popup.getClass().getDeclaredField("mPopup");
//            mFieldPopup.setAccessible(true);
//            MenuPopupHelper mPopup = (MenuPopupHelper) mFieldPopup.get(popup);
//            mPopup.setForceShowIcon(true);
//        } catch (Exception e) {
//        }

        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_stop:
                webView.stopLoading();
                return true;

            case R.id.action_reload:
                webView.stopLoading();
                webView.reload();
                return true;

            case R.id.action_forward:
                if (webView.canGoForward()) {
                    webView.goForward();
                }
                return true;

            case R.id.action_settings:
                return true;

            case R.id.action_new_tab:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    startActivity(intent);
                }
                return true;

            case R.id.action_about:
                showAboutDialog();
                return true;

            case R.id.action_share:
                shareUrl();
                return true;
        }

        return false;
    }

    private void composeEmail(String[] addresses, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void showAboutDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.app_name)
                .content(R.string.about_summary)
                .positiveText("OK")
                .neutralText("EMAIL")
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        composeEmail(
                                new String[]{"sazidozon@gmail.com"},
                                "[Browse]: Feedback & Suggestions",
                                ""
                        );
                    }
                })
                .show();
    }

    private void shareUrl() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
//        i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
        i.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
        startActivity(Intent.createChooser(i, "Share URL"));
    }

    private class InsideWebViewClient extends WebViewClient {

        private Activity activity;

        public InsideWebViewClient(Activity activity) {
            this.activity = activity;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        // Force links to be opened inside WebView and not in Default Browser
        // Thanks http://stackoverflow.com/a/33681975/1815624
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        // Force links to be opened inside WebView and not in Default Browser
        // Thanks http://stackoverflow.com/a/33681975/1815624
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Toast.makeText(
                    activity,
                    "Error occurred: " + description,
                    Toast.LENGTH_SHORT
            ).show();
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            Toast.makeText(
                    activity,
                    "Error occurred: " + error.getDescription(),
                    Toast.LENGTH_SHORT
            ).show();
            super.onReceivedError(view, request, error);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            webView.requestFocus();
        }
    }

}
