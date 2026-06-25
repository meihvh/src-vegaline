package ru.govno.client.module.modules;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventLightingCheck;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.DisplayCheck;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.GaussianBlur;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.URender.other.NoiseAnimation;

public class WorldRender extends Module {
   public static WorldRender get;
   public BoolSettings ClientPlayersSkins;
   public BoolSettings FastWorldLoad;
   public BoolSettings RenderBarrier;
   public BoolSettings ItemPhysics;
   public BoolSettings ChunksDebuger;
   public BoolSettings FullBright;
   public BoolSettings BlockLightFix;
   public BoolSettings WorldBloom;
   public BoolSettings CustomParticles;
   public BoolSettings DecreaseTotemParticle;
   public BoolSettings BloomParticles;
   public BoolSettings WorldReTime;
   public BoolSettings SkyRecolor;
   public BoolSettings FogRedistance;
   public BoolSettings ClearWeather;
   public BoolSettings CustomCamDist;
   public BoolSettings AntiAliasing;
   public BoolSettings AltReverseCamera;
   public BoolSettings FreeLook;
   public BoolSettings CustomViewBobbing;
   public BoolSettings CameraTweaks;
   public BoolSettings CameraFovRework;
   public BoolSettings SpawnAnimations;
   public BoolSettings BlurBlocks;
   public BoolSettings BlocksAlignment;
   public BoolSettings DeepBlocksShadows;
   public BoolSettings ClampFpsOnMinimized;
   public ModeSettings SkinsApplyTo;
   public ModeSettings SelfSkin;
   public ModeSettings BrightMode;
   public ModeSettings Time;
   public ModeSettings SkyColorMode;
   public ModeSettings ViewShakingType;
   public ModeSettings AAFactor;
   public FloatSettings BloomPower;
   public FloatSettings ParticleSpeed;
   public FloatSettings ParticleCount;
   public FloatSettings TimeCustom;
   public FloatSettings TimeSpinSpeed;
   public FloatSettings SkyFadeSpeed;
   public FloatSettings SkyClientColBright;
   public FloatSettings SkyBright;
   public FloatSettings FogDistanceCustom;
   public FloatSettings CameraRedistance;
   public FloatSettings CameraSmoothing;
   public ColorSettings SkyColorPick;
   public ColorSettings SkyColorPick2;
   private ResourceLocation PS_SHAPES_TEXTURE = null;
   private final NoiseAnimation altWorldLoading = new NoiseAnimation();
   private double dx;
   private double dy;
   private double dz;
   private double pdx;
   private double pdy;
   private double pdz;
   private double lx;
   private double ly;
   private double lz;
   private Vec3d lastTranslated;
   public boolean freeLookState;
   public boolean prevFreeLookState;
   private float sYaw;
   private float sPitch;
   private float cYawOff;
   private float cPitchOff;
   private float prevCYawOff;
   private float prevCPitchOff;
   public float offYawOrient;
   public float offPitchOrient;
   AnimationUtils orientYawAnim = new AnimationUtils(0.0F, 0.0F, 0.2F);
   AnimationUtils orientPitchAnim = new AnimationUtils(0.0F, 0.0F, 0.2F);
   private final float[] cameraOrientsPlus = new float[2];
   public AnimationUtils fovMultiplier = new AnimationUtils(1.0F, 1.0F, 0.08F);
   public boolean isItemPhysics = false;
   protected AnimationUtils spinnedTime = new AnimationUtils(0.0F, 0.0F, 0.075F);
   protected float current = 0.0F;
   boolean smoothingTime;
   float oldTime = -1.2398746E8F;
   private boolean prevDeepBlocksShadows;
   boolean rend = true;
   boolean viewBobbing;
   boolean viewTriggerActCH;
   public float oldGamma;
   private boolean wantToCustomLoading;
   private final TimerHelper soundDelay = TimerHelper.TimerHelperReseted();
   private int shakeType = 0;
   private final int[] COLORS_SHAPES = new int[]{
      ColorUtils.getColor(0, 125, 255), ColorUtils.getColor(255, 0, 45), ColorUtils.getColor(255, 0, 232), ColorUtils.getColor(0, 255, 130)
   };
   private final Tessellator tessellator = Tessellator.getInstance();
   private final BufferBuilder buffer = this.tessellator.getBuffer();
   private final Random RANDOM = new Random();
   private final List<WorldRender.PSParticle> PS_PARTICLES_LIST = new ArrayList<>();

