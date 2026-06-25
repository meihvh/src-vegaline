package net.minecraft.client.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class EntityOtherPlayerMP extends AbstractClientPlayer {
   private int otherPlayerMPPosRotationIncrements;
   private double otherPlayerMPX;
   private double otherPlayerMPY;
   private double otherPlayerMPZ;
   private double otherPlayerMPYaw;
   private double otherPlayerMPPitch;
   public double serverX;
   public double serverY;
   public double serverZ;
   public double prevServerX;
   public double prevServerY;
   public double prevServerZ;
   public boolean canResolveAsServerPoses;

   public EntityOtherPlayerMP(World worldIn, GameProfile gameProfileIn) {
      super(worldIn, gameProfileIn);
      this.stepHeight = 1.0F;
      this.noClip = true;
      this.renderOffsetY = 0.25F;
   }

   @Override
   public boolean isInRangeToRenderDist(double distance) {
      double d0 = this.getEntityBoundingBox().getAverageEdgeLength() * 10.0;
      if (Double.isNaN(d0)) {
         d0 = 1.0;
      }

      d0 = d0 * 64.0 * getRenderDistanceWeight();
      return distance < d0 * d0;
   }

   @Override
   public boolean attackEntityFrom(DamageSource source, float amount) {
      return true;
   }

   @Override
   public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
      this.prevServerX = this.serverX;
      this.prevServerY = this.serverY;
      this.prevServerZ = this.serverZ;
      this.canResolveAsServerPoses = this.prevServerX != 0.0 || this.prevServerY != 0.0 || this.prevServerZ != 0.0;
      this.serverX = x;
      this.serverY = y;
      this.serverZ = z;
      if (this.canResolveAsServerPoses) {
         this.canResolveAsServerPoses = this.serverX != 0.0 || this.serverY != 0.0 || this.serverZ != 0.0;
      }

      this.otherPlayerMPX = x;
      this.otherPlayerMPY = y;
      this.otherPlayerMPZ = z;
      this.otherPlayerMPYaw = (double)yaw;
      this.otherPlayerMPPitch = (double)pitch;
      this.otherPlayerMPPosRotationIncrements = posRotationIncrements;
   }

   @Override
   public void onUpdate() {
      this.renderOffsetY = 0.0F;
      super.onUpdate();
      this.prevLimbSwingAmount = this.limbSwingAmount;
      double d0 = this.posX - this.prevPosX;
      double d1 = this.posZ - this.prevPosZ;
      float f = MathHelper.sqrt(d0 * d0 + d1 * d1) * 4.0F;
      if (f > 1.0F) {
         f = 1.0F;
      }

      this.limbSwingAmount = this.limbSwingAmount + (f - this.limbSwingAmount) * 0.4F;
      this.limbSwing = this.limbSwing + this.limbSwingAmount;
   }

   @Override
   public void onLivingUpdate() {
      if (this.otherPlayerMPPosRotationIncrements > 0) {
         double d0 = this.posX + (this.otherPlayerMPX - this.posX) / (double)this.otherPlayerMPPosRotationIncrements;
         double d1 = this.posY + (this.otherPlayerMPY - this.posY) / (double)this.otherPlayerMPPosRotationIncrements;
         double d2 = this.posZ + (this.otherPlayerMPZ - this.posZ) / (double)this.otherPlayerMPPosRotationIncrements;
         double d3 = this.otherPlayerMPYaw - (double)this.rotationYaw;

         while (d3 < -180.0) {
            d3 += 360.0;
         }

         while (d3 >= 180.0) {
            d3 -= 360.0;
         }

         this.rotationYaw = (float)((double)this.rotationYaw + d3 / (double)this.otherPlayerMPPosRotationIncrements);
         this.rotationPitch = (float)(
            (double)this.rotationPitch + (this.otherPlayerMPPitch - (double)this.rotationPitch) / (double)this.otherPlayerMPPosRotationIncrements
         );
         this.otherPlayerMPPosRotationIncrements--;
         this.setPosition(d0, d1, d2);
         this.setRotation(this.rotationYaw, this.rotationPitch);
      }

      this.prevCameraYaw = this.cameraYaw;
      this.updateArmSwingProgress();
      float f1 = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
      float f = (float)Math.atan(-this.motionY * 0.2F) * 15.0F;
      if (f1 > 0.1F) {
         f1 = 0.1F;
      }

      if (!this.onGround || this.getHealth() <= 0.0F) {
         f1 = 0.0F;
      }

      if (this.onGround || this.getHealth() <= 0.0F) {
         f = 0.0F;
      }

      this.cameraYaw = this.cameraYaw + (f1 - this.cameraYaw) * 0.4F;
      this.cameraPitch = this.cameraPitch + (f - this.cameraPitch) * 0.8F;
      this.world.theProfiler.startSection("push");
      this.collideWithNearbyEntities();
      this.world.theProfiler.endSection();
   }

   @Override
   public void addChatMessage(ITextComponent component) {
      Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(component);
   }

   @Override
   public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
      return false;
   }

   @Override
   public BlockPos getPosition() {
      return new BlockPos(this.posX + 0.5, this.posY + 0.5, this.posZ + 0.5);
   }
}
