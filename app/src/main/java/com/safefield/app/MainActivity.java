package com.safefield.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int CAMERA_PERMISSION_REQUEST = 2001;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new SafeFieldBridge(), "SafeFieldAndroid");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) MainActivity.this.filePathCallback.onReceiveValue(null);
                MainActivity.this.filePathCallback = filePathCallback;
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    try {
                        File photoFile = createImageFile();
                        cameraImageUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", photoFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (IOException e) { cameraIntent = null; }
                } else cameraIntent = null;
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
                galleryIntent.setType("image/*");
                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, galleryIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Selecionar ou tirar foto");
                if (cameraIntent != null) chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
                startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST);
                return true;
            }
        });
        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    public class SafeFieldBridge {
        @JavascriptInterface
        public void printCurrentPage() {
            runOnUiThread(new Runnable() { public void run() {
                if (webView == null) return;
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                if (printManager == null) return;
                String jobName = "SafeField_PT_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
                PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);
                PrintAttributes attributes = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setColorMode(PrintAttributes.COLOR_MODE_COLOR).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();
                printManager.print(jobName, adapter, attributes);
            }});
        }

        @JavascriptInterface
        public void sharePdfFromHtml(final String html, final String fileName) {
            runOnUiThread(new Runnable() { public void run() {
                Toast.makeText(MainActivity.this, "Gerando PDF...", Toast.LENGTH_SHORT).show();
                createPdfAndShare(html, fileName);
            }});
        }

        @JavascriptInterface
        public void shareBase64Pdf(final String base64Pdf, final String fileName) {
            runOnUiThread(new Runnable() { public void run() {
                try {
                    String cleanName = sanitizeFileName(fileName == null || fileName.trim().isEmpty() ? "SafeField_PT" : fileName);
                    File pdfFile = new File(getCacheDir(), cleanName + ".pdf");
                    byte[] data = Base64.decode(base64Pdf, Base64.DEFAULT);
                    FileOutputStream out = new FileOutputStream(pdfFile);
                    out.write(data);
                    out.close();
                    sharePdfFile(pdfFile);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Erro ao compartilhar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }});
        }
    }

    private void createPdfAndShare(String html, String rawFileName) {
        final String cleanName = sanitizeFileName(rawFileName == null || rawFileName.trim().isEmpty() ? "SafeField_PT" : rawFileName);
        final File pdfFile = new File(getCacheDir(), cleanName + ".pdf");
        final WebView pdfWebView = new WebView(this);
        WebSettings settings = pdfWebView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        pdfWebView.setBackgroundColor(Color.WHITE);
        pdfWebView.setVisibility(View.INVISIBLE);
        addContentView(pdfWebView, new ViewGroup.LayoutParams(1240, 1754));
        pdfWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(final WebView view, String url) {
                view.postDelayed(new Runnable() { public void run() {
                    try {
                        writeWebViewToPdf(view, pdfFile);
                        try { ViewGroup parent = (ViewGroup) view.getParent(); if (parent != null) parent.removeView(view); } catch (Exception ignored) {}
                        sharePdfFile(pdfFile);
                    } catch (Exception e) { Toast.makeText(MainActivity.this, "Erro ao gerar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
                }}, 1800);
            }
        });
        pdfWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
    }

    private void writeWebViewToPdf(WebView view, File pdfFile) throws IOException {
        int pageWidth = 1240;
        int pageHeight = 1754;
        int contentHeight = (int) (view.getContentHeight() * view.getScale());
        if (contentHeight < pageHeight) contentHeight = pageHeight;
        view.measure(View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, pageWidth, pageHeight);
        PdfDocument document = new PdfDocument();
        FileOutputStream out = null;
        try {
            int pageNumber = 1;
            for (int top = 0; top < contentHeight; top += pageHeight) {
                if (pageNumber > 20) break;
                view.scrollTo(0, top);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);
                view.draw(canvas);
                document.finishPage(page);
                pageNumber++;
            }
            out = new FileOutputStream(pdfFile);
            document.writeTo(out);
        } finally {
            view.scrollTo(0, 0);
            if (out != null) out.close();
            document.close();
        }
    }

    private String sanitizeFileName(String name) { return name.replaceAll("[^a-zA-Z0-9_\\-]", "_"); }
    private void sharePdfFile(File pdfFile) {
        if (!pdfFile.exists() || pdfFile.length() == 0) { Toast.makeText(this, "PDF não foi gerado.", Toast.LENGTH_LONG).show(); return; }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Compartilhar PDF da PT"));
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return File.createTempFile("SF_PHOTO_" + timeStamp + "_", ".jpg", getExternalCacheDir());
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                if (data == null || data.getData() == null) { if (cameraImageUri != null) results = new Uri[]{cameraImageUri}; }
                else results = new Uri[]{data.getData()};
            }
            if (filePathCallback != null) { filePathCallback.onReceiveValue(results); filePathCallback = null; }
            cameraImageUri = null;
        }
    }
    @Override
    public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
