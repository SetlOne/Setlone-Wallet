package com.setlone.app.web3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.setlone.app.BuildConfig;
import com.setlone.app.entity.TransactionReturn;
import com.setlone.app.entity.URLLoadInterface;
import com.setlone.app.web3.entity.Address;
import com.setlone.app.web3.entity.WalletAddEthereumChainObject;
import com.setlone.app.web3.entity.Web3Call;
import com.setlone.app.web3.entity.Web3Transaction;
import com.setlone.token.entity.EthereumMessage;
import com.setlone.token.entity.EthereumTypedMessage;
import com.setlone.token.entity.Signable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class Web3View extends WebView {
    private static final String JS_PROTOCOL_CANCELLED = "cancelled";
    private static final String JS_PROTOCOL_ON_SUCCESSFUL = "SetlOne.executeCallback(%1$s, null, \"%2$s\")";
    private static final String JS_PROTOCOL_EXPR_ON_SUCCESSFUL = "SetlOne.executeCallback(%1$s, null, %2$s)";
    private static final String JS_PROTOCOL_ON_FAILURE = "SetlOne.executeCallback(%1$s, \"%2$s\", null)";
    private final Web3ViewClient webViewClient;
    @Nullable
    private OnSignTransactionListener onSignTransactionListener;
    private final OnSignTransactionListener innerOnSignTransactionListener = new OnSignTransactionListener() {
        @Override
        public void onSignTransaction(Web3Transaction transaction, String url)
        {
            if (onSignTransactionListener != null)
            {
                onSignTransactionListener.onSignTransaction(transaction, url);
            }
        }
    };
    @Nullable
    private OnSignMessageListener onSignMessageListener;
    private final OnSignMessageListener innerOnSignMessageListener = new OnSignMessageListener() {
        @Override
        public void onSignMessage(EthereumMessage message)
        {
            if (onSignMessageListener != null)
            {
                onSignMessageListener.onSignMessage(message);
            }
        }
    };
    @Nullable
    private OnSignPersonalMessageListener onSignPersonalMessageListener;
    private final OnSignPersonalMessageListener innerOnSignPersonalMessageListener = new OnSignPersonalMessageListener() {
        @Override
        public void onSignPersonalMessage(EthereumMessage message)
        {
            onSignPersonalMessageListener.onSignPersonalMessage(message);
        }
    };
    @Nullable
    private OnSignTypedMessageListener onSignTypedMessageListener;
    private final OnSignTypedMessageListener innerOnSignTypedMessageListener = new OnSignTypedMessageListener() {
        @Override
        public void onSignTypedMessage(EthereumTypedMessage message)
        {
            onSignTypedMessageListener.onSignTypedMessage(message);
        }
    };
    @Nullable
    private OnEthCallListener onEthCallListener;
    private final OnEthCallListener innerOnEthCallListener = new OnEthCallListener() {
        @Override
        public void onEthCall(Web3Call txData)
        {
            onEthCallListener.onEthCall(txData);
        }
    };
    @Nullable
    private OnWalletAddEthereumChainObjectListener onWalletAddEthereumChainObjectListener;
    private final OnWalletAddEthereumChainObjectListener innerAddChainListener = new OnWalletAddEthereumChainObjectListener() {
        @Override
        public void onWalletAddEthereumChainObject(long callbackId, WalletAddEthereumChainObject chainObject)
        {
            onWalletAddEthereumChainObjectListener.onWalletAddEthereumChainObject(callbackId, chainObject);
        }
    };
    @Nullable
    private OnWalletActionListener onWalletActionListener;
    private final OnWalletActionListener innerOnWalletActionListener = new OnWalletActionListener() {
        @Override
        public void onRequestAccounts(long callbackId)
        {
            onWalletActionListener.onRequestAccounts(callbackId);
        }

        @Override
        public void onWalletSwitchEthereumChain(long callbackId, WalletAddEthereumChainObject chainObj)
        {
            onWalletActionListener.onWalletSwitchEthereumChain(callbackId, chainObj);
        }
    };
    private URLLoadInterface loadInterface;

    public Web3View(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        webViewClient = new Web3ViewClient(getContext());
        init();
    }

    public Web3View(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        webViewClient = new Web3ViewClient(getContext());
        init();
    }

    @Override
    public void setWebChromeClient(WebChromeClient client)
    {
        super.setWebChromeClient(client);
    }

    @Override
    public void setWebViewClient(@NonNull WebViewClient client)
    {
        super.setWebViewClient(new WrapWebViewClient(webViewClient, client));
    }

    @Override
    public void loadUrl(@NonNull String url, @NonNull Map<String, String> additionalHttpHeaders)
    {
        super.loadUrl(url, additionalHttpHeaders);
    }

    @Override
    public void loadUrl(@NonNull String url)
    {
        loadUrl(url, getWeb3Headers());
    }

    /* Required for CORS requests */
    @NotNull
    @Contract(" -> new")
    private Map<String, String> getWeb3Headers()
    {
        //headers
        return new HashMap<String, String>() {{
            put("Connection", "close");
            put("Content-Type", "text/plain");
            put("Access-Control-Allow-Origin", "*");
            put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
            put("Access-Control-Max-Age", "600");
            put("Access-Control-Allow-Credentials", "true");
            put("Access-Control-Allow-Headers", "accept, authorization, Content-Type");
        }};
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void init()
    {
        WebSettings settings = getSettings();
        
        // 기본 JavaScript 및 DOM 설정
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        
        // 뷰포트 및 렌더링 최적화
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        
        // 캐시 및 네트워크 최적화
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // AppCache는 Android API 33+에서 완전히 제거됨 - 더 이상 사용 불가
        
        // 줌 컨트롤
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        
        // Mixed Content 허용 (DApp에서 필요할 수 있음)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
        {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        // 미디어 재생 최적화
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // 파일 접근 설정
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        // User Agent 설정
        settings.setUserAgentString(settings.getUserAgentString()
                + "SetlOne(Platform=Android&AppVersion=" + BuildConfig.VERSION_NAME + ")");
        
        // 디버깅 (개발용)
        WebView.setWebContentsDebuggingEnabled(true);
        
        setInitialScale(0);
        
        // 하드웨어 가속 레이어 타입 설정 - 성능 향상의 핵심
        // LAYER_TYPE_HARDWARE는 GPU 가속을 사용하여 애니메이션과 렌더링이 훨씬 부드러워집니다
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
        {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else
        {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        addJavascriptInterface(new SignCallbackJSInterface(
                this,
                innerOnSignTransactionListener,
                innerOnSignMessageListener,
                innerOnSignPersonalMessageListener,
                innerOnSignTypedMessageListener,
                innerOnEthCallListener,
                innerAddChainListener,
                innerOnWalletActionListener), "alpha");

        // 다크 모드 지원
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
        {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true);
        }
        
        // 추가 성능 최적화: 렌더링 우선순위 설정 (deprecated이지만 구형 기기 호환성)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)
        {
            try
            {
                // 구형 API에서만 사용 가능한 메서드
                java.lang.reflect.Method method = settings.getClass().getMethod("setRenderPriority", WebSettings.RenderPriority.class);
                method.invoke(settings, WebSettings.RenderPriority.HIGH);
            }
            catch (Exception e)
            {
                Timber.tag("WEB_VIEW").d("setRenderPriority not available: " + e.getMessage());
            }
        }
    }

    @Nullable
    public Address getWalletAddress()
    {
        return webViewClient.getJsInjectorClient().getWalletAddress();
    }

    public void setWalletAddress(@NonNull Address address)
    {
        webViewClient.getJsInjectorClient().setWalletAddress(address);
    }

    public long getChainId()
    {
        return webViewClient.getJsInjectorClient().getChainId();
    }

    public void setChainId(long chainId, boolean isTokenscript)
    {
        if (isTokenscript){
            webViewClient.getJsInjectorClient().setTSChainId(chainId);
        } else {
            webViewClient.getJsInjectorClient().setChainId(chainId);
        }
    }

    public void setWebLoadCallback(URLLoadInterface iFace)
    {
        loadInterface = iFace;
    }

    public void setOnSignTransactionListener(@Nullable OnSignTransactionListener onSignTransactionListener)
    {
        this.onSignTransactionListener = onSignTransactionListener;
    }

    public void setOnSignMessageListener(@Nullable OnSignMessageListener onSignMessageListener)
    {
        this.onSignMessageListener = onSignMessageListener;
    }

    public void setOnSignPersonalMessageListener(@Nullable OnSignPersonalMessageListener onSignPersonalMessageListener)
    {
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
    }

    public void setOnSignTypedMessageListener(@Nullable OnSignTypedMessageListener onSignTypedMessageListener)
    {
        this.onSignTypedMessageListener = onSignTypedMessageListener;
    }

    public void setOnEthCallListener(@Nullable OnEthCallListener onEthCallListener)
    {
        this.onEthCallListener = onEthCallListener;
    }

    public void setOnWalletAddEthereumChainObjectListener(@Nullable OnWalletAddEthereumChainObjectListener onWalletAddEthereumChainObjectListener)
    {
        this.onWalletAddEthereumChainObjectListener = onWalletAddEthereumChainObjectListener;
    }

    public void setOnWalletActionListener(@Nullable OnWalletActionListener onWalletActionListener)
    {
        this.onWalletActionListener = onWalletActionListener;
    }

    public void onSignTransactionSuccessful(TransactionReturn txData)
    {
        callbackToJS(txData.tx.leafPosition, JS_PROTOCOL_ON_SUCCESSFUL, txData.hash);
    }

    public void onSignMessageSuccessful(Signable message, String signHex)
    {
        long callbackId = message.getCallbackId();
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void onCallFunctionSuccessful(long callbackId, String result)
    {
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, result);
    }

    public void onCallFunctionError(long callbackId, String error)
    {
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, error);
    }

    public void onSignCancel(long callbackId)
    {
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, JS_PROTOCOL_CANCELLED);
    }

    private void callbackToJS(long callbackId, String function, String param)
    {
        String callback = String.format(function, callbackId, param);
        post(() -> evaluateJavascript(callback, value ->Timber.tag("WEB_VIEW").d(value)));
    }

    public void onWalletActionSuccessful(long callbackId, String expression)
    {
        String callback = String.format(JS_PROTOCOL_EXPR_ON_SUCCESSFUL, callbackId, expression);
        post(() -> evaluateJavascript(callback, Timber::d));
    }

    public void resetView()
    {
        webViewClient.resetInject();
    }

    private class WrapWebViewClient extends WebViewClient {
        private final Web3ViewClient internalClient;
        private final WebViewClient externalClient;
        private boolean loadingError = false;
        private boolean redirect = false;

        public WrapWebViewClient(Web3ViewClient internalClient, WebViewClient externalClient)
        {
            this.internalClient = internalClient;
            this.externalClient = externalClient;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            super.onPageStarted(view, url, favicon);
            clearCache(true);
            if (!redirect)
            {
                view.evaluateJavascript(internalClient.getProviderString(view), null);
                view.evaluateJavascript(internalClient.getInitString(view), null);
                internalClient.resetInject();
            }

            redirect = false;
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);

            if (!redirect && !loadingError)
            {
                if (loadInterface != null)
                {
                    loadInterface.onWebpageLoaded(url, view.getTitle());
                }
            }
            else if (!loadingError && loadInterface != null)
            {
                loadInterface.onWebpageLoadComplete();
            }

            redirect = false;
            loadingError = false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final Uri uri = request.getUrl();
            final String url = uri.toString();
            redirect = true;

            return externalClient.shouldOverrideUrlLoading(view, request)
                    || internalClient.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
        {
            loadingError = true;
            if (externalClient != null)
                externalClient.onReceivedError(view, request, error);
        }
    }
}
