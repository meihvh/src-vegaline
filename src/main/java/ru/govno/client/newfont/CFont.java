/*
 * Decompiled with CFR 0.152.
 */
package ru.govno.client.newfont;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.DynamicTexture;

public class CFont {
    public int fontStripXSize = 8;
    public int imgSize = 512;
    public CharData[] charData;
    protected Font font;
    public int fixedFontScaleInt;
    public float scaleMultiplier = 1.0f;
    protected int fontHeight = -1;
    protected DynamicTexture tex;
    public float charMoveRX;
    public float charMoveRY;
    public float charH;

    public CFont(Font font, int fixedFontScaleInt) {
        this.font = font;
        this.fixedFontScaleInt = fixedFontScaleInt;
        this.scaleMultiplier = (float)this.fixedFontScaleInt / (float)font.getSize();
        this.charData = new CharData[1280];
        this.imgSize = (int)(16.0f * (font.getSize2D() + (float)this.fontStripXSize));
        this.tex = this.prepareFont(font, this.charData);
    }

    public DynamicTexture prepareFont(Font font, CharData[] chars) {
        try {
            return new DynamicTexture(this.generateFontImage(font, chars));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public BufferedImage generateFontImage(Font font, CharData[] chars) {
        if (this.tex != null) {
            this.tex.deleteGlTexture();
        }
        int imgSize = this.imgSize;
        BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, 2);
        Graphics2D graphics = (Graphics2D)bufferedImage.getGraphics();
        graphics.setFont(font);
        graphics.setColor(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, imgSize, imgSize);
        graphics.setColor(Color.WHITE);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fontMetrics = graphics.getFontMetrics();
        float charHeight = 0.0f;
        int positionX = 0;
        int positionY = 1;
        for (int i = 0; i < chars.length; ++i) {
            char ch = (char)i;
            if (ch > '\u040e' || ch < '\u0100') {
                CharData charData = new CharData();
                Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(ch), graphics);
                float width = dimensions.getBounds().width + this.fontStripXSize;
                float height = dimensions.getBounds().height;
                if ((float)positionX + width >= (float)imgSize) {
                    positionX = 0;
                    positionY = (int)((float)positionY + charHeight);
                    charHeight = 0.0f;
                }
                if (height > charHeight) {
                    charHeight = height;
                }
                float storedX = (short)positionX;
                float storedY = (short)positionY;
                if (height > (float)this.fontHeight) {
                    this.fontHeight = (int)height;
                }
                chars[i] = charData;
                graphics.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
                positionX = (int)((float)positionX + width);
                charData.finalizeCoords(width, height, storedX, storedY, this.scaleMultiplier, this.fontStripXSize, imgSize);
                continue;
            }
            chars[i] = null;
        }
        this.finalizeMoveChars();
        return bufferedImage;
    }

    public void fastQuadCharData(CharData data, BufferBuilder buf, float x, float y, int c) {
        buf.pos((x += this.charMoveRX) + data.renderW, y += this.charMoveRY).tex(data.texX1, data.texY0).color(c).endVertex();
        buf.pos(x + data.renderW, y + data.renderH).tex(data.texX1, data.texY1).color(c).endVertex();
        buf.pos(x, y + data.renderH).tex(data.texX0, data.texY1).color(c).endVertex();
        buf.pos(x, y).tex(data.texX0, data.texY0).color(c).endVertex();
    }

    public void fastQuadCharDataV(CharData data, BufferBuilder buf, float x, float y, int c0, int c1) {
        buf.pos((x += this.charMoveRX) + data.renderW, y += this.charMoveRY).tex(data.texX1, data.texY0).color(c0).endVertex();
        buf.pos(x + data.renderW, y + data.renderH).tex(data.texX1, data.texY1).color(c1).endVertex();
        buf.pos(x, y + data.renderH).tex(data.texX0, data.texY1).color(c1).endVertex();
        buf.pos(x, y).tex(data.texX0, data.texY0).color(c0).endVertex();
    }

    public void fastQuadCharDataH(CharData data, BufferBuilder buf, float x, float y, int c0, int c1) {
        buf.pos((x += this.charMoveRX) + data.renderW, y += this.charMoveRY).tex(data.texX1, data.texY0).color(c1).endVertex();
        buf.pos(x + data.renderW, y + data.renderH).tex(data.texX1, data.texY1).color(c1).endVertex();
        buf.pos(x, y + data.renderH).tex(data.texX0, data.texY1).color(c0).endVertex();
        buf.pos(x, y).tex(data.texX0, data.texY0).color(c0).endVertex();
    }

    public void fastQuadCharDataVH(CharData data, BufferBuilder buf, float x, float y, int c0, int c1, int c2, int c3) {
        buf.pos((x += this.charMoveRX) + data.renderW, y += this.charMoveRY).tex(data.texX1, data.texY0).color(c1).endVertex();
        buf.pos(x + data.renderW, y + data.renderH).tex(data.texX1, data.texY1).color(c2).endVertex();
        buf.pos(x, y + data.renderH).tex(data.texX0, data.texY1).color(c3).endVertex();
        buf.pos(x, y).tex(data.texX0, data.texY0).color(c0).endVertex();
    }

    public float getHeight() {
        return this.charH;
    }

    public float getStringWidth(String text) {
        float width = 0.0f;
        for (char c : text.toCharArray()) {
            if (c >= this.charData.length || c < '\u0000') continue;
            width += this.charData[c].getWidth;
        }
        return width;
    }

    public Font getFont() {
        return this.font;
    }

    public void setFont(Font font) {
        this.font = font;
        this.scaleMultiplier = (float)this.fixedFontScaleInt / (float)font.getSize();
        this.charData = new CharData[1280];
        this.imgSize = (int)(16.0f * (font.getSize2D() + (float)(this.fontStripXSize * 2)));
        this.tex = this.prepareFont(font, this.charData);
    }

    private void finalizeMoveChars() {
        float sc0 = 1.0f / this.scaleMultiplier;
        this.charMoveRX = -1.0f + sc0 * 0.25f;
        this.charMoveRY = -3.0f + sc0 * 0.075f;
        this.charH = (float)this.fixedFontScaleInt / 2.0f - 3.0f;
    }

    public class CharData {
        public float texX0;
        public float texY0;
        public float texX1;
        public float texY1;
        public float renderW;
        public float renderH;
        public float getWidth;

        protected CharData() {
        }

        public void finalizeCoords(float width, float height, float storedX, float storedY, float scaleMultiplier, int fontStripXSize, int imgSize) {
            this.renderW = width * scaleMultiplier / 2.0f;
            this.renderH = height * scaleMultiplier / 2.0f;
            this.getWidth = (width - ((float)fontStripXSize + (scaleMultiplier - 1.0f) / 2.0f)) / 2.0f * scaleMultiplier;
            this.texX0 = storedX / (float)imgSize;
            this.texY0 = storedY / (float)imgSize;
            this.texX1 = this.texX0 + width / (float)imgSize;
            this.texY1 = this.texY0 + height / (float)imgSize;
        }
    }
}
