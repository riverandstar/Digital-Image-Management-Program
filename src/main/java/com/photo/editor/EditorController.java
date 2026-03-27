package com.photo.editor;

import com.photo.controller.MainController;
import com.photo.model.ImageFile;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;


import java.util.*;

public class EditorController {

    @FXML public StackPane mainEditPane;
    @FXML public ImageView editImageView;
    @FXML public AnchorPane drawPane;
    @FXML public Button btnSharpen, btnBlur, btnBrush, btnColorCycle, btnUndo, btnSave;

    private final BrushTool brushTool = new BrushTool();
    private final BeautyTool beautyTool = new BeautyTool();
    private ImageFile currentImageFile;
    private MainController mainController;

    private double lastX, lastY;

    // 颜色循环：红 → 黄 → 蓝 → 绿
    private final List<Color> colorList = Arrays.asList(Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN);
    private int colorIndex = 0;

    // 回溯历史（0.5秒保存一次）
    private final Deque<Image> imageHistory = new ArrayDeque<>();
    private final Deque<List<Shape>> drawHistory = new ArrayDeque<>();
    private long lastSaveTime = 0;
    private static final long HISTORY_DELAY = 500;

    public void initData(ImageFile imageFile, MainController mainController) {
        // 给成员变量赋值，这行是核心！
        this.currentImageFile = imageFile;
        this.mainController = mainController;

        // 加载原图
        if (imageFile != null && imageFile.getFile() != null) {
            editImageView.setImage(new javafx.scene.image.Image(imageFile.getFile().toURI().toString()));
        }

        // 初始化颜色（你原来的逻辑）
        colorIndex = 0;
        brushTool.setColor(colorList.get(colorIndex));
        updateColorButtonText();

        // 保存初始历史
        saveHistory();
    }

    // ===================== 自动保存历史 =====================
    private void saveHistory() {
        long now = System.currentTimeMillis();
        if (now - lastSaveTime < HISTORY_DELAY && !imageHistory.isEmpty()) return;

        imageHistory.push(editImageView.getImage());
        List<Shape> shapesCopy = new ArrayList<>();
        for (var node : drawPane.getChildren()) {
            if (node instanceof Shape s) shapesCopy.add(s);
        }
        drawHistory.push(shapesCopy);
        lastSaveTime = now;
    }

    // ===================== 按钮功能 =====================
    @FXML
    private void applySharpen() {
        saveHistory();
        beautyTool.applySharpen(editImageView);
    }

    @FXML
    private void applyBlur() {
        saveHistory();
        beautyTool.applyBlur(editImageView);
    }

    // 点击画笔 → 强制变回黑色
    @FXML
    private void useBrush() {
        brushTool.setColor(Color.BLACK);
        btnColorCycle.setText("彩色(黑)");
    }

    // 循环切换：红→黄→蓝→绿
    @FXML
    private void nextColor() {
        colorIndex = (colorIndex + 1) % colorList.size();
        brushTool.setColor(colorList.get(colorIndex));
        updateColorButtonText();
    }

    // 在按钮上显示当前颜色
    private void updateColorButtonText() {
        Color c = colorList.get(colorIndex);
        String name = "红";
        if (c == Color.YELLOW) name = "黄";
        else if (c == Color.BLUE) name = "蓝";
        else if (c == Color.GREEN) name = "绿";
        btnColorCycle.setText("彩色(" + name + ")");
    }

    // ===================== 一键回溯（0.5s版） =====================
    @FXML
    private void undoAll() {
        if (!imageHistory.isEmpty()) {
            editImageView.setImage(imageHistory.pop());
        }
        if (!drawHistory.isEmpty()) {
            drawPane.getChildren().setAll(drawHistory.pop());
        }
        editImageView.setEffect(null);
    }



    // ===================== 绘画 =====================
    @FXML
    private void onMousePressed(javafx.scene.input.MouseEvent e) {
        lastX = e.getX();
        lastY = e.getY();
        saveHistory();
    }

    @FXML
    private void onMouseDragged(javafx.scene.input.MouseEvent e) {
        Shape line = brushTool.drawLine(lastX, lastY, e.getX(), e.getY());
        drawPane.getChildren().add(line);
        lastX = e.getX();
        lastY = e.getY();
    }

    @FXML
    private void onMouseReleased(javafx.scene.input.MouseEvent e) {}

    @FXML
    private void saveImage() {
        try {
            // 1. 先合成最终图片（和currentImageFile无关，永远不会空）
            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(javafx.scene.paint.Color.WHITE);
            javafx.scene.image.Image finalImage = mainEditPane.snapshot(params, null);

            if (finalImage == null) {
                throw new Exception("合成图片失败，没有可保存的内容");
            }

            // 2. 打开保存窗口，让用户选路径，不依赖currentImageFile
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("保存图片");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PNG图片", "*.png")
            );

            // 只有currentImageFile不为空时，才设置默认路径和文件名
            if (currentImageFile != null && currentImageFile.getFile() != null && currentImageFile.getFile().getParentFile().exists()) {
                fileChooser.setInitialDirectory(currentImageFile.getFile().getParentFile());
                fileChooser.setInitialFileName("edited_" + currentImageFile.getFile().getName().replaceAll("\\.(jpg|jpeg|png)$", ".png"));
            } else {
                fileChooser.setInitialFileName("edited_image_" + System.currentTimeMillis() + ".png");
            }

            // 弹出保存窗口，用户取消就直接退出
            java.io.File saveFile = fileChooser.showSaveDialog(btnSave.getScene().getWindow());
            if (saveFile == null) {
                return;
            }

            // 3. 写入图片到文件
            javax.imageio.ImageIO.write(
                    javafx.embed.swing.SwingFXUtils.fromFXImage(finalImage, null),
                    "png",
                    saveFile
            );

            // 4. 成功提示
            javafx.scene.control.Alert successAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            successAlert.setTitle("保存成功");
            successAlert.setHeaderText("图片已保存");
            successAlert.setContentText("保存路径：" + saveFile.getAbsolutePath());
            successAlert.showAndWait();

            // 5. 只有主控制器和图片都不为空时，才刷新主界面
            if (mainController != null && currentImageFile != null) {
                mainController.loadDirectoryImages(mainController.getCurrentDir());
            }

            // 6. 关闭窗口
            javafx.stage.Stage stage = (javafx.stage.Stage) btnSave.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            errorAlert.setTitle("保存失败");
            errorAlert.setHeaderText("图片保存出错");
            errorAlert.setContentText("错误原因：" + e.getMessage());
            errorAlert.showAndWait();
        }
    }
}