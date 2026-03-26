package com.photo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        // 加载主界面FXML
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/fxml/main-view.fxml"));
        // 获取屏幕可用区域（排除任务栏等系统UI）
        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();

        // 窗口大小设为屏幕的80%（可根据需求调整比例，如0.75）
        double windowWidth = screenWidth * 0.8;
        double windowHeight = screenHeight * 0.8;

        // 限制最小窗口尺寸（防止缩放过小导致布局错乱）
        double minWidth = 800;
        double minHeight = 600;
        windowWidth = Math.max(windowWidth, minWidth);
        windowHeight = Math.max(windowHeight, minHeight);

        Scene scene = new Scene(fxmlLoader.load(), windowWidth, windowHeight);

        // 修复空指针警告：安全加载CSS，先判断资源是否存在
        URL cssResource = MainApp.class.getResource("/css/style.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.out.println("提示：样式文件未找到，将使用默认样式");
        }

        stage.setTitle("电子图片管理程序");
        stage.setResizable(true);
        // 设置窗口最小尺寸
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
        // 窗口居中显示
        stage.centerOnScreen();
        stage.setScene(scene);
        stage.show();
    }

    // 对外暴露主舞台，供幻灯片窗口调用
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    // 抑制未使用警告，明确这是程序启动入口
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        // 传入启动参数，修复args未使用的提示
        launch(args);
    }
}