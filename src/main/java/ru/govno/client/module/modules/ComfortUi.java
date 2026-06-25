package ru.govno.client.module.modules;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import optifine.Config;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.DisplayCheck;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.Vec2fColored;

public class ComfortUi extends Module {
   public static ComfortUi get;
   public BoolSettings BetterTabOverlay;
   public BoolSettings ScreensDarking;
   public BoolSettings ChatAnimations;
   public BoolSettings ContainerAnim;
   public BoolSettings InvParticles;
   public BoolSettings AnimPauseScreen;
   public BoolSettings AddClientButtons;
   public BoolSettings BetterButtons;
   public BoolSettings BetterChatline;
   public BoolSettings BetterDebugF3;
   public BoolSettings ClipHelperInChat;
   public BoolSettings PaintInChat;
   public BoolSettings CutChatMsgBg;
   public BoolSettings FastChatMsgQuit;
   public BoolSettings NoStartInvHint;
   public BoolSettings GuiTransitions;
   public BoolSettings ScreenRounding;
   public BoolSettings GuiMouseTrails;
   public BoolSettings HDGuiItems;
   public BoolSettings SmoothMcScroll;
   private final ColorSettings PickTrailColor;
   private final ModeSettings TrailColorMode;
   public static int alphaTransition = 0;
   private int mouseSpeed;
   private final CopyOnWriteArrayList<ComfortUi.MouseTrailPoint> mouseTrailPoints = new CopyOnWriteArrayList<>();
   private int lastMouseX;
   private int lastMouseY;
   public boolean cancelTooltip;

   public ComfortUi() {
      super("ComfortUi", 0, Module.Category.RENDER);
      get = this;
      this.settings.add(this.BetterTabOverlay = new BoolSettings("BetterTabOverlay", true, this));
      this.settings.add(this.ScreensDarking = new BoolSettings("ScreensDarking", true, this));
      this.settings.add(this.ChatAnimations = new BoolSettings("ChatAnimations", true, this));
      this.settings.add(this.ContainerAnim = new BoolSettings("ContainerAnim", true, this));
      this.settings.add(this.InvParticles = new BoolSettings("InvParticles", true, this));
      this.settings.add(this.AnimPauseScreen = new BoolSettings("AnimPauseScreen", true, this));
      this.settings.add(this.AddClientButtons = new BoolSettings("AddClientButtons", true, this));
      this.settings.add(this.BetterButtons = new BoolSettings("BetterButtons", true, this));
      this.settings.add(this.BetterChatline = new BoolSettings("BetterChatline", true, this));
      this.settings.add(this.BetterDebugF3 = new BoolSettings("BetterDebugF3", true, this));
      this.settings.add(this.ClipHelperInChat = new BoolSettings("ClipHelperInChat", true, this));
      this.settings.add(this.PaintInChat = new BoolSettings("PaintInChat", true, this));
      this.settings.add(this.CutChatMsgBg = new BoolSettings("CutChatMsgBg", true, this));
      this.settings.add(this.FastChatMsgQuit = new BoolSettings("FastChatMsgQuit", true, this));
      this.settings.add(this.NoStartInvHint = new BoolSettings("NoStartInvHint", true, this));
      this.settings.add(this.GuiTransitions = new BoolSettings("GuiTransitions", true, this));
      this.settings.add(this.ScreenRounding = new BoolSettings("ScreenRounding", true, this));
      this.settings.add(this.GuiMouseTrails = new BoolSettings("GuiMouseTrails", false, this));
      this.settings
         .add(
            this.TrailColorMode = new ModeSettings(
               "TrailColorMode",
               "PickColor",
               this,
               new String[]{"ClientColor", "PickColor", "Rainbow", "SpeedPickToWhite", "RainbowToWhite"},
               () -> this.GuiMouseTrails.getBool()
            )
         );
      this.settings
         .add(
            this.PickTrailColor = new ColorSettings(
               "PickTrailColor",
               -1,
               this,
               () -> this.GuiMouseTrails.getBool()
                     && !this.TrailColorMode.getMode().contains("Rainbow")
                     && !this.TrailColorMode.getMode().equalsIgnoreCase("ClientColor")
            )
         );
      this.settings.add(this.HDGuiItems = new BoolSettings("HDGuiItems", false, this));
      this.settings.add(this.SmoothMcScroll = new BoolSettings("SmoothMcScroll", true, this));
      this.setDemand(1, 1);
   }