   public WorldRender() {
      super("WorldRender", 0, Module.Category.RENDER);
      this.settings.add(this.ClientPlayersSkins = new BoolSettings("ClientPlayersSkins", true, this));
      this.settings
         .add(
            this.SkinsApplyTo = new ModeSettings(
               "SkinsApplyTo",
               "Self&Friends",
               this,
               new String[]{"Self", "Friends", "Self&Friends", "Players", "Self&Players", "Players&Friends", "All"},
               () -> this.ClientPlayersSkins.getBool()
            )
         );
      this.settings
         .add(
            this.SelfSkin = new ModeSettings(
               "SelfSkin",
               "Skin4",
               this,
               new String[]{
                  "Skin1",
                  "Skin2",
                  "Skin3",
                  "Skin4",
                  "Skin5",
                  "Skin6",
                  "Skin7",
                  "Skin8",
                  "Skin9",
                  "Skin10",
                  "Skin11",
                  "Skin12",
                  "Skin13",
                  "Skin14",
                  "Skin15",
                  "Skin16",
                  "Skin17",
                  "Skin18",
                  "Skin19",
                  "Skin20",
                  "Skin21",
                  "Skin22",
                  "Skin23"
               },
               () -> this.ClientPlayersSkins.getBool() && (this.SkinsApplyTo.getMode().contains("Self") || this.SkinsApplyTo.getMode().equalsIgnoreCase("All"))
            )
         );
      this.settings.add(this.FastWorldLoad = new BoolSettings("FastWorldLoad", false, this));
      this.settings.add(this.RenderBarrier = new BoolSettings("RenderBarrier", false, this));
      this.settings.add(this.ItemPhysics = new BoolSettings("ItemPhysics", true, this));
      this.settings.add(this.ChunksDebuger = new BoolSettings("ChunksDebuger", false, this));
      this.settings.add(this.FullBright = new BoolSettings("FullBright", false, this));
      this.settings.add(this.BrightMode = new ModeSettings("BrightMode", "Vision", this, new String[]{"Vision", "Gamma"}, () -> this.FullBright.getBool()));
      this.settings.add(this.BlockLightFix = new BoolSettings("BlockLightFix", true, this));
      this.settings.add(this.WorldBloom = new BoolSettings("WorldBloom", true, this));
      this.settings.add(this.BloomPower = new FloatSettings("BloomPower", 0.4F, 0.75F, 0.05F, this, () -> this.WorldBloom.getBool()));
      this.settings.add(this.CustomParticles = new BoolSettings("CustomParticles", false, this));
      this.settings.add(this.ParticleSpeed = new FloatSettings("ParticleSpeed", 0.5F, 1.5F, 0.3F, this, () -> this.CustomParticles.getBool()));
      this.settings.add(this.ParticleCount = new FloatSettings("ParticleCount", 0.85F, 5.0F, 0.25F, this, () -> this.CustomParticles.getBool()));
      this.settings.add(this.DecreaseTotemParticle = new BoolSettings("DecreaseTotemParticle", true, this, () -> this.CustomParticles.getBool()));
      this.settings.add(this.BloomParticles = new BoolSettings("BloomParticles", false, this, () -> this.CustomParticles.getBool()));
      this.settings.add(this.WorldReTime = new BoolSettings("WorldReTime", true, this));
      this.settings
         .add(
            this.Time = new ModeSettings(
               "Time",
               "Night",
               this,
               new String[]{"Evening", "Night", "Morning", "Day", "SpinTime", "Custom", "RealWorldTime"},
               () -> this.WorldReTime.getBool()
            )
         );
      this.settings
         .add(
            this.TimeCustom = new FloatSettings(
               "TimeCustom", 14000.0F, 24000.0F, 0.0F, this, () -> this.WorldReTime.getBool() && this.Time.currentMode.equalsIgnoreCase("Custom")
            )
         );
      this.settings
         .add(
            this.TimeSpinSpeed = new FloatSettings(
               "TimeSpinSpeed", 1.0F, 3.0F, 0.1F, this, () -> this.WorldReTime.getBool() && this.Time.currentMode.equalsIgnoreCase("SpinTime")
            )
         );
      this.settings.add(this.SkyRecolor = new BoolSettings("SkyRecolor", false, this));
      this.settings
         .add(
            this.SkyColorMode = new ModeSettings(
               "SkyColorMode", "Colored", this, new String[]{"Colored", "Fade", "Client", "ReBright"}, () -> this.SkyRecolor.getBool()
            )
         );
      this.settings
         .add(
            this.SkyColorPick = new ColorSettings(
               "SkyColorPick",
               ColorUtils.getColor(40, 40, 255, 140),
               this,
               () -> this.SkyRecolor.getBool()
                     && (this.SkyColorMode.currentMode.equalsIgnoreCase("Colored") || this.SkyColorMode.currentMode.equalsIgnoreCase("Fade"))
            )
         );
      this.settings
         .add(
            this.SkyColorPick2 = new ColorSettings(
               "SkyColorPick2",
               ColorUtils.getColor(40, 40, 255, 60),
               this,
               () -> this.SkyRecolor.getBool() && this.SkyColorMode.currentMode.equalsIgnoreCase("Fade")
            )
         );
      this.settings
         .add(
            this.SkyFadeSpeed = new FloatSettings(
               "SkyFadeSpeed", 0.35F, 1.5F, 0.1F, this, () -> this.SkyRecolor.getBool() && this.SkyColorMode.currentMode.equalsIgnoreCase("Fade")
            )
         );
      this.settings
         .add(
            this.SkyClientColBright = new FloatSettings(
               "SkyClientColBright", 0.6F, 1.0F, 0.05F, this, () -> this.SkyRecolor.getBool() && this.SkyColorMode.currentMode.equalsIgnoreCase("Client")
            )
         );
      this.settings
         .add(
            this.SkyBright = new FloatSettings(
               "SkyBright", 0.4F, 1.0F, 0.0F, this, () -> this.SkyRecolor.getBool() && this.SkyColorMode.currentMode.equalsIgnoreCase("ReBright")
            )
         );
      this.settings.add(this.FogRedistance = new BoolSettings("FogRedistance", false, this));
      this.settings.add(this.FogDistanceCustom = new FloatSettings("FogDistanceCustom", 40.0F, 120.0F, 15.0F, this, () -> this.FogRedistance.getBool()));
      this.settings.add(this.ClearWeather = new BoolSettings("ClearWeather", true, this));
      this.settings.add(this.CustomCamDist = new BoolSettings("CustomCamDist", true, this));
      this.settings.add(this.CameraRedistance = new FloatSettings("CameraRedistance", 4.0F, 15.0F, 1.0F, this, () -> this.CustomCamDist.getBool()));
      this.settings.add(this.AntiAliasing = new BoolSettings("AntiAliasing", true, this));
      this.settings
         .add(this.AAFactor = new ModeSettings("AAFactor", "AA-x2", this, new String[]{"AA-x2", "AA-x4", "AA-x8"}, () -> this.AntiAliasing.getBool()));
      this.settings.add(this.AltReverseCamera = new BoolSettings("AltReverseCamera", false, this));
      this.settings.add(this.FreeLook = new BoolSettings("FreeLook", false, this));
      this.settings.add(this.CustomViewBobbing = new BoolSettings("CustomViewBobbing", false, this));
      this.settings
         .add(
            this.ViewShakingType = new ModeSettings(
               "ViewShakingType",
               "Increased",
               this,
               new String[]{"Increased", "StableCamera", "SpeedLike", "Agressive"},
               () -> this.CustomViewBobbing.getBool()
            )
         );
      this.settings.add(this.CameraTweaks = new BoolSettings("CameraTweaks", false, this));
      this.settings.add(this.CameraSmoothing = new FloatSettings("CameraSmoothing", 1.3F, 2.0F, 0.5F, this, () -> this.CameraTweaks.getBool()));
      this.settings.add(this.CameraFovRework = new BoolSettings("CameraFovRework", false, this, () -> this.CameraTweaks.getBool()));
      this.settings.add(this.SpawnAnimations = new BoolSettings("SpawnAnimations", false, this));
      this.settings.add(this.BlurBlocks = new BoolSettings("BlurBlocks", false, this));
      this.settings.add(this.BlocksAlignment = new BoolSettings("BlocksAlignment", false, this));
      this.settings.add(this.DeepBlocksShadows = new BoolSettings("DeepBlocksShadows", false, this));
      this.settings.add(this.ClampFpsOnMinimized = new BoolSettings("ClampFpsOnMinimized", true, this));

      try {
         DynamicTexture dynamicTexture = new DynamicTexture(
            TextureUtil.readBufferedImage(
               Minecraft.getMinecraft()
                  .getResourceManager()
                  .getResource(new ResourceLocation("vegaline/modules/worldrender/particles/particleshapesatlas.png"))
                  .getInputStream()
            )
         );
         dynamicTexture.setBlurMipmap(true, false);
         mc.getTextureManager()
            .bindTexture(
               this.PS_SHAPES_TEXTURE = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(dynamicTexture.toString(), dynamicTexture)
            );
      } catch (Exception var2) {
         var2.fillInStackTrace();
      }

      get = this;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   public static boolean doDecreaseSet5FpsLimitAlways() {
//      if (!Panic.stop && get != null && get.isActived() && get.ClampFpsOnMinimized.getBool()) {
//         try {
//            return !DisplayCheck.isVisible();
//         } catch (Exception var1) {
//            var1.printStackTrace();
//         }
//      }

      return false;
   }

   public static boolean decreaseTotemParticles() {
      return get.isActived() && get.CustomParticles.getBool() && get.DecreaseTotemParticle.getBool();
   }

   private void updateTranslationCamera() {
      if (this.isActived() && this.CameraTweaks.getBool()) {
         this.pdx = (this.dx + this.lx) / 2.0;
         this.pdy = (this.dy + this.ly) / 2.0;
         this.pdz = (this.dz + this.lz) / 2.0;
         this.lx = Minecraft.player.posX - Minecraft.player.lastTickPosX;
         this.ly = Minecraft.player.posY - Minecraft.player.lastTickPosY;
         this.lz = Minecraft.player.posZ - Minecraft.player.lastTickPosZ;
         this.dx = this.lx;
         this.dy = this.ly;
         this.dz = this.lz;
      }
   }

   public Vec3d getLastTranslated() {
      return this.isActived() && this.CameraTweaks.getBool() && this.lastTranslated != null ? this.lastTranslated : Vec3d.ZERO;
   }

   public void translationCamera(float partialTicks) {
      if (!Panic.stop && this.isActived() && this.CameraTweaks.getBool()) {
         double tx = MathUtils.lerp(this.pdx, this.dx, (double)partialTicks);
         double ty = MathUtils.lerp(this.pdy, this.dy, (double)partialTicks);
         double tz = MathUtils.lerp(this.pdz, this.dz, (double)partialTicks);
         if (Fly.get != null && Fly.get.isActived() && Fly.get.Mode.getMode().equalsIgnoreCase("MatrixBPFlat")) {
            ty = 0.0;
         }

         float mulSmoothTick = 0.55F * Math.min(this.CameraSmoothing.getFloat(), 1.0F);
         tx *= (double)mulSmoothTick;
         ty *= (double)mulSmoothTick;
         tz *= (double)mulSmoothTick;
         this.lastTranslated = new Vec3d(tx, ty, tz);
         GlStateManager.translate(tx, ty, tz);
      }
   }

   public void updateFreeLookRotation(float yawPlus, float pitchPlus, float partialTicks) {
      this.prevCYawOff = this.cYawOff;
      this.prevCPitchOff = this.cPitchOff;
      this.cYawOff += yawPlus;
      this.cPitchOff += pitchPlus;
      if (this.cPitchOff >= 90.0F - Minecraft.player.rotationPitch) {
         this.cPitchOff = 90.0F - Minecraft.player.rotationPitch;
      }

      if (this.cPitchOff <= -90.0F - Minecraft.player.rotationPitch) {
         this.cPitchOff = -90.0F - Minecraft.player.rotationPitch;
      }

      this.offYawOrient = MathUtils.lerp(this.prevCYawOff, this.cYawOff, partialTicks);
      this.offPitchOrient = MathUtils.lerp(this.prevCPitchOff, this.cPitchOff, partialTicks);
   }

   public void updateFreeLookState(boolean enabledModule) {
      if ((!this.FreeLook.getBool() || !enabledModule) && this.freeLookState) {
         this.freeLookState = false;
         mc.gameSettings.thirdPersonView = 0;
      } else {
         if (!this.FreeLook.getBool()) {
            return;
         }

         if (this.actived) {
            this.freeLookState = mc.gameSettings.keyBindTogglePerspective.isKeyDown();
            mc.gameSettings.thirdPersonView = this.freeLookState ? 1 : 0;
         }
      }

      if (this.freeLookState != this.prevFreeLookState) {
         if (this.freeLookState) {
            this.sYaw = Minecraft.player.rotationYaw;
            this.sPitch = Minecraft.player.rotationPitch;
         } else {
            this.sYaw = 0.0F;
            this.sPitch = 0.0F;
            this.cPitchOff = 0.0F;
            this.cYawOff = 0.0F;
            this.offYawOrient = 0.0F;
            this.offPitchOrient = 0.0F;
         }
      }

      if (this.sYaw != 0.0F || this.sPitch != 0.0F) {
         Minecraft.player.rotationYaw = this.sYaw;
         Minecraft.player.rotationPitch = this.sPitch;
      }

      this.prevFreeLookState = this.freeLookState;
   }

   private int getClientSkinsCount() {
      return this.SelfSkin.modes.length;
   }

   public ResourceLocation updatedResourceSkin(ResourceLocation prevResource, Entity entity) {
      if (get != null && this.actived && entity != null && entity instanceof EntityPlayer player && get.ClientPlayersSkins.getBool()) {
         boolean can = false;
         String number = this.SkinsApplyTo.getMode();
         switch (number) {
            case "Self":
               can = player instanceof EntityPlayerSP;
               break;
            case "Friends":
               boolean var14;
               label111: {
                  if (player instanceof EntityOtherPlayerMP mp && !Client.friendManager.isFriend(mp.getName())) {
                     var14 = true;
                     break label111;
                  }

                  var14 = false;
               }

               can = var14;
               break;
            case "Self&Friends":
               boolean var13;
               label130: {
                  label101:
                  if (!(player instanceof EntityPlayerSP)) {
                     if (player instanceof EntityOtherPlayerMP mp && Client.friendManager.isFriend(mp.getName())) {
                        break label101;
                     }

                     var13 = false;
                     break label130;
                  }

                  var13 = true;
               }

               can = var13;
               break;
            case "Players":
               boolean var12;
               label94: {
                  if (player instanceof EntityOtherPlayerMP mp && !Client.friendManager.isFriend(mp.getName())) {
                     var12 = true;
                     break label94;
                  }

                  var12 = false;
               }

               can = var12;
               break;
            case "Self&Players":
               boolean var10000;
               label129: {
                  label87:
                  if (!(player instanceof EntityPlayerSP)) {
                     if (player instanceof EntityOtherPlayerMP mp && !Client.friendManager.isFriend(mp.getName())) {
                        break label87;
                     }

                     var10000 = false;
                     break label129;
                  }

                  var10000 = true;
               }

               can = var10000;
               break;
            case "Players&Friends":
               can = !(player instanceof EntityPlayerSP);
               break;
            case "All":
               can = true;
         }

         if (!can) {
            return prevResource;
         }

         int numberx = (
                  !(player instanceof EntityPlayerSP) && player != FreeCam.fakePlayer && player != FakePlayer.fakePlayer
                     ? player.getEntityId()
                     : Integer.parseInt(this.SelfSkin.currentMode.replace("Skin", "")) - 1
               )
               % this.getClientSkinsCount()
            + 1;
         prevResource = new ResourceLocation("vegaline/modules/worldrender/skins/default/skin" + numberx + ".png");
      }

      return prevResource;
   }

   public float setupedGammaNightVision() {
      float gamma = 0.0F;
      if (!Panic.stop && this.isActived() && this.FullBright.canBeRender() && this.BrightMode.currentMode.equalsIgnoreCase("Vision")) {
         gamma = this.FullBright.getAnimation();
      }

      return gamma;
   }

   public boolean isReverseCamera() {
      return !Panic.stop && get != null && this.actived && this.AltReverseCamera.getBool() && Keyboard.isKeyDown(56) && mc.currentScreen == null;
   }

   public float[] getLastCameraOrients() {
      return get != null && get.isActived() && get.CameraTweaks.getBool() ? this.cameraOrientsPlus : new float[2];
   }

   public float[] orientCustom(float partialTicks) {
      if (!Panic.stop && get != null && get.actived && this.CameraTweaks.getBool()) {
         float smPC = this.CameraSmoothing.getFloat() / 2.0F;
         float cameraSpeed = 0.6F - 0.5F * smPC;
         this.orientYawAnim.speed = cameraSpeed;
         this.orientPitchAnim.speed = cameraSpeed;
         float yaw = MathUtils.lerp(Minecraft.player.prevRotationYaw, Minecraft.player.rotationYaw, Math.abs(partialTicks));
         float pitch = MathUtils.lerp(Minecraft.player.prevRotationPitch, Minecraft.player.rotationPitch, Math.abs(partialTicks));
         this.orientYawAnim.to = yaw;
         this.orientPitchAnim.to = pitch;
         float transYaw = (partialTicks >= 0.0F ? this.orientYawAnim.getAnimAndSetupInfinitySpeed() : this.orientYawAnim.anim) - yaw;
         float transPitch = (partialTicks >= 0.0F ? this.orientPitchAnim.getAnimAndSetupInfinitySpeed() : this.orientPitchAnim.anim) - pitch;
         this.cameraOrientsPlus[0] = transYaw;
         this.cameraOrientsPlus[1] = transPitch;
         return new float[]{transYaw, transPitch};
      } else {
         return new float[]{0.0F, 0.0F};
      }
   }

   public float getClientFovMul(float prevVal, float partialTicks) {
      if (!this.altWorldLoading.hasFinished() && this.SpawnAnimations.getBool()) {
         float progressNoise = this.altWorldLoading.getNoiseProgress();
         float waveProgressNoise = (float)MathUtils.easeInOutQuadWave((double)progressNoise);
         if (this.shakeType == 0) {
            prevVal += 1.0F - progressNoise;
         } else if (this.shakeType == 1) {
            prevVal = (float)((double)prevVal + (1.0 - MathUtils.easeInOutQuad((double)progressNoise)) * 3.0);
         } else if (this.shakeType == 2 || this.shakeType == 3) {
            prevVal = (float)((double)prevVal + MathUtils.easeOutBounce((double)Math.min(waveProgressNoise, 1.0F)));
         }
      }

      if (!Panic.stop && get.isActived() && get.CameraTweaks.getBool() && this.CameraFovRework.getBool()) {
         float fovTo = 0.0F;
         this.fovMultiplier.speed = 0.04F;
         if (Minecraft.player != null) {
            if (mc.gameSettings.thirdPersonView == 1
               && Minecraft.player.isSneaking()
               && MoveMeHelp.getSpeed() == 0.0
               && Minecraft.player.posY == Minecraft.player.lastTickPosY) {
               fovTo = 0.05F;
            } else {
               if (mc.pointedEntity != null) {
                  fovTo += 0.01F;
               }

               if (Minecraft.player.isBowing() || Minecraft.player.isDrinking()) {
                  fovTo += (
                        0.05F
                           + MathUtils.clamp(
                              ((float)Minecraft.player.getItemInUseMaxCount() + partialTicks) / (Minecraft.player.isDrinking() ? 32.0F : 21.0F), 0.0F, 1.0F
                           )
                     )
                     * (Minecraft.player.isDrinking() ? -0.015F : 0.5F);
               }
            }
         }

         this.fovMultiplier.getAnimAndSetupInfinitySpeed();
         this.fovMultiplier.to = fovTo;
         return prevVal + this.fovMultiplier.anim;
      } else {
         if (mc.currentScreen == Client.clickGuiScreen) {
            float animPC = Math.min(ClickGuiScreen.globalAlpha.anim / 255.0F * ClickGuiScreen.scale.anim, 1.0F);
            prevVal -= animPC * 0.25F / 5.0F;
         }

         return prevVal;
      }
   }

   public double cameraRedistance(double prevDistance) {
      if (get != null) {
         get.stateAnim.to = get.isActived() ? 1.0F : 0.0F;
      }

      return !Panic.stop && get != null
         ? MathUtils.lerp(prevDistance, (double)this.CameraRedistance.getAnimation(), (double)(this.CustomCamDist.getAnimation() * get.stateAnim.getAnim()))
         : prevDistance;
   }

   public float weatherReStrengh(float prevStrengh) {
      return !Panic.stop && get != null && get.actived && this.ClearWeather.getBool() ? 0.0F : prevStrengh;
   }

   public float[] getSkyColorRGB(float prevRed, float prevGreen, float prevBlue) {
      Module MOD = get;
      if (!Panic.stop && MOD != null && MOD.actived && this.SkyRecolor.canBeRender()) {
         String mode = this.SkyColorMode.currentMode;
         float renderAnim = this.SkyRecolor.getAnimation();
         float red = prevRed;
         float green = prevGreen;
         float blue = prevBlue;
         switch (mode) {
            case "Colored": {
               int pick1 = this.SkyColorPick.color;
               float aLP = ColorUtils.getGLAlphaFromColor(pick1);
               float[] rgbFloat = new float[]{
                  ColorUtils.getGLRedFromColor(pick1) * aLP, ColorUtils.getGLGreenFromColor(pick1) * aLP, ColorUtils.getGLBlueFromColor(pick1) * aLP
               };
               red = rgbFloat[0];
               green = rgbFloat[1];
               blue = rgbFloat[2];
               break;
            }
            case "Fade": {
               int pick1 = this.SkyColorPick.color;
               int pick2 = this.SkyColorPick2.color;
               float fadeSpeed = this.SkyFadeSpeed.getFloat() * 0.5F;
               int pickFadedColor = ColorUtils.fadeColorIndexed(pick1, pick2, fadeSpeed, 0);
               float aLP = ColorUtils.getGLAlphaFromColor(pickFadedColor);
               float[] rgbFloat = new float[]{
                  ColorUtils.getGLRedFromColor(pickFadedColor) * aLP,
                  ColorUtils.getGLGreenFromColor(pickFadedColor) * aLP,
                  ColorUtils.getGLBlueFromColor(pickFadedColor) * aLP
               };
               red = rgbFloat[0];
               green = rgbFloat[1];
               blue = rgbFloat[2];
               break;
            }
            case "Client": {
               int col = ClientColors.getColor1(0, this.SkyClientColBright.getFloat());
               float aLP = ColorUtils.getGLAlphaFromColor(col);
               float[] rgbFloat = new float[]{
                  ColorUtils.getGLRedFromColor(col) * aLP, ColorUtils.getGLGreenFromColor(col) * aLP, ColorUtils.getGLBlueFromColor(col) * aLP
               };
               red = rgbFloat[0];
               green = rgbFloat[1];
               blue = rgbFloat[2];
               break;
            }
            case "ReBright":
               float bright = MathUtils.clamp(this.SkyBright.getFloat(), 0.0F, 1.0F);
               red = prevRed * bright;
               green = prevGreen * bright;
               blue = prevBlue * bright;
         }

         return new float[]{MathUtils.lerp(prevRed, red, renderAnim), MathUtils.lerp(prevGreen, green, renderAnim), MathUtils.lerp(prevBlue, blue, renderAnim)};
      } else {
         return new float[]{prevRed, prevGreen, prevBlue};
      }
   }

   public float getRedistanceFogValue(float prevMaxDstSq) {
      return !Panic.stop && get.actived && this.FogRedistance.canBeRender()
         ? MathUtils.lerp(prevMaxDstSq, this.FogDistanceCustom.getAnimation(), this.FogRedistance.getAnimation())
         : prevMaxDstSq;
   }

   private long calculateLocalTime() {
      LocalTime currentTime = LocalTime.now();
      int hours = currentTime.getHour();
      int minutes = currentTime.getMinute();
      int seconds = currentTime.getSecond();
      Month currentMonth = LocalDate.now().getMonth();
      double dayMultiplier = this.getDayMultiplierBySeason(currentMonth);
      long baseGameTime = (long)(hours * 1000 + minutes * 1000 / 60 + seconds * 1000 / 3600);
      return (long)((double)baseGameTime * dayMultiplier);
   }

   private double getDayMultiplierBySeason(Month month) {
      int sunriseHour;
      int sunsetHour;
      switch (month) {
         case DECEMBER:
         case JANUARY:
         case FEBRUARY:
            sunriseHour = 8;
            sunsetHour = 16;
            break;
         case MARCH:
         case APRIL:
         case MAY:
            sunriseHour = 6;
            sunsetHour = 19;
            break;
         case JUNE:
         case JULY:
         case AUGUST:
            sunriseHour = 5;
            sunsetHour = 21;
            break;
         case SEPTEMBER:
         case OCTOBER:
         case NOVEMBER:
            sunriseHour = 7;
            sunsetHour = 18;
            break;
         default:
            sunriseHour = 6;
            sunsetHour = 18;
      }

      int dayDuration = sunsetHour - sunriseHour;
      int nightDuration = 24 - dayDuration;
      return (double)dayDuration / 12.0;
   }

   public long getWorldReTime(long oldTime) {
      Module mod = get;
      boolean enabled = mod != null && mod.actived && this.WorldReTime.getBool();
      boolean sataFlag = enabled || MathUtils.getDifferenceOf(this.spinnedTime.anim, (float)(oldTime % 24000L)) > 80.0F;
      if (enabled) {
         String mode = this.Time.currentMode;
         if (mode != null) {
            switch (mode) {
               case "Evening":
                  this.current = 12800.0F;
                  break;
               case "Night":
                  this.current = 18000.0F;
                  break;
               case "Morning":
                  this.current = 23500.0F;
                  break;
               case "Day":
                  this.current = 6000.0F;
                  break;
               case "SpinTime":
                  this.current = (float)(System.currentTimeMillis() % (long)((int)(10000.0F / this.TimeSpinSpeed.getFloat())))
                     / (10000.0F / this.TimeSpinSpeed.getFloat())
                     * 24000.0F;
                  if (MathUtils.getDifferenceOf(this.spinnedTime.anim, this.current) > 23000.0F) {
                     this.spinnedTime.setAnim(this.current * 0.9F);
                  }
                  break;
               case "Custom":
                  this.current = this.TimeCustom.getFloat();
                  break;
               case "RealWorldTime":
                  this.current = (float)this.calculateLocalTime();
            }

            this.spinnedTime.to = this.current;
            this.smoothingTime = true;
         } else {
            this.spinnedTime.to = (float)(oldTime % 24000L);
         }
      } else {
         this.spinnedTime.to = (this.oldTime != -1.2398746E8F ? this.oldTime : (float)oldTime) % 24000.0F;
         if (this.smoothingTime && !sataFlag) {
            this.smoothingTime = false;
         }
      }

      return this.smoothingTime ? (long)this.spinnedTime.getAnimAndSetupInfinitySpeed() : oldTime;
   }

   public int particleReCount(int prevCount) {
      float count = 1.0F;
      if (get != null) {
         Module mod = get;
         if (mod.actived && this.CustomParticles.getBool()) {
            count *= this.ParticleCount.getFloat();
         }
      }

      return (int)((float)prevCount * count);
   }

   public float particleReSpeed(float prevParticleSpeed) {
      float speed = 1.0F;
      if (get != null) {
         Module mod = get;
         if (mod.actived && this.CustomParticles.getBool()) {
            speed *= this.ParticleSpeed.getFloat();
         }
      }

      return prevParticleSpeed * speed;
   }

   public boolean isRenderBloom() {
      return get != null && get.actived && this.WorldBloom.canBeRender()
         ? this.BloomPower.getFloat() > 0.0F && this.BloomPower.getFloat() <= 1.0F
         : FragEffects.get.totemAnimation.anim != 0.0F;
   }

   public void drawWorldBloom() {
      float powerBloom = this.BloomPower.getAnimation() * this.WorldBloom.getAnimation()
         + (float)MathUtils.easeOutCubic((double)FragEffects.get.totemAnimation.anim) / 3.0F;
      GaussianBlur.renderBlur(0.8F - Math.min(powerBloom, 1.0F) * 0.375F, "INFINITY-drawWorldBloom");
   }

   private void updateDeepShadows(boolean actived) {
      boolean usement = actived && this.DeepBlocksShadows.getBool();
      if (this.prevDeepBlocksShadows != usement) {
         this.prevDeepBlocksShadows = usement;
         mc.renderGlobal.loadRenderers();
      }
   }

   public static boolean isDeepShadows() {
      return get != null && get.isActived() && get.DeepBlocksShadows.getBool();
   }

   @EventTarget
   public void onLightingCheck(EventLightingCheck event) {
      if (this.BlockLightFix.getBool()
         && (
            event.getEnumSkyBlock() == EnumSkyBlock.SKY
               || event.getEnumSkyBlock() == EnumSkyBlock.BLOCK && Minecraft.player != null && Minecraft.player.getDistanceToBlockPos(event.getPos()) > 64.0
               || event.getEnumSkyBlock() == EnumSkyBlock.SKY && event.getPos().getY() >= 253
         )) {
         event.cancel();
      }
   }

   @Override
   public void onUpdate() {
      if (this.SpawnAnimations.getBool()
         && Minecraft.player != null
         && Minecraft.player.ticksExisted < 20
         && !this.wantToCustomLoading
         && !this.altWorldLoading.hasFinished()
         && mc.world != null) {
         int tick = this.shakeType == 0 ? 4 : (this.shakeType == 1 ? 4 : (this.shakeType == 2 ? 6 : 7));
         int longestTicks = this.shakeType == 0 ? 4 : (this.shakeType == 1 ? 4 : (this.shakeType == 2 ? 1 : 2));
         if (Minecraft.player.ticksExisted >= tick + 1 && Minecraft.player.ticksExisted <= tick + 1 + longestTicks) {
            this.genPSParticles((int)(980.0F / (float)longestTicks), 4.0F, Minecraft.player.getPositionVector(), 2750L);
         }
      }

      this.updatePSParticlesList();
      this.updateDeepShadows(true);
      this.updateTranslationCamera();
      if (this.CustomViewBobbing.getBool()) {
         if (!mc.gameSettings.viewBobbing) {
            Client.msg("§f§lModules:§r §7[§lWorldRender§r§7]: Для использования CustomViewBobbing был включен ViewBobbing в игре.", false);
            this.viewBobbing = mc.gameSettings.viewBobbing;
            mc.gameSettings.viewBobbing = true;
            this.viewTriggerActCH = true;
         }
      } else if (this.viewTriggerActCH) {
         mc.gameSettings.viewBobbing = this.viewBobbing;
         this.viewTriggerActCH = false;
      }

      this.updateFreeLookState(this.actived);
      this.isItemPhysics = this.ItemPhysics.getBool();
      if (this.ChunksDebuger.getBool() && Minecraft.player.ticksExisted < 7 && Minecraft.player.ticksExisted > 5) {
         mc.renderGlobal.loadRenderers();
      }

      if (Minecraft.player.getActivePotionEffect(Potion.getPotionById(16)) != null
         && Minecraft.player.getActivePotionEffect(Potion.getPotionById(16)).getDuration() >= 16345) {
         Minecraft.player.removeActivePotionEffect(Potion.getPotionById(16));
      }

      if (this.rend != this.RenderBarrier.getBool()) {
         mc.renderGlobal.loadRenderers();
         this.rend = this.RenderBarrier.getBool();
      }

      if (this.FullBright.getBool() && this.BrightMode.currentMode.equalsIgnoreCase("Gamma")) {
         if (mc.gameSettings.gammaSetting != 1000.0F) {
            this.oldGamma = mc.gameSettings.gammaSetting;
         }

         mc.gameSettings.gammaSetting = 1000.0F;
      } else if (this.oldGamma != -1.0F) {
         mc.gameSettings.gammaSetting = this.oldGamma;
         this.oldGamma = -1.0F;
      }
   }

   @Override
   public void onToggled(boolean actived) {
      this.updateDeepShadows(false);
      this.updateFreeLookState(false);
      this.isItemPhysics = false;
      if (this.RenderBarrier.getBool()) {
         mc.renderGlobal.loadRenderers();
      }

      if (this.FullBright.getBool()) {
         if (actived) {
            if (this.BrightMode.currentMode.equalsIgnoreCase("Gamma")) {
               this.oldGamma = mc.gameSettings.gammaSetting;
            }
         } else {
            this.isItemPhysics = false;
            if (this.BrightMode.currentMode.equalsIgnoreCase("Gamma") && this.oldGamma != -1.0F) {
               mc.gameSettings.gammaSetting = this.oldGamma;
               this.oldGamma = -1.0F;
            }
         }
      }

      super.onToggled(actived);
   }

   @EventTarget
   public void onTimeUpdatePacket(EventReceivePacket event) {
      if (event.getPacket() instanceof SPacketTimeUpdate packetTime && this.actived && this.WorldReTime.getBool()) {
         this.oldTime = (float)packetTime.getWorldTime();
      }
   }

   public void worldLoadHookCancel(WorldClient worldClient, Runnable doLoadWorld) {
      if (doLoadWorld != null) {
         doLoadWorld.run();
      } else {
         mc.world = null;
      }

      if (this.isActived() && this.SpawnAnimations.getBool() && worldClient != null) {
         this.wantToCustomLoading = true;

         try {
            if (this.wantToCustomLoading && this.soundDelay.hasReached(700.0)) {
               this.shakeType = this.RANDOM.nextInt(5);
               MusicHelper.playSound("loadworld" + this.shakeType + ".wav", 0.3F * mc.gameSettings.getSoundLevel(SoundCategory.MASTER));
               this.soundDelay.reset();
            }
         } catch (Exception var4) {
            var4.fillInStackTrace();
         }
      }
   }

   public void updateCustomLoadWorld() {
      if (!this.SpawnAnimations.getBool()) {
         if (this.wantToCustomLoading) {
            this.wantToCustomLoading = false;
         }
      } else {
         float speedMul = (this.shakeType == 0 ? 0.65F : (this.shakeType == 1 ? 0.5F : (this.shakeType == 2 ? 0.45F : (this.shakeType == 3 ? 0.425F : 1.0F))))
            * 1.175F;
         this.altWorldLoading.update((this.wantToCustomLoading ? 0.12F : 0.055F) * speedMul, this.wantToCustomLoading);
         ScaledResolution sr = new ScaledResolution(mc);
         this.altWorldLoading
            .insertRender2D(() -> RenderUtils.drawRect(0.0, 0.0, (double)sr.getScaledWidth(), (double)sr.getScaledHeight(), ColorUtils.getColor(0)), sr, 16);
         if (this.altWorldLoading.hasFinished()) {
            this.wantToCustomLoading = false;
         } else if (Minecraft.player != null) {
            mc.timer.tempSpeed = (double)(1.0F - (1.0F - this.altWorldLoading.getNoiseProgress()) / 1.15F);
         }
      }
   }

   private float[] getPsShapesUVMinMax(int indexTexture) {
      float[] uv = new float[]{0.0F, (float)indexTexture / 4.0F, 1.0F, 0.0F};
      uv[3] = uv[1] + 0.25F;
      return uv;
   }

   private int getPsShapeColor(int indexTexture, float toWhite, float alphaPC) {
      int color = this.COLORS_SHAPES[indexTexture];
      if (toWhite != 0.0F) {
         color = ColorUtils.getOverallColorFrom(color, -1, toWhite);
      }

      if (alphaPC != 1.0F) {
         color = ColorUtils.swapAlpha(color, alphaPC * 255.0F);
      }

      return color;
   }

   private void shapeQuadWithOutBegin(float x, float y, float x2, float y2, float[] uv, int color) {
      this.buffer.pos((double)x, (double)y).tex((double)uv[0], (double)uv[1]).color(color).endVertex();
      this.buffer.pos((double)x2, (double)y).tex((double)uv[2], (double)uv[1]).color(color).endVertex();
      this.buffer.pos((double)x2, (double)y2).tex((double)uv[2], (double)uv[3]).color(color).endVertex();
      this.buffer.pos((double)x, (double)y2).tex((double)uv[0], (double)uv[3]).color(color).endVertex();
   }

   private void updatePSParticlesList() {
      if (!this.PS_PARTICLES_LIST.isEmpty()) {
         try {
            this.PS_PARTICLES_LIST.removeIf(WorldRender.PSParticle::isToRemove);
            if (this.PS_PARTICLES_LIST.isEmpty()) {
               return;
            }

            this.PS_PARTICLES_LIST.forEach(WorldRender.PSParticle::update);
         } catch (Exception var2) {
            var2.printStackTrace();
         }
      }
   }

   private void renderPSParticles(float partialTicks, float scale) {
      if (!this.PS_PARTICLES_LIST.isEmpty()) {
         List<WorldRender.PSParticle> psParts = this.PS_PARTICLES_LIST.stream().filter(psPart -> !psPart.isToRemove()).toList();
         if (!psParts.isEmpty()) {
            double glX = RenderManager.viewerPosX;
            double glY = RenderManager.viewerPosY;
            double glZ = RenderManager.viewerPosZ;
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            mc.entityRenderer.disableLightmap();
            GL11.glAlphaFunc(516, 0.003921569F);
            GL11.glLineWidth(1.0F);
            GL11.glEnable(3553);
            GL11.glDisable(2896);
            GL11.glShadeModel(7425);
            GL11.glDisable(3008);
            GL11.glDisable(2884);
            GL11.glDepthMask(false);
            GL11.glTranslated(-glX, -glY, -glZ);
            GL11.glTexParameteri(3553, 10240, 9728);
            mc.getTextureManager().bindTexture(this.PS_SHAPES_TEXTURE);
            psParts.forEach(psPart -> psPart.draw(partialTicks, scale, 1.0F));
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTranslated(glX, glY, glZ);
            GL11.glDepthMask(true);
            GL11.glEnable(2884);
            GL11.glAlphaFunc(516, 0.1F);
            GL11.glLineWidth(1.0F);
            GL11.glShadeModel(7424);
            GL11.glEnable(3553);
            GlStateManager.resetColor();
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            GL11.glPopMatrix();
         }
      }
   }

   private void genPSParticles(int crateSount, float maxDistance, Vec3d ofPos, long lifeTime) {
      try {
         for (int counter = 0; counter < crateSount; counter++) {
            float dst = maxDistance * this.RANDOM.nextFloat(1.0F);
            float yaw = this.RANDOM.nextFloat() * 360.0F;
            float pitch = this.RANDOM.nextFloat(-90.0F, 0.0F);
            float xOff = MathHelper.sin(MathHelper.toRadians(yaw)) * dst;
            float zOff = -MathHelper.cos(MathHelper.toRadians(yaw)) * dst;
            float yOff = MathHelper.sin(MathHelper.toRadians(pitch)) * dst;
            Vec3d partPos = ofPos.addVector((double)xOff, (double)yOff, (double)zOff);
            this.PS_PARTICLES_LIST.add(new WorldRender.PSParticle(partPos, (long)((float)lifeTime * this.RANDOM.nextFloat(0.5F, 1.5F))));
         }
      } catch (Exception var14) {
         var14.printStackTrace();
      }
   }

   @Override
   public void alwaysRender3D(float partialTicks) {
      if (this.isActived()) {
         this.renderPSParticles(partialTicks, 2.5F);
      }
   }

   @EventTarget
   public void onPacketReceive(EventReceivePacket event) {
      if (this.actived && this.SpawnAnimations.getBool()) {
         if (event.getPacket() instanceof SPacketSoundEffect effectSound
            && Minecraft.player != null
            && Minecraft.player.ticksExisted <= 15
            && (
               effectSound.getCategory() != SoundCategory.PLAYERS && effectSound.getCategory() != SoundCategory.BLOCKS
                  || effectSound.getCategory() != SoundCategory.WEATHER
            )) {
            event.cancel();
         }

         if (event.getPacket() instanceof SPacketPlayerPosLook lookPacket && Minecraft.player != null) {
            double dstXZ = Minecraft.player.getDistance(lookPacket.getX(), Minecraft.player.posY, lookPacket.getZ());
            if (dstXZ > 256.0 && Minecraft.player.getSpeed() < dstXZ / 5.0) {
               this.wantToCustomLoading = true;
               this.altWorldLoading.update(this.wantToCustomLoading ? 0.2F : 0.06F, this.wantToCustomLoading);
            }
         }
      }
   }

   private class PSParticle {
      private double xPos;
      private double yPos;
      private double zPos;
      private double prevXPos;
      private double prevYPos;
      private double prevZPos;
      private double motionX = (double)WorldRender.this.RANDOM.nextFloat(-0.5F, 0.5F);
      private double motionY = (double)WorldRender.this.RANDOM.nextFloat(0.7F);
      private double motionZ = (double)WorldRender.this.RANDOM.nextFloat(-0.5F, 0.5F);
      private final long spawnTime = System.currentTimeMillis();
      private final long maxTime;
      private final byte variant = (byte)WorldRender.this.RANDOM.nextInt(4);
      private final float[] uv = WorldRender.this.getPsShapesUVMinMax(this.variant);

      public PSParticle(Vec3d pos, long maxTime) {
         this.xPos = pos.xCoord;
         this.yPos = pos.yCoord;
         this.zPos = pos.zCoord;
         this.prevXPos = pos.xCoord;
         this.prevYPos = pos.yCoord;
         this.prevZPos = pos.zCoord;
         this.maxTime = maxTime;
      }

      public float getTimePC() {
         return (float)Math.min(System.currentTimeMillis() - this.spawnTime, this.maxTime) / (float)this.maxTime;
      }

      public boolean isToRemove() {
         return this.getTimePC() == 1.0F;
      }

      public void update() {
         this.prevXPos = this.xPos;
         this.prevYPos = this.yPos;
         this.prevZPos = this.zPos;
         this.xPos = this.xPos + this.motionX;
         this.yPos = this.yPos + this.motionY;
         this.zPos = this.zPos + this.motionZ;
         if (Module.mc.world != null) {
            BlockPos bposX = new BlockPos(this.xPos + this.motionX, this.yPos + this.motionY, this.zPos);
            BlockPos bposY = new BlockPos(this.xPos, this.yPos + this.motionY, this.zPos);
            BlockPos bposZ = new BlockPos(this.xPos, this.yPos + this.motionY, this.zPos + this.motionZ);
            IBlockState stX = Module.mc.world.getBlockState(bposX);
            IBlockState stY = Module.mc.world.getBlockState(bposY);
            IBlockState stZ = Module.mc.world.getBlockState(bposZ);
            if (stX.getMaterial().blocksMovement()) {
               this.motionX *= -0.5;
            }

            if (stY.getMaterial().blocksMovement()) {
               this.motionY *= -0.6F;
               this.motionX *= 0.9F;
               this.motionZ *= 0.9F;
            }

            if (stZ.getMaterial().blocksMovement()) {
               this.motionZ *= -0.5;
            }
         }

         this.motionX *= 0.99F;
         this.motionY *= 0.97F;
         this.motionY -= 0.02F;
         this.motionZ *= 0.99F;
      }

      public double getXPos(float partialTicks) {
         return this.prevXPos + (this.xPos - this.prevXPos) * (double)partialTicks;
      }

      public double getYPos(float partialTicks) {
         return this.prevYPos + (this.yPos - this.prevYPos) * (double)partialTicks;
      }

      public double getZPos(float partialTicks) {
         return this.prevZPos + (this.zPos - this.prevZPos) * (double)partialTicks;
      }

      public void draw(float partialTicks, float scale, float alphaPC) {
         alphaPC *= Math.min((1.0F - this.getTimePC()) * 2.0F, 1.0F);
         if (!(alphaPC * 255.0F < 1.0F)) {
            scale *= alphaPC;
            float toWhite = 1.0F - Math.min(this.getTimePC() * 1.5F, 1.0F);
            toWhite = (float)MathUtils.easeInOutQuad((double)toWhite);
            toWhite *= toWhite;
            int color = WorldRender.this.getPsShapeColor(this.variant, toWhite, alphaPC);
            GL11.glPushMatrix();
            GL11.glTranslated(this.getXPos(partialTicks), this.getYPos(partialTicks), this.getZPos(partialTicks));
            GL11.glRotated((double)(Module.mc.getRenderManager().playerViewY + WorldRender.get.offYawOrient), 0.0, -1.0, 0.0);
            GL11.glRotated(
               (double)(Module.mc.getRenderManager().playerViewX + WorldRender.get.offPitchOrient),
               Module.mc.gameSettings.thirdPersonView == 2 ? -1.0 : 1.0,
               0.0,
               0.0
            );
            GL11.glScaled(-0.1F, -0.1F, 0.1F);
            WorldRender.this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            WorldRender.this.shapeQuadWithOutBegin(-scale / 2.0F, -scale / 2.0F, scale / 2.0F, scale / 2.0F, this.uv, color);
            WorldRender.this.tessellator.draw();
            GL11.glPopMatrix();
         }
      }
   }
}
