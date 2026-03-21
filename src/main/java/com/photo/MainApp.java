package com.photo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {
    private static Stage primaryStage;
    // 主窗口尺寸常量
    public static final double MAIN_WIDTH = 1200;
    public static final double MAIN_HEIGHT = 800;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        // 加载主界面FXML
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), MAIN_WIDTH, MAIN_HEIGHT);

        // 修复空指针警告：安全加载CSS，先判断资源是否存在
        URL cssResource = MainApp.class.getResource("/css/style.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.out.println("提示：样式文件未找到，将使用默认样式");
        }

        stage.setTitle("电子图片管理程序");
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