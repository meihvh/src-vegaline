/*
 * Decompiled with CFR 0.152.
 */
package ru.govno.client.newfont;

import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import ru.govno.client.module.modules.UiScaleControl;
import ru.govno.client.newfont.CFontRenderer;

public class Fonts {
    private static float lastLpSCFactor = 1.0f;
    public static CFontRenderer time_30 = new CFontRenderer(Fonts.getFontTTF("tech", 30.0f, lastLpSCFactor), 30, "tech");
    public static CFontRenderer time_17 = new CFontRenderer(Fonts.getFontTTF("tech", 17.0f, lastLpSCFactor), 17, "tech");
    public static CFontRenderer time_14 = new CFontRenderer(Fonts.getFontTTF("tech", 14.0f, lastLpSCFactor), 14, "tech");
    public static CFontRenderer mntsb_7 = new CFontRenderer(Fonts.getFontTTF("mntsb", 7.0f, lastLpSCFactor), 7, "mntsb");
    public static CFontRenderer mntsb_10 = new CFontRenderer(Fonts.getFontTTF("mntsb", 10.0f, lastLpSCFactor), 10, "mntsb");
    public static CFontRenderer mntsb_12 = new CFontRenderer(Fonts.getFontTTF("mntsb", 12.0f, lastLpSCFactor), 12, "mntsb");
    public static CFontRenderer mntsb_13 = new CFontRenderer(Fonts.getFontTTF("mntsb", 13.0f, lastLpSCFactor), 13, "mntsb");
    public static CFontRenderer mntsb_14 = new CFontRenderer(Fonts.getFontTTF("mntsb", 14.0f, lastLpSCFactor), 14, "mntsb");
    public static CFontRenderer mntsb_15 = new CFontRenderer(Fonts.getFontTTF("mntsb", 15.0f, lastLpSCFactor), 15, "mntsb");
    public static CFontRenderer mntsb_16 = new CFontRenderer(Fonts.getFontTTF("mntsb", 16.0f, lastLpSCFactor), 16, "mntsb");
    public static CFontRenderer mntsb_18 = new CFontRenderer(Fonts.getFontTTF("mntsb", 18.0f, lastLpSCFactor), 18, "mntsb");
    public static CFontRenderer mntsb_20 = new CFontRenderer(Fonts.getFontTTF("mntsb", 20.0f, lastLpSCFactor), 20, "mntsb");
    public static CFontRenderer mntsb_36 = new CFontRenderer(Fonts.getFontTTF("mntsb", 36.0f, lastLpSCFactor), 36, "mntsb");
    public static CFontRenderer iconswex_24 = new CFontRenderer(Fonts.getFontTTF("iconswex", 24.0f, lastLpSCFactor), 24, "iconswex");
    public static CFontRenderer iconswex_36 = new CFontRenderer(Fonts.getFontTTF("iconswex", 36.0f, lastLpSCFactor), 36, "iconswex");
    public static CFontRenderer roadrage_36 = new CFontRenderer(Fonts.getFontTTF("roadrage", 36.0f, lastLpSCFactor), 36, "roadrage");
    public static CFontRenderer comfortaaRegular_12 = new CFontRenderer(Fonts.getFontTTF("comfortaa-regular", 12.0f, lastLpSCFactor), 12, "comfortaa-regular");
    public static CFontRenderer comfortaaRegular_13 = new CFontRenderer(Fonts.getFontTTF("comfortaa-regular", 13.0f, lastLpSCFactor), 13, "comfortaa-regular");
    public static CFontRenderer comfortaaRegular_14 = new CFontRenderer(Fonts.getFontTTF("comfortaa-regular", 14.0f, lastLpSCFactor), 14, "comfortaa-regular");
    public static CFontRenderer comfortaaRegular_15 = new CFontRenderer(Fonts.getFontTTF("comfortaa-regular", 15.0f, lastLpSCFactor), 15, "comfortaa-regular");
    public static CFontRenderer comfortaaRegular_16 = new CFontRenderer(Fonts.getFontTTF("comfortaa-regular", 16.0f, lastLpSCFactor), 16, "comfortaa-regular");
    public static CFontRenderer comfortaaRegular_17 = new CFontRenderer(Fonts.getFontTTF("comfortaa-regular", 17.0f, lastLpSCFactor), 17, "comfortaa-regular");
    public static CFontRenderer comfortaaRegular_18 = new CFontRenderer(Fonts.getFontTTF("comfortaa-regular", 18.0f, lastLpSCFactor), 18, "comfortaa-regular");
    public static CFontRenderer comfortaaRegular_22 = new CFontRenderer(Fonts.getFontTTF("comfortaa-regular", 22.0f, lastLpSCFactor), 22, "comfortaa-regular");
    public static CFontRenderer comfortaaBold_12 = new CFontRenderer(Fonts.getFontTTF("comfortaa-bold", 12.0f, lastLpSCFactor), 12, "comfortaa-bold");
    public static CFontRenderer comfortaaBold_13 = new CFontRenderer(Fonts.getFontTTF("comfortaa-bold", 13.0f, lastLpSCFactor), 13, "comfortaa-bold");
    public static CFontRenderer comfortaaBold_14 = new CFontRenderer(Fonts.getFontTTF("comfortaa-bold", 14.0f, lastLpSCFactor), 14, "comfortaa-bold");
    public static CFontRenderer comfortaaBold_15 = new CFontRenderer(Fonts.getFontTTF("comfortaa-bold", 15.0f, lastLpSCFactor), 15, "comfortaa-bold");
    public static CFontRenderer comfortaaBold_16 = new CFontRenderer(Fonts.getFontTTF("comfortaa-bold", 16.0f, lastLpSCFactor), 16, "comfortaa-bold");
    public static CFontRenderer comfortaaBold_17 = new CFontRenderer(Fonts.getFontTTF("comfortaa-bold", 17.0f, lastLpSCFactor), 17, "comfortaa-bold");
    public static CFontRenderer comfortaaBold_18 = new CFontRenderer(Fonts.getFontTTF("comfortaa-bold", 18.0f, lastLpSCFactor), 18, "comfortaa-bold");
    public static CFontRenderer comfortaa_12 = new CFontRenderer(Fonts.getFontTTF("comfortaa-light", 12.0f, lastLpSCFactor), 12, "comfortaa-light");
    public static CFontRenderer comfortaa_18 = new CFontRenderer(Fonts.getFontTTF("comfortaa-light", 18.0f, lastLpSCFactor), 18, "comfortaa-light");
    public static CFontRenderer roboto_16 = new CFontRenderer(Fonts.getFontTTF("roboto", 16.0f, lastLpSCFactor), 16, "roboto");
    public static CFontRenderer roboto_13 = new CFontRenderer(Fonts.getFontTTF("roboto", 13.0f, lastLpSCFactor), 13, "roboto");
    public static CFontRenderer neverlose500_13 = new CFontRenderer(Fonts.getFontTTF("neverlose500", 13.0f, lastLpSCFactor), 13, "neverlose500");
    public static CFontRenderer neverlose500_15 = new CFontRenderer(Fonts.getFontTTF("neverlose500", 15.0f, lastLpSCFactor), 15, "neverlose500");
    public static CFontRenderer neverlose500_16 = new CFontRenderer(Fonts.getFontTTF("neverlose500", 16.0f, lastLpSCFactor), 16, "neverlose500");
    public static CFontRenderer neverlose500_17 = new CFontRenderer(Fonts.getFontTTF("neverlose500", 17.0f, lastLpSCFactor), 17, "neverlose500");
    public static CFontRenderer neverlose500_18 = new CFontRenderer(Fonts.getFontTTF("neverlose500", 18.0f, lastLpSCFactor), 18, "neverlose500");
    public static CFontRenderer smallestpixel_16 = new CFontRenderer(Fonts.getFontTTF("smallpixel", 16.0f, lastLpSCFactor), 16, "smallpixel");
    public static CFontRenderer smallestpixel_20 = new CFontRenderer(Fonts.getFontTTF("smallpixel", 20.0f, lastLpSCFactor), 20, "smallpixel");
    public static CFontRenderer smallestpixel_24 = new CFontRenderer(Fonts.getFontTTF("smallpixel", 24.0f, lastLpSCFactor), 24, "smallpixel");
    public static CFontRenderer stylesicons_18 = new CFontRenderer(Fonts.getFontTTF("stylesicons", 18.0f, lastLpSCFactor), 18, "stylesicons");
    public static CFontRenderer stylesicons_20 = new CFontRenderer(Fonts.getFontTTF("stylesicons", 20.0f, lastLpSCFactor), 20, "stylesicons");
    public static CFontRenderer stylesicons_24 = new CFontRenderer(Fonts.getFontTTF("stylesicons", 24.0f, lastLpSCFactor), 24, "stylesicons");
    public static CFontRenderer noise_14 = new CFontRenderer(Fonts.getFontTTF("noise", 14.0f, lastLpSCFactor), 14, "noise");
    public static CFontRenderer noise_15 = new CFontRenderer(Fonts.getFontTTF("noise", 15.0f, lastLpSCFactor), 15, "noise");
    public static CFontRenderer noise_16 = new CFontRenderer(Fonts.getFontTTF("noise", 16.0f, lastLpSCFactor), 16, "noise");
    public static CFontRenderer noise_17 = new CFontRenderer(Fonts.getFontTTF("noise", 17.0f, lastLpSCFactor), 17, "noise");
    public static CFontRenderer noise_18 = new CFontRenderer(Fonts.getFontTTF("noise", 18.0f, lastLpSCFactor), 18, "noise");
    public static CFontRenderer noise_20 = new CFontRenderer(Fonts.getFontTTF("noise", 20.0f, lastLpSCFactor), 20, "noise");
    public static CFontRenderer noise_24 = new CFontRenderer(Fonts.getFontTTF("noise", 24.0f, lastLpSCFactor), 24, "noise");
    public static CFontRenderer minecraftia_14 = new CFontRenderer(Fonts.getFontTTF("minecraftia", 14.0f, lastLpSCFactor), 14, "minecraftia");
    public static CFontRenderer minecraftia_16 = new CFontRenderer(Fonts.getFontTTF("minecraftia", 16.0f, lastLpSCFactor), 16, "minecraftia");
    public static CFontRenderer minecraftia_18 = new CFontRenderer(Fonts.getFontTTF("minecraftia", 18.0f, lastLpSCFactor), 18, "minecraftia");
    public static CFontRenderer minecraftia_20 = new CFontRenderer(Fonts.getFontTTF("minecraftia", 20.0f, lastLpSCFactor), 20, "minecraftia");
    public static boolean rescaleInProcess;
    public static int rescalePrepareCurrent;
    public static int rescalePreparesCount;
    public static int rescaleProcessCurrent;
    public static int rescaleProcessCount;
    private static final Map<String, CFontUpdateCallSingle> namedMapCallsUpdatesFonts;
    private static boolean updateScaleForce;
    public static boolean wasCatched;

