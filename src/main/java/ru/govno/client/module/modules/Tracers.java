package ru.govno.client.module.modules;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;

public class Tracers extends Module {
   BoolSettings Players;
   BoolSettings Friends;
   BoolSettings Mobs;
   BoolSettings LineAtLegsToHead;
   BoolSettings LinesGlowing;
   BoolSettings ApplyStippleLines;
   ColorSettings PlayerPick;
   ColorSettings FriendPick;
   ColorSettings MobsPick;
   FloatSettings LineWidthI;
   FloatSettings StippleStepPixels;
   ModeSettings StippleStartOf;
   private final AnimationUtils alphaPC = new AnimationUtils(0.0F, 0.0F, 0.025F);
   private final Tessellator tessellator = Tessellator.getInstance();
   private final BufferBuilder buffer = this.tessellator.getBuffer();
   private final List<Entity> entities = new CopyOnWriteArrayList<>();

   public Tracers() {
      super("Tracers", 0, Module.Category.RENDER);
      this.settings.add(this.Players = new BoolSettings("Players", true, this));
      this.settings.add(this.PlayerPick = new ColorSettings("PlayerColor", ColorUtils.getColor(255, 40, 95, 195), this, () -> this.Players.getBool()));
      this.settings.add(this.Friends = new BoolSettings("Friends", true, this));
      this.settings.add(this.FriendPick = new ColorSettings("FriendColor", ColorUtils.getColor(0, 255, 0), this, () -> this.Friends.getBool()));
      this.settings.add(this.Mobs = new BoolSettings("Mobs", false, this));
      this.settings.add(this.MobsPick = new ColorSettings("MobColor", ColorUtils.getColor(0, 170, 120, 110), this, () -> this.Mobs.getBool()));
      this.settings
         .add(
            this.LineWidthI = new FloatSettings(
               "LineWidth", 0.05F, 3.5F, 0.05F, this, () -> this.Players.getBool() || this.Friends.getBool() || this.Mobs.getBool()
            )
         );
      this.settings
         .add(
            this.LineAtLegsToHead = new BoolSettings(
               "LineAtLegsToHead", false, this, () -> this.Players.getBool() || this.Friends.getBool() || this.Mobs.getBool()
            )
         );
      this.settings
         .add(this.LinesGlowing = new BoolSettings("LinesGlowing", false, this, () -> this.Players.getBool() || this.Friends.getBool() || this.Mobs.getBool()));
      this.settings
         .add(
            this.ApplyStippleLines = new BoolSettings(
               "ApplyStippleLines", false, this, () -> this.Players.getBool() || this.Friends.getBool() || this.Mobs.getBool()
            )
         );
      this.settings.add(this.StippleStartOf = new ModeSettings("StippleStartOf", "Entity", this, new String[]{"Crosshair", "Entity"}));
      this.settings
         .add(
            this.StippleStepPixels = new FloatSettings(
               "StippleStepPixels",
               3.0F,
               20.0F,
               0.5F,
               this,
               () -> (this.Players.getBool() || this.Friends.getBool() || this.Mobs.getBool()) && this.ApplyStippleLines.getBool()
            )
         );
      this.setDemand(2, 1);
   }

   private float alphaPC(boolean modIsEnabled, List<Entity> entities) {
      this.alphaPC.to = modIsEnabled && entities != null && mc.gameSettings.thirdPersonView == 0 ? 1.0F : 0.0F;
      return this.alphaPC.getAnim();
   }

   private int[] getColor(Entity entityIn, float alphaPC) {
      int color = Integer.MIN_VALUE;
      if (entityIn instanceof EntityOtherPlayerMP player) {
         color = Client.friendManager.isFriend(player.getName()) ? this.FriendPick.getCol() : this.PlayerPick.getCol();
      } else if (entityIn instanceof EntityMob || entityIn instanceof EntityAnimal || entityIn instanceof EntityVillager) {
         color = Client.friendManager.isFriend(entityIn.getName())
            ? ColorUtils.getOverallColorFrom(this.MobsPick.getCol(), this.FriendPick.getCol())
            : this.MobsPick.getCol();
      }

      color = ColorUtils.swapAlpha(color, alphaPC * (float)ColorUtils.getAlphaFromColor(color));
      int color2 = ColorUtils.swapAlpha(color, alphaPC * (float)ColorUtils.getAlphaFromColor(color) / 4.0F);
      return new int[]{color, color2};
   }

   private double interpolate(double val, double val2, float pt) {
      return val + (val2 - val) * (double)pt;
   }

   private Vec3d getRenderEntityPos(Entity fromEntity, float pTicks) {
      double x = this.interpolate(fromEntity.lastTickPosX, fromEntity.posX, pTicks);
      double y = this.interpolate(fromEntity.lastTickPosY, fromEntity.posY, pTicks);
      double z = this.interpolate(fromEntity.lastTickPosZ, fromEntity.posZ, pTicks);
      return new Vec3d(x, y, z);
   }

   private Tracers.DVec3d DVecToEntity(Entity fromEntity, RenderManager manager, boolean lineAtLegsToHead) {
      float radiansF = (float) (-Math.PI / 180.0);
      Vec3d returnPos = new Vec3d(RenderManager.viewerPosX, RenderManager.viewerPosY, RenderManager.viewerPosZ);
      Vec3d second = this.getRenderEntityPos(fromEntity, mc.getRenderPartialTicks())
         .addVector(0.0, lineAtLegsToHead ? 0.0 : (double)(fromEntity.height / 2.0F), 0.0)
         .add(returnPos.scale(-1.0));
      float[] cameraRotsPlus = WorldRender.get.getLastCameraOrients();
      Vec3d first = new Vec3d(0.0, Crosshair.get.isActived() ? -6.0E-4 : 0.0, 0.285)
         .rotatePitch((Minecraft.player.rotationPitch + cameraRotsPlus[1]) * radiansF)
         .rotateYaw((Minecraft.player.rotationYaw + cameraRotsPlus[0]) * radiansF)
         .addVector(0.0, (double)Minecraft.player.getEyeHeight(), 0.0)
         .add(WorldRender.get.getLastTranslated().scale(-1.0));
      return lineAtLegsToHead ? new Tracers.DVec3d(first, second, second.addVector(0.0, (double)fromEntity.height, 0.0)) : new Tracers.DVec3d(first, second);
   }

   private void vertexFromVec3d(Vec3d vec, int color) {
      this.buffer.pos(vec).color(color).endVertex();
   }

   private void setup3dLinesRender(Runnable displaysLine, Runnable displaysGlow, float lineW, boolean glow, float glowWPlus, int setStippleLines) {
      boolean viewBobbing = mc.gameSettings.viewBobbing;
      mc.gameSettings.viewBobbing = false;
      mc.entityRenderer.setupCameraTransform(mc.getRenderPartialTicks(), 0);
      mc.gameSettings.viewBobbing = viewBobbing;
      mc.entityRenderer.disableLightmap();
      GL11.glDisable(3553);
      GL11.glDisable(2896);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glShadeModel(7425);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      GL11.glDisable(2929);
      GL11.glAlphaFunc(516, 0.003921569F);
      if (setStippleLines > 0) {
         GL11.glEnable(2852);
         GL11.glLineStipple(setStippleLines, (short)-21846);
      }

      GL11.glLineWidth(lineW);
      displaysLine.run();
      if (setStippleLines > 0) {
         GL11.glDisable(2852);
      }

      if (glow) {
         GL11.glBlendFunc(770, 1);
         GL11.glLineWidth(1.0E-4F + lineW + glowWPlus);
         displaysGlow.run();
      }

      GL11.glAlphaFunc(516, 0.1F);
      GL11.glEnable(2929);
      GL11.glLineWidth(1.0F);
      GL11.glHint(3154, 4352);
      GL11.glDisable(2848);
      GL11.glEnable(3553);
      GL11.glShadeModel(7424);
      GL11.glBlendFunc(770, 771);
   }

   private static boolean[] enabledTypesENTITY(Tracers mod) {
      return new boolean[]{mod.Players.getBool(), mod.Friends.getBool(), mod.Mobs.getBool()};
   }

   private boolean updatedList(boolean[] currents) {
      if (mc.world == null) {
         return false;
      } else {
         this.entities.clear();
         mc.world
            .getLoadedEntityList()
            .forEach(
               entity -> {
                  if (entity != null
                     && entity instanceof EntityLivingBase base
                     && base.isEntityAlive()
                     && (
                        base instanceof EntityOtherPlayerMP player
                              && (
                                 currents[0] && !Client.friendManager.isFriend(player.getName())
                                    || currents[1] && Client.friendManager.isFriend(player.getName())
                              )
                           || currents[2] && (base instanceof EntityMob || base instanceof EntityAnimal || base instanceof EntityVillager)
                     )) {
                     this.entities.add(base);
                  }
               }
            );
         return Minecraft.player != null && this.entities != null || !this.entities.isEmpty();
      }
   }

