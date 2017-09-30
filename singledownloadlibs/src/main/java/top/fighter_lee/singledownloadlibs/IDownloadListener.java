package top.fighter_lee.singledownloadlibs;

import java.io.File;


public interface IDownloadListener {
    /**
     * 取消下载
     */
    void onCancel();

    /**
     * 下载失败
     */
    void onFail(int error);


    /**
     * 下载监听
     */
    void onProgress(int progress);


    /**
     * 下载完成
     */
    void onComplete(File file);

    /**
     * 下载准备
     */
    void onPrepare(long totalSize);
}