    public static CFontRenderer[] allToInit() {
        return new CFontRenderer[]{time_30, time_17, time_14, mntsb_7, mntsb_10, mntsb_12, mntsb_13, mntsb_14, mntsb_15, mntsb_16, mntsb_18, mntsb_20, mntsb_36, iconswex_24, iconswex_36, roadrage_36, comfortaaRegular_12, comfortaaRegular_13, comfortaaRegular_14, comfortaaRegular_15, comfortaaRegular_16, comfortaaRegular_17, comfortaaRegular_18, comfortaaRegular_22, comfortaaBold_12, comfortaaBold_13, comfortaaBold_14, comfortaaBold_15, comfortaaBold_16, comfortaaBold_17, comfortaaBold_18, comfortaa_12, comfortaa_18, roboto_16, roboto_13, neverlose500_13, neverlose500_15, neverlose500_16, neverlose500_17, neverlose500_18, smallestpixel_16, smallestpixel_20, smallestpixel_24, stylesicons_18, stylesicons_20, stylesicons_24, noise_14, noise_15, noise_16, noise_17, noise_18, noise_20, noise_24, minecraftia_14, minecraftia_16, minecraftia_18, minecraftia_20};
    }

    private static void addFontRescaleTask(CFontRenderer font, float mulScaleSet, int maxAttemptsLoadNewScale, int allToInitCurrentObjIndex) {
        if (!namedMapCallsUpdatesFonts.containsKey(font.displayNameData)) {
            namedMapCallsUpdatesFonts.put(font.displayNameData, new CFontUpdateCallSingle(font, mulScaleSet, maxAttemptsLoadNewScale, allToInitCurrentObjIndex));
        } else {
            namedMapCallsUpdatesFonts.replace(font.displayNameData, new CFontUpdateCallSingle(font, mulScaleSet, maxAttemptsLoadNewScale, allToInitCurrentObjIndex));
        }
        ++rescalePrepareCurrent;
        rescaleInProcess = true;
    }

