package ru.govno.client.module.modules;

import javax.vecmath.Vector2f;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.input.Mouse;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.UControllers.DualsenseController;
import ru.govno.client.utils.Versions.NewPhisicsFixes;
import viamcp.fixes.AttackOrder;

public class AimAssist extends Module {
   static EntityLivingBase target;
   BoolSettings OnlyAttackKey;
   BoolSettings AimBot;
   BoolSettings AutoAttack;
   BoolSettings OldVerCooldown;
   BoolSettings CPSBypass;
   BoolSettings SmartCrits;
   BoolSettings StopSprint;
   BoolSettings Players;
   BoolSettings Invis;
   BoolSettings Walls;
   BoolSettings Mobs;
   FloatSettings Range;
   FloatSettings Fov;
   FloatSettings Speed;
   ModeSettings HitsMode;
   TimerHelper timerHelper = new TimerHelper();
   boolean hitR2Counted;
   int hitCounter;

   public AimAssist() {
      super("AimAssist", 0, Module.Category.COMBAT);
      this.settings.add(this.OnlyAttackKey = new BoolSettings("OnlyAttackKey", true, this, () -> this.AutoAttack.getBool() || this.AimBot.getBool()));
      this.settings.add(this.AimBot = new BoolSettings("AimBot", true, this));
      this.settings.add(this.Range = new FloatSettings("Range", 4.0F, 6.0F, 1.0F, this, () -> this.AutoAttack.getBool() || this.AimBot.getBool()));
      this.settings.add(this.Fov = new FloatSettings("Fov", 90.0F, 360.0F, 40.0F, this, () -> this.AimBot.getBool()));
      this.settings.add(this.Speed = new FloatSettings("Speed", 3.0F, 7.0F, 0.5F, this, () -> this.AimBot.getBool()));
      this.settings.add(this.AutoAttack = new BoolSettings("AutoAttack", true, this));
      this.settings
         .add(
            this.OldVerCooldown = new BoolSettings(
               "OldVerCooldown",
               false,
               this,
               () -> !NewPhisicsFixes.isOldVersion() && this.AutoAttack.getBool() && (this.Players.getBool() || this.Mobs.getBool())
            )
         );
      this.settings
         .add(this.CPSBypass = new BoolSettings("CPSBypass", false, this, () -> this.AutoAttack.getBool() && (this.Players.getBool() || this.Mobs.getBool())));
      this.settings.add(this.HitsMode = new ModeSettings("HitsMode", "Attack", this, new String[]{"Click", "Attack"}, () -> this.AutoAttack.getBool()));
      this.settings.add(this.SmartCrits = new BoolSettings("SmartCrits", true, this, () -> this.AutoAttack.getBool()));
      this.settings.add(this.StopSprint = new BoolSettings("StopSprint", true, this, () -> this.AutoAttack.getBool()));
      this.settings.add(this.Players = new BoolSettings("Players", true, this, () -> this.AutoAttack.getBool() || this.AimBot.getBool()));
      this.settings
         .add(
            this.Invis = new BoolSettings(
               "Invis", false, this, () -> (this.AutoAttack.getBool() || this.AimBot.getBool()) && (this.Players.getBool() || this.Mobs.getBool())
            )
         );
      this.settings
         .add(
            this.Walls = new BoolSettings(
               "Walls", false, this, () -> (this.AutoAttack.getBool() || this.AimBot.getBool()) && (this.Players.getBool() || this.Mobs.getBool())
            )
         );
      this.settings.add(this.Mobs = new BoolSettings("Mobs", false, this, () -> this.AutoAttack.getBool() || this.AimBot.getBool()));
      this.setDemand(0, 1);
   }

   private boolean isOldCooldown() {
      return this.OldVerCooldown.getBool() || NewPhisicsFixes.isOldVersion();
   }

