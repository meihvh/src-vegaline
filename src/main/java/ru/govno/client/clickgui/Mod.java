/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.input.Keyboard
 *  org.lwjgl.opengl.GL11
 */
package ru.govno.client.clickgui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.clickgui.CheckBox;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.clickgui.Colors;
import ru.govno.client.clickgui.Comp;
import ru.govno.client.clickgui.Modes;
import ru.govno.client.clickgui.Panel;
import ru.govno.client.clickgui.Set;
import ru.govno.client.clickgui.Slider;
import ru.govno.client.clickgui.TriangleGroup;
import ru.govno.client.module.DemandPC;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.ClickGui;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.module.settings.Settings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;

public class Mod
extends Comp {
    boolean binding;
    boolean prevHover;
    AnimationUtils bindShowAnim = new AnimationUtils(0.0f, 0.0f, 0.1f);
    AnimationUtils textureShowAnim = new AnimationUtils(0.0f, 0.0f, 0.04f);
    AnimationUtils bindingAnim = new AnimationUtils(0.0f, 0.0f, 0.1f);
    AnimationUtils bindHoldAnim = new AnimationUtils(0.0f, 0.0f, 0.1f);
    AnimationUtils bindWaveAnim = new AnimationUtils(0.0f, 0.0f, 0.05f);
    TimerHelper holdBindTimer = new TimerHelper();
    private ResourceLocation moduleTexture = null;
    int keyBindToSet = -1;
    AnimationUtils setsAnim = new AnimationUtils(0.0f, 0.0f, 0.1f);
    AnimationUtils alpha = new AnimationUtils(0.0f, 0.0f, 0.1f);
    AnimationUtils toggleAnim = new AnimationUtils(0.0f, 0.0f, 0.06f);
    ArrayList<Set> sets = new ArrayList();
    boolean open = false;
    AnimationUtils openAnim = new AnimationUtils(this.getHeight(), this.getHeight(), 0.125f);
    AnimationUtils sdvig = new AnimationUtils(0.0f, 0.0f, 0.125f);
    AnimationUtils bindConflictAnim = new AnimationUtils(0.0f, 0.0f, 0.075f);
    AnimationUtils bindConflictAngleAnim = new AnimationUtils(0.0f, 0.0f, 0.05f);
    Module module;
    boolean last;
    boolean first;
    TriangleGroup triangleGroup;
    float tgExt;
    float tgH;
    List<NanoBindParticle> nanoBindParticlesList = new ArrayList<NanoBindParticle>();
    static float xn;
    static float yn;
    static float alphaD;
    static String descript;
    float height;
    float prevHeight;
    public boolean wantToClick = true;
    public boolean wantToClick2 = true;

    float maxBindTime() {
        return this.keyBindToSet == 211 ? 700.0f : 800.0f;
    }

    void updateBinding() {
        if (!this.binding || this.keyBindToSet == -1 || !Keyboard.isKeyDown((int)this.keyBindToSet)) {
            this.bindHoldAnim.to = 0.0f;
            this.holdBindTimer.reset();
        }
        if (this.binding) {
            if (this.keyBindToSet != -1 && this.holdBindTimer.hasReached(this.maxBindTime()) && this.bindHoldAnim.getAnim() > 0.9722222f) {
                int prevBind = this.module.getBind();
                this.module.setBind(this.keyBindToSet == 211 ? 0 : this.keyBindToSet);
                if (prevBind != this.module.getBind()) {
                    ClientTune.get.playGuiModuleBindSong(this.module.getBind() != 0);
                    this.bindWaveAnim.setAnim(1.0f);
                }
                this.binding = false;
            }
            this.bindHoldAnim.to = MathUtils.clamp((float)this.holdBindTimer.getTime() / this.maxBindTime(), 0.0f, 1.0f);
        }
        this.bindingAnim.to = this.binding || this.bindWaveAnim.getAnim() > 0.004f ? 1.0f : 0.0f;
        this.bindHoldAnim.speed = 0.1f;
        this.nanoBindParticlesRemoveAuto();
    }

    @Override
    public void keyPressed(int key) {
        super.keyPressed(key);
        if (this.binding && !this.holdBindTimer.hasReached(50.0) && key != 42 && key != 56 && key != 58 && key != 1) {
            this.keyBindToSet = key;
        }
        if (!ClickGuiScreen.colose) {
            this.sets.forEach(set -> set.keyPressed(key));
        }
    }

    public void onGuiClosed() {
        this.binding = false;
        this.keyBindToSet = -1;
        this.sets.forEach(set -> set.onGuiClosed());
    }

    private boolean hasBindConflict() {
        return this.module.getBind() != 0 && Client.clickGuiScreen.panels.stream().anyMatch(panel -> panel.mods.stream().anyMatch(mod -> mod != this && mod.module.getBind() != 0 && mod.module.getBind() == this.module.getBind()));
    }

    private boolean hasBadBind() {
        int bind = this.module.getBind();
        return this.module.getName().equalsIgnoreCase("ClickGui") && bind == 54 || bind == 17 || bind == 30 || bind == 31 || bind == 32 || bind == 33 || bind == 18 || bind == 16 || bind == 53;
    }

    public Mod(Module module, boolean last, boolean first) {
        this.last = last;
        this.first = first;
        this.module = module;
        for (Settings setting : module.settings) {
            if (setting instanceof BoolSettings) {
                BoolSettings boolSet = (BoolSettings)setting;
                this.sets.add(new CheckBox(boolSet));
                continue;
            }
            if (setting instanceof FloatSettings) {
                FloatSettings floatSet = (FloatSettings)setting;
                this.sets.add(new Slider(floatSet));
                continue;
            }
            if (setting instanceof ModeSettings) {
                ModeSettings modeSet = (ModeSettings)setting;
                this.sets.add(new Modes(modeSet));
                continue;
            }
            if (!(setting instanceof ColorSettings)) continue;
            ColorSettings colorSet = (ColorSettings)setting;
            this.sets.add(new Colors(colorSet));
        }
        this.tgExt = 20.0f;
        float setsH = 0.0f;
        for (Set set : this.sets) {
            float h = 0.0f;
            if (set.setting instanceof BoolSettings) {
                h = set.getHeight();
            } else if (set.setting instanceof FloatSettings) {
                h = set.getHeight();
            } else if (set.setting instanceof ModeSettings) {
                int n;
                Settings settings = set.setting;
                if (settings instanceof ModeSettings) {
                    ModeSettings mode = (ModeSettings)settings;
                    n = mode.modes.length;
                } else {
                    n = 0;
                }
                h = 18.0f + 13.0f * (float)n;
            } else if (set.setting instanceof ColorSettings) {
                h = 58.0f;
            }
            setsH += h + 1.5f;
        }
        this.tgH = setsH;
        String thisTextureName = module.getName().toLowerCase() + "_cover";
        String format = ".jpg";
        try {
            ResourceLocation resLoc;
            this.moduleTexture = resLoc = new ResourceLocation("vegaline/ui/clickgui/components/mod/modules/covers/" + thisTextureName + format);
            BufferedImage buffer = null;
            try (InputStream inputStream = Minecraft.getMinecraft().getResourceManager().getResource(this.moduleTexture).getInputStream();){
                buffer = ImageIO.read(inputStream);
            }
            catch (IOException iOException) {
                // empty catch block
            }
            if (buffer != null && this.moduleTexture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(this.moduleTexture);
                Minecraft.getMinecraft().getTextureManager().getTexture(this.moduleTexture).setBlurMipmap(true, false);
            } else {
                Minecraft.getMinecraft().getTextureManager().deleteTexture(this.moduleTexture);
                this.moduleTexture = null;
            }
        }
        catch (Exception e) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(this.moduleTexture);
            this.moduleTexture = null;
        }
    }

    void addNanoBindParticle(float circleX, float circleY, float holdProgress, float ofRange) {
        this.nanoBindParticlesList.add(new NanoBindParticle(circleX, circleY, holdProgress, ofRange));
    }

    void nanoBindParticlesRemoveAuto() {
        if (!this.nanoBindParticlesList.isEmpty()) {
            this.nanoBindParticlesList.removeIf(NanoBindParticle::toRemove);
        }
    }

    void drawAllBindNanoParticles(float alphaPC) {
        this.nanoBindParticlesList.forEach(nanoBindParticle -> nanoBindParticle.drawAndMovement(alphaPC));
    }

    String getKeyName(int key, boolean staples) {
        return key == 0 ? (staples ? "[NONE]" : "-") : (staples ? "[" : "") + Keyboard.getKeyName((int)key).toUpperCase() + (staples ? "]" : "");
    }

    public void drawScreen(float x, float y, int step, int mouseX, int mouseY, float partialTicks, ScaledResolution sr) {
        String bindShowText;
        int warnColor;
        boolean badBind;
        float demandBoolAPC;
        boolean hasSets;
        boolean canShowModTexture;
        boolean hover;
        boolean canRenderModule;
        this.prevHeight = this.height;
        this.height = this.getHeight();
        this.openAnim.getAnim();
        boolean bl = canRenderModule = !(x < -this.getWidth() - 40.0f) && !(x > (float)(sr.getScaledWidth() + 40)) && !(y < -this.height - 40.0f) && !(y > (float)(sr.getScaledHeight() + 40)) || MathUtils.getDifferenceOf(this.openAnim.anim, this.height) > 0.001f;
        if (!canRenderModule) {
            return;
        }
        float lpSCFactor = ScaledResolution.lpSCFactor();
        String modulename = this.module.getName();
        int ScaledAlpha = (int)ClickGuiScreen.globalAlpha.anim + 1;
        float ScaledAlphaPercent = (float)ScaledAlpha / 255.0f;
        super.drawScreen(x, y, step, mouseX, mouseY, partialTicks);
        if (this.toggleAnim.to == 1.0f && (double)this.toggleAnim.getAnim() > 0.99) {
            this.toggleAnim.setAnim(1.0f);
            this.toggleAnim.to = 0.0f;
        }
        if (this.toggleAnim.to == 0.0f && (double)this.toggleAnim.getAnim() < 0.001) {
            this.toggleAnim.setAnim(0.0f);
        }
        int i = 20;
        this.openAnim.to = this.height;
        this.openAnim.speed = 0.15f + (Math.abs(MathUtils.getDifferenceOf(this.openAnim.anim, this.height)) < 5.5f || this.height < this.openAnim.anim && this.open ? 0.5f : 0.0f);
        this.bindShowAnim.to = Keyboard.isKeyDown((int)56) && this.module.bind != 0 ? 1.0f : 0.0f;
        boolean bl2 = hover = this.ishover(x, y, x + this.getWidth(), y + this.height, mouseX, mouseY) && !Client.clientColosUI.isHovered();
        if (this.prevHover != hover) {
            if (hover && !this.binding && !(this.bindingAnim.anim > 0.003f) && !this.open && !(MathUtils.getDifferenceOf(this.openAnim.anim, this.openAnim.to) > 0.0f) && (double)MathUtils.getDifferenceOf(ClickGuiScreen.scrollSmoothX, 0.0f) < 0.1 && (double)MathUtils.getDifferenceOf(ClickGuiScreen.scrollSmoothY, 0.0f) < 0.1) {
                ClientTune.get.playGuiScreenModuleHoveringSong();
            }
            this.prevHover = hover;
        }
        boolean bl3 = canShowModTexture = this.moduleTexture != null && !this.open && ClickGui.instance.ModsPreviewImages.canBeRender();
        if (canShowModTexture) {
            boolean currentTob;
            boolean bl4 = currentTob = hover || this.sdvig.anim > 0.05f || ClickGui.instance.AlwaysModsPreview.getBool();
            if (!currentTob && this.textureShowAnim.to == 1.0f && this.textureShowAnim.anim < 0.9f) {
                currentTob = true;
            }
            float currentTo = currentTob ? 1 : 0;
            if (MathUtils.getDifferenceOf((float)currentTo, this.textureShowAnim.anim) < 0.003f) {
                this.textureShowAnim.setAnim((float)currentTo);
            }
            this.textureShowAnim.to = (float)currentTo;
        } else {
            this.textureShowAnim.to = 0.0f;
        }
        this.textureShowAnim.speed = this.open ? 0.05f : (this.textureShowAnim.to == 0.0f ? 0.03f : 0.0175f);
        this.textureShowAnim.getAnim();
        float textureShowAnim = this.textureShowAnim.anim * ClickGui.instance.ModsPreviewImages.getAnimation();
        boolean bl5 = canShowModTexture = textureShowAnim * 255.0f >= 1.0f;
        float f = this.module.actived ? 0.05f : (this.alpha.speed = hover ? 0.1f : 0.02f);
        this.alpha.to = this.module.actived ? ScaledAlphaPercent * 255.0f : (hover || this.open || this.binding ? 100.0f : 0.0f);
        this.alpha.getAnim();
        this.sdvig.to = (float)(this.binding ? -1 : (this.ishover(x, y, x + this.getWidth(), y + 16.0f, mouseX, mouseY) && !this.open && !Client.clientColosUI.isHovered() && !Client.clickGuiScreen.moduleHasEqualSearch(this.module) ? 5 : 0)) + (Client.clickGuiScreen.moduleHasEqualSearch(this.module) ? (System.currentTimeMillis() % 700L >= 550L ? 3.5f : 1.0f) : 0.0f);
        this.sdvig.speed = this.ishover(x, y, x + this.getWidth(), y + 16.0f, mouseX, mouseY) || this.binding ? 0.3f : 0.05f;
        this.sdvig.getAnim();
        this.bindShowAnim.getAnim();
        float future = ClickGui.instance.FuturisticModsGet;
        if (this.alpha.anim >= 1.0f || canShowModTexture) {
            float alphaPC = MathUtils.clamp(this.alpha.anim / 255.0f * (ClickGuiScreen.globalAlpha.anim / 255.0f) * ClickGuiScreen.scale.anim, 0.0f, 255.0f);
            int col1 = ClickGuiScreen.getColor((int)y, this.module.category);
            int col2 = ClickGuiScreen.getColor((int)(this.getWidth() + y), this.module.category);
            int col3 = ClickGuiScreen.getColor((int)(this.getWidth() + y + 20.0f), this.module.category);
            int col4 = ClickGuiScreen.getColor((int)(y + 20.0f), this.module.category);
            int pCol1 = col1;
            int pCol2 = col2;
            int pCol3 = col3;
            int pCol4 = col4;
            float gc = 1.5f + 1.5f * future;
            col1 = ColorUtils.swapAlpha(col1, (float)ColorUtils.getAlphaFromColor(col1) * alphaPC / gc);
            col2 = ColorUtils.swapAlpha(col2, (float)ColorUtils.getAlphaFromColor(col2) * alphaPC / gc);
            col3 = ColorUtils.swapAlpha(col3, (float)ColorUtils.getAlphaFromColor(col3) * alphaPC / gc);
            col4 = ColorUtils.swapAlpha(col4, (float)ColorUtils.getAlphaFromColor(col4) * alphaPC / gc);
            Runnable setsOverlayStart = () -> {
                if (this.openAnim.anim > 21.0f) {
                    float radius = MathUtils.clamp(MathUtils.getDifferenceOf(this.openAnim.anim, 21.0f), 0.0f, 2.5f);
                    StencilUtil.initStencilToWrite();
                    RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(x + 2.5f, y + 17.5f, x + this.getWidth() - 1.5f, y + this.openAnim.anim - 1.5f, radius, 0.0f, -1, -1, -1, -1, false, true, false);
                    StencilUtil.readStencilBuffer(0);
                }
            };
            if (this.openAnim.anim > 21.0f) {
                float radius = MathUtils.clamp(MathUtils.getDifferenceOf(this.openAnim.anim, 21.0f), 0.0f, 2.5f);
                StencilUtil.initStencilToWrite();
                RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(x + 2.5f, y + 17.5f, x + this.getWidth() - 1.5f, y + this.openAnim.anim - 1.5f, radius, 0.0f, -1, -1, -1, -1, false, true, false);
                StencilUtil.readStencilBuffer(1);
                RenderUtils.drawInsideFullRoundedFullGradientShadowRectWithBloomBool(x + 2.5f, y + 17.5f, x + this.getWidth() - 1.5f, y + this.openAnim.anim - 1.5f, 0.0f, radius, col1, col2, col3, col4, true);
                StencilUtil.readStencilBuffer(0);
            }
            if (future != 0.0f) {
                float fooOffset = MathUtils.lerp(1.25f, 0.0f, Math.min((float)MathUtils.easeOutBack(textureShowAnim * textureShowAnim * textureShowAnim * textureShowAnim * textureShowAnim * textureShowAnim * 2.25f), 1.0f));
                float ticoOffset = 0.5f;
                float x2 = x + this.getWidth() + 0.5f - fooOffset;
                float y2 = y + this.openAnim.anim + 1.0f - fooOffset;
                float delta = 3.5f;
                float stepI = 4.625f;
                float offsetPCTo2 = 1.0f / delta;
                int timeMax = 1400;
                int timeStepI0 = (int)((float)timeMax / delta);
                long ms = System.currentTimeMillis();
                int i0 = 0;
                int i1 = 0;
                float prevX = x + 1.0f + fooOffset;
                boolean a = false;
                BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
                for (float fX = x + 1.0f + fooOffset; fX < x2 + stepI; fX += stepI) {
                    if (fX > x2) {
                        fX = x2;
                    }
                    float prevY = y + 0.5f + fooOffset;
                    for (float fY = y + 0.5f + fooOffset; fY < y2 + stepI; fY += stepI) {
                        if (fY > y2) {
                            fY = y2;
                        }
                        if (145.0f * alphaPC * future >= 1.0f) {
                            int fadeCol = ColorUtils.swapAlpha(ColorUtils.getQuadColor(col1, col2, col3, col4, fX / x2, fY / y2), 145.0f * alphaPC * future);
                            float i0PC = (float)((ms + (long)((i0 + i1) * timeStepI0)) % (long)timeMax) / (float)timeMax;
                            float pcGlobal = (float)MathUtils.easeInOutQuadWave(i0PC);
                            float pcGlobal2 = (float)MathUtils.easeInOutQuadWave((i0PC + offsetPCTo2) % 1.0f);
                            int cc0 = ColorUtils.getOverallColorFrom(0, fadeCol, pcGlobal);
                            int cc1 = ColorUtils.getOverallColorFrom(0, fadeCol, pcGlobal2);
                            float i0PCN = (float)((ms + (long)((i0 + i1 - 1) * timeStepI0)) % (long)timeMax) / (float)timeMax;
                            float pcGlobalN = (float)MathUtils.easeInOutQuadWave(i0PCN);
                            float pcGlobal2N = (float)MathUtils.easeInOutQuadWave((i0PCN + offsetPCTo2) % 1.0f);
                            int cc0N = ColorUtils.getOverallColorFrom(0, fadeCol, pcGlobalN);
                            int cc1N = ColorUtils.getOverallColorFrom(0, fadeCol, pcGlobal2N);
                            if (ColorUtils.getAlphaFromColor(cc0) >= 8 || ColorUtils.getAlphaFromColor(cc1) >= 8) {
                                if (!a) {
                                    bufferBuilder.begin(0, DefaultVertexFormats.POSITION_COLOR);
                                    a = true;
                                }
                                bufferBuilder.pos(fX + ticoOffset, prevY - ticoOffset).color(cc0).endVertex();
                                bufferBuilder.pos(prevX - ticoOffset, prevY - ticoOffset).color(cc0N).endVertex();
                                bufferBuilder.pos(prevX - ticoOffset, fY + ticoOffset).color(cc1N).endVertex();
                                bufferBuilder.pos(fX + ticoOffset, fY + ticoOffset).color(cc1).endVertex();
                            }
                        }
                        prevY = fY;
                        ++i0;
                    }
                    ++i1;
                    i0 = 0;
                    prevX = fX;
                }
                if (a) {
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.disableLighting();
                    GL11.glDisable((int)3008);
                    GL11.glShadeModel((int)7425);
                    GL11.glPointSize((float)(0.75f * ScaledResolution.lpSCFactor()));
                    GL11.glEnable((int)2832);
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE_MINUS_CONSTANT_ALPHA, GlStateManager.DestFactor.ZERO);
                    Tessellator.getInstance().vboUploader.draw(bufferBuilder, false);
                    bufferBuilder.drawMode = 7;
                    Tessellator.getInstance().draw();
                    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.DestFactor.ZERO);
                    GL11.glPointSize((float)1.0f);
                    GL11.glEnable((int)2832);
                    GL11.glShadeModel((int)7424);
                    GL11.glEnable((int)3008);
                    GL11.glShadeModel((int)7424);
                    GlStateManager.enableTexture2D();
                }
            }
            if (!this.last && !this.first) {
                RenderUtils.drawFullGradientRectPro(x + 1.0f, y + 0.5f, x + this.getWidth() + 0.5f, y + this.openAnim.anim + 1.0f, col4, col3, col2, col1, true);
            }
            float r;
            if (this.last) {
                r = 3.0f;
                RenderUtils.drawFullGradientRectPro(x + 1.0f, y + 0.5f, x + this.getWidth() + 0.5f, y + this.openAnim.anim + 1.0f - r, col4, col3, col2, col1, true);
                RenderUtils.drawFullGradientRectPro(x + 1.0f + r, y + this.openAnim.anim + 1.0f - r, x + this.getWidth() + 0.5f - r, y + this.openAnim.anim + 1.0f, col4, col3, col3, col4, true);
                RenderUtils.drawCroneShadow(x + 1.0f + r, y + this.openAnim.anim + 1.0f - r, -90, 0, 0.0f, r - 0.5f, col4, col4, true);
                RenderUtils.drawCroneShadow(x + 1.0f + r, y + this.openAnim.anim + 1.0f - r, -90, 0, r - 0.5f, 0.5f, col4, ColorUtils.swapAlpha(col4, 0.0f), true);
                RenderUtils.drawCroneShadow(x + this.getWidth() + 0.5f - r, y + this.openAnim.anim + 1.0f - r, 0, 90, 0.0f, r - 0.5f, col3, col3, true);
                RenderUtils.drawCroneShadow(x + this.getWidth() + 0.5f - r, y + this.openAnim.anim + 1.0f - r, 0, 90, r - 0.5f, 0.5f, col3, ColorUtils.swapAlpha(col3, 0.0f), true);
            } else if (this.first) {
                r = 3.0f;
                RenderUtils.drawFullGradientRectPro(x + 1.0f, y + 0.5f + r, x + this.getWidth() + 0.5f, y + this.openAnim.anim + 1.0f, col4, col3, col2, col1, true);
                RenderUtils.drawFullGradientRectPro(x + 1.0f + r, y + 0.5f, x + this.getWidth() + 0.5f - r, y + 0.5f + r, col1, col2, col2, col1, true);
                RenderUtils.drawCroneShadow(x + 1.0f + r, y + 0.5f + r, 180, 270, 0.0f, r - 0.5f, col1, col1, true);
                RenderUtils.drawCroneShadow(x + 1.0f + r, y + 0.5f + r, 180, 270, r - 0.5f, 0.5f, col1, ColorUtils.swapAlpha(col1, 0.0f), true);
                RenderUtils.drawCroneShadow(x + this.getWidth() + 0.5f - r, y + 0.5f + r, -270, -180, 0.0f, r - 0.5f, col2, col2, true);
                RenderUtils.drawCroneShadow(x + this.getWidth() + 0.5f - r, y + 0.5f + r, -270, -180, r - 0.5f, 0.5f, col2, ColorUtils.swapAlpha(col2, 0.0f), true);
            }
            if (canShowModTexture) {
                float aPC = textureShowAnim * Math.min(ScaledAlphaPercent * ScaledAlphaPercent, 1.0f) * Math.max(Math.min(this.alpha.anim / 255.0f, 0.8f + Math.min(this.sdvig.anim / 5.0f * 0.15f, 0.15f)), 0.15f + 0.35f * Math.min(this.sdvig.anim / 5.0f, 1.0f));
                float animPC = this.textureShowAnim.to == 0.0f ? (float)MathUtils.easeOutCubic(textureShowAnim) : (float)MathUtils.easeOutCubic(textureShowAnim * textureShowAnim * textureShowAnim);
                float animIntervalAddition = 0.2f * animPC;
                long delayEaseAddition = 6000L;
                float x1 = x + 1.0f;
                float x2 = x + this.getWidth() + 0.5f;
                float y1 = y + 0.5f;
                float y2 = y + 17.0f;
                this.drawModuleTexture(x1, y1, x2, y2, aPC, animPC += -animIntervalAddition * (float)MathUtils.easeInOutQuadWave((float)((System.currentTimeMillis() - (long)(y % ((float)sr.getScaledHeight() * 2.0f) / (float)sr.getScaledHeight() * (float)delayEaseAddition * 2.0f)) % delayEaseAddition) / (float)delayEaseAddition), textureShowAnim, setsOverlayStart);
            }
            if (!this.first && !this.last && (double)this.sdvig.anim > 0.25) {
                float h = 17.0f;
                RenderUtils.glRenderStart();
                GL11.glPointSize((float)(0.75f * lpSCFactor));
                RenderUtils.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
                for (float ys = y + 1.0f; ys < y + h; ys += 0.25f) {
                    float centerDiff = (ys - (y + 0.5f)) / (h - 1.0f);
                    centerDiff = (float)MathUtils.easeInOutQuadWave(centerDiff);
                    float xs = x + 1.5f + MathUtils.clamp(this.sdvig.anim / 1.5f - 0.5f, 0.0f, 10.0f) * centerDiff;
                    RenderUtils.glColor(ColorUtils.swapAlpha(-1, 255.0f * alphaPC * MathUtils.clamp(this.sdvig.anim / 3.0f, 0.0f, 1.0f) * MathUtils.clamp(0.5f + centerDiff / 2.0f, 0.0f, 1.0f)));
                    GL11.glVertex2d((double)xs, (double)ys);
                    RenderUtils.buffer.pos(xs, ys).color(ColorUtils.swapAlpha(-1, 255.0f * alphaPC * MathUtils.clamp(this.sdvig.anim / 3.0f, 0.0f, 1.0f) * MathUtils.clamp(0.5f + centerDiff / 2.0f, 0.0f, 1.0f))).endVertex();
                }
                RenderUtils.tessellator.draw();
                GL11.glPointSize((float)1.0f);
                RenderUtils.buffer.begin(9, DefaultVertexFormats.POSITION_COLOR);
                int fPolyCol = ColorUtils.swapAlpha(-1, this.alpha.anim / 4.0f * alphaPC * MathUtils.clamp(this.sdvig.anim / 2.0f, 0.0f, 1.0f));
                RenderUtils.buffer.pos(x + 1.0f, y + 0.5f).color(fPolyCol).endVertex();
                for (float ys = y + 0.5f; ys < y + 0.5f + h; ys += 0.5f) {
                    float centerDiff = (ys - (y + 0.5f)) / h;
                    centerDiff = (float)MathUtils.easeInOutQuadWave(centerDiff);
                    float xs = x + 1.0f + this.sdvig.anim / 1.5f * centerDiff;
                    RenderUtils.buffer.pos(xs, ys).color(fPolyCol).endVertex();
                }
                RenderUtils.buffer.pos(x + 1.0f, y + 0.5f + h).color(fPolyCol).endVertex();
                RenderUtils.tessellator.draw();
                RenderUtils.glRenderStop();
            }
            if (!this.open) {
                float highLightAPC = Math.min(this.sdvig.anim / 5.0f, 1.0f);
                highLightAPC = MathUtils.valWave01(highLightAPC) * alphaPC;
                if (hover) {
                    highLightAPC = MathUtils.lerp(MathUtils.lerp(highLightAPC, 1.0f, highLightAPC), 1.0f, highLightAPC);
                }
                if (highLightAPC * 255.0f > 1.0f) {
                    RenderUtils.drawLightContureRect(x + 1.5f, y + 1.0f, x + this.getWidth(), this.open ? (double)(y + this.openAnim.anim + 0.5f) : (double)(y + 16.5f), ColorUtils.getColor(255, 255, 255, 255.0f * highLightAPC));
                }
            }
            if (this.toggleAnim.getAnim() != 0.0f && this.toggleAnim.to != 0.0f) {
                float togPC = this.toggleAnim.anim;
                float togAlphaPC = ((double)this.toggleAnim.anim > 0.5 ? 1.0f - this.toggleAnim.anim : this.toggleAnim.anim) * 4.0f;
                togAlphaPC = togAlphaPC > 1.0f ? 1.0f : togAlphaPC;
                int togColor = ColorUtils.swapAlpha(ColorUtils.getOverallColorFrom(col1, this.module.isLocked() ? ColorUtils.getColor(255, 0, 0) : -1, 0.4f), Math.min(ScaledAlphaPercent * ScaledAlphaPercent * togAlphaPC * this.alpha.anim * (this.module.isLocked() ? 6.0f : 1.0f), 255.0f));
                if (ColorUtils.getAlphaFromColor(togColor) >= 1) {
                    float x1s = x + 1.0f;
                    float x2s = x + this.getWidth() + 0.5f;
                    float x2WPC = x1s + (x2s - x1s) * (this.module.actived ? togPC : 1.0f - togPC);
                    for (int j = 0; j < 2; ++j) {
                        RenderUtils.drawAlphedSideways(x1s, y + 0.5f, x2WPC, y + this.openAnim.anim + 1.0f, this.module.actived ? togColor : 0, togColor, true);
                        RenderUtils.drawAlphedSideways(x2WPC, y + 0.5f, x2s, y + this.openAnim.anim + 1.0f, togColor, 0, true);
                    }
                    if (this.module.actived) {
                        RenderUtils.drawAlphedSideways(x2WPC, y + 0.5f, x2s, y + 1.5f, ColorUtils.swapAlpha(togColor, 0.0f), togColor, true);
                        RenderUtils.drawAlphedSideways(x2WPC, y + this.openAnim.anim, x2s, y + this.openAnim.anim + 1.0f, ColorUtils.swapAlpha(togColor, 0.0f), togColor, true);
                        RenderUtils.drawAlphedSideways(x2s - 1.0f, y + 1.5f, x2s, y + this.openAnim.anim, togColor, togColor, true);
                    } else {
                        RenderUtils.drawAlphedSideways(x1s, y + 0.5f, x2WPC, y + 1.5f, togColor, ColorUtils.swapAlpha(togColor, 0.0f), true);
                        RenderUtils.drawAlphedSideways(x1s, y + this.openAnim.anim, x2WPC, y + this.openAnim.anim + 1.0f, togColor, ColorUtils.swapAlpha(togColor, 0.0f), true);
                        RenderUtils.drawAlphedSideways(x1s, y + 1.5f, x1s + 1.0f, y + this.openAnim.anim, togColor, togColor, true);
                    }
                }
            }
            StencilUtil.readStencilBuffer(1);
            if (this.openAnim.anim > 21.0f) {
                this.tgExt = 16.0f;
                if (this.triangleGroup == null) {
                    this.triangleGroup = TriangleGroup.gen(-this.tgExt / 2.0f, -this.tgExt - 12.0f, this.getWidth() + this.tgExt * 1.5f, this.tgH + this.tgExt + 60.0f, this.tgExt, 0.01f, 100L, 1800L, 0.75f);
                }
            } else if (this.triangleGroup != null) {
                this.triangleGroup = null;
            }
            if (this.openAnim.anim > 21.0f) {
                if (this.triangleGroup != null) {
                    float togAlphaPC = this.toggleAnim.to == 0.0f ? 0.0f : ((double)this.toggleAnim.anim > 0.5 ? 1.0f - this.toggleAnim.anim : this.toggleAnim.anim) * 4.0f;
                    togAlphaPC = togAlphaPC > 1.0f ? 1.0f : togAlphaPC;
                    float tLeft = this.module.actived ? 0.0f : togAlphaPC;
                    float tRight = this.module.actived ? togAlphaPC : 0.0f;
                    this.triangleGroup.setColors(ColorUtils.getOverallColorFrom(pCol1, ColorUtils.swapAlpha(-1, ColorUtils.getAlphaFromColor(pCol1)), tLeft / 2.0f), ColorUtils.getOverallColorFrom(pCol2, ColorUtils.swapAlpha(-1, ColorUtils.getAlphaFromColor(pCol2)), tRight / 3.0f), ColorUtils.getOverallColorFrom(pCol3, ColorUtils.swapAlpha(-1, ColorUtils.getAlphaFromColor(pCol3)), tRight / 3.0f), ColorUtils.getOverallColorFrom(pCol4, ColorUtils.swapAlpha(-1, ColorUtils.getAlphaFromColor(pCol4)), tLeft / 2.0f));
                    GL11.glTranslated((double)x, (double)y, (double)0.0);
                    this.triangleGroup.drawAllInZone(-10.0f, 16.0f, this.getWidth() + 20.0f, this.height + 12.0f, alphaPC / 5.0f, true);
                    GL11.glTranslated((double)(-x), (double)(-y), (double)0.0);
                }
                int n = i = this.module.isLocked() ? 35 : 19;
                if (this.module.isLocked()) {
                    Fonts.neverlose500_18.drawString("Settings was locked", x + this.getWidth() / 2.0f - Fonts.neverlose500_18.getStringWidth("Settings was locked") / 2.0f, y + 24.5f, ColorUtils.getColor(155, (int)(155.0f * ScaledAlphaPercent * ScaledAlphaPercent)));
                } else {
                    for (Set set2 : this.sets) {
                        if (!set2.setting.isVisible()) continue;
                        float h = set2.getHeight();
                        if ((float)i + h / 4.0f < this.openAnim.anim && y + (float)i >= -h && y + (float)i <= (float)(sr.getScaledHeight() + 1) && x + set2.getWidth() >= 0.0f && x <= (float)(sr.getScaledWidth() + 1)) {
                            set2.drawScreen(x + 1.0f, y + (float)i, step + 2, mouseX, mouseY, partialTicks);
                        }
                        i = (int)((float)i + (h + 1.0f));
                    }
                }
            }
            StencilUtil.uninitStencilBuffer();
        }
        this.updateBinding();
        float bindX = x + this.getWidth() - 15.5f;
        float bindY = y + 1.0f;
        float bindX2 = x + this.getWidth();
        float bindY2 = y + 16.5f;
        float bindW = bindX2 - bindX;
        float bindH = bindY2 - bindY;
        float bindCircleX = bindX + bindW / 2.0f;
        float bindCircleY = bindY + bindH / 2.0f;
        float bindAnim = this.bindingAnim.getAnim();
        if (bindAnim > 0.01f) {
            float bindAlpha = Math.min(bindAnim * 255.0f * ScaledAlphaPercent * ScaledAlphaPercent, 255.0f);
            float effectAPC = this.bindWaveAnim.getAnim() * ScaledAlphaPercent * ScaledAlphaPercent;
            this.bindWaveAnim.speed = 0.025f;
            int bindCircleColor = ColorUtils.swapAlpha(-1, bindAlpha);
            int bindCircleBGColor = ColorUtils.swapAlpha(0, bindAlpha / 4.25f);
            int bindTextColor = ColorUtils.swapAlpha(-1, bindAlpha);
            float holdProgress = this.bindHoldAnim.getAnim();
            if ((double)holdProgress < 0.025) {
                holdProgress = 0.0f;
            }
            int bindCircleProgress360 = (int)(360.0f * holdProgress);
            float bindCircleWidth = 2.0f + 2.5f * holdProgress * ScaledResolution.lpSCFactor();
            float bindCircleBGWidth = 2.0f + 2.5f * holdProgress * ScaledResolution.lpSCFactor();
            float bindProgressCircleRange = bindW * bindAnim / 2.0f - bindCircleWidth / 2.0f;
            float bindProgressCircleBGRange = bindW * bindAnim / 2.0f - bindCircleBGWidth / 2.0f;
            CFontRenderer bindFont = Fonts.comfortaaBold_14;
            String moduleBindText = this.getKeyName(this.module.getBind(), false).replace("-", "");
            if (moduleBindText.length() > 2) {
                moduleBindText = MathUtils.getStringPercent(moduleBindText, bindAnim * 2.0f);
            }
            float bindTextW = bindFont.getStringWidth(moduleBindText);
            float bindTextX = MathUtils.clamp(bindX + bindW / 2.0f - bindTextW / 2.0f, x, x + (moduleBindText.length() > 2 ? this.getWidth() - (bindW + 2.0f + bindTextW) : this.getWidth() - bindTextW - 1.0f));
            float bindTextY = bindY + bindH / 2.0f - 1.5f;
            if (bindAlpha >= 33.0f) {
                bindFont.drawStringWithBloomAndShadow(moduleBindText, bindTextX, bindTextY, bindTextColor, 0.7f, 6);
            }
            if ((double)holdProgress > 0.05) {
                this.addNanoBindParticle(0.0f, 0.0f, holdProgress, bindProgressCircleRange);
                this.addNanoBindParticle(0.0f, 0.0f, holdProgress + 0.9444444f, bindProgressCircleRange);
            }
            if (effectAPC <= 0.01f && moduleBindText.isEmpty()) {
                for (int j = 0; j < 3; ++j) {
                    this.addNanoBindParticle(0.0f, 0.0f, 0.0f, 0.0f);
                }
            }
            RenderUtils.drawClientCircleWithOverallToColor(bindCircleX, bindCircleY, bindProgressCircleRange, 359.0f, bindCircleBGWidth, bindAlpha / 255.0f, bindCircleBGColor, 1.0f);
            if ((float)bindCircleProgress360 > 1.0f && this.binding) {
                RenderUtils.drawClientCircleWithOverallToColor(bindCircleX, bindCircleY, bindProgressCircleRange, bindCircleProgress360, bindCircleWidth, bindAlpha / 255.0f, bindCircleColor, 1.0f);
            }
            if (effectAPC > 0.01f) {
                StencilUtil.uninitStencilBuffer();
                float effectAlpha = (double)effectAPC > 0.5 ? 1.0f - effectAPC : effectAPC;
                effectAlpha = effectAlpha < 0.0f ? 0.0f : ((effectAlpha *= 2.0f) > 1.0f ? 1.0f : effectAPC);
                bindCircleColor = ColorUtils.swapAlpha(-1, bindAlpha * effectAlpha);
                for (int ii = 0; ii <= 12; ++ii) {
                    this.addNanoBindParticle(0.0f, 0.0f, (float)ii / 12.0f - 0.083333336f * (float)(MathUtils.easeOutElastic(effectAlpha) + MathUtils.easeInCircle(effectAlpha)) / 2.0f * 6.0f, bindProgressCircleRange - 3.0f + (1.0f - effectAPC * effectAPC) * 8.0f + 10.0f * ((double)effectAlpha > 0.5 ? 1.0f - effectAlpha : effectAlpha) * 2.0f);
                }
                float bindCircleEffWidth = 1.0f + 14.0f * (float)MathUtils.easeInOutQuadWave(effectAlpha);
                float bindEffectCircleRange = bindProgressCircleBGRange * 2.0f * effectAPC;
                int effCol = ColorUtils.swapAlpha(ColorUtils.swapDark(bindCircleColor, effectAPC), (float)ColorUtils.getAlphaFromColor(bindCircleColor) * effectAPC);
                RenderUtils.drawClientCircleWithOverallToColor(bindCircleX, bindCircleY, bindEffectCircleRange, 359.0f, bindCircleEffWidth, effectAPC, effCol, 1.0f);
                if (bindAlpha >= 33.0f) {
                    String newModuleBindName = this.getKeyName(this.keyBindToSet, false).replace("DELETE", "");
                    float bindNewTextW = bindFont.getStringWidth(newModuleBindName);
                    float bindTextNewX = MathUtils.clamp(bindX + bindW / 2.0f - bindTextW / 2.0f, x, x + (moduleBindText.length() > 2 ? this.getWidth() - (bindW + 2.0f + bindNewTextW) : this.getWidth() - bindNewTextW - 1.0f));
                    GL11.glPushMatrix();
                    RenderUtils.customScaledObject2D(bindX, bindY, bindW, bindH, 1.0f + effectAPC * 4.0f);
                    RenderUtils.customRotatedObject2D(bindX, bindY, bindW, bindH, -effectAPC * 30.0f);
                    if (bindAlpha * effectAlpha >= 33.0f) {
                        bindFont.drawStringWithShadow(newModuleBindName, bindTextNewX, bindTextY, ColorUtils.swapAlpha(-1, bindAlpha * effectAlpha));
                    }
                    GL11.glPopMatrix();
                }
            }
        }
        GL11.glTranslated((double)bindCircleX, (double)bindCircleY, (double)0.0);
        this.drawAllBindNanoParticles(MathUtils.clamp(ClickGuiScreen.globalAlpha.anim / 255.0f * ClickGuiScreen.scale.anim, 0.0f, 1.0f));
        GL11.glTranslated((double)(-bindCircleX), (double)(-bindCircleY), (double)0.0);
        float bindDePC = 1.0f - bindAnim;
        boolean bl6 = hasSets = this.module.isVisible() && this.sets.stream().anyMatch(set -> set.setting.isVisible());
        if (hasSets && (float)((int)(this.alpha.anim * 0.87058824f + 36.0f)) * bindDePC > 32.0f) {
            float cur;
            GL11.glPushMatrix();
            int setsCol = ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), MathUtils.clamp((this.alpha.anim * 0.87058824f * ScaledAlphaPercent + 45.0f * ScaledAlphaPercent) * bindDePC, 2.0f, 255.0f));
            int setsColBG = ColorUtils.swapAlpha(0, MathUtils.clamp(Math.min(this.alpha.anim + textureShowAnim * 80.0f, 255.0f) * 0.87058824f * ScaledAlphaPercent * bindDePC / (5.5f - textureShowAnim * 3.0f), 0.0f, 255.0f));
            float settsX = x + this.getWidth() - 8.0f + 0.5f;
            float settsY = y + 9.0f + 0.5f;
            this.setsAnim.to = cur = (float)(this.open ? 360 : 0);
            this.setsAnim.getAnim();
            RenderUtils.drawAlphedRect(x + this.getWidth() - 16.5f, y + 1.0f, x + this.getWidth(), y + 16.5f, setsColBG);
            RenderUtils.drawSmoothCircle(x + this.getWidth() - 8.0f, y + 9.0f, 5.25f + (this.open ? 1.5f : 0.0f), setsColBG);
            GL11.glTranslated((double)-0.25, (double)-0.5, (double)0.0);
            RenderUtils.customScaledObject2D(settsX, settsY, 0.0f, 0.0f, ScaledAlphaPercent * ScaledAlphaPercent * bindDePC);
            RenderUtils.customRotatedObject2D(settsX, settsY, 0.0f, 0.0f, this.setsAnim.anim);
            if (ColorUtils.getAlphaFromColor(setsCol) >= 32) {
                Fonts.iconswex_24.drawString("h", x + this.getWidth() - 12.0f, y + 8.0f, setsCol);
            }
            GL11.glPopMatrix();
            if (this.open) {
                this.addNanoBindParticle(0.0f, 0.0f, (float)Math.random(), 1.5f);
                this.addNanoBindParticle(0.0f, 0.0f, (float)Math.random(), 3.25f);
            }
        }
        if ((demandBoolAPC = ClickGui.instance.PreviewDemandGet) * 255.0f >= 1.0f) {
            this.drawDemandRight(x + this.getWidth() - 1.0f, y + (hasSets ? 2.0f : 7.0f), ScaledAlphaPercent * ScaledAlphaPercent * ScaledAlphaPercent * Math.max(this.alpha.anim * 2.1f, 110.0f) / 255.0f * bindDePC * demandBoolAPC);
        }
        this.bindConflictAnim.to = (badBind = this.hasBadBind()) || this.hasBindConflict() ? 1.0f : 0.0f;
        float bindConflictAnim = this.bindConflictAnim.getAnim();
        if ((double)bindConflictAnim > 0.03 && 255.0f * ScaledAlphaPercent >= 20.0f && ColorUtils.getAlphaFromColor(warnColor = ColorUtils.swapAlpha(badBind ? ColorUtils.fadeColor(0, ColorUtils.getColor(255, 40, 40), 1.5f) : -1, MathUtils.clamp(Math.max(this.alpha.anim, 55.0f) * ScaledAlphaPercent * ScaledAlphaPercent, 25.0f, 255.0f))) >= 1) {
            float warnSize = 18.0f;
            float warnX = x + this.getWidth() - 0.5f - 16.0f * (hasSets ? 1.0f : bindAnim) - warnSize;
            float warnY = y - 0.5f;
            int pc90to360 = (int)((float)(System.currentTimeMillis() % 2000L) / 500.0f + 500.0f) * 90;
            this.bindConflictAngleAnim.to = pc90to360;
            float bindConflictAngleAnim = this.bindConflictAngleAnim.getAngleAnim();
            GL11.glPushMatrix();
            RenderUtils.customScaledObject2D(warnX, warnY, warnSize, warnSize, bindConflictAnim);
            RenderUtils.customRotatedObject2D(warnX, warnY, warnSize, warnSize, bindConflictAngleAnim);
            Minecraft.getMinecraft().getTextureManager().bindTexture(ClickGuiScreen.BOUND_CONFLICT);
            GL11.glTranslated((double)warnX, (double)warnY, (double)0.0);
            RenderUtils.glColor(warnColor);
            GL11.glBlendFunc((int)770, (int)32772);
            Gui.drawModalRectWithCustomSizedTexture(0.0f, 0.0f, 0.0f, 0.0f, warnSize, warnSize, warnSize, warnSize);
            GL11.glBlendFunc((int)770, (int)771);
            GlStateManager.resetColor();
            GL11.glPopMatrix();
        }
        int cr = ColorUtils.getColor((int)(85.0f + this.alpha.anim / 255.0f * 110.0f));
        int clC = ClickGuiScreen.getColor((int)(y + 20.0f), this.module.category);
        int modCol = ColorUtils.swapAlpha(ColorUtils.getOverallColorFrom(cr, clC, 0.15f), MathUtils.clamp((float)ColorUtils.getAlphaFromColor(cr) * ScaledAlphaPercent * ScaledAlphaPercent, 0.0f, 255.0f));
        CFontRenderer fontMod = Fonts.comfortaaBold_16;
        String string = bindShowText = this.bindShowAnim.anim > 0.0f ? this.getKeyName(this.module.getBind(), true) : "";
        if (ColorUtils.getAlphaFromColor(modCol) >= 33) {
            Runnable postStr0;
            float moduleNameStringX = x + (this.sdvig.anim + (Fonts.roboto_16.getStringWidth(bindShowText) + 5.0f) * this.bindShowAnim.anim) + 4.0f;
            float moduleNameStringY = y + 8.75f - fontMod.getHeight() / 2.0f;
            String modNameString = (Client.clickGuiScreen.moduleHasEqualSearch(this.module) ? "\u00a7c->\u00a7e " : "") + modulename;
            float waveModAPC = (float)MathUtils.easeOutCirc(this.toggleAnim.anim * this.toggleAnim.anim * this.toggleAnim.anim);
            Runnable preStr0 = (double)waveModAPC > 0.05 ? () -> {
                GL11.glPushMatrix();
                RenderUtils.customRotatedObject2D(moduleNameStringX, moduleNameStringY, fontMod.getStringWidth(modNameString), fontMod.getHeight() + 1.0f, (float)MathUtils.easeInOutQuadWave(1.0f - waveModAPC) / (fontMod.getStringWidth(modNameString) / 2.0f) * this.getWidth() * 0.5f);
            } : null;
            Runnable runnable = postStr0 = (double)waveModAPC > 0.05 ? () -> GL11.glPopMatrix() : null;
            if (preStr0 != null) {
                preStr0.run();
            }
            if (ScaledAlphaPercent > 0.4f) {
                fontMod.addCachedrawStringWithShadow(modNameString, moduleNameStringX, moduleNameStringY, modCol, preStr0, postStr0);
            } else {
                fontMod.addCachedrawString(modNameString, moduleNameStringX, moduleNameStringY, modCol, preStr0, postStr0);
            }
            if (waveModAPC * (float)ScaledAlpha >= 26.0f) {
                Runnable preStr1 = () -> {
                    if (preStr0 != null) {
                        preStr0.run();
                    }
                    GL11.glPushMatrix();
                    GL11.glBlendFunc((int)770, (int)1);
                    RenderUtils.customScaledObject2DPro(moduleNameStringX, moduleNameStringY, fontMod.getStringWidth(modNameString), fontMod.getHeight() + 1.0f, 1.0f + waveModAPC * 2.5f / fontMod.getStringWidth(modNameString), 1.0f + waveModAPC / 4.0f);
                };
                Runnable postStr1 = () -> {
                    GL11.glBlendFunc((int)770, (int)771);
                    GL11.glPopMatrix();
                    if (postStr0 != null) {
                        postStr0.run();
                    }
                };
                int modBloomCol = ColorUtils.swapAlpha(ColorUtils.toDark(modCol, 0.07f), (float)ColorUtils.getAlphaFromColor(modCol) * waveModAPC);
                fontMod.addCachedrawStringWithShadow(modNameString, moduleNameStringX, moduleNameStringY, modBloomCol, preStr1, postStr1);
                fontMod.addCachedrawStringWithShadow(modNameString, moduleNameStringX - 0.5f, moduleNameStringY - 0.5f, modBloomCol, preStr1, postStr1);
                fontMod.addCachedrawStringWithShadow(modNameString, moduleNameStringX - 0.5f, moduleNameStringY + 0.5f, modBloomCol, preStr1, postStr1);
                fontMod.addCachedrawStringWithShadow(modNameString, moduleNameStringX + 0.5f, moduleNameStringY - 0.5f, modBloomCol, preStr1, postStr1);
                fontMod.addCachedrawStringWithShadow(modNameString, moduleNameStringX + 0.5f, moduleNameStringY + 0.5f, modBloomCol, preStr1, postStr1);
            }
            if (this.module.isLocked()) {
                this.drawIcon(moduleNameStringX + fontMod.getStringWidth(modNameString) + 1.0f, moduleNameStringY - 2.0f, ScaledAlphaPercent, "locked" + (this.module.isBetaModule() ? "&beta" : ""));
            } else if (this.module.isBetaModule()) {
                this.drawIcon(moduleNameStringX + fontMod.getStringWidth(modNameString) + 1.0f, moduleNameStringY - 2.0f, ScaledAlphaPercent, "beta");
            }
            if (postStr0 != null) {
                postStr0.run();
            }
        }
        if (Client.clickGuiScreen.moduleHasEqualSearch(this.module)) {
            ClickGuiScreen.spawnParticleRandPos(x + this.getWidth() / 2.0f, y + 8.0f, this.getWidth() / 3.0f, 4.0f, 30L);
        }
        if (this.bindShowAnim.anim > 0.1f && 255.0f * ScaledAlphaPercent >= 33.0f) {
            float bindWidthText = Fonts.roboto_16.getStringWidth(bindShowText) + 5.0f;
            float bindExtX = -bindWidthText * (1.0f - this.bindShowAnim.anim);
            StencilUtil.initStencilToWrite();
            RenderUtils.drawRect(x + 2.0f, y, x + this.getWidth(), y + 16.0f, -1);
            StencilUtil.readStencilBuffer(1);
            float bindX1 = MathUtils.clamp(x + 2.5f + bindExtX, x + 1.0f, x + this.getWidth());
            float bindX2S = MathUtils.clamp(x + 3.0f + bindExtX + Fonts.roboto_16.getStringWidth(bindShowText) + 2.5f, x + 1.0f, x + this.getWidth());
            int bindColBase = ColorUtils.reverseColor(clC, false);
            GL11.glBlendFunc((int)770, (int)771);
            RenderUtils.drawAlphedRect(bindX1, y + 2.0f, bindX2S, y + 15.5f, ColorUtils.swapAlpha(ColorUtils.toDark(bindColBase, 0.1f), (int)(ScaledAlphaPercent * 180.0f)));
            Fonts.roboto_16.drawStringWithShadow(bindShowText, x + 4.0f + bindExtX, y + 9.5f - Fonts.roboto_16.getHeight() / 2.0f, ColorUtils.swapAlpha(bindColBase, (int)(165.0f * ScaledAlphaPercent)));
            StencilUtil.uninitStencilBuffer();
        }
        this.setupDescriptions(x, y, mouseX, mouseY);
    }

    void setupDescriptions(float x, float y, int mouseX, int mouseY) {
        if (ClickGui.instance.Descriptions.getBool()) {
            xn = mouseX + 15;
            yn = mouseY + 15;
            boolean render = false;
            for (Panel panel : Client.clickGuiScreen.panels) {
                if (panel.dragging) {
                    return;
                }
                if (!panel.open || !(xn - 15.0f > panel.X) || !(xn - 15.0f < panel.posX.anim + panel.getWidth()) || !(yn - 15.0f > panel.Y + 24.0f) || !(yn - 15.0f < panel.posY.anim + panel.animOpen.anim)) continue;
                render = true;
            }
            if (this.ishover(x, y, x + this.getWidth(), y + 16.0f, mouseX, mouseY) && this.module != null && this.module.name != null && render) {
                ClickGuiScreen.descriptionName = this.module.name;
                ClickGuiScreen.colorCategory = this.module.category;
                String description = "";
                for (String descriptions : Client.moduleManager.getModuleDescriptionList()) {
                    if (ClickGuiScreen.descriptionName == null || !descriptions.startsWith(ClickGuiScreen.descriptionName)) continue;
                    description = descriptions.replace(ClickGuiScreen.descriptionName, "");
                }
                descript = description;
                if (alphaD < 255.0f) {
                    alphaD = 255.0f;
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        this.wantToClick = true;
        this.wantToClick2 = true;
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (this.open) {
            for (Set set : this.sets) {
                set.mouseReleased(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void mouseClicked(int x, int y, int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(x, y, mouseX, mouseY, mouseButton);
        boolean lock = this.module.isLocked();
        if (this.wantToClick || this.wantToClick2) {
            if (this.ishover(x + 1, y - 2, (float)x + this.getWidth(), y + 14, mouseX, mouseY)) {
                if (mouseButton == 2 && !lock) {
                    boolean bl = this.binding = !this.binding;
                    if (this.binding && this.module.getBind() == 0) {
                        for (int s = 0; s < 40; ++s) {
                            float r = (float)Math.random();
                            this.addNanoBindParticle(0.0f, 0.0f, r, 2.0f);
                            this.addNanoBindParticle(0.0f, 0.0f, r, -3.0f);
                        }
                    }
                    ClientTune.get.playGuiModuleBindingToggleSong(this.binding);
                }
                if (this.ishover((float)x + this.getWidth() - 16.0f, y - 2, (float)x + this.getWidth(), y + 14, mouseX, mouseY) && (mouseButton == 1 || mouseButton == 0) && this.module.settings.stream().filter(Settings::isVisible).collect(Collectors.toList()).size() > 0) {
                    boolean bl = this.open = !this.open;
                    if (this.open) {
                        Client.clickGuiScreen.panels.stream().filter(panel -> panel.open).forEach(panel -> panel.mods.stream().filter(mod -> mod != this).filter(mod -> mod.open).forEach(mod -> {
                            mod.open = false;
                            ClientTune.get.playGuiModuleOpenOrCloseSong(false);
                        }));
                    } else {
                        this.sets.stream().map(Set::getHasModes).filter(Objects::nonNull).filter(mode -> mode.open).forEach(mode -> {
                            mode.open = false;
                            mode.playClose = true;
                        });
                        this.sets.stream().map(Set::getHasColors).filter(Objects::nonNull).filter(color -> color.open).forEach(color -> {
                            color.open = false;
                            color.playClose = true;
                        });
                    }
                    ClientTune.get.playGuiModuleOpenOrCloseSong(this.open);
                    if (this.binding) {
                        this.binding = false;
                        ClientTune.get.playGuiModuleBindingToggleSong(this.binding);
                    }
                }
                if (!(this.ishover((float)x + this.getWidth() - 16.0f, y - 2, (float)x + this.getWidth(), y + 14, mouseX, mouseY) && this.module.settings.stream().filter(Settings::isVisible).collect(Collectors.toList()).size() != 0 || mouseButton != 0)) {
                    this.module.toggle(!this.module.actived);
                    if (this.module.isActived() && this.open) {
                        this.open = false;
                        ClientTune.get.playGuiModuleOpenOrCloseSong(false);
                        this.height = this.getHeight();
                    }
                    this.toggleAnim.setAnim(0.0f);
                    this.toggleAnim.to = 1.0f;
                    if (this.binding) {
                        this.binding = false;
                    }
                    float deOff = 1.0f;
                    float deX = this.getWidth() - 7.75f;
                    float deY = 8.25f;
                    float detW = this.getWidth() + deOff * 2.0f;
                    float detH = this.height + deOff * 2.0f;
                    int s = 0;
                    while ((float)s < detW / 1.5f) {
                        this.addNanoBindParticle(detW * (float)Math.random() - deX - deOff, -deY - deOff, 0.0f, 1.0f);
                        this.addNanoBindParticle(detW * (float)Math.random() - deX - deOff, detH - deY - deOff, 0.0f, 1.0f);
                        ++s;
                    }
                    s = 0;
                    while ((float)s < detH / 1.5f) {
                        this.addNanoBindParticle(-deX - deOff, detH * (float)Math.random() - deY - deOff, 0.0f, 1.0f);
                        this.addNanoBindParticle(detW - deX - deOff, detH * (float)Math.random() - deY - deOff, 0.0f, 1.0f);
                        ++s;
                    }
                }
            }
            if (this.open) {
                int i = 16;
                for (Set set : this.sets) {
                    if (!set.setting.isVisible()) continue;
                    set.mouseClicked(x + 1, y + i, mouseX, mouseY, mouseButton);
                    i = (int)((float)i + (set.getHeight() + 1.0f));
                }
            }
            if (this.ishover(x + 1, y - 2, (float)x + this.getWidth(), (float)y + this.getHeight() - 2.0f, mouseX, mouseY)) {
                this.wantToClick2 = false;
                this.wantToClick = false;
            }
        }
    }

    @Override
    public float getHeight() {
        int i = 16 + (this.open ? 6 : 0);
        if (this.open) {
            if (this.module.isLocked()) {
                i = (int)((float)i + 16.0f);
            } else {
                for (Set set : this.sets) {
                    i = (int)((float)i + (set != null && set.setting.isVisible() ? set.getHeight() + 1.0f : 0.0f));
                }
            }
        }
        return i;
    }

    private void drawModuleTexture(float x1, float y1, float x2, float y2, float aPC, float animPC, float baseAnimPC, Runnable fixStencil) {
        float preX = x1;
        float scaleOff = 0.7f;
        float scaled = 1.0f + scaleOff - scaleOff * animPC;
        if (MathUtils.getDifferenceOf(scaled, 1.0f) < 0.003f) {
            scaled = 1.0f;
        }
        StencilUtil.initStencilToWrite();
        RenderUtils.drawRect(x1, y1, x2, y2, -1);
        StencilUtil.readStencilBuffer(1);
        RenderUtils.glRenderStart();
        GL11.glPushMatrix();
        RenderUtils.customScaledObject2D(x1, y1, x2 - x1, y2 - y1, scaled);
        float xPC = Math.min(baseAnimPC * 2.0f, 1.0f);
        x1 -= (x2 - x1) * (1.0f - MathUtils.lerp(xPC, 1.0f, xPC));
        x2 = MathUtils.lerp(x1, x2, xPC);
        int color0 = ColorUtils.swapAlpha(-1, 255.0f * aPC);
        int color1 = ColorUtils.swapAlpha(-1, 255.0f * aPC * aPC * Math.max((xPC - 0.5f) * 2.0f, 0.0f));
        Minecraft.getMinecraft().getTextureManager().bindTexture(this.moduleTexture);
        GL11.glEnable((int)3553);
        GL11.glDisable((int)3008);
        GL11.glDisable((int)2929);
        GL11.glShadeModel((int)7425);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(x1, y2).tex(0.0, 1.0).color(color0).endVertex();
        bufferbuilder.pos(x2, y2).tex(xPC, 1.0).color(color1).endVertex();
        bufferbuilder.pos(x2, y1).tex(xPC, 0.0).color(color1).endVertex();
        bufferbuilder.pos(x1, y1).tex(0.0, 0.0).color(color0).endVertex();
        tessellator.draw();
        GL11.glPopMatrix();
        bufferbuilder.begin(2, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferbuilder.pos(x1, y2).tex(0.03125, 1.0).color(0).endVertex();
        bufferbuilder.pos(x2, y2).tex(xPC, 1.0).color(color1).endVertex();
        bufferbuilder.pos(x2, y1).tex(xPC, 0.03125).color(color1).endVertex();
        bufferbuilder.pos(x1, y1).tex(0.03125, 0.03125).color(0).endVertex();
        GL11.glBlendFunc((int)770, (int)1);
        float lpSCFactor = ScaledResolution.lpSCFactor();
        GL11.glLineWidth((float)(0.5f * lpSCFactor));
        tessellator.vboUploader.draw(tessellator.worldRenderer, false);
        GL11.glLineWidth((float)(2.25f * lpSCFactor));
        tessellator.vboUploader.draw(tessellator.worldRenderer, false);
        GL11.glLineWidth((float)(4.25f * lpSCFactor));
        tessellator.vboUploader.draw(tessellator.worldRenderer, true);
        tessellator.worldRenderer.finishDrawing();
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glLineWidth((float)1.0f);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        GL11.glShadeModel((int)7424);
        GL11.glEnable((int)3008);
        GL11.glEnable((int)2929);
        RenderUtils.glRenderStop();
        StencilUtil.uninitStencilBuffer();
        fixStencil.run();
    }

    private void drawIcon(float x, float y, float alphaPC, String str) {
        if ((double)alphaPC < 0.1) {
            return;
        }
        int bgColor = ColorUtils.getColor(9, 9, 17, 100.0f * alphaPC);
        int outBgColL = ColorUtils.getColor(180, 180, 130, 30.0f * alphaPC);
        int outBgColR = ColorUtils.getColor(180, 180, 130, 80.0f * alphaPC);
        CFontRenderer font = Fonts.mntsb_10;
        float xOff = 0.5f;
        float yOff = 1.0f;
        float strH = font.getHeight() + 1.5f;
        float y2 = y + yOff * 2.0f + strH;
        float delayOffset = 7.0f;
        float timeDelay = 700.0f * delayOffset;
        float textX = x + xOff + 0.5f;
        float textY = y + yOff + 1.5f;
        textX = (float)((int)(textX * 2.0f)) / 2.0f;
        int charIndex = 0;
        int fColor = ColorUtils.getColor(255, 255, 255, 60.0f * alphaPC);
        int sColor = ColorUtils.getColor(255, this.module.isLocked() ? 80 : 190, 0, Math.min(255.0f * alphaPC, 255.0f));
        for (char theChar : str.toCharArray()) {
            String charStr = String.valueOf(theChar);
            float charW = font.getStringWidth(charStr);
            float timePC = (float)((System.currentTimeMillis() - (long)((int)(timeDelay * ((float)charIndex / (float)str.length()) / delayOffset))) % (long)((int)timeDelay)) / timeDelay * delayOffset;
            int textColor = ColorUtils.getOverallColorFrom(fColor, sColor, timePC = (float)MathUtils.easeInOutQuadWave(MathUtils.clamp(timePC, 0.0f, 1.0f)));
            if (ColorUtils.getAlphaFromColor(textColor) >= 33) {
                font.drawStringWithShadow(charStr, textX, textY, textColor);
            }
            ++charIndex;
            textX += charW * 1.1f;
        }
        float x2 = textX + xOff;
        if (this.module.isLocked() && ColorUtils.getAlphaFromColor(bgColor) >= 20) {
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(x, y, x2, y2, (y2 - y) / 2.0f, (y2 - y) / 2.0f, bgColor, bgColor, bgColor, bgColor, false, true, true);
        }
        if (ColorUtils.getAlphaFromColor(outBgColL) >= 5) {
            RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBool(x, y, x2, y2, (y2 - y) / 4.0f, outBgColL, outBgColR, outBgColR, outBgColL, true);
        }
    }

    private void drawDemandRight(float x, float y, float alphaPC) {
        DemandPC demand;
        block11: {
            block10: {
                float f = 0;
                demand = this.module.getDemand();
                if (demand == null) break block10;
                alphaPC = Math.min(alphaPC, 1.0f);
                if (!(f * 255.0f < 1.0f)) break block11;
            }
            return;
        }
        int colBG = ColorUtils.getColor(this.module.isLocked() ? 45 : 9, 9, 9, 180.0f * alphaPC);
        int colBgOut = ColorUtils.getColor(255, this.module.isLocked() ? 40 : 255, this.module.isLocked() ? 40 : 255, (this.module.isLocked() ? 255.0f : 130.0f) * alphaPC);
        int colOff = ColorUtils.getColor(115, 115, 115, 160.0f * alphaPC);
        float offsets = 0.5f;
        float quadScale = 1.0f;
        float w = quadScale * (float)DemandPC.maxDemandValue + (float)(DemandPC.maxDemandValue + 1) * offsets;
        float h = offsets * (float)(DemandPC.valuesCount + 1) + quadScale * (float)DemandPC.valuesCount;
        RenderUtils.drawRect(x -= w, y, x + w, y + h, colBG);
        if (demand.getLevel03FPS() == 0 && demand.getLevel03CPU() == 0 || this.module.isLocked()) {
            RenderUtils.drawRect(x + offsets, y + h / 2.0f - offsets / 2.0f, x + w - offsets, y + h / 2.0f + offsets / 2.0f, colBgOut);
            return;
        }
        float yStartQuadsCicles = y + offsets;
        for (int yTor = 0; yTor < DemandPC.valuesCount; ++yTor) {
            int demandCurrentValue = 0;
            int colOn = -1;
            switch (yTor) {
                case 0: {
                    demandCurrentValue = demand.getLevel03FPS();
                    colOn = ColorUtils.getColor(0, 255, 255, 255.0f * alphaPC);
                    break;
                }
                case 1: {
                    demandCurrentValue = demand.getLevel03CPU();
                    colOn = ColorUtils.getColor(255, 80, 80, 255.0f * alphaPC);
                }
            }
            float xStartQuadsCicles = x + offsets;
            for (int xTor = 0; xTor < DemandPC.maxDemandValue; ++xTor) {
                boolean on = xTor < demandCurrentValue;
                int colorQuad = on ? colOn : colOff;
                RenderUtils.drawRect(xStartQuadsCicles, yStartQuadsCicles, xStartQuadsCicles + quadScale, yStartQuadsCicles + quadScale, colorQuad);
                xStartQuadsCicles += offsets + quadScale;
            }
            yStartQuadsCicles += offsets + quadScale;
        }
    }

    @Override
    public float getWidth() {
        return 118.5f;
    }

    class NanoBindParticle {
        float x;
        float y;
        float speed = (float)Math.random() / 22.25f * ScaledResolution.lpSCFactor();
        float radian = (float)Math.random() * 360.0f;
        float maxTime = 450.0f + (float)((int)(150.0 * Math.random()));
        long startTime = System.currentTimeMillis();

        NanoBindParticle(float circleX, float circleY, float holdProgress, float ofRange) {
            float radian = MathHelper.toRadians(holdProgress * 360.0f + 180.0f);
            if (holdProgress == 0.0f) {
                if (ofRange != 0.0f) {
                    this.speed /= 1.3f;
                }
                if (ofRange == 1.0f) {
                    this.speed /= 3.0f;
                }
                ofRange = 0.0f;
                this.maxTime /= 2.0f;
            } else {
                this.radian = radian / (float)Math.PI * 180.0f - 60.0f - 10.0f + 20.0f * (float)Math.random();
            }
            this.x = circleX + MathHelper.sin(radian) * ofRange;
            this.y = circleY + MathHelper.cos(radian) * ofRange;
            this.speed *= ofRange / 4.0f + 6.0f;
        }

        float timePC() {
            return MathUtils.clamp((float)(System.currentTimeMillis() - this.startTime) / this.maxTime, 0.0f, 1.0f);
        }

        float alphaPC() {
            return 1.0f - this.timePC();
        }

        boolean toRemove() {
            return this.timePC() == 1.0f;
        }

        void drawAndMovement(float alphaPC) {
            this.x += MathHelper.sin(MathHelper.toRadians(this.radian)) * this.speed;
            this.y += MathHelper.cos(MathHelper.toRadians(this.radian)) * this.speed;
            if ((alphaPC *= this.alphaPC()) == 0.0f) {
                return;
            }
            int color = ColorUtils.swapAlpha(ColorUtils.getOverallColorFrom(ClickGuiScreen.getColor((int)((this.x + this.y) * 3.0f), Mod.this.module.category), -1, 0.5f - alphaPC * alphaPC * 0.5f), 255.0f * (float)MathUtils.easeInOutQuad(alphaPC));
            GL11.glEnable((int)3042);
            GL11.glBlendFunc((int)770, (int)32772);
            GL11.glDisable((int)3553);
            GL11.glDisable((int)3008);
            GL11.glEnable((int)2832);
            GL11.glPointSize((float)(alphaPC * alphaPC * (this.maxTime / 1000.0f) * 11.0f * ScaledResolution.lpSCFactor()));
            RenderUtils.glColor(color);
            GL11.glBegin((int)0);
            GL11.glVertex2d((double)this.x, (double)this.y);
            GL11.glEnd();
            GL11.glPointSize((float)1.0f);
            GL11.glEnable((int)3008);
            GL11.glEnable((int)3553);
            GL11.glBlendFunc((int)770, (int)771);
            GlStateManager.resetColor();
        }
    }
}
