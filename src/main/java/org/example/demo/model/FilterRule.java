package org.example.demo.model;

import java.awt.*;

public class FilterRule {
    public String pattern;
    public Color color;

    public FilterRule(String pattern, String hexColor) {
        this.pattern = pattern;
        this.color = Color.decode(hexColor);
    }
}
