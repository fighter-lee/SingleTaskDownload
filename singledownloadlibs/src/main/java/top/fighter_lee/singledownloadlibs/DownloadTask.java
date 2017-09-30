package top.fighter_lee.singledownloadlibs;


import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


/**
 * Created by lyb on 2016/03/22.
 * description  ${下载任务管理类}
 */

public class DownloadTask {
    public static final int CONNECT_TIMEOUT = 30 * 1000; //请求网络超时时间
    private static final String TAG = "DownloadTask";
    public static int runningCount;
    public static int threadNum;
    private String download_url;
    private String md5;
    private File download_path;
    private IDownloadListener mIDownloadListener;
    private File downFile;
    private long blockSize;
    private boolean newTask = false;
    public static boolean netError = false;
    private Context mContext;
    private long downloadSize;
    private static boolean shouldPrint = false;
    private static final long CHECK_TIME = (long) (1000 * 60 * 1); //下载过程中，抛出异常的间隔时间
    private long fileSize;

    //设置下载url
    public DownloadTask setDownloadUrl(String url) {
        download_url = url;
        Log.d(TAG, download_url);
        return this;
    }

    //设置下载文件
    public DownloadTask setDownloadFile(File downFile) {
        this.downFile = downFile;
        this.download_path = downFile.getParentFile();
        return this;
    }

    //设置Context
    public DownloadTask setContext(Context context) {
        this.mContext = context;
        return this;
    }

    //设置下载文件大小
    public DownloadTask setFileMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    //设置下载监听
    public DownloadTask setDownloadListener(IDownloadListener listener) {
        mIDownloadListener = listener;
        return this;
    }

    //开始执行下载任务
    public void execute() {

        new DownTask().execute();

    }

