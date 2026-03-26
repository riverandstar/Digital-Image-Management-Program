package com.photo.editor;

import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Light;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;

public class BeautyTool {

    // 锐化
    public void applySharpen(ImageView view) {
        ColorAdjust sharpen = new ColorAdjust();
        sharpen.setContrast(0.2); // 提高对比度，色彩更分明
        sharpen.setBrightness(-0.1); // 稍微压暗亮部，边缘更清晰
        view.setEffect(sharpen);
    }

    // 模糊
    public void applyBlur(ImageView view) {
        GaussianBlur blur = new GaussianBlur();
        blur.setRadius(5);
        view.setEffect(blur);
    }

    // 亮度
    public void applyBrightness(ImageView view) {
        ColorAdjust ca = new ColorAdjust();
        ca.setBrightness(0.2);
        view.setEffect(ca);
    }

    // 对比度
    public void applyContrast(ImageView view) {
        ColorAdjust ca = new ColorAdjust();
        ca.setContrast(0.3);
        view.setEffect(ca);
    }
}