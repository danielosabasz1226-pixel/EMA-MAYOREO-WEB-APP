package com.ema.mayoreo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER = 4101;
    private static final int SAVE_DOCUMENT = 4102;
    private static final String HOME_URL =
        "file:///android_asset/ema/index.html";

    private WebView webView;
    private WebView printWebView;
    private ValueCallback<Uri[]> fileCallback;

    private byte[] pendingSaveBytes;
    private String pendingSaveMime = "application/octet-stream";

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(8, 55, 112));
        getWindow().setNavigationBarColor(Color.WHITE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(5), dp(3), dp(5), dp(3));
        toolbar.setBackgroundColor(Color.rgb(11, 78, 162));

        Button back = makeButton("←");
        Button home = makeButton("⌂");
        Button reload = makeButton("⟳");

        TextView title = new TextView(this);
        title.setText("EMA");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        toolbar.addView(back, new LinearLayout.LayoutParams(dp(52), dp(48)));
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        toolbar.addView(home, new LinearLayout.LayoutParams(dp(52), dp(48)));
        toolbar.addView(reload, new LinearLayout.LayoutParams(dp(52), dp(48)));

        root.addView(toolbar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
        ));

        webView = new WebView(this);
        configureWebView(webView);

        root.addView(webView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        setContentView(root);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        back.setOnClickListener(v -> handleBack());
        home.setOnClickListener(v -> loadHome());
        reload.setOnClickListener(v -> reloadFresh());

        loadHome();
    }

    private void configureWebView(WebView view) {
        WebSettings s = view.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setTextZoom(100);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager.getInstance().setAcceptCookie(true);

        view.setBackgroundColor(Color.WHITE);
        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        view.setVerticalScrollBarEnabled(false);
        view.setHorizontalScrollBarEnabled(false);

        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(
                    "(function(){document.documentElement.classList.add('ema-android');" +
                    "document.body.classList.add('ema-android');" +
                    "document.querySelectorAll('.cod').forEach(function(e){e.remove();});})();",
                    null
                );
            }

            @Override
            public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error
            ) {
                if (request == null || request.isForMainFrame()) {
                    Toast.makeText(
                        MainActivity.this,
                        "EMA tuvo un problema al cargar. Pulsa ⟳ para actualizar.",
                        Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });

        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> uploadMsg,
                FileChooserParams fileChooserParams
            ) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = uploadMsg;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel.sheet.macroEnabled.12",
                    "text/csv",
                    "text/html",
                    "application/octet-stream"
                });
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

                try {
                    startActivityForResult(intent, FILE_CHOOSER);
                } catch (Exception error) {
                    fileCallback = null;
                    Toast.makeText(
                        MainActivity.this,
                        "No se pudo abrir el selector de archivos.",
                        Toast.LENGTH_LONG
                    ).show();
                    return false;
                }
                return true;
            }
        });

        view.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(
                String url,
                String userAgent,
                String contentDisposition,
                String mimeType,
                long contentLength
            ) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {
                    Toast.makeText(
                        MainActivity.this,
                        "Usa la opción Guardar cuando EMA la muestre.",
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(20);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private void loadHome() {
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.loadUrl(HOME_URL + "?v=1.3.0");
    }

    private void reloadFresh() {
        webView.clearCache(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.loadUrl(HOME_URL + "?v=1.3.0");
    }

    private void handleBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }

    private void saveFileWithSystem(String filename, String mime, byte[] bytes) {
        pendingSaveBytes = bytes;
        pendingSaveMime = mime == null || mime.isEmpty()
            ? "application/octet-stream"
            : mime;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(pendingSaveMime);
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        try {
            startActivityForResult(intent, SAVE_DOCUMENT);
        } catch (Exception error) {
            pendingSaveBytes = null;
            Toast.makeText(
                this,
                "No se pudo abrir Guardar archivo.",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private void printHtmlNative(String html) {
        if (html == null || html.isEmpty()) {
            Toast.makeText(this, "No hay contenido para imprimir.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (printWebView != null) {
            printWebView.destroy();
            printWebView = null;
        }

        printWebView = new WebView(this);
        configureWebView(printWebView);

        printWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager =
                    (PrintManager) getSystemService(PRINT_SERVICE);

                if (printManager == null) {
                    Toast.makeText(
                        MainActivity.this,
                        "La impresión no está disponible en este teléfono.",
                        Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                PrintDocumentAdapter adapter =
                    printWebView.createPrintDocumentAdapter("EMA-Pedido");

                PrintAttributes attributes = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();

                printManager.print("EMA · Pedido", adapter, attributes);
            }
        });

        printWebView.loadDataWithBaseURL(
            "file:///android_asset/ema/",
            html,
            "text/html",
            "UTF-8",
            null
        );
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void saveBase64(String filename, String mime, String base64) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                saveFileWithSystem(
                    filename == null || filename.isEmpty() ? "EMA-archivo" : filename,
                    mime,
                    data
                );
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(
                    MainActivity.this,
                    "No se pudo preparar el archivo.",
                    Toast.LENGTH_LONG
                ).show());
            }
        }

        @JavascriptInterface
        public void saveBlob(String filename, String mime, String base64) {
            saveBase64(filename, mime, base64);
        }

        @JavascriptInterface
        public void printHtml(String html) {
            runOnUiThread(() -> printHtmlNative(html));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER) {
            if (fileCallback == null) return;

            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }

            fileCallback.onReceiveValue(results);
            fileCallback = null;
            return;
        }

        if (requestCode == SAVE_DOCUMENT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingSaveBytes != null) {
                Uri uri = data.getData();

                try {
                    getContentResolver().openOutputStream(uri).use(output -> {
                        if (output == null) {
                            throw new IllegalStateException("No se pudo abrir el destino.");
                        }
                        output.write(pendingSaveBytes);
                        output.flush();
                    });

                    Toast.makeText(
                        this,
                        "Archivo guardado correctamente.",
                        Toast.LENGTH_SHORT
                    ).show();
                } catch (Exception error) {
                    Toast.makeText(
                        this,
                        "No se pudo guardar el archivo.",
                        Toast.LENGTH_LONG
                    ).show();
                }
            }

            pendingSaveBytes = null;
            pendingSaveMime = "application/octet-stream";
        }
    }

    @Override
    protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        if (printWebView != null) {
            printWebView.stopLoading();
            printWebView.destroy();
            printWebView = null;
        }

        pendingSaveBytes = null;
        super.onDestroy();
    }
}
