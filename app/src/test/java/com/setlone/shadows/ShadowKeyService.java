package com.setlone.shadows;

import android.content.Context;

import com.setlone.app.entity.AnalyticsProperties;
import com.setlone.app.service.AnalyticsServiceType;
import com.setlone.app.service.KeyService;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(KeyService.class)
public class ShadowKeyService
{
    @Implementation
    public void __constructor__(Context ctx, AnalyticsServiceType<AnalyticsProperties> analyticsService) {

    }
}
