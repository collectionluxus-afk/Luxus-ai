package com.luxus.ai;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.Intent;
import android.net.Uri;
import android.view.Window;
import android.webkit.WebChromeClient;

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
        AppBridge(Context c){ context=c; }
        @JavascriptInterface public void print(){
            runOnUiThread(() -> {
                try {
                    webView.setDrawingCacheEnabled(true);
                    webView.measure(
                        View.MeasureSpec.makeMeasureSpec(webView.getWidth(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    );
                    webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());
                    int pageWidth = 595;
                    int pageHeight = 842;
                    float scale = pageWidth / (float)Math.max(1, webView.getWidth());
                    int contentHeight = webView.getHeight();
                    int pageCount = Math.max(1, (int)Math.ceil(contentHeight * scale / pageHeight));
                    PdfDocument pdf = new PdfDocument();
                    for (int i = 0; i < pageCount; i++) {
                        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create();
                        PdfDocument.Page page = pdf.startPage(info);
                        Canvas canvas = page.getCanvas();
                        canvas.drawColor(Color.WHITE);
                        canvas.save();
                        canvas.scale(scale, scale);
                        canvas.translate(0, -i * pageHeight / scale);
                        webView.draw(canvas);
                        canvas.restore();
                        pdf.finishPage(page);
                    }
                    File dir;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Luxus AI");
                    } else {
                        dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Luxus AI");
                    }
                    if (!dir.exists() && !dir.mkdirs()) throw new IOException("Could not create PDF folder");
                    File file = new File(dir, "Luxus_AI_Quotation_" + System.currentTimeMillis() + ".pdf");
                    FileOutputStream out = new FileOutputStream(file);
                    pdf.writeTo(out);
                    out.close();
                    pdf.close();
                    Toast.makeText(MainActivity.this, "PDF saved successfully", Toast.LENGTH_LONG).show();
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("application/pdf");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    try { startActivity(Intent.createChooser(share, "Share quotation PDF")); } catch(Exception ignored) {}
                } catch(Exception e) {
                    Toast.makeText(MainActivity.this, "PDF save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
        @JavascriptInterface public void openWhatsApp(String url){
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch(Exception ignored){}
        }
    }
    @Override public void onBackPressed(){ if(webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
