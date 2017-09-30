package top.fighter_lee.singledownloadlibs;


public class Error {

    /**
     * 无网络连接
     */
    public static final int NET_DISCONNECT = -1;
    /**
     * 下载已经取消
     */
    public static final int CANCEL = -2;
    /**
     * 未定义错误
     */
    public static final int ERROR = -3;
    /**
     * 读取文件错误
     */
    public static final int DOWN_FILE_NOT_EXIST = -4;
    /**
     * 服务器响应状态错误：非 200,206
     */
    public static final int SERVER_RESPONSE = -5;
    /**
     * 下载文件超时：网络状态不好，导致下载文件2分钟无响应
     */
    public static final int DOWNLOADING_TIME_OUT = -6;
    /**
     * 下载文件期间，网络访问异常
     */
    public static final int DOWNLOADING_NET_EXCEPTION = -7;
    /**
     * 文件md5校验失败
     */
    public static final int DOWNLOADING_INVALID_FILE = -8;

    /**
     * 下载路径不合法
     */
    public static final int DOWNLOADING_PACKAGE_IS_INVALID = -9;

    /**
     * 下载链接有误或者网络问题
     */
    public static final int DOWNLOADING_LINK_ERROR = -10;

}