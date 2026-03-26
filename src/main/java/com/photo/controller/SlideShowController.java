package com.photo.controller;

import com.photo.editor.EditorController;
import com.photo.model.ImageFile;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SlideShowController implements Initializable {
    // 界面组件绑定
    @FXML
    private ImageView imageView;
    @FXML
    private Label tipLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private Button prevBtn;
    @FXML
    private Button nextBtn;
    @FXML
    private Button zoomInBtn;
    @FXML
    private Button zoomOutBtn;
    @FXML
    private Button playBtn;
    @FXML
    private Button stopBtn;

    // 核心数据
    private MainController mainController;
    private List<ImageFile> imageList;
    private int currentIndex;
    private double currentScale = 1.0;
    private final double SCALE_STEP = 0.2;
    private Timeline playTimeline;
    private boolean isPlaying = false;
    private int fixedWidth = 1100;
    private int fixedHeight = 600;
    public void setFixedImageSize(int width, int height) {
        this.fixedWidth = width;
        this.fixedHeight = height;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> autoPlayNext()));
        playTimeline.setCycleCount(Timeline.INDEFINITE);
        stopBtn.setDisable(true);

        prevBtn.setOnAction(e -> showPrevImage());
        nextBtn.setOnAction(e -> showNextImage());
        zoomInBtn.setOnAction(e -> zoomIn());
        zoomOutBtn.setOnAction(e -> zoomOut());
        playBtn.setOnAction(e -> startPlay());
        stopBtn.setOnAction(e -> stopPlay());


        imageView.setFitWidth(1100);
        imageView.setFitHeight(600);
        imageView.setPreserveRatio(true);
    }

    // 接收主界面传递的图片列表和当前索引
    public void setImageList(List<ImageFile> imageList, int currentIndex) {
        this.imageList = imageList;
        this.currentIndex = currentIndex;
        showCurrentImage();
    }

    // 显示当前索引的图片
    private void showCurrentImage() {
        if (imageList == null || imageList.isEmpty()) return;

        ImageFile currentImage = imageList.get(currentIndex);
        //  加载原图，保持比例
        Image image = new Image(
                currentImage.getFile().toURI().toString(),
                1100,
                600,
                true,  // 保持比例
                true,
                false
        );
        imageView.setImage(image);

        currentScale = 1.0;
        imageView.setScaleX(currentScale);
        imageView.setScaleY(currentScale);

        pageLabel.setText((currentIndex + 1) + " / " + imageList.size());
        tipLabel.setText("当前图片：" + currentImage.getFileName() + " | 大小：" + currentImage.getFile().length() / 1024 + " KB");

        prevBtn.setDisable(currentIndex == 0);
        nextBtn.setDisable(currentIndex == imageList.size() - 1);

        if (currentIndex == 0) {
            tipLabel.setText(tipLabel.getText() + " | 已经是第一张图片");
        } else if (currentIndex == imageList.size() - 1) {
            tipLabel.setText(tipLabel.getText() + " | 已经是最后一张图片");
        }
    }
    // 上一张图片
    private void showPrevImage() {
        if (currentIndex > 0) {
            currentIndex--;
            showCurrentImage();
        }
    }

    // 下一张图片
    private void showNextImage() {
        if (currentIndex < imageList.size() - 1) {
            currentIndex++;
            showCurrentImage();
        }
    }

    // 放大图片
    private void zoomIn() {
        currentScale += SCALE_STEP;
        imageView.setScaleX(currentScale);
        imageView.setScaleY(currentScale);
        tipLabel.setText("当前缩放比例：" + String.format("%.1f", currentScale * 100) + "%");
    }

    // 缩小图片
    private void zoomOut() {
        if (currentScale > SCALE_STEP) {
            currentScale -= SCALE_STEP;
            imageView.setScaleX(currentScale);
            imageView.setScaleY(currentScale);
            tipLabel.setText("当前缩放比例：" + String.format("%.1f", currentScale * 100) + "%");
        }
    }

    // 开始自动播放
    private void startPlay() {
        if (!isPlaying) {
            isPlaying = true;
            playTimeline.play();
            playBtn.setDisable(true);
            stopBtn.setDisable(false);
            tipLabel.setText("自动播放已启动，1秒切换一张");
        }
    }

    // 停止自动播放
    private void stopPlay() {
        if (isPlaying) {
            isPlaying = false;
            playTimeline.stop();
            playBtn.setDisable(false);
            stopBtn.setDisable(true);
            tipLabel.setText("自动播放已停止");
        }
    }

    // 自动播放切换下一张
    private void autoPlayNext() {
        if (currentIndex < imageList.size() - 1) {
            showNextImage();
        } else {
            // 到最后一张停止播放
            stopPlay();
            tipLabel.setText("播放完毕，已到最后一张图片");
        }
    }

    @FXML
    private void openEditorController() {
        try {
            FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/editor.fxml")
            );
            Scene scene = new javafx.scene.Scene(loader.load(), 900, 650);

            EditorController editorcontroller = loader.getController();
            editorcontroller.initData(imageList.get(currentIndex), mainController);

            Stage stage = new Stage();
            stage.setTitle("图片编辑器");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 窗口关闭时停止播放
    public void onWindowClose() {
        stopPlay();
    }
}