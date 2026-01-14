package com.pandulapeter.beagle.logOkHttp;

import okhttp3.Interceptor;
import okhttp3.Response;
import java.io.IOException;

/**
 * Stub implementation of BeagleOkHttpLogger to prevent NoClassDefFoundError
 * when WalletConnect tries to reference this class at runtime.
 * 
 * This is a workaround for WalletConnect's optional Beagle dependency.
 * Beagle is excluded from the build, so this stub provides a no-op implementation.
 */
public class BeagleOkHttpLogger implements Interceptor {
    
    /**
     * Singleton instance that WalletConnect expects to find.
     * This is accessed via BeagleOkHttpLogger.INSTANCE in WalletConnect's CoreNetworkModule.
     */
    public static final BeagleOkHttpLogger INSTANCE = new BeagleOkHttpLogger();
    
    private BeagleOkHttpLogger() {
        // Private constructor for singleton pattern
    }
    
    /**
     * Returns a logger instance that WalletConnect expects.
     * This is a stub method that returns null to disable logging.
     */
    public Object getLogger() {
        // Return null as a stub - WalletConnect should handle null gracefully
        return null;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        // No-op: just pass through the request without logging
        return chain.proceed(chain.request());
    }
}
