package com.photo.editor;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class BrushTool {
    private double thickness = 2;
    private double opacity = 1.0;
    private Color currentColor = Color.RED;

    public static final Color RED    = Color.RED;
    public static final Color ORANGE = Color.ORANGE;
    public static final Color YELLOW = Color.YELLOW;
    public static final Color GREEN  = Color.GREEN;
    public static final Color CYAN   = Color.CYAN;
    public static final Color BLUE   = Color.BLUE;
    public static final Color PURPLE = Color.PURPLE;

    public void setThickness(double t) { this.thickness = t; }
    public void setOpacity(double o) { this.opacity = o; }
    public void setColor(Color c) { this.currentColor = c; }

    public Circle draw(double x, double y) {
        Circle dot = new Circle(x, y, thickness);
        dot.setFill(new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), opacity));
        return dot;
    }
}