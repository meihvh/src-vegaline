package ru.govno.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;

public class EntityBox extends Module {
   public static EntityBox get;
   public BoolSettings BoxScalling;
   public BoolSettings Predict;
   public BoolSettings ExtendedRange;
   public BoolSettings PlayersResolver;
   public BoolSettings ModelSyncRealityAABB;
   public FloatSettings BoxScale;
   public FloatSettings TicksCount;
   public FloatSettings EntitiesReach;
   public FloatSettings BlocksReach;
   public ModeSettings PredictMode;
   private static float boxScale = 1.0F;
   private static float blocksReach;
   private static float entitiesReach;
   private static float ticksOffset;
   private static float selfPingTicksFloat;
   private static boolean pingSyncPredictTicks;
   private static boolean resolvePlayers;
   private static boolean renderModelSyncPosAABB;

   public EntityBox() {
      super("EntityBox", 0, Module.Category.COMBAT);
      this.settings.add(this.BoxScalling = new BoolSettings("BoxScalling", true, this));
      this.settings.add(this.BoxScale = new FloatSettings("BoxScale", 1.2F, 2.0F, 0.75F, this, () -> this.BoxScalling.getBool()));
      this.settings.add(this.Predict = new BoolSettings("Predict", true, this));
      this.settings.add(this.PredictMode = new ModeSettings("PredictMode", "Ticks", this, new String[]{"Ticks", "PingSync"}, () -> this.Predict.getBool()));
      this.settings
         .add(
            this.TicksCount = new FloatSettings(
               "TicksCount", 1.75F, 4.0F, 0.25F, this, () -> this.Predict.getBool() && this.PredictMode.getMode().equalsIgnoreCase("Ticks")
            )
         );
      this.settings.add(this.PlayersResolver = new BoolSettings("PlayersResolver", false, this));
      this.settings
         .add(this.ModelSyncRealityAABB = new BoolSettings("ModelSyncRealityAABB", false, this, () -> this.Predict.getBool() || this.PlayersResolver.getBool()));
      this.settings.add(this.ExtendedRange = new BoolSettings("ExtendedRange", true, this));
      this.settings.add(this.EntitiesReach = new FloatSettings("EntitiesReach", 0.6F, 3.0F, 0.0F, this, () -> this.ExtendedRange.getBool()));
      this.settings.add(this.BlocksReach = new FloatSettings("BlocksReach", 0.4F, 2.0F, 0.0F, this, () -> this.ExtendedRange.getBool()));
      this.setDemand(0, 2);
      get = this;
   }

   public static boolean isRenderModelSyncPosAABB() {
      return renderModelSyncPosAABB && !Panic.stop && get != null && get.isActived();
   }

   public static AxisAlignedBB getExtendedHitbox(Vec3d addPos, Entity entityIn, float scale, AxisAlignedBB prevBox) {
      if (prevBox == null || entityIn.world == null || !entityIn.world.isRemote) {
         return prevBox;
      } else if (entityIn instanceof EntityPlayerSP) {
         return prevBox;
      } else {
         boolean addPosIsZero = addPos.xCoord == 0.0 && addPos.yCoord == 0.0 && addPos.zCoord == 0.0;
         double w = (prevBox.maxX - prevBox.minX) * (double)scale;
         double wD2 = w / 2.0;
         double h = prevBox.maxY - prevBox.minY;
         double x;
         double y;
         double z;
         if (addPosIsZero) {
            x = entityIn.posX;
            y = entityIn.posY;
            z = entityIn.posZ;
         } else {
            x = entityIn.posX + addPos.xCoord;
            y = entityIn.posY + addPos.yCoord;
            z = entityIn.posZ + addPos.zCoord;
         }

         Vec3d firstPos = new Vec3d(x - wD2, y, z - wD2);
         Vec3d secondPos = firstPos.addVector(w, h, w);
         AxisAlignedBB aabb = new AxisAlignedBB(firstPos, secondPos);
         return aabb == null ? prevBox : aabb;
      }
   }

   private static int getPlayerPingMS(EntityPlayer player) {
      if (player == null) {
         return 0;
      } else {
         NetHandlerPlayClient net;
         NetworkPlayerInfo info;
         return (net = mc.getConnection()) != null && (info = net.getPlayerInfo(player.getUniqueID())) != null ? info.getResponseTime() : 0;
      }
   }

   @Override
   public void onRender2D(ScaledResolution sr) {
      boxScale = MathUtils.lerp(1.0F, this.BoxScale.getAnimation(), this.BoxScalling.getAnimation());
      blocksReach = this.ExtendedRange.getAnimation() * this.BlocksReach.getAnimation();
      entitiesReach = this.ExtendedRange.getAnimation() * this.EntitiesReach.getAnimation();
      ticksOffset = mc.isSingleplayer() ? 0.0F : this.Predict.getAnimation() * this.TicksCount.getAnimation();
      pingSyncPredictTicks = this.PredictMode.getMode().equalsIgnoreCase("PingSync");
      selfPingTicksFloat = MathUtils.clamp((float)getPlayerPingMS(Minecraft.player) / 50.0F / (float)GameSyncTPS.getGameConpense(1.0, 1.0F), 0.0F, 2.0F);
      resolvePlayers = this.PlayersResolver.getBool();
      renderModelSyncPosAABB = (this.Predict.getBool() || this.PlayersResolver.getBool()) && this.ModelSyncRealityAABB.getBool();
   }

   @Override
   public void onToggled(boolean actived) {
      if (!actived) {
         boxScale = 1.0F;
         blocksReach = 0.0F;
         entitiesReach = 0.0F;
         ticksOffset = 0.0F;
         resolvePlayers = false;
         renderModelSyncPosAABB = false;
      }
   }

   public static boolean hitboxModState() {
      return get.actived;
   }

   public static float hitboxModSizeBox() {
      return boxScale;
   }

   public static float hitboxModReachBlocks() {
      return blocksReach;
   }

   public static float hitboxModReachEntities() {
      return entitiesReach;
   }

   public static float hitboxModPredictSize(Entity entityIn) {
      if (pingSyncPredictTicks) {
         if (entityIn instanceof EntityOtherPlayerMP otherPlayer) {
            float playerPingTicks = (float)getPlayerPingMS(otherPlayer) / 50.0F / 2.0F;
            return MathUtils.clamp(playerPingTicks + selfPingTicksFloat / Math.max(playerPingTicks, 1.0F) / 2.0F, 0.0F, 4.0F);
         } else {
            return selfPingTicksFloat;
         }
      } else {
         return entityIn instanceof EntityLivingBase ? ticksOffset : 0.0F;
      }
   }

   public static Vec3d hitboxModAddVec(Entity entityIn, float ticks) {
      boolean nullTicks = ticks <= 0.0F;
      Vec3d predictVec = nullTicks
         ? Vec3d.ZERO
         : new Vec3d(
            (entityIn.posX - entityIn.prevPosX) * (double)ticks,
            (entityIn.posY - entityIn.prevPosY) * (double)ticks,
            (entityIn.posZ - entityIn.prevPosZ) * (double)ticks
         );
      if (resolvePlayers && entityIn instanceof EntityOtherPlayerMP otherPlayer && otherPlayer.canResolveAsServerPoses && Minecraft.player != null) {
         Vec3d sPosPrev = new Vec3d(otherPlayer.prevServerX, otherPlayer.prevServerY, otherPlayer.prevServerZ);
         Vec3d sPosCurrent = new Vec3d(otherPlayer.posX, otherPlayer.posY, otherPlayer.posZ);
         Vec3d playerPosition = otherPlayer.getPositionVector();
         Vec3d mcPosition = Minecraft.player.getPositionVector();
         Vec3d virtPos = sPosPrev.distanceTo(mcPosition) < sPosCurrent.distanceTo(mcPosition) ? sPosPrev : sPosCurrent;
         float pTicks = mc.getRenderPartialTicks();
         Vec3d deltaVec = new Vec3d(otherPlayer.posX - otherPlayer.prevPosX, otherPlayer.posY - otherPlayer.prevPosY, otherPlayer.posZ - otherPlayer.prevPosZ);
         virtPos = virtPos.add(deltaVec.add(deltaVec.scale((double)(pTicks / 2.0F - 1.0F))));
         double resolverScale = virtPos.distanceTo(playerPosition);
         if (resolverScale > 0.0 && resolverScale <= 12.0) {
            predictVec = predictVec.add(virtPos.add(playerPosition.scale(-1.0)));
         }
      }

      return predictVec;
   }

   public static boolean entityIsCurrentToExtend(Entity entityIn) {
      return entityIn != null && entityIn instanceof EntityLivingBase && !(entityIn instanceof EntityPlayerSP);
   }
}
