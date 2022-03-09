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
    public static final String TAG = "k3b.ZipSafProv" ;
    private static final String PATH_DELIMITER = "/";
    public static boolean debug = true;

    /** load on demand via getRepository() */
    private static MountInfoRepository repository = null;

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
     * @param context The current Context
     * @param debugContext
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
        if (debug) Log.i(TAG, "queryRoots");

        if (getContext() == null || ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (debug) Log.i(TAG, "queryRoots no read permissions");
            return null;
        }
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);

        // Add Home directories
        MountInfoRepository repository = getRepository();

        for (MountInfo mountInfo : repository.getAll()) {
            if (!repository.isSpecialItem(mountInfo)) {
                final MatrixCursor.RowBuilder row = result.newRow();
                // These columns are required
                row.add(Root.COLUMN_ROOT_ID, mountInfo.zipId);
                row.add(Root.COLUMN_DOCUMENT_ID, mountInfo.zipId);
                row.add(Root.COLUMN_TITLE, mountInfo.zipId);
                row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_SEARCH |
                        Root.FLAG_SUPPORTS_IS_CHILD | Root.FLAG_SUPPORTS_EJECT);
                row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
            }
        }
        return result;
    }


    /**
     * Ejects the root. Throws {@link IllegalStateException} if ejection failed.
     *
     * @param rootId the root to be ejected.
     * @see Root#FLAG_SUPPORTS_EJECT
     */
    @Override
    public void ejectRoot(String rootId) {
        if (debug) Log.i(TAG, "ejectRoot " + rootId);
        MountInfo mountInfo = getMountInfo(rootId);
        if (mountInfo == null)  throw new IllegalStateException("Eject: Root " + rootId + " not found");

        getRepository().remove(mountInfo);

        getContext().getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(BuildConfig.DOCUMENTS_AUTHORITY), null);
    }

    /**
     * Create a new document and return its newly generated DocumentsContract.Document.COLUMN_DOCUMENT_ID.
     * You must allocate a new DocumentsContract.Document.COLUMN_DOCUMENT_ID to represent the
     * document, which must not change once returned.
     *
     * @param parentDocumentId the parent directory to create the new document under.
     * @param mimeType the concrete MIME type associated with the new document. If the MIME type is not supported, the provider must throw.
     * @param displayName the display name of the new document. The provider may alter this name to meet any internal constraints, such as avoiding conflicting names.
     * @return new created documentID.
     * @throws AuthenticationRequiredException – If authentication is required from the user (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public String createDocument(final String parentDocumentId, final String mimeType,
            final String displayName) {
        if (debug) Log.i(TAG, "not implemented createDocument(" + parentDocumentId + "," + mimeType + "," + displayName + ")");
        return null;
    }

    /**
     * Open and return a thumbnail of the requested document.
     * A provider should return a thumbnail closely matching the hinted size, attempting to
     * serve from a local cache if possible. A provider should never return images more than double
     * the hinted size.
     * If you perform expensive operations to download or generate a thumbnail, you should
     * periodically check CancellationSignal.isCanceled() to abort abandoned thumbnail requests.
     *
     * See Also:
     * DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
     *
     * @param documentId the document to return.
     * @param sizeHint hint of the optimal thumbnail dimensions.
     * @param signal used by the caller to signal if the request should be cancelled. May be null.
     * @return
     * @throws FileNotFoundException
     * @throws AuthenticationRequiredException – If authentication is required from the user (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public AssetFileDescriptor openDocumentThumbnail(final String documentId, final Point sizeHint,
                                                     final CancellationSignal signal) throws FileNotFoundException {
        if (debug) Log.i(TAG, "openDocumentThumbnail(" + documentId +"," + sizeHint + ")");

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
        Bitmap bitmap = BitmapFactory.decodeFile(documentId, options);
        // Write out the thumbnail to a temporary file
        File tempFile;
        FileOutputStream out = null;
        try {
            tempFile = File.createTempFile("thumbnail", null, getContext().getCacheDir());
            out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (IOException e) {
            Log.e(TAG, "openDocumentThumbnail " + documentId +
                    " Error writing thumbnail", e);
            return null;
        } finally {
            closeSilently(out,  "Error closing thumbnail");
        }
        // It appears the Storage Framework UI caches these results quite aggressively so there is little reason to
        // write your own caching layer beyond what you need to return a single AssetFileDescriptor
        return new AssetFileDescriptor(ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY), 0,
                AssetFileDescriptor.UNKNOWN_LENGTH);
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
        if (debug) Log.i(TAG, "isChildDocument(" + parentDocumentId + "," + documentId + ")");

        return documentId.startsWith(getDirectoryID(parentDocumentId));
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
        if (debug) Log.i(TAG, "queryChildDocuments " + parentDocumentId);

        if (ZipReadStorageProvider.isMissingReadPermission(getContext(), "queryChildDocuments")) {
            return null;
        }
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        LocalFileHeader localFileHeader;
        MountInfo mountInfo = getRepository().getById(getRootId(parentDocumentId));
        try (ZipInputStream zipInputStream = getZipInputStream(parentDocumentId, mountInfo)){
           String dir = getDirectoryID(getZipPath(parentDocumentId));
            Set<String> duplicates = new HashSet<>();
           while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
               includeLocalFileHeader(result, mountInfo.zipId, dir, localFileHeader, duplicates);
           }
        } catch (IOException ioException) {
            Log.e(TAG, "queryChildDocuments " + parentDocumentId + " " + ioException.getMessage(), ioException);
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
        if (debug) Log.i(TAG, "queryDocument " + documentId);

        if (ZipReadStorageProvider.isMissingReadPermission(getContext(), "queryDocument")) {
            return null;
        }
        // Create a cursor with either the requested fields, or the default projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);

        LocalFileHeader localFileHeader;
        MountInfo mountInfo = getRepository().getById(getRootId(documentId));
        try (ZipInputStream zipInputStream = getZipInputStream(documentId, mountInfo)){
            String zipPath = getZipPath(documentId);
            while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
                if (zipPath.equals(localFileHeader.getFileName())) {
                    includeLocalFileHeader(result, mountInfo.zipId, zipPath, localFileHeader, null);
                }
            }
        } catch (IOException ioException) {
            Log.e(TAG, "queryDocument " + documentId + ": " + ioException.getMessage(), ioException);
        }
        return result;
    }

    /** to allow unittests without an existing zip file */
    void includeLocalFileHeader(MatrixCursor result, String zipId, String dir, LocalFileHeader localFileHeader, Set<String> alreadyIncluded) {
        String relPath = getRelPath(localFileHeader, dir);
        if (relPath != null) {
            boolean isDirectory = localFileHeader.isDirectory();
            int end;
            if (!isDirectory && (end = relPath.indexOf(PATH_DELIMITER) + 1) > 0) {
                isDirectory = true;
                relPath = relPath.substring(0,end);
            }
            if (alreadyIncluded == null || !alreadyIncluded.contains(relPath)) {
                if (isDirectory) {
                    includeDir(result, zipId, relPath, dir + relPath);
                } else {
                    includeFile(result, zipId, localFileHeader, relPath);
                }
                alreadyIncluded.add(relPath);
            }
        }
    }

    private void includeDir(final MatrixCursor result, @NonNull String zipId, String dirNameWithoutPath,String zipDirPath) {
        includeResult(result, dirNameWithoutPath, getDocumentId(zipId, zipDirPath), MIME_TYPE_DIR, null, null, 0);
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
        if (mimeType.startsWith("image/"))
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;

        includeResult(result, filenameWithoutPath, getDocumentId(zipId, file.getFileName()), mimeType, file.getLastModifiedTime(), file.getUncompressedSize(), flags);
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
        if (debug) Log.i(TAG, "getDocumentType " + documentId + " => " + mime);
        return mime;
    }

    String getDocumentTypeImpl(String documentId) {
        if (ZipReadStorageProvider.isMissingReadPermission(getContext(), "getDocumentType")) {
            return null;
        }
        File file = new File(documentId);
        if (file.isDirectory())
            return MIME_TYPE_DIR;
        // From FileProvider.getType(Uri)
        final int lastDot = file.getName().lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = file.getName().substring(lastDot + 1);
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
        if (debug) Log.i(TAG, "not implemented deleteDocument " + documentId);

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
     * @throws FileNotFoundException
     * @throws AuthenticationRequiredException – If authentication is required from the user (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public String renameDocument(final String documentId, final String displayName) throws FileNotFoundException {
        if (debug) Log.i(TAG, "not implemented renameDocument " + documentId);
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
     * @throws FileNotFoundException
     * @throws AuthenticationRequiredException – If authentication is required from the user (such as login credentials), but it is not guaranteed that the client will handle this properly.
     */
    @Override
    public ParcelFileDescriptor openDocument(final String documentId, final String mode,
                                             final CancellationSignal signal) throws FileNotFoundException {
        if (debug) Log.i(TAG, "openDocument " + documentId);

        if (ZipReadStorageProvider.isMissingReadPermission(getContext(), "openDocument")) {
            return null;
        }
        File file = new File(documentId);


        // return createPipeDescriptor(null, "openDocument " + documentId);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
    }

    // https://stackoverflow.com/questions/18212152/transfer-inputstream-to-another-service-across-process-boundaries-with-parcelf
    @Nullable
    protected ParcelFileDescriptor createPipeDescriptor(InputStream is, String dbgContext) {
        int len;
        byte[] buf = new byte[1024];
        OutputStream os = null;
        try {
            ParcelFileDescriptor[] pfd = ParcelFileDescriptor.createPipe();//  inputStreamService.inputStream();
            os = new ParcelFileDescriptor.AutoCloseOutputStream(pfd[1]); // write side of pipe
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            return pfd[0]; // read side of pipe
        } catch (IOException e) {
            Log.e(TAG, "createPipeDescriptor " + dbgContext, e );
        } finally {
            closeSilently(is,  dbgContext + " inputStream");
            closeSilently(os,  dbgContext + " outputStream");
        }
        return null;
    }

    protected void closeSilently(Closeable stream, Object dbgContext) {
        if (stream != null) {
            try { stream.close(); } catch (IOException e1) {
                Log.e(TAG, "closeSilently " + dbgContext.toString(), e1 );
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
        if (debug) Log.i(TAG, "onCreate");
        return true;
    }

    //--------------
    private static String getRelPath(LocalFileHeader localFileHeader, String zipParentDir) {
        String zipPath = (localFileHeader != null) ? localFileHeader.getFileName() : null;
        return getRelPath(zipPath, zipParentDir);
    }

    /** @return zipPath without zipParentDir or null, if zipPath is not below zipParentDir. */
    @Nullable static String getRelPath(String zipPath, String zipParentDir) {
        if (zipPath != null && zipPath.startsWith(zipParentDir)) return zipPath.substring(zipParentDir.length());
        return null;
    }

    private static MountInfoRepository getRepository() {
        if (repository == null) {
            repository = MountInfoRepository.getInstance();
        }
        return repository;
    }

    @Nullable
    private static MountInfo getMountInfo(String documentId) {
        if (documentId == null) return null;
        return getRepository().getById(getRootId(documentId));
    }

    private String getDocumentId(String zipId, String fileName) {
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
        if (end >= 0) result = documentId.substring(0,end);
        if (debug) Log.i(TAG, "getRootId(" + documentId + ") => " + result);
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
        if (debug) Log.i(TAG, "getZipPath(" + documentId + ") => " + result);
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
    private ZipInputStream getZipInputStream(String documentId, MountInfo mountInfo) throws FileNotFoundException {
        if (mountInfo != null) {
            InputStream inputStream = getContext().getContentResolver().openInputStream(Uri.parse(mountInfo.uri));
            return getZipInputStream(inputStream);
        }
        throw new FileNotFoundException(documentId);
    }

    @NonNull
    private ZipInputStream getZipInputStream(InputStream inputStream) {
        return new ZipInputStream(inputStream);
    }


    @NonNull
    private static char[] getPasswordChars(MountInfo mountInfo) {
        if (mountInfo.password == null) return null;
        return mountInfo.password.toCharArray();
    }

}
