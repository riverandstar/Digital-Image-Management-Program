package com.photo.editor;

import com.photo.controller.MainController;
import com.photo.model.ImageFile;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import javax.imageio.ImageIO;
import java.io.File;

public class EditorController {

    // ===================== FXML 绑定 =====================
    @FXML private ImageView editImageView;   // 图片
    @FXML private AnchorPane drawPane;       // 绘画层
    @FXML private HBox colorPanel;           // 七色面板（默认隐藏）
    @FXML private Slider thicknessSlider;     // 粗细滑块

    // ===================== 工具类 =====================
    private final BrushTool brushTool = new BrushTool();
    private final BeautyTool beautyTool = new BeautyTool(); // ✅ 已改成 BeautyTool

    // ===================== 外部传入的数据 =====================
    private ImageFile currentImageFile;
    private MainController mainController;

    // ===================== 外部调用：传入图片 =====================
    public void initData(ImageFile imageFile, MainController mainController) {
        this.currentImageFile = imageFile;
        this.mainController = mainController;
        editImageView.setImage(new javafx.scene.image.Image(imageFile.getFile().toURI().toString()));
    }

    // ===================== 颜色面板显示/隐藏 =====================
    @FXML private void toggleColorPanel() {
        colorPanel.setVisible(!colorPanel.isVisible());
    }

    // ===================== 7种颜色选择 =====================
    @FXML private void selectRed()    { brushTool.setColor(BrushTool.RED); }
    @FXML private void selectOrange() { brushTool.setColor(BrushTool.ORANGE); }
    @FXML private void selectYellow() { brushTool.setColor(BrushTool.YELLOW); }
    @FXML private void selectGreen()  { brushTool.setColor(BrushTool.GREEN); }
    @FXML private void selectCyan()   { brushTool.setColor(BrushTool.CYAN); }
    @FXML private void selectBlue()   { brushTool.setColor(BrushTool.BLUE); }
    @FXML private void selectPurple() { brushTool.setColor(BrushTool.PURPLE); }

    // ===================== 画笔绘画 =====================
    @FXML private void startDraw() {
        drawPane.setOnMouseDragged(event -> {
            brushTool.setThickness(thicknessSlider.getValue());
            drawPane.getChildren().add(
                    brushTool.draw(event.getX(), event.getY())
            );
        });
    }

    // ===================== 4个美化功能（全部走 BeautyTool） =====================
    @FXML private void applySharpen() {
        beautyTool.applySharpen(editImageView);
    }

    @FXML private void applyBlur() {
        beautyTool.applyBlur(editImageView);
    }

    @FXML private void applyBrightness() {
        beautyTool.applyBrightness(editImageView);
    }

    @FXML private void applyContrast() {
        beautyTool.applyContrast(editImageView);
    }

    // ===================== 保存并同步到主界面 =====================
    @FXML private void saveImage() {
        try {
            File outputFile = currentImageFile.getFile();

            // 把绘画内容 + 图片一起保存
            javafx.scene.image.WritableImage snapshot = new javafx.scene.image.WritableImage(
                    (int) drawPane.getWidth(),
                    (int) drawPane.getHeight()
            );
            drawPane.snapshot(null, snapshot);
            ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", outputFile);

            // 保存后刷新主界面
            mainController.loadDirectoryImages(mainController.getCurrentDir());

            // 关闭编辑器
            drawPane.getScene().getWindow().hide();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}