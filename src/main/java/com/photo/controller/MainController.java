package com.photo.controller;

import com.photo.MainApp;
import com.photo.model.ImageFile;
import com.photo.utils.FileUtils;
import com.photo.utils.PDFUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    // 界面组件绑定
    @FXML
    private TreeView<File> directoryTree;
    @FXML
    private FlowPane thumbnailPane;
    @FXML
    private Label tipLabel;
    @FXML
    private Label dirInfoLabel;
    @FXML
    private Button slideShowBtn;

    // 核心数据
    private File currentDir;
    private final ObservableList<ImageFile> currentImageList = FXCollections.observableArrayList();
    private final ObservableList<VBox> selectedThumbnails = FXCollections.observableArrayList();
    private final List<File> copiedFiles = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initDirectoryTree();
        initThumbnailPaneEvent();
        slideShowBtn.setOnAction(e -> openSlideShow(0));
    }

    // 初始化目录树（只显示文件夹）
    private void initDirectoryTree() {
        // 获取系统根目录
        File[] roots = File.listRoots();
        TreeItem<File> rootItem = new TreeItem<>(null);
        rootItem.setExpanded(true);

        if (roots != null) {
            for (File root : roots) {
                TreeItem<File> rootNode = createTreeItem(root);
                rootItem.getChildren().add(rootNode);
            }
        }

        directoryTree.setRoot(rootItem);
        directoryTree.setShowRoot(false);
        // 目录点击事件
        directoryTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.getValue() != null) {
                        loadDirectoryImages(newValue.getValue());
                    }
                }
        );
    }

    // 递归创建目录树节点
    private TreeItem<File> createTreeItem(File file) {
        return new TreeItem<>(file) {
            // 懒加载，优化性能
            private boolean isLoaded = false;

            @Override
            public ObservableList<TreeItem<File>> getChildren() {
                if (!isLoaded) {
                    isLoaded = true;
                    super.getChildren().setAll(loadChildren(this));
                }
                return super.getChildren();
            }

            @Override
            public boolean isLeaf() {
                return !file.isDirectory();
            }
        };
    }

    // 加载子目录（只显示文件夹）
    private List<TreeItem<File>> loadChildren(TreeItem<File> parentItem) {
        List<TreeItem<File>> children = new ArrayList<>();
        File file = parentItem.getValue();
        if (file.isDirectory()) {
            File[] files = file.listFiles(File::isDirectory);
            if (files != null) {
                for (File childFile : files) {
                    children.add(createTreeItem(childFile));
                }
            }
        }
        return children;
    }

    // 加载指定目录的所有图片
    private void loadDirectoryImages(File dir) {
        this.currentDir = dir;
        currentImageList.clear();
        thumbnailPane.getChildren().clear();
        selectedThumbnails.clear();

        if (!dir.isDirectory()) {
            tipLabel.setText("错误：选中的不是有效目录");
            System.err.println("无效目录: " + dir.getAbsolutePath());
            return;
        }

        // 先打印目录信息，排查问题
        System.out.println("正在加载目录: " + dir.getAbsolutePath());

        // 筛选支持的图片文件
        File[] allFiles = dir.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            dirInfoLabel.setText("当前目录：" + dir.getAbsolutePath() + " | 图片数量：0");
            tipLabel.setText("当前目录为空，无任何文件");
            System.out.println("目录为空，无任何文件");
            return;
        }

        // 筛选图片文件
        List<File> imageFileList = Arrays.stream(allFiles)
                .filter(FileUtils::isImageFile)
                .toList();

        System.out.println("目录中识别到的图片文件数量: " + imageFileList.size());

        if (imageFileList.isEmpty()) {
            dirInfoLabel.setText("当前目录：" + dir.getAbsolutePath() + " | 图片数量：0");
            tipLabel.setText("当前目录无支持的图片文件（支持格式：JPG/JPEG/PNG/GIF/BMP）");
            return;
        }

        // 加载图片到列表
        long totalSize = 0;
        int successCount = 0;
        int failCount = 0;

        for (File file : imageFileList) {
            ImageFile imageFile = new ImageFile(file);
            if (imageFile.getThumbnail() != null) {
                currentImageList.add(imageFile);
                totalSize += imageFile.getFileSize();
                successCount++;
            } else {
                failCount++;
            }
        }

        // 渲染缩略图
        renderThumbnails();
        // 更新信息
        dirInfoLabel.setText("当前目录：" + dir.getAbsolutePath() + " | 图片数量：" + currentImageList.size());
        tipLabel.setText("加载完成，成功加载" + successCount + "张图片，失败" + failCount + "张，总大小：" + FileUtils.formatFileSize(totalSize));
        System.out.println("最终加载成功: " + successCount + "张，失败: " + failCount + "张");
    }

    // 渲染缩略图到预览区
    private void renderThumbnails() {
        thumbnailPane.getChildren().clear();
        for (ImageFile imageFile : currentImageList) {
            // 每个缩略图的容器
            VBox thumbnailBox = new VBox(5);
            thumbnailBox.getStyleClass().add("thumbnail-box");
            thumbnailBox.setUserData(imageFile);

            // 图片视图
            ImageView imageView = new ImageView(imageFile.getThumbnail());
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(FileUtils.THUMBNAIL_WIDTH);
            imageView.setFitHeight(FileUtils.THUMBNAIL_HEIGHT);

            // 文件名标签
            Label nameLabel = new Label(imageFile.getFileName());
            nameLabel.setMaxWidth(FileUtils.THUMBNAIL_WIDTH);
            nameLabel.setWrapText(true);

            thumbnailBox.getChildren().addAll(imageView, nameLabel);
            // 绑定点击事件
            bindThumbnailEvent(thumbnailBox);
            // 绑定右键菜单
            bindContextMenu(thumbnailBox);

            thumbnailPane.getChildren().add(thumbnailBox);
        }
    }

    // 绑定缩略图点击事件（单选、多选、双击）
    private void bindThumbnailEvent(VBox thumbnailBox) {
        thumbnailBox.setOnMouseClicked(e -> {
            // 左键点击
            if (e.getButton() == MouseButton.PRIMARY) {
                // Ctrl+点击 多选
                if (e.isControlDown()) {
                    toggleThumbnailSelected(thumbnailBox);
                }
                // 双击打开幻灯片
                else if (e.getClickCount() == 2) {
                    int index = thumbnailPane.getChildren().indexOf(thumbnailBox);
                    openSlideShow(index);
                }
                // 单选
                else {
                    clearAllSelected();
                    setThumbnailSelected(thumbnailBox, true);
                }
                // 更新选中提示
                tipLabel.setText("已选中 " + selectedThumbnails.size() + " 张图片");
            }
        });
    }

    // 初始化预览区空白点击事件（取消选中）
    private void initThumbnailPaneEvent() {
        thumbnailPane.setOnMouseClicked(e -> {
            if (e.getTarget() == thumbnailPane) {
                clearAllSelected();
                tipLabel.setText("已取消所有选中");
            }
        });

        // 框选多选功能
        thumbnailPane.setOnDragDetected(e -> thumbnailPane.startFullDrag());
    }

    // 设置缩略图选中状态
    private void setThumbnailSelected(VBox thumbnailBox, boolean selected) {
        if (selected) {
            if (!selectedThumbnails.contains(thumbnailBox)) {
                selectedThumbnails.add(thumbnailBox);
                thumbnailBox.getStyleClass().add("selected");
            }
        } else {
            selectedThumbnails.remove(thumbnailBox);
            thumbnailBox.getStyleClass().remove("selected");
        }
    }

    // 切换选中状态
    private void toggleThumbnailSelected(VBox thumbnailBox) {
        setThumbnailSelected(thumbnailBox, !selectedThumbnails.contains(thumbnailBox));
    }

    // 清除所有选中
    private void clearAllSelected() {
        for (VBox box : selectedThumbnails) {
            box.getStyleClass().remove("selected");
        }
        selectedThumbnails.clear();
    }

    // 绑定右键菜单（新增PDF转换选项）
    private void bindContextMenu(VBox thumbnailBox) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("复制");
        MenuItem renameItem = new MenuItem("重命名");
        MenuItem deleteItem = new MenuItem("删除");
        // 新增：转换为PDF选项
        MenuItem toPdfItem = new MenuItem("转换为PDF");

        // 复制事件
        copyItem.setOnAction(e -> copySelectedImages());
        // 重命名事件
        renameItem.setOnAction(e -> renameSelectedImages());
        // 删除事件
        deleteItem.setOnAction(e -> deleteSelectedImages());
        // PDF转换事件
        toPdfItem.setOnAction(e -> convertSelectedToPdf());

        contextMenu.getItems().addAll(copyItem, renameItem, deleteItem, toPdfItem);
        thumbnailBox.setOnContextMenuRequested(e -> {
            // 右键选中当前缩略图
            if (!selectedThumbnails.contains(thumbnailBox)) {
                clearAllSelected();
                setThumbnailSelected(thumbnailBox, true);
                tipLabel.setText("已选中 " + selectedThumbnails.size() + " 张图片");
            }
            contextMenu.show(thumbnailBox, e.getScreenX(), e.getScreenY());
        });
    }

    // 新增：将选中的图片转换为PDF
    @FXML
    public void convertSelectedToPdf() {
        // 1. 校验选中状态
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要转换的图片");
            return;
        }

        // 2. 收集选中的图片文件
        List<File> selectedImageFiles = new ArrayList<>();
        for (VBox box : selectedThumbnails) {
            ImageFile imageFile = (ImageFile) box.getUserData();
            selectedImageFiles.add(imageFile.getFile());
        }

        // 3. 打开文件保存对话框，让用户选择PDF保存路径
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存PDF文件");
        fileChooser.setInitialFileName("图片转换结果.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF文件 (*.pdf)", "*.pdf"));

        // 默认保存到当前目录
        if (currentDir != null) {
            fileChooser.setInitialDirectory(currentDir);
        }

        File outputPdfFile = fileChooser.showSaveDialog(MainApp.getPrimaryStage());
        if (outputPdfFile == null) {
            tipLabel.setText("已取消PDF转换");
            return;
        }

        // 4. 执行PDF转换
        try {
            PDFUtils.imagesToPdf(selectedImageFiles, outputPdfFile);
            tipLabel.setText("PDF转换成功！文件路径：" + outputPdfFile.getAbsolutePath());
        } catch (Exception e) {
            tipLabel.setText("PDF转换失败：" + e.getMessage());
            System.err.println("PDF转换异常：");
            e.printStackTrace();
            // 弹窗提示详细错误
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("转换失败");
            alert.setHeaderText("图片转PDF失败");
            alert.setContentText("错误信息：" + e.getMessage());
            alert.showAndWait();
        }
    }

    // 复制选中图片
    @FXML
    public void copySelectedImages() {
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要复制的图片");
            return;
        }
        copiedFiles.clear();
        for (VBox box : selectedThumbnails) {
            ImageFile imageFile = (ImageFile) box.getUserData();
            copiedFiles.add(imageFile.getFile());
        }
        // 系统剪贴板同步
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putFiles(copiedFiles);
        clipboard.setContent(content);

        tipLabel.setText("已复制 " + copiedFiles.size() + " 张图片");
    }

    // 粘贴图片
    @FXML
    public void pasteImages() {
        if (currentDir == null || !currentDir.isDirectory()) {
            tipLabel.setText("请先选择目标目录");
            return;
        }
        if (copiedFiles.isEmpty()) {
            tipLabel.setText("没有可粘贴的图片");
            return;
        }

        int successCount = 0;
        for (File sourceFile : copiedFiles) {
            try {
                FileUtils.copyFile(sourceFile, currentDir);
                successCount++;
            } catch (IOException e) {
                System.err.println("粘贴文件失败: " + e.getMessage());
            }
        }
        // 刷新目录
        loadDirectoryImages(currentDir);
        tipLabel.setText("粘贴完成，成功粘贴 " + successCount + " 张图片");
    }

    // 重命名选中图片
    @FXML
    public void renameSelectedImages() {
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要重命名的图片");
            return;
        }

        // 单选重命名
        if (selectedThumbnails.size() == 1) {
            ImageFile imageFile = (ImageFile) selectedThumbnails.get(0).getUserData();
            TextInputDialog dialog = new TextInputDialog(FileUtils.getFileNameWithoutExtension(imageFile.getFileName()));
            dialog.setTitle("重命名图片");
            dialog.setHeaderText("请输入新的文件名（扩展名不变）");
            dialog.setContentText("文件名：");

            dialog.showAndWait().ifPresent(newName -> {
                if (newName.isBlank()) {
                    tipLabel.setText("文件名不能为空");
                    return;
                }
                String ext = FileUtils.getFileExtension(imageFile.getFileName());
                File newFile = new File(imageFile.getFile().getParentFile(), newName + ext);
                if (imageFile.getFile().renameTo(newFile)) {
                    loadDirectoryImages(currentDir);
                    tipLabel.setText("重命名成功");
                } else {
                    tipLabel.setText("重命名失败，请检查文件名是否合法");
                }
            });
        }
        // 多选批量重命名
        else {
            // 批量重命名对话框
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("批量重命名");
            dialog.setHeaderText("请输入批量重命名规则");

            // 对话框内容
            VBox content = new VBox(10);
            TextField prefixField = new TextField("NewImage");
            TextField startNumField = new TextField("1");
            TextField digitField = new TextField("4");

            content.getChildren().addAll(
                    new Label("名称前缀："), prefixField,
                    new Label("起始编号："), startNumField,
                    new Label("编号位数："), digitField
            );
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    try {
                        String prefix = prefixField.getText().trim();
                        int startNum = Integer.parseInt(startNumField.getText().trim());
                        int digitCount = Integer.parseInt(digitField.getText().trim());

                        if (prefix.isBlank() || digitCount < 1) {
                            tipLabel.setText("参数不合法，请检查输入");
                            return;
                        }

                        // 执行批量重命名
                        int currentNum = startNum;
                        int successCount = 0;
                        for (VBox box : selectedThumbnails) {
                            ImageFile imageFile = (ImageFile) box.getUserData();
                            if (FileUtils.batchRenameFile(imageFile.getFile(), prefix, currentNum, digitCount)) {
                                successCount++;
                            }
                            currentNum++;
                        }
                        loadDirectoryImages(currentDir);
                        tipLabel.setText("批量重命名完成，成功 " + successCount + " 张，失败 " + (selectedThumbnails.size() - successCount) + " 张");
                    } catch (NumberFormatException e) {
                        tipLabel.setText("编号必须为数字，请检查输入");
                    }
                }
            });
        }
    }

    // 删除选中图片
    @FXML
    public void deleteSelectedImages() {
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要删除的图片");
            return;
        }

        // 确认对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除确认");
        alert.setHeaderText("您确定要删除选中的 " + selectedThumbnails.size() + " 张图片吗？");
        alert.setContentText("此操作将永久删除文件，无法恢复！");

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                int successCount = 0;
                for (VBox box : selectedThumbnails) {
                    ImageFile imageFile = (ImageFile) box.getUserData();
                    try {
                        Files.delete(imageFile.getFile().toPath());
                        successCount++;
                    } catch (IOException e) {
                        System.err.println("删除文件失败: " + e.getMessage());
                    }
                }
                loadDirectoryImages(currentDir);
                tipLabel.setText("删除完成，成功删除 " + successCount + " 张图片");
            }
        });
    }

    // 打开幻灯片窗口
    private void openSlideShow(int currentIndex) {
        if (currentImageList.isEmpty()) {
            tipLabel.setText("当前目录无图片，无法打开幻灯片");
            return;
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/fxml/slideshow-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
            Stage slideStage = new Stage();
            slideStage.setTitle("幻灯片播放");
            slideStage.setScene(scene);
            slideStage.initModality(Modality.APPLICATION_MODAL);
            slideStage.initOwner(MainApp.getPrimaryStage());

            // 传递数据给幻灯片控制器
            SlideShowController controller = fxmlLoader.getController();
            controller.setImageList(currentImageList, currentIndex);

            slideStage.show();
        } catch (IOException e) {
            System.err.println("打开幻灯片失败: " + e.getMessage());
            tipLabel.setText("打开幻灯片失败");
        }
    }
}