package com.videorecord;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wuwentao on 2019/2/28.
 */

public class FileUtils {
    static List<VideoFileStatus> list = new ArrayList<>();

    public static void clearLisr() {
        if (null != list) {
            list.clear();
        }
    }

    public static List<VideoFileStatus> searchMp4Infos(File file, String[] ext) {
        if (file != null) {
            if (file.isDirectory()) {
                File[] listFile = file.listFiles();
                if (listFile != null) {
                    for (int i = 0; i < listFile.length; i++) {
                        searchMp4Infos(listFile[i], ext);
                    }
                }
            } else {
                String filename = file.getAbsolutePath();
                for (int i = 0; i < ext.length; i++) {
                    if (filename.endsWith(ext[i])) {
                        Log.e("searchMp4Infos   ", filename);
                        VideoFileStatus videoFileStatus = new VideoFileStatus();
                        videoFileStatus.filePath = filename;
                        videoFileStatus.isCheck = false;
                        list.add(videoFileStatus);
                        break;
                    }
                }
            }
        }
        return list;
    }

    /**
     * 获取外置sdk（TF卡）路径
     *
     * @param context
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static boolean getExternalStorageState(Context context) {
        StorageManager mStorageManager;
        String mExternalSD = null; // 外置sd卡 1
        String[] mPath;
        if (context != null) {
            mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            try {
                Method mMethodGetPaths = mStorageManager.getClass().getMethod("getVolumePaths");
                mPath = (String[]) mMethodGetPaths.invoke(mStorageManager);
                if (mPath.length > 1) {
                    mExternalSD = mPath[1];
                    aUrl = mPath[1];
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (TextUtils.isEmpty(Environment.getExternalStorageDirectory().getPath())) {
            return false;
        }
        try {
            return "mounted".equals(Environment.getStorageState(new File(mExternalSD)));
        } catch (Exception rex) {
            return false;
        }
    }

    /**
     * 获取外置sdk（TF卡）路径
     *
     * @return
     */
    private static String getRemovableStoragePath() {
        StorageManager mStorageManager = (StorageManager) MyApplication.getInstance().getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return "null";
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return "null";
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return "null";
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return "null";
        }
        return "null";
    }

    private static String sRemovableSdcardPath;
    public static boolean isCanRead = true;
    private static String aUrl = "";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getSDCard1() {
        if (TextUtils.isEmpty(aUrl)) {
            getExternalStorageState(MyApplication.getInstance());
        }
        String path = aUrl + "/Android/data/" + MyApplication.getInstance().getPackageName() + "/files/playback";
        File file = new File(path);
        if (!file.exists()) {
            if (file.mkdirs()) {
                Log.e("FASFDS11111111111111", "canWrite  " + file.canWrite() + "  canRead" + file.canRead() + "  path " + file.getPath() + file.canExecute());
            }
        }
        Log.e("FASFDS", "canWrite  " + file.canWrite() + "  canRead" + file.canRead() + "  path " + file.getPath() + file.canExecute());
        if (file.canWrite() && file.canRead()) {
            return file.getPath();
        }
        return null;
    }

    /**
     * 获取存储路径
     *
     * @return
     */
    public static String getBaseCachePath() {
        String path;
        File cacheDir;
        if (TextUtils.isEmpty(sRemovableSdcardPath)) {
            sRemovableSdcardPath = getRemovableStoragePath();
        }
        if (!sRemovableSdcardPath.equals("null") && isCanRead) {
            MyApplication.getInstance().getExternalFilesDir(null).getAbsolutePath();
            path = sRemovableSdcardPath + "/Android/data/" + MyApplication.getInstance().getPackageName() + "/files/recorder_video";
            cacheDir = new File(path);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            if (cacheDir.canWrite() && cacheDir.canRead()) {
                return cacheDir.getPath(); //返回外置sdk（TF卡）存储路径
            } else {
                isCanRead = false;
            }
            return getBaseCachePath();
        } else {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) && Environment.getExternalStorageDirectory().canWrite()) {
                path = Environment.getExternalStorageDirectory().getPath() + "/recorder_video";
            } else {
                path = Environment.getDataDirectory().getAbsolutePath() + "/recorder_video";
            }
            cacheDir = new File(path);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            return cacheDir.getPath();//返回内置sdk存储路径
        }
    }

    public static String getExternalStoragePath() {
        String path;
        File cacheDir;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) && Environment.getExternalStorageDirectory().canWrite()) {
            path = Environment.getExternalStorageDirectory().getPath() + "/recorder_video";
        } else {
            path = Environment.getDataDirectory().getAbsolutePath() + "/recorder_video";
        }
        cacheDir = new File(path);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir.getPath();
    }

    public static String getOutputMediaFile(String path) {
        Date date = new Date();
        String ymd = new SimpleDateFormat("yyyyMMdd").format(date);
        String hms = new SimpleDateFormat("HHmmss").format(date);
        String saveDir = path + File.separator + ymd;
        File file = new File(saveDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(ymd);
        builder.append(hms);
        builder.append(".mp4");
        String fileName = builder.toString();
        File file1 = new File(saveDir, fileName);
        return file1.getPath();
    }


    public static String getFileName(String path) {
        String regEx = ".+/(.+)$";
        // String regEx = ".+\\\\(.+)$";
        // String str = "c:\\dir1\\dir2\\文件.pdf";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(path);
        if (!m.find()) {
            System.out.println("文件路径格式错误!");
            return "";
        }
        return m.group(1);
    }

    /**
     * 递归删除文件和文件夹
     *
     * @param file 要删除的根目录
     */
    public static void RecursionDeleteFile(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] childFile = file.listFiles();
            if (childFile == null || childFile.length == 0) {
                file.delete();
                return;
            }
            for (File f : childFile) {
                RecursionDeleteFile(f);
            }
            file.delete();
        }
    }

    public static List<FileEvent> getDirectoryFileName(File file) {
        List<FileEvent> directoryFileNameList = new ArrayList<>();
        if (file != null) {
            if (file.isDirectory()) {
                File[] listFile = file.listFiles();
                if (listFile != null) {
                    for (int i = 0; i < listFile.length; i++) {
                        FileEvent fileEvent = new FileEvent();
                        fileEvent.fileName = listFile[i].getName();
                        fileEvent.path = listFile[i].getAbsolutePath();
                        directoryFileNameList.add(fileEvent);
                        Log.e(" listFile[i].getName()   ", listFile[i].getName());
                    }
                }
            }
        }
        return directoryFileNameList;
    }

    public static int nDaysBetweenTwoDate(Date firstString, String secondString) {
        Log.e(" nDaysBetweenTwoDate   ", secondString);
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        Date secondDate = null;
        try {
            secondDate = df.parse(secondString);
        } catch (Exception e) {
            // 日期型字符串格式错误
            System.out.println("日期型字符串格式错误");
            return 0;
        }
        int nDay = (int) ((firstString.getTime() - secondDate.getTime()) / (24 * 60 * 60 * 1000));
        return nDay;
    }

    public static void deleteOldVideo() {
        Date date = new Date();
        for (FileEvent fileEvent : FileUtils.getDirectoryFileName(new File(FileUtils.getBaseCachePath()))) {
            Log.e("handleMessage", FileUtils.nDaysBetweenTwoDate(date, fileEvent.fileName) + "  rewrewrew");
            Log.e("handleMessage", fileEvent.path);
            if (FileUtils.nDaysBetweenTwoDate(date, fileEvent.fileName) >= 7) {
                FileUtils.RecursionDeleteFile(new File(fileEvent.path));
            }
        }
    }

    public static boolean isBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                /*
                BACKGROUND=400 EMPTY=500 FOREGROUND=100
                GONE=1000 PERCEPTIBLE=130 SERVICE=300 ISIBLE=200
                 */
                Log.i(context.getPackageName(), "此appimportace ="
                        + appProcess.importance
                        + ",context.getClass().getName()="
                        + context.getClass().getName());
                if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    Log.i(context.getPackageName(), "处于后台"
                            + appProcess.processName);
                    return true;
                } else {
                    Log.i(context.getPackageName(), "处于前台"
                            + appProcess.processName);
                    return false;
                }
            }
        }
        return false;
    }


    /**
     * 删除单个文件
     *
     * @param filePath$Name 要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static  boolean deleteSingleFile(String filePath$Name) {
        File file = new File(filePath$Name);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.e("--Method--", "Copy_Delete.deleteSingleFile: 删除单个文件" + filePath$Name + "成功！");
                return true;
            }
        }

        return false;
    }

}