   private int getMousePointBaseColor(float speedPC01, int indexOf) {
      int color = this.PickTrailColor.getCol();
      String var4 = this.TrailColorMode.getMode();
      switch (var4) {
         case "ClientColor":
            color = ClientColors.getColor1(indexOf, 1.0F);
            break;
         case "Rainbow":
            color = Color.getHSBColor((float)(System.currentTimeMillis() % 400L) / 400.0F, 0.9F, 1.0F).getRGB();
            break;
         case "RainbowToWhite":
            color = Color.getHSBColor((float)(System.currentTimeMillis() % 400L) / 400.0F, 0.9F, 1.0F).getRGB();
            color = ColorUtils.getOverallColorFrom(ColorUtils.swapAlpha(-1, (float)ColorUtils.getAlphaFromColor(color)), color, speedPC01);
            break;
         case "SpeedPickToWhite":
            color = ColorUtils.getOverallColorFrom(ColorUtils.swapAlpha(-1, (float)ColorUtils.getAlphaFromColor(color)), color, speedPC01);
      }

      return color;
   }

   public boolean isUsement() {
      return get != null && this.actived && !Panic.stop;
   }

   public boolean isBetterTabOverlay() {
      return this.isUsement() && this.BetterTabOverlay.getBool();
   }

   public boolean isScreensDarking() {
      return this.isUsement() && this.ScreensDarking.getBool();
   }

   public boolean isChatAnimations() {
      return this.isUsement() && this.ChatAnimations.getBool();
   }

   public boolean isContainerAnim() {
      return this.isUsement() && this.ContainerAnim.getBool();
   }

   public boolean isAnimPauseScreen() {
      return this.isUsement() && this.AnimPauseScreen.getBool();
   }

   public boolean isAddClientButtons() {
      return this.isUsement() && this.AddClientButtons.getBool();
   }

   public boolean isBetterButtons() {
      boolean apply = this.isUsement() && this.BetterButtons.getBool();
      if (apply && Config.isShaders()) {
         this.BetterButtons.setBool(false);
         ClientTune.get.playGuiScreenCheckBox(false);
         Client.msg("§f§lModules:§r §7[§l" + this.name + "§r§7]: выключите шейдеры для использования BetterButtons.", false);
      }

      return apply && System.getProperty("os.name").startsWith("Windows");
   }

   public boolean isBetterChatline() {
      return this.isUsement() && this.BetterChatline.getBool();
   }

   public boolean isBetterDebugF3() {
      return this.isUsement() && this.BetterDebugF3.getBool();
   }

   public boolean isClipHelperInChat() {
      return this.isUsement() && this.ClipHelperInChat.getBool();
   }

   public boolean isPaintInChat() {
      return this.isUsement() && this.PaintInChat.getBool();
   }

   public boolean isInvParticles() {
      return this.isUsement() && this.InvParticles.getBool();
   }

   public boolean isCutChatMsgBg() {
      return this.isUsement() && this.CutChatMsgBg.getBool();
   }

   public boolean isFastChatMsgQuit() {
      return this.isUsement() && this.FastChatMsgQuit.getBool();
   }

   public boolean isNoStartInvHint() {
      return this.isUsement() && this.NoStartInvHint.getBool();
   }

   public boolean isGuiTransitions() {
      return this.isUsement() && this.GuiTransitions.getBool();
   }

   public boolean isHDGuiItems() {
      return this.isUsement() && this.HDGuiItems.getBool();
   }

   public boolean isSmoothMcScroll() {
      return this.isUsement() && this.SmoothMcScroll.getBool();
   }

   public boolean influencePostRenderScreen$RoundScreen(ScaledResolution sr) {
      if (this.isUsement() && DisplayCheck.isVisible()) {
         float aPC = this.ScreenRounding.getAnimation();
         if (aPC >= 0.003921569F) {
            GL11.glPushMatrix();
            mc.entityRenderer.setupOverlayRendering();
            float bordersAPC = 0.3333F;
            float scaleFactorPC = (float)sr.getScaledWidth() / 1440.0F;
            float radius = aPC * (DisplayCheck.isFullscreen() ? 14.0F : 10.0F) * scaleFactorPC;
            float preRadius = aPC * 0.5F;
            int colUp = ColorUtils.getColor(0, 0, 0, aPC > 0.99607843F ? 255.0F : (float)MathUtils.easeInOutExpo((double)aPC) * 255.0F);
            int colDown = colUp;
            boolean fullScreen = DisplayCheck.isVisible() && DisplayCheck.isFullscreen();
            boolean hasDecoration = !fullScreen && DisplayCheck.isVisible();
            if (!fullScreen && !hasDecoration) {
               return false;
            }

            if (hasDecoration) {
               colUp = ColorUtils.swapAlpha(ColorUtils.getColor(245), (float)ColorUtils.getAlphaFromColor(colUp));
            }

            int col0Up = ColorUtils.swapAlpha(colUp, 0.0F);
            int col0Down = ColorUtils.swapAlpha(colDown, 0.0F);
            boolean[] roundsBoolSides = new boolean[]{fullScreen || hasDecoration, fullScreen || hasDecoration, fullScreen, fullScreen};
            int borderColUp = ColorUtils.swapAlpha(colUp, (float)ColorUtils.getAlphaFromColor(colUp) * bordersAPC);
            int borderColDown = ColorUtils.swapAlpha(colDown, (float)ColorUtils.getAlphaFromColor(colDown) * bordersAPC);
            RenderUtils.drawLightContureRectSmoothFullGradient(
               0.5F, 0.5F, (float)sr.getScaledWidth() - 0.5F, (float)sr.getScaledHeight() - 0.5F, borderColUp, borderColUp, borderColDown, borderColDown, false
            );
            GL11.glDisable(3008);
            if (roundsBoolSides[0]) {
               RenderUtils.drawCroneShadow(
                  (double)(radius - preRadius), (double)(radius - preRadius), -180, -90, radius - preRadius, preRadius, col0Up, colUp, false
               );
               RenderUtils.drawCroneShadow((double)(radius - preRadius), (double)(radius - preRadius), -180, -90, radius, radius, colUp, colUp, false);
               GL11.glDisable(3008);
            }

            if (roundsBoolSides[1]) {
               RenderUtils.drawCroneShadow(
                  (double)((float)sr.getScaledWidth() - (radius - preRadius)),
                  (double)(radius - preRadius),
                  -270,
                  -180,
                  radius - preRadius,
                  preRadius,
                  col0Up,
                  colUp,
                  false
               );
               RenderUtils.drawCroneShadow(
                  (double)((float)sr.getScaledWidth() - (radius - preRadius)), (double)(radius - preRadius), -270, -180, radius, radius, colUp, colUp, false
               );
               GL11.glDisable(3008);
            }

            if (roundsBoolSides[2]) {
               RenderUtils.drawCroneShadow(
                  (double)((float)sr.getScaledWidth() - (radius - preRadius)),
                  (double)((float)sr.getScaledHeight() - (radius - preRadius)),
                  -360,
                  -270,
                  radius - preRadius,
                  preRadius,
                  col0Down,
                  colDown,
                  false
               );
               RenderUtils.drawCroneShadow(
                  (double)((float)sr.getScaledWidth() - (radius - preRadius)),
                  (double)((float)sr.getScaledHeight() - (radius - preRadius)),
                  -360,
                  -270,
                  radius,
                  radius,
                  colDown,
                  colDown,
                  false
               );
               GL11.glDisable(3008);
            }

            if (roundsBoolSides[3]) {
               RenderUtils.drawCroneShadow(
                  (double)(radius - preRadius),
                  (double)((float)sr.getScaledHeight() - (radius - preRadius)),
                  -90,
                  0,
                  radius - preRadius,
                  preRadius,
                  col0Down,
                  colDown,
                  false
               );
               RenderUtils.drawCroneShadow(
                  (double)(radius - preRadius), (double)((float)sr.getScaledHeight() - (radius - preRadius)), -90, 0, radius, radius, colDown, colDown, false
               );
               GL11.glDisable(3008);
            }

            mc.entityRenderer.setupOverlayRendering(mc.gameSettings.guiScale);
            GL11.glPopMatrix();
            return true;
         }
      }

      return false;
   }

   private boolean isGuiMouseTrails() {
      return this.isUsement()
         && this.GuiMouseTrails.getBool()
         && DisplayCheck.isVisible()
         && DisplayCheck.isActive()
         && (mc.currentScreen != Client.clickGuiScreen || !ClickGuiScreen.colose)
         && (!(mc.currentScreen instanceof GuiContainer) || GuiContainer.inter.to != 0.0F);
   }

