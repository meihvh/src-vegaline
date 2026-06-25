/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.opengl.GL11
 */
package ru.govno.client.clickgui;

import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.Vec2f;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.clickgui.Set;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.module.modules.TargetHUD;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;

public class Modes
extends Set {
    boolean open = false;
    AnimationUtils anim = new AnimationUtils(0.0f, 0.0f, 0.15f);
    AnimationUtils changeAnim = new AnimationUtils(0.0f, 0.0f, 0.15f);
    AnimationUtils arrow = new AnimationUtils(0.0f, 0.0f, 0.15f);
    ModeSettings setting;
    public boolean playClose;

    public Modes(ModeSettings setting) {
        super(setting);
        this.setting = setting;
    }

    private boolean isOverSize() {
        return this.setting.modes.length > 18;
    }

    private float modeHeight(boolean overSize) {
        return overSize ? 9.0f : 13.0f;
    }

    @Override
    public void drawScreen(float x, float y, int step, int mouseX, int mouseY, float partialTicks) {
        float toRot;
        super.drawScreen(x, y, step, mouseX, mouseY, partialTicks);
        boolean overSize = this.isOverSize();
        float scaledAlphaPercent = ClickGuiScreen.globalAlpha.anim / 255.0f;
        scaledAlphaPercent *= scaledAlphaPercent;
        float f = this.anim.to = this.open ? 1.0f : 0.0f;
        if (MathUtils.getDifferenceOf(this.anim.getAnim(), this.anim.to) < 0.03f) {
            this.anim.setAnim(this.anim.to);
        }
        this.anim.speed = 0.15f;
        this.arrow.to = toRot = this.open ? -90.0f : 0.0f;
        float f2 = this.arrow.speed = MathUtils.getDifferenceOf(this.arrow.getAnim(), toRot) > 1.0f ? 0.1f : 0.2f;
        if (this.changeAnim.getAnim() >= 4.0f) {
            this.changeAnim.to = 0.0f;
        }
        if (this.playClose) {
            ClientTune.get.playGuiCheckOpenOrCloseSong(false);
            this.playClose = false;
        }
        float getHeight = this.getHeight();
        int cc = ColorUtils.getColor(0, 0, 0, (int)(110.0f * scaledAlphaPercent));
        RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(x + 4.0f, y + 2.0f, x + this.getWidth() - 4.0f, y + getHeight - 1.0f, 2.0f, 0.5f, cc, cc, cc, cc, false, true, true);
        RenderUtils.drawAlphedSideways(x + 4.0f, y + 1.5f, x + this.getWidth() - 4.0f, y + 2.5f, ColorUtils.swapAlpha(ClickGuiScreen.getColor((int)((float)step + y / getHeight), this.setting.module.category), 255.0f * scaledAlphaPercent), ColorUtils.swapAlpha(ClickGuiScreen.getColor((int)((float)(step + 120) + y / getHeight), this.setting.module.category), 255.0f * scaledAlphaPercent));
        RenderUtils.fixShadows();
        GlStateManager.resetColor();
        if (scaledAlphaPercent * 255.0f >= 33.0f) {
            Fonts.comfortaaBold_12.drawString(this.setting.getName(), x + 8.0f, y + 8.5f, ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), 255.0f * scaledAlphaPercent));
            if ((175.0f - 175.0f * MathUtils.clamp(this.changeAnim.anim / 4.0f, 0.0f, 1.0f)) * scaledAlphaPercent >= 33.0f) {
                Fonts.comfortaaBold_12.drawString(this.setting.currentMode, x + 11.0f + Fonts.comfortaaBold_12.getStringWidth(this.setting.getName()) + this.changeAnim.anim, y + 8.5f + this.changeAnim.anim / 4.0f * (float)(this.changeAnim.to == 0.0f ? -1 : 1), ColorUtils.swapAlpha(-1, MathUtils.clamp((175.0f - 175.0f * MathUtils.clamp(this.changeAnim.anim / 4.0f, 0.0f, 1.0f)) * scaledAlphaPercent, 26.0f, 175.0f)));
            }
        }
        GlStateManager.enableAlpha();
        this.drawArrow(x + this.getWidth() - 12.0f, y + 6.0f, ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), MathUtils.clamp(26.0f * (this.ishover(x + 4.0f, y + 2.0f, x + this.getWidth() - 4.0f, y + getHeight + 2.0f, mouseX, mouseY) ? 1.75f : 1.0f) * scaledAlphaPercent + 175.0f * this.anim.anim * scaledAlphaPercent, 0.0f, 255.0f)));
        float modeHeight = this.modeHeight(overSize);
        float height = 17.0f;
        StencilUtil.initStencilToWrite();
        RenderUtils.drawRect(x + 5.0f, y + 17.0f, x + this.getWidth() - 5.0f, y + getHeight - 3.0f, ColorUtils.getColor(255, 255, 255, 60));
        StencilUtil.readStencilBuffer(1);
        if (getHeight > 18.5f) {
            boolean hoverAny = this.ishover(x + 4.0f, y + 2.0f, x + this.getWidth() - 4.0f, y + getHeight - 1.0f, mouseX, mouseY);
            if (hoverAny && TargetHUD.get != null && this.setting.module.getName().equalsIgnoreCase("TargetHUD") && this.setting.getName().equalsIgnoreCase(TargetHUD.get.Mode.getName()) && TargetHUD.get.isActived()) {
                TargetHUD.get.framesShowingForce = 2;
            }
            int index = 0;
            for (String mode : this.setting.modes) {
                float ciclePC01 = MathUtils.clamp((float)index / ((float)this.setting.modes.length - 1.0f), 0.0f, 1.0f);
                float waveCiclePC = (float)MathUtils.easeOutCirc(MathUtils.valWave01(ciclePC01));
                CFontRenderer modedFont = overSize ? (mode.equalsIgnoreCase(this.setting.currentMode) ? Fonts.comfortaaBold_14 : Fonts.comfortaaBold_12) : (mode.equalsIgnoreCase(this.setting.currentMode) ? Fonts.comfortaaBold_17 : Fonts.comfortaaBold_13);
                float width = modedFont.getStringWidth(mode) / 2.0f;
                if (height < getHeight) {
                    int rectCol = ColorUtils.swapAlpha(ColorUtils.getColor(10), 90.0f * scaledAlphaPercent * scaledAlphaPercent);
                    int rectCol2 = ColorUtils.swapAlpha(ColorUtils.getColor(0), 200.0f * scaledAlphaPercent * scaledAlphaPercent);
                    float rectX = x + 6.0f;
                    float rectY = y + height;
                    float rectX2 = x + this.getWidth() - 6.0f;
                    float rectY2 = y + height + modeHeight - 0.5f;
                    RenderUtils.drawAlphedRect(rectX + 0.5f, rectY, rectX2 - 0.5f, rectY + 0.5f, rectCol2);
                    RenderUtils.drawAlphedRect(rectX + 0.5f, rectY + 0.5f, rectX2 - 0.5f, rectY2 - 0.5f, rectCol);
                    RenderUtils.drawAlphedRect(rectX + 1.0f, rectY2 - 0.5f, rectX2 - 1.0f, rectY2, rectCol2);
                    RenderUtils.drawAlphedRect(rectX, rectY + 0.5f, rectX + 0.5f, rectY2 - 1.0f, rectCol2);
                    RenderUtils.drawAlphedRect(rectX2 - 0.5f, rectY + 0.5f, rectX2, rectY2 - 1.0f, rectCol2);
                    float effect = 0.0f;
                    if (scaledAlphaPercent * 255.0f >= 33.0f) {
                        float yn = rectY + (rectY2 - rectY) / 2.0f - modedFont.getHeight() / 2.0f + 0.5f + (mode.equalsIgnoreCase(this.setting.currentMode) ? -this.changeAnim.anim / 2.5f : 0.0f);
                        yn = (float)((int)(yn * 2.0f)) / 2.0f;
                        if (mode.equalsIgnoreCase(this.setting.currentMode)) {
                            float f3 = 0;
                            effect = this.changeAnim.anim / 4.5f * (this.changeAnim.anim / 4.5f);
                            if (f3 * 255.0f > 10.0f) {
                                int col1 = ColorUtils.getOverallColorFrom(ClickGuiScreen.getColor((int)yn, this.setting.module.category), ClickGuiScreen.getColor((int)yn + 180, this.setting.module.category), (int)(yn / 2000.0f));
                                col1 = ColorUtils.swapAlpha(col1, (float)ColorUtils.getAlphaFromColor(col1) * (scaledAlphaPercent * this.anim.anim));
                                modedFont.drawStringWithBloom(mode, x + this.getWidth() / 2.0f - width + 0.5f, yn, ColorUtils.getOverallColorFrom(ColorUtils.swapAlpha(-1, ColorUtils.getAlphaFromColor(col1)), col1, effect), effect * 0.175f, (int)(modeHeight * 2.0f));
                            } else {
                                effect = 0.0f;
                            }
                            float xn = x + this.getWidth() / 2.0f - width + width * 2.0f / (float)mode.length() / 4.0f;
                            xn -= modedFont.getStringWidth(mode);
                            float index2 = 0.0f;
                            for (char c : mode.toCharArray()) {
                                int col1 = ColorUtils.getOverallColorFrom(ClickGuiScreen.getColor((int)index2 * 5, this.setting.module.category), ClickGuiScreen.getColor(index * 5 + 180, this.setting.module.category), (float)index / modedFont.getStringWidth(mode) % 1.0f);
                                if (ColorUtils.getAlphaFromColor(col1 = ColorUtils.swapAlpha(col1, (float)ColorUtils.getAlphaFromColor(col1) * (scaledAlphaPercent * this.anim.anim) * (1.0f - effect))) >= 32) {
                                    modedFont.drawString(String.valueOf(c), modedFont.getStringWidth(mode) + index2 + xn + 0.5f, yn + 0.5f, ColorUtils.swapDark(col1, ColorUtils.getBrightnessFromColor(col1) / 3.0f));
                                    modedFont.drawString(String.valueOf(c), modedFont.getStringWidth(mode) + index2 + xn, yn, col1);
                                }
                                index2 += modedFont.getStringWidth(String.valueOf(c));
                            }
                        } else {
                            modedFont.drawStringWithShadow(mode, x + this.getWidth() / 2.0f - width, yn, ColorUtils.swapAlpha(ColorUtils.getColor((int)(140.0f + 40.0f * waveCiclePC)), MathUtils.clamp(175.0f * (this.anim.anim / 2.0f + 0.5f) * scaledAlphaPercent, 26.0f, 255.0f)));
                        }
                    }
                }
                height += modeHeight;
                ++index;
            }
        }
        StencilUtil.uninitStencilBuffer();
    }

    @Override
    public void mouseClicked(int x, int y, int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(x, y, mouseX, mouseY, mouseButton);
        if (this.ishover(x + 4, y + 2, (float)x + this.getWidth() - 4.0f, (float)y + this.getHeight() + 2.0f, mouseX, mouseY)) {
            boolean overSize = this.isOverSize();
            float modeHeight = this.modeHeight(overSize);
            if (mouseButton == 0) {
                int curMode = -1;
                float height = 16.5f;
                if (this.setting instanceof ModeSettings && this.open) {
                    for (String mode : this.setting.modes) {
                        if (this.ishover(x + 6, (float)y + height, (float)x + this.getWidth() - 6.0f, (float)y + height + modeHeight - 0.5f, mouseX, mouseY) && (curMode = (int)(height / modeHeight) - 1) > -1) break;
                        height += modeHeight;
                    }
                    try {
                        if (curMode != -1) {
                            if (!this.setting.currentMode.equalsIgnoreCase(this.setting.modes[curMode])) {
                                this.setting.currentMode = this.setting.modes[curMode];
                                ClientTune.get.playGuiScreenChangeModeSong(true);
                                this.changeAnim.to = 4.5f;
                                if (this.setting.modes.length < 6) {
                                    this.playClose = true;
                                    this.open = false;
                                }
                            } else {
                                ClientTune.get.playGuiScreenChangeModeSong(false);
                                this.changeAnim.setAnim(0.75f);
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if ((mouseButton == 1 || mouseButton == 0) && this.ishover(x + 4, y + 4, (float)x + this.getWidth() - 5.0f, y + (this.open ? 18 : 20), mouseX, mouseY)) {
                boolean bl = this.open = !this.open;
                if (this.open) {
                    Client.clickGuiScreen.panels.stream().filter(panel -> panel.open).filter(panel -> panel.category == this.setting.module.category).forEach(panel -> panel.mods.stream().filter(mod -> mod.open).filter(mod -> this.setting.module == mod.module).forEach(module -> module.sets.stream().map(Set::getHasModes).filter(Objects::nonNull).filter(modes -> modes != this).filter(modes -> modes.open).forEach(set -> {
                        set.open = false;
                        ClientTune.get.playGuiCheckOpenOrCloseSong(false);
                    })));
                }
                if (this.open) {
                    ClientTune.get.playGuiCheckOpenOrCloseSong(true);
                } else {
                    this.playClose = true;
                }
            }
        }
    }

    public void drawArrow(float xpos, float ypos, int color) {
        GL11.glPushMatrix();
        float ex = 4.5f;
        float xp = xpos - ex / 2.0f * (-this.arrow.anim / 90.0f);
        float yp = ypos + ex / 4.0f * (-this.arrow.anim / 90.0f);
        RenderUtils.customRotatedObject2D(xp, yp, ex / 2.0f, ex, this.arrow.anim - 90.0f);
        ArrayList<Vec2f> vec2fs = new ArrayList<Vec2f>();
        vec2fs.add(new Vec2f(xp, yp));
        vec2fs.add(new Vec2f(xp - ex, yp + ex));
        vec2fs.add(new Vec2f(xp + ex, yp + ex));
        RenderUtils.drawSome(vec2fs, color);
        GL11.glPopMatrix();
    }

    @Override
    public float getWidth() {
        return 118.0f;
    }

    @Override
    public float getHeight() {
        float modeHeight = this.modeHeight(this.isOverSize());
        return 18.0f + modeHeight * this.anim.getAnim() * (float)this.setting.modes.length + (this.open ? 1.5f : 0.0f);
    }
}
