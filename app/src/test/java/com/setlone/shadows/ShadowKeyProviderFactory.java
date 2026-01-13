package com.setlone.shadows;

import com.setlone.app.di.mock.KeyProviderMockImpl;
import com.setlone.app.repository.KeyProvider;
import com.setlone.app.repository.KeyProviderFactory;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(KeyProviderFactory.class)
public class ShadowKeyProviderFactory
{
    @Implementation
    public static KeyProvider get() {
        return new KeyProviderMockImpl();
    }
}
