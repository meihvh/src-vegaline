/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 */
package ru.govno.client.newfont;

import com.google.common.collect.Lists;
import java.util.ArrayList;

public class FontDraws {
    private static final ArrayList<FontDraw> coordStrings = Lists.newArrayList();
    private static final ArrayList<FontDraw> coordStringsWithGL = Lists.newArrayList();

    public static void addDraw(String text, float x, float y, int color) {
        coordStrings.add(new FontDraw(text, x, y, color));
    }

    public static void addDraw(String text, float x, float y, int color, Runnable preDraw, Runnable postDraw) {
        coordStringsWithGL.add(new FontDraw(text, x, y, color, preDraw, postDraw));
    }

    public static ArrayList<FontDraw> getDraws() {
        return coordStrings;
    }

    public static ArrayList<FontDraw> getDrawsWithGL() {
        return coordStringsWithGL;
    }

    public static void cleanup() {
        coordStrings.clear();
        coordStringsWithGL.clear();
    }

    public static class FontDraw {
        private final String text;
        private final float x;
        private final float y;
        private final int color;
        private Runnable preString;
        private Runnable postString;

        public FontDraw(String text, float x, float y, int color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
        }

        public FontDraw(String text, float x, float y, int color, Runnable preString, Runnable postString) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.preString = preString;
            this.postString = postString;
        }

        public String getText() {
            return this.text;
        }

        public float getX() {
            return this.x;
        }

        public float getY() {
            return this.y;
        }

        public int getColor() {
            return this.color;
        }
    }
}
