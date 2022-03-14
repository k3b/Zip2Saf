package de.k3b.android.zip2saf;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import de.k3b.zip2saf.data.MountInfo;
import de.k3b.zip2saf.data.MountInfoRepository;

public class Zip2SafHelper {
    static final String PATH_DELIMITER = "/";
    /**
     * load on demand via getRepository()
     */
    private static MountInfoRepository repository = null;

    private static long millisecsSince2100;

    static {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        // 2100-02-01
        cal.set(2100, 1, 1);
        millisecsSince2100 = cal.getTimeInMillis();
    }

    //--------------
    static String getRelPath(LocalFileHeader localFileHeader, String zipParentDir) {
        String zipPath = (localFileHeader != null) ? localFileHeader.getFileName() : null;
        return getRelPath(zipPath, zipParentDir);
    }

    /**
     * @return zipPath without zipParentDir or null, if zipPath is not below zipParentDir.
     */
    @Nullable
    static String getRelPath(String zipPath, String zipParentDir) {
        if (zipPath != null && zipPath.startsWith(zipParentDir)) return zipPath.substring(zipParentDir.length());
        return null;
    }

    static MountInfoRepository getRepository() {
        if (repository == null) {
            repository = MountInfoRepository.getInstance();
        }
        return repository;
    }

    @Nullable
    static MountInfo getMountInfo(String documentId) {
        if (documentId == null) return null;
        return getRepository().getById(getRootId(documentId));
    }

    static String getDocumentId(String zipId, String fileName) {
        return zipId + PATH_DELIMITER + fileName;
    }

    /**
     * @return zipID without pathInsideZip. null if error
     */
    static String getRootId(String documentId) {
        if ((documentId == null) || (documentId.isEmpty())) return null;

        if (documentId.startsWith(PATH_DELIMITER)) return getRootId(documentId.substring(1));

        String result = documentId;
        int end = documentId.indexOf(PATH_DELIMITER);
        if (end >= 0) result = documentId.substring(0, end);
        if (ZipReadStorageProvider.debug) Log.i(ZipReadStorageProvider.TAG, "getRootId('"
                + documentId + "') => '" + result + "'");
        return result;
    }

    /**
     * @return pathInsideZip without zipID. "" if root
     */
    static String getZipPath(String documentId) {
        String result = "";
        if ((documentId != null) && (!documentId.isEmpty())) {

            if (documentId.startsWith(PATH_DELIMITER)) return getZipPath(documentId.substring(1));
            int begin = documentId.indexOf(PATH_DELIMITER) + 1;

            if (begin > 1 && documentId.length() > begin) {
                result = documentId.substring(begin);
            }
        }
        if (ZipReadStorageProvider.debug) Log.i(ZipReadStorageProvider.TAG, "getZipPath('"
                + documentId + "') => '" + result + "'");
        return result;
    }

    @NonNull
    static String getDirectoryID(@Nullable String parentDocumentId) {
        if (parentDocumentId == null) return "";
        if (parentDocumentId.length() > 0 && !parentDocumentId.endsWith(PATH_DELIMITER)) {
            return parentDocumentId + PATH_DELIMITER;
        }
        return parentDocumentId;
    }

    @NonNull
    static ZipInputStream getZipInputStream(InputStream inputStream, String password) {
        return new ZipInputStream(inputStream, password == null ? null : password.toCharArray());
    }

    public static String decodeDocumentId(String path) {
        if (false) {
            if (path == null || path.isEmpty()) return "";
            return DocumentsContract.getDocumentId(Uri.parse(path));
        }
        try {
            path = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    @Nullable
    public static Long getTimeInMilliSince1970OrNull(long timeInSecsSince1970) {
        // assume Secs Since 1970-01-01
        Long result = timeInSecsSince1970;
        if (result == null || result == 0) {
            return null;
        }

        // assume milliSecs since 1970 as required by Document.COLUMN_LAST_MODIFIED
        result *= 1000;

        if (result > millisecsSince2100) {
            // if calculated time is bigger than 2100-01-01 assume that
            // input time timeInSecsSince1970 was already in milliSecs since 1970
            return timeInSecsSince1970;
        }

        return result;
    }

    @NonNull
    public static File getThumbCacheDir(@NonNull Context context, @NonNull String rootId) {
        File cacheDir = new File(context.getCacheDir(), rootId);
        cacheDir.mkdirs();
        return cacheDir;
    }

    public static void clearThumbCache(@NonNull Context context, @NonNull String zipId) {
        File thumbCacheDir = getThumbCacheDir(context, zipId);
        int delCount = 0;
        if (thumbCacheDir.exists()) {
            for (File f : thumbCacheDir.listFiles()) {
                if (f.delete()) delCount++;
            }
            if (thumbCacheDir.delete()) delCount++;
        }

        if (delCount > 0) {
            Log.i(ZipReadStorageProvider.TAG, "clearThumbCache('" + zipId +
                    "')  items deleted: " + delCount);

        }
    }
}
