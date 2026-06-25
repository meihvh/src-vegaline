/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.lwjgl.opengl.GL11
 */
package ru.govno.client.newfont;

import com.google.common.collect.Lists;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import optifine.Config;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.modules.ClientColors;
import ru.govno.client.module.modules.NameSecurity;
import ru.govno.client.newfont.CFont;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.BloomUtil;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class CFontRenderer
extends CFont {
    private final float[] charWidthFloat = new float[256];
    private final byte[] glyphWidth = new byte[65536];
    private final int[] colorCode = new int[32];
    public String displayNameData;
    public String generalFontName;
    public static boolean instantSetVLAA;
    private final List<DrawStringsCache> cachedDrawsNoShadow = Lists.newArrayList();
    private final List<DrawStringsCache> cachedDrawsWithShadow = Lists.newArrayList();
    private final List<DrawStringsCacheGL> cachedDrawsNoShadowGL = Lists.newArrayList();
    private final List<DrawStringsCacheGL> cachedDrawsWithShadowGL = Lists.newArrayList();

    public CFontRenderer(Font font, int fixedFontScaleInt, String generalFontName) {
        super(font, fixedFontScaleInt);
        this.setupMinecraftColorcodes();
        this.fixedFontScaleInt = fixedFontScaleInt;
        this.scaleMultiplier = (float)this.fixedFontScaleInt / (float)font.getSize();
        this.displayNameData = font.getName() + "-scB" + fixedFontScaleInt + "-scT" + font.getSize();
        this.generalFontName = generalFontName;
    }

    public float drawStringWithShadow(String text, float x, float y, int color) {
        float shadowWidth = this.drawString(text, x + 0.5f, y + 0.5f, color, true);
        return Math.max(shadowWidth, this.drawString(text, x, y, color, false));
    }

    public float drawString(String text, float x, float y, int color) {
        return this.drawString(text, x, y, color, false);
    }

    public float drawStringWithBloom(String text, float x, float y, int color, float bloomAPC, int radius) {
        if (!Config.isShaders() && !Config.isFastRender()) {
            GL11.glBlendFunc((int)770, (int)1);
            BloomUtil.renderShadow(() -> this.drawString(text, x, y, -1, false), color, radius, 0, Math.max((float)radius * bloomAPC, 1.0f), false);
            GL11.glBlendFunc((int)770, (int)771);
        }
        return this.drawString(text, x, y, color, false);
    }

    public float drawStringWithBloomAndShadow(String text, float x, float y, int color, float bloomAPC, int radius) {
        if (!Config.isShaders() && !Config.isFastRender()) {
            GL11.glBlendFunc((int)770, (int)1);
            BloomUtil.renderShadow(() -> this.drawString(text, x, y, -1, false), color, radius, 0, Math.max((float)radius * (bloomAPC *= Math.min(ColorUtils.getGLAlphaFromColor(color), 1.0f)), 1.0f), false);
            GL11.glBlendFunc((int)770, (int)771);
        }
        return this.drawStringWithShadow(text, x, y, color);
    }

    public void drawVGradientString(String text, float x, float y, int color, int color2) {
        GL11.glEnable((int)3089);
        for (double newY = (double)(y - 0.5f); newY < (double)(y + this.getHeight() + 3.0f); newY += 0.5) {
            RenderUtils.scissor(0.0, (float)newY - 1.0f, 100000.0, 0.5);
            GL11.glTranslated((double)x, (double)y, (double)0.0);
            this.drawString(text, 0.0f, 0.0f, ColorUtils.getOverallColorFrom(color, color2, (float)MathUtils.clamp((newY - (double)y) / (double)(this.getHeight() + 3.0f), 0.0, 1.0)));
            GL11.glTranslated((double)(-x), (double)(-y), (double)0.0);
        }
        GL11.glDisable((int)3089);
    }

    public void drawVHGradientString(String text, float x, float y, int color, int color2, int color3, int color4) {
        x -= this.getStringWidth(text);
        float index = 0.0f;
        for (char c : text.toCharArray()) {
            int col1 = ColorUtils.getOverallColorFrom(color, color2, index / this.getStringWidth(text));
            int col2 = ColorUtils.getOverallColorFrom(color4, color3, index / this.getStringWidth(text));
            this.drawVGradientString(String.valueOf(c), this.getStringWidth(text) + index + x, y, col1, col2);
            index += this.getStringWidth(String.valueOf(c));
        }
    }

    public void drawHGradientString(String text, float x, float y, int color, int color2) {
        x -= this.getStringWidth(text);
        float index = 0.0f;
        for (char c : text.toCharArray()) {
            int col1 = ColorUtils.getOverallColorFrom(color, color2, index / this.getStringWidth(text));
            this.drawString(String.valueOf(c), this.getStringWidth(text) + index + x, y, col1);
            index += this.getStringWidth(String.valueOf(c));
        }
    }

    public void drawClientColoredString(String text, float x, float y, float alphaPC, boolean shadow) {
        x -= this.getStringWidth(text);
        float index = 0.0f;
        for (char c : text.toCharArray()) {
            int col1 = ColorUtils.getOverallColorFrom(ClientColors.getColor1((int)index * 5), ClientColors.getColor2((int)index * 5), index / this.getStringWidth(text));
            if (ColorUtils.getAlphaFromColor(col1 = ColorUtils.swapAlpha(col1, (float)ColorUtils.getAlphaFromColor(col1) * alphaPC)) >= 32) {
                if (shadow) {
                    this.drawString(String.valueOf(c), this.getStringWidth(text) + index + x + 0.5f, y + 0.5f, ColorUtils.swapDark(col1, ColorUtils.getBrightnessFromColor(col1) / 3.0f));
                }
                this.drawString(String.valueOf(c), this.getStringWidth(text) + index + x, y, col1);
            }
            index += this.getStringWidth(String.valueOf(c));
        }
    }

    public void drawClientColoredString(String text, float x, float y, float alphaPC, boolean shadow, int indexPlus) {
        x -= this.getStringWidth(text);
        float index = 0.0f;
        for (char c : text.toCharArray()) {
            int col1 = ColorUtils.getOverallColorFrom(ClientColors.getColor1((int)index * 5 + indexPlus), ClientColors.getColor2((int)index * 5 + indexPlus), index / this.getStringWidth(text));
            if (ColorUtils.getAlphaFromColor(col1 = ColorUtils.swapAlpha(col1, (float)ColorUtils.getAlphaFromColor(col1) * alphaPC)) >= 32) {
                if (shadow) {
                    this.drawString(String.valueOf(c), this.getStringWidth(text) + index + x + 0.5f, y + 0.5f, ColorUtils.swapDark(col1, ColorUtils.getBrightnessFromColor(col1) / 1.5f));
                }
                this.drawString(String.valueOf(c), this.getStringWidth(text) + index + x, y, col1);
            }
            index += this.getStringWidth(String.valueOf(c));
        }
    }

    public void drawClientColoredString(String text, float x, float y, float alphaPC, boolean shadow, int indexPlus, boolean reverseColor) {
        x -= this.getStringWidth(text);
        float index = 0.0f;
        for (char c : text.toCharArray()) {
            float pc = index / this.getStringWidth(text);
            int col1 = ColorUtils.getOverallColorFrom(ClientColors.getColor1((int)index * 5 + indexPlus), ClientColors.getColor2((int)index * 5 + indexPlus), reverseColor ? 1.0f - pc : pc);
            if (ColorUtils.getAlphaFromColor(col1 = ColorUtils.swapAlpha(col1, (float)ColorUtils.getAlphaFromColor(col1) * alphaPC)) >= 32) {
                if (shadow) {
                    this.drawString(String.valueOf(c), this.getStringWidth(text) + index + x + 0.5f, y + 0.5f, ColorUtils.swapDark(col1, ColorUtils.getBrightnessFromColor(col1) / 1.5f));
                }
                this.drawString(String.valueOf(c), this.getStringWidth(text) + index + x, y, col1);
            }
            index += this.getStringWidth(String.valueOf(c));
        }
    }

    public void drawStringWithOutline(String text, float x, float y, int color) {
        int alphedCBlack = ColorUtils.getColor(0, 0, 0, ColorUtils.getAlphaFromColor(color));
        this.drawString(text, x - 0.5f, y - 0.5f, alphedCBlack, false);
        this.drawString(text, x + 0.5f, y + 0.5f, alphedCBlack, false);
        this.drawString(text, x + 0.5f, y, alphedCBlack, false);
        this.drawString(text, x, y + 0.5f, alphedCBlack, false);
        this.drawString(text, x - 0.5f, y, alphedCBlack, false);
        this.drawString(text, x, y - 0.5f, alphedCBlack, false);
        this.drawString(text, x, y, color, false);
    }

    public float drawCenteredString(String text, float x, float y, int color) {
        return this.drawString(text, x - this.getStringWidth(text) / 2.0f, y, color);
    }

    public List<String> listFormattedStringToWidth(String str, int wrapWidth) {
        return Arrays.asList(this.wrapFormattedStringToWidth(str, wrapWidth).split("\n"));
    }

    public String trimStringToWidth(String text, int width) {
        return this.trimStringToWidth(text, width, false);
    }

    public float drawString(String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        text = NameSecurity.replacedIfActive(text);
        if ((color & 0xFC000000) == 0) {
            color |= 0xFF000000;
        }
        if (shadow) {
            color = (color & 0xFCFCFC) >> 2 | color & new Color(20, 20, 20, 200).getRGB();
        }
        CFont.CharData[] currentData = this.charData;
        int alpha = color >> 24 & 0xFF;
        int size = text.length();
        int charColor = color;
        boolean drawStarted = false;
        for (int i = 0; i < size; ++i) {
            CFont.CharData charData;
            char character = text.charAt(i);
            if (String.valueOf(character).equals("\u00a7")) {
                int colorIndex = 21;
                try {
                    colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
                }
                catch (Exception exception) {
                    // empty catch block
                }
                if (colorIndex < 16) {
                    if (colorIndex < 0) {
                        colorIndex = 15;
                    }
                    if (shadow) {
                        colorIndex += 16;
                    }
                    int colorcode = this.colorCode[colorIndex];
                    charColor = ColorUtils.swapAlpha(colorcode, alpha);
                } else if (colorIndex == 21 || colorIndex == 17 || colorIndex == 18 || colorIndex == 19 || colorIndex == 20) {
                    charColor = ColorUtils.swapAlpha(color, alpha);
                }
                ++i;
                continue;
            }
            if (character >= currentData.length || currentData == null || (charData = currentData[character]) == null) continue;
            if (!drawStarted) {
                RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            }
            this.fastQuadCharData(charData, RenderUtils.buffer, x, y, charColor);
            drawStarted = true;
            x += charData.getWidth;
        }
        if (drawStarted) {
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(this.tex.getGlTextureId());
            boolean cull = GL11.glIsEnabled((int)2884);
            if (cull) {
                GL11.glDisable((int)2884);
            }
            GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
            GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
            RenderUtils.tessellator.draw();
            GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
            if (cull) {
                GL11.glEnable((int)2884);
            }
        } else {
            RenderUtils.buffer.reset();
        }
        return x;
    }

    @Override
    public float getStringWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        text = NameSecurity.replacedIfActive(text);
        float width = 0.0f;
        for (int i = 0; i < text.length(); i = (int)((short)(i + 1))) {
            char character = text.charAt(i);
            if (character == '\u00a7') {
                i = (short)(i + 1);
                continue;
            }
            CFont.CharData currentData = this.charData[character % this.charData.length];
            if (currentData == null) continue;
            width += currentData.getWidth;
        }
        return width;
    }

    public float getCharWidth(char theChar) {
        CFont.CharData data = this.charData[theChar % this.charData.length];
        return data == null ? 0.0f : data.getWidth + 0.5f * this.scaleMultiplier;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        this.scaleMultiplier = (float)this.fixedFontScaleInt / (float)font.getSize();
        this.displayNameData = font.getName() + "-scB" + this.fixedFontScaleInt + "-scT" + font.getSize();
    }

    String wrapFormattedStringToWidth(String str, int wrapWidth) {
        if (str.length() <= 1) {
            return str;
        }
        int i = this.sizeStringToWidth(str, wrapWidth);
        if (str.length() <= i) {
            return str;
        }
        String s = str.substring(0, i);
        char c0 = str.charAt(i);
        boolean flag = c0 == ' ' || c0 == '\n';
        String s1 = CFontRenderer.getFormatFromString(s) + str.substring(i + (flag ? 1 : 0));
        return s + "\n" + this.wrapFormattedStringToWidth(s1, wrapWidth);
    }

    public static String getFormatFromString(String text) {
        String s = "";
        int i = -1;
        int j = text.length();
        while ((i = text.indexOf(167, i + 1)) != -1) {
            if (i >= j - 1) continue;
            char c0 = text.charAt(i + 1);
            if (CFontRenderer.isFormatColor(c0)) {
                s = "\u00a7" + c0;
                continue;
            }
            if (!CFontRenderer.isFormatSpecial(c0)) continue;
            s = (String)s + "\u00a7" + c0;
        }
        return s;
    }

    private int sizeStringToWidth(String str, int wrapWidth) {
        int j;
        str = NameSecurity.replacedIfActive(str);
        int i = str.length();
        float f = 0.0f;
        int k = -1;
        boolean flag = false;
        for (j = 0; j < i; ++j) {
            char c0 = str.charAt(j);
            switch (c0) {
                case '\n': {
                    --j;
                    break;
                }
                case ' ': {
                    k = j;
                }
                default: {
                    f += this.getCharWidthFloat(c0);
                    if (!flag) break;
                    f += 1.0f;
                    break;
                }
                case '\u00a7': {
                    char c1;
                    if (j >= i - 1) break;
                    if ((c1 = str.charAt(++j)) != 'l' && c1 != 'L') {
                        if (c1 != 'r' && c1 != 'R' && !CFontRenderer.isFormatColor(c1)) break;
                        flag = false;
                        break;
                    }
                    flag = true;
                }
            }
            if (c0 == '\n') {
                k = ++j;
                break;
            }
            if (Math.round(f) > wrapWidth) break;
        }
        return j != i && k != -1 && k < j ? k : j;
    }

    private float getCharWidthFloat(char p_getCharWidthFloat_1_) {
        if (p_getCharWidthFloat_1_ == '\u00a7') {
            return -1.0f;
        }
        if (p_getCharWidthFloat_1_ != ' ' && p_getCharWidthFloat_1_ != '\u00a0') {
            int i = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000".indexOf(p_getCharWidthFloat_1_);
            if (p_getCharWidthFloat_1_ > '\u0000' && i != -1) {
                return this.charWidthFloat[i];
            }
            if (this.glyphWidth[p_getCharWidthFloat_1_] != 0) {
                int j = this.glyphWidth[p_getCharWidthFloat_1_] & 0xFF;
                int k = j >>> 4;
                int l = j & 0xF;
                return (++l - k) / 2 + 1;
            }
            return 0.0f;
        }
        return this.charWidthFloat[32];
    }

    public String trimStringToWidth(String text, int width, boolean reverse) {
        StringBuilder stringbuilder = new StringBuilder();
        float f = 0.0f;
        int i = reverse ? text.length() - 1 : 0;
        int j = reverse ? -1 : 1;
        boolean flag = false;
        boolean flag1 = false;
        for (int k = i; k >= 0 && k < text.length() && f < (float)width; k += j) {
            char c0 = text.charAt(k);
            float f1 = this.getCharWidthFloat(c0);
            if (flag) {
                flag = false;
                if (c0 != 'l' && c0 != 'L') {
                    if (c0 == 'r' || c0 == 'R') {
                        flag1 = false;
                    }
                } else {
                    flag1 = true;
                }
            } else if (f1 < 0.0f) {
                flag = true;
            } else {
                f += f1;
                if (flag1) {
                    f += 1.0f;
                }
            }
            if (f > (float)width) break;
            if (reverse) {
                stringbuilder.insert(0, c0);
                continue;
            }
            stringbuilder.append(c0);
        }
        return stringbuilder.toString();
    }

    private static boolean isFormatSpecial(char formatChar) {
        return formatChar >= 'k' && formatChar <= 'o' || formatChar >= 'K' && formatChar <= 'O' || formatChar == 'r' || formatChar == 'R';
    }

    private static boolean isFormatColor(char colorChar) {
        return colorChar >= '0' && colorChar <= '9' || colorChar >= 'a' && colorChar <= 'f' || colorChar >= 'A' && colorChar <= 'F';
    }

    private void setupMinecraftColorcodes() {
        for (int index = 0; index < 32; ++index) {
            int noClue = (index >> 3 & 1) * 85;
            int red = (index >> 2 & 1) * 170 + noClue;
            int green = (index >> 1 & 1) * 170 + noClue;
            int blue = (index >> 0 & 1) * 170 + noClue;
            if (index == 6) {
                red += 85;
            }
            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            this.colorCode[index] = (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
        }
    }

    public void test() {
        try {
            FillFontColorOne filler = new FillFontColorOne("\u00a700\u00a711\u00a722\u00a733\u00a744\u00a755\u00a766\u00a777\u00a788\u00a799\u00a7aa\u00a7bb\u00a7cc\u00a7dd\u00a7ee\u00a7ff\u00a7kk\u00a7ll\u00a7mm\u00a7nn\u00a7oo\u00a7rr and CUSTOM COLORS!!!", false, ClientColors.getColorQ(1), ClientColors.getColorQ(2), ClientColors.getColorQ(3), ClientColors.getColorQ(4));
            if (filler.hasAnyChar()) {
                RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                filler.cicleBufferFill(RenderUtils.buffer, 300.0f, 300.0f);
                boolean shade = true;
                if (shade) {
                    GL11.glShadeModel((int)7425);
                }
                GlStateManager.enableBlend();
                GlStateManager.enableTexture2D();
                GlStateManager.bindTexture(this.tex.getGlTextureId());
                boolean cull = GL11.glIsEnabled((int)2884);
                if (cull) {
                    GL11.glDisable((int)2884);
                }
                if (this.scaleMultiplier != 1.0f || instantSetVLAA) {
                    GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
                    GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
                    RenderUtils.tessellator.draw();
                    GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
                    GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
                } else {
                    RenderUtils.tessellator.draw();
                }
                if (cull) {
                    GL11.glEnable((int)2884);
                }
                if (shade) {
                    GL11.glShadeModel((int)7424);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addCachedrawString(String text, float x, float y, int color) {
        this.cachedDrawsNoShadow.add(new DrawStringsCache(text, x, y, color));
    }

    public void addCachedrawStringWithShadow(String text, float x, float y, int color) {
        this.cachedDrawsWithShadow.add(new DrawStringsCache(text, x, y, color));
    }

    public void addCachedrawString(String text, float x, float y, int color, Runnable pre, Runnable post) {
        if (pre == null && post == null) {
            this.addCachedrawString(text, x, y, color);
        } else {
            this.cachedDrawsNoShadowGL.add(new DrawStringsCacheGL(text, x, y, color, pre, post));
        }
    }

    public void addCachedrawStringWithShadow(String text, float x, float y, int color, Runnable pre, Runnable post) {
        if (pre == null && post == null) {
            this.addCachedrawStringWithShadow(text, x, y, color);
        } else {
            this.cachedDrawsWithShadowGL.add(new DrawStringsCacheGL(text, x, y, color, pre, post));
        }
    }

    public void drawAllCaches() {
        this.drawAllCaches(true);
    }

    public void drawAllCaches(boolean cleanup) {
        char character;
        int i;
        int charColor;
        int size;
        int alpha;
        CFont.CharData[] currentData;
        float y;
        float x;
        int color;
        String text;
        if (!this.cachedDrawsNoShadowGL.isEmpty()) {
            for (DrawStringsCacheGL drawStringsCacheGL : this.cachedDrawsNoShadowGL) {
                drawStringsCacheGL.pre.run();
                this.drawString(drawStringsCacheGL.text, drawStringsCacheGL.x, drawStringsCacheGL.y, drawStringsCacheGL.color);
                drawStringsCacheGL.post.run();
            }
        }
        if (!this.cachedDrawsWithShadowGL.isEmpty()) {
            for (DrawStringsCacheGL drawStringsCacheGL : this.cachedDrawsWithShadowGL) {
                drawStringsCacheGL.pre.run();
                this.drawStringWithShadow(drawStringsCacheGL.text, drawStringsCacheGL.x, drawStringsCacheGL.y, drawStringsCacheGL.color);
                drawStringsCacheGL.post.run();
            }
        }
        boolean drawStarted = false;
        if (!this.cachedDrawsNoShadow.isEmpty()) {
            for (DrawStringsCache draw : this.cachedDrawsNoShadow) {
                text = draw.text;
                text = NameSecurity.replacedIfActive(text);
                color = draw.color;
                x = draw.x;
                y = draw.y;
                if (text == null || text.isEmpty()) continue;
                if ((color & 0xFC000000) == 0) {
                    color |= 0xFF000000;
                }
                currentData = this.charData;
                alpha = color >> 24 & 0xFF;
                size = text.length();
                charColor = color;
                for (i = 0; i < size; ++i) {
                    CFont.CharData charData;
                    character = text.charAt(i);
                    if (String.valueOf(character).equals("\u00a7")) {
                        int colorIndex = 21;
                        try {
                            colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
                        }
                        catch (Exception exception) {
                            // empty catch block
                        }
                        if (colorIndex < 16) {
                            if (colorIndex < 0) {
                                colorIndex = 15;
                            }
                            int colorcode = this.colorCode[colorIndex];
                            charColor = ColorUtils.swapAlpha(colorcode, alpha);
                        } else if (colorIndex == 21 || colorIndex == 17 || colorIndex == 18 || colorIndex == 19 || colorIndex == 20) {
                            charColor = ColorUtils.swapAlpha(color, alpha);
                        }
                        ++i;
                        continue;
                    }
                    if (character >= currentData.length || currentData == null || (charData = currentData[character]) == null) continue;
                    if (!drawStarted) {
                        RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                    }
                    this.fastQuadCharData(charData, RenderUtils.buffer, x, y, charColor);
                    drawStarted = true;
                    x += charData.getWidth;
                }
            }
        }
        if (!this.cachedDrawsWithShadow.isEmpty()) {
            for (DrawStringsCache draw : this.cachedDrawsWithShadow) {
                text = draw.text;
                text = NameSecurity.replacedIfActive(text);
                color = draw.color;
                x = draw.x;
                y = draw.y;
                if (text == null || text.isEmpty()) continue;
                if ((color & 0xFC000000) == 0) {
                    color |= 0xFF000000;
                }
                currentData = this.charData;
                alpha = color >> 24 & 0xFF;
                size = text.length();
                charColor = color;
                for (i = 0; i < size; ++i) {
                    CFont.CharData charData;
                    character = text.charAt(i);
                    if (String.valueOf(character).equals("\u00a7")) {
                        int colorIndex = 21;
                        try {
                            colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
                        }
                        catch (Exception colorcode) {
                            // empty catch block
                        }
                        if (colorIndex < 16) {
                            if (colorIndex < 0) {
                                colorIndex = 15;
                            }
                            int colorcode = this.colorCode[colorIndex];
                            charColor = ColorUtils.swapAlpha(colorcode, alpha);
                        } else if (colorIndex == 21 || colorIndex == 17 || colorIndex == 18 || colorIndex == 19 || colorIndex == 20) {
                            charColor = ColorUtils.swapAlpha(color, alpha);
                        }
                        ++i;
                        continue;
                    }
                    if (character >= currentData.length || currentData == null || (charData = currentData[character]) == null) continue;
                    if (!drawStarted) {
                        RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                    }
                    this.fastQuadCharData(charData, RenderUtils.buffer, x + 0.5f, y + 0.5f, ColorUtils.swapAlpha(ColorUtils.toDark(charColor, 0.2f), (float)ColorUtils.getAlphaFromColor(charColor) * 0.666666f));
                    this.fastQuadCharData(charData, RenderUtils.buffer, x, y, charColor);
                    drawStarted = true;
                    x += charData.getWidth;
                }
            }
        }
        if (drawStarted) {
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(this.tex.getGlTextureId());
            boolean bl = GL11.glIsEnabled((int)2884);
            if (bl) {
                GL11.glDisable((int)2884);
            }
            GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
            GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
            RenderUtils.tessellator.draw();
            GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
            GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
            if (bl) {
                GL11.glEnable((int)2884);
            }
        } else {
            RenderUtils.buffer.reset();
        }
        if (cleanup) {
            this.cleanupAllCaches();
        }
    }

    public void cleanupAllCaches() {
        this.cachedDrawsNoShadowGL.clear();
        this.cachedDrawsWithShadowGL.clear();
        this.cachedDrawsNoShadow.clear();
        this.cachedDrawsWithShadow.clear();
    }

    public class FillFontColorOne {
        private final List<CFont.CharData> charDataTemp = new ArrayList<CFont.CharData>();
        private final List<Integer[]> charColorTemp = new ArrayList<Integer[]>();
        private final int colorsCount1or2or4;

        private int getColor(String of, char theCurrentChar, int indexChar, int customColor, int alpha, boolean applyShadowColor) {
            int currentCheckIndex = indexChar;
            while (currentCheckIndex > 1) {
                char char0 = of.charAt(--currentCheckIndex - 1);
                char char1 = of.charAt(currentCheckIndex);
                if (!String.valueOf(char0).equals("\u00a7")) continue;
                int colorIndex = 21;
                try {
                    colorIndex = "0123456789abcdefklmnor".indexOf(char1);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                if (colorIndex < 0 || colorIndex >= 16) break;
                int colorcode = CFontRenderer.this.colorCode[colorIndex];
                return applyShadowColor ? ColorUtils.swapAlpha(ColorUtils.toDark(colorcode, 0.2f), alpha) : ColorUtils.swapAlpha(colorcode, alpha);
            }
            return applyShadowColor ? ColorUtils.toDark(customColor, 0.2f) : customColor;
        }

        public FillFontColorOne(String of, boolean applyShadowColor, int ... color1or2or4) {
            of = NameSecurity.replacedIfActive(of);
            int textLength = of.length();
            this.charDataTemp.clear();
            this.charColorTemp.clear();
            this.colorsCount1or2or4 = color1or2or4.length;
            int[] alphaArray = new int[this.colorsCount1or2or4];
            for (int alphaIndex = 0; alphaIndex < this.colorsCount1or2or4; ++alphaIndex) {
                alphaArray[alphaIndex] = ColorUtils.getAlphaFromColor(color1or2or4[alphaIndex]);
            }
            for (int indexChar = 0; indexChar < textLength; ++indexChar) {
                char theChar = of.charAt(indexChar);
                if (indexChar >= 0 && String.valueOf(of.charAt(indexChar)).equals("\u00a7")) {
                    ++indexChar;
                    continue;
                }
                CFont.CharData currentCharData = CFontRenderer.this.charData[theChar % CFontRenderer.this.charData.length];
                if (currentCharData == null) continue;
                this.charDataTemp.add(currentCharData);
                Integer[] withCodeColors = new Integer[this.colorsCount1or2or4];
                for (int colorBaseIndex = 0; colorBaseIndex < this.colorsCount1or2or4; ++colorBaseIndex) {
                    int withCodeColor = this.getColor(of, theChar, indexChar, color1or2or4[colorBaseIndex], alphaArray[colorBaseIndex], applyShadowColor);
                    withCodeColors[colorBaseIndex] = withCodeColor;
                }
                this.charColorTemp.add(withCodeColors);
            }
        }

        public boolean hasAnyChar() {
            return !this.charDataTemp.isEmpty() && (this.colorsCount1or2or4 == 1 || this.colorsCount1or2or4 == 2 || this.colorsCount1or2or4 == 4);
        }

        public void cicleBufferFill(BufferBuilder buffer, float x, float y) {
            switch (this.colorsCount1or2or4) {
                case 1: {
                    for (int indexCharData = 0; indexCharData < this.charColorTemp.size(); ++indexCharData) {
                        CFont.CharData charData = this.charDataTemp.get(indexCharData);
                        Integer[] charColors = this.charColorTemp.get(indexCharData);
                        CFontRenderer.this.fastQuadCharData(charData, buffer, x, y, charColors[0]);
                        x += charData.getWidth;
                    }
                    break;
                }
                case 2: {
                    for (int indexCharData = 0; indexCharData < this.charColorTemp.size(); ++indexCharData) {
                        CFont.CharData charData = this.charDataTemp.get(indexCharData);
                        Integer[] charColors = this.charColorTemp.get(indexCharData);
                        CFontRenderer.this.fastQuadCharDataV(charData, buffer, x, y, charColors[0], charColors[1]);
                        x += charData.getWidth;
                    }
                    break;
                }
                case 4: {
                    for (int indexCharData = 0; indexCharData < this.charColorTemp.size(); ++indexCharData) {
                        CFont.CharData charData = this.charDataTemp.get(indexCharData);
                        Integer[] charColors = this.charColorTemp.get(indexCharData);
                        CFontRenderer.this.fastQuadCharDataVH(charData, buffer, x, y, charColors[0], charColors[1], charColors[2], charColors[3]);
                        x += charData.getWidth;
                    }
                    break;
                }
            }
        }
    }

    private class DrawStringsCache {
        private final String text;
        private final float x;
        private final float y;
        private final int color;

        public DrawStringsCache(String text, float x, float y, int color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    private class DrawStringsCacheGL {
        private final String text;
        private final float x;
        private final float y;
        private final int color;
        private final Runnable pre;
        private final Runnable post;

        public DrawStringsCacheGL(String text, float x, float y, int color, Runnable pre, Runnable post) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.pre = pre;
            this.post = post;
        }
    }
}
