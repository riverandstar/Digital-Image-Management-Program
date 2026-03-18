package com.photo.utils;

import javafx.scene.image.Image;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class FileUtils {
    // 支持的图片格式（全小写，匹配时统一转小写）
    public static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            ".jpg", ".jpeg", ".gif", ".png", ".bmp"
    );

    // 缩略图固定宽高
    public static final int THUMBNAIL_WIDTH = 150;
    public static final int THUMBNAIL_HEIGHT = 150;

    // 判断是否为支持的图片文件（增强鲁棒性）
    public static boolean isImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) return false;
        String fileName = file.getName().toLowerCase();
        return SUPPORTED_FORMATS.stream().anyMatch(fileName::endsWith);
    }

    // 生成保持比例的缩略图（纯JavaFX实现，同步加载，无Swing依赖）
    public static Image generateThumbnail(File file) {
        try {
            // 同步加载图片，不使用后台异步加载，确保加载完成再处理
            // 直接按缩略图尺寸加载，自动缩放，节省内存，提升性能
            Image thumbnailImage = new Image(
                    file.toURI().toString(),
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT,
                    true, // 保持宽高比
                    true, // 高质量平滑缩放
                    false // 禁用后台异步加载，同步加载
            );

            // 检查图片是否加载失败
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

    // 计算文件大小（格式化输出）
    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    // 复制文件，重名自动重命名
    public static File copyFile(File sourceFile, File targetDir) throws IOException {
        if (!targetDir.isDirectory()) throw new IOException("目标不是有效目录");

        String fileName = sourceFile.getName();
        File targetFile = new File(targetDir, fileName);

        // 重名处理
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

    // 批量重命名文件
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

    // 获取文件名（不含后缀）
    public static String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex == -1 ? fileName : fileName.substring(0, lastDotIndex);
    }

    // 获取文件后缀
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex);
    }
}