package com.photo.editor;

import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;

public class BeautyTool {
    private final ColorAdjust ca = new ColorAdjust();
    private final GaussianBlur blur = new GaussianBlur();

    // 锐化（提高对比度模拟锐化）
    public void applySharpen(ImageView iv) {
        ca.setContrast(0.35);
        ca.setBrightness(0);
        iv.setEffect(ca);
    }

    // 模糊
    public void applyBlur(ImageView iv) {
        blur.setRadius(4);
        iv.setEffect(blur);
    }

    // 新增：亮度
    public void applyBrightness(ImageView iv) {
        ca.setBrightness(0.2);
        ca.setContrast(0);
        iv.setEffect(ca);
    }

    // 新增：对比度
    public void applyContrast(ImageView iv) {
        ca.setContrast(0.25);
        ca.setBrightness(0);
        iv.setEffect(ca);
    }

    // 清空所有美化效果（给回溯用）
    public void clearEffect(ImageView iv) {
        iv.setEffect(null);
    }
}