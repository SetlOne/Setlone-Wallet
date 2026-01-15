package com.setlone.app.entity;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;

import org.jetbrains.annotations.NotNull;

/**
 * Created by JB on 24/10/2021.
 * 성능 최적화: 메모리 캐시 및 디스크 캐시 크기 조정
 */
@GlideModule
public class SetlOneGlideModule extends AppGlideModule
{
    // 메모리 캐시 크기: 50MB (기본값은 25MB)
    private static final int MEMORY_CACHE_SIZE = 50 * 1024 * 1024;
    // 디스크 캐시 크기: 250MB (기본값은 250MB)
    private static final int DISK_CACHE_SIZE = 250 * 1024 * 1024;

    @Override
    public void applyOptions(@NotNull Context context, GlideBuilder builder) {
        builder.setLogLevel(Log.ERROR);
        
        // 메모리 캐시 최적화: 더 많은 이미지를 메모리에 캐시하여 로딩 속도 향상
        builder.setMemoryCache(new LruResourceCache(MEMORY_CACHE_SIZE));
        
        // 디스크 캐시 최적화: 네트워크 요청 감소로 성능 향상
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, DISK_CACHE_SIZE));
    }
}
