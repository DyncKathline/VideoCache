package com.kathline.videocache.cache;

import android.content.Context;

import com.danikula.videocache.HttpProxyCache;
import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.HttpProxyCacheServerClients;
import com.danikula.videocache.Logger;
import com.danikula.videocache.ProxyCacheException;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 抖音预加载工具，使用AndroidVideoCache实现
 */
public class PreloadManager {

    private static PreloadManager sPreloadManager;

    /**
     * 单线程池，按照添加顺序依次执行{@link PreloadTask}
     */
    private ExecutorService mExecutorService = Executors.newScheduledThreadPool(2);

    /**
     * 保存正在预加载的{@link PreloadTask}
     */
    private LinkedHashMap<String, PreloadTask> mPreloadTasks = new LinkedHashMap<>();

    /**
     * 标识是否需要预加载
     */
    private boolean mIsStartPreload = true;

    private HttpProxyCacheServer mHttpProxyCacheServer;

    /**
     * 预加载的大小，每个视频预加载20%，这个参数可根据实际情况调整
     */
    public static final int PRELOAD_LENGTH = 20;

    private PreloadManager(Context context) {
        mHttpProxyCacheServer = ProxyVideoCacheManager.getProxy(context);
    }

    public static PreloadManager getInstance(Context context) {
        if (sPreloadManager == null) {
            synchronized (PreloadManager.class) {
                if (sPreloadManager == null) {
                    sPreloadManager = new PreloadManager(context.getApplicationContext());
                }
            }
        }
        return sPreloadManager;
    }

    /**
     * 开始预加载
     *
     * @param rawUrl 原始视频地址
     */
    public void addPreloadTask(String rawUrl, int percentsPreLoad) {
        if (isPreloaded(rawUrl, percentsPreLoad)) return;
        final PreloadTask task = new PreloadTask();
        task.mRawUrl = rawUrl;
        task.mPercentsPreLoad = percentsPreLoad;
        task.mCacheServer = mHttpProxyCacheServer;
        Logger.info("addPreloadTask: " + percentsPreLoad);
        mPreloadTasks.put(rawUrl, task);

        if (mIsStartPreload) {
            //开始预加载
            task.executeOn(mExecutorService);
            task.setOnListener(new PreloadTask.OnListener() {
                @Override
                void onSuccess() {

                }

                @Override
                void onFinish() {
                    super.onFinish();
                    PreloadTask preloadTask = mPreloadTasks.get(task.mRawUrl);
                    if (preloadTask != null) {
                        mPreloadTasks.remove(preloadTask.mRawUrl);
                    }
                    mIsStartPreload = true;
                    synchronized (this) {
                        for (Map.Entry<String, PreloadTask> next : mPreloadTasks.entrySet()) {
                            PreloadTask task = next.getValue();
                            task.executeOn(mExecutorService);
                            break;
                        }
                    }
                }
            });
        }
    }

    /**
     * 判断该播放地址是否已经预加载
     */
    private boolean isPreloaded(String rawUrl, int percentsPreLoad) {
        //先判断是否有缓存文件，如果已经存在缓存文件，并且其大小大于1KB，则表示已经预加载完成了
        File cacheFile = mHttpProxyCacheServer.getCacheFile(rawUrl);
        if (cacheFile.exists()) {
            if (cacheFile.length() >= 1024) {
                return true;
            } else {
                //这种情况一般是缓存出错，把缓存删掉，重新缓存
                cacheFile.delete();
                return false;
            }
        }
        //再判断是否有临时缓存文件，如果已经存在临时缓存文件，并且临时缓存文件超过了预加载大小，则表示已经预加载完成了
        File tempCacheFile = mHttpProxyCacheServer.getTempCacheFile(rawUrl);
        if (tempCacheFile.exists()) {
            try {
                HttpProxyCacheServerClients clients = mHttpProxyCacheServer.getClients(rawUrl);
                final HttpProxyCache cacheProxy = clients.startProcessRequest();
                long length = cacheProxy.source.length();
                return tempCacheFile.length() >= Math.abs(length) * (percentsPreLoad / 100.0);
            } catch (ProxyCacheException e) {
                e.printStackTrace();
            }
            return false;
        }

        return false;
    }

    /**
     * 暂停预加载
     *
     * @param rawUrl 缓存视频地址
     */
    public void pausePreload(String rawUrl) {
        Logger.debug("pausePreload：" + rawUrl);
//        mIsStartPreload = false;
        for (Map.Entry<String, PreloadTask> next : mPreloadTasks.entrySet()) {
            PreloadTask task = next.getValue();

            if (task.mRawUrl.equals(rawUrl)) {
                task.cancel();
            }
        }
    }

    /**
     * 恢复预加载
     *
     * @param rawUrl    缓存视频地址
     */
    public void resumePreload(String rawUrl) {
        Logger.debug("resumePreload：" + rawUrl);
//        mIsStartPreload = true;
        for (Map.Entry<String, PreloadTask> next : mPreloadTasks.entrySet()) {
            PreloadTask task = next.getValue();

            if (task.mRawUrl.equals(rawUrl)) {
                task.cancel();
            }
        }
    }

    /**
     * 通过原始地址取消预加载
     *
     * @param rawUrl 原始地址
     */
    public void removePreloadTask(String rawUrl) {
        PreloadTask task = mPreloadTasks.get(rawUrl);
        if (task != null) {
            task.cancel();
            mPreloadTasks.remove(rawUrl);
        }
    }

    /**
     * 取消所有的预加载
     */
    public void removeAllPreloadTask() {
        Iterator<Map.Entry<String, PreloadTask>> iterator = mPreloadTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PreloadTask> next = iterator.next();
            PreloadTask task = next.getValue();
            task.cancel();
            iterator.remove();
        }
    }

    /**
     * 获取播放地址
     */
    public String getPlayUrl(String rawUrl) {
        PreloadTask task = mPreloadTasks.get(rawUrl);
        if (task != null) {
            task.cancel();
            if (isPreloaded(rawUrl, task.mPercentsPreLoad)) {
                return mHttpProxyCacheServer.getProxyUrl(rawUrl);
            } else {
                return rawUrl;
            }
        }
        return rawUrl;
    }
}
