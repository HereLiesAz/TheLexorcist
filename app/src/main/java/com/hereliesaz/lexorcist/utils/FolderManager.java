package com.hereliesaz.lexorcist.utils;

import android.util.Log;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class FolderManager {

    private static final String TAG = "FolderManager";
    private final Drive googleDriveService;

    public FolderManager(Drive googleDriveService) {
        this.googleDriveService = googleDriveService;
    }

    public String getOrCreateFolder(String folderName, String parentFolderId) throws IOException {
        String folderId = getFolderId(folderName, parentFolderId);
        if (folderId == null) {
            folderId = createFolder(folderName, parentFolderId);
        }
        return folderId;
    }

    private String getFolderId(String folderName, String parentFolderId) throws IOException {
        String query;
        if (parentFolderId == null) {
            query = "name = '" + folderName + "' and mimeType = 'application/vnd.google-apps.folder' and 'root' in parents and trashed = false";
        } else {
            query = "name = '" + folderName + "' and mimeType = 'application/vnd.google-apps.folder' and '" + parentFolderId + "' in parents and trashed = false";
        }

        FileList result = googleDriveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            return null;
        }
        return files.get(0).getId();
    }

    private String createFolder(String folderName, String parentFolderId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        if (parentFolderId != null) {
            fileMetadata.setParents(Collections.singletonList(parentFolderId));
        }

        File file = googleDriveService.files().create(fileMetadata)
                .setFields("id")
                .execute();
        Log.d(TAG, "Folder ID: " + file.getId());
        return file.getId();
    }
}
