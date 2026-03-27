package com.photo.editor;

import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;

public class BeautyTool {
    private final ColorAdjust ca = new ColorAdjust();
    private final GaussianBlur blur = new GaussianBlur();

    public void applySharpen(ImageView iv) {
        ca.setContrast(0.35);
        iv.setEffect(ca);
    }

    public void applyBlur(ImageView iv) {
        blur.setRadius(4);
        iv.setEffect(blur);
    }
}