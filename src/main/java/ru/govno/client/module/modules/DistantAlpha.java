package ru.govno.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;

public class DistantAlpha extends Module {
   public static DistantAlpha get;
   public ModeSettings AlphaCurve;
   public FloatSettings MinAlpha;
   public FloatSettings KillDistance;
   public FloatSettings StartDistance;
   private final byte ONCE = 1;
   private float TEMP_APC = 1.0F;
   private boolean TEMP_BRIGHTED;
   private final float[] tempMulRGBA = new float[]{1.0F, 1.0F, 1.0F, 1.0F};

   public DistantAlpha() {
      super("DistantAlpha", 0, Module.Category.RENDER);
      this.settings
         .add(
            this.AlphaCurve = new ModeSettings("AlphaCurve", "Linear", this, new String[]{"Linear", "InCirc", "OutCirc", "InOutQuad", "InOutExpo", "OutCubic"})
         );
      this.settings.add(this.MinAlpha = new FloatSettings("MinAlpha", 0.2F, 0.5F, 0.0F, this));
      this.settings.add(this.StartDistance = new FloatSettings("StartDistance", 1.0F, 2.0F, 1.1F, this));
      this.settings.add(this.KillDistance = new FloatSettings("KillDistance", 0.4F, 1.0F, 0.15F, this));
      this.setDemand(0, 2);
      get = this;
   }

   private Vec3d cameraPos() {
      if (mc.world != null) {
         EntityPlayerSP player = Minecraft.player;
         if (player != null) {
            float partialTicks = mc.getRenderPartialTicks();
            float f = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
            float f1 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
            double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double)partialTicks;
            double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double)partialTicks + (double)player.getEyeHeight();
            double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)partialTicks;
            f += WorldRender.get.offPitchOrient;
            f1 += WorldRender.get.offYawOrient;
            if (Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) {
               int sideMul = Minecraft.getMinecraft().gameSettings.thirdPersonView == 1 ? 1 : -1;
               double camDist = WorldRender.get.cameraRedistance(4.0) * (double)sideMul;
               d0 += (double)MathHelper.sin(MathHelper.toRadians(f1)) * camDist;
               d1 += (double)MathHelper.sin(MathHelper.toRadians(f)) * camDist;
               d2 += (double)(-MathHelper.cos(MathHelper.toRadians(f1))) * camDist;
            }

