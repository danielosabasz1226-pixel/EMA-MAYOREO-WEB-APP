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
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER = 4101;
    private static final int SAVE_DOCUMENT = 4102;
    private static final String HOME_URL = "file:///android_asset/ema/index.html";

    private WebView webView;
    private WebView printWebView;
    private ValueCallback<Uri[]> fileCallback;

    private byte[] pendingSaveBytes;
    private String pendingSaveMime = "application/octet-stream";

    private LinearLayout bottomBar;
    private TextView screenTitle;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(7, 54, 111));
        getWindow().setNavigationBarColor(Color.WHITE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244, 247, 251));

        LinearLayout appBar = new LinearLayout(this);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setPadding(dp(12), dp(4), dp(8), dp(4));
        appBar.setBackgroundColor(Color.rgb(11, 78, 162));

        TextView logo = new TextView(this);
        logo.setText("EMA");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(20);
        logo.setTypeface(null, android.graphics.Typeface.BOLD);

        screenTitle = new TextView(this);
        screenTitle.setText("Inicio");
        screenTitle.setTextColor(Color.WHITE);
        screenTitle.setTextSize(15);
        screenTitle.setGravity(Gravity.CENTER_VERTICAL);

        Button refresh = headerButton("↻");
        Button back = headerButton("‹");

        appBar.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(50)));
        appBar.addView(screenTitle, new LinearLayout.LayoutParams(0, dp(50), 1f));
        appBar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(50)));
        appBar.addView(refresh, new LinearLayout.LayoutParams(dp(48), dp(50)));

        root.addView(appBar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(58)
        ));

        webView = new WebView(this);
        configureWebView(webView);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        root.addView(webView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        bottomBar = new LinearLayout(this);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(dp(6), dp(5), dp(6), dp(5));
        bottomBar.setBackgroundColor(Color.WHITE);
        root.addView(bottomBar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(72)
        ));

        setContentView(root);

        addNav("⌂", "Inicio", false, v -> home());
        addNav("+", "Pedido", true, v -> newOrder());
        addNav("▣", "Excel", false, v -> importExcel());
        addNav("◷", "Historial", false, v -> history());
        addNav("⋯", "Más", false, v -> moreMenu());

        refresh.setOnClickListener(v -> reloadFresh());
        back.setOnClickListener(v -> handleBack());

        loadHome();
    }

    private Button headerButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(24);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(0, 0, 0, 0);
        b.setBackgroundColor(Color.TRANSPARENT);
        return b;
    }

    private void addNav(String icon, String label, boolean primary, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setText(icon + "\n" + label);
        b.setTextSize(11);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(primary ? Color.WHITE : Color.rgb(97, 112, 138));
        b.setPadding(0, dp(4), 0, dp(4));
        b.setBackgroundColor(primary ? Color.rgb(11, 78, 162) : Color.TRANSPARENT);
        b.setOnClickListener(listener);
        bottomBar.addView(b, new LinearLayout.LayoutParams(0, dp(60), 1f));
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

        view.setBackgroundColor(Color.TRANSPARENT);
        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        view.setVerticalScrollBarEnabled(false);
        view.setHorizontalScrollBarEnabled(false);

        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(
                    "(function(){" +
                    "document.documentElement.classList.add('ema-android');" +
                    "if(document.body) document.body.classList.add('ema-android');" +
                    "document.querySelectorAll('.cod').forEach(function(e){e.remove();});" +
                    "})();",
                    null
                );
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request == null || request.isForMainFrame()) {
                    Toast.makeText(
                        MainActivity.this,
                        "No se pudo cargar EMA. Pulsa ↻ para reintentar.",
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
                    "application/octet-stream"
                });
                intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                );

                try {
                    startActivityForResult(intent, FILE_CHOOSER);
                } catch (Exception error) {
                    fileCallback = null;
                    Toast.makeText(
                        MainActivity.this,
                        "No se pudo abrir Excel.",
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
                        "Usa Guardar desde EMA.",
                        Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    private void js(String code) {
        if (webView == null) return;
        webView.evaluateJavascript(
            "(function(){try{return (" + code + ");}catch(e){return false;}})();",
            null
        );
    }

    private void setTitle(String value) {
        if (screenTitle != null) screenTitle.setText(value);
    }

    private void home() {
        setTitle("Inicio");
        js("window.EMAAndroid?.scrollTop()");
        webView.loadUrl(HOME_URL + "?v=android-1.4");
    }

    private void newOrder() {
        setTitle("Nuevo pedido");
        js("window.EMAAndroid?.newOrder()");
    }

    private void importExcel() {
        setTitle("Importar Excel");
        js("window.EMAAndroid?.openImport()");
    }

    private void history() {
        setTitle("Historial");
        js("window.EMAAndroid?.history()");
    }

    private void moreMenu() {
        final String[] options = {
            "Imprimir",
            "Clientes",
            "Vendedores",
            "Actualizar",
            "Inicio"
        };

        new android.app.AlertDialog.Builder(this)
            .setTitle("Más opciones")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        setTitle("Imprimir");
                        js("window.EMAAndroid?.print()");
                        break;
                    case 1:
                        setTitle("Clientes");
                        js("window.EMAAndroid?.customers()");
                        break;
                    case 2:
                        setTitle("Vendedores");
                        js("window.EMAAndroid?.sellers()");
                        break;
                    case 3:
                        reloadFresh();
                        break;
                    default:
                        home();
                        break;
                }
            })
            .show();
    }

    private void loadHome() {
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.loadUrl(HOME_URL + "?v=android-1.4");
    }

    private void reloadFresh() {
        webView.clearCache(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.loadUrl(HOME_URL + "?v=android-1.4");
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
                "No se pudo abrir Guardar.",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private void printHtmlNative(String html) {
        if (html == null || html.isEmpty()) return;

        if (printWebView != null) {
            printWebView.destroy();
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
                        "Impresión no disponible en este teléfono.",
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
                try {
                    java.io.OutputStream output = getContentResolver().openOutputStream(data.getData());
                    if (output == null) throw new IllegalStateException("destino nulo");
                    try {
                        output.write(pendingSaveBytes);
                        output.flush();
                    } finally {
                        output.close();
                    }

                    Toast.makeText(
                        this,
                        "Archivo guardado.",
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