    private static void executeAllCallsRescalesFonts() {
        if (namedMapCallsUpdatesFonts.isEmpty()) {
            rescaleInProcess = false;
            rescalePrepareCurrent = 0;
            rescaleProcessCurrent = 0;
            return;
        }
        rescaleInProcess = true;
        CFontRenderer[] allToInit = Fonts.allToInit();
        for (String nameTask : namedMapCallsUpdatesFonts.keySet()) {
            CFontUpdateCallSingle call = namedMapCallsUpdatesFonts.get(nameTask);
            if (call == null) {
                ++rescaleProcessCurrent;
                continue;
            }
            if (!call.executeTryHasEnd(allToInit)) break;
            namedMapCallsUpdatesFonts.remove(nameTask);
            ++rescaleProcessCurrent;
            break;
        }
    }

    public static void triggerUpdateScale() {
        updateScaleForce = true;
    }

    public static void updateScale() {
        float lpSCFactor = (UiScaleControl.doFontScaleAdaptation ? ScaledResolution.lpSCFactor() : 1.0f) * 1.25f;
        if (lastLpSCFactor == -1.0f) {
            lastLpSCFactor = lpSCFactor;
        }
        boolean callingChangesDataFonts = false;
        if (lastLpSCFactor != lpSCFactor || updateScaleForce) {
            try {
                int indexFontInArray = 0;
                for (CFontRenderer loadedFont : Fonts.allToInit()) {
                    if (loadedFont != null) {
                        Fonts.addFontRescaleTask(loadedFont, lpSCFactor, 20, indexFontInArray);
                        callingChangesDataFonts = true;
                    }
                    ++indexFontInArray;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            lastLpSCFactor = lpSCFactor;
            updateScaleForce = false;
        }
        if (!callingChangesDataFonts) {
            Fonts.executeAllCallsRescalesFonts();
        }
    }

    public static Font getFontTTF(String name, int size) {
        Font font;
        try {
            InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("font/" + name + ".ttf")).getInputStream();
            font = Font.createFont(0, is);
            font = font.deriveFont(0, size);
            wasCatched = false;
        }
        catch (Exception var4) {
            System.out.println("Error loading font");
            font = new Font("name", 0, size);
            wasCatched = true;
        }
        return font;
    }

    public static Font getFontTTF(String name, float size, float mulScale) {
        Font font;
        if ((size *= mulScale) > 72.0f) {
            size = 72.0f;
        }
        try {
            InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("font/" + name + ".ttf")).getInputStream();
            font = Font.createFont(0, is);
            font = font.deriveFont(0, size);
            wasCatched = false;
        }
        catch (Exception var4) {
            System.out.println("Error loading font - " + name);
            font = new Font("null", 0, (int)size);
            wasCatched = true;
        }
        return font;
    }

