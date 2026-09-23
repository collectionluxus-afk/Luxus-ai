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
            PrintManager pm=(PrintManager)getSystemService(Context.PRINT_SERVICE);
            PrintDocumentAdapter adapter=webView.createPrintDocumentAdapter("Luxus_AI_Quotation");
            pm.print("Luxus AI Quotation", adapter, new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build());
        }
        @JavascriptInterface public void openWhatsApp(String url){
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch(Exception ignored){}
        }
    }
    @Override public void onBackPressed(){ if(webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
