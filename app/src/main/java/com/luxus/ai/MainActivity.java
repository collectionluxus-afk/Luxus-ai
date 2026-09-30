package com.luxus.ai;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PageRange;
import android.print.PrintDocumentInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.os.Environment;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends Activity {
    WebView webView;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AppBridge(this), "Android");
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class AppBridge {
        private final Context context;
        AppBridge(Context c) { context = c; }

        @JavascriptInterface
        public void print() {
            runOnUiThread(() -> {
                webView.postDelayed(() -> saveRealPdf(), 300);
            });
        }

        private void saveRealPdf() {
            final String fileName = "Luxus_AI_Quotation_" + System.currentTimeMillis() + ".pdf";
            final PrintAttributes attributes = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(new PrintAttributes.Resolution(
                            "luxus_pdf", "Luxus PDF", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();

            final PrintDocumentAdapter adapter =
                    webView.createPrintDocumentAdapter("Luxus_AI_Quotation");

            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    Toast.makeText(MainActivity.this,
                            "Android 10+ required for direct PDF save",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/Luxus AI");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri uri = getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("Cannot create PDF file");

                adapter.onLayout(
                        null,
                        attributes,
                        new CancellationSignal(),
                        new PrintDocumentAdapter.LayoutResultCallback() {
                            @Override public void onLayoutFinished(
                                    PrintDocumentInfo info, boolean changed) {
                                try {
                                    ParcelFileDescriptor pfd =
                                            getContentResolver().openFileDescriptor(uri, "w");
                                    if (pfd == null) throw new IOException("Cannot open PDF file");

                                    adapter.onWrite(
                                            new PageRange[]{PageRange.ALL_PAGES},
                                            pfd,
                                            new CancellationSignal(),
                                            new PrintDocumentAdapter.WriteResultCallback() {
                                                @Override public void onWriteFinished(PageRange[] pages) {
                                                    try { pfd.close(); } catch (Exception ignored) {}
                                                    ContentValues done = new ContentValues();
                                                    done.put(MediaStore.Downloads.IS_PENDING, 0);
                                                    getContentResolver().update(uri, done, null, null);
                                                    Toast.makeText(MainActivity.this,
                                                            "PDF saved: Downloads/Luxus AI",
                                                            Toast.LENGTH_LONG).show();

                                                    Intent view = new Intent(Intent.ACTION_VIEW);
                                                    view.setDataAndType(uri, "application/pdf");
                                                    view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                    try { startActivity(view); } catch (Exception ignored) {}
                                                }

                                                @Override public void onWriteFailed(CharSequence error) {
                                                    try { pfd.close(); } catch (Exception ignored) {}
                                                    getContentResolver().delete(uri, null, null);
                                                    Toast.makeText(MainActivity.this,
                                                            "PDF save failed: " + error,
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                } catch (Exception e) {
                                    getContentResolver().delete(uri, null, null);
                                    Toast.makeText(MainActivity.this,
                                            "PDF write failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override public void onLayoutFailed(CharSequence error) {
                                getContentResolver().delete(uri, null, null);
                                Toast.makeText(MainActivity.this,
                                        "PDF layout failed: " + error,
                                        Toast.LENGTH_LONG).show();
                            }
                        },
                        null
                );
            } catch (Exception e) {
                Toast.makeText(MainActivity.this,
                        "PDF failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        @JavascriptInterface
        public void openWhatsApp(String url) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (Exception ignored) {}
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