    private class DownTask extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "downloadFile() start.");
            DownConfig.isCancel = false;
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            if (null == download_path) {
                Log.d(TAG, "download_task() path is invalid");
                onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.DOWNLOADING_PACKAGE_IS_INVALID);
                return false;
            }

            Log.d(TAG, "downloadFile() url = " + download_url + " downFolder = " + download_path.getAbsolutePath() + " fileName = " + downFile.getName());

            File parentFile = new File(download_path.getAbsolutePath());
            boolean isUserful = FileUtil.createOrExistsDir(parentFile.getAbsolutePath());
            if (!isUserful) {
                //路径不可用
                Log.d(TAG, "download_task() path is invalid");
                onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.DOWNLOADING_PACKAGE_IS_INVALID);
                return false;
            }

            //判断是否新任务
            if (!TextUtils.isEmpty(md5) && downFile.exists() && FileUtil.validateFile(downFile.getAbsolutePath().toString(), md5)) {
                Log.d(TAG, "execute() download file is exit & md5 varify success!");
                //文件存在且完整
                newTask = false;
                return true;
            } else if (Utils.isNewtask(mContext)) {
                Log.d(TAG, "execute() is new task,download file will delet.");
                downFile.delete();
                newTask = true;
            } else {
                Log.d(TAG, "execute() need to download from breakpoint!");
                newTask = false;
            }

            //cup核心数+1
            threadNum = Utils.getNumCores() + 1;
            Log.d(TAG, "threadNum:" + threadNum);
            netError = false;
            FileDownloadThread[] threads = new FileDownloadThread[threadNum];
            try {

                URL url = new URL(download_url);
                Log.d(TAG, "download file http path:" + download_url);
                HttpURLConnection conn;
                if (download_url.startsWith("https")) {
                    conn = (HttpsURLConnection) url.openConnection();
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setRequestMethod("GET");
                int statusCode = conn.getResponseCode();
                if (statusCode == HttpURLConnection.HTTP_OK ||
                        statusCode == HttpURLConnection.HTTP_PARTIAL) {
                    // 读取下载文件总大小
                    fileSize = conn.getContentLength();
                    if (fileSize <= 0) {
                        onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.DOWNLOADING_LINK_ERROR);
                        return false;
                    }
                    onProgressUpdate(Constant.DOWNLOAD_PREPARE);
                    runningCount = threadNum;

                    // 计算每条线程下载的数据长度
                    blockSize = (fileSize % threadNum) == 0 ? fileSize / threadNum
                            : fileSize / threadNum + 1;
                    Log.d(TAG, "fileSize:" + fileSize + "  blockSize:" + blockSize);

                    for (int i = 0; i < threads.length; i++) {
                        // 启动线程，分别下载每个线程需要下载的部分
                        threads[i] = new FileDownloadThread(url, downFile, blockSize,
                                (i + 1), mContext);
                        threads[i].setName("Thread:" + i);
                        threads[i].start();
                    }
                    long old_time = System.currentTimeMillis();
                    boolean isfinished = false;
                    boolean download_overtime = false;
                    long downloadedAllSize = 0;

                    downloadPrintStart();

                    while (!isfinished) {
                        isfinished = true;
                        // 当前所有线程下载总量
                        downloadedAllSize = 0;
                        for (int i = 0; i < threads.length; i++) {
                            downloadedAllSize += threads[i].getDownloadLength();
                            if (!threads[i].isCompleted()) {
                                isfinished = false;
                            }
                        }
                        if (!DownConfig.isCancel) {
                            Log.d(TAG, "doInBackground: " + downloadedAllSize + "," + fileSize);
                            onProgressUpdate(Constant.DOWNLOAD_PROGRESS, (int) ((downloadedAllSize * 100) / fileSize));
                        }

                        //下载超时
                        if (fileNotChange(downloadedAllSize)) {
                            long current_time = System.currentTimeMillis();
                            if (current_time - old_time >= CHECK_TIME) {
                                download_overtime = true;
                                isfinished = true;
                            }
                        } else {
                            old_time = System.currentTimeMillis();
                        }
                        SystemClock.sleep(500);

                    }
                    if (DownConfig.isCancel) {
                        onProgressUpdate(Constant.DOWNLOAD_CANCEL);
                        return false;
                    }
                    if (download_overtime) {
                        Log.d(TAG, "execute() download overtime!");
                        onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.DOWNLOADING_TIME_OUT);
                        return false;
                    }
                    if (netError) {
                        Log.d(TAG, "execute() .downloading_net_exception");
                        onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.DOWNLOADING_NET_EXCEPTION);
                        return false;
                    }
                    if (!downFile.exists()) {
                        Log.d(TAG, "onFail(),error_code:" + Error.DOWN_FILE_NOT_EXIST + ",message:download file not exist!");
                        onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.DOWN_FILE_NOT_EXIST);
                        return false;
                    }
                    Log.d(TAG, " all of downloadSize:" + downloadedAllSize);
                    if (isfinished) {
                        if (!TextUtils.isEmpty(md5)) {
                            if (FileUtil.validateFile(downFile.getAbsolutePath().toString(), md5)) {
                                int size = Utils.getCacheDownFile(mContext).size();
                                Log.d(TAG, "本地标识文件size:" + size);
                                if (size > 0) {
                                    boolean b = Utils.deletCacheDownFile(mContext);
                                    Log.d(TAG, "删除标识文件:" + b);
                                }
                                onProgressUpdate(Constant.DOWNLOAD_COMPLETED);
                                return true;
                            } else {
                                Log.d(TAG, "onFail(),error_code:" + Error.DOWNLOADING_INVALID_FILE + ",message:download finished. but the file is invalid.");
                                Utils.deletUpgradeFile(mContext, downFile.getAbsolutePath());
                                onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.DOWNLOADING_INVALID_FILE);
                                return false;
                            }
                        } else {
                            int size = Utils.getCacheDownFile(mContext).size();
                            Log.d(TAG, "本地标识文件size:" + size);
                            if (size > 0) {
                                boolean b = Utils.deletCacheDownFile(mContext);
                                Log.d(TAG, "删除标识文件:" + b);
                            }
                            onProgressUpdate(Constant.DOWNLOAD_COMPLETED);
                            return true;
                        }
                    }
                } else {
                    Log.e(TAG, "downloadFile() statusCode = " + statusCode);
                    onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.SERVER_RESPONSE);
                    return false;
                }
            } catch (Exception e) {
                Log.d(TAG, "onFail(),error_code:" + Error.ERROR + ",message:" + e.toString());
                onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.ERROR);
                return false;
            }
            onProgressUpdate(Constant.DOWNLOAD_FAILED, Error.ERROR);
            return false;

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (null == mIDownloadListener) {
                return;
            }
            if (values[0] == Constant.DOWNLOAD_PREPARE) {
                mIDownloadListener.onPrepare(fileSize);

            } else if (values[0] == Constant.DOWNLOAD_CANCEL) {
                mIDownloadListener.onCancel();

            } else if (values[0] == Constant.DOWNLOAD_PROGRESS) {
                long progress = values[1];
                mIDownloadListener.onProgress((int) progress);

            } else if (values[0] == Constant.DOWNLOAD_FAILED) {
                long error_code = values[1];
                mIDownloadListener.onFail((int) error_code);

            }
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            downloadPrintEnd();
            super.onPostExecute(aBoolean);
        }
    }


    //文件没有改变
    private boolean fileNotChange(long downloadedAllSize) {
        if (downloadedAllSize <= downloadSize) {
            return true;
        }
        this.downloadSize = downloadedAllSize;
        return false;
    }

    //日志隔一秒打印一次，避免过多打印不用的数据
    private void downloadPrintStart() {
        shouldPrint = true;
        Runnable printRun = new Runnable() {
            @Override
            public void run() {
                while (shouldPrint) {
                    Log.d(TAG, "downloadPrint() download size:" + downloadSize + ",total size:" + fileSize + ",已完成：" + (downloadSize * 100) / fileSize + "%");
                    SystemClock.sleep(1000);
                }
            }
        };
        new Thread(printRun).start();
    }

    //停止打印
    public static void downloadPrintEnd() {
        shouldPrint = false;
    }
}
