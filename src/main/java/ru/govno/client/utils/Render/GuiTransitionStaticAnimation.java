package ru.govno.client.utils.Render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiCreateFlatWorld;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiCustomizeSkin;
import net.minecraft.client.gui.GuiCustomizeWorldScreen;
import net.minecraft.client.gui.GuiFlatPresets;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenAddServer;
import net.minecraft.client.gui.GuiScreenCustomizePresets;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiScreenServerList;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.GuiWorldEdit;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.MathHelper;
import optifine.GuiAnimationSettingsOF;
import optifine.GuiDetailSettingsOF;
import optifine.GuiOtherSettingsOF;
import optifine.GuiPerformanceSettingsOF;
import optifine.GuiQualitySettingsOF;
import optifine.GuiScreenOF;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.ui.login.GuiAltLogin;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.URender.other.NoiseAnimation;
import shadersmod.client.GuiShaderOptions;
import shadersmod.client.GuiShaders;

public class GuiTransitionStaticAnimation {
   private final Minecraft mc = Minecraft.getMinecraft();
   private final AnimationUtils alphaPC = new AnimationUtils(0.0F, 0.0F, 0.02F);
   private int width;
   private int height;
   private Framebuffer framebuffer;
   private final NoiseAnimation noiseAnimation = new NoiseAnimation();

   public GuiTransitionStaticAnimation() {
      this.framebuffer = ShaderUtility.createFrameBuffer(this.framebuffer);
   }

   private boolean canAnimate() {
      if (!Panic.stop && ComfortUi.get != null && ComfortUi.get.isGuiTransitions()) {
         GuiScreen s = this.mc.currentScreen;
         return s == null
            ? false
            : s instanceof GuiCreateFlatWorld
               || s instanceof GuiCreateWorld
               || s instanceof GuiCustomizeSkin
               || s instanceof GuiCustomizeWorldScreen
               || s instanceof GuiFlatPresets
               || s instanceof GuiLanguage
               || s instanceof GuiMainMenu
               || s instanceof GuiMultiplayer
               || s instanceof GuiOptions
               || s instanceof GuiScreenAddServer
               || s instanceof GuiScreenCustomizePresets
               || s instanceof GuiScreenOptionsSounds
               || s instanceof GuiScreenResourcePacks
               || s instanceof GuiScreenServerList
               || s instanceof GuiScreenOF
               || s instanceof GuiWorldEdit
               || s instanceof GuiYesNo
               || s instanceof GuiWorldSelection
               || s instanceof GuiVideoSettings
               || s instanceof GuiShaderOptions
               || s instanceof GuiShaders
               || s instanceof GuiAnimationSettingsOF
               || s instanceof GuiQualitySettingsOF
               || s instanceof GuiOtherSettingsOF
               || s instanceof GuiDetailSettingsOF
               || s instanceof GuiPerformanceSettingsOF
               || s instanceof GuiControls
               || s instanceof GuiAltLogin
               || s instanceof GuiScreenAdvancements;
      } else {
         return false;
      }
   }

   private void drawTex(int glTexture, float alphaPC, float scaleCenter, boolean bloom, boolean smoothTex) {
      int color = ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDisable(3008);
      GL11.glDisable(2896);
      GL11.glDisable(2929);
      GL11.glDisable(2884);
      GL11.glEnable(3042);
      GL11.glShadeModel(7425);
      GL11.glBlendFunc(770, bloom ? 1 : 771);
      GL11.glBindTexture(3553, glTexture);
      if (smoothTex) {
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10241, 9729);
      }

      BufferBuilder buffer = Tessellator.getInstance().getBuffer();
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      buffer.pos(0.0, 0.0).tex(0.0, 0.0).color(color).endVertex();
      buffer.pos((double)this.width, 0.0).tex(1.0, 0.0).color(color).endVertex();
      buffer.pos((double)this.width, (double)this.height).tex(1.0, 1.0).color(color).endVertex();
      buffer.pos(0.0, (double)this.height).tex(0.0, 1.0).color(color).endVertex();
      GL11.glPushMatrix();
      RenderUtils.customScaledObject2D(0.0F, 0.0F, (float)this.width, (float)this.height, scaleCenter);
      Tessellator.getInstance().draw();
      GL11.glPopMatrix();
      GL11.glBindTexture(3553, 0);
      if (smoothTex) {
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexParameteri(3553, 10241, 9728);
      }

      GL11.glShadeModel(7424);
      GL11.glDisable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(2929);
      GL11.glEnable(2884);
      GL11.glEnable(3008);
   }

   public void reinitAnimation() {
      if (this.canAnimate()) {
         ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
         this.width = sr.getScaledWidth();
         this.height = sr.getScaledHeight();
         this.framebuffer = ShaderUtility.createFrameBuffer(this.framebuffer);
         this.framebuffer.framebufferClear();
         this.framebuffer.bindFramebuffer(true);
         this.drawTex(this.mc.getFramebuffer().framebufferTexture, 1.0F, 1.0F, false, true);
         this.framebuffer.unbindFramebuffer();
         this.alphaPC.setAnim(1.0F);
      }
   }

   public void renderAnimation(float animSpeed, float alphaPC) {
      if (this.canAnimate()) {
         this.alphaPC.speed = animSpeed;
         alphaPC *= this.alphaPC.getAnim();
         if (!(alphaPC * 255.0F < 1.0F)) {
            int noiseReduction = 9 + GuiScreen.closesCounter % 5;
            this.noiseAnimation.setNoisePC(1.0F - (float)MathUtils.easeInOutQuad((double)(alphaPC * (0.5F + alphaPC * 0.5F))));
            float ext = 90.0F * (1.0F - this.noiseAnimation.getNoiseProgress());
            int iterations = 5;
            int iCol = ColorUtils.getColor(0, 0, 0, 105.0F * alphaPC);

            for (int i2 = 0; i2 < iterations; i2++) {
               float i2PCMax = (float)i2 / (float)iterations;
               float extE = ext * i2PCMax;
               int iColE = ColorUtils.swapAlpha(iCol, (float)ColorUtils.getAlphaFromColor(iCol) * i2PCMax);

               for (int i = 0; i < 360; i += 60) {
                  float radian = MathHelper.toRadians((float)i);
                  GL11.glPushMatrix();
                  GL11.glTranslated((double)(MathHelper.sin(radian) * extE), (double)(MathHelper.cos(radian) * extE), 0.0);
                  this.noiseAnimation
                     .insertRender2D(
                        () -> RenderUtils.drawAlphedRect(0.0, 0.0, (double)this.width, (double)this.height, iColE),
                        new ScaledResolution(this.mc),
                        noiseReduction
                     );
                  GL11.glPopMatrix();
               }
            }

             float finalAlphaPC = alphaPC;
             float finalAlphaPC1 = alphaPC;
             float finalAlphaPC2 = alphaPC;
             this.noiseAnimation.insertRender2D(() -> {
               this.drawTex(this.mc.getFramebuffer().framebufferTexture, 1.0F, 1.0F, false, true);
               this.drawTex(this.framebuffer.framebufferTexture, finalAlphaPC * finalAlphaPC1 * finalAlphaPC2, 1.0F + (1.0F - finalAlphaPC), false, true);
            }, new ScaledResolution(this.mc), noiseReduction);
         }
      }
   }

   public boolean animationFinished() {
      return this.alphaPC.anim * 255.0F < 1.0F;
   }
}
