package com.mohammedsazid.android.browse;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements TextView.OnEditorActionListener, PopupMenu.OnMenuItemClickListener {

    private WebView webView;
    private EditText addressBarEt;
    private TextView titleTv;
    private ImageView iconIv;
    private ProgressBar progressBar;
    private ImageButton menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupWebView();
		setupAddressBar();
        webView.loadUrl("http://google.com/");
    }

    private void bindViews() {
        webView = (WebView) findViewById(R.id.browse_webview);
        addressBarEt = (EditText) findViewById(R.id.addressbar_et);
        titleTv = (TextView) findViewById(R.id.title_tv);
        iconIv = (ImageView) findViewById(R.id.icon_iv);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        menuButton = (ImageButton) findViewById(R.id.menu_button);
    }
	
	private void setupAddressBar() {
		addressBarEt.setOnEditorActionListener(this);
        addressBarEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
					if(hasFocus){
                        addressBarEt.selectAll();
					}
				}
		});
	}

    private void setupWebView() {
        final AppCompatActivity activity = this;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
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
        });

        webView.setWebChromeClient(new WebChromeClient() {
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
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
                setTaskTitleAndIcon(titleTv.getText().toString(), icon);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTaskTitleAndIcon(String title, @Nullable Bitmap icon) {
        titleTv.setText(title);
        setTitle(title);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription;

            if (icon != null) {
                iconIv.setImageBitmap(icon);

                taskDescription = new ActivityManager.TaskDescription("B: " + title, icon);
                setTaskDescription(taskDescription);
            } else {
                iconIv.setImageResource(R.drawable.ic_default);

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
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
        }

        return true;
    }

    private void loadWebPage(String queryOrUrl) {
        boolean isUrl = Patterns.WEB_URL.matcher(queryOrUrl).matches();
        if (isUrl) {
//            if (!queryOrUrl.startsWith("http") || !queryOrUrl.startsWith("https")) {
//                queryOrUrl = "http://" +  queryOrUrl;
//            }
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
        if (webView.canGoBack()) {
            webView.stopLoading();
            webView.goBack();
        } else {
            super.onBackPressed();
        }
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

                return true;
        }

        return false;
    }
}