   void hit(EntityLivingBase entityIn, boolean mouse, boolean stopSprint) {
      boolean sprint = Minecraft.player.serverSprintState;
      boolean saveCrit = this.canCrits() && sprint && stopSprint;
      if (saveCrit) {
         Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SPRINTING));
      }

      if (mouse) {
         mc.clickMouse();
      } else {
         AttackOrder.sendFixedAttack(Minecraft.player, entityIn, EnumHand.MAIN_HAND);
      }

      this.hitR2Counted = this.hitCounter % 5 == 2;
      this.hitCounter++;
      if (saveCrit) {
         Minecraft.player.serverSprintState = sprint;
      }
   }

   public float msCooldown() {
      if (!this.isOldCooldown()) {
         float maxDeviation = 1.0F;
         float msPardon = 100.0F;
         double attributeAttackSpeed = Minecraft.player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).getAttributeValue();
         long msCooldown = (long)Math.max(1.0 / attributeAttackSpeed * (double)(1000.0F * maxDeviation) - (double)msPardon, 450.0);
         if (this.CPSBypass.getBool() && this.hitR2Counted) {
            msCooldown += 50L;
         }

         msCooldown = (long)((double)msCooldown / GameSyncTPS.getGameConpense(1.0, GameSyncTPS.instance.SyncPercent.getFloat()));
         return (float)(msCooldown + 20L);
      } else {
         return this.CPSBypass.getBool() && this.hitR2Counted ? 100.0F : 50.0F;
      }
   }

   boolean hasCooled() {
      return this.timerHelper.hasReached((double)this.msCooldown()) && Minecraft.player.getCooledAttackStrength(0.0F) != 0.0F;
   }

   public boolean isCritical() {
      double x = Minecraft.player.posX;
      double y = Minecraft.player.posY;
      double z = Minecraft.player.posZ;
      boolean adobeHead = MoveMeHelp.isBlockAboveHead();
      if (!adobeHead) {
         if ((double)Minecraft.player.fallDistance <= 0.08) {
            return false;
         }
      } else if (Minecraft.player.isCollidedVertically && !Minecraft.player.onGround && adobeHead && (double)Minecraft.player.fallDistance < 0.01) {
         return false;
      }

      if (!adobeHead || Minecraft.player.fallDistance == 0.0F && Minecraft.player.isJumping()) {
         return !adobeHead
            ? true
            : Minecraft.player.fallDistance != 0.0F
               || mc.world.getBlockState(new BlockPos(x, y - 0.2001, z)).getBlock() != Blocks.AIR
                  && (!AirJump.get.actived || mc.world.getBlockState(new BlockPos(x, y - 0.2001, z)).getBlock() == Blocks.AIR || !Minecraft.player.isJumping());
      } else {
         return false;
      }
   }

   public boolean getBlockWithExpand(float expand, double x, double y, double z, Block block) {
      return mc.world.getBlockState(new BlockPos(x, y + 0.03, z)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x + (double)expand, y, z + (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x - (double)expand, y, z - (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x + (double)expand, y, z - (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x - (double)expand, y, z + (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x + (double)expand, y, z)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x - (double)expand, y, z)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x, y, z + (double)expand)).getBlock() == block
         || mc.world.getBlockState(new BlockPos(x, y, z - (double)expand)).getBlock() == block;
   }

   boolean confirmCritsInWater(double x, double y, double z) {
      boolean confirm = false;
      if (WaterSpeed.get.actived) {
         if (Entity.Getmotiony > 0.0
               && mc.world.getBlockState(new BlockPos(x, y - 0.3, z)).getBlock() == Blocks.WATER
               && !(mc.world.getBlockState(new BlockPos(x, y - 1.4, z)) instanceof BlockLiquid)
               && mc.world.getBlockState(new BlockPos(x, y + 1.0, z)).getBlock() == Blocks.AIR
            || ru.govno.client.module.modules.Speed.posBlock(x, y - 1.0, z) && mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.WATER) {
            confirm = true;
         }

         if (mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.WATER
            && mc.world.getBlockState(new BlockPos(x, y + 0.4, z)).getBlock() == Blocks.AIR) {
            confirm = true;
         }
      }

      if (mc.world.getBlockState(new BlockPos(x, y + Minecraft.player.getWaterOffset(), z)).getBlock() == Blocks.WATER
         && mc.world.getBlockState(new BlockPos(x, y + Minecraft.player.getWaterOffset() + 0.001, z)).getBlock() == Blocks.AIR
         && (Minecraft.player.isJumping() || Minecraft.player.fallDistance > 0.0F)) {
         confirm = true;
      }

      return confirm;
   }

   boolean canCrits() {
      double x = Minecraft.player.posX;
      double y = Minecraft.player.posY;
      double z = Minecraft.player.posZ;
      float ext = Minecraft.player.isJumping() ? 0.04F : 0.0F;
      if (ElytraBoost.get.actived && ElytraBoost.get.Mode.currentMode.equalsIgnoreCase("NcpFly") && ElytraBoost.canElytra()) {
         return false;
      } else if (Criticals.get.actived
         && Criticals.get.EntityHit.getBool()
         && !Criticals.get.HitMode.currentMode.equalsIgnoreCase("Grim")
         && !Minecraft.player.isJumping()
         && Minecraft.player.onGround) {
         return false;
      } else if ((!Minecraft.player.isInWeb || mc.world.getBlockState(new BlockPos(x, y + 0.01, z)).getBlock() == Blocks.AIR)
         && (!Minecraft.player.isInWater() || this.confirmCritsInWater(x, y, z))
         && !Minecraft.player.isInLava()) {
         if (!JesusSpeed.isSwimming && !Minecraft.player.isElytraFlying() && (!JesusSpeed.isJesused || Minecraft.player.fallDistance == 0.0F)) {
            if (Fly.get.actived) {
               return false;
            } else if (FreeCam.get.actived) {
               return false;
            } else if (Minecraft.player != null && Minecraft.player.isPotionActive(Potion.getPotionById(25))) {
               return false;
            } else if (ElytraBoost.get.actived && ElytraBoost.get.Mode.currentMode.equalsIgnoreCase("Vanilla")) {
               return false;
            } else if ((
                  MoveHelper.getBlockWithExpand(Minecraft.player.width / 2.0F, x, y + (double)ext, z, Blocks.WEB)
                     || MoveHelper.getBlockWithExpand(Minecraft.player.width / 2.0F, x, y + (double)ext, z, Blocks.WATER)
                     || MoveHelper.getBlockWithExpand(Minecraft.player.width / 2.0F, x, y + (double)ext, z, Blocks.LAVA)
               )
               && !this.confirmCritsInWater(x, y, z)) {
               return false;
            } else if (Minecraft.player.isJumping()
               || !Minecraft.player.onGround
               || Criticals.get.actived && Criticals.get.EntityHit.getBool() && !Criticals.get.HitMode.getMode().equalsIgnoreCase("NcpYPort")) {
               return !Minecraft.player.isOnLadder()
                     && !Minecraft.player.isElytraFlying()
                     && !Minecraft.player.capabilities.isFlying
                     && !Minecraft.player.isRiding()
                     && !Minecraft.player.isSpectator()
                  ? !Criticals.grimUpCriticals()
                  : false;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   Entity getMouseOverEntity() {
      EntityLivingBase base = MathUtils.getPointedEntity(new Vector2f(Minecraft.player.rotationYaw, Minecraft.player.rotationPitch), 100.0, 1.0F, false);
      return base != null && base != Minecraft.player && base.isEntityAlive() ? base : null;
   }

   @Override
   public void onUpdate() {
      target = this.getTarget(this.Range.getFloat());
      if (target != null) {
         if (this.AutoAttack.getBool()
            && target != HitAura.TARGET_ROTS
            && (Mouse.isButtonDown(0) || DualsenseController.R2_PRESSED || !this.OnlyAttackKey.getBool())) {
            mc.gameSettings.keyBindAttack.pressed = false;
            if (mc.objectMouseOver == null) {
               return;
            }

            if (this.getMouseOverEntity() == target || mc.objectMouseOver.entityHit == target || this.isValidEntity((EntityLivingBase)mc.pointedEntity)) {
               boolean hitting = this.hasCooled()
                  && (PushAttack.get.isActived() && !PushAttack.get.OnlyUsingSwing.getBool() || !Minecraft.player.isHandActive())
                  && (this.isOldCooldown() || this.isCritical() || !this.canCrits());
               if (hitting) {
                  this.hit(target, this.HitsMode.currentMode.equalsIgnoreCase("Click"), this.StopSprint.getBool());
                  this.timerHelper.reset();
               }
            }
         }

         super.onUpdate();
      }
   }

   @Override
   public void onMovement() {
      target = this.getTarget(this.Range.getFloat());
      TargetStrafe.target = target;
      if (target != null) {
         float f = this.faceTarget(target, 360.0F, 360.0F, false)[0];
         float f2 = this.faceTarget(target, 360.0F, 360.0F, false)[1];
         if ((!this.OnlyAttackKey.getBool() || Mouse.isButtonDown(0) || DualsenseController.R2_PRESSED)
            && this.getMouseOverEntity() != target
            && this.AimBot.getBool()
            && this.isInFOV(target, (double)this.Fov.getFloat())) {
            if (Minecraft.player.rotationYaw != f) {
               Minecraft.player.rotationYaw = MathUtils.lerp(Minecraft.player.rotationYaw, Rotation(target)[0], 0.05F * this.Speed.getFloat());
            }

            if (this.isInFOVPitch(target, (double)this.Fov.getFloat())) {
               if (Minecraft.player.rotationPitch != f2) {
                  Minecraft.player.rotationPitch = MathUtils.lerp(Minecraft.player.rotationPitch, Rotation(target)[1], 0.025F * this.Speed.getFloat());
               }

               Minecraft.player.rotationPitch = MathUtils.clamp(Minecraft.player.rotationPitch, -90.0F, 90.0F);
            }
         }
      }
   }

   public EntityLivingBase getTarget(float range) {
      if (HitAura.TARGET_ROTS != null) {
         return HitAura.TARGET_ROTS;
      } else {
         EntityLivingBase base = null;

         for (Object o : mc.world.loadedEntityList) {
            Entity entity = (Entity)o;
            EntityLivingBase living;
            if (entity instanceof EntityLivingBase
               && this.isValidEntity(living = (EntityLivingBase)entity)
               && living.getHealth() != 0.0F
               && Minecraft.player.getDistanceToEntity(living) <= range) {
               range = Minecraft.player.getDistanceToEntity(living);
               base = living;
            }
         }

         return base;
      }
   }

   public boolean isValidEntity(EntityLivingBase baseIn) {
      boolean players = this.Players.getBool();
      boolean mobs = this.Mobs.getBool();
      boolean walls = this.Walls.getBool();
      boolean invis = this.Invis.getBool();
      return HitAura.TARGET_ROTS != null
         ? baseIn == HitAura.TARGET_ROTS
         : baseIn != null
            && baseIn.getHealth() != 0.0F
            && (players && baseIn instanceof EntityOtherPlayerMP MP && MP != FreeCam.fakePlayer || mobs && !(baseIn instanceof EntityPlayer))
            && (walls || Minecraft.player.canEntityBeSeen(baseIn))
            && !Client.friendManager.isFriend(baseIn.getName())
            && (invis || !baseIn.isInvisible())
            && !Client.summit(baseIn);
   }

   public static float[] Rotation(Entity e) {
      double d = e.posX - Minecraft.player.posX;
      double d1 = e.posZ - Minecraft.player.posZ;
      if (e instanceof EntityLivingBase entitylivingbase) {
         ;
      }

      EntityLivingBase entitylivingbase = (EntityLivingBase)e;
      float y = (float)(entitylivingbase.posY + (double)entitylivingbase.getEyeHeight() - (double)(entitylivingbase.getEyeHeight() / 3.0F));
      double lastY = (double)y + 0.2F - (Minecraft.player.posY + (double)Minecraft.player.getEyeHeight());
      double d2 = (double)MathHelper.sqrt(d * d + d1 * d1);
      float yaw = (float)(Math.atan2(d1, d) * 180.0 / Math.PI - 92.0) + RandomUtils.nextFloat(1.0F, 7.0F);
      float pitch = (float)(-(Math.atan2(lastY, d2) * 210.0 / Math.PI)) + RandomUtils.nextFloat(1.0F, 7.0F);
      yaw = Minecraft.player.rotationYaw + RotationUtil.getSensitivity(MathHelper.wrapDegrees(yaw - Minecraft.player.rotationYaw));
      pitch = Minecraft.player.rotationPitch + RotationUtil.getSensitivity(MathHelper.wrapDegrees(pitch - Minecraft.player.rotationPitch));
      pitch = MathHelper.clamp(pitch, -88.5F, 89.9F);
      return new float[]{yaw, pitch};
   }

   public float[] faceTarget(Entity target, float p_706252, float p_706253, boolean miss) {
      double var4x = target.posX - Minecraft.player.posX;
      double var5 = target.posZ - Minecraft.player.posZ;
      double var7;
      if (target instanceof EntityLivingBase var6) {
         var7 = var6.posY + (double)var6.getEyeHeight() - (Minecraft.player.posY + (double)Minecraft.player.getEyeHeight());
      } else {
         AxisAlignedBB aabb = target.getEntityBoundingBoxCL();
         var7 = (aabb.minY + aabb.maxY) / 2.0 - (Minecraft.player.posY + (double)Minecraft.player.getEyeHeight());
      }

      double var8 = (double)MathHelper.sqrt(var4x * var4x + var5 * var5);
      float var9 = (float)(Math.atan2(var5, var4x) * 180.0 / Math.PI) - 90.0F;
      float var10 = (float)(-(Math.atan2(var7 - (target instanceof EntityPlayer ? 0.25 : 0.0), var8) * 180.0 / Math.PI));
      float f = mc.gameSettings.mouseSensitivity * 0.9F + 0.2F;
      float gcd = f * f * f * 1.2F;
      float pitch = this.updateRotation(Minecraft.player.rotationPitch, var10, p_706253);
      float yaw = this.updateRotation(Minecraft.player.rotationYaw, var9, p_706252);
      yaw -= yaw % gcd;
      pitch -= pitch % gcd;
      return new float[]{yaw, pitch};
   }

   public float updateRotation(float current, float intended, float speed) {
      float f = MathHelper.wrapDegrees(intended - current);
      if (f > speed) {
         f = speed;
      }

      if (f < -speed) {
         f = -speed;
      }

      return current + f;
   }

   private boolean isInFOV(EntityLivingBase entity, double angle) {
      angle *= 0.5;
      double angleDiff = (double)getAngleDifference(Minecraft.player.rotationYaw, getRotations(entity.posX, entity.posY, entity.posZ)[0]);
      return angleDiff > 0.0 && angleDiff < angle || -angle < angleDiff && angleDiff < 0.0;
   }

   private boolean isInFOVPitch(EntityLivingBase entity, double angle) {
      double angleDiff = (double)getAngleDifferencePitch(CPacketPlayer.lastSendedPitch, getRotations(entity.posX, entity.posY, entity.posZ)[1]);
      return angleDiff > 0.0 && angleDiff < angle || -angle < angleDiff && angleDiff < 0.0;
   }

   private static float getAngleDifference(float dir, float yaw) {
      float f = Math.abs(yaw - dir) % 360.0F;
      return f > 180.0F ? 360.0F - f : f;
   }

   private static float getAngleDifferencePitch(float dir, float pitch) {
      float f = Math.abs(pitch - dir) % 90.0F;
      return f > 0.0F ? 90.0F - f : f;
   }

   private static float[] getRotations(double x, double y, double z) {
      double diffX = x - Minecraft.player.posX;
      double diffY = (y + 0.5) / 2.0 - (Minecraft.player.posY + (double)Minecraft.player.getEyeHeight());
      double diffZ = z - Minecraft.player.posZ;
      double dist = (double)MathHelper.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
      float pitch = (float)(-(Math.atan2(diffY, dist) * 180.0 / Math.PI)) + (float)(Minecraft.player.getDistanceToEntity(target) >= 3.0F ? 3 : 1);
      return new float[]{yaw, pitch};
   }
}