            return new Vec3d(d0, d1, d2).add(WorldRender.get.getLastTranslated());
         }
      }

      return new Vec3d(RenderManager.renderPosX, RenderManager.renderPosY, RenderManager.renderPosZ).add(WorldRender.get.getLastTranslated());
   }

   private double getDistanceAtCamera(double x, double y, double z) {
      return Math.sqrt(this.cameraPos().squareDistanceTo(RenderManager.renderPosX + x, RenderManager.renderPosY + y, RenderManager.renderPosZ + z));
   }

   private float calcAlphaPC(double x, double y, double z) {
      float linear = (float)(
         Math.max(this.getDistanceAtCamera(x, y, z) - (double)this.KillDistance.getFloat(), 0.0)
            / (double)(this.StartDistance.getFloat() - this.KillDistance.getFloat())
      );
      linear = MathUtils.clamp(linear, 0.0F, 1.0F);
      float minAlpha = Math.max(this.MinAlpha.getFloat() + 0.0999F, 0.0F);
      linear = minAlpha + Math.max(linear - minAlpha, 0.0F);
      String var9 = this.AlphaCurve.getMode();
      switch (var9) {
         case "Linear":
            return linear;
         case "InOutQuad":
            return linear < 0.5F ? 2.0F * linear * linear : 1.0F - (float)Math.pow((double)(-2.0F * linear + 2.0F), 2.0) / 2.0F;
         case "InCirc":
            return 1.0F - (float)Math.sqrt(1.0 - Math.pow((double)linear, 2.0));
         case "OutCirc":
            return (float)Math.sqrt(1.0 - Math.pow((double)(linear - 1.0F), 2.0));
         case "InOutExpo":
            return linear < 0.5F
               ? (float)Math.pow(2.0, (double)(20.0F * linear - 10.0F)) / 2.0F
               : (2.0F - (float)Math.pow(2.0, (double)(-20.0F * linear + 10.0F))) / 2.0F;
         case "OutCubic":
            return 1.0F - (float)Math.pow((double)(1.0F - linear), 3.0);
         default:
            return 1.0F;
      }
   }

   private boolean notClearWhite(float[] rgba) {
      return ColorUtils.getColor((int)(rgba[0] * 255.0F), (int)(rgba[1] * 255.0F), (int)(rgba[2] * 255.0F), rgba[3] * 255.0F) != -1;
   }

   public void onRenderEntity(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, byte pass) {
      if (get.isActived() && (!(entity instanceof EntityPlayerSP) || Minecraft.getMinecraft().gameSettings.thirdPersonView != 0)) {
         switch (pass) {
            case 0:
               this.TEMP_APC = MathUtils.clamp(this.calcAlphaPC(x, y + (double)entity.getEyeHeight(), z), 0.003921569F, 1.0F);
               this.TEMP_BRIGHTED = this.notClearWhite(this.tempMulRGBA);
               if (this.TEMP_APC != 1.0F) {
                  if (!GL11.glIsEnabled(3042)) {
                     GL11.glEnable(3042);
                  }

                  if (this.TEMP_APC < 0.1F) {
                     GL11.glAlphaFunc(516, 0.0F);
                  }

                  if (this.TEMP_BRIGHTED) {
                     GL11.glColor4f(this.tempMulRGBA[0], this.tempMulRGBA[1], this.tempMulRGBA[2], this.TEMP_APC * this.tempMulRGBA[3]);
                  } else {
                     GL11.glColor4f(1.0F, 1.0F, 1.0F, this.TEMP_APC);
                  }
               }
               break;
            case 1:
               if (this.TEMP_APC != 1.0F) {
                  if (!GL11.glIsEnabled(3042)) {
                     GL11.glEnable(3042);
                  }

                  if (this.TEMP_BRIGHTED) {
                     GL11.glColor4f(this.tempMulRGBA[0], this.tempMulRGBA[1], this.tempMulRGBA[2], this.TEMP_APC * this.tempMulRGBA[3]);
                  } else {
                     GL11.glColor4f(1.0F, 1.0F, 1.0F, this.TEMP_APC);
                  }
               }
               break;
            case 2:
               if (this.TEMP_APC != 1.0F) {
                  GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                  GL11.glAlphaFunc(516, 0.1F);
                  if (GL11.glIsEnabled(3042)) {
                     GL11.glDisable(3042);
                  }

                  this.TEMP_APC = 1.0F;
               }
         }
      }
   }

   public void setTempEntityBrightness(float r, float g, float b, float a) {
      this.tempMulRGBA[0] = r;
      this.tempMulRGBA[1] = g;
      this.tempMulRGBA[2] = b;
      this.tempMulRGBA[3] = a;
   }

   public void setTempEntityBrightness(float[] rgba) {
      this.tempMulRGBA[0] = rgba[0];
      this.tempMulRGBA[1] = rgba[1];
      this.tempMulRGBA[2] = rgba[2];
      this.tempMulRGBA[3] = rgba[3];
   }

   public void unsetTempEntityBrightness() {
      this.tempMulRGBA[0] = 1.0F;
      this.tempMulRGBA[1] = 1.0F;
      this.tempMulRGBA[2] = 1.0F;
      this.tempMulRGBA[3] = 1.0F;
   }

   public boolean dontSetEntityBrightness(Entity entity) {
      float partialTicks = mc.getRenderPartialTicks();
      return this.isActived()
         && entity != null
         && (!(entity instanceof EntityPlayerSP) || Minecraft.getMinecraft().gameSettings.thirdPersonView != 0)
         && this.calcAlphaPC(
               entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTicks - RenderManager.renderPosX,
               entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTicks + (double)entity.getEyeHeight() - RenderManager.renderPosY,
               entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTicks - RenderManager.renderPosZ
            )
            < 1.0F;
   }
}
