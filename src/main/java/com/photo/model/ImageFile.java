package com.photo.model;

import com.photo.utils.FileUtils;
import javafx.scene.image.Image;
import java.io.File;

public class ImageFile {
    private final File file;
    private final Image thumbnail;
    private final String fileName;
    private final long fileSize;

    public ImageFile(File file) {
        this.file = file;
        this.fileName = file.getName();
        this.fileSize = file.length();
        this.thumbnail = FileUtils.generateThumbnail(file);
    }

    // Getter方法
    public File getFile() {
        return file;
    }

    public Image getThumbnail() {
        return thumbnail;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }
}