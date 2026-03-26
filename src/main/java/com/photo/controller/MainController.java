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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
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
    @FXML
    private TextField pathField;

    @SuppressWarnings("unused")
    @FXML
    private Button jumpBtn;

    @FXML
    private TreeView<File> directoryTree;
    @FXML
    private FlowPane thumbnailPane;
    @FXML
    private Label tipLabel;
    @FXML
    private Label dirInfoLabel;

    private File currentDir;
    private final ObservableList<ImageFile> currentImageList = FXCollections.observableArrayList();
    private final ObservableList<VBox> selectedThumbnails = FXCollections.observableArrayList();
    private final List<File> copiedFiles = new ArrayList<>();


    private ContextMenu currentOpenMenu;

    private Image cepanImage;
    private Image zhuomianImage;
    private Image wenjianjiaImage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initIcons();
        initDirectoryTree();
        initThumbnailPaneEvent();
        initPathBar();
    }

    @SuppressWarnings("ConstantConditions")
    private void initIcons() {
        try {
            zhuomianImage = new Image(getClass().getResourceAsStream("/images/zhuomian.png"));
            cepanImage = new Image(getClass().getResourceAsStream("/images/cepan.png"));
            wenjianjiaImage = new Image(getClass().getResourceAsStream("/images/wenjianjia.png"));
        } catch (Exception e) {
            try {
                cepanImage = new Image(getClass().getResourceAsStream("/com/sun/javafx/scene/control/skin/icons/disk.png"));
                wenjianjiaImage = new Image(getClass().getResourceAsStream("/com/sun/javafx/scene/control/skin/icons/folder.png"));
                zhuomianImage = wenjianjiaImage;
            } catch (Exception ex) {
                cepanImage = null;
                zhuomianImage = null;
                wenjianjiaImage = null;
            }
        }

        if (zhuomianImage != null && zhuomianImage.isError()) zhuomianImage = null;
        if (cepanImage != null && cepanImage.isError()) cepanImage = null;
        if (wenjianjiaImage != null && wenjianjiaImage.isError()) wenjianjiaImage = null;
    }

    private void initPathBar() {
        pathField.setOnAction(e -> jumpToPath());
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (desktop.exists()) {
            pathField.setText(desktop.getAbsolutePath());
            loadDirectoryImages(desktop);
        }
    }

    @FXML
    public void jumpToPath() {
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            tipLabel.setText("路径不能为空");
            return;
        }
        File targetDir = new File(path);
        if (targetDir.exists() && targetDir.isDirectory()) {
            loadDirectoryImages(targetDir);
            updateTreeSelection();
            tipLabel.setText("成功跳转到：" + path);
        } else {
            tipLabel.setText("路径无效：" + path);
        }
    }

    private void updateTreeSelection() {
        directoryTree.getSelectionModel().clearSelection();
    }

    private void initDirectoryTree() {
        TreeItem<File> rootItem = new TreeItem<>(null);
        rootItem.setExpanded(true);

        File desktopDir = new File(System.getProperty("user.home") + "/Desktop");
        TreeItem<File> desktopItem = createTreeItemWithIcon(desktopDir, zhuomianImage);
        rootItem.getChildren().add(desktopItem);

        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                TreeItem<File> rootNode = createTreeItemWithIcon(root, cepanImage);
                rootItem.getChildren().add(rootNode);
            }
        }

        directoryTree.setRoot(rootItem);
        directoryTree.setShowRoot(false);

        directoryTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                TreeItem<File> treeItem = getTreeItem();
                if (treeItem != null) {
                    setGraphic(treeItem.getGraphic());
                }

                String displayName = getRelativeFileName(file);
                displayName = removeDuplicateFileNamePrefix(displayName);
                setText(displayName);
            }
        });

        directoryTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.getValue() != null) {
                        File selectedDir = newValue.getValue();
                        loadDirectoryImages(selectedDir);
                        pathField.setText(selectedDir.getAbsolutePath());
                    }
                }
        );
    }

    // ==================== 已恢复文件夹图标 ====================
    private TreeItem<File> createTreeItemWithIcon(File file, Image iconImage) {
        TreeItem<File> item = new TreeItem<>(file) {
            private boolean isLoaded = false;

            @Override
            public ObservableList<TreeItem<File>> getChildren() {
                if (!isLoaded) {
                    isLoaded = true;
                    super.getChildren().setAll(loadChildrenWithIcon(this));
                }
                return super.getChildren();
            }

            @Override
            public boolean isLeaf() {
                return !file.isDirectory();
            }
        };

        HBox hbox = new HBox(5);
        ImageView iconView = new ImageView();

        // 磁盘 + 桌面 + 所有文件夹 → 都显示图标
        if (iconImage != null) {
            iconView.setImage(iconImage);
            iconView.setPreserveRatio(true);
            iconView.setFitHeight(16);
            iconView.setFitWidth(16);
        } else {
            iconView.setVisible(false);
            iconView.setManaged(false);
        }

        hbox.getChildren().add(iconView);
        item.setGraphic(hbox);

        return item;
    }

    private String getRelativeFileName(File file) {
        if (file == null) return "";
        if (file.getParent() == null) {
            return file.getAbsolutePath();
        }
        return file.getName();
    }

    private String removeDuplicateFileNamePrefix(String fileName) {
        if (fileName == null || fileName.isEmpty()) return fileName;

        int lastDot = fileName.lastIndexOf('.');
        String name = lastDot == -1 ? fileName : fileName.substring(0, lastDot);
        String ext = lastDot == -1 ? "" : fileName.substring(lastDot);

        int len = name.length();
        for (int i = 1; i <= len / 2; i++) {
            String p = name.substring(0, i);
            if (name.substring(i).startsWith(p)) {
                name = name.substring(i);
                return removeDuplicateFileNamePrefix(name + ext);
            }
        }
        return name + ext;
    }

    private List<TreeItem<File>> loadChildrenWithIcon(TreeItem<File> parentItem) {
        List<TreeItem<File>> children = new ArrayList<>();
        File file = parentItem.getValue();
        if (file.isDirectory()) {
            File[] files = file.listFiles(File::isDirectory);
            if (files != null) {
                for (File childFile : files) {
                    TreeItem<File> childItem = createTreeItemWithIcon(childFile, wenjianjiaImage);
                    children.add(childItem);
                }
            }
        }
        return children;
    }

    public void loadDirectoryImages(File dir) {
        this.currentDir = dir;
        currentImageList.clear();
        thumbnailPane.getChildren().clear();
        selectedThumbnails.clear();

        if (!dir.isDirectory()) {
            tipLabel.setText("错误：选中的不是有效目录");
            return;
        }

        File[] allFiles = dir.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            dirInfoLabel.setText("当前目录：" + dir.getAbsolutePath() + " | 图片数量：0");
            tipLabel.setText("当前目录为空，无任何文件");
            return;
        }

        List<File> imageFileList = Arrays.stream(allFiles)
                .filter(FileUtils::isImageFile)
                .toList();

        if (imageFileList.isEmpty()) {
            dirInfoLabel.setText("当前目录：" + dir.getAbsolutePath() + " | 图片数量：0");
            tipLabel.setText("当前目录无支持的图片文件（支持格式：JPG/JPEG/PNG/GIF/BMP）");
            return;
        }

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

        renderThumbnails();
        dirInfoLabel.setText("当前目录：" + dir.getAbsolutePath() + " | 图片数量：" + currentImageList.size());
        tipLabel.setText("加载完成，成功加载" + successCount + "张图片，失败" + failCount + "张，总大小：" + FileUtils.formatFileSize(totalSize));
    }

    private void renderThumbnails() {
        thumbnailPane.getChildren().clear();
        for (ImageFile imageFile : currentImageList) {
            VBox thumbnailBox = new VBox(5);
            thumbnailBox.getStyleClass().add("thumbnail-box");
            thumbnailBox.setUserData(imageFile);

            // 固定容器大小
            thumbnailBox.setPrefWidth(200);
            thumbnailBox.setPrefHeight(120);
            thumbnailBox.setMaxWidth(200);
            thumbnailBox.setMaxHeight(120);

            // ========== 核心：让图片居中 ==========
            thumbnailBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            // ====================================

            ImageView imageView = new ImageView(imageFile.getThumbnail());
            // 保持比例 + 固定区域
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(200);
            imageView.setFitHeight(100);
            imageView.setSmooth(true);

            Label nameLabel = new Label(imageFile.getFileName());
            nameLabel.setMaxWidth(200);
            nameLabel.setWrapText(true);
            nameLabel.setAlignment(javafx.geometry.Pos.CENTER);

            thumbnailBox.getChildren().addAll(imageView, nameLabel);
            bindThumbnailEvent(thumbnailBox);
            bindContextMenu(thumbnailBox);

            thumbnailPane.getChildren().add(thumbnailBox);
        }
    }
    private void bindThumbnailEvent(VBox thumbnailBox) {
        thumbnailBox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.isControlDown()) {
                    toggleThumbnailSelected(thumbnailBox);
                } else if (e.getClickCount() == 2) {
                    int index = thumbnailPane.getChildren().indexOf(thumbnailBox);
                    openSlideShow(index);
                } else {
                    clearAllSelected();
                    setThumbnailSelected(thumbnailBox, true);
                }
                tipLabel.setText("已选中 " + selectedThumbnails.size() + " 张图片");
            }
        });
    }

    /**
     * 创建公用的右键菜单（复制、粘贴、重命名、删除、转换为PDF）
     */
    private ContextMenu createCommonContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem copyItem = new MenuItem("复制");
        MenuItem pasteItem = new MenuItem("粘贴");
        MenuItem renameItem = new MenuItem("重命名");
        MenuItem deleteItem = new MenuItem("删除");
        MenuItem toPdfItem = new MenuItem("转换为PDF");

        copyItem.setOnAction(e -> copySelectedImages());
        pasteItem.setOnAction(e -> pasteImages());
        renameItem.setOnAction(e -> renameSelectedImages());
        deleteItem.setOnAction(e -> deleteSelectedImages());
        toPdfItem.setOnAction(e -> convertSelectedToPdf());

        menu.getItems().addAll(copyItem, pasteItem, renameItem, deleteItem, toPdfItem);
        return menu;
    }
    // 修改后的 initThumbnailPaneEvent
    private void initThumbnailPaneEvent() {
        thumbnailPane.setOnMouseClicked(e -> {
            if (e.getTarget() == thumbnailPane) {
                clearAllSelected();
                tipLabel.setText("已取消所有选中");
                // 关闭可能打开的菜单
                if (currentOpenMenu != null && currentOpenMenu.isShowing()) {
                    currentOpenMenu.hide();
                }
            }
        });

        thumbnailPane.setOnDragDetected(e -> thumbnailPane.startFullDrag());

        thumbnailPane.setOnContextMenuRequested(e -> {
            // 关闭之前的菜单
            if (currentOpenMenu != null && currentOpenMenu.isShowing()) {
                currentOpenMenu.hide();
            }
            ContextMenu menu = createCommonContextMenu();
            currentOpenMenu = menu;
            menu.show(thumbnailPane, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

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

    private void toggleThumbnailSelected(VBox thumbnailBox) {
        setThumbnailSelected(thumbnailBox, !selectedThumbnails.contains(thumbnailBox));
    }

    private void clearAllSelected() {
        for (VBox box : selectedThumbnails) {
            box.getStyleClass().remove("selected");
        }
        selectedThumbnails.clear();
    }

    private void bindContextMenu(VBox thumbnailBox) {
        thumbnailBox.setOnContextMenuRequested(e -> {
            // 如果已有打开的菜单，先关闭它
            if (currentOpenMenu != null && currentOpenMenu.isShowing()) {
                currentOpenMenu.hide();
            }
            // 确保当前缩略图被选中
            if (!selectedThumbnails.contains(thumbnailBox)) {
                clearAllSelected();
                setThumbnailSelected(thumbnailBox, true);
                tipLabel.setText("已选中 " + selectedThumbnails.size() + " 张图片");
            }
            // 创建新菜单并记录
            ContextMenu contextMenu = createCommonContextMenu();
            currentOpenMenu = contextMenu;
            contextMenu.show(thumbnailBox, e.getScreenX(), e.getScreenY());
        });
    }


    @FXML
    public void convertSelectedToPdf() {
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要转换的图片");
            return;
        }

        List<File> selectedImageFiles = new ArrayList<>();
        for (VBox box : selectedThumbnails) {
            ImageFile imageFile = (ImageFile) box.getUserData();
            selectedImageFiles.add(imageFile.getFile());
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存PDF文件");
        fileChooser.setInitialFileName("图片转换结果.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF文件 (*.pdf)", "*.pdf"));

        if (currentDir != null) {
            fileChooser.setInitialDirectory(currentDir);
        }

        File outputPdfFile = fileChooser.showSaveDialog(MainApp.getPrimaryStage());
        if (outputPdfFile == null) {
            tipLabel.setText("已取消PDF转换");
            return;
        }

        try {
            PDFUtils.imagesToPdf(selectedImageFiles, outputPdfFile);
            tipLabel.setText("PDF转换成功！文件路径：" + outputPdfFile.getAbsolutePath());
        } catch (Exception e) {
            tipLabel.setText("PDF转换失败：" + e.getMessage());
            System.err.println("PDF转换失败: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("转换失败");
            alert.setHeaderText("图片转PDF失败");
            alert.setContentText("错误信息：" + e.getMessage());
            alert.showAndWait();
        }
    }

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
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putFiles(copiedFiles);
        clipboard.setContent(content);
        tipLabel.setText("已复制 " + copiedFiles.size() + " 张图片");
    }

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
        loadDirectoryImages(currentDir);
        tipLabel.setText("粘贴完成，成功粘贴 " + successCount + " 张图片");
    }

    @FXML
    public void renameSelectedImages() {
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要重命名的图片");
            return;
        }

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
        } else {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("批量重命名");
            dialog.setHeaderText("请输入批量重命名规则");

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

    @FXML
    public void deleteSelectedImages() {
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要删除的图片");
            return;
        }

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

            SlideShowController controller = fxmlLoader.getController();
            controller.setImageList(currentImageList, currentIndex);
            controller.setFixedImageSize(300, 100); // 新增这一行

            slideStage.show();
        } catch (IOException e) {
            System.err.println("打开幻灯片失败: " + e.getMessage());
            tipLabel.setText("打开幻灯片失败");
        }
    }
    // 获取当前目录（给编辑器用的）
    public File getCurrentDir() {
        return currentDir;
    }
}