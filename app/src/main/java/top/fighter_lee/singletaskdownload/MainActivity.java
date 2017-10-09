package top.fighter_lee.singletaskdownload;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.File;

import top.fighter_lee.singledownloadlibs.DownConfig;
import top.fighter_lee.singledownloadlibs.DownloadTask;
import top.fighter_lee.singledownloadlibs.IDownloadListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.pb);
    }

    public void down_start(View view) {
        new DownloadTask()
                .setContext(this)
                .setDownloadDir(getCacheDir())
//                .setDownloadFileName("download.zip")//设置下载文件名
                .setDownloadUrl("https://dldir1.qq.com/qqfile/qq/QQ8.9.5/22057/QQ8.9.5.exe")
                .setDownloadListener(new IDownloadListener() {
                    @Override
                    public void onCancel() {
                        Log.d(TAG, "onCancel: ");
                    }

                    @Override
                    public void onFail(int error) {
                        Log.d(TAG, "onFail: " + error);
                    }

                    @Override
                    public void onProgress(int progress) {
                        Log.d(TAG, "onProgress: " + progress);
                        progressBar.setProgress(progress);
                    }

                    @Override
                    public void onComplete(File file) {
                        Log.d(TAG, "onComplete: ");
                    }

                    @Override
                    public void onPrepare(long totalSize) {
                        Log.d(TAG, "onPrepare: " + totalSize);
                    }
                })
                .execute();
    }

    public void down_cancel(View view) {
        DownConfig.cancelDownload();
    }
}