    static {
        rescaleProcessCount = rescalePreparesCount = Fonts.allToInit().length;
        namedMapCallsUpdatesFonts = new HashMap<String, CFontUpdateCallSingle>();
        wasCatched = false;
    }

    private static class CFontUpdateCallSingle {
        private final float mulScaleTo;
        private Font newFontAny;
        private int loadFontAttempts;
        private final int loadFontAttemptsMax;
        private boolean processEnded;
        private final String oldFontName;
        private final int oldFontBaseScaleNotMultiplied;
        private final int allToInitCurrentObjIndex;

        public CFontUpdateCallSingle(CFontRenderer buildFrom, float mulScaleTo, int loadFontAttemptsMax, int allToInitCurrentObjIndex) {
            this.mulScaleTo = mulScaleTo;
            this.loadFontAttemptsMax = loadFontAttemptsMax;
            this.oldFontName = buildFrom.generalFontName;
            this.oldFontBaseScaleNotMultiplied = buildFrom.fixedFontScaleInt;
            this.allToInitCurrentObjIndex = allToInitCurrentObjIndex;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public boolean executeTryHasEnd(CFontRenderer[] allToInit) {
            if (this.processEnded) {
                return true;
            }
            if (this.newFontAny == null) {
                CompletableFuture.runAsync(() -> {
                    this.newFontAny = Fonts.getFontTTF(this.oldFontName, this.oldFontBaseScaleNotMultiplied, this.mulScaleTo);
                    if (this.newFontAny.getFontName().equalsIgnoreCase("null")) {
                        this.newFontAny = null;
                    }
                });
                return false;
            }
            if (this.newFontAny != null) {
                try {
                    allToInit[this.allToInitCurrentObjIndex % allToInit.length].setFont(this.newFontAny);
                    ++this.loadFontAttempts;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    boolean bl = false;
                    return bl;
                }
                finally {
                    this.processEnded = true;
                }
            }
            return this.loadFontAttempts >= this.loadFontAttemptsMax;
        }
    }
}
