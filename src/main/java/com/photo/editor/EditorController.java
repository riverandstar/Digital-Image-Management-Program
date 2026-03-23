package com.photo.editor;

import com.photo.controller.MainController;
import com.photo.model.ImageFile;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.Insets;

import javax.imageio.ImageIO;
import java.io.File;

public class EditorController {

    @FXML private ImageView editImageView;
    @FXML private AnchorPane drawPane;
    @FXML private HBox colorPanel;
    @FXML private Slider thicknessSlider;
    @FXML private StackPane imageContainer;

    private final BrushTool brushTool = new BrushTool();
    private final BeautyTool beautyTool = new BeautyTool();
    private ImageFile currentImageFile;
    private MainController mainController;
    private boolean isBrushEnabled = false;

    public void initData(ImageFile imageFile, MainController mainController) {
        this.currentImageFile = imageFile;
        this.mainController = mainController;
        editImageView.setImage(new javafx.scene.image.Image(imageFile.getFile().toURI().toString()));
        drawPane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    // ===================== 你的逻辑：点画笔 = 显示颜色 =====================
    @FXML
    private void toggleColorPanel() {
        colorPanel.setVisible(true);
    }

    // ===================== 核心：选颜色 = 自动开启画笔！ =====================
    @FXML private void selectRed()    { brushTool.setColor(Color.RED); enableBrush(); }
    @FXML private void selectOrange() { brushTool.setColor(Color.ORANGE); enableBrush(); }
    @FXML private void selectYellow() { brushTool.setColor(Color.YELLOW); enableBrush(); }
    @FXML private void selectGreen()  { brushTool.setColor(Color.GREEN); enableBrush(); }
    @FXML private void selectCyan()   { brushTool.setColor(Color.CYAN); enableBrush(); }
    @FXML private void selectBlue()   { brushTool.setColor(Color.BLUE); enableBrush(); }
    @FXML private void selectPurple() { brushTool.setColor(Color.PURPLE); enableBrush(); }

    // 统一开启画笔
    private void enableBrush() {
        isBrushEnabled = true;
        System.out.println("✅ 已选颜色，画笔开启！现在可以画画");
    }

    // ===================== 画画 =====================
    @FXML
    private void onMouseDragged(MouseEvent event) {
        if (!isBrushEnabled) return;

        double x = event.getX();
        double y = event.getY();
        double size = thicknessSlider.getValue();

        Circle dot = new Circle(x, y, size);
        dot.setFill(brushTool.getCurrentColor());
        drawPane.getChildren().add(dot);
    }

    // ===================== 功能按钮 =====================
    @FXML private void applySharpen() { beautyTool.applySharpen(editImageView); }
    @FXML private void applyBlur()    { beautyTool.applyBlur(editImageView); }
    @FXML private void applyBrightness() { beautyTool.applyBrightness(editImageView); }
    @FXML private void applyContrast() { beautyTool.applyContrast(editImageView); }

    // ===================== 保存（修复版：图片+画一起保存） =====================
    @FXML
    private void saveImage() {
        try {
            File outputFile = currentImageFile.getFile();

            // 保存整个 StackPane（图片 + 画）
            javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(
                    (int) imageContainer.getWidth(),
                    (int) imageContainer.getHeight()
            );
            imageContainer.snapshot(null, image);

            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);
            mainController.loadDirectoryImages(mainController.getCurrentDir());
            drawPane.getScene().getWindow().hide();
            System.out.println("✅ 保存成功：原图 + 绘画都在！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}