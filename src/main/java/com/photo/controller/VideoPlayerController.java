package com.photo.controller;

import com.photo.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public class VideoPlayerController {
    @FXML private MediaView videoView;
    @FXML private Slider videoSlider;
    @FXML private Button playBtn;
    @FXML private Button pauseBtn;
    @FXML private Button closeBtn;
    // 新增音量控制
    @FXML private Slider volumeSlider;

    private MediaPlayer mediaPlayer;
    private Stage videoStage;
    // 新增：最大缩放比例（如果需要）
    private static final double MAX_SCALE = 3.0;
    private double scale = 1.0;

    public void setVideoFile(File file) {
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        videoView.setMediaPlayer(mediaPlayer);

        // 关键：设置视频自适应，优先适配窗口（而非固定主窗口尺寸）
        videoView.setPreserveRatio(true);
        // 获取视频窗口（通过closeBtn的场景）
        videoStage = (Stage) closeBtn.getScene().getWindow();
        // 监听窗口尺寸变化，动态调整视频大小
        videoStage.widthProperty().addListener((obs, oldVal, newVal) -> adjustVideoSize());
        videoStage.heightProperty().addListener((obs, oldVal, newVal) -> adjustVideoSize());

        // 初始化视频尺寸（适配窗口初始大小）
        adjustVideoSize();

        // 进度条逻辑
        mediaPlayer.currentTimeProperty().addListener((a,b,newTime)->{
            if (!videoSlider.isValueChanging()) {
                double total = mediaPlayer.getTotalDuration().toSeconds();
                if (total > 0) videoSlider.setValue(newTime.toSeconds() / total * 100);
            }
        });

        videoSlider.valueProperty().addListener((a,b,newVal)->{
            if (videoSlider.isValueChanging() && mediaPlayer != null) {
                double total = mediaPlayer.getTotalDuration().toSeconds();
                mediaPlayer.seek(Duration.seconds(total * newVal.doubleValue() / 100));
            }
        });

        // 新增音量控制
        volumeSlider.setMin(0.0);
        volumeSlider.setMax(1.0);
        volumeSlider.setValue(0.8);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });
        mediaPlayer.setVolume(volumeSlider.getValue());

        playBtn.setOnAction(e -> {
            mediaPlayer.play();
            playBtn.setDisable(true);
            pauseBtn.setDisable(false);
        });
        pauseBtn.setOnAction(e -> {
            mediaPlayer.pause();
            pauseBtn.setDisable(true);
            playBtn.setDisable(false);
        });
        closeBtn.setOnAction(e -> {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            videoStage.close();
        });

        mediaPlayer.play();
        playBtn.setDisable(true);
        pauseBtn.setDisable(false);
    }

    // 适配视频大小到窗口（预留控件区域，避免遮挡）
    private void adjustVideoSize() {
        if (videoStage == null) return;
        // 减去控件区域的高度（按钮+滑块约80px），宽度预留20px边距
        double availableWidth = (videoStage.getWidth() - 20) * scale;
        double availableHeight = (videoStage.getHeight() - 80) * scale;

        // 限制最大缩放
        availableWidth = Math.min(availableWidth, videoStage.getWidth() * MAX_SCALE);
        availableHeight = Math.min(availableHeight, videoStage.getHeight() * MAX_SCALE);

        videoView.setFitWidth(availableWidth);
        videoView.setFitHeight(availableHeight);
    }

    // 新增缩放方法（如果需要）
    @FXML
    public void zoomIn() {
        if (scale < MAX_SCALE) {
            scale += 0.1;
            adjustVideoSize();
        }
    }

    @FXML
    public void zoomOut() {
        scale = Math.max(0.1, scale - 0.1);
        adjustVideoSize();
    }
}