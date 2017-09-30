package top.fighter_lee.singledownloadlibs;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Utils {

    /**
     * 得到cpu核心数
     *
     * @return cpu核心数
     */
    public static int getNumCores() {

        try {
            return Runtime.getRuntime().availableProcessors();
        } catch (Exception e) {
            return 4;
        }
    }

    /**
     * 清除缓存文件
     *
     * @param context
     */
    public static void cleanCache(Context context) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            deleteFilesByDirectory(context.getExternalCacheDir());
        }
    }

    public static String getCachePath(Context context) {
        String path = "";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            //sd卡可用
            path = context.getExternalCacheDir().getAbsolutePath();
        }else{
            //sd卡不可用
            path = context.getCacheDir().getAbsolutePath();
        }
        return path;
    }

    /**
     * 获取存储在本地的下载记录文件
     *
     * @param context
     * @return 本地的下载记录文件
     */
    public static List<File> getCacheDownFile(Context context) {
        List<File> file_list = new ArrayList();
        for (int i = 0; i < (getNumCores()+1); i++) {
            File file = new File(getCachePath(context) + "/" + (i + 1) + ".txt");
            if (file.exists() && file.length() > 0) {
                file_list.add(file);
            }
        }
        return file_list;
    }

    public static boolean deletCacheDownFile(Context context) {
        List<File> cacheDownFile = getCacheDownFile(context);
        boolean delete = true;
        for (File file : cacheDownFile) {
            boolean del = file.delete();
            if (del = false)
                delete = false;
        }
        return delete;
    }


    public static boolean isNewtask(Context context) {
        List<File> cacheDownFile = getCacheDownFile(context);
        if (cacheDownFile.size() > 0) {
            return false;
        }
        return true;
    }

    /**
     * 删除方法 这里只会删除某个文件夹下的文件，如果传入的directory是个文件，将不做处理 * * @param directory
     */
    private static void deleteFilesByDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            for (File item : directory.listFiles()) {
                item.delete();
            }
        }
    }

    public static boolean deletUpgradeFile(Context context,String path) {
        File file = new File(path);
        if (!Utils.isNewtask(context)) {
            Utils.deletCacheDownFile(context);
        }
        return file.delete();
    }

}
