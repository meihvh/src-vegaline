package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class BackTrack extends Module {
   public static BackTrack get;
   private final FloatSettings TargetRange;
   private final FloatSettings MinSpeedThreshold;
   private final FloatSettings MaxSpeedThreshold;
   private final FloatSettings MinDistanceThreshold;
   private final FloatSettings TrackTicksMax;
   private final BoolSettings RenderTracks;
   private final BoolSettings RuleNoElytra;
   private final BoolSettings RuleNoLiquid;
   private final BoolSettings RuleOnlyPlayers;
   private final ModeSettings ColorMode;
   private final ColorSettings PickColor;
   private final FloatSettings Brightness;
   private final BoolSettings SmoothTracksMoves;
   private final BoolSettings PlayerModelRenderType;
   private final ModeSettings RenderModelType;
   private EntityPlayer self;
   final List<Integer> trackableEntitiesId = new ArrayList<>();
   final HashMap<Integer, BackTrack.PreviousTicksEntityTracker> tracks = new HashMap<>();

   public BackTrack() {
      super("BackTrack", 0, Module.Category.COMBAT);
      this.settings.add(this.TargetRange = new FloatSettings("TargetRange", 6.0F, 12.0F, 3.0F, this));
      this.settings.add(this.MinSpeedThreshold = new FloatSettings("MinSpeedThreshold", 0.08F, 0.3F, 0.01F, this));
      this.settings.add(this.MaxSpeedThreshold = new FloatSettings("MaxSpeedThreshold", 1.0F, 3.0F, 0.5F, this));
      this.settings.add(this.MinDistanceThreshold = new FloatSettings("MinDistanceThreshold", 2.7F, 5.0F, 0.0F, this));
      this.settings.add(this.TrackTicksMax = new FloatSettings("TrackTicksMax", 4.0F, 10.0F, 1.0F, this));
      this.settings.add(this.RenderTracks = new BoolSettings("RenderTracks", true, this));
      this.settings.add(this.RuleNoElytra = new BoolSettings("RuleNoElytra", true, this));
      this.settings.add(this.RuleNoLiquid = new BoolSettings("RuleNoLiquid", false, this));
      this.settings.add(this.RuleOnlyPlayers = new BoolSettings("RuleOnlyPlayers", false, this));
      this.settings
         .add(this.ColorMode = new ModeSettings("ColorMode", "Rainbow", this, new String[]{"Client", "Rainbow", "Picker"}, () -> this.RenderTracks.getBool()));
      this.settings
         .add(
            this.PickColor = new ColorSettings(
               "PickColor", ColorUtils.getColor(31, 133, 255), this, () -> this.RenderTracks.getBool() && this.ColorMode.getMode().equalsIgnoreCase("Picker")
            )
         );
      this.settings
         .add(
            this.Brightness = new FloatSettings(
               "Brightness", 0.5F, 1.0F, 0.1F, this, () -> this.RenderTracks.getBool() && !this.ColorMode.getMode().equalsIgnoreCase("Picker")
            )
         );
      this.settings.add(this.SmoothTracksMoves = new BoolSettings("SmoothTracksMoves", false, this, () -> this.RenderTracks.getBool()));
      this.settings.add(this.PlayerModelRenderType = new BoolSettings("PlayerModelRenderType", false, this, () -> this.RenderTracks.getBool()));
      this.settings
         .add(
            this.RenderModelType = new ModeSettings(
               "RenderModelType",
               "Fill",
               this,
               new String[]{"Fill", "Out", "Out&Glow"},
               () -> this.RenderTracks.getBool() && this.PlayerModelRenderType.getBool()
            )
         );
      this.setDemand(1, 2);
      get = this;
   }

   @Override
   public void onUpdate() {
      this.self = (EntityPlayer)(FreeCam.get.isActived() && FreeCam.fakePlayer != null ? FreeCam.fakePlayer : Minecraft.player);
      if (this.self != null) {
         this.trackableEntitiesId.clear();
         if (mc.world != null) {
            float range = this.TargetRange.getFloat();
            float minDistance = this.MinDistanceThreshold.getFloat();
            this.trackableEntitiesId
               .addAll(
                  mc.world
                     .getLoadedEntityList()
                     .stream()
                     .map(Entity::getLivingBaseOf)
                     .filter(Objects::nonNull)
                     .filter(
                        base -> (!this.RuleOnlyPlayers.getBool() || base instanceof EntityOtherPlayerMP)
                              && base != Minecraft.player
                              && base != this.self
                              && base.ticksExisted > 1
                              && base.isEntityAlive()
                              && base.getDistanceToEntity(this.self) <= range
                              && base.getDistanceToEntity(this.self) >= minDistance
                              && (this.tracks.containsKey(base.getEntityId()) || this.hasEntityMove(base))
                              && (!this.RuleNoElytra.getBool() || !base.isElytraFlying())
                              && (
                                 !this.RuleNoLiquid.getBool()
                                    || !base.isInWater()
                                       && !base.isInLava()
                                       && !base.isInWeb
                                       && !mc.world.getBlockState(base.getPosition()).getMaterial().isLiquid()
                              )
                              && !Client.friendManager.isFriend(base.getName())
                     )
                     .map(Entity::getEntityId)
                     .toList()
               );
         }

         for (int entityId : this.trackableEntitiesId) {
            if (!this.tracks.containsKey(entityId)) {
               this.tracks.put(entityId, new BackTrack.PreviousTicksEntityTracker(this.TrackTicksMax.getInt(), entityId));
            }
         }

         List<Integer> rems = new ArrayList<>();
         if (!this.tracks.isEmpty()) {
            for (Integer entityIdx : this.tracks.keySet()) {
               BackTrack.PreviousTicksEntityTracker tracker = this.tracks.get(entityIdx);
               if (tracker.removeIf()) {
                  rems.add(entityIdx);
               } else {
                  tracker.setMemoryTicks(this.TrackTicksMax.getInt());
                  tracker.updatePrevs();
               }
            }

            for (int rem : rems) {
               this.tracks.remove(rem);
            }
         }
      }
   }

   @Override
   public void onToggled(boolean actived) {
      this.stateAnim.to = actived ? 1.0F : 0.0F;
      this.tracks.clear();
      super.onToggled(actived);
   }

   private int getColorTrack(int index, float alphaPC) {
      index *= 60;
      String colorMode = this.ColorMode.getMode();
      int color = 0;
      switch (colorMode) {
         case "Client":
            color = ClientColors.getColor1(index, alphaPC * this.Brightness.getFloat());
            break;
         case "Rainbow":
            color = ColorUtils.swapAlpha(ColorUtils.rainbowGui(0, (long)index), 255.0F * alphaPC * this.Brightness.getFloat());
            break;
         case "Picker":
            color = ColorUtils.swapAlpha(this.PickColor.color, (float)ColorUtils.getAlphaFromColor(this.PickColor.color) * alphaPC);
      }

      return ColorUtils.getOverallColorFrom(color, ColorUtils.swapAlpha(-1, (float)ColorUtils.getAlphaFromColor(color)), 0.125F);
   }

   private void postPrepareDrawPlayerBloomModel(EntityPlayer player, int color, Vec3d onPos, float partialTicks) {
      Runnable defaultRenderModel = () -> {
         GameType gm = Minecraft.player.connection.getPlayerInfo(player.getUniqueID()).getGameType();
         player.setGameType(GameType.SPECTATOR);
         player.noRenderArms = true;
         mc.renderManager
            .doRenderEntityNoShadow(
               player, onPos.xCoord, onPos.yCoord, onPos.zCoord, MathUtils.lerp(player.prevRotationYaw, player.rotationYaw, partialTicks), partialTicks, true
            );
         player.noRenderArms = false;
         player.setGameType(gm);
      };
      Runnable renderBloomModelStage = () -> {
         GL11.glDisable(3553);
         GL11.glDisable(2896);
         GL11.glEnable(3008);
         GL11.glAlphaFunc(516, 0.003921569F);
         GL11.glDisable(3042);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 1);
         RenderUtils.glColor(color);
         defaultRenderModel.run();
         RenderUtils.resetColor();
         GL11.glBlendFunc(770, 771);
         GL11.glAlphaFunc(516, 0.1F);
      };
      renderBloomModelStage.run();
   }

   @Override
   public void alwaysRender3DV2(float partialTicks) {
      this.TrackTicksMax.setFloat((float)((int)(this.TrackTicksMax.getFloat() + 0.5F)));
      if (!this.tracks.isEmpty() && this.stateAnim.getAnim() > 0.003921569F && this.RenderTracks.canBeRender()) {
         float lw = MathUtils.clamp(((float)mc.displayHeight / 2.0F + (float)mc.displayWidth / 2.0F) / 80.0F, 0.25F, 40.0F);
         boolean smoothPos = this.SmoothTracksMoves.getBool();
         boolean lines = this.RenderModelType.getMode().contains("Out");
         boolean glowLines = lines && this.RenderModelType.getMode().contains("Out&Glow");

         for (Integer entityId : this.trackableEntitiesId) {
            BackTrack.PreviousTicksEntityTracker tracker = this.tracks.get(entityId);
            if (tracker != null && !tracker.getAxises().isEmpty()) {
               float aPC = this.stateAnim.anim * this.RenderTracks.getAnimation();
               RenderUtils.setup3dForBlockPos(
                  () -> {
                     GL11.glEnable(2929);
                     GL11.glDepthMask(false);
                     GL11.glDisable(2896);
                     if (!this.PlayerModelRenderType.getBool() || !(tracker.getEntity() instanceof EntityPlayer player)) {
                        int index = 0;

                        for (BackTrack.TickedAxis axis : tracker.getAxises()) {
                           float axisAPC = axis.getAlphaPC(partialTicks);
                           int color = this.getColorTrack(index, aPC * axisAPC);
                           if (ColorUtils.getAlphaFromColor(color) >= 1) {
                              AxisAlignedBB aabb = axis.getAabb(
                                    index == 0 ? null : tracker.getAxises().get(index - 1).getAabb(), smoothPos ? partialTicks : 0.0F
                                 )
                                 .expandXyz(-0.02);
                              drawGradientAlphaBox(aabb, false, true, 0, ColorUtils.toDark(color, 0.03F + axisAPC * 0.07F));
                              GL11.glLineWidth(0.025F);
                              drawCanisterBox(aabb, true, false, false, ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) / 2.0F), 0, 0);
                              GL11.glLineStipple(1, Short.reverseBytes((short)16));
                              GL11.glEnable(2852);
                              GL11.glEnable(2848);
                              GL11.glHint(3154, 4354);
                              GL11.glLineWidth(lw);
                              GlStateManager.tryBlendFuncSeparate(
                                 GlStateManager.SourceFactor.SRC_ALPHA_SATURATE,
                                 GlStateManager.DestFactor.ONE_MINUS_CONSTANT_COLOR,
                                 GlStateManager.SourceFactor.ONE,
                                 GlStateManager.DestFactor.ZERO
                              );
                              drawCanisterBox(aabb, true, false, false, ColorUtils.toDark(color, 0.333333F), 0, 0);
                              GlStateManager.tryBlendFuncSeparate(
                                 GlStateManager.SourceFactor.SRC_ALPHA_SATURATE,
                                 GlStateManager.DestFactor.ONE,
                                 GlStateManager.SourceFactor.ONE,
                                 GlStateManager.DestFactor.ZERO
                              );
                              GL11.glDisable(2852);
                              GL11.glDisable(2848);
                              GL11.glHint(3154, 4352);
                              GL11.glLineWidth(1.0F);
                           }

                           index++;
                        }
                     } else if (!tracker.getAxises().isEmpty()) {
                        try {
                           boolean canDraw = Minecraft.player != null
                              && Minecraft.player.connection != null
                              && tracker.getEntity() != null
                              && Minecraft.player.connection.getPlayerInfo(tracker.getEntity().getUniqueID()) != null;
                           if (canDraw) {
                              int index = 0;
                              List<BackTrack.TickedAxis> axises = tracker.getAxises();

                              for (BackTrack.TickedAxis axis : axises) {
                                 int rC = axis == axises.get(0) ? 2 : 1;

                                 for (int i = 0; i < rC; i++) {
                                    Vec3d renderModelPos = rC > 1 && i == 0
                                       ? new Vec3d(0.0, -1000.0, 0.0)
                                       : axis.getPosition(index == 0 ? null : tracker.getAxises().get(index - 1).getAabb(), smoothPos ? partialTicks : 0.0F);
                                    int color = this.getColorTrack(index, aPC * axis.getAlphaPC(partialTicks));
                                    float[] linesWidths = lines ? (glowLines ? new float[]{0.5F, 1.25F, 16.0F} : new float[]{1.5F}) : null;
                                    float[] APCs = lines ? (glowLines ? new float[]{1.0F, 0.25F, 0.1F} : new float[]{1.0F}) : null;
                                    if (linesWidths != null && linesWidths.length > 0) {
                                       GL11.glEnable(32823);
                                       GlStateManager.glPolygonMode(1032, 6913);
                                       int indexSOSI = 0;
                                       float[] var21 = linesWidths;
                                       int var22 = linesWidths.length;

                                       for (int var23 = 0; var23 < var22; var23++) {
                                          Float lineWidth = var21[var23];
                                          GL11.glLineWidth(lineWidth);
                                          int c = ColorUtils.toDark(color, 0.1F);
                                          this.postPrepareDrawPlayerBloomModel(
                                             player,
                                             ColorUtils.swapAlpha(c, (float)ColorUtils.getAlphaFromColor(c) * APCs[indexSOSI]),
                                             renderModelPos,
                                             partialTicks
                                          );
                                          indexSOSI++;
                                       }

                                       GL11.glLineWidth(1.0F);
                                       GlStateManager.glPolygonMode(1032, 6914);
                                       GL11.glDisable(10754);
                                    } else {
                                       this.postPrepareDrawPlayerBloomModel(player, ColorUtils.toDark(color, 0.3F), renderModelPos, partialTicks);
                                    }
                                 }

                                 index++;
                              }
                           }
                        } catch (Exception var26) {
                           var26.printStackTrace();
                        }
                     }

                     GL11.glDepthMask(true);
                  },
                  true
               );
            }
         }
      }
   }

   public List<AxisAlignedBB> getTracksAsEntity(Entity entity, AxisAlignedBB defaultAxis, boolean sorted) {
      if (entity == null) {
         return null;
      } else {
         List<AxisAlignedBB> list = new ArrayList<>();
         list.add(defaultAxis);
         BackTrack.PreviousTicksEntityTracker tracker = this.tracks.get(entity.getEntityId());
         if (tracker != null) {
            list.addAll(tracker.getAxisesToBoxes());
         }

         if (sorted) {
            list.sort(Comparator.comparing(obj -> this.getVecAsAxis(obj).distanceTo(this.self.getPositionVector())));
         }

         return list;
      }
   }

   private boolean hasEntityMove(Entity entity) {
      double dx = Math.abs(entity.posX - entity.lastTickPosX);
      double dy = Math.abs(entity.posY - entity.lastTickPosY);
      double dz = Math.abs(entity.posZ - entity.lastTickPosZ);
      double sqrt = Math.sqrt(dx * dx + dy * dy + dz * dz);
      return sqrt >= (double)this.MinSpeedThreshold.getFloat() && sqrt <= (double)this.MaxSpeedThreshold.getFloat();
   }

   private Vec3d getVecAsAxis(AxisAlignedBB axis) {
      return new Vec3d(axis.minX + (axis.maxX - axis.minX) / 2.0, axis.minY, axis.minZ + (axis.maxZ - axis.minZ) / 2.0);
   }

   public static void drawCanisterBox(
      AxisAlignedBB axisalignedbb, boolean outlineBox, boolean decussationBox, boolean fullBox, int outlineColor, int decussationColor, int fullColor
   ) {
      GlStateManager.pushMatrix();
      GL11.glDisable(3008);
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      if (outlineBox) {
         RenderUtils.glColor(outlineColor);
         RenderUtils.buffer.begin(2, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (decussationBox) {
         RenderUtils.glColor(decussationColor);
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (outlineBox) {
         RenderUtils.glColor(outlineColor);
         RenderUtils.buffer.begin(2, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (decussationBox) {
         RenderUtils.glColor(decussationColor);
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (outlineBox) {
         RenderUtils.glColor(outlineColor);
         RenderUtils.buffer.begin(2, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (decussationBox) {
         RenderUtils.glColor(decussationColor);
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (outlineBox) {
         RenderUtils.glColor(outlineColor);
         RenderUtils.buffer.begin(2, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (decussationBox) {
         RenderUtils.glColor(decussationColor);
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (outlineBox) {
         RenderUtils.glColor(outlineColor);
         RenderUtils.buffer.begin(2, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (decussationBox) {
         RenderUtils.glColor(decussationColor);
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (outlineBox) {
         RenderUtils.glColor(outlineColor);
         RenderUtils.buffer.begin(2, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (decussationBox) {
         RenderUtils.glColor(decussationColor);
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(1, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (fullBox) {
         RenderUtils.glColor(fullColor);
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION);
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).endVertex();
         RenderUtils.buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).endVertex();
         RenderUtils.tessellator.draw();
      }

      GL11.glEnable(3008);
      GL11.glHint(3154, 4352);
      GL11.glDisable(2848);
      GlStateManager.resetColor();
      GlStateManager.popMatrix();
   }

   public static void drawGradientAlphaBox(AxisAlignedBB bb, boolean outlineBox, boolean fullBox, int outlineColor, int fullColor) {
      GlStateManager.pushMatrix();
      GL11.glDisable(3008);
      GL11.glDisable(2884);
      GL11.glShadeModel(7425);
      GL11.glEnable(2848);
      double x1 = bb.minX;
      double y1 = bb.minY;
      double z1 = bb.minZ;
      double x2 = bb.maxX;
      double y2 = bb.maxY;
      double z2 = bb.maxZ;
      double wx = x2 - x1;
      double wy = y2 - y1;
      double wz = z2 - z1;
      if (outlineBox) {
         RenderUtils.buffer.begin(2, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x1, y1, z1).color(outlineColor).endVertex();
         RenderUtils.buffer.pos(x2, y1, z1).color(outlineColor).endVertex();
         RenderUtils.buffer.pos(x2, y1, z2).color(outlineColor).endVertex();
         RenderUtils.buffer.pos(x1, y1, z2).color(outlineColor).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x1, y1, z1).color(outlineColor).endVertex();
         RenderUtils.buffer.pos(x1, y2, z1).color(0).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x2, y1, z1).color(outlineColor).endVertex();
         RenderUtils.buffer.pos(x2, y2, z1).color(0).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x1, y1, z2).color(outlineColor).endVertex();
         RenderUtils.buffer.pos(x1, y2, z2).color(0).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x2, y1, z2).color(outlineColor).endVertex();
         RenderUtils.buffer.pos(x2, y2, z2).color(0).endVertex();
         RenderUtils.tessellator.draw();
      }

      if (fullBox) {
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x1, y1, z1).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1 + wx / 2.0, y1, z1).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1 + wx / 2.0, y1, z1 + wz / 2.0).color(0).endVertex();
         RenderUtils.buffer.pos(x1, y1, z1 + wz / 2.0).color(fullColor).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x2, y1, z1).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1 + wx / 2.0, y1, z1).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1 + wx / 2.0, y1, z1 + wz / 2.0).color(0).endVertex();
         RenderUtils.buffer.pos(x2, y1, z1 + wz / 2.0).color(fullColor).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x2, y1, z2).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1 + wx / 2.0, y1, z2).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1 + wx / 2.0, y1, z1 + wz / 2.0).color(0).endVertex();
         RenderUtils.buffer.pos(x2, y1, z1 + wz / 2.0).color(fullColor).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x1, y1, z2).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1 + wx / 2.0, y1, z2).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1 + wx / 2.0, y1, z1 + wz / 2.0).color(0).endVertex();
         RenderUtils.buffer.pos(x1, y1, z1 + wz / 2.0).color(fullColor).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x1, y1, z1).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1, y2, z1).color(0).endVertex();
         RenderUtils.buffer.pos(x2, y2, z1).color(0).endVertex();
         RenderUtils.buffer.pos(x2, y1, z1).color(fullColor).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x1, y1, z1).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1, y2, z1).color(0).endVertex();
         RenderUtils.buffer.pos(x1, y2, z2).color(0).endVertex();
         RenderUtils.buffer.pos(x1, y1, z2).color(fullColor).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x1, y1, z2).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x1, y2, z2).color(0).endVertex();
         RenderUtils.buffer.pos(x2, y2, z2).color(0).endVertex();
         RenderUtils.buffer.pos(x2, y1, z2).color(fullColor).endVertex();
         RenderUtils.tessellator.draw();
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         RenderUtils.buffer.pos(x2, y1, z1).color(fullColor).endVertex();
         RenderUtils.buffer.pos(x2, y2, z1).color(0).endVertex();
         RenderUtils.buffer.pos(x2, y2, z2).color(0).endVertex();
         RenderUtils.buffer.pos(x2, y1, z2).color(fullColor).endVertex();
         RenderUtils.tessellator.draw();
      }

      GL11.glDisable(2848);
      GL11.glShadeModel(7424);
      GL11.glEnable(3008);
      GL11.glEnable(2884);
      GlStateManager.glLineWidth(1.0F);
      GlStateManager.popMatrix();
   }

   private class PreviousTicksEntityTracker {
      private int memoryTicks;
      private final int entityId;
      private List<BackTrack.TickedAxis> axisList = new ArrayList<>();

      public PreviousTicksEntityTracker(int memoryTicks, int entityId) {
         this.memoryTicks = memoryTicks;
         this.entityId = entityId;
      }

      public void setMemoryTicks(int memoryTicks) {
         this.memoryTicks = memoryTicks;
      }

      private EntityLivingBase getEntity() {
         Entity entity = Module.mc.world.getEntityByID(this.entityId);
         return entity instanceof EntityLivingBase ? (EntityLivingBase)entity : null;
      }

      public void updatePrevs() {
         EntityLivingBase trackableEntity = this.getEntity();
         if (trackableEntity != null && BackTrack.this.hasEntityMove(trackableEntity)) {
            if (this.axisList == null) {
               this.axisList = new ArrayList<>();
            }

            this.axisList.add(BackTrack.this.new TickedAxis(trackableEntity, this.memoryTicks));
         }

         this.axisList.removeIf(BackTrack.TickedAxis::removeIf);
         this.axisList.forEach(BackTrack.TickedAxis::update);
      }

      public List<BackTrack.TickedAxis> getAxises() {
         return this.axisList;
      }

      public List<AxisAlignedBB> getAxisesToBoxes() {
         return this.axisList.stream().map(BackTrack.TickedAxis::getAabb).toList();
      }

      public List<AxisAlignedBB> getAxisesToBoxes(float partialTicks) {
         if (partialTicks != 0.0F && partialTicks != 1.0F) {
            List<AxisAlignedBB> boxes = new ArrayList<>();
            int index = 0;

            for (BackTrack.TickedAxis axis : this.axisList) {
               if (index == 0) {
                  boxes.add(axis.getAabb());
                  index++;
               } else {
                  BackTrack.TickedAxis prevTickedAxis = null;

                  try {
                     prevTickedAxis = this.axisList.get(index - 1);
                  } catch (IndexOutOfBoundsException var8) {
                     var8.printStackTrace();
                  }

                  if (prevTickedAxis != null) {
                     boxes.add(axis.getAabb(prevTickedAxis.getAabb(), partialTicks));
                  }

                  index++;
               }
            }

            return this.axisList.stream().map(BackTrack.TickedAxis::getAabb).toList();
         } else {
            return this.getAxisesToBoxes();
         }
      }

      public boolean removeIf() {
         Entity entity = this.getEntity();
         return entity == null
            || !entity.isEntityAlive()
            || this.axisList.size() < 2 && !BackTrack.this.hasEntityMove(entity)
            || Client.friendManager.isFriend(entity.getName());
      }
   }

   private class TickedAxis {
      private final EntityLivingBase base;
      private final AxisAlignedBB aabb;
      private int ticks;
      private final int ticksMax;

      public TickedAxis(EntityLivingBase base, int ticksAlive) {
         this.base = base;
         this.aabb = this.base.getEntityBoundingBoxCL();
         this.ticksMax = this.ticks = ticksAlive;
      }

      public void update() {
         this.ticks--;
      }

      public float getAlphaPC(float partialTicks) {
         return (float)MathUtils.easeInOutQuadWave((double)Math.max(((float)this.ticks + 0.5F - partialTicks) / (float)this.ticksMax, 0.0F));
      }

      public boolean removeIf() {
         return this.ticks <= 0;
      }

      public AxisAlignedBB getAabb() {
         return this.aabb;
      }

      public AxisAlignedBB getAabb(AxisAlignedBB prevAABB, float partialTicks) {
         return prevAABB != null && !prevAABB.equals(this.aabb) && partialTicks != 0.0F && partialTicks != 1.0F
            ? new AxisAlignedBB(
               MathUtils.lerp(prevAABB.minX, this.aabb.minX, (double)partialTicks),
               MathUtils.lerp(prevAABB.minY, this.aabb.minY, (double)partialTicks),
               MathUtils.lerp(prevAABB.minZ, this.aabb.minZ, (double)partialTicks),
               MathUtils.lerp(prevAABB.maxX, this.aabb.maxX, (double)partialTicks),
               MathUtils.lerp(prevAABB.maxY, this.aabb.maxY, (double)partialTicks),
               MathUtils.lerp(prevAABB.maxZ, this.aabb.maxZ, (double)partialTicks)
            )
            : this.aabb;
      }

      public Vec3d getPosition() {
         return new Vec3d(this.aabb.minX + (this.aabb.maxX - this.aabb.minX) / 2.0, this.aabb.minY, this.aabb.minZ + (this.aabb.maxZ - this.aabb.minZ) / 2.0);
      }

      public Vec3d getPosition(AxisAlignedBB prevAABB, float partialTicks) {
         AxisAlignedBB aabb = this.getAabb(prevAABB, partialTicks);
         return new Vec3d(aabb.minX + (aabb.maxX - aabb.minX) / 2.0, aabb.minY, aabb.minZ + (aabb.maxZ - aabb.minZ) / 2.0);
      }

      public EntityLivingBase getEntity() {
         return this.base;
      }
   }
}
