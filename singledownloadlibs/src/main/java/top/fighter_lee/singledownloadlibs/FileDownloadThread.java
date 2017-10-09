package top.fighter_lee.singledownloadlibs;

import android.content.Context;
import android.util.Log;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by lyb on 2016/3/23.
 * description  ${下载任务线程}
 */
public class FileDownloadThread extends Thread {

    private static final String TAG = FileDownloadThread.class.getSimpleName();
    private final Context mContext;

    private boolean isCompleted = false;
    private long downloadLength = 0;
    private File down_file;
    private URL downloadUrl;
    private int threadId;
    private long blockSize;
    long proressNow;

    private long startPos;
    private long endPos;

    public FileDownloadThread(URL downloadUrl, File file, long blocksize,
                              int threadId, Context context) {
        this.downloadUrl = downloadUrl;
        this.down_file = file;
        this.threadId = threadId;
        this.blockSize = blocksize;
        this.mContext = context;

        startPos = blockSize * (threadId - 1);
        endPos = blockSize * threadId - 1;
        try {
            File record_file = new File(Utils.getCachePath(mContext) + "/" + threadId + ".txt");
            if (file.exists() && file.length() > 0) {
                FileInputStream fis = new FileInputStream(record_file);
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(fis));

                String readLine = br.readLine();
                long newStartIndex = Long.valueOf(readLine);
                proressNow = newStartIndex - startPos;

                startPos = newStartIndex;
                br.close();
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void run() {

        RandomAccessFile raf = null;

        try {
            HttpURLConnection conn = (HttpURLConnection) downloadUrl.openConnection();
            conn.setConnectTimeout(DownloadTask.CONNECT_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setAllowUserInteraction(true);
            conn.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
            Log.d(TAG, Thread.currentThread().getName() + "  bytes="
                    + startPos + "-" + endPos);
            int response_code = conn.getResponseCode();
            if (response_code == 206
                    || response_code == 200) {
                Log.d(TAG, "run() ." + threadId + "请求成功,开始执行下载任务~");
                InputStream is = conn.getInputStream();
                raf = new RandomAccessFile(down_file, "rw");
                raf.seek(startPos);

                byte[] buffer = new byte[1024 * 1024];
                int len = -1;
                while ((len = is.read(buffer)) != -1) {
                    if (DownConfig.isCancel) {
                        Log.d(TAG, "线程" + threadId + "取消下载");
                        break;
                    }
                    raf.write(buffer, 0, len);
                    downloadLength += len;
                    RandomAccessFile raf2 = new RandomAccessFile(
                            Utils.getCachePath(mContext)
                                    + "/" + threadId + ".txt",
                            "rwd");
                    raf2.write(String.valueOf(downloadLength + startPos)
                            .getBytes());
                    raf2.close();
                }
                raf.close();
                isCompleted = true;
                Log.d(TAG, threadId + "thread task has finished,all size:"
                        + downloadLength);
                synchronized (FileDownloadThread.class) {
                    DownloadTask.runningCount--;
                    Log.d(TAG, "runningCount:" + DownloadTask.runningCount);

                }
            }
            isCompleted = true;

        } catch (IOException e) {
            DownloadTask.netError = true;
            Log.d(TAG, threadId + ",Exception:" + e.toString());
            isCompleted = true;
            e.printStackTrace();
        }
        if (DownloadTask.runningCount == 0 && !DownConfig.isCancel) {
            for (int i = 0; i <= DownloadTask.threadNum; i++) {
                File file2 = new File(Utils.getCachePath(mContext)
                        + "/"
                        + i
                        + ".txt");
                Log.d(TAG, "file delete!");
                file2.delete();
            }
        }
    }

    //此线程是否完成下载(异常,取消,下载完成属于完成下载)
    public boolean isCompleted() {
        return isCompleted;
    }

    //获取此线程的下载长度
    public long getDownloadLength() {
        return downloadLength + proressNow;
    }

}