   private void drawBeginTracer(Entity fromEntity, RenderManager manager, float alphaPC, float brightPC01, boolean lineAtLegsToHead, boolean orderOfEntity) {
      int[] color = this.getColor(fromEntity, alphaPC);
      Tracers.DVec3d dVec = this.DVecToEntity(fromEntity, manager, lineAtLegsToHead);

      assert mc.getRenderViewEntity() != null;

      if (brightPC01 < 1.0F) {
         this.buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
         if (orderOfEntity) {
            this.vertexFromVec3d(dVec.first, ColorUtils.toDark(color[0], brightPC01));
            this.vertexFromVec3d(dVec.second, ColorUtils.toDark(color[1], brightPC01));
            if (dVec.third != null) {
               this.vertexFromVec3d(dVec.third, ColorUtils.toDark(color[1], brightPC01));
            }
         } else {
            if (dVec.third != null) {
               this.vertexFromVec3d(dVec.third, ColorUtils.toDark(color[1], brightPC01));
            }

            this.vertexFromVec3d(dVec.second, ColorUtils.toDark(color[1], brightPC01));
            this.vertexFromVec3d(dVec.first, ColorUtils.toDark(color[0], brightPC01));
         }

         this.tessellator.draw();
      } else {
         this.buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
         if (orderOfEntity) {
            this.vertexFromVec3d(dVec.first, color[0]);
            this.vertexFromVec3d(dVec.second, color[1]);
            if (dVec.third != null) {
               this.vertexFromVec3d(dVec.third, color[1]);
            }
         } else {
            if (dVec.third != null) {
               this.vertexFromVec3d(dVec.third, color[1]);
            }

            this.vertexFromVec3d(dVec.second, color[1]);
            this.vertexFromVec3d(dVec.first, color[0]);
         }

         this.tessellator.draw();
      }
   }

   private void drawTracers(
      List entities,
      RenderManager manager,
      float alphaPC,
      float lineW,
      boolean glow,
      float glowWPlus,
      float glowApcOfApc,
      int setStippleLines,
      boolean stippleOfEntity,
      boolean lineAtLegsToHead
   ) {
      this.setup3dLinesRender(
         () -> entities.forEach(e -> this.drawBeginTracer((Entity)e, manager, alphaPC, 1.0F, lineAtLegsToHead, stippleOfEntity)),
         () -> entities.forEach(e -> this.drawBeginTracer((Entity)e, manager, alphaPC, glowApcOfApc, lineAtLegsToHead, stippleOfEntity)),
         lineW,
         glow,
         glowWPlus,
         setStippleLines
      );
   }

   @Override
   public void alwaysRender3D() {
      this.StippleStepPixels.setFloat((float)((int)(this.StippleStepPixels.getFloat() * 2.0F + 0.5F)) / 2.0F);
      float alphaPC = this.alphaPC(this.actived, this.entities);
      if (!Panic.stop && !(alphaPC < 0.05F)) {
         RenderManager manager = mc.getRenderManager();
         boolean[] currentTypes = enabledTypesENTITY(this);
         if (this.updatedList(currentTypes)) {
            int stipple = (int)((this.StippleStepPixels.getAnimation() + 0.5F) * this.ApplyStippleLines.getAnimation());
            this.drawTracers(
               this.entities,
               manager,
               alphaPC,
               this.LineWidthI.getFloat(),
               this.LinesGlowing.canBeRender(),
               6.0F,
               0.125F * this.LinesGlowing.getAnimation(),
               stipple,
               stipple > 0 && this.StippleStartOf.getMode().equalsIgnoreCase("Entity"),
               this.LineAtLegsToHead.getBool()
            );
         }
      }
   }

   @Override
   public void onUpdate() {
      if (Crosshair.get.isActived() && Crosshair.get.MouseMotions.getBool()) {
         Client.msg("§f§lModules:§r §7[§l" + this.getName() + "§r§7]: Tracers не совместим с", false);
         Client.msg("§7MouseMotions в Crosshair, выключено,", false);
         Client.msg("§7что-бы включить MouseMotions отключите Tracers.", false);
         Crosshair.get.MouseMotions.setBool(false);
         ClientTune.get.playGuiScreenCheckBox(false);
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (Crosshair.get.isActived() && Crosshair.get.MouseMotions.getBool()) {
         Client.msg("§f§lModules:§r §7[§l" + this.getName() + "§r§7]: Tracers не совместим с", false);
         Client.msg("§7MouseMotions в Crosshair, выключено,", false);
         Client.msg("§7что-бы включить MouseMotions отключите Tracers.", false);
         Crosshair.get.MouseMotions.setBool(false);
         ClientTune.get.playGuiScreenCheckBox(false);
      }

      super.onToggled(actived);
   }

   protected final class DVec3d {
      Vec3d first;
      Vec3d second;
      Vec3d third;

      private DVec3d(Vec3d first, Vec3d second) {
         this.first = first;
         this.second = second;
      }

      private DVec3d(Vec3d first, Vec3d second, Vec3d third) {
         this.first = first;
         this.second = second;
         this.third = third;
      }
   }
}
