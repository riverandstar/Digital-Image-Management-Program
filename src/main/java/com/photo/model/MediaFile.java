package com.photo.model;

import com.photo.utils.FileUtils;
import javafx.scene.image.Image;
import java.io.File;

// 重命名为MediaFile，支持图片+视频
public class MediaFile {
    private final File file;
    private final Image thumbnail;
    private final String fileName;
    private final long fileSize;
    private final boolean isImage; // 区分图片/视频
    private final boolean isVideo;

    public MediaFile(File file) {
        this.file = file;
        this.fileName = file.getName();
        this.fileSize = file.length();
        this.isImage = FileUtils.isImageFile(file);
        this.isVideo = FileUtils.isVideoFile(file);
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

    public boolean isImage() {
        return isImage;
    }

    public boolean isVideo() {
        return isVideo;
    }
}