package com.mohammedsazid.android.browse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
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

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements TextView.OnEditorActionListener, PopupMenu.OnMenuItemClickListener, MenuItem.OnMenuItemClickListener, AdvancedWebView.Listener, DownloadListener {

    public static final int ID_SAVE_IMAGE = 1;
    public static final int ID_OPEN_IMAGE = 2;
    public static final int ID_OPEN_IMAGE_IN_NEW_WINDOW = 3;
    public static final int ID_SAVE_LINK = 4;
    public static final int ID_COPY_LINK = 5;
    public static final int ID_SHARE_LINK = 6;
    public static final int ID_OPEN_LINK_IN_NEW_WINDOW = 7;
    private static final long UI_HIDE_DELAY = TimeUnit.SECONDS.toMillis(3);
    private List<String> autoCompleteList = new ArrayList<>();
    private Handler handler = new Handler();
    private Runnable uiHiderRunnable;
    private SharedPreferences pref;

    private PopupMenu popup;
    private AdvancedWebView webView;
    private ViewGroup nonVideoLayout;
    private ViewGroup videoLayout;
    //    private View loadingView;
    private AutoCompleteTextView addressBarEt;
    private TextView titleTv;
    private ImageView iconIv;
    private ProgressBar progressBar;
    private ImageButton menuButton;
    private View bottomBar;

    @SuppressWarnings("FieldCanBeLocal")
    private WebChromeClient webChromeClient;

    public static void openAppDetailsIntent(Context context, String packageName) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:" + packageName));
        context.startActivity(intent);
    }

    public static void openAppSettings(final Context context, final String packageName) {
        try {
            final Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);

        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        bindViews();
        setupWebView();
        setupAutoCompleteList();
        setupAddressBar();
        prepareMainMenu(menuButton);
        prepareUiHiderRunnable();
        checkAndLaunchUrlFromIntent(getIntent());
    }

    private void bindViews() {
        webView = findViewById(R.id.browse_webView);
        nonVideoLayout = findViewById(R.id.nonVideoLayout);
        videoLayout = findViewById(R.id.videoLayout);
        addressBarEt = findViewById(R.id.addressBar_et);
        titleTv = findViewById(R.id.title_tv);
        iconIv = findViewById(R.id.icon_iv);
        progressBar = findViewById(R.id.progressBar);
        menuButton = findViewById(R.id.menu_button);
        bottomBar = findViewById(R.id.bottom_bar);

//        loadingView = getLayoutInflater().inflate(R.layout.view_loading_video, null);
    }

    private void prepareUiHiderRunnable() {
        uiHiderRunnable = () -> {
            if (webView.hasFocus()) {
                bottomBar.animate()
                        .setDuration(200)
                        .translationY(bottomBar.getMeasuredHeight())
                        .start();
            } else {
                handler.postDelayed(uiHiderRunnable, UI_HIDE_DELAY);
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

//        hideUi();
    }

    private void checkAndLaunchUrlFromIntent(Intent intent) {
        if (intent != null
                && intent.getAction() != null
                && intent.getData() != null
                && intent.getAction().equals(Intent.ACTION_VIEW)) {
            loadWebPage(getIntent().getDataString());
        } else if (TextUtils.isEmpty(webView.getUrl())) {
            webView.loadUrl(pref.getString("pref_home_page_key", "https://google.com/"));
        }
    }

    private void setupAutoCompleteList() {
        autoCompleteList.add("gmail.com");
        autoCompleteList.add("google.com");
        autoCompleteList.add("facebook.com");
        autoCompleteList.add("twitter.com");
        autoCompleteList.add("youtube.com");
        autoCompleteList.add("keep.google.com");
        autoCompleteList.add("saved.io");
    }

    private void setupAddressBar() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, autoCompleteList);
        addressBarEt.setAdapter(adapter);

        addressBarEt.setOnEditorActionListener(this);
        addressBarEt.setFocusable(true);
        addressBarEt.setFocusableInTouchMode(true);
        addressBarEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                addressBarEt.selectAll();
            }
        });
    }

    private void setupWebView() {
//        webView.getSettings().setJavaScriptEnabled(true);
//        webView.getSettings().setAllowFileAccess(true);
//        webView.getSettings().setAllowContentAccess(true);
//        webView.getSettings().setDomStorageEnabled(true);
//        webView.getSettings().setDatabaseEnabled(true);
        webView.setListener(this, this);

        loadWebViewSettingsOnResume();

        webChromeClient = new CustomWebChromeClient();

        webView.setOnTouchListener(new WebViewTouchListener());

        webView.setDownloadListener(this);

        webView.setWebChromeClient(webChromeClient);
        webView.setWebViewClient(new InsideWebViewClient(this));
        webView.addHttpHeader("X-Requested-With", getString(R.string.app_name));

//        webView.getSettings().setAppCacheEnabled(true);
//        webView.getSettings().setAppCachePath(getCacheDir().getAbsolutePath()
//                + File.separator + "appCache" + File.separator);
        webView.setSaveEnabled(true);

        registerForContextMenu(webView);
    }

    private boolean isAClick(float startX, float endX, float startY, float endY) {
        int CLICK_ACTION_THRESHOLD = 5;
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);
        return !(differenceX > CLICK_ACTION_THRESHOLD || differenceY > CLICK_ACTION_THRESHOLD);
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
        flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        WebView.HitTestResult hitTestResult = webView.getHitTestResult();

        switch (hitTestResult.getType()) {
            case WebView.HitTestResult.IMAGE_TYPE:
                menu.setHeaderTitle(hitTestResult.getExtra());
                menu.add(0, ID_SAVE_IMAGE, 0, "Save image").setOnMenuItemClickListener(this);
                menu.add(0, ID_OPEN_IMAGE, 1, "Open image").setOnMenuItemClickListener(this);

                menu.add(0, ID_OPEN_IMAGE_IN_NEW_WINDOW, 2, "Open image in new window")
                        .setOnMenuItemClickListener(this);
                break;
            case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                menu.setHeaderTitle(hitTestResult.getExtra());
                menu.add(0, ID_COPY_LINK, 0, "Copy link").setOnMenuItemClickListener(this);
                menu.add(0, ID_SHARE_LINK, 1, "Share link").setOnMenuItemClickListener(this);
                menu.add(0, ID_SAVE_LINK, 2, "Save link").setOnMenuItemClickListener(this);

                menu.add(1, ID_OPEN_IMAGE, 4, "Open image").setOnMenuItemClickListener(this);
                menu.add(1, ID_SAVE_IMAGE, 6, "Save image").setOnMenuItemClickListener(this);

                menu.add(0, ID_OPEN_LINK_IN_NEW_WINDOW, 3, "Open link in new window")
                        .setOnMenuItemClickListener(this);
                menu.add(1, ID_OPEN_IMAGE_IN_NEW_WINDOW, 5, "Open image in new window").setOnMenuItemClickListener(this);
                break;
            case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                menu.setHeaderTitle(hitTestResult.getExtra());
                menu.add(0, ID_COPY_LINK, 0, "Copy link").setOnMenuItemClickListener(this);
                menu.add(0, ID_SHARE_LINK, 1, "Share link").setOnMenuItemClickListener(this);
                menu.add(0, ID_SAVE_LINK, 2, "Save link").setOnMenuItemClickListener(this);

                menu.add(0, ID_OPEN_LINK_IN_NEW_WINDOW, 3, "Open link in new window")
                        .setOnMenuItemClickListener(this);
                break;
        }
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(0);
    }

    private void setTaskTitleAndIcon(String title, @Nullable Bitmap icon) {
        titleTv.setText(title);
        setTitle(title);
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

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            webView.stopLoading();
            loadWebPage(textView.getText().toString());
            hideKeyboard();
        }

        return true;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
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

        String[] searchDomainAndQueryParam = getSearchDomainAndQueryParam();

        Uri searchQuery = Uri.parse(searchDomainAndQueryParam[0])
                .buildUpon()
                .appendQueryParameter(searchDomainAndQueryParam[1], queryOrUrl)
                .build();
        webView.loadUrl(searchQuery.toString());
    }

    private String[] getSearchDomainAndQueryParam() {
        String[] s = new String[2];

        switch (pref.getString("search_engine_list_key", "0")) {
            case "0":
                s[0] = "https://www.google.com/search";
                s[1] = "q";
                break;
            case "1":
                s[0] = "https://www.bing.com/search";
                s[1] = "q";
                break;
            case "2":
                s[0] = "https://search.yahoo.com/search";
                s[1] = "p";
                break;
            case "3":
                s[0] = "https://duckduckgo.com/";
                s[1] = "q";
                break;
            case "4":
                s[0] = "https://www.youtube.com/results";
                s[1] = "search_query";
                break;
        }

        return s;
    }

    private void loadWebViewSettingsOnResume() {
        webView.setCookiesEnabled(pref.getBoolean("pref_cookies_key", true));
        if (pref.getBoolean("pref_cookies_key", true)) {
            webView.setThirdPartyCookiesEnabled(
                    pref.getBoolean("pref_third_party_cookies_key", true));
        }
        webView.setMixedContentAllowed(pref.getBoolean("pref_mixed_content_key", true));
        webView.setDesktopMode(pref.getBoolean("pref_desktop_mode_key", false));
        webView.getSettings().setLoadsImagesAutomatically(
                pref.getBoolean("pref_image_loading_key", true));
//        webView.getSettings().setBlockNetworkImage(
//                pref.getBoolean("pref_image_loading_key", true));
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        addressBarEt.setText(webView.getUrl());
    }

    @Override
    protected void onDestroy() {
        webView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!webView.onBackPressed()) {
            return;
        }

        super.onBackPressed();
//        if (!webChromeClient.onBackPressed()) {
//            if (webView.canGoBack()) {
//                webView.stopLoading();
//                webView.goBack();
//            } else {
//                super.onBackPressed();
//            }
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        webView.onActivityResult(requestCode, resultCode, data);
//        webChromeClient.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        loadWebViewSettingsOnResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    public void onMenuButtonClick(View v) {
//        webView.stopLoading();
        showMainMenu();
    }

    private void prepareMainMenu(View v) {
        popup = new PopupMenu(this, v);
        v.setOnTouchListener(popup.getDragToOpenListener());
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
    }

    private void showMainMenu() {
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        WebView.HitTestResult hitTestResult = webView.getHitTestResult();

        int itemId = item.getItemId();
        if (itemId == R.id.action_stop) {
            webView.stopLoading();
            return true;
        } else if (itemId == R.id.action_reload) {
            webView.stopLoading();
            webView.reload();
            return true;
        } else if (itemId == R.id.action_forward) {
            if (webView.canGoForward()) {
                webView.goForward();
            }
            return true;
        } else if (itemId == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, BrowseSettings.class);
            startActivity(settingsIntent);
            return true;
        } else if (itemId == R.id.action_new_tab) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_add_to_home_screen) {
            addCurrentSiteToHomeScreen();
            return true;
        } else if (itemId == R.id.action_share) {
            shareUrl(webView.getUrl());
            return true;

            // --- WebView context menu items --- //
        } else if (itemId == ID_SAVE_IMAGE) {// this will fail in several occasion
            // TODO: 9/30/16 http://stackoverflow.com/questions/3474448/saving-image-webview-android/3475772#3475772
            downloadFile(hitTestResult.getExtra(),
                    Uri.parse(hitTestResult.getExtra())
                            .getLastPathSegment());
            return true;
        } else if (itemId == ID_OPEN_IMAGE) {
            loadWebPage(hitTestResult.getExtra());
            return true;
        } else if (itemId == ID_SAVE_LINK) {
            downloadFile(hitTestResult.getExtra(),
                    Uri.parse(hitTestResult.getExtra())
                            .getLastPathSegment());
            return true;
        } else if (itemId == ID_SHARE_LINK) {
            shareUrl(hitTestResult.getExtra());
            return true;
        } else if (itemId == ID_COPY_LINK) {
            copyToClipboard(this, hitTestResult.getExtra());
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == ID_OPEN_IMAGE_IN_NEW_WINDOW || itemId == ID_OPEN_LINK_IN_NEW_WINDOW) {//                Intent newWindowIntent = new Intent(this, MainActivity.class);
            Intent newWindowIntent = new Intent(
                    Intent.ACTION_VIEW, Uri.parse(hitTestResult.getExtra()));
            newWindowIntent.setComponent(new ComponentName(this, MainActivity.class));
            startActivity(newWindowIntent);
            return true;
        }

        return false;
    }

    private void addCurrentSiteToHomeScreen() {
        final Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
        shortcutIntent.setComponent(new ComponentName(this, MainActivity.class));
        shortcutIntent.setData(Uri.parse(webView.getUrl()));

        new MaterialDialog.Builder(this)
                .title("Add to Home screen")
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                .input("Shortcut name", webView.getTitle(), false, (dialog, input) -> {
                    final Intent intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, input.toString());
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, webView.getFavicon());
                    intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    sendBroadcast(intent);

                    Toast.makeText(MainActivity.this,
                            "Shortcut added to Home screen",
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public void copyToClipboard(Context context, String text) {
        try {
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setText(text);
                }
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData
                        .newPlainText(
                                context.getResources().getString(
                                        R.string.app_name), text);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void downloadFile(String url, String fileName) {
//        String fileName = Uri.parse(url).getLastPathSegment();
        try {
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(url));

            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            dm.enqueue(request);
            Toast.makeText(this, "Downloading file...",
                    Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
//            url = url.replace("data:image/jpeg;base64,","");
            url = url.substring(url.indexOf(","));

            byte[] imageAsBytes = Base64.decode(url.getBytes(), 0);
            if (imageAsBytes == null) {
                return;
            }

            Bitmap image = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
            String filePath = getImageFileName();

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filePath);
                image.compress(Bitmap.CompressFormat.PNG, 100, out);

                Toast.makeText(this, "Image saved at:\n" + filePath,
                        Toast.LENGTH_LONG).show();
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Please enable STORAGE permission!",
                    Toast.LENGTH_LONG).show();
            openAppDetailsIntent(MainActivity.this, getPackageName());
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to download file",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private String getImageFileName() {
        String filePath = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator
                + "Image #0.png";
        File file = new File(filePath);

        int counter = 0;
        while (file.exists()) {
            filePath = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    + File.separator
                    + "Image #" + counter + ".png";
            file = new File(filePath);
            counter++;
        }

        return filePath;
    }

    private byte[] readImageFromBytes(String imageData) {
        try {
            InputStream is = new ByteArrayInputStream(imageData.getBytes());
            byte[] buffer = new byte[imageData.getBytes().length];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while ((bytesRead = is.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            return output.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

//    private void composeEmail(String[] addresses, String subject, String body) {
//        Intent intent = new Intent(Intent.ACTION_SENDTO);
//        intent.setData(Uri.parse("mailto:"));
//        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
//        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
//        intent.putExtra(Intent.EXTRA_TEXT, body);
//        if (intent.resolveActivity(getPackageManager()) != null) {
//            startActivity(intent);
//        }
//    }

    private void shareUrl(String url) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
//        i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
        i.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(i, "Share URL"));
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        webView.requestFocus();
    }

    @Override
    public void onPageFinished(String url) {
//        showUi();
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        Toast.makeText(
                this,
                "Failed to load URL: " + failingUrl + "\n" + description,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
//        downloadFile(url, suggestedFilename);
//        if (AdvancedWebView.handleDownload(this, url, suggestedFilename)) {
//            Toast.makeText(this, "Downloading file…", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(this, "Failed to download file", Toast.LENGTH_SHORT).show();
//        }
    }

    @Override
    public void onExternalPageRequest(String url) {
        Toast.makeText(
                this,
                "onExternalPageRequest(url = " + url + ")",
                Toast.LENGTH_SHORT).show();
    }

    private void showVideoInFullscreen(View view) {
        nonVideoLayout.setVisibility(View.INVISIBLE);
        videoLayout.setVisibility(View.VISIBLE);
        videoLayout.addView(view);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(attrs);
        hideSystemUI();
    }

    private void revertFullscreenVideo() {
        videoLayout.removeAllViews();
        videoLayout.setVisibility(View.GONE);
        nonVideoLayout.setVisibility(View.VISIBLE);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(attrs);
        showSystemUI();
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        request.setMimeType(mimetype);
        //------------------------COOKIE!!------------------------
        String cookies = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("cookie", cookies);
        //------------------------COOKIE!!------------------------
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("Downloading file...");
        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        try {
            try {
                dm.enqueue(request);
            } catch (SecurityException e) {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                dm.enqueue(request);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Please enable STORAGE permission!",
                    Toast.LENGTH_LONG).show();
            openAppSettings(this, getPackageName());

            return;
        }
        // if the download manager app has been disabled on the device
        catch (IllegalArgumentException e) {
            Toast.makeText(this, "Please enable the download manager.", Toast.LENGTH_SHORT).show();
            // show the settings screen where the user can enable the download manager app again
            openAppSettings(this, AdvancedWebView.PACKAGE_NAME_DOWNLOAD_MANAGER);
            return;
        } catch (Exception ignored) {
        }

        Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
    }

    @SuppressWarnings("deprecation")
    private class InsideWebViewClient extends WebViewClient {

        private Activity activity;

        InsideWebViewClient(Activity activity) {
            this.activity = activity;
        }

        @Override
        // Force links to be opened inside WebView and not in Default Browser
        // Thanks http://stackoverflow.com/a/33681975/1815624
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (uri.getScheme().equals("market")) {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(marketIntent);
                return true;
            }

            view.loadUrl(request.getUrl().toString());
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        // Force links to be opened inside WebView and not in Default Browser
        // Thanks http://stackoverflow.com/a/33681975/1815624
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (uri.getScheme().equals("market")) {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(marketIntent);
                return true;
            }

            view.loadUrl(url);
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            Toast.makeText(
                    activity,
                    "Error: " + error.getDescription(),
                    Toast.LENGTH_SHORT
            ).show();
            super.onReceivedError(view, request, error);
        }
    }

    class CustomWebChromeClient extends WebChromeClient {

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

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
            showVideoInFullscreen(view);
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            revertFullscreenVideo();
        }
    }

    class WebViewTouchListener implements View.OnTouchListener {
        private float startX;
        private float startY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (addressBarEt.hasFocus()) {
                addressBarEt.clearFocus();
                hideKeyboard();
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    break;
                case MotionEvent.ACTION_UP: {
                    float endX = event.getX();
                    float endY = event.getY();
                    if (isAClick(startX, endX, startY, endY)) {
                        hideKeyboard();
                        webView.requestFocus();
                    } else {
//                        showUi();
                    }
                    break;
                }
            }
            return false;
        }
    }
}
