package com.photo.editor;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class BrushTool {
    private Color color = Color.BLACK;

    public void setColor(Color color) {
        this.color = color;
    }

    public Line drawLine(double x1, double y1, double x2, double y2) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(color);
        line.setStrokeWidth(2.5);
        return line;
    }
}