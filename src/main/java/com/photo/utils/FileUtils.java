package com.photo.utils;

import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.embed.swing.SwingFXUtils;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class FileUtils {
    // 支持的图片格式（全小写）
    public static final List<String> SUPPORTED_IMAGE_FORMATS = Arrays.asList(
            ".jpg", ".jpeg", ".gif", ".png", ".bmp"
    );

    // 支持的视频格式（全小写）
    public static final List<String> SUPPORTED_VIDEO_FORMATS = Arrays.asList(
            ".mp4", ".avi", ".mov", ".mkv", ".flv", ".wmv"
    );

    // 缩略图固定宽高
    public static final int THUMBNAIL_WIDTH = 150;
    public static final int THUMBNAIL_HEIGHT = 150;

    // 判断是否为支持的图片文件
    public static boolean isImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) return false;
        String fileName = file.getName().toLowerCase();
        return SUPPORTED_IMAGE_FORMATS.stream().anyMatch(fileName::endsWith);
    }

    // 判断是否为支持的视频文件
    public static boolean isVideoFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) return false;
        String fileName = file.getName().toLowerCase();
        return SUPPORTED_VIDEO_FORMATS.stream().anyMatch(fileName::endsWith);
    }

    // 判断是否为支持的媒体文件（图片+视频）
    public static boolean isMediaFile(File file) {
        return isImageFile(file) || isVideoFile(file);
    }

    // 生成图片/视频缩略图
    public static Image generateThumbnail(File file) {
        if (isImageFile(file)) {
            return generateImageThumbnail(file);
        } else if (isVideoFile(file)) {
            return generateVideoThumbnail(file);
        }
        return null;
    }

    // 生成图片缩略图（原有逻辑）
    private static Image generateImageThumbnail(File file) {
        try {
            Image thumbnailImage = new Image(
                    file.toURI().toString(),
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT,
                    true, // 保持宽高比
                    true, // 高质量平滑缩放
                    false // 同步加载
            );

            if (thumbnailImage.isError()) {
                System.err.println("图片加载失败: " + file.getAbsolutePath() + "，错误: " + thumbnailImage.getException().getMessage());
                return null;
            }

            return thumbnailImage;
        } catch (Exception e) {
            System.err.println("生成图片缩略图异常: " + file.getAbsolutePath() + "，异常信息: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 生成视频缩略图（提取第一帧）
    private static Image generateVideoThumbnail(File file) {
        try {
            Media media = new Media(file.toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);

            // 等待媒体加载完成
            mediaPlayer.setOnReady(() -> {
                mediaPlayer.seek(Duration.seconds(1)); // 定位到1秒处（避免黑屏）
            });

            // 同步等待视频帧加载（阻塞当前线程，确保获取到帧）
            Thread.sleep(1000);

            // 捕获视频帧并转为Image
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(mediaView.snapshot(null, null), null);
            if (bufferedImage == null) {
                // 备用：使用默认视频图标
                return new Image(FileUtils.class.getResourceAsStream("/icons/video-placeholder.png"),
                        THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true, true);
            }

            // 缩放为指定尺寸
            BufferedImage scaledImage = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            scaledImage.getGraphics().drawImage(bufferedImage.getScaledInstance(
                    THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, java.awt.Image.SCALE_SMOOTH), 0, 0, null);

            // 转为JavaFX Image
            Image thumbnail = SwingFXUtils.toFXImage(scaledImage, null);

            // 释放资源
            mediaPlayer.stop();
            mediaPlayer.dispose();

            return thumbnail;
        } catch (Exception e) {
            System.err.println("生成视频缩略图异常: " + file.getAbsolutePath() + "，异常信息: " + e.getMessage());
            e.printStackTrace();
            // 返回默认视频占位图
            try {
                return new Image(FileUtils.class.getResourceAsStream("/icons/video-placeholder.png"),
                        THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true, true);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    // 原有工具方法（保持不变）
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
}