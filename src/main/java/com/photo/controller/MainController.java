package com.photo.controller;

import com.photo.MainApp;
import com.photo.model.MediaFile;
import com.photo.utils.FileUtils;
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

    // 核心数据：替换ImageFile为MediaFile
    private File currentDir;
    private final ObservableList<MediaFile> currentMediaList = FXCollections.observableArrayList();
    private final ObservableList<VBox> selectedThumbnails = FXCollections.observableArrayList();
    private final List<File> copiedFiles = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initDirectoryTree();
        initThumbnailPaneEvent();
        slideShowBtn.setOnAction(e -> {
            // 修复幻灯片按钮点击：选中第一个文件打开
            if (!currentMediaList.isEmpty()) {
                openSlideShow(0);
            } else {
                tipLabel.setText("当前目录无媒体文件，无法打开幻灯片");
            }
        });
    }

    // 初始化目录树（逻辑不变）
    private void initDirectoryTree() {
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
        directoryTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.getValue() != null) {
                        loadDirectoryMedia(newValue.getValue());
                    }
                }
        );
    }

    // 递归创建目录树节点（逻辑不变）
    private TreeItem<File> createTreeItem(File file) {
        return new TreeItem<>(file) {
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

    // 加载子目录（逻辑不变）
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

    // 加载目录下的所有媒体文件（图片+视频）
    private void loadDirectoryMedia(File dir) {
        this.currentDir = dir;
        currentMediaList.clear();
        thumbnailPane.getChildren().clear();
        selectedThumbnails.clear();

        if (!dir.isDirectory()) {
            tipLabel.setText("错误：选中的不是有效目录");
            System.err.println("无效目录: " + dir.getAbsolutePath());
            return;
        }

        System.out.println("正在加载目录: " + dir.getAbsolutePath());

        File[] allFiles = dir.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            dirInfoLabel.setText("当前目录：" + dir.getAbsolutePath() + " | 媒体文件数量：0");
            tipLabel.setText("当前目录为空，无任何文件");
            System.out.println("目录为空，无任何文件");
            return;
        }

        // 筛选媒体文件（图片+视频）
        List<File> mediaFileList = Arrays.stream(allFiles)
                .filter(FileUtils::isMediaFile)
                .toList();

        System.out.println("目录中识别到的媒体文件数量: " + mediaFileList.size());

        if (mediaFileList.isEmpty()) {
            dirInfoLabel.setText("当前目录：" + dir.getAbsolutePath() + " | 媒体文件数量：0");
            tipLabel.setText("当前目录无支持的媒体文件（图片：JPG/PNG等；视频：MP4/AVI等）");
            return;
        }

        // 加载媒体文件到列表
        long totalSize = 0;
        int successCount = 0;
        int failCount = 0;

        for (File file : mediaFileList) {
            MediaFile mediaFile = new MediaFile(file);
            if (mediaFile.getThumbnail() != null) {
                currentMediaList.add(mediaFile);
                totalSize += mediaFile.getFileSize();
                successCount++;
            } else {
                failCount++;
            }
        }

        // 渲染缩略图
        renderThumbnails();
        // 更新信息
        dirInfoLabel.setText("当前目录：" + dir.getAbsolutePath() + " | 媒体文件数量：" + currentMediaList.size());
        tipLabel.setText("加载完成，成功加载" + successCount + "个媒体文件，失败" + failCount + "个，总大小：" + FileUtils.formatFileSize(totalSize));
        System.out.println("最终加载成功: " + successCount + "个，失败: " + failCount + "个");
    }

    // 渲染缩略图（修复：确保每个缩略图正确绑定事件）
    private void renderThumbnails() {
        thumbnailPane.getChildren().clear();
        for (int i = 0; i < currentMediaList.size(); i++) {
            MediaFile mediaFile = currentMediaList.get(i);
            VBox thumbnailBox = new VBox(5);
            thumbnailBox.getStyleClass().add("thumbnail-box");
            thumbnailBox.setUserData(mediaFile);
            // 存储索引，避免双击时获取索引错误
            thumbnailBox.setId(String.valueOf(i));

            ImageView imageView = new ImageView(mediaFile.getThumbnail());
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(FileUtils.THUMBNAIL_WIDTH);
            imageView.setFitHeight(FileUtils.THUMBNAIL_HEIGHT);

            Label nameLabel = new Label(mediaFile.getFileName());
            nameLabel.setMaxWidth(FileUtils.THUMBNAIL_WIDTH);
            nameLabel.setWrapText(true);

            thumbnailBox.getChildren().addAll(imageView, nameLabel);
            // 强制绑定事件（修复双击无响应核心）
            bindThumbnailEvent(thumbnailBox, i);
            bindContextMenu(thumbnailBox);

            thumbnailPane.getChildren().add(thumbnailBox);
        }
    }

    // 修复：缩略图点击事件（新增索引参数，确保双击能正确获取索引）
    private void bindThumbnailEvent(VBox thumbnailBox, int mediaIndex) {
        // 修复：确保点击事件优先级正确
        thumbnailBox.setOnMouseClicked(null);
        thumbnailBox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                // 修复：先处理选中逻辑
                if (e.getClickCount() == 1) {
                    if (e.isControlDown()) {
                        toggleThumbnailSelected(thumbnailBox);
                    } else {
                        clearAllSelected();
                        setThumbnailSelected(thumbnailBox, true);
                    }
                    tipLabel.setText("已选中 " + selectedThumbnails.size() + " 个媒体文件");
                }
                // 修复：双击单独处理，确保触发
                else if (e.getClickCount() == 2) {
                    System.out.println("双击媒体文件，索引：" + mediaIndex);
                    openSlideShow(mediaIndex);
                }
            }
        });

        // 修复：防止子组件拦截点击事件
        thumbnailBox.getChildren().forEach(child -> {
            child.setOnMouseClicked(e -> {
                e.consume();
                thumbnailBox.fireEvent(e);
            });
        });
    }

    // 初始化预览区事件（逻辑不变）
    private void initThumbnailPaneEvent() {
        thumbnailPane.setOnMouseClicked(e -> {
            if (e.getTarget() == thumbnailPane) {
                clearAllSelected();
                tipLabel.setText("已取消所有选中");
            }
        });
        thumbnailPane.setOnDragDetected(e -> thumbnailPane.startFullDrag());
    }

    // 设置缩略图选中状态（逻辑不变）
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

    // 切换选中状态（逻辑不变）
    private void toggleThumbnailSelected(VBox thumbnailBox) {
        setThumbnailSelected(thumbnailBox, !selectedThumbnails.contains(thumbnailBox));
    }

    // 清除所有选中（逻辑不变）
    private void clearAllSelected() {
        for (VBox box : selectedThumbnails) {
            box.getStyleClass().remove("selected");
        }
        selectedThumbnails.clear();
    }

    // 右键菜单（逻辑不变，仅替换ImageFile为MediaFile）
    private void bindContextMenu(VBox thumbnailBox) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("复制");
        MenuItem renameItem = new MenuItem("重命名");
        MenuItem deleteItem = new MenuItem("删除");

        copyItem.setOnAction(e -> copySelectedMedia());
        renameItem.setOnAction(e -> renameSelectedMedia());
        deleteItem.setOnAction(e -> deleteSelectedMedia());

        contextMenu.getItems().addAll(copyItem, renameItem, deleteItem);
        thumbnailBox.setOnContextMenuRequested(e -> {
            if (!selectedThumbnails.contains(thumbnailBox)) {
                clearAllSelected();
                setThumbnailSelected(thumbnailBox, true);
                tipLabel.setText("已选中 " + selectedThumbnails.size() + " 个媒体文件");
            }
            contextMenu.show(thumbnailBox, e.getScreenX(), e.getScreenY());
        });
    }

    // 复制选中媒体文件（替换ImageFile为MediaFile）
    @FXML
    public void copySelectedMedia() {
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要复制的媒体文件");
            return;
        }
        copiedFiles.clear();
        for (VBox box : selectedThumbnails) {
            MediaFile mediaFile = (MediaFile) box.getUserData();
            copiedFiles.add(mediaFile.getFile());
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putFiles(copiedFiles);
        clipboard.setContent(content);

        tipLabel.setText("已复制 " + copiedFiles.size() + " 个媒体文件");
    }

    // 粘贴媒体文件（逻辑不变）
    @FXML
    public void pasteImages() {
        if (currentDir == null || !currentDir.isDirectory()) {
            tipLabel.setText("请先选择目标目录");
            return;
        }
        if (copiedFiles.isEmpty()) {
            tipLabel.setText("没有可粘贴的媒体文件");
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
        loadDirectoryMedia(currentDir);
        tipLabel.setText("粘贴完成，成功粘贴 " + successCount + " 个媒体文件");
    }

    // 重命名选中媒体文件（替换ImageFile为MediaFile）
    @FXML
    public void renameSelectedMedia() {
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要重命名的媒体文件");
            return;
        }

        // 单选重命名
        if (selectedThumbnails.size() == 1) {
            MediaFile mediaFile = (MediaFile) selectedThumbnails.get(0).getUserData();
            TextInputDialog dialog = new TextInputDialog(FileUtils.getFileNameWithoutExtension(mediaFile.getFileName()));
            dialog.setTitle("重命名媒体文件");
            dialog.setHeaderText("请输入新的文件名（扩展名不变）");
            dialog.setContentText("文件名：");

            dialog.showAndWait().ifPresent(newName -> {
                if (newName.isBlank()) {
                    tipLabel.setText("文件名不能为空");
                    return;
                }
                String ext = FileUtils.getFileExtension(mediaFile.getFileName());
                File newFile = new File(mediaFile.getFile().getParentFile(), newName + ext);
                if (mediaFile.getFile().renameTo(newFile)) {
                    loadDirectoryMedia(currentDir);
                    tipLabel.setText("重命名成功");
                } else {
                    tipLabel.setText("重命名失败，请检查文件名是否合法");
                }
            });
        } else {
            // 批量重命名（逻辑不变，仅替换ImageFile为MediaFile）
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("批量重命名");
            dialog.setHeaderText("请输入批量重命名规则");

            VBox content = new VBox(10);
            TextField prefixField = new TextField("NewMedia");
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
                            MediaFile mediaFile = (MediaFile) box.getUserData();
                            if (FileUtils.batchRenameFile(mediaFile.getFile(), prefix, currentNum, digitCount)) {
                                successCount++;
                            }
                            currentNum++;
                        }
                        loadDirectoryMedia(currentDir);
                        tipLabel.setText("批量重命名完成，成功 " + successCount + " 个，失败 " + (selectedThumbnails.size() - successCount) + " 个");
                    } catch (NumberFormatException e) {
                        tipLabel.setText("编号必须为数字，请检查输入");
                    }
                }
            });
        }
    }

    // 删除选中媒体文件（替换ImageFile为MediaFile）
    @FXML
    public void deleteSelectedMedia() {
        if (selectedThumbnails.isEmpty()) {
            tipLabel.setText("请先选择要删除的媒体文件");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除确认");
        alert.setHeaderText("您确定要删除选中的 " + selectedThumbnails.size() + " 个媒体文件吗？");
        alert.setContentText("此操作将永久删除文件，无法恢复！");

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                int successCount = 0;
                for (VBox box : selectedThumbnails) {
                    MediaFile mediaFile = (MediaFile) box.getUserData();
                    try {
                        Files.delete(mediaFile.getFile().toPath());
                        successCount++;
                    } catch (IOException e) {
                        System.err.println("删除文件失败: " + e.getMessage());
                    }
                }
                loadDirectoryMedia(currentDir);
                tipLabel.setText("删除完成，成功删除 " + successCount + " 个媒体文件");
            }
        });
    }

    // 修复：打开幻灯片窗口（增加异常处理+日志）
    private void openSlideShow(int currentIndex) {
        // 边界检查
        if (currentMediaList.isEmpty()) {
            tipLabel.setText("无媒体文件可预览");
            return;
        }
        if (currentIndex < 0 || currentIndex >= currentMediaList.size()) {
            currentIndex = 0;
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/slideshow-view.fxml"));
            // 修复：确保FXML资源加载成功
            if (fxmlLoader.getLocation() == null) {
                tipLabel.setText("FXML文件未找到：/fxml/slideshow-view.fxml");
                return;
            }
            Scene scene = new Scene(fxmlLoader.load(), 1150, 750);
            Stage stage = new Stage();
            stage.setTitle("图片&视频预览");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            // 修复：绑定主窗口，防止窗口层级问题
            stage.initOwner(MainApp.getPrimaryStage());

            SlideShowController c = fxmlLoader.getController();
            c.setMediaList(new ArrayList<>(currentMediaList), currentIndex);

            System.out.println("打开幻灯片预览，当前索引：" + currentIndex);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            tipLabel.setText("打开预览失败：" + ex.getMessage());
        }
    }

    @FXML
    public void copySelectedImages() {
        copySelectedMedia();
    }

    @FXML
    public void renameSelectedImages() {
        renameSelectedMedia();
    }

    @FXML
    public void deleteSelectedImages() {
        deleteSelectedMedia();
    }
}