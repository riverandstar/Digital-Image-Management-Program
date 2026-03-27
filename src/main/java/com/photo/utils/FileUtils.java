package com.photo.utils;

import javafx.scene.image.Image;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FileUtils {

    public static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            ".jpg", ".jpeg", ".gif", ".png", ".bmp"
    );

    public static final int THUMBNAIL_WIDTH = 150;
    public static final int THUMBNAIL_HEIGHT = 150;

    public static boolean isImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        return SUPPORTED_FORMATS.stream().anyMatch(fileName::endsWith);
    }

    public static Image generateThumbnail(File file) {
        try {
            Image thumbnailImage = new Image(
                    file.toURI().toString(),
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT,
                    true,
                    true,
                    false
            );

            if (thumbnailImage.isError()) {
                System.err.println("图片加载失败: " + file.getAbsolutePath() + "，错误: " + thumbnailImage.getException().getMessage());
                return null;
            }
            return thumbnailImage;
        } catch (Exception e) {
            System.err.println("生成缩略图异常: " + file.getAbsolutePath() + "，异常信息: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static File copyFile(File sourceFile, File targetDir) throws IOException {
        if (!targetDir.isDirectory()) throw new IOException("目标不是有效目录");

        String fileName = sourceFile.getName();
        File targetFile = new File(targetDir, fileName);

        int copyCount = 1;
        String nameWithoutExt = getFileNameWithoutExtension(fileName);
        String ext = getFileExtension(fileName);

        while (targetFile.exists()) {
            targetFile = new File(targetDir, nameWithoutExt + "_" + copyCount + ext);
            copyCount++;
        }

        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return targetFile;
    }

    public static boolean batchRenameFile(File file, String prefix, int number, int digitCount) {
        try {
            String ext = getFileExtension(file.getName());
            String newName = prefix + String.format("%0" + digitCount + "d", number) + ext;
            File newFile = new File(file.getParentFile(), newName);
            return file.renameTo(newFile);
        } catch (Exception e) {
            System.err.println("重命名文件失败: " + file.getAbsolutePath() + "，错误: " + e.getMessage());
            return false;
        }
    }

    public static String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex == -1 ? fileName : fileName.substring(0, lastDotIndex);
    }

    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex);
    }

    // ====================== 你要的：按类型选择图片 ======================
    public static Optional<File> selectImageByType(List<File> imageFiles, String targetType) {
        if (imageFiles == null || imageFiles.isEmpty() || targetType == null) {
            return Optional.empty();
        }
        String lowerType = targetType.toLowerCase().startsWith(".") ? targetType.toLowerCase() : "." + targetType.toLowerCase();
        return imageFiles.stream()
                .filter(FileUtils::isImageFile)
                .filter(file -> file.getName().toLowerCase().endsWith(lowerType))
                .findFirst();
    }
}