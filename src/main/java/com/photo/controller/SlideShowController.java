package com.photo.controller;

import com.photo.model.MediaFile;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SlideShowController implements Initializable {

    @FXML private StackPane mediaContainer;
    @FXML private ImageView imageView;
    @FXML private MediaView mediaView;
    @FXML private Label pageLabel;
    @FXML private Label tipLabel;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Button zoomInBtn;
    @FXML private Button zoomOutBtn;
    @FXML private Button playBtn;
    @FXML private Button stopBtn;
    @FXML private Button videoPlayPauseBtn;
    @FXML private Slider videoProgressSlider;
    @FXML private Slider volumeSlider;

    private List<MediaFile> mediaList;
    private int currentIndex;
    private double scale = 1.0;
    private static final double MAX_SCALE = 3.0;
    private Timeline autoPlay;
    private MediaPlayer mediaPlayer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        mediaView.setVisible(false);
        imageView.setVisible(true);

        imageView.setPreserveRatio(true);
        mediaView.setPreserveRatio(true);

        mediaContainer.widthProperty().addListener((obs, oldVal, newVal) -> adjustMediaSize());
        mediaContainer.heightProperty().addListener((obs, oldVal, newVal) -> adjustMediaSize());

        autoPlay = new Timeline(new javafx.animation.KeyFrame(Duration.seconds(3), e -> next()));
        autoPlay.setCycleCount(Timeline.INDEFINITE);

        prevBtn.setOnAction(e -> prev());
        nextBtn.setOnAction(e -> next());

        zoomInBtn.setOnAction(e -> {
            if (scale < MAX_SCALE) {
                scale += 0.1;
                adjustMediaSize();
            }
        });
        zoomOutBtn.setOnAction(e -> {
            scale = Math.max(0.5, scale - 0.1);
            adjustMediaSize();
        });

        playBtn.setOnAction(e -> {
            autoPlay.play();
            tipLabel.setText("已开启自动播放");
        });
        stopBtn.setOnAction(e -> {
            autoPlay.stop();
            if (mediaPlayer != null) mediaPlayer.pause();
            tipLabel.setText("已停止");
        });

        videoPlayPauseBtn.setOnAction(e -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.play();
                }
            }
        });

        videoProgressSlider.valueProperty().addListener((obs, old, newVal) -> {
            if (videoProgressSlider.isValueChanging() && mediaPlayer != null) {
                double total = mediaPlayer.getTotalDuration().toSeconds();
                mediaPlayer.seek(Duration.seconds(total * newVal.doubleValue() / 100));
            }
        });

        volumeSlider.setValue(0.8);
        volumeSlider.valueProperty().addListener((obs, old, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });
    }

    public void setMediaList(List<MediaFile> list, int index) {
        if (list == null || list.isEmpty()) return;
        this.mediaList = list;
        this.currentIndex = Math.max(0, Math.min(index, list.size() - 1));
        show();
    }

    private void show() {
        // ====================== 【重要修改】切换时自动关闭上一个视频 ======================
        closeMedia(); // 每次切换前强制关闭视频

        MediaFile mf = mediaList.get(currentIndex);
        pageLabel.setText((currentIndex + 1) + " / " + mediaList.size());
        tipLabel.setText("文件：" + mf.getFileName());

        if (mf.isImage()) {
            imageView.setVisible(true);
            mediaView.setVisible(false);
            imageView.setImage(new Image(mf.getFile().toURI().toString(), true));
        } else {
            imageView.setVisible(false);
            mediaView.setVisible(true);
            Media media = new Media(mf.getFile().toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.play();
        }
        adjustMediaSize();
    }

    private void adjustMediaSize() {
        double w = mediaContainer.getWidth() * scale;
        double h = mediaContainer.getHeight() * scale;
        imageView.setFitWidth(w);
        imageView.setFitHeight(h);
        mediaView.setFitWidth(w);
        mediaView.setFitHeight(h);
    }

    // ====================== 上一个（自动关闭视频） ======================
    private void prev() {
        if (currentIndex > 0) {
            currentIndex--;
            show();
        }
    }

    // ====================== 下一个（自动关闭视频） ======================
    private void next() {
        if (currentIndex < mediaList.size() - 1) {
            currentIndex++;
            show();
        }
    }

    // ====================== 【新增】安全关闭视频 ======================
    private void closeMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        scale = 1.0;
    }
}