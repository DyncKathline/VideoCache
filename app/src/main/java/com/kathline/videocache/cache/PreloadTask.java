package com.kathline.videocache.cache;

import com.danikula.videocache.HttpProxyCache;
import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.HttpProxyCacheServerClients;
import com.danikula.videocache.Logger;
import com.danikula.videocache.ProxyCacheException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;

public class PreloadTask implements Runnable {

    /**
     * 原始地址
     */
    public String mRawUrl;

    /**
     * 缓存文件大小的百分比
     */
    public int mPercentsPreLoad;

    /**
     * VideoCache服务器
     */
    public HttpProxyCacheServer mCacheServer;

    /**
     * 是否被取消
     */
    private boolean mIsCanceled;

    /**
     * 是否正在预加载
     */
    private boolean mIsExecuted;

    public static abstract class OnListener {
        void onStart() {}

        abstract void onSuccess();

        void onFail(int code, String msg) {}

        void onFinish() {}

        void onCancel() {}
    }

    private OnListener onListener;

    public void setOnListener(OnListener listener) {
        onListener = listener;
    }

    @Override
    public void run() {
        if(onListener != null) {
            onListener.onStart();
        }
        if (!mIsCanceled) {
            start();
        }
        mIsExecuted = false;
        mIsCanceled = false;
    }

    /**
     * 开始预加载
     */
    private void start() {
        Logger.info("开始预加载：" + mRawUrl);
        try {
            HttpProxyCacheServerClients clients = mCacheServer.getClients(mRawUrl);
            final HttpProxyCache cacheProxy = clients.startProcessRequest();
            if (!cacheProxy.cache.isCompleted()) {
                long length = cacheProxy.source.length();
                long cacheLen = cacheProxy.cache.available();
//                Logger.debug("预加载文件大小" + length + " 本地缓存大小　" + cacheLen + "  " + (cacheLen < Math.abs(length) * (mPercentsPreLoad / 100.0)));
                if (cacheLen < Math.abs(length) * (mPercentsPreLoad / 100.0)) {
                    startLoad(Math.abs(length));
                }
            }
        } catch (ProxyCacheException e) {
            e.printStackTrace();
        }
    }

    public void startLoad(long totalLen) {
        int targetLen = (int) (totalLen * (mPercentsPreLoad / 100.0));
        HttpURLConnection connection = null;
        try {
            //获取HttpProxyCacheServer的代理地址
            String proxyUrl = mCacheServer.getProxyUrl(mRawUrl);
            URL url = new URL(proxyUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            connection.setRequestProperty("Range", String.format("bytes=%d-%d", 0, targetLen));
            InputStream in = new BufferedInputStream(connection.getInputStream());
            int length;
            int read = -1;
            byte[] bytes = new byte[8 * 1024];
            while ((length = in.read(bytes)) != -1) {
                read += length;
                //预加载完成或者取消预加载
//                Logger.debug("预下载客户端　total " + read+"        应该下载　"+targetLen +"   readCount " +length);
                if (mIsCanceled || read >= targetLen) {
                    Logger.info("结束预加载：" + mRawUrl);
                    if(onListener != null) {
                        onListener.onSuccess();
                    }
                    break;
                }
            }
            if (read == -1) { //这种情况一般是预加载出错了，删掉缓存
                Logger.info("预加载失败：" + mRawUrl);
                File cacheFile = mCacheServer.getCacheFile(mRawUrl);
                if (cacheFile.exists()) {
                    cacheFile.delete();
                }
                if(onListener != null) {
                    onListener.onFail(-1, mRawUrl);
                }
            }
        } catch (Exception e) {
            Logger.info("异常结束预加载：" + mRawUrl);
            if(onListener != null) {
                onListener.onFail(-2, mRawUrl);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if(onListener != null) {
                onListener.onFinish();
            }
        }
    }

    /**
     * 将预加载任务提交到线程池，准备执行
     */
    public void executeOn(ExecutorService executorService) {
        if (mIsExecuted) return;
        mIsExecuted = true;
        executorService.submit(this);
    }

    /**
     * 取消预加载任务
     */
    public void cancel() {
        if (mIsExecuted) {
            mIsCanceled = true;
        }
        if(onListener != null) {
            onListener.onCancel();
        }
    }
}
