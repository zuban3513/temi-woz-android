package com.cdi.temiwoz;

import androidx.appcompat.app.AppCompatActivity;
// import androidx.recyclerview.widget.OrientationHelper;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.net.http.SslError;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.SslErrorHandler;

import java.net.UnknownHostException;

/*
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
*/

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.listeners.*;

public class MainActivity extends AppCompatActivity implements OnRobotReadyListener {

    private Robot robot;
    private RobotApi robotApi;
    WebView interfaceView;
    TemiWebsocketServer server;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        interfaceView = new WebView(this);
        WebSettings webSettings = interfaceView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // SSLエラー(自己署名証明書)を無視してロード継続 + エラー時の状態をログ化
        interfaceView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                android.util.Log.d("TemiWoz", "SSL error ignored: " + error);
                handler.proceed();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                android.util.Log.e("TemiWoz", "WebView error: " + errorCode + " " + description + " url=" + failingUrl);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                android.util.Log.d("TemiWoz", "onPageStarted: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                android.util.Log.d("TemiWoz", "onPageFinished: " + url);
            }
        });



        setContentView(interfaceView);

        interfaceView.loadUrl("about:blank");

        robot = Robot.getInstance();
        robotApi = new RobotApi(robot);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Add listeners
        robot.addOnRobotReadyListener(this);
        robot.showTopBar();

        // Add WebSocket server
        int port = 8175;
        try {
            server = new TemiWebsocketServer( port );
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        server.addActivity(this);
        server.addRobotApi(robotApi);
        server.start();
    }

    @Override
    protected void onPause() {
        // Stop listeners
        robot.removeOnRobotReadyListener(this);

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop listeners
        robot.removeOnRobotReadyListener(this);
        // Stop WebSocket Server
        try {
            server.stop(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        robotApi.stop();
    }

    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                robot.onStart(activityInfo);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setInterfaceUrl(String url) {
        String current = interfaceView.getUrl();
        if (url.equals(current)) {
            // 同一URLでも確実に再読込(エラー画面からの回復)
            interfaceView.reload();
        } else {
            interfaceView.loadUrl(url);
        }
    }
}