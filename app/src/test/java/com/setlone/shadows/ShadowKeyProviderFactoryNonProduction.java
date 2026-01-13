package com.setlone.shadows;


import com.setlone.app.di.mock.KeyProviderMockNonProductionImpl;
import com.setlone.app.repository.KeyProvider;
import com.setlone.app.repository.KeyProviderFactory;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(KeyProviderFactory.class)
public class ShadowKeyProviderFactoryNonProduction
{
    @Implementation
    public static KeyProvider get() {
        return new KeyProviderMockNonProductionImpl();
    }
}
