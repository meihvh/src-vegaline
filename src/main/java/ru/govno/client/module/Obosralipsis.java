package ru.govno.client.module;

import java.util.ArrayList;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.modules.WorldRender;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class Obosralipsis extends Module {
   private final BoolSettings SelfDetect;
   private final ArrayList<Obosralipsis.Perdun> updatedPerunsList = new ArrayList<>();
   private final ArrayList<Obosralipsis.SplashAnimatedVec3dColored> shitAnimationsList = new ArrayList<>();
   private final ArrayList<Obosralipsis.PhysicalParticleOfShit> splashParticlesList = new ArrayList<>();
   private Vec3d cameraPosUpdated = Vec3d.ZERO;

   public Obosralipsis() {
      super("Obosralipsis", 0, Module.Category.MISC);
      this.settings.add(this.SelfDetect = new BoolSettings("SelfDetect", true, this));
      this.setDemand(3, 3);
   }

   private int getFartTicksDuration() {
      return 4;
   }

   private int getMaxShitFlyingDistance(EntityPlayer player) {
      return 10;
   }

   private boolean addSpreadsChancedTemp() {
      return Math.random() > 0.3F;
   }

   private int getShitCount(EntityPlayer player) {
      return 550 / this.getFartTicksDuration();
   }

   private int getFartSoundCountInFolder() {
      return 11;
   }

   private void playFartSFXRandom(float volume) {
      int randomIntScale = new Random().nextInt(this.getFartSoundCountInFolder()) + 1;
      String soundName = "fartN" + randomIntScale + ".wav";
      MusicHelper.playSound(soundName, volume);
   }

   private float getVolumeForFartSoundOfPlayer(EntityPlayer player) {
      float volumeMax = 0.8F;
      float volumeMin = volumeMax / 20.0F;
      int distanceMax = 15;
      double dstPC = MathUtils.easeOutCubic(Math.min(this.cameraPosUpdated.distanceTo(this.getAssPositionVector(player)) / (double)((float)distanceMax), 1.0));
      double mcVolumePC = (double)mc.gameSettings.getSoundLevel(SoundCategory.MASTER);
      return MathUtils.lerp(volumeMax, volumeMin, (float)MathUtils.easeInOutQuad(dstPC * (1.0 - mcVolumePC)));
   }

   private void playFartSFXRandom(EntityPlayer player) {
      this.playFartSFXRandom(this.getVolumeForFartSoundOfPlayer(player));
   }

   private int getRandomShitColor() {
      float lrpPC = (float)Math.random();
      return ColorUtils.getOverallColorFrom(ColorUtils.getColor(35, 15, 0), ColorUtils.getColor(116, 50, 0), lrpPC * lrpPC);
   }

   private ArrayList<Obosralipsis.Perdun> controlPerdunList(boolean clear, long minFartDelay, long maxFartDelay) {
      if (clear) {
         this.updatedPerunsList.clear();
         return this.updatedPerunsList;
      } else {
         this.updatedPerunsList.removeIf(Obosralipsis.Perdun::isToRemove);
         if (mc.world == null) {
            return this.updatedPerunsList;
         } else {
            int fartDuration = this.getFartTicksDuration();

            for (EntityPlayer player : mc.world.playerEntities) {
               if (player != Minecraft.player || this.SelfDetect.getBool()) {
                  if (player != null
                     && player.isEntityAlive()
                     && !(Minecraft.player.getDistanceToEntity(player) > 10.0F)
                     && Minecraft.player.canEntityBeSeen(player)) {
                     if (player == Minecraft.player && Minecraft.player.isSneaking()) {
                        minFartDelay = 200L;
                        maxFartDelay = 300L;
                     }

                     Obosralipsis.Perdun findAnyPerdun = this.updatedPerunsList.stream().filter(perdun -> perdun.getPlayer() == player).findAny().orElse(null);
                     if (findAnyPerdun == null) {
                        this.updatedPerunsList.add(new Obosralipsis.Perdun(player, minFartDelay, maxFartDelay, fartDuration));
                     } else {
                        findAnyPerdun.setData(minFartDelay, maxFartDelay, fartDuration);
                     }
                  } else {
                     Obosralipsis.Perdun findAnyPerdun = this.updatedPerunsList.stream().filter(perdun -> perdun.getPlayer() == player).findAny().orElse(null);
                     if (findAnyPerdun != null) {
                        this.updatedPerunsList.remove(findAnyPerdun);
                     }
                  }
               }
            }

            return this.updatedPerunsList;
         }
      }
   }

   private Vec3d getAssPositionVector(EntityPlayer player) {
      return player.getPositionVector()
         .addVector(
            (double)(MathHelper.sin(MathHelper.toRadians(player.renderYawOffset)) * player.width / 5.0F),
            (double)player.getEyeHeight() / 2.5,
            (double)(-MathHelper.cos(MathHelper.toRadians(player.renderYawOffset)) * player.width / 5.0F)
         );
   }

   private float[] getRandFartRadiansYaw$Pitch(EntityPlayer player, float randYaw, float randPitch, float downValue) {
      float yawRandom = (float)Math.random() * 360.0F;
      float randRadian = MathHelper.toRadians(yawRandom);
      float randomDistancePC = yawRandom * 100.0F % 1.0F;
      randomDistancePC *= randomDistancePC;
      float yawAdditionPC01 = -MathHelper.sin(randRadian) * randomDistancePC;
      float pitchAdditionPC01 = MathHelper.cos(randRadian) * randomDistancePC;
      return new float[]{
         MathHelper.toRadians(MathUtils.wrapAngleTo180_float(player.renderYawOffset + 180.0F + randYaw * yawAdditionPC01)),
         MathHelper.toRadians(-MathUtils.clamp(downValue + randPitch * pitchAdditionPC01, -90.0F, 90.0F))
      };
   }

   private Vec3d[] startEndBlowPosesRand(EntityPlayer player, float randYaw, float randPitch, float downValue, float maxRange) {
      Vec3d start = this.getAssPositionVector(player);
      float[] radians = this.getRandFartRadiansYaw$Pitch(player, randYaw, randPitch, downValue);
      Vec3d end = start.addVector(
         (double)(-MathHelper.sin(radians[0]) * maxRange), (double)(MathHelper.sin(radians[1]) * maxRange), (double)(MathHelper.cos(radians[0]) * maxRange)
      );
      RayTraceResult ray = mc.world.rayTraceBlocks(start, end, false, true, true);
      if (ray == null) {
         return null;
      } else {
         end = ray.hitVec.addVector(0.0, 0.01, 0.0);
         return new Vec3d[]{start, end};
      }
   }

   private void addSplashAnim(EntityPlayer player, int countAdd) {
      float maxDistance = (float)this.getMaxShitFlyingDistance(player);

      for (int iteration = 0; iteration < countAdd; iteration++) {
         int shitColor = this.getRandomShitColor();
         int timeAnimation = 100 + (int)(250.0 * Math.random());
         int timeAlive = timeAnimation + 2700 + (int)(700.0 * Math.random());
         Vec3d[] animationPoses = this.startEndBlowPosesRand(player, 28.0F, 26.0F, 70.0F, maxDistance);
         if (animationPoses != null) {
            float dstDEPC = 1.0F - Math.min((float)animationPoses[0].distanceTo(animationPoses[1]), 1.0F);
            animationPoses = this.startEndBlowPosesRand(player, 30.0F + 90.0F * dstDEPC, 25.0F + 70.0F * dstDEPC, 45.0F, maxDistance);
            if (animationPoses != null) {
               this.shitAnimationsList
                  .add(new Obosralipsis.SplashAnimatedVec3dColored(animationPoses[0], animationPoses[1], timeAnimation, timeAlive, shitColor));
            }
         }
      }
   }

   private void updateSplashAnimationsList(boolean clear) {
      if (clear) {
         this.shitAnimationsList.clear();
      } else if (!this.shitAnimationsList.isEmpty()) {
         try {
            this.shitAnimationsList.removeIf(Obosralipsis.SplashAnimatedVec3dColored::onIsToRemove);
         } catch (Exception var3) {
            throw var3;
         }
      }
   }

   private void drawSplashAnimations(float partialTicks, float modAPC) {
      if (!this.shitAnimationsList.isEmpty() && !(modAPC * 255.0F < 1.0F)) {
         RenderUtils.setup3dForBlockPos(() -> {
            float beginScale = 7.225F;
            RenderUtils.tessellator.getBuffer().begin(0, DefaultVertexFormats.POSITION_COLOR);

            for (Obosralipsis.SplashAnimatedVec3dColored splashAnim : this.shitAnimationsList) {
               float timePC = splashAnim.getTimePC();
               int renderColor = splashAnim.getAlphedPointColor(timePC);
               Vec3d renderPos = splashAnim.getAnimatedVec3d(timePC);
               RenderUtils.tessellator.getBuffer().pos(renderPos.xCoord, renderPos.yCoord, renderPos.zCoord).color(renderColor).endVertex();
            }

            GL11.glPointSize(7.225F);
            GL11.glEnable(2929);
            GL11.glDepthMask(false);
            RenderUtils.tessellator.draw();
            GL11.glPointSize(1.0F);
            GL11.glDepthMask(true);
         }, false);
      }
   }

   private void addSpread(Obosralipsis.SplashAnimatedVec3dColored splash) {
      this.splashParticlesList.add(new Obosralipsis.PhysicalParticleOfShit(splash));
   }

   private void updateSpreadsList(boolean clear) {
      if (clear) {
         this.splashParticlesList.clear();
      } else if (!this.splashParticlesList.isEmpty()) {
         this.splashParticlesList.removeIf(Obosralipsis.PhysicalParticleOfShit::isToRemove);
         if (!this.splashParticlesList.isEmpty()) {
            this.splashParticlesList.forEach(part -> part.updatePhysics(0.02F));
         }
      }
   }

   private void drawSpreads(float partialTicks, float modAPC) {
      if (!this.splashParticlesList.isEmpty() && !(modAPC * 255.0F < 1.0F)) {
         RenderUtils.setup3dForBlockPos(() -> {
            float beginScale = 8.725F;
            float aPC = 0.8F;
            RenderUtils.tessellator.getBuffer().begin(0, DefaultVertexFormats.POSITION_COLOR);

            for (Obosralipsis.PhysicalParticleOfShit spreadPart : this.splashParticlesList) {
               int renderColor = spreadPart.getRenderColor(0.8F);
               Vec3d renderPos = spreadPart.getRenderVec3d(partialTicks);
               RenderUtils.tessellator.getBuffer().pos(renderPos.xCoord, renderPos.yCoord, renderPos.zCoord).color(renderColor).endVertex();
            }

            GL11.glPointSize(8.725F);
            GL11.glEnable(2929);
            GL11.glDepthMask(false);
            RenderUtils.tessellator.draw();
            GL11.glPointSize(1.0F);
            GL11.glDepthMask(true);
         }, false);
      }
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

   @Override
   public void alwaysRender3D(float partialTicks) {
      if (this.isActived()) {
         this.cameraPosUpdated = this.cameraPos();
         this.drawSplashAnimations(partialTicks, 1.0F);
         this.drawSpreads(partialTicks, 1.0F);
      }
   }

   @Override
   public void onUpdate() {
      this.updateSplashAnimationsList(false);
      this.updateSpreadsList(false);
      ArrayList<Obosralipsis.Perdun> perduns = this.controlPerdunList(false, 1600L, 7000L);
      if (!perduns.isEmpty()) {
         perduns.forEach(Obosralipsis.Perdun::processing);
      }
   }

   @Override
   public void onToggled(boolean actived) {
      this.updateSplashAnimationsList(true);
      this.updateSpreadsList(true);
      this.controlPerdunList(true, 0L, 0L);
   }

   private class Perdun {
      private long minFartDelay;
      private long maxFartDelay;
      private long fartDelay;
      private final TimerHelper fartTimer = TimerHelper.TimerHelperReseted();
      private final EntityPlayer player;
      private int fartTicksBackward = Integer.MAX_VALUE;
      private int fartTicksBackwardToSet;

      public Perdun(EntityPlayer player, long minFartDelay, long maxFartDelay, int fartTicksBackwardToSet) {
         this.player = player;
         this.fartTicksBackwardToSet = fartTicksBackwardToSet;
         this.setData(minFartDelay, maxFartDelay, fartTicksBackwardToSet);
         this.setRandFartDelay();
         this.fartTimer.setTime((long)MathUtils.lerp(0.0F, (float)this.fartDelay, (float)Math.random()));
      }

      public void setData(long minFartDelay, long maxFartDelay, int fartTicksBackwardToSet) {
         this.minFartDelay = minFartDelay;
         this.maxFartDelay = maxFartDelay;
         this.fartTicksBackwardToSet = fartTicksBackwardToSet;
      }

      public void setRandFartDelay() {
         this.fartDelay = (long)MathUtils.lerp((float)this.minFartDelay, (float)this.maxFartDelay, (float)Math.random());
      }

      public EntityPlayer getPlayer() {
         return this.player;
      }

      public void processing() {
         if (this.fartTimer.hasReached((double)this.fartDelay)) {
            this.setRandFartDelay();
            this.fartTicksBackward = 0;
            this.fartTimer.reset();
         }

         if (this.fartTicksBackward < this.fartTicksBackwardToSet) {
            if (this.player != null) {
               Obosralipsis.this.addSplashAnim(this.player, Obosralipsis.this.getShitCount(this.player));
               if (this.fartTicksBackward == 0 && this.player != Minecraft.player) {
                  this.player.hurtTime = 6;
               }

               this.player.getSimulation().moveSpeed.setAnim(this.player.getSimulation().moveSpeed.anim + 0.15F);
               if (this.fartTicksBackward == 1) {
                  Obosralipsis.this.playFartSFXRandom(this.player);
               }
            }

            this.fartTicksBackward++;
         }
      }

      public boolean isToRemove() {
         return this.player == null || Module.mc.world == null || Module.mc.world.getEntityByID(this.player.getEntityId()) == null;
      }
   }

   private class PhysicalParticleOfShit {
      private final Vec3d pos;
      private final Vec3d lastPos;
      private final Vec3d motion;
      private final int maxTimeAlive;
      private final int color;
      private final TimerHelper timeAlive = TimerHelper.TimerHelperReseted();

      public PhysicalParticleOfShit(Obosralipsis.SplashAnimatedVec3dColored splash) {
         this.maxTimeAlive = splash.maxTimeAlive;
         float maxTimeAlive = (float)this.maxTimeAlive;
         float speedMul = 185.0F;
         this.motion = new Vec3d(
            (splash.end.xCoord - splash.start.xCoord) / (double)maxTimeAlive * (double)speedMul,
            (splash.end.yCoord - splash.start.yCoord) / (double)maxTimeAlive * (double)speedMul,
            (splash.end.zCoord - splash.start.zCoord) / (double)maxTimeAlive * (double)speedMul
         );
         this.pos = new Vec3d(splash.end.xCoord - this.motion.xCoord, splash.end.yCoord - this.motion.yCoord, splash.end.zCoord - this.motion.zCoord);
         this.lastPos = new Vec3d(splash.end.xCoord - this.motion.xCoord, splash.end.yCoord - this.motion.yCoord, splash.end.zCoord - this.motion.zCoord);
         this.color = splash.baseColor;
      }

      public void updatePhysics(float emulateCollisionBox) {
         this.lastPos.xCoord = this.pos.xCoord;
         this.lastPos.yCoord = this.pos.yCoord;
         this.lastPos.zCoord = this.pos.zCoord;
         if (Module.mc.world != null) {
            emulateCollisionBox /= 2.0F;
            float predicate = 2.0F;
            if (!Module.mc
               .world
               .getCollisionBoxes(
                  null,
                  new AxisAlignedBB(this.pos.addVector(this.motion.xCoord * (double)predicate, (double)emulateCollisionBox, 0.0))
                     .expandXyz((double)emulateCollisionBox)
               )
               .isEmpty()) {
               this.motion.xCoord = 0.0;
               this.motion.zCoord *= 0.95F;
            }

            this.motion.xCoord *= 0.93F;
            if (this.motion.yCoord > -0.05F) {
               this.motion.yCoord -= 0.005F;
            }

            if (!Module.mc
               .world
               .getCollisionBoxes(
                  null, new AxisAlignedBB(this.pos.addVector(0.0, this.motion.yCoord * (double)predicate, 0.0)).expandXyz((double)emulateCollisionBox)
               )
               .isEmpty()) {
               this.motion.yCoord = -this.motion.yCoord * 0.1F;
            }

            if (!Module.mc
               .world
               .getCollisionBoxes(
                  null,
                  new AxisAlignedBB(this.pos.addVector(0.0, (double)emulateCollisionBox, this.motion.zCoord * (double)predicate))
                     .expandXyz((double)emulateCollisionBox)
               )
               .isEmpty()) {
               this.motion.xCoord *= 0.95F;
               this.motion.zCoord = 0.0;
            }

            this.motion.zCoord *= 0.93F;
         }

         this.pos.xCoord = this.pos.xCoord + this.motion.xCoord;
         this.pos.yCoord = this.pos.yCoord + this.motion.yCoord;
         this.pos.zCoord = this.pos.zCoord + this.motion.zCoord;
      }

      public Vec3d getRenderVec3d(float partialTicks) {
         return new Vec3d(
            MathUtils.lerp(this.lastPos.xCoord, this.pos.xCoord, (double)partialTicks),
            MathUtils.lerp(this.lastPos.yCoord, this.pos.yCoord, (double)partialTicks),
            MathUtils.lerp(this.lastPos.zCoord, this.pos.zCoord, (double)partialTicks)
         );
      }

      private float getTimePC() {
         return Math.min((float)this.timeAlive.getTime() / (float)this.maxTimeAlive, 1.0F);
      }

      public int getRenderColor(float aPC) {
         float timePC = this.getTimePC();
         aPC *= 1.0F - timePC;
         aPC *= Math.min(timePC / 0.075F, 1.0F);
         return ColorUtils.swapAlpha(ColorUtils.toDark(this.color, 0.6F), (float)ColorUtils.getAlphaFromColor(this.color) * aPC);
      }

      public boolean isToRemove() {
         return this.getTimePC() == 1.0F || Module.mc.world == null;
      }
   }

   private class SplashAnimatedVec3dColored {
      private final int maxTimeAnim;
      private final int maxTimeAlive;
      private final int baseColor;
      private final TimerHelper timerHelper = TimerHelper.TimerHelperReseted();
      private final Vec3d start;
      private final Vec3d end;
      private boolean spawnSpreadWaiting = Obosralipsis.this.addSpreadsChancedTemp();

      public SplashAnimatedVec3dColored(Vec3d start, Vec3d end, int maxTimeAnim, int maxTimeAlive, int color) {
         this.start = start;
         this.end = end;
         this.maxTimeAnim = maxTimeAnim;
         this.maxTimeAlive = maxTimeAlive;
         this.baseColor = color;
      }

      public float getTimePC() {
         return Math.min((float)this.timerHelper.getTime() / (float)this.maxTimeAnim, 1.0F);
      }

      public int getAlphedPointColor(float timePC) {
         float aPCAnim = MathUtils.valWave01(timePC);
         aPCAnim = MathUtils.lerp(
            aPCAnim,
            1.0F - Math.min((float)this.timerHelper.getTime() / (float)this.maxTimeAlive, 1.0F),
            1.0F - aPCAnim * aPCAnim * aPCAnim * aPCAnim * aPCAnim * aPCAnim * aPCAnim
         );
         return ColorUtils.swapAlpha(this.baseColor, (float)ColorUtils.getAlphaFromColor(this.baseColor) * aPCAnim);
      }

      public Vec3d getAnimatedVec3d(float timePC) {
         return new Vec3d(
            MathUtils.lerp(this.start.xCoord, this.end.xCoord, (double)timePC),
            MathUtils.lerp(this.start.yCoord, this.end.yCoord, (double)timePC),
            MathUtils.lerp(this.start.zCoord, this.end.zCoord, (double)timePC)
         );
      }

      public boolean onIsToRemove() {
         if (this.spawnSpreadWaiting && this.timerHelper.hasReached((double)this.maxTimeAnim)) {
            Obosralipsis.this.addSpread(this);
            this.spawnSpreadWaiting = false;
         }

         return this.timerHelper.hasReached((double)this.maxTimeAlive) || this.start == null || this.end == null;
      }
   }
}