   public void onDrawGuiScreen(int mouseX, int mouseY, boolean pre) {
      if (!pre) {
         this.mouseSpeed = (int)Math.sqrt(
            (double)(
               Math.abs(this.lastMouseX - mouseX) * Math.abs(this.lastMouseX - mouseX)
                  + Math.abs(this.lastMouseY - mouseY) * Math.abs(this.lastMouseY - mouseY)
            )
         );
         if (this.isGuiMouseTrails()) {
            this.mousePointRemoveAuto(false);
            this.addMousePoint(mouseX, mouseY, 250L);
            List<Vec2fColored> pointsLine = this.getMouseTrailPoints(1.0F);
            if (pointsLine == null || pointsLine.isEmpty()) {
               return;
            }

            float lineWidth = 1.0F + Math.min((float)this.mouseSpeed / 75.0F, 1.0F) * 2.5F;
            GL11.glLineWidth(lineWidth * ScaledResolution.lpSCFactor());
            GL11.glDisable(2929);
            RenderUtils.drawVec2Colored(pointsLine, 3);
            GL11.glEnable(2929);
            GL11.glLineWidth(1.0F);
            List<Vec2fColored> pointsPolygon = this.getMouseTrailPoints(0.05F);
            if (pointsPolygon == null || pointsPolygon.isEmpty()) {
               return;
            }

            GL11.glBlendFunc(770, 1);
            GL11.glDisable(2929);
            RenderUtils.drawVec2Colored(pointsPolygon, 8);
            RenderUtils.drawVec2Colored(pointsPolygon, 5);
            GL11.glLineWidth(lineWidth * 3.0F);
            RenderUtils.drawVec2Colored(pointsPolygon, 3);
            GL11.glLineWidth(1.0F);
            GL11.glEnable(2929);
            GL11.glBlendFunc(770, 771);
         }

         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
      }
   }

   public void onInitOrCloseGuiScreen(boolean init) {
      if (init && this.isGuiMouseTrails()) {
         this.mousePointRemoveAuto(true);
      }
   }

   private void addMousePoint(int mouseX, int mouseY, long maxTime) {
      if (this.mouseSpeed > 0 && this.mouseSpeed < 400) {
         ScaledResolution sr = new ScaledResolution(mc);
         float aPC = Math.min((float)this.mouseSpeed / 75.0F, 1.0F);
         if (mouseX >= 0 && mouseY >= 0 && mouseX <= sr.getScaledWidth() && mouseY <= sr.getScaledHeight()) {
            this.mouseTrailPoints
               .add(
                  new ComfortUi.MouseTrailPoint(
                     (float)mouseX,
                     (float)mouseY,
                     maxTime,
                     ColorUtils.swapAlpha(this.getMousePointBaseColor(1.0F - aPC, this.mouseTrailPoints.size()), aPC * 255.0F)
                  )
               );
         }
      }
   }

   private void mousePointRemoveAuto(boolean clear) {
      if (clear) {
         if (!this.mouseTrailPoints.isEmpty()) {
            this.mouseTrailPoints.clear();
         }
      } else {
         this.mouseTrailPoints.removeIf(ComfortUi.MouseTrailPoint::removeIf);
      }
   }

   private List<Vec2fColored> getMouseTrailPoints(float aPC) {
      return this.mouseTrailPoints.isEmpty()
         ? null
         : (
            aPC == 1.0F
               ? this.mouseTrailPoints.stream().map(ComfortUi.MouseTrailPoint::getColoredVec).toList()
               : this.mouseTrailPoints
                  .stream()
                  .map(ComfortUi.MouseTrailPoint::getColoredVec)
                  .map(colVec -> new Vec2fColored(colVec.getX(), colVec.getY(), colVec.color = ColorUtils.toDark(colVec.color, aPC)))
                  .toList()
         );
   }

   @Override
   public void onUpdate() {
      if (this.isNoStartInvHint()) {
         if (mc.currentScreen == null) {
            this.cancelTooltip = true;
         } else if (Mouse.getDX() != 0 || Mouse.getDY() != 0) {
            this.cancelTooltip = false;
         }
      } else if (this.cancelTooltip) {
         this.cancelTooltip = false;
      }
   }

   @Override
   public void onToggled(boolean actived) {
      this.cancelTooltip = false;
      this.mousePointRemoveAuto(true);
      super.onToggled(actived);
   }

   private class MouseTrailPoint {
      private final float x;
      private final float y;
      private final long startMS = System.currentTimeMillis();
      private final long maxTimeAlive;
      private final int baseColor;

      public MouseTrailPoint(float x, float y, long maxTimeAlive, int baseColor) {
         this.x = x;
         this.y = y;
         this.maxTimeAlive = maxTimeAlive;
         this.baseColor = baseColor;
      }

      public float getTimePC() {
         return Math.min((float)(System.currentTimeMillis() - this.startMS) / (float)this.maxTimeAlive, 1.0F);
      }

      public Vec2fColored getColoredVec() {
         int color = ColorUtils.swapAlpha(
            this.baseColor, (float)ColorUtils.getAlphaFromColor(this.baseColor) * Math.min(MathUtils.valWave01(this.getTimePC()) * 1.25F, 1.0F)
         );
         return new Vec2fColored(this.x, this.y, color);
      }

      public boolean removeIf() {
         return this.getTimePC() == 1.0F;
      }
   }
}
