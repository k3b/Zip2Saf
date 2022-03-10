/*
    Copyright (C) 2022 k3b

    This file is part of de.k3b.android.zip2saf (https://github.com/k3b/Zip2Saf/)

    This program is free software: you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
    FOR A PARTICULAR PURPOSE. See the GNU General Public License
    for more details.

    You should have received a copy of the GNU General Public License along with
    this program. If not, see <http://www.gnu.org/licenses/>
    */

package de.k3b.android.zip2saf;

import static android.provider.DocumentsContract.Document.MIME_TYPE_DIR;

import android.Manifest;
import android.app.AuthenticationRequiredException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import de.k3b.zip2saf.data.MountInfo;
import de.k3b.zip2saf.data.MountInfoRepository;

public class ZipReadStorageProvider extends DocumentsProvider {
    /** debug= false : do not log.info() what the app is doing */
    public static boolean debug = true;

    public static final String TAG = "k3b.ZipSafProv" ;

    /**
     * Default root projection: everything but Root.COLUMN_MIME_TYPES, Root.COLUMN_SUMMARY, Root.COLUMN_AVAILABLE_BYTES
     */
    private final static String[] DEFAULT_ROOT_PROJECTION = new String[]{Root.COLUMN_ROOT_ID,
            Root.COLUMN_FLAGS, Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_ICON};
    /**
     * Default document projection: everything but Document.COLUMN_ICON and Document.COLUMN_SUMMARY
     */
    private final static String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME, Document.COLUMN_FLAGS, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED};

    /**
     * Check to see if we are missing the Storage permission group. In those cases, we cannot
     * access local files and must invalidate any root URIs currently available.
     *
     * @param context      The current Context
     * @param debugContext why this method was called
     * @return whether the permission has been granted it is safe to proceed
     */
    static boolean isMissingReadPermission(@Nullable Context context, String debugContext) {
        return isMissingPermission(context, debugContext, "isMissingReadPermission", Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private static boolean isMissingPermission(@Nullable Context context, String debugContext, String api, String permission) {
        if (context == null) {
            Log.i(TAG, api + " no context " + debugContext);
            return true;
        }
        if (ContextCompat.checkSelfPermission(context,
                permission) != PackageManager.PERMISSION_GRANTED) {
            // Make sure that our root is invalidated as apparently we lost permission
            context.getContentResolver().notifyChange(
                    DocumentsContract.buildRootsUri(BuildConfig.DOCUMENTS_AUTHORITY), null);
            Log.i(TAG, api + " no permissions for " + debugContext);
            return true;
        }
        return false;
    }

    protected static void closeSilently(Closeable stream, Object dbgContext) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e1) {
                Log.e(TAG, "closeSilently " + dbgContext.toString(), e1);
            }
        }
    }


    /**
     * Ejects the root. Throws {@link IllegalStateException} if ejection failed.
     *
     * @param rootId the root to be ejected.
     * @see Root#FLAG_SUPPORTS_EJECT
     */
    @Override
    public void ejectRoot(String rootId) {
        log("ejectRoot " + rootId);
        MountInfo mountInfo = Zip2SafHelper.getMountInfo(rootId);
        if (mountInfo == null)
            throw new IllegalStateException("Eject: Root " + rootId + " not found");

        MountInfoRepository repository = Zip2SafHelper.getRepository();
        repository.remove(mountInfo);

        getContext().getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(BuildConfig.DOCUMENTS_AUTHORITY), null);
        AndroidMountInfoRepositoryHelper.saveRepository(getContext().getApplicationContext(), repository);
    }

    /**
     * Create a new document and return its newly generated DocumentsContract.Document.COLUMN_DOCUMENT_ID.
     * You must allocate a new DocumentsContract.Document.COLUMN_DOCUMENT_ID to represent the
     * document, which must not change once returned.
     *
     * @param parentDocumentId the parent directory to create the new document under.
     * @param mimeType         the concrete MIME type associated with the new document. If the MIME type is not supported, the provider must throw.
     * @param displayName      the display name of the new document. The provider may alter this name to meet any internal constraints, such as avoiding conflicting names.
     * @return new created documentID.
     * @throws AuthenticationRequiredException – If authentication is required from the user (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public String createDocument(final String parentDocumentId, final String mimeType,
                                 final String displayName) {
        log("not implemented createDocument(" + parentDocumentId + "," + mimeType + "," + displayName + ")");
        return null;
    }

    /**
     * Return all roots currently provided. To display to users, you must define at least one root.
     * You should avoid making network requests to keep this request fast.
     * Each root is defined by the metadata columns described in DocumentsContract.Root,
     * including DocumentsContract.Root.COLUMN_DOCUMENT_ID which points to a directory representing
     * a tree of documents to display under that root.
     *
     * If this set of roots changes, you must call
     * ContentResolver.notifyChange(Uri, android.database.ContentObserver, boolean)
     * with DocumentsContract.buildRootsUri(String) to notify the system.
     *
     * @param projection list of DocumentsContract.Root columns to put into the cursor.
     *                   If null all supported columns should be included.
     */
    @Override
    public Cursor queryRoots(final String[] projection) {
        log("queryRoots");

        if (getContext() == null || ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            log("queryRoots no read permissions");
            return null;
        }
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);

        // Add Home directories
        MountInfoRepository repository = Zip2SafHelper.getRepository();

        for (MountInfo mountInfo : repository.getAll()) {
            if (!repository.isSpecialItem(mountInfo)) {
                final MatrixCursor.RowBuilder row = result.newRow();
                // These columns are required
                row.add(Root.COLUMN_ROOT_ID, mountInfo.zipId);
                row.add(Root.COLUMN_DOCUMENT_ID, mountInfo.zipId);
                row.add(Root.COLUMN_TITLE, mountInfo.zipId);
                int flags = Root.FLAG_LOCAL_ONLY;
                // flags |= Root.FLAG_SUPPORTS_SEARCH
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    flags |= Root.FLAG_SUPPORTS_IS_CHILD;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags |= Root.FLAG_SUPPORTS_EJECT;
                }
                row.add(Root.COLUMN_FLAGS, flags);
                row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
            }
        }
        return result;
    }

    /**
     * Test if a document is descendant (child, grandchild, etc) from the given parent. For example,
     * providers must implement this to support Intent.ACTION_OPEN_DOCUMENT_TREE.
     * You should avoid making network requests to keep this request fast.
     *
     * See Also:
     * DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD
     *
     * @param parentDocumentId parent to verify against.
     * @param documentId child to verify.
     * @return true if given document is a descendant of the given parent.
     */
    @Override
    public boolean isChildDocument(final String parentDocumentId, final String documentId) {
        boolean result = documentId.startsWith(Zip2SafHelper.getDirectoryID(parentDocumentId));
        log("isChildDocument(" + parentDocumentId + "," + documentId + ") ==> " + result);
        return result;
    }

    /**
     * Override this method to return the children documents contained in the requested directory.
     * This must return immediate descendants only.
     * If your provider is cloud-based, and you have data cached locally, you may return the local
     * data immediately, setting DocumentsContract.EXTRA_LOADING on Cursor extras to indicate that
     * you are still fetching additional data. Then, when the network data is available, you can
     * send a change notification to trigger a requery and return the complete contents.
     * To return a Cursor with extras, you need to extend and override Cursor.getExtras().
     * To support change notifications, you must Cursor.setNotificationUri(ContentResolver, Uri)
     * with a relevant Uri, such as DocumentsContract.buildChildDocumentsUri(String, String).
     * Then you can call ContentResolver.notifyChange(Uri, android.database.ContentObserver, boolean)
     * with that Uri to send change notifications.
     * See Also:
     * DocumentsContract.EXTRA_LOADING, DocumentsContract.EXTRA_INFO, DocumentsContract.EXTRA_ERROR
     * @param parentDocumentId the directory to return children for.
     * @param projection list of DocumentsContract.Document columns to put into the cursor. If null
     *                   all supported columns should be included.
     * @param sortOrder how to order the rows, formatted as an SQL
     *                  {@code ORDER BY} clause (excluding the ORDER BY itself).
     *                  Passing {@code null} will use the default sort order, which
     *                  may be unordered. This ordering is a hint that can be used to
     *                  prioritize how data is fetched from the network, but UI may
     *                  always enforce a specific ordering.
     * @throws AuthenticationRequiredException – If authentication is required from the user
     *      (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public Cursor queryChildDocuments(final String parentDocumentId, final String[] projection,
            final String sortOrder) {
        log("queryChildDocuments(" + parentDocumentId+ ",sort=" + sortOrder + ")");

        if (ZipReadStorageProvider.isMissingReadPermission(getContext(), "queryChildDocuments")) {
            return null;
        }
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        LocalFileHeader localFileHeader;
        MountInfo mountInfo = Zip2SafHelper.getRepository().getById(Zip2SafHelper.getRootId(parentDocumentId));
        try (ZipInputStream zipInputStream = getZipInputStream(parentDocumentId, mountInfo)){
           String dir = Zip2SafHelper.getDirectoryID(Zip2SafHelper.getZipPath(parentDocumentId));
            Set<String> duplicates = new HashSet<>();
           while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
               includeLocalFileHeader(result, mountInfo.zipId, dir, localFileHeader, duplicates);
           }
        } catch (IOException ioException) {
            Log.e(TAG, "queryChildDocuments(" + parentDocumentId+ ",sort=" + sortOrder + ") : " + ioException.getMessage(), ioException);
        }
        return result;
    }

    /**
     * Return metadata for the single requested document. You should avoid making network requests
     * to keep this request fast.
     * @param documentId the document to return.
     * @param projection list of DocumentsContract.Document columns to put into the cursor. If null
     *                   all supported columns should be included.
     * @throws  AuthenticationRequiredException – If authentication is required from the user
     *          (such as login credentials), but it is not guaranteed that the client will
     *          handle this properly.
     */
    @Override
    public Cursor queryDocument(final String documentId, final String[] projection) {
        log("queryDocument " + documentId);

        if (ZipReadStorageProvider.isMissingReadPermission(getContext(), "queryDocument")) {
            return null;
        }
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);

        LocalFileHeader localFileHeader;
        MountInfo mountInfo = Zip2SafHelper.getRepository().getById(Zip2SafHelper.getRootId(documentId));
        try (ZipInputStream zipInputStream = getZipInputStream(documentId, mountInfo)){
            String zipPath = Zip2SafHelper.getZipPath(documentId);
            if (zipPath.isEmpty()) {
                // special case: root dir
                includeDir(result,mountInfo.zipId,"","");
            } else {
                while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
                    if (zipPath.equals(localFileHeader.getFileName())) {
                        includeLocalFileHeader(result, mountInfo.zipId, zipPath, localFileHeader, null);
                    }
                }
            }
        } catch (IOException ioException) {
            Log.e(TAG, "queryDocument " + documentId + ": " + ioException.getMessage(), ioException);
        }
        return result;
    }

    private void log(String msg) {
        if (debug) {
            Log.i(TAG, msg);
        }
    }

    private InputStream openZipEntryInputStream(final String documentId, String debugContext) {
        LocalFileHeader localFileHeader;
        MountInfo mountInfo = Zip2SafHelper.getRepository().getById(Zip2SafHelper.getRootId(documentId));
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = getZipInputStream(documentId, mountInfo);
            String zipPath = Zip2SafHelper.getZipPath(documentId);
            while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
                if (zipPath.equals(localFileHeader.getFileName())) {
                    // found: close is done outside
                    return zipInputStream;
                }
            }
        } catch (IOException ioException) {
            Log.e(TAG, "openZipEntryInputStream " + documentId + ": " + ioException.getMessage(), ioException);
        }
        // not found: close zip file
        closeSilently(zipInputStream, debugContext + "-openZipEntryInputStream '" + documentId + "': entry not found");
        return null;
    }

    /**
     * Open and return a thumbnail of the requested document.
     * A provider should return a thumbnail closely matching the hinted size, attempting to
     * serve from a local cache if possible. A provider should never return images more than double
     * the hinted size.
     * If you perform expensive operations to download or generate a thumbnail, you should
     * periodically check CancellationSignal.isCanceled() to abort abandoned thumbnail requests.
     * <p>
     * See Also:
     * DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
     *
     * @param documentId the document to return.
     * @param sizeHint   hint of the optimal thumbnail dimensions.
     * @param signal     used by the caller to signal if the request should be cancelled. May be null.
     * @throws AuthenticationRequiredException – If authentication is required from the user (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public AssetFileDescriptor openDocumentThumbnail(final String documentId, final Point sizeHint,
                                                     final CancellationSignal signal) throws FileNotFoundException {
        log("openDocumentThumbnail(" + documentId + "," + sizeHint + ")");

        if (ZipReadStorageProvider.isMissingReadPermission(getContext(), "openDocumentThumbnail")) {
            return null;
        }
        // Assume documentId points to an image file. Build a thumbnail no larger than twice the sizeHint
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(documentId, options);
        final int targetHeight = 2 * sizeHint.y;
        final int targetWidth = 2 * sizeHint.x;
        final int height = options.outHeight;
        final int width = options.outWidth;
        options.inSampleSize = 1;
        if (height > targetHeight || width > targetWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / options.inSampleSize) > targetHeight
                    || (halfWidth / options.inSampleSize) > targetWidth) {
                options.inSampleSize *= 2;
            }
        }
        options.inJustDecodeBounds = false;
        File tempFile;
        FileOutputStream out = null;
        InputStream inputStream = null;

        try {
            inputStream = openZipEntryInputStream(documentId, "create thumbnail from openDocumentThumbnail " + documentId);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            tempFile = File.createTempFile("thumbnail", null, getContext().getCacheDir());
            out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (IOException e) {
            Log.e(TAG, "openDocumentThumbnail " + documentId +
                    " Error writing thumbnail", e);
            return null;
        } finally {
            closeSilently(inputStream, "Error closing thumbnail original");
            closeSilently(out, "Error closing thumbnail generated thumbnail");
        }
        // It appears the Storage Framework UI caches these results quite aggressively so there is little reason to
        // write your own caching layer beyond what you need to return a single AssetFileDescriptor
        return new AssetFileDescriptor(ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY), 0,
                AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    private void includeDir(final MatrixCursor result, @NonNull String zipId, String dirNameWithoutPath, String zipDirPath) {
        includeResult(result, dirNameWithoutPath, Zip2SafHelper.getDocumentId(zipId, zipDirPath), MIME_TYPE_DIR, null, null, 0);
    }

    static private Long orNull(long uncompressedSize) {
        if (uncompressedSize == 0) return null;
        return uncompressedSize;
    }

    private void includeFile(final MatrixCursor result, @NonNull String zipId, final LocalFileHeader file, String filenameWithoutPath) {
        String mimeType = getDocumentType(filenameWithoutPath);
        /*
        @SuppressLint("InlinedApi")
        int flags = file.canWrite()
                ? Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_WRITE | Document.FLAG_SUPPORTS_RENAME
                | (mimeType.equals(Document.MIME_TYPE_DIR) ? Document.FLAG_DIR_SUPPORTS_CREATE : 0) : 0;
         */
        int flags = 0;
        // We only show thumbnails for image files - expect a call to openDocumentThumbnail for each file that has
        // this flag set
        if (mimeType.startsWith("image/")) {
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        // zipfile entry may or may not contain a value for LastModified or UncompressedSize
        Long lastModifiedTimeInMilliSince1970OrNull = Zip2SafHelper.getTimeInMilliSince1970OrNull(
                file.getLastModifiedTimeEpoch());

        includeResult(result, filenameWithoutPath, Zip2SafHelper.getDocumentId(
                zipId, file.getFileName()), mimeType, lastModifiedTimeInMilliSince1970OrNull,
                orNull(file.getUncompressedSize()), flags);
    }

    void includeResult(MatrixCursor result, String filenameWithoutPath, String documentId,
                               String mimeType, Long lastModifiedTime, Long uncompressedSize, int flags) {
        final MatrixCursor.RowBuilder row = result.newRow();
        // These columns are required
        row.add(Document.COLUMN_DOCUMENT_ID, documentId);
        row.add(Document.COLUMN_DISPLAY_NAME, filenameWithoutPath);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_FLAGS, flags);
        // COLUMN_SIZE is required, but can be null
        row.add(Document.COLUMN_SIZE, uncompressedSize);
        // These columns are optional
        row.add(Document.COLUMN_LAST_MODIFIED, lastModifiedTime);
        // Document.COLUMN_ICON can be a resource id identifying a custom icon. The system provides default icons
        // based on mime type
        // Document.COLUMN_SUMMARY is optional additional information about the file
    }

    /**
     * Return concrete MIME type of the requested document. Must match the value
     * of {@link Document#COLUMN_MIME_TYPE} for this document. The default
     * implementation queries {@link #queryDocument(String, String[])}, so
     * providers may choose to override this as an optimization.
     * <p>
     * @throws AuthenticationRequiredException If authentication is required from
     *            the user (such as login credentials), but it is not guaranteed
     *            that the client will handle this properly.
     */
    @Override
    public String getDocumentType(final String documentId) {
        String mime = getDocumentTypeImpl(documentId);
        log("getDocumentType " + documentId + " => " + mime);
        return mime;
    }

    String getDocumentTypeImpl(String documentId) {
        if (ZipReadStorageProvider.isMissingReadPermission(getContext(), "getDocumentType")) {
            return null;
        }
        // From FileProvider.getType(Uri)
        final int lastDot = documentId.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = documentId.substring(lastDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    /**
     * Upon returning, any URI permission grants for the given document will be revoked. If additional documents were deleted as a side effect of this call (such as documents inside a directory) the implementor is responsible for revoking those permissions using revokeDocumentPermission(String).
     * @param documentId the document to delete.
     * @throws AuthenticationRequiredException – If authentication is required from the user (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public void deleteDocument(final String documentId) {
        log("not implemented deleteDocument " + documentId);

        return;
    }

    /**
     * Rename an existing document.
     * If a different DocumentsContract.Document.COLUMN_DOCUMENT_ID must be used
     * to represent the renamed document, generate and return it. Any outstanding URI permission
     * grants will be updated to point at the new document. If the original
     * DocumentsContract.Document.COLUMN_DOCUMENT_ID is still valid after the rename, return null.
     * @param documentId the document to rename.
     * @param displayName the updated display name of the document. The provider may alter this name to meet any internal constraints, such as avoiding conflicting names.
     * @return new generated documentId
     * @throws AuthenticationRequiredException – If authentication is required from the user (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public String renameDocument(final String documentId, final String displayName) throws FileNotFoundException {
        log("not implemented renameDocument " + documentId);
        return null;
    }

    /**
     * Open and return the requested document.
     * Your provider should return a reliable ParcelFileDescriptor to detect when the remote caller
     * has finished reading or writing the document.
     * Mode "r" should always be supported. Provider should throw UnsupportedOperationException if
     * the passing mode is not supported. You may return a pipe or socket pair if the mode is
     * exclusively "r" or "w", but complex modes like "rw" imply a normal file on disk that
     * supports seeking.
     *
     * If you block while downloading content, you should periodically check
     * CancellationSignal.isCanceled() to abort abandoned open requests.
     * See Also:
     * ParcelFileDescriptor.open(java.io.File, int, android.os.Handler, OnCloseListener), ParcelFileDescriptor.createReliablePipe(), ParcelFileDescriptor.createReliableSocketPair(), ParcelFileDescriptor.parseMode(String)
     *
     * @param documentId the document to return.
     * @param mode the mode to open with, such as 'r', 'w', or 'rw'.
     * @param signal used by the caller to signal if the request should be cancelled. May be null.
     * @throws AuthenticationRequiredException – If authentication is required from the user (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public ParcelFileDescriptor openDocument(final String documentId, final String mode,
                                             final CancellationSignal signal) throws FileNotFoundException {
        log("openDocument " + documentId);

        if (ZipReadStorageProvider.isMissingReadPermission(getContext(), "openDocument")) {
            return null;
        }

        InputStream is = openZipEntryInputStream(documentId, "openDocument");
        return createPipeDescriptor(is, "openDocument " + documentId);
    }

    /**
     * to allow unittests without an existing zip file
     */
    void includeLocalFileHeader(MatrixCursor result, String zipId, String dir, LocalFileHeader localFileHeader, Set<String> alreadyIncluded) {
        String relPath = Zip2SafHelper.getRelPath(localFileHeader, dir);
        if (relPath != null) {
            boolean isDirectory = localFileHeader.isDirectory();
            int end;
            if (!isDirectory && (end = relPath.indexOf(Zip2SafHelper.PATH_DELIMITER) + 1) > 0) {
                isDirectory = true;
                relPath = relPath.substring(0, end);
            }
            if (alreadyIncluded == null || !alreadyIncluded.contains(relPath)) {
                if (isDirectory) {
                    includeDir(result, zipId, relPath, dir + relPath);
                } else {
                    includeFile(result, zipId, localFileHeader, relPath);
                }
                if (alreadyIncluded != null) {
                    alreadyIncluded.add(relPath);
                }
            }
        }
    }

    // https://stackoverflow.com/questions/18212152/transfer-inputstream-to-another-service-across-process-boundaries-with-parcelf
    @Nullable
    protected ParcelFileDescriptor createPipeDescriptor(InputStream is, String dbgContext) {
        OutputStream os = null;
        try {
            ParcelFileDescriptor[] pfd = ParcelFileDescriptor.createPipe();//  inputStreamService.inputStream();
            os = new ParcelFileDescriptor.AutoCloseOutputStream(pfd[1]); // write side of pipe

            new TransferThread(is, os, dbgContext).start();
            return pfd[0]; // read side of pipe
        } catch (IOException e) {
            Log.e(TAG, dbgContext + " createPipeDescriptor", e);
            closeSilently(is, dbgContext + " inputStream");
            closeSilently(os, dbgContext + " outputStream");
        }
        return null;
    }

    static class TransferThread extends Thread {
        protected final InputStream in;
        protected final OutputStream out;
        private final String dbgContext;

        TransferThread(InputStream in, OutputStream out, String dbgContext) {
            super("TransferThread " + dbgContext);
            this.in = in;
            this.out = out;
            this.dbgContext = dbgContext;
            setDaemon(true);
        }

        @Override
        public void run() {
            int len;
            byte[] buf = new byte[1024];
            try {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (IOException ioException) {
                Log.e(TAG, dbgContext, ioException);

            } finally {
                closeSilently(in, dbgContext + " inputStream");
                closeSilently(out, dbgContext + " outputStream");
            }
        }
    }

    /**
     * Implement this to initialize your content provider on startup. This method is called for
     * all registered content providers on the application main thread at application launch time.
     * It must not perform lengthy operations, or application startup will be delayed.
     * You should defer nontrivial initialization (such as opening, upgrading, and scanning databases)
     * until the content provider is used (via query, insert, etc). Deferred initialization keeps
     * application startup fast, avoids unnecessary work if the provider turns out not to be
     * needed, and stops database errors (such as a full disk) from halting application launch.
     * If you use SQLite, android.database.sqlite.SQLiteOpenHelper is a helpful utility class that
     * makes it easy to manage databases, and will automatically defer opening until first use. If you do use SQLiteOpenHelper, make sure to avoid calling android.database.sqlite.SQLiteOpenHelper.getReadableDatabase or android.database.sqlite.SQLiteOpenHelper.getWritableDatabase from this method. (Instead, override android.database.sqlite.SQLiteOpenHelper.onOpen to initialize the database when it is first opened.)
     * @return true if the provider was successfully loaded, false otherwise
     *
     */
    @Override
    public boolean onCreate() {
        log("onCreate");
        return true;
    }

    @NonNull
    private ZipInputStream getZipInputStream(String documentId, MountInfo mountInfo) throws FileNotFoundException {
        if (mountInfo != null) {
            InputStream inputStream = getContext().getContentResolver().openInputStream(Uri.parse(mountInfo.uri));
            return Zip2SafHelper.getZipInputStream(inputStream, mountInfo.password);
        }
        throw new FileNotFoundException(documentId);
    }

}
