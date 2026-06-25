package net.minecraft.client.renderer;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ICrashReportDetail;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MouseFilter;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import optifine.Config;
import optifine.CustomColors;
import optifine.Lagometer;
import optifine.RandomMobs;
import optifine.Reflector;
import optifine.ReflectorForge;
import optifine.TextureUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Project;
import ru.govno.client.Client;
import ru.govno.client.clickgui.TriangleGroup;
import ru.govno.client.event.events.Event3D;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.BackTrack;
import ru.govno.client.module.modules.BadTrip;
import ru.govno.client.module.modules.Bypass;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.module.modules.ESP;
import ru.govno.client.module.modules.EntityBox;
import ru.govno.client.module.modules.FragEffects;
import ru.govno.client.module.modules.NoRender;
import ru.govno.client.module.modules.PlayerHelper;
import ru.govno.client.module.modules.WorldRender;
import ru.govno.client.module.modules.Xray;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.DisplayCheck;
import ru.govno.client.utils.UProfiler;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import shadersmod.client.Shaders;
import shadersmod.client.ShadersRender;

public class EntityRenderer implements IResourceManagerReloadListener {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final ResourceLocation RAIN_TEXTURES = new ResourceLocation("textures/environment/rain.png");
   private static final ResourceLocation SNOW_TEXTURES = new ResourceLocation("textures/environment/snow.png");
   public static boolean anaglyphEnable;
   public static int anaglyphField;
   private final Minecraft mc;
   public final IResourceManager resourceManager;
   private final Random random = new Random();
   private float farPlaneDistance;
   public ItemRenderer itemRenderer;
   private final MapItemRenderer theMapItemRenderer;
   private int rendererUpdateCount;
   private Entity pointedEntity;
   private MouseFilter mouseFilterXAxis = new MouseFilter();
   private MouseFilter mouseFilterYAxis = new MouseFilter();
   private final float thirdPersonDistance = 4.0F;
   private float thirdPersonDistancePrev = 4.0F;
   private float smoothCamYaw;
   private float smoothCamPitch;
   private float smoothCamFilterX;
   private float smoothCamFilterY;
   private float smoothCamPartialTicks;
   private float fovModifierHand;
   private float fovModifierHandPrev;
   private float bossColorModifier;
   private float bossColorModifierPrev;
   private boolean cloudFog;
   private final boolean renderHand = true;
   private final boolean drawBlockOutline = true;
   private long timeWorldIcon;
   private long prevFrameTime = Minecraft.getSystemTime();
   private long renderEndNanoTime;
   private final DynamicTexture lightmapTexture;
   private final int[] lightmapColors;
   private final ResourceLocation locationLightMap;
   private boolean lightmapUpdateNeeded;
   private float torchFlickerX;
   private float torchFlickerDX;
   private int rainSoundCounter;
   private final float[] rainXCoords = new float[1024];
   private final float[] rainYCoords = new float[1024];
   private final FloatBuffer fogColorBuffer = GLAllocation.createDirectFloatBuffer(16);
   public float fogColorRed;
   public float fogColorGreen;
   public float fogColorBlue;
   private float fogColor2;
   private float fogColor1;
   private int debugViewDirection;
   private boolean debugView;
   private final double cameraZoom = 1.0;
   private double cameraYaw;
   private double cameraPitch;
   private ItemStack field_190566_ab;
   private int field_190567_ac;
   private float field_190568_ad;
   private float field_190569_ae;
   public ShaderGroup theShaderGroup;
   private static final ResourceLocation[] SHADERS_TEXTURES = new ResourceLocation[]{
      new ResourceLocation("shaders/post/notch.json"),
      new ResourceLocation("shaders/post/fxaa.json"),
      new ResourceLocation("shaders/post/art.json"),
      new ResourceLocation("shaders/post/bumpy.json"),
      new ResourceLocation("shaders/post/blobs2.json"),
      new ResourceLocation("shaders/post/pencil.json"),
      new ResourceLocation("shaders/post/color_convolve.json"),
      new ResourceLocation("shaders/post/deconverge.json"),
      new ResourceLocation("shaders/post/flip.json"),
      new ResourceLocation("shaders/post/invert.json"),
      new ResourceLocation("shaders/post/ntsc.json"),
      new ResourceLocation("shaders/post/outline.json"),
      new ResourceLocation("shaders/post/phosphor.json"),
      new ResourceLocation("shaders/post/scan_pincushion.json"),
      new ResourceLocation("shaders/post/sobel.json"),
      new ResourceLocation("shaders/post/bits.json"),
      new ResourceLocation("shaders/post/desaturate.json"),
      new ResourceLocation("shaders/post/green.json"),
      new ResourceLocation("shaders/post/blur.json"),
      new ResourceLocation("shaders/post/wobble.json"),
      new ResourceLocation("shaders/post/blobs.json"),
      new ResourceLocation("shaders/post/antialias.json"),
      new ResourceLocation("shaders/post/creeper.json"),
      new ResourceLocation("shaders/post/spider.json")
   };
   public static final int SHADER_COUNT = SHADERS_TEXTURES.length;
   private int shaderIndex;
   private boolean useShader;
   public int frameCount;
   private boolean initialized = false;
   private World updatedWorld = null;
   public boolean fogStandard = false;
   private float clipDistance = 128.0F;
   private long lastServerTime = 0L;
   private int lastServerTicks = 0;
   private int serverWaitTime = 0;
   private int serverWaitTimeCurrent = 0;
   private float avgServerTimeDiff = 0.0F;
   private float avgServerTickDiff = 0.0F;
   private long lastErrorCheckTimeMs = 0L;
   private final ShaderGroup[] fxaaShaders = new ShaderGroup[10];
   private boolean loadVisibleChunks = false;
   AnimationUtils zoomer = new AnimationUtils(1.0F, 1.0F, 0.05F);
   AnimationUtils whell = new AnimationUtils(0.0F, 0.0F, 0.05F);
   AnimationUtils e = new AnimationUtils(0.0F, 0.0F, 0.1F);
   private TriangleGroup targetESPElement;
   public List<EntityRenderer.RunnableRenderTile> listTileOutlines = Lists.newArrayList();
   AnimationUtils cfgPC = new AnimationUtils(0.0F, 0.0F, 0.1F);
   TimerHelper cfgLinearTime = new TimerHelper();

   public EntityRenderer(Minecraft mcIn, IResourceManager resourceManagerIn) {
      this.shaderIndex = SHADER_COUNT;
      this.mc = mcIn;
      this.resourceManager = resourceManagerIn;
      this.itemRenderer = mcIn.getItemRenderer();
      this.theMapItemRenderer = new MapItemRenderer(mcIn.getTextureManager());
      this.lightmapTexture = new DynamicTexture(16, 16);
      this.locationLightMap = mcIn.getTextureManager().getDynamicTextureLocation("lightMap", this.lightmapTexture);
      this.lightmapColors = this.lightmapTexture.getTextureData();
      this.theShaderGroup = null;

      for (int i = 0; i < 32; i++) {
         for (int j = 0; j < 32; j++) {
            float f = (float)(j - 16);
            float f1 = (float)(i - 16);
            float f2 = MathHelper.sqrt(f * f + f1 * f1);
            this.rainXCoords[i << 5 | j] = -f1 / f2;
            this.rainYCoords[i << 5 | j] = f / f2;
         }
      }
   }

   public boolean isShaderActive() {
      return OpenGlHelper.shadersSupported && this.theShaderGroup != null;
   }

   public void stopUseShader() {
      if (this.theShaderGroup != null) {
         this.theShaderGroup.deleteShaderGroup();
      }

      this.theShaderGroup = null;
      this.shaderIndex = SHADER_COUNT;
   }

   public void switchUseShader() {
      this.useShader = !this.useShader;
   }

   public void loadEntityShader(@Nullable Entity entityIn) {
      if (OpenGlHelper.shadersSupported) {
         if (this.theShaderGroup != null) {
            this.theShaderGroup.deleteShaderGroup();
         }

         this.theShaderGroup = null;
         if (entityIn instanceof EntityCreeper) {
            this.loadShader(new ResourceLocation("shaders/post/creeper.json"));
         } else if (entityIn instanceof EntitySpider) {
            this.loadShader(new ResourceLocation("shaders/post/spider.json"));
         } else if (entityIn instanceof EntityEnderman) {
            this.loadShader(new ResourceLocation("shaders/post/invert.json"));
         } else if (Reflector.ForgeHooksClient_loadEntityShader.exists()) {
            Reflector.call(Reflector.ForgeHooksClient_loadEntityShader, entityIn, this);
         }
      }
   }

   public void loadShader(ResourceLocation resourceLocationIn) {
      if (OpenGlHelper.isFramebufferEnabled()) {
         try {
            this.theShaderGroup = new ShaderGroup(this.mc.getTextureManager(), this.resourceManager, this.mc.getFramebuffer(), resourceLocationIn);
            this.theShaderGroup.createBindFramebuffers(this.mc.displayWidth, this.mc.displayHeight);
            this.useShader = true;
         } catch (IOException var3) {
            LOGGER.warn("Failed to load shader: {}", resourceLocationIn, var3);
            this.shaderIndex = SHADER_COUNT;
            this.useShader = false;
         } catch (JsonSyntaxException var4) {
            LOGGER.warn("Failed to load shader: {}", resourceLocationIn, var4);
            this.shaderIndex = SHADER_COUNT;
            this.useShader = false;
         }
      }
   }

   @Override
   public void onResourceManagerReload(IResourceManager resourceManager) {
      if (this.theShaderGroup != null) {
         this.theShaderGroup.deleteShaderGroup();
      }

      this.theShaderGroup = null;
      if (this.shaderIndex == SHADER_COUNT) {
         this.loadEntityShader(this.mc.getRenderViewEntity());
      } else {
         this.loadShader(SHADERS_TEXTURES[this.shaderIndex]);
      }
   }

   public void updateRenderer() {
      if (OpenGlHelper.shadersSupported && ShaderLinkHelper.getStaticShaderLinkHelper() == null) {
         ShaderLinkHelper.setNewStaticShaderLinkHelper();
      }

      this.updateFovModifierHand();
      this.updateTorchFlicker();
      this.fogColor2 = this.fogColor1;
      this.thirdPersonDistancePrev = 4.0F;
      if (this.mc.gameSettings.smoothCamera) {
         float f = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
         float f1 = f * f * f * 8.0F;
         this.smoothCamFilterX = this.mouseFilterXAxis.smooth(this.smoothCamYaw, 0.05F * f1);
         this.smoothCamFilterY = this.mouseFilterYAxis.smooth(this.smoothCamPitch, 0.05F * f1);
         this.smoothCamPartialTicks = 0.0F;
         this.smoothCamYaw = 0.0F;
         this.smoothCamPitch = 0.0F;
      } else {
         this.smoothCamFilterX = 0.0F;
         this.smoothCamFilterY = 0.0F;
         this.mouseFilterXAxis.reset();
         this.mouseFilterYAxis.reset();
      }

      if (this.mc.getRenderViewEntity() == null) {
         this.mc.setRenderViewEntity(Minecraft.player);
      }

      Entity entity = this.mc.getRenderViewEntity();
      double d2 = entity.posX;
      double d0 = entity.posY + (double)entity.getEyeHeight();
      double d1 = entity.posZ;
      float f2 = this.mc.world.getLightBrightness(new BlockPos(d2, d0, d1));
      float f3 = MathHelper.clamp((float)this.mc.gameSettings.renderDistanceChunks / 16.0F, 0.0F, 1.0F);
      float f4 = f2 * (1.0F - f3) + f3;
      this.fogColor1 = this.fogColor1 + (f4 - this.fogColor1) * 0.1F;
      this.rendererUpdateCount++;
      this.itemRenderer.updateEquippedItem();
      this.addRainParticles();
      this.bossColorModifierPrev = this.bossColorModifier;
      if (this.mc.ingameGUI.getBossOverlay().shouldDarkenSky()) {
         this.bossColorModifier += 0.05F;
         if (this.bossColorModifier > 1.0F) {
            this.bossColorModifier = 1.0F;
         }
      } else if (this.bossColorModifier > 0.0F) {
         this.bossColorModifier -= 0.0125F;
      }

      if (this.field_190567_ac > 0) {
         this.field_190567_ac--;
         if (this.field_190567_ac == 0) {
            this.field_190566_ab = null;
         }
      }
   }

   public ShaderGroup getShaderGroup() {
      return this.theShaderGroup;
   }

   public void updateShaderGroupSize(int width, int height) {
      if (OpenGlHelper.shadersSupported) {
         if (this.theShaderGroup != null) {
            this.theShaderGroup.createBindFramebuffers(width, height);
         }

         this.mc.renderGlobal.createBindEntityOutlineFbs(width, height);
      }
   }

   public void getMouseOver(float partialTicks) {
      Entity entity = this.mc.getRenderViewEntity();
      if (entity != null && this.mc.world != null) {
         this.mc.mcProfiler.startSection("pick");
         this.mc.pointedEntity = null;
         double d0 = (double)this.mc.playerController.getBlockReachDistance();
         this.mc.objectMouseOver = entity.rayTrace(d0, partialTicks);
         Vec3d vec3d = entity.getPositionEyes(partialTicks);
         boolean flag = false;
         int i = 3;
         double d1 = d0;
         if (this.mc.playerController.extendedReach()) {
            d1 = 6.0;
            d0 = d1;
         } else if (d0 > 3.0) {
            flag = true;
         }

         if (this.mc.objectMouseOver != null) {
            d1 = this.mc.objectMouseOver.hitVec.distanceTo(vec3d);
         }

         Vec3d vec3d1 = entity.getLook(1.0F);
         Vec3d vec3d2 = vec3d.addVector(vec3d1.xCoord * d0, vec3d1.yCoord * d0, vec3d1.zCoord * d0);
         this.pointedEntity = null;
         Vec3d vec3d3 = null;
         float f = 1.0F;
         List<Entity> list = this.mc
            .world
            .getEntitiesInAABBexcluding(
               entity,
               entity.getEntityBoundingBoxCL().addCoord(vec3d1.xCoord * d0, vec3d1.yCoord * d0, vec3d1.zCoord * d0).expand(1.0, 1.0, 1.0),
               Predicates.and(EntitySelectors.NOT_SPECTATING, new Predicate<Entity>() {
                  public boolean apply(@Nullable Entity p_apply_1_) {
                     return p_apply_1_ != null && p_apply_1_.canBeCollidedWith();
                  }
               })
            )
            .stream()
            .sorted(Comparator.comparing(a -> -entity.getDistanceToEntity(a)))
            .toList();
         double d2 = d1;

         for (int j = 0; j < list.size(); j++) {
            Entity entity1 = list.get(j);

            for (AxisAlignedBB axisalignedbb : BackTrack.get
               .getTracksAsEntity(entity1, entity1.getEntityBoundingBoxCL().expandXyz((double)entity1.getCollisionBorderSize()), true)) {
               RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);
               if (!axisalignedbb.isVecInside(vec3d)) {
                  if (raytraceresult != null) {
                     double d3 = vec3d.distanceTo(raytraceresult.hitVec);
                     if (d3 < d2 || d2 == 0.0) {
                        boolean flag1 = false;
                        if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                           flag1 = Reflector.callBoolean(entity1, Reflector.ForgeEntity_canRiderInteract);
                        }

                        if (flag1 || entity1.getLowestRidingEntity() != entity.getLowestRidingEntity()) {
                           this.pointedEntity = entity1;
                           vec3d3 = raytraceresult.hitVec;
                           d2 = d3;
                           break;
                        }

                        if (d2 == 0.0) {
                           this.pointedEntity = entity1;
                           vec3d3 = raytraceresult.hitVec;
                           break;
                        }
                     }
                  }
               } else if (d2 >= 0.0) {
                  this.pointedEntity = entity1;
                  vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
                  d2 = 0.0;
                  break;
               }
            }
         }

         if (this.pointedEntity != null
            && flag
            && vec3d.distanceTo(vec3d3) > (double)(3.0F + (EntityBox.hitboxModState() ? EntityBox.hitboxModReachEntities() : 0.0F))) {
            this.pointedEntity = null;
            this.mc.objectMouseOver = new RayTraceResult(RayTraceResult.Type.MISS, vec3d3, null, new BlockPos(vec3d3));
         }

         if (this.pointedEntity != null && (d2 < d1 || this.mc.objectMouseOver == null)) {
            this.mc.objectMouseOver = new RayTraceResult(this.pointedEntity, vec3d3);
            if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
               this.mc.pointedEntity = this.pointedEntity;
            }
         }

         this.mc.mcProfiler.endSection();
      }
   }

   private void updateFovModifierHand() {
      float f = 1.0F;
      if (this.mc.getRenderViewEntity() instanceof AbstractClientPlayer abstractclientplayer) {
         f = abstractclientplayer.getFovModifier();
      }

      this.fovModifierHandPrev = this.fovModifierHand;
      this.fovModifierHand = this.fovModifierHand + (f - this.fovModifierHand) * 0.5F;
      if (this.fovModifierHand > 1.5F) {
         this.fovModifierHand = 1.5F;
      }

      if (this.fovModifierHand < 0.1F) {
         this.fovModifierHand = 0.1F;
      }
   }

   private void whelling(boolean reset) {
      if (reset) {
         this.whell.to = 0.0F;
         this.whell.speed = 0.2F;
      } else if (Mouse.hasWheel()) {
         this.whell.to = this.whell.to + (float)Mouse.getDWheel() / 40.0F;
         this.whell.speed = this.whell.to <= 0.0F ? 0.1F : 0.05F;
         if (this.whell.to < 0.0F) {
            this.whell.to = 0.0F;
         }
      }
   }

   private float getWhell() {
      return this.whell.getAnim();
   }

   private float getZooming() {
      float zoom = WorldRender.get.getClientFovMul(this.zoomer.getAnim() + FragEffects.get.getStrikeEffectFovModifyPC(), this.mc.getRenderPartialTicks());
      float foving = Panic.stop ? zoom : zoom + this.getWhell();
      return foving < 0.75F ? 0.75F : foving;
   }

   private void zoomTo(float to) {
      this.zoomer.to = to;
      if (Panic.stop) {
         this.zoomer.setAnim(to);
      }
   }

   private void zoomSetSpeed(float speedCurrent) {
      this.zoomer.speed = speedCurrent;
   }

   private float getFOVModifier(float partialTicks, boolean useFOVSetting) {
      if (this.debugView) {
         return 90.0F;
      } else {
         Entity entity = this.mc.getRenderViewEntity();
         float f = 70.0F;
         if (useFOVSetting) {
            f = this.mc.gameSettings.fovSetting;
            if (Config.isDynamicFov()) {
               f *= this.fovModifierHandPrev + (this.fovModifierHand - this.fovModifierHandPrev) * partialTicks;
            }
         }

         boolean flag = false;
         if (this.mc.currentScreen == null) {
            flag = GameSettings.isKeyDown(this.mc.gameSettings.ofKeyBindZoom);
         }

         this.whelling(Panic.stop || !flag);
         if (flag) {
            if (!Config.zoomMode) {
               Config.zoomMode = true;
               if (Panic.stop) {
                  this.mc.gameSettings.smoothCamera = true;
               }

               this.mc.renderGlobal.displayListEntitiesDirty = true;
            }

            this.zoomTo(5.0F);
            this.zoomSetSpeed(0.04F);
         } else {
            this.zoomSetSpeed(0.1F);
            this.zoomTo(1.0F);
            if (Config.zoomMode) {
               Config.zoomMode = false;
               this.mc.gameSettings.smoothCamera = false;
               this.mouseFilterXAxis = new MouseFilter();
               this.mouseFilterYAxis = new MouseFilter();
               this.mc.renderGlobal.displayListEntitiesDirty = true;
            }
         }

         f /= this.getZooming();
         if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).getHealth() <= 0.0F) {
            float f1 = (float)((EntityLivingBase)entity).deathTime + partialTicks;
            f /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
         }

         IBlockState iblockstate = ActiveRenderInfo.getBlockStateAtEntityViewpoint(this.mc.world, entity, partialTicks);
         if (iblockstate.getMaterial() == Material.WATER) {
            f = f * 60.0F / 70.0F;
         }

         return f;
      }
   }

   private void hurtCameraEffect(float partialTicks) {
      if (!NoRender.get.actived || !NoRender.get.HurtCam.getBool()) {
         if (this.mc.getRenderViewEntity() instanceof EntityLivingBase entitylivingbase) {
            float f = (float)entitylivingbase.hurtTime / 1.5F - partialTicks;
            if (entitylivingbase.getHealth() <= 0.0F) {
               float f1 = (float)entitylivingbase.deathTime + partialTicks;
               GlStateManager.rotate(40.0F - 8000.0F / (f1 + 200.0F), 0.0F, 0.0F, 1.0F);
            }

            if (f < 0.0F) {
               return;
            }

            f /= (float)entitylivingbase.maxHurtTime;
            f = MathHelper.sin(f * f * f * f * (float) Math.PI);
            float f2 = entitylivingbase.attackedAtYaw;
            GlStateManager.rotate(-f2, 0.0F, 1.0F, 0.0F);
            if (Minecraft.player.ticksExisted % 2 == 0) {
               GlStateManager.rotate(-f * 2.0F, 0.0F, 0.0F, 1.0F);
            } else {
               GlStateManager.rotate(f * 2.0F, 0.0F, 0.0F, 1.0F);
            }

            GlStateManager.rotate(f2, 0.0F, 1.0F, 0.0F);
         }
      }
   }

   private void setupViewBobbing(float partialTicks) {
      if (this.mc.getRenderViewEntity() instanceof EntityPlayer entityplayer) {
         float f = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
         float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
         float f2 = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
         float f3 = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
         if (!Panic.stop && WorldRender.get.isActived() && WorldRender.get.CustomViewBobbing.getBool()) {
            float mulF3 = 1.0F;
            float mulF1 = 1.0F;
            String var9 = WorldRender.get.ViewShakingType.getMode();
            switch (var9) {
               case "Increased":
                  mulF3 = 2.332F;
                  break;
               case "StableCamera":
                  mulF3 -= 2.0F;
                  mulF1 = 0.75F;
                  break;
               case "SpeedLike":
                  mulF3 = -1.24F;
                  mulF1 = (float)(2.2F * this.mc.timer.speed * this.mc.timer.tempSpeed);
                  break;
               case "Agressive":
                  mulF3 = 3.0F;
                  mulF1 = (float)(1.3F * this.mc.timer.speed * this.mc.timer.tempSpeed);
            }

            f1 *= mulF1;
            f3 *= mulF3;
         }

         GlStateManager.translate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0F);
         GlStateManager.rotate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F, 0.0F, 0.0F, 1.0F);
         GlStateManager.rotate(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F, 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate(f3, 1.0F, 0.0F, 0.0F);
      }
   }

   public void orientCamera(float partialTicks) {
      Entity entity = this.mc.getRenderViewEntity();
      float f = entity.getEyeHeight();
      double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTicks;
      double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTicks + (double)f;
      double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTicks;
      boolean freelook = !Panic.stop && WorldRender.get.isActived() && WorldRender.get.freeLookState;
      WorldRender.get.updateFreeLookState(WorldRender.get.isActived());
      float yawPlus = 0.0F;
      float pitchPlus = 0.0F;
      float smoothYaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
      float smoothPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
      if (freelook) {
         yawPlus += WorldRender.get.offYawOrient;
         pitchPlus += WorldRender.get.offPitchOrient;
         smoothYaw += yawPlus;
         smoothPitch += pitchPlus;
      }

      float[] valCameraEdits = WorldRender.get.orientCustom(partialTicks);
      if (MathUtils.getDifferenceOf(valCameraEdits[0], 0.0F) > 0.002F || MathUtils.getDifferenceOf(valCameraEdits[1], 0.0F) > 0.002F) {
         yawPlus += valCameraEdits[0];
         smoothYaw += valCameraEdits[0];
         pitchPlus += valCameraEdits[1];
         smoothPitch += valCameraEdits[1];
      }

      if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPlayerSleeping()) {
         f = (float)((double)f + 1.0);
         GlStateManager.translate(0.0F, 0.3F, 0.0F);
         if (!this.mc.gameSettings.debugCamEnable) {
            BlockPos blockpos = new BlockPos(entity);
            IBlockState iblockstate = this.mc.world.getBlockState(blockpos);
            Block block = iblockstate.getBlock();
            if (Reflector.ForgeHooksClient_orientBedCamera.exists()) {
               Reflector.callVoid(Reflector.ForgeHooksClient_orientBedCamera, this.mc.world, blockpos, iblockstate, entity);
            } else if (block == Blocks.BED) {
               int j = iblockstate.getValue(BlockBed.FACING).getHorizontalIndex();
               GlStateManager.rotate((float)(j * 90), 0.0F, 1.0F, 0.0F);
            }

            GlStateManager.rotate(smoothYaw + 180.0F, 0.0F, -1.0F, 0.0F);
            GlStateManager.rotate(smoothPitch * partialTicks, -1.0F, 0.0F, 0.0F);
         }
      } else if (this.mc.gameSettings.thirdPersonView > 0) {
         double dist = WorldRender.get.cameraRedistance(Minecraft.player.isChild() ? 2.25 : 4.0);
         double d3 = dist;
         if (this.mc.gameSettings.debugCamEnable) {
            GlStateManager.translate(0.0F, 0.0F, (float)(-dist));
         } else {
            float f1 = entity.rotationYaw + yawPlus;
            float f2 = entity.rotationPitch + pitchPlus;
            if (this.mc.gameSettings.thirdPersonView == 2) {
               f2 += 180.0F;
            }

            double d4 = (double)(-MathHelper.sin(f1 * (float) (Math.PI / 180.0)) * MathHelper.cos(f2 * (float) (Math.PI / 180.0))) * dist;
            double d5 = (double)(MathHelper.cos(f1 * (float) (Math.PI / 180.0)) * MathHelper.cos(f2 * (float) (Math.PI / 180.0))) * dist;
            double d6 = (double)(-MathHelper.sin(f2 * (float) (Math.PI / 180.0))) * dist;
            boolean noCol = NoRender.get.actived && NoRender.get.CameraCollide.getBool();
            if (dist != 0.0) {
               for (int i = 0; i < 8; i++) {
                  float f3 = (float)((i & 1) * 2 - 1);
                  float f4 = (float)((i >> 1 & 1) * 2 - 1);
                  float f5 = (float)((i >> 2 & 1) * 2 - 1);
                  f3 *= 0.1F;
                  f4 *= 0.1F;
                  f5 *= 0.1F;
                  RayTraceResult raytraceresult = this.mc
                     .world
                     .rayTraceBlocks(
                        new Vec3d(d0 + (double)f3, d1 + (double)f4, d2 + (double)f5),
                        new Vec3d(d0 - d4 + (double)f3 + (double)f5, d1 - d6 + (double)f4, d2 - d5 + (double)f5)
                     );
                  if (raytraceresult != null) {
                     double d7 = raytraceresult.hitVec.distanceTo(new Vec3d(d0, d1, d2));
                     if (!noCol) {
                        if (d7 < d3) {
                           d3 = d7;
                        }
                     } else if (d7 < d3) {
                        d3 = dist;
                     }
                  }
               }
            }

            if (this.mc.gameSettings.thirdPersonView == 2) {
               GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            }

            GlStateManager.rotate(entity.rotationPitch - f2 + pitchPlus, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(entity.rotationYaw - f1 + yawPlus, 0.0F, 1.0F, 0.0F);
            if (noCol) {
               GlStateManager.translate(0.0, 0.0, -dist);
            } else {
               GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
            }

            GlStateManager.rotate(f1 - entity.rotationYaw - yawPlus, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(f2 - entity.rotationPitch - pitchPlus, 1.0F, 0.0F, 0.0F);
         }
      } else {
         GlStateManager.translate(0.0F, 0.0F, 0.1F);
      }

      if (Reflector.EntityViewRenderEvent_CameraSetup_Constructor.exists()) {
         if (!this.mc.gameSettings.debugCamEnable) {
            float f6 = smoothYaw + 180.0F;
            float f7 = smoothPitch * partialTicks;
            float f8 = 0.0F;
            if (entity instanceof EntityAnimal entityanimal1) {
               f6 = entityanimal1.prevRotationYawHead + (entityanimal1.rotationYawHead - entityanimal1.prevRotationYawHead) * partialTicks + 180.0F;
            }

            IBlockState iblockstate1 = ActiveRenderInfo.getBlockStateAtEntityViewpoint(this.mc.world, entity, partialTicks);
            Object object = Reflector.newInstance(Reflector.EntityViewRenderEvent_CameraSetup_Constructor, this, entity, iblockstate1, partialTicks, f6, f7, f8);
            Reflector.postForgeBusEvent(object);
            f8 = Reflector.callFloat(object, Reflector.EntityViewRenderEvent_CameraSetup_getRoll);
            f7 = Reflector.callFloat(object, Reflector.EntityViewRenderEvent_CameraSetup_getPitch);
            f6 = Reflector.callFloat(object, Reflector.EntityViewRenderEvent_CameraSetup_getYaw);
            GlStateManager.rotate(f8, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f7, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(f6, 0.0F, 1.0F, 0.0F);
         }
      } else if (!this.mc.gameSettings.debugCamEnable) {
         GlStateManager.rotate(smoothPitch, 1.0F, 0.0F, 0.0F);
         if (entity instanceof EntityAnimal entityanimal) {
            GlStateManager.rotate(
               entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F, 0.0F, 1.0F, 0.0F
            );
         } else {
            this.e.to = WorldRender.get.isReverseCamera() ? 180.0F : 0.0F;
            if (entity instanceof EntityPlayerSP sp && this.e.getAnim() > 0.03F) {
               sp.rotationYawHead = sp.rotationYaw + this.e.getAnim();
            }

            GlStateManager.rotate(smoothYaw + this.e.getAnim() + 180.0F, 0.0F, 1.0F, 0.0F);
         }
      }

      GlStateManager.translate(0.0F, -f, 0.0F);
      d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTicks;
      d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTicks + (double)f;
      d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTicks;
      this.cloudFog = this.mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
   }

   private final boolean cancelFog() {
      return !Panic.stop && Client.moduleManager != null && NoRender.get.actived && NoRender.get.FogEffect.getBool();
   }

   public void setupCameraTransformCompactCalcMatrix(float partialTicks) {
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      if (1.0 != 1.0) {
         GlStateManager.translate((float)this.cameraYaw, (float)(-this.cameraPitch), 0.0F);
         GlStateManager.scale(1.0, 1.0, 1.0);
      }

      Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      this.hurtCameraEffect(partialTicks);
      if (this.mc.gameSettings.viewBobbing) {
         this.setupViewBobbing(partialTicks);
      }

      this.orientCamera(partialTicks);
      Vec3d trans = WorldRender.get.getLastTranslated();
      GL11.glTranslated(trans.xCoord, trans.yCoord, trans.zCoord);
   }

   public void setupCameraTransform(float partialTicks, int pass) {
      this.farPlaneDistance = (float)(this.mc.gameSettings.renderDistanceChunks * 16);
      if (Config.isFogFancy()) {
         this.farPlaneDistance *= 0.95F;
      }

      if (Config.isFogFast()) {
         this.farPlaneDistance *= 0.83F;
      }

      if (this.cancelFog()) {
         this.farPlaneDistance = 300.0F;
      } else {
         this.farPlaneDistance = WorldRender.get.getRedistanceFogValue(this.farPlaneDistance);
      }

      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      float f = 0.07F;
      if (this.mc.gameSettings.anaglyph) {
         GlStateManager.translate((float)(-(pass * 2 - 1)) * 0.07F, 0.0F, 0.0F);
      }

      this.clipDistance = this.farPlaneDistance * 2.0F;
      if (this.clipDistance < 173.0F) {
         this.clipDistance = 173.0F;
      }

      if (1.0 != 1.0) {
         GlStateManager.translate((float)this.cameraYaw, (float)(-this.cameraPitch), 0.0F);
         GlStateManager.scale(1.0, 1.0, 1.0);
      }

      Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      if (this.mc.gameSettings.anaglyph) {
         GlStateManager.translate((float)(pass * 2 - 1) * 0.1F, 0.0F, 0.0F);
      }

      this.hurtCameraEffect(partialTicks);
      if (this.mc.gameSettings.viewBobbing) {
         this.setupViewBobbing(partialTicks);
      }

      float f1 = Minecraft.player.prevTimeInPortal + (Minecraft.player.timeInPortal - Minecraft.player.prevTimeInPortal) * partialTicks;
      boolean alkash = false;
      if (f1 == 0.0F) {
         alkash = BadTrip.get.isAlkash();
         if (alkash) {
            f1 = 0.4F * BadTrip.get.Alkash.getAnimation();
         }
      }

      if (f1 > 0.0F && (Panic.stop || !Bypass.get.isActived() || !Bypass.get.PortalGodmode.getBool() || !Bypass.cancelPortal)) {
         int i = 20;
         if (Minecraft.player.isPotionActive(MobEffects.NAUSEA)) {
            i = 7;
         }

         float f2 = 5.0F / (f1 * f1 + 5.0F) - f1 * 0.04F;
         float apdC = ((float)this.rendererUpdateCount + partialTicks) * (float)i;
         if (alkash) {
            apdC = ((float)this.rendererUpdateCount + partialTicks) * (float)i / 5.0F;
         }

         f2 *= f2;
         GlStateManager.rotate(apdC, 0.0F, 1.0F, 1.0F);
         GlStateManager.scale(1.0F / f2, 1.0F, 1.0F);
         GlStateManager.rotate(apdC, 0.0F, -1.0F, -1.0F);
      }

      this.orientCamera(partialTicks);
      WorldRender.get.translationCamera(partialTicks);
      if (this.debugView) {
         switch (this.debugViewDirection) {
            case 0:
               GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
               break;
            case 1:
               GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
               break;
            case 2:
               GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
               break;
            case 3:
               GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
               break;
            case 4:
               GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
         }
      }
   }

   private void renderHand(float partialTicks, int pass) {
      this.renderHand(partialTicks, pass, true, true, false);
   }

   public void renderHand(float p_renderHand_1_, int p_renderHand_2_, boolean p_renderHand_3_, boolean p_renderHand_4_, boolean p_renderHand_5_) {
      if (!this.debugView) {
         GlStateManager.matrixMode(5889);
         GlStateManager.loadIdentity();
         float f = 0.07F;
         if (this.mc.gameSettings.anaglyph) {
            GlStateManager.translate((float)(-(p_renderHand_2_ * 2 - 1)) * 0.07F, 0.0F, 0.0F);
         }

         if (Config.isShaders()) {
            Shaders.applyHandDepth();
         }

         Project.gluPerspective(
            this.getFOVModifier(p_renderHand_1_, false), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.farPlaneDistance * 2.0F
         );
         GlStateManager.matrixMode(5888);
         GlStateManager.loadIdentity();
         if (this.mc.gameSettings.anaglyph) {
            GlStateManager.translate((float)(p_renderHand_2_ * 2 - 1) * 0.1F, 0.0F, 0.0F);
         }

         boolean flag = false;
         if (p_renderHand_3_) {
            GlStateManager.pushMatrix();
            this.hurtCameraEffect(p_renderHand_1_);
            if (this.mc.gameSettings.viewBobbing) {
               this.setupViewBobbing(p_renderHand_1_);
            }

            flag = this.mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping();
            boolean flag1 = !ReflectorForge.renderFirstPersonHand(this.mc.renderGlobal, p_renderHand_1_, p_renderHand_2_);
            if (flag1 && this.mc.gameSettings.thirdPersonView == 0 && !flag && !this.mc.gameSettings.hideGUI && !this.mc.playerController.isSpectator()) {
               this.enableLightmap();
               if (Config.isShaders()) {
                  ShadersRender.renderItemFP(this.itemRenderer, p_renderHand_1_, p_renderHand_5_);
               } else {
                  this.itemRenderer.renderItemInFirstPerson(p_renderHand_1_);
               }

               this.disableLightmap();
            }

            GlStateManager.popMatrix();
         }

         if (!p_renderHand_4_) {
            return;
         }

         this.disableLightmap();
         if (this.mc.gameSettings.thirdPersonView == 0 && !flag) {
            this.itemRenderer.renderOverlays(p_renderHand_1_);
            this.hurtCameraEffect(p_renderHand_1_);
         }

         if (this.mc.gameSettings.viewBobbing) {
            this.setupViewBobbing(p_renderHand_1_);
         }
      }
   }

   public void disableLightmap() {
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.disableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      if (Config.isShaders()) {
         Shaders.disableLightmap();
      }
   }

   public void enableLightmap() {
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.matrixMode(5890);
      GlStateManager.loadIdentity();
      float f = 0.00390625F;
      GlStateManager.scale(0.00390625F, 0.00390625F, 0.00390625F);
      GlStateManager.translate(8.0F, 8.0F, 8.0F);
      GlStateManager.matrixMode(5888);
      this.mc.getTextureManager().bindTexture(this.locationLightMap);
      GlStateManager.glTexParameteri(3553, 10241, 9729);
      GlStateManager.glTexParameteri(3553, 10240, 9729);
      GlStateManager.glTexParameteri(3553, 10242, 10496);
      GlStateManager.glTexParameteri(3553, 10243, 10496);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      if (Config.isShaders()) {
         Shaders.enableLightmap();
      }
   }

   private void updateTorchFlicker() {
      this.torchFlickerDX = (float)((double)this.torchFlickerDX + (Math.random() - Math.random()) * Math.random() * Math.random());
      this.torchFlickerDX = (float)((double)this.torchFlickerDX * 0.9);
      this.torchFlickerX = this.torchFlickerX + (this.torchFlickerDX - this.torchFlickerX);
      this.lightmapUpdateNeeded = true;
   }

   private void updateLightmap(float partialTicks) {
      if (this.lightmapUpdateNeeded) {
         this.mc.mcProfiler.startSection("lightTex");
         World world = this.mc.world;
         if (world != null) {
            if (Config.isCustomColors()
               && CustomColors.updateLightmap(world, this.torchFlickerX, this.lightmapColors, Minecraft.player.isPotionActive(MobEffects.NIGHT_VISION))) {
               this.lightmapTexture.updateDynamicTexture();
               this.lightmapUpdateNeeded = false;
               this.mc.mcProfiler.endSection();
               return;
            }

            float f = world.getSunBrightness(1.0F);
            float f1 = f * 0.95F + 0.05F;

            for (int i = 0; i < 256; i++) {
               float f2 = world.provider.getLightBrightnessTable()[i / 16] * f1;
               float f3 = world.provider.getLightBrightnessTable()[i % 16] * (this.torchFlickerX * 0.1F + 1.5F);
               if (world.getLastLightningBolt() > 0 && (Panic.stop || !NoRender.get.actived || !NoRender.get.LightShotBolt.getBool())) {
                  f2 = world.provider.getLightBrightnessTable()[i / 16];
               }

               float f4 = f2 * (f * 0.65F + 0.35F);
               float f5 = f2 * (f * 0.65F + 0.35F);
               float f6 = f3 * ((f3 * 0.6F + 0.4F) * 0.6F + 0.4F);
               float f7 = f3 * (f3 * f3 * 0.6F + 0.4F);
               float f8 = f4 + f3;
               float f9 = f5 + f6;
               float f10 = f2 + f7;
               f8 = f8 * 0.96F + 0.03F;
               f9 = f9 * 0.96F + 0.03F;
               f10 = f10 * 0.96F + 0.03F;
               if (this.bossColorModifier > 0.0F) {
                  float f11 = this.bossColorModifierPrev + (this.bossColorModifier - this.bossColorModifierPrev) * partialTicks;
                  f8 = f8 * (1.0F - f11) + f8 * 0.7F * f11;
                  f9 = f9 * (1.0F - f11) + f9 * 0.6F * f11;
                  f10 = f10 * (1.0F - f11) + f10 * 0.6F * f11;
               }

               if (world.provider.getDimensionType().getId() == 1) {
                  f8 = 0.22F + f3 * 0.75F;
                  f9 = 0.28F + f6 * 0.75F;
                  f10 = 0.25F + f7 * 0.75F;
               }

               if (Reflector.ForgeWorldProvider_getLightmapColors.exists()) {
                  float[] afloat = new float[]{f8, f9, f10};
                  Reflector.call(world.provider, Reflector.ForgeWorldProvider_getLightmapColors, partialTicks, f, f2, f3, afloat);
                  f8 = afloat[0];
                  f9 = afloat[1];
                  f10 = afloat[2];
               }

               f8 = MathHelper.clamp(f8, 0.0F, 1.0F);
               f9 = MathHelper.clamp(f9, 0.0F, 1.0F);
               f10 = MathHelper.clamp(f10, 0.0F, 1.0F);
               float clientGammaVision = Xray.get.isActived() ? 1.0F : WorldRender.get.setupedGammaNightVision();
               if (Minecraft.player.isPotionActive(MobEffects.NIGHT_VISION) || clientGammaVision > 0.0F) {
                  float f15 = clientGammaVision > 0.0F ? clientGammaVision : this.getNightVisionBrightness(Minecraft.player, partialTicks);
                  float f12 = 1.0F / f8;
                  if (f12 > 1.0F / f9) {
                     f12 = 1.0F / f9;
                  }

                  if (f12 > 1.0F / f10) {
                     f12 = 1.0F / f10;
                  }

                  f8 = f8 * (1.0F - f15) + f8 * f12 * f15;
                  f9 = f9 * (1.0F - f15) + f9 * f12 * f15;
                  f10 = f10 * (1.0F - f15) + f10 * f12 * f15;
               }

               if (f8 > 1.0F) {
                  f8 = 1.0F;
               }

               if (f9 > 1.0F) {
                  f9 = 1.0F;
               }

               if (f10 > 1.0F) {
                  f10 = 1.0F;
               }

               float f16 = this.mc.gameSettings.gammaSetting;
               float f17 = 1.0F - f8;
               float f13 = 1.0F - f9;
               float f14 = 1.0F - f10;
               f17 = 1.0F - f17 * f17 * f17 * f17;
               f13 = 1.0F - f13 * f13 * f13 * f13;
               f14 = 1.0F - f14 * f14 * f14 * f14;
               f8 = f8 * (1.0F - f16) + f17 * f16;
               f9 = f9 * (1.0F - f16) + f13 * f16;
               f10 = f10 * (1.0F - f16) + f14 * f16;
               f8 = f8 * 0.96F + 0.03F;
               f9 = f9 * 0.96F + 0.03F;
               f10 = f10 * 0.96F + 0.03F;
               if (f8 > 1.0F) {
                  f8 = 1.0F;
               }

               if (f9 > 1.0F) {
                  f9 = 1.0F;
               }

               if (f10 > 1.0F) {
                  f10 = 1.0F;
               }

               if (f8 < 0.0F) {
                  f8 = 0.0F;
               }

               if (f9 < 0.0F) {
                  f9 = 0.0F;
               }

               if (f10 < 0.0F) {
                  f10 = 0.0F;
               }

               int j = 255;
               int k = (int)(f8 * 255.0F);
               int l = (int)(f9 * 255.0F);
               int i1 = (int)(f10 * 255.0F);
               this.lightmapColors[i] = 0xFF000000 | k << 16 | l << 8 | i1;
            }

            this.lightmapTexture.updateDynamicTexture();
            this.lightmapUpdateNeeded = false;
            this.mc.mcProfiler.endSection();
         }
      }
   }

   public float getNightVisionBrightness(EntityLivingBase entitylivingbaseIn, float partialTicks) {
      int i = entitylivingbaseIn.getActivePotionEffect(MobEffects.NIGHT_VISION).getDuration();
      return i > 200 ? 1.0F : 0.7F + MathHelper.sin(((float)i - partialTicks) * (float) Math.PI * 0.2F) * 0.3F;
   }

   public void updateCameraAndRender(float partialTicks, long nanoTime) {
      this.frameInit();
      boolean flag = DisplayCheck.isActive();
      if (!flag && this.mc.gameSettings.pauseOnLostFocus && (!this.mc.gameSettings.touchscreen || !Mouse.isButtonDown(1))) {
         if (Minecraft.getSystemTime() - this.prevFrameTime > 500L) {
            this.mc.displayInGameMenu();
         }
      } else {
         this.prevFrameTime = Minecraft.getSystemTime();
      }

      this.mc.mcProfiler.startSection("mouse");
      if (flag && Minecraft.IS_RUNNING_ON_MAC && this.mc.inGameHasFocus && !Mouse.isInsideWindow()) {
         Mouse.setGrabbed(false);
         Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2 - 20);
         Mouse.setGrabbed(true);
      }

      if (this.mc.inGameHasFocus && flag) {
         this.mc.mouseHelper.mouseXYChange();
         this.mc.func_193032_ao().func_193299_a(this.mc.mouseHelper);
         float f = this.mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
         float f1 = f * f * f * 8.0F;
         float f2 = (float)this.mc.mouseHelper.deltaX * f1;
         float f3 = (float)this.mc.mouseHelper.deltaY * f1;
         int i = 1;
         if (this.mc.gameSettings.invertMouse) {
            i = -1;
         }

         boolean freelook = !Panic.stop && WorldRender.get.isActived() && WorldRender.get.freeLookState;
         if (freelook) {
            WorldRender.get.updateFreeLookRotation(f2 * (float)i * 0.2F, f3 * (float)(-i) * 0.2F, partialTicks);
         }

         if (this.mc.gameSettings.smoothCamera) {
            this.smoothCamYaw += f2;
            this.smoothCamPitch += f3;
            float f4 = partialTicks - this.smoothCamPartialTicks;
            this.smoothCamPartialTicks = partialTicks;
            f2 = this.smoothCamFilterX * f4;
            f3 = this.smoothCamFilterY * f4;
            if (!freelook) {
               Minecraft.player.setAngles(f2, f3 * (float)i);
            }
         } else {
            this.smoothCamYaw = 0.0F;
            this.smoothCamPitch = 0.0F;
            if (!freelook) {
               Minecraft.player.setAngles(f2, f3 * (float)i);
            }
         }
      }

      this.mc.mcProfiler.endSection();
      boolean pure = !Minecraft.temporalImageSizeMoreThan16x;
      if (!this.mc.skipRenderWorld) {
         anaglyphEnable = this.mc.gameSettings.anaglyph;
         final ScaledResolution scaledresolution = new ScaledResolution(this.mc);
         int i1 = scaledresolution.getScaledWidth();
         int j1 = scaledresolution.getScaledHeight();
         final int k1 = Mouse.getX() * i1 / this.mc.displayWidth;
         final int l1 = j1 - Mouse.getY() * j1 / this.mc.displayHeight - 1;
         int i2 = this.mc.gameSettings.limitFramerate;
         if (this.mc.world == null) {
            GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            this.setupOverlayRendering();
            this.renderEndNanoTime = System.nanoTime();
            TileEntityRendererDispatcher.instance.renderEngine = this.mc.getTextureManager();
            TileEntityRendererDispatcher.instance.fontRenderer = this.mc.fontRendererObj;
         } else {
            this.mc.mcProfiler.startSection("level");
            int j = Math.min(Minecraft.getDebugFPS(), i2);
            j = Math.max(j, 60);
            long k = System.nanoTime() - nanoTime;
            long l = Math.max((long)(1000000000 / j / 4) - k, 0L);
            this.renderWorld(partialTicks, System.nanoTime() + l);
            if (this.mc.isSingleplayer() && this.timeWorldIcon < Minecraft.getSystemTime() - 1000L) {
               this.timeWorldIcon = Minecraft.getSystemTime();
               if (!this.mc.getIntegratedServer().isWorldIconSet()) {
                  this.createWorldIcon();
               }
            }

            if (OpenGlHelper.shadersSupported) {
               this.mc.renderGlobal.renderEntityOutlineFramebuffer();
               if (this.theShaderGroup != null && this.useShader) {
                  GlStateManager.matrixMode(5890);
                  GlStateManager.pushMatrix();
                  GlStateManager.loadIdentity();
                  this.theShaderGroup.loadShaderGroup(partialTicks);
                  GlStateManager.popMatrix();
               }

               this.mc.getFramebuffer().bindFramebuffer(true);
            }

            this.renderEndNanoTime = System.nanoTime();
            this.mc.mcProfiler.endStartSection("gui");
            if (this.mc.runScreenshot) {
               switch (this.mc.ticksScreenshotsUpdate) {
                  case 0:
                     Panic.enablePanic();
                  case 1:
                     this.mc
                        .ingameGUI
                        .getChatGUI()
                        .printChatMessage(
                           ScreenShotHelper.saveScreenshot(this.mc.mcDataDir, this.mc.displayWidth, this.mc.displayHeight, this.mc.framebufferMc)
                        );
                  case 2:
                     Panic.disablePanic();
                     this.mc.runScreenshot = false;
                  default:
                     this.mc.ticksScreenshotsUpdate++;
               }
            }

            if (!this.mc.gameSettings.hideGUI || this.mc.currentScreen != null) {
               GlStateManager.alphaFunc(516, 0.1F);
               this.setupOverlayRendering();
               if (!Panic.stop && WorldRender.get.isRenderBloom()) {
                  WorldRender.get.drawWorldBloom();
               }

               this.func_190563_a(i1, j1, partialTicks);
               this.mc.ingameGUI.renderGameOverlay(partialTicks);
               if (!Panic.stop) {
                  for (Module module : Client.moduleManager.modules) {
                     try {
                        module.onAlwaysPostRenderThread();
                     } catch (Exception var22) {
                        var22.printStackTrace();
                     }
                  }
               }

               if (this.mc.gameSettings.ofShowFps && !this.mc.gameSettings.showDebugInfo) {
                  Config.drawFps();
               }

               if (this.mc.gameSettings.showDebugInfo) {
                  Lagometer.showLagometer(scaledresolution);
               }
            }

            this.mc.mcProfiler.endSection();
            this.drawConfigSaveAnimation(1.0F, scaledresolution);
            GlStateManager.resetColor();
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
            GL11.glBlendFunc(770, 771);
         }

         if (this.mc.currentScreen != null) {
            GlStateManager.clear(256);

            try {
               if (Reflector.ForgeHooksClient_drawScreen.exists()) {
                  Reflector.callVoid(Reflector.ForgeHooksClient_drawScreen, this.mc.currentScreen, k1, l1, this.mc.func_193989_ak());
               } else {
                  if (ComfortUi.get != null) {
                     ComfortUi.get.onDrawGuiScreen(k1, l1, true);
                  }

                  GL11.glPushMatrix();
                  this.mc.currentScreen.drawScreen(k1, l1, this.mc.func_193989_ak());
                  GL11.glPopMatrix();
                  if (ComfortUi.get != null) {
                     ComfortUi.get.onDrawGuiScreen(k1, l1, false);
                  }
               }
            } catch (Throwable var21) {
               CrashReport crashreport = CrashReport.makeCrashReport(var21, "Rendering screen");
               CrashReportCategory crashreportcategory = crashreport.makeCategory("Screen render details");
               crashreportcategory.setDetail("Screen name", new ICrashReportDetail<String>() {
                  public String call() throws Exception {
                     return EntityRenderer.this.mc.currentScreen.getClass().getCanonicalName();
                  }
               });
               crashreportcategory.setDetail("Mouse location", new ICrashReportDetail<String>() {
                  public String call() throws Exception {
                     return String.format("Scaled: (%d, %d). Absolute: (%d, %d)", k1, l1, Mouse.getX(), Mouse.getY());
                  }
               });
               crashreportcategory.setDetail(
                  "Screen size",
                  new ICrashReportDetail<String>() {
                     public String call() throws Exception {
                        return String.format(
                           "Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %d",
                           scaledresolution.getScaledWidth(),
                           scaledresolution.getScaledHeight(),
                           EntityRenderer.this.mc.displayWidth,
                           EntityRenderer.this.mc.displayHeight,
                           ScaledResolution.getScaleFactor()
                        );
                     }
                  }
               );
               throw new ReportedException(crashreport);
            }
         }

         try {
            Fonts.updateScale();
         } catch (Exception var20) {
            var20.printStackTrace();
         }
      }

      GuiScreen.fade.renderAnimation(0.02F, 1.0F);
      ScaledResolution sr = new ScaledResolution(this.mc);
      ComfortUi.get.influencePostRenderScreen$RoundScreen(sr);
      this.frameFinish(sr, pure);
      this.waitForServerThread();
      Lagometer.updateLagometer();
      if (this.mc.gameSettings.ofProfiler) {
         this.mc.gameSettings.showDebugProfilerChart = true;
      }
   }

   private String tos(String... l) {
      String i = "";

      for (String s : l) {
         i = i + s;
      }

      return i;
   }

   private void createWorldIcon() {
      if (this.mc.renderGlobal.getRenderedChunks() > 10 && this.mc.renderGlobal.hasNoChunkUpdates() && !this.mc.getIntegratedServer().isWorldIconSet()) {
         BufferedImage bufferedimage = ScreenShotHelper.createScreenshot(this.mc.displayWidth, this.mc.displayHeight, this.mc.getFramebuffer());
         int i = bufferedimage.getWidth();
         int j = bufferedimage.getHeight();
         int k = 0;
         int l = 0;
         if (i > j) {
            k = (i - j) / 2;
            i = j;
         } else {
            l = (j - i) / 2;
         }

         try {
            BufferedImage bufferedimage1 = new BufferedImage(64, 64, 1);
            Graphics graphics = bufferedimage1.createGraphics();
            graphics.drawImage(bufferedimage, 0, 0, 64, 64, k, l, k + i, l + i, null);
            graphics.dispose();
            ImageIO.write(bufferedimage1, "png", this.mc.getIntegratedServer().getWorldIconFile());
         } catch (IOException var8) {
            LOGGER.warn("Couldn't save auto screenshot", (Throwable)var8);
         }
      }
   }

   public void renderStreamIndicator(float partialTicks) {
      this.setupOverlayRendering();
   }

   private boolean isDrawBlockOutline() {
      Objects.requireNonNull(this);
      Entity entity = this.mc.getRenderViewEntity();
      boolean flag = entity instanceof EntityPlayer && !this.mc.gameSettings.hideGUI;
      if (flag && !((EntityPlayer)entity).capabilities.allowEdit) {
         ItemStack itemstack = ((EntityPlayer)entity).getHeldItemMainhand();
         if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos blockpos = this.mc.objectMouseOver.getBlockPos();
            IBlockState iblockstate = this.mc.world.getBlockState(blockpos);
            Block block = iblockstate.getBlock();
            if (this.mc.playerController.getCurrentGameType() == GameType.SPECTATOR) {
               flag = ReflectorForge.blockHasTileEntity(iblockstate) && this.mc.world.getTileEntity(blockpos) instanceof IInventory;
            } else {
               flag = !itemstack.func_190926_b() && (itemstack.canDestroy(block) || itemstack.canPlaceOn(block));
            }
         }
      }

      return flag;
   }

   public void renderWorld(float partialTicks, long finishTimeNano) {
      this.updateLightmap(partialTicks);
      if (this.mc.getRenderViewEntity() == null) {
         this.mc.setRenderViewEntity(Minecraft.player);
      }

      this.getMouseOver(partialTicks);
      if (Config.isShaders()) {
         Shaders.beginRender(this.mc, partialTicks, finishTimeNano);
      }

      GlStateManager.enableDepth();
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, 0.1F);
      this.mc.mcProfiler.startSection("center");
      if (this.mc.gameSettings.anaglyph) {
         anaglyphField = 0;
         GlStateManager.colorMask(false, true, true, false);
         this.renderWorldPass(0, partialTicks, finishTimeNano);
         anaglyphField = 1;
         GlStateManager.colorMask(true, false, false, false);
         this.renderWorldPass(1, partialTicks, finishTimeNano);
         GlStateManager.colorMask(true, true, true, false);
      } else {
         this.renderWorldPass(2, partialTicks, finishTimeNano);
      }

      this.mc.mcProfiler.endSection();
   }

   private void renderWorldPass(int pass, float partialTicks, long finishTimeNano) {
      UProfiler uProfiler = Client.uProfilers.getProfiler("3d objs");
      uProfiler.startCalc();
      this.renderRainSnow(partialTicks);
      uProfiler.addObj("Rain/Snow Minecraft render3d");
      boolean flag = Config.isShaders();
      if (flag) {
         Shaders.beginRenderPass(pass, partialTicks, finishTimeNano);
      }

      RenderGlobal renderglobal = this.mc.renderGlobal;
      ParticleManager particlemanager = this.mc.effectRenderer;
      boolean flag1 = this.isDrawBlockOutline();
      GlStateManager.enableCull();
      this.mc.mcProfiler.endStartSection("clear");
      if (flag) {
         Shaders.setViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
      } else {
         GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
      }

      this.updateFogColor(partialTicks);
      GlStateManager.clear(16640);
      if (flag) {
         Shaders.clearRenderBuffer();
      }

      this.mc.mcProfiler.endStartSection("camera");
      this.setupCameraTransform(partialTicks, pass);
      uProfiler.addObj("CameraTransform Minecraft render3d");
      if (flag) {
         Shaders.setCamera(partialTicks);
      }

      ActiveRenderInfo.updateRenderInfo(Minecraft.player, this.mc.gameSettings.thirdPersonView == 2);
      this.mc.mcProfiler.endStartSection("frustum");
      ClippingHelper clippinghelper = ClippingHelperImpl.getInstance();
      this.mc.mcProfiler.endStartSection("culling");
      ICamera icamera = new Frustum(clippinghelper);
      Entity entity = this.mc.getRenderViewEntity();
      double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks;
      double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks;
      double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks;
      if (flag) {
         ShadersRender.setFrustrumPosition(icamera, d0, d1, d2);
      } else {
         icamera.setPosition(d0, d1, d2);
      }

      uProfiler.addObj("CameraPosition Minecraft render3d");
      if ((Config.isSkyEnabled() || Config.isSunMoonEnabled() || Config.isStarsEnabled()) && !Shaders.isShadowPass) {
         this.setupFog(-1, partialTicks);
         this.mc.mcProfiler.endStartSection("sky");
         GlStateManager.matrixMode(5889);
         GlStateManager.loadIdentity();
         Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance);
         GlStateManager.matrixMode(5888);
         if (flag) {
            Shaders.beginSky();
         }

         renderglobal.renderSky(partialTicks, pass);
         if (flag) {
            Shaders.endSky();
         }

         GlStateManager.matrixMode(5889);
         GlStateManager.loadIdentity();
         Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance);
         GlStateManager.matrixMode(5888);
      } else {
         GlStateManager.disableBlend();
      }

      this.setupFog(0, partialTicks);
      uProfiler.addObj("Sky&Fog Minecraft render3d");
      GlStateManager.shadeModel(7425);
      if (entity.posY + (double)entity.getEyeHeight() < 128.0 + (double)(this.mc.gameSettings.ofCloudsHeight * 128.0F)) {
         this.renderCloudsCheck(renderglobal, partialTicks, pass, d0, d1, d2);
      }

      uProfiler.addObj("CloudsRender Minecraft render3d");
      this.mc.mcProfiler.endStartSection("prepareterrain");
      this.setupFog(0, partialTicks);
      this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
      RenderHelper.disableStandardItemLighting();
      this.mc.mcProfiler.endStartSection("terrain_setup");
      this.checkLoadVisibleChunks(entity, partialTicks, icamera, Minecraft.player.isSpectator());
      if (flag) {
         ShadersRender.setupTerrain(renderglobal, entity, (double)partialTicks, icamera, this.frameCount++, Minecraft.player.isSpectator());
      } else {
         renderglobal.setupTerrain(entity, (double)partialTicks, icamera, this.frameCount++, Minecraft.player.isSpectator());
      }

      if (pass == 0 || pass == 2) {
         this.mc.mcProfiler.endStartSection("updatechunks");
         Lagometer.timerChunkUpload.start();
         this.mc.renderGlobal.updateChunks(finishTimeNano);
         Lagometer.timerChunkUpload.end();
      }

      uProfiler.addObj("Chucks Minecraft render3d");
      this.mc.mcProfiler.endStartSection("terrain");
      Lagometer.timerTerrain.start();
      if (this.mc.gameSettings.ofSmoothFps && pass > 0) {
         this.mc.mcProfiler.endStartSection("finish");
         GL11.glFinish();
         this.mc.mcProfiler.endStartSection("terrain");
      }

      GlStateManager.matrixMode(5888);
      GlStateManager.pushMatrix();
      if (flag) {
         ShadersRender.beginTerrainSolid();
      }

      uProfiler.addObj("Tarrain Minecraft render3d");
      float g = 0.55F;
      float g2 = 0.48F;
      boolean blur = WorldRender.get != null && WorldRender.get.isActived() && WorldRender.get.BlurBlocks.canBeRender();
      if (blur) {
         float a = WorldRender.get.BlurBlocks.getAnimation();
         GL11.glAlphaFunc(516, MathUtils.lerp(g, g2, a));
      }

      this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(blur, false);
      renderglobal.renderBlockLayer(BlockRenderLayer.SOLID, (double)partialTicks, pass, entity);
      GlStateManager.enableAlpha();
      if (flag) {
         ShadersRender.beginTerrainCutoutMipped();
      }

      if (blur) {
         this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
      }

      renderglobal.renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, (double)partialTicks, pass, entity);
      this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(blur, false);
      if (flag) {
         ShadersRender.beginTerrainCutout();
      }

      renderglobal.renderBlockLayer(BlockRenderLayer.CUTOUT, (double)partialTicks, pass, entity);
      this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
      if (Config.isShaders()) {
         drawClient(uProfiler, partialTicks, this.mc);
      }

      if (flag) {
         ShadersRender.endTerrain();
      }

      if (blur) {
         GL11.glAlphaFunc(516, 0.1F);
      }

      Lagometer.timerTerrain.end();
      uProfiler.addObj("BlocksRender Minecraft render3d");
      GlStateManager.shadeModel(7424);
      GlStateManager.alphaFunc(516, 0.1F);
      if (!this.debugView) {
         GlStateManager.matrixMode(5888);
         GlStateManager.popMatrix();
         GlStateManager.pushMatrix();
         RenderHelper.enableStandardItemLighting();
         this.mc.mcProfiler.endStartSection("entities");
         if (Reflector.ForgeHooksClient_setRenderPass.exists()) {
            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, 0);
         }

         renderglobal.renderEntities(entity, icamera, partialTicks);
         if (Reflector.ForgeHooksClient_setRenderPass.exists()) {
            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, -1);
         }

         RenderHelper.disableStandardItemLighting();
         this.disableLightmap();
         uProfiler.addObj("Entities Minecraft render3d");
      }

      GlStateManager.matrixMode(5888);
      GlStateManager.popMatrix();
      if (flag1 && this.mc.objectMouseOver != null && !entity.isInsideOfMaterial(Material.WATER)) {
         EntityPlayer entityplayer = (EntityPlayer)entity;
         GlStateManager.disableAlpha();
         this.mc.mcProfiler.endStartSection("outline");
         if (!Reflector.ForgeHooksClient_onDrawBlockHighlight.exists()
            || !Reflector.callBoolean(Reflector.ForgeHooksClient_onDrawBlockHighlight, renderglobal, entityplayer, this.mc.objectMouseOver, 0, partialTicks)) {
            renderglobal.drawSelectionBox(entityplayer, this.mc.objectMouseOver, 0, partialTicks);
         }

         GlStateManager.enableAlpha();
         uProfiler.addObj("Outlines ents Minecraft render3d");
      }

      if (this.mc.debugRenderer.shouldRender()) {
         boolean flag2 = GlStateManager.isFogEnabled();
         GlStateManager.disableFog();
         this.mc.debugRenderer.renderDebug(partialTicks, finishTimeNano);
         GlStateManager.setFogEnabled(flag2);
      }

      if (!renderglobal.damagedBlocks.isEmpty() && (!ESP.get.isActived() || !ESP.get.BreakOver.getBool())) {
         this.mc.mcProfiler.endStartSection("destroyProgress");
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
         );
         this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
         renderglobal.drawBlockDamageTexture(Tessellator.getInstance(), Tessellator.getInstance().getBuffer(), entity, partialTicks);
         this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
         GlStateManager.disableBlend();
      }

      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.disableBlend();
      uProfiler.addObj("RenderDamageBlocks Minecraft render3d");
      if (!this.debugView) {
         this.enableLightmap();
         this.mc.mcProfiler.endStartSection("litParticles");
         if (flag) {
            Shaders.beginLitParticles();
         }

         particlemanager.renderLitParticles(entity, partialTicks);
         RenderHelper.disableStandardItemLighting();
         this.setupFog(0, partialTicks);
         this.mc.mcProfiler.endStartSection("particles");
         if (flag) {
            Shaders.beginParticles();
         }

         particlemanager.renderParticles(entity, partialTicks);
         if (flag) {
            Shaders.endParticles();
         }

         this.disableLightmap();
      }

      GlStateManager.depthMask(false);
      GlStateManager.enableCull();
      this.mc.mcProfiler.endStartSection("weather");
      if (flag) {
         Shaders.beginWeather();
      }

      this.renderRainSnow(partialTicks);
      if (flag) {
         Shaders.endWeather();
      }

      GlStateManager.depthMask(true);
      renderglobal.renderWorldBorder(entity, partialTicks);
      if (flag) {
         ShadersRender.renderHand0(this, partialTicks, pass);
         Shaders.preWater();
      }

      GlStateManager.disableBlend();
      GlStateManager.enableCull();
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      GlStateManager.alphaFunc(516, 0.1F);
      this.setupFog(0, partialTicks);
      GlStateManager.enableBlend();
      GlStateManager.depthMask(false);
      this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
      GlStateManager.shadeModel(7425);
      this.mc.mcProfiler.endStartSection("translucent");
      if (flag) {
         Shaders.beginWater();
      }

      renderglobal.renderBlockLayer(BlockRenderLayer.TRANSLUCENT, (double)partialTicks, pass, entity);
      if (flag) {
         Shaders.endWater();
      }

      uProfiler.addObj("Blocks Minecraft rendering 3d");
      this.listTileOutlines.clear();

      for (TileEntity tileentity : this.updatedWorld.getLoadedTileEntityList()) {
         if (tileentity.isGlowing()) {
            this.listTileOutlines.add(new EntityRenderer.RunnableRenderTile(tileentity, () -> {
               if (Config.isShaders()) {
                  Shaders.nextBlockEntity(tileentity);
               }

               TileEntityRendererDispatcher.instance.renderTileEntity(tileentity, partialTicks, -1);
            }));
         }
      }

      if (!this.listTileOutlines.isEmpty()) {
         this.mc.entityRenderer.listTileOutlines.forEach(tileRender -> tileRender.tile.setGlowingPostGlowFrames(false, 0));
      }

      uProfiler.addObj("ESP Tiles glows render 3D");
      if ((Reflector.ForgeHooksClient_setRenderPass.exists() || pass == 0 && !this.listTileOutlines.isEmpty()) && !this.debugView) {
         RenderHelper.enableStandardItemLighting();
         this.mc.mcProfiler.endStartSection("entities");
         Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, 1);
         this.mc.renderGlobal.renderEntities(entity, icamera, partialTicks);
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
         Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, -1);
         RenderHelper.disableStandardItemLighting();
      }

      uProfiler.addObj("Entities Minecraft render3d");
      GlStateManager.shadeModel(7424);
      GlStateManager.depthMask(true);
      GlStateManager.enableCull();
      GlStateManager.disableBlend();
      GlStateManager.disableFog();
      if (entity.posY + (double)entity.getEyeHeight() >= 128.0 + (double)(this.mc.gameSettings.ofCloudsHeight * 128.0F)) {
         this.mc.mcProfiler.endStartSection("aboveClouds");
         this.renderCloudsCheck(renderglobal, partialTicks, pass, d0, d1, d2);
      }

      if (Reflector.ForgeHooksClient_dispatchRenderLast.exists()) {
         this.mc.mcProfiler.endStartSection("forge_render_last");
         Reflector.callVoid(Reflector.ForgeHooksClient_dispatchRenderLast, renderglobal, partialTicks);
      }

      uProfiler.addObj("Clouds Minecraft render3d");
      if (!Config.isShaders()) {
         drawClient(uProfiler, partialTicks, this.mc);
      }

      if (!Panic.stop && this.mc.world != null) {
         this.mc
            .world
            .playerEntities
            .stream()
            .filter(p -> p != null && p.isEntityAlive() && (!p.hasNewVersionMoves || !p.isLay) && !p.isElytraFlying() && Client.summit(p))
            .forEach(player -> {
               double d0S = Minecraft.player.prevPosX + (Minecraft.player.posX - Minecraft.player.prevPosX) * (double)partialTicks;
               double d1S = Minecraft.player.prevPosY + (Minecraft.player.posY - Minecraft.player.prevPosY) * (double)partialTicks;
               double d2S = Minecraft.player.prevPosZ + (Minecraft.player.posZ - Minecraft.player.prevPosZ) * (double)partialTicks;
               double d0I = player.prevPosX + (player.posX - player.prevPosX) * (double)partialTicks;
               double d1I = player.prevPosY + (player.posY - player.prevPosY) * (double)partialTicks;
               double d2I = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)partialTicks;
               GL11.glPushMatrix();
               GL11.glTranslated(-d0S + d0I, -d1S + d1I, -d2S + d2I);
               RenderUtils.drawPenisOnEntity(player, 0.0, 0.0, 0.0);
               GL11.glPopMatrix();
            });
      }

      if (!Panic.stop) {
         PlayerHelper.insert_EntityRenderer_preRenderHand_HOOK();
      }

      this.mc.mcProfiler.endStartSection("hand");
      Objects.requireNonNull(this);
      if (!Shaders.isShadowPass) {
         if (flag) {
            ShadersRender.renderHand1(this, partialTicks, pass);
            Shaders.renderCompositeFinal();
         }

         GlStateManager.clear(256);
         uProfiler.addObj("renderShaders composite Minecraft 3d");
         if (!Panic.stop && !this.mc.runScreenshot) {
            for (Module mod : Client.moduleManager.getModuleList()) {
               mod.alwaysRender3DV2();
               mod.alwaysRender3DV2(partialTicks);
               uProfiler.addObj(mod.getName() + " AlwaysRender3DV2");
            }
         }

         if (flag) {
            ShadersRender.renderFPOverlay(this, partialTicks, pass);
         } else {
            this.renderHand(partialTicks, pass);
         }

         uProfiler.addObj("renderHand Minecraft 3d");
      }

      if (flag) {
         Shaders.endRender();
      }

      uProfiler.addObj("endShaders begin Minecraft 3d");
      uProfiler.endCalc(true);
   }

   private static void drawClient(UProfiler uProfiler, float partialTicks, Minecraft mc) {
      if (Config.isShaders()) {
         RenderUtils.skipOptifineShader(() -> {
            uProfiler.addObj("Shaders skipping Events 'Optifine'");
            if (!Panic.stop && !mc.runScreenshot) {
               mc.sndHandleEdit.getSurround().draw3dTest();
               uProfiler.addObj("RTXSoundSurround render3d");
               if (!Panic.stop) {
                  Client.pointRenderer.render3D();
                  uProfiler.addObj("Shaders, PointsRender3d");
               }

               Event3D event3Dx = new Event3D(partialTicks);
               event3Dx.call();
               uProfiler.addObj("Shaders, Event 3D");

               for (Module modx : Client.moduleManager.getModuleList()) {
                  modx.alwaysRender3D(partialTicks);
                  modx.alwaysRender3D();
                  uProfiler.addObj(modx.getName() + "Shaders, AlwaysRender3d");
               }
            }
         });
      } else if (!Panic.stop && !mc.runScreenshot) {
         mc.sndHandleEdit.getSurround().draw3dTest();
         uProfiler.addObj("RTXSoundSurround render3d");
         if (!Panic.stop) {
            Client.pointRenderer.render3D();
            uProfiler.addObj("PointsRender3d");
         }

         Event3D event3D = new Event3D(partialTicks);
         event3D.call();
         uProfiler.addObj("Event 3D");

         for (Module mod : Client.moduleManager.getModuleList()) {
            mod.alwaysRender3D(partialTicks);
            mod.alwaysRender3D();
            uProfiler.addObj(mod.getName() + " AlwaysRender3d");
         }
      }
   }

   private void renderCloudsCheck(RenderGlobal renderGlobalIn, float partialTicks, int pass, double p_180437_4_, double p_180437_6_, double p_180437_8_) {
      if (this.mc.gameSettings.renderDistanceChunks >= 4 && !Config.isCloudsOff() && Shaders.shouldRenderClouds(this.mc.gameSettings)) {
         this.mc.mcProfiler.endStartSection("clouds");
         GlStateManager.matrixMode(5889);
         GlStateManager.loadIdentity();
         Project.gluPerspective(
            this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance * 4.0F
         );
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         this.setupFog(0, partialTicks);
         renderGlobalIn.renderClouds(partialTicks, pass, p_180437_4_, p_180437_6_, p_180437_8_);
         GlStateManager.disableFog();
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5889);
         GlStateManager.loadIdentity();
         Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.clipDistance);
         GlStateManager.matrixMode(5888);
      }
   }

   private void addRainParticles() {
      float f = this.mc.world.getRainStrength(1.0F);
      if (!Config.isRainFancy()) {
         f /= 2.0F;
      }

      if (f != 0.0F && Config.isRainSplash()) {
         this.random.setSeed((long)this.rendererUpdateCount * 312987231L);
         Entity entity = this.mc.getRenderViewEntity();
         World world = this.mc.world;
         BlockPos blockpos = new BlockPos(entity);
         int i = 10;
         double d0 = 0.0;
         double d1 = 0.0;
         double d2 = 0.0;
         int j = 0;
         int k = (int)(100.0F * f * f);
         if (this.mc.gameSettings.particleSetting == 1) {
            k >>= 1;
         } else if (this.mc.gameSettings.particleSetting == 2) {
            k = 0;
         }

         for (int l = 0; l < k; l++) {
            BlockPos blockpos1 = world.getPrecipitationHeight(
               blockpos.add(this.random.nextInt(10) - this.random.nextInt(10), 0, this.random.nextInt(10) - this.random.nextInt(10))
            );
            Biome biome = world.getBiome(blockpos1);
            BlockPos blockpos2 = blockpos1.down();
            IBlockState iblockstate = world.getBlockState(blockpos2);
            if (blockpos1.getY() <= blockpos.getY() + 10
               && blockpos1.getY() >= blockpos.getY() - 10
               && biome.canRain()
               && biome.getFloatTemperature(blockpos1) >= 0.15F) {
               double d3 = this.random.nextDouble();
               double d4 = this.random.nextDouble();
               AxisAlignedBB axisalignedbb = iblockstate.getBoundingBox(world, blockpos2);
               if (iblockstate.getMaterial() == Material.LAVA || iblockstate.getBlock() == Blocks.MAGMA) {
                  this.mc
                     .world
                     .spawnParticle(
                        EnumParticleTypes.SMOKE_NORMAL,
                        (double)blockpos1.getX() + d3,
                        (double)((float)blockpos1.getY() + 0.1F) - axisalignedbb.minY,
                        (double)blockpos1.getZ() + d4,
                        0.0,
                        0.0,
                        0.0,
                        new int[0]
                     );
               } else if (iblockstate.getMaterial() != Material.AIR) {
                  if (this.random.nextInt(++j) == 0) {
                     d0 = (double)blockpos2.getX() + d3;
                     d1 = (double)((float)blockpos2.getY() + 0.1F) + axisalignedbb.maxY - 1.0;
                     d2 = (double)blockpos2.getZ() + d4;
                  }

                  this.mc
                     .world
                     .spawnParticle(
                        EnumParticleTypes.WATER_DROP,
                        (double)blockpos2.getX() + d3,
                        (double)((float)blockpos2.getY() + 0.1F) + axisalignedbb.maxY,
                        (double)blockpos2.getZ() + d4,
                        0.0,
                        0.0,
                        0.0,
                        new int[0]
                     );
               }
            }
         }

         if (j > 0 && this.random.nextInt(3) < this.rainSoundCounter++) {
            this.rainSoundCounter = 0;
            if (d1 > (double)(blockpos.getY() + 1) && world.getPrecipitationHeight(blockpos).getY() > MathHelper.floor((float)blockpos.getY())) {
               this.mc.world.playSound(d0, d1, d2, SoundEvents.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.1F, 0.5F, false);
            } else {
               this.mc.world.playSound(d0, d1, d2, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 0.2F, 1.0F, false);
            }
         }
      }
   }

   public void renderRainSnow(float partialTicks) {
      if (Reflector.ForgeWorldProvider_getWeatherRenderer.exists()) {
         WorldProvider worldprovider = this.mc.world.provider;
         Object object = Reflector.call(worldprovider, Reflector.ForgeWorldProvider_getWeatherRenderer);
         if (object != null) {
            Reflector.callVoid(object, Reflector.IRenderHandler_render, partialTicks, this.mc.world, this.mc);
            return;
         }
      }

      float f5 = this.mc.world.getRainStrength(partialTicks);
      if (f5 > 0.0F) {
         if (Config.isRainOff()) {
            return;
         }

         this.enableLightmap();
         Entity entity = this.mc.getRenderViewEntity();
         World world = this.mc.world;
         int i = MathHelper.floor(entity.posX);
         int j = MathHelper.floor(entity.posY);
         int k = MathHelper.floor(entity.posZ);
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder bufferbuilder = tessellator.getBuffer();
         GlStateManager.disableCull();
         GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
         GlStateManager.alphaFunc(516, 0.1F);
         double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks;
         double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks;
         double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks;
         int l = MathHelper.floor(d1);
         int i1 = 5;
         if (Config.isRainFancy()) {
            i1 = 10;
         }

         int j1 = -1;
         float f = (float)this.rendererUpdateCount + partialTicks;
         bufferbuilder.setTranslation(-d0, -d1, -d2);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for (int k1 = k - i1; k1 <= k + i1; k1++) {
            for (int l1 = i - i1; l1 <= i + i1; l1++) {
               int i2 = (k1 - k + 16) * 32 + l1 - i + 16;
               double d3 = (double)this.rainXCoords[i2] * 0.5;
               double d4 = (double)this.rainYCoords[i2] * 0.5;
               blockpos$mutableblockpos.setPos(l1, 0, k1);
               Biome biome = world.getBiome(blockpos$mutableblockpos);
               if (biome.canRain() || biome.getEnableSnow()) {
                  int j2 = world.getPrecipitationHeight(blockpos$mutableblockpos).getY();
                  int k2 = j - i1;
                  int l2 = j + i1;
                  if (k2 < j2) {
                     k2 = j2;
                  }

                  if (l2 < j2) {
                     l2 = j2;
                  }

                  int i3 = j2;
                  if (j2 < l) {
                     i3 = l;
                  }

                  if (k2 != l2) {
                     this.random.setSeed((long)l1 * (long)l1 * 3121L + (long)l1 * 45238971L ^ (long)k1 * (long)k1 * 418711L + (long)k1 * 13761L);
                     blockpos$mutableblockpos.setPos(l1, k2, k1);
                     float f1 = biome.getFloatTemperature(blockpos$mutableblockpos);
                     if (world.getBiomeProvider().getTemperatureAtHeight(f1, j2) >= 0.15F) {
                        if (j1 != 0) {
                           if (j1 >= 0) {
                              tessellator.draw();
                           }

                           j1 = 0;
                           this.mc.getTextureManager().bindTexture(RAIN_TEXTURES);
                           bufferbuilder.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                        }

                        double d5 = -(
                              (double)(this.rendererUpdateCount + l1 * l1 * 3121 + l1 * 45238971 + k1 * k1 * 418711 + k1 * 13761 & 31) + (double)partialTicks
                           )
                           / 32.0
                           * (3.0 + this.random.nextDouble());
                        double d6 = (double)((float)l1 + 0.5F) - entity.posX;
                        double d7 = (double)((float)k1 + 0.5F) - entity.posZ;
                        float f2 = MathHelper.sqrt(d6 * d6 + d7 * d7) / (float)i1;
                        float f3 = ((1.0F - f2 * f2) * 0.5F + 0.5F) * f5;
                        blockpos$mutableblockpos.setPos(l1, i3, k1);
                        int j3 = world.getCombinedLight(blockpos$mutableblockpos, 0);
                        int k3 = j3 >> 16 & 65535;
                        int l3 = j3 & 65535;
                        bufferbuilder.pos((double)l1 - d3 + 0.5, (double)l2, (double)k1 - d4 + 0.5)
                           .tex(0.0, (double)k2 * 0.25 + d5)
                           .color(1.0F, 1.0F, 1.0F, f3)
                           .lightmap(k3, l3)
                           .endVertex();
                        bufferbuilder.pos((double)l1 + d3 + 0.5, (double)l2, (double)k1 + d4 + 0.5)
                           .tex(1.0, (double)k2 * 0.25 + d5)
                           .color(1.0F, 1.0F, 1.0F, f3)
                           .lightmap(k3, l3)
                           .endVertex();
                        bufferbuilder.pos((double)l1 + d3 + 0.5, (double)k2, (double)k1 + d4 + 0.5)
                           .tex(1.0, (double)l2 * 0.25 + d5)
                           .color(1.0F, 1.0F, 1.0F, f3)
                           .lightmap(k3, l3)
                           .endVertex();
                        bufferbuilder.pos((double)l1 - d3 + 0.5, (double)k2, (double)k1 - d4 + 0.5)
                           .tex(0.0, (double)l2 * 0.25 + d5)
                           .color(1.0F, 1.0F, 1.0F, f3)
                           .lightmap(k3, l3)
                           .endVertex();
                     } else {
                        if (j1 != 1) {
                           if (j1 >= 0) {
                              tessellator.draw();
                           }

                           j1 = 1;
                           this.mc.getTextureManager().bindTexture(SNOW_TEXTURES);
                           bufferbuilder.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                        }

                        double d8 = (double)(-((float)(this.rendererUpdateCount & 511) + partialTicks) / 512.0F);
                        double d9 = this.random.nextDouble() + (double)f * 0.01 * (double)((float)this.random.nextGaussian());
                        double d10 = this.random.nextDouble() + (double)(f * (float)this.random.nextGaussian()) * 0.001;
                        double d11 = (double)((float)l1 + 0.5F) - entity.posX;
                        double d12 = (double)((float)k1 + 0.5F) - entity.posZ;
                        float f6 = MathHelper.sqrt(d11 * d11 + d12 * d12) / (float)i1;
                        float f4 = ((1.0F - f6 * f6) * 0.3F + 0.5F) * f5;
                        blockpos$mutableblockpos.setPos(l1, i3, k1);
                        int i4 = (world.getCombinedLight(blockpos$mutableblockpos, 0) * 3 + 15728880) / 4;
                        int j4 = i4 >> 16 & 65535;
                        int k4 = i4 & 65535;
                        bufferbuilder.pos((double)l1 - d3 + 0.5, (double)l2, (double)k1 - d4 + 0.5)
                           .tex(0.0 + d9, (double)k2 * 0.25 + d8 + d10)
                           .color(1.0F, 1.0F, 1.0F, f4)
                           .lightmap(j4, k4)
                           .endVertex();
                        bufferbuilder.pos((double)l1 + d3 + 0.5, (double)l2, (double)k1 + d4 + 0.5)
                           .tex(1.0 + d9, (double)k2 * 0.25 + d8 + d10)
                           .color(1.0F, 1.0F, 1.0F, f4)
                           .lightmap(j4, k4)
                           .endVertex();
                        bufferbuilder.pos((double)l1 + d3 + 0.5, (double)k2, (double)k1 + d4 + 0.5)
                           .tex(1.0 + d9, (double)l2 * 0.25 + d8 + d10)
                           .color(1.0F, 1.0F, 1.0F, f4)
                           .lightmap(j4, k4)
                           .endVertex();
                        bufferbuilder.pos((double)l1 - d3 + 0.5, (double)k2, (double)k1 - d4 + 0.5)
                           .tex(0.0 + d9, (double)l2 * 0.25 + d8 + d10)
                           .color(1.0F, 1.0F, 1.0F, f4)
                           .lightmap(j4, k4)
                           .endVertex();
                     }
                  }
               }
            }
         }

         if (j1 >= 0) {
            tessellator.draw();
         }

         bufferbuilder.setTranslation(0.0, 0.0, 0.0);
         GlStateManager.enableCull();
         GlStateManager.disableBlend();
         GlStateManager.alphaFunc(516, 0.1F);
         this.disableLightmap();
      }
   }

   public void setupOverlayRendering() {
      ScaledResolution scaledresolution = new ScaledResolution(this.mc);
      GlStateManager.clear(256);
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.ortho(0.0, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0, 1000.0, 3000.0);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.translate(0.0F, 0.0F, -2000.0F);
   }

   public void setupOverlayRendering(int factor) {
      ScaledResolution scaledresolution = new ScaledResolution(this.mc, factor);
      GlStateManager.clear(256);
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.ortho(0.0, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0, 1000.0, 3000.0);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.translate(0.0F, 0.0F, -2000.0F);
   }

   private void updateFogColor(float partialTicks) {
      World world = this.mc.world;
      Entity entity = this.mc.getRenderViewEntity();
      float f = 0.25F + 0.75F * (float)this.mc.gameSettings.renderDistanceChunks / 32.0F;
      f = 1.0F - (float)Math.pow((double)f, 0.25);
      Vec3d vec3d = world.getSkyColor(this.mc.getRenderViewEntity(), partialTicks);
      vec3d = CustomColors.getWorldSkyColor(vec3d, world, this.mc.getRenderViewEntity(), partialTicks);
      float f1 = (float)vec3d.xCoord;
      float f2 = (float)vec3d.yCoord;
      float f3 = (float)vec3d.zCoord;
      Vec3d vec3d1 = world.getFogColor(partialTicks);
      vec3d1 = CustomColors.getWorldFogColor(vec3d1, world, this.mc.getRenderViewEntity(), partialTicks);
      this.fogColorRed = (float)vec3d1.xCoord;
      this.fogColorGreen = (float)vec3d1.yCoord;
      this.fogColorBlue = (float)vec3d1.zCoord;
      if (this.mc.gameSettings.renderDistanceChunks >= 4) {
         double d0 = MathHelper.sin(world.getCelestialAngleRadians(partialTicks)) > 0.0F ? -1.0 : 1.0;
         Vec3d vec3d2 = new Vec3d(d0, 0.0, 0.0);
         float f5 = (float)entity.getLook(partialTicks).dotProduct(vec3d2);
         if (f5 < 0.0F) {
            f5 = 0.0F;
         }

         if (f5 > 0.0F) {
            float[] afloat = world.provider.calcSunriseSunsetColors(world.getCelestialAngle(partialTicks), partialTicks);
            if (afloat != null) {
               f5 *= afloat[3];
               this.fogColorRed = this.fogColorRed * (1.0F - f5) + afloat[0] * f5;
               this.fogColorGreen = this.fogColorGreen * (1.0F - f5) + afloat[1] * f5;
               this.fogColorBlue = this.fogColorBlue * (1.0F - f5) + afloat[2] * f5;
            }
         }
      }

      this.fogColorRed = this.fogColorRed + (f1 - this.fogColorRed) * f;
      this.fogColorGreen = this.fogColorGreen + (f2 - this.fogColorGreen) * f;
      this.fogColorBlue = this.fogColorBlue + (f3 - this.fogColorBlue) * f;
      float f8 = world.getRainStrength(partialTicks);
      if (f8 > 0.0F) {
         float f4 = 1.0F - f8 * 0.5F;
         float f10 = 1.0F - f8 * 0.4F;
         this.fogColorRed *= f4;
         this.fogColorGreen *= f4;
         this.fogColorBlue *= f10;
      }

      float f9 = world.getThunderStrength(partialTicks);
      if (f9 > 0.0F) {
         float f11 = 1.0F - f9 * 0.5F;
         this.fogColorRed *= f11;
         this.fogColorGreen *= f11;
         this.fogColorBlue *= f11;
      }

      IBlockState iblockstate1 = ActiveRenderInfo.getBlockStateAtEntityViewpoint(this.mc.world, entity, partialTicks);
      if (this.cloudFog) {
         Vec3d vec3d4 = world.getCloudColour(partialTicks);
         this.fogColorRed = (float)vec3d4.xCoord;
         this.fogColorGreen = (float)vec3d4.yCoord;
         this.fogColorBlue = (float)vec3d4.zCoord;
      } else if (Reflector.ForgeBlock_getFogColor.exists()) {
         Vec3d vec3d5 = ActiveRenderInfo.projectViewFromEntity(entity, (double)partialTicks);
         BlockPos blockpos = new BlockPos(vec3d5);
         IBlockState iblockstate = this.mc.world.getBlockState(blockpos);
         Vec3d vec3d3 = (Vec3d)Reflector.call(
            iblockstate.getBlock(),
            Reflector.ForgeBlock_getFogColor,
            this.mc.world,
            blockpos,
            iblockstate,
            entity,
            new Vec3d((double)this.fogColorRed, (double)this.fogColorGreen, (double)this.fogColorBlue),
            partialTicks
         );
         this.fogColorRed = (float)vec3d3.xCoord;
         this.fogColorGreen = (float)vec3d3.yCoord;
         this.fogColorBlue = (float)vec3d3.zCoord;
      } else if (iblockstate1.getMaterial() == Material.WATER) {
         float f12 = 0.0F;
         if (entity instanceof EntityLivingBase) {
            f12 = (float)EnchantmentHelper.getRespirationModifier((EntityLivingBase)entity) * 0.2F;
            if (((EntityLivingBase)entity).isPotionActive(MobEffects.WATER_BREATHING)) {
               f12 = f12 * 0.3F + 0.6F;
            }
         }

         this.fogColorRed = 0.02F + f12;
         this.fogColorGreen = 0.02F + f12;
         this.fogColorBlue = 0.2F + f12;
         Vec3d vec3d7 = CustomColors.getUnderwaterColor(
            this.mc.world, this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().posY + 1.0, this.mc.getRenderViewEntity().posZ
         );
         if (vec3d7 != null) {
            this.fogColorRed = (float)vec3d7.xCoord;
            this.fogColorGreen = (float)vec3d7.yCoord;
            this.fogColorBlue = (float)vec3d7.zCoord;
         }
      } else if (iblockstate1.getMaterial() == Material.LAVA) {
         this.fogColorRed = 0.6F;
         this.fogColorGreen = 0.1F;
         this.fogColorBlue = 0.0F;
         Vec3d vec3d6 = CustomColors.getUnderlavaColor(
            this.mc.world, this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().posY + 1.0, this.mc.getRenderViewEntity().posZ
         );
         if (vec3d6 != null) {
            this.fogColorRed = (float)vec3d6.xCoord;
            this.fogColorGreen = (float)vec3d6.yCoord;
            this.fogColorBlue = (float)vec3d6.zCoord;
         }
      }

      float f13 = this.fogColor2 + (this.fogColor1 - this.fogColor2) * partialTicks;
      this.fogColorRed *= f13;
      this.fogColorGreen *= f13;
      this.fogColorBlue *= f13;
      double d1 = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks) * world.provider.getVoidFogYFactor();
      if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(MobEffects.BLINDNESS)) {
         int i = ((EntityLivingBase)entity).getActivePotionEffect(MobEffects.BLINDNESS).getDuration();
         if (i < 20) {
            d1 *= (double)(1.0F - (float)i / 20.0F);
         } else {
            d1 = 0.0;
         }
      }

      if (d1 < 1.0) {
         if (d1 < 0.0) {
            d1 = 0.0;
         }

         d1 *= d1;
         this.fogColorRed = (float)((double)this.fogColorRed * d1);
         this.fogColorGreen = (float)((double)this.fogColorGreen * d1);
         this.fogColorBlue = (float)((double)this.fogColorBlue * d1);
      }

      if (this.bossColorModifier > 0.0F) {
         float f14 = this.bossColorModifierPrev + (this.bossColorModifier - this.bossColorModifierPrev) * partialTicks;
         this.fogColorRed = this.fogColorRed * (1.0F - f14) + this.fogColorRed * 0.7F * f14;
         this.fogColorGreen = this.fogColorGreen * (1.0F - f14) + this.fogColorGreen * 0.6F * f14;
         this.fogColorBlue = this.fogColorBlue * (1.0F - f14) + this.fogColorBlue * 0.6F * f14;
      }

      if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(MobEffects.NIGHT_VISION)) {
         float f15 = this.getNightVisionBrightness((EntityLivingBase)entity, partialTicks);
         float f6 = 1.0F / this.fogColorRed;
         if (f6 > 1.0F / this.fogColorGreen) {
            f6 = 1.0F / this.fogColorGreen;
         }

         if (f6 > 1.0F / this.fogColorBlue) {
            f6 = 1.0F / this.fogColorBlue;
         }

         this.fogColorRed = this.fogColorRed * (1.0F - f15) + this.fogColorRed * f6 * f15;
         this.fogColorGreen = this.fogColorGreen * (1.0F - f15) + this.fogColorGreen * f6 * f15;
         this.fogColorBlue = this.fogColorBlue * (1.0F - f15) + this.fogColorBlue * f6 * f15;
      }

      if (this.mc.gameSettings.anaglyph) {
         float f16 = (this.fogColorRed * 30.0F + this.fogColorGreen * 59.0F + this.fogColorBlue * 11.0F) / 100.0F;
         float f17 = (this.fogColorRed * 30.0F + this.fogColorGreen * 70.0F) / 100.0F;
         float f7 = (this.fogColorRed * 30.0F + this.fogColorBlue * 70.0F) / 100.0F;
         this.fogColorRed = f16;
         this.fogColorGreen = f17;
         this.fogColorBlue = f7;
      }

      GlStateManager.clearColor(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 0.0F);
      if (Reflector.EntityViewRenderEvent_FogColors_Constructor.exists()) {
         Object object = Reflector.newInstance(
            Reflector.EntityViewRenderEvent_FogColors_Constructor,
            this,
            entity,
            iblockstate1,
            partialTicks,
            this.fogColorRed,
            this.fogColorGreen,
            this.fogColorBlue
         );
         Reflector.postForgeBusEvent(object);
         this.fogColorRed = Reflector.callFloat(object, Reflector.EntityViewRenderEvent_FogColors_getRed);
         this.fogColorGreen = Reflector.callFloat(object, Reflector.EntityViewRenderEvent_FogColors_getGreen);
         this.fogColorBlue = Reflector.callFloat(object, Reflector.EntityViewRenderEvent_FogColors_getBlue);
      }

      float[] RECOL_FOG = WorldRender.get.getSkyColorRGB(this.fogColorRed, this.fogColorGreen, this.fogColorBlue);
      this.fogColorRed = RECOL_FOG[0];
      this.fogColorGreen = RECOL_FOG[1];
      this.fogColorBlue = RECOL_FOG[2];
      Shaders.setClearColor(
         this.cancelFog() ? 0.0F : this.fogColorRed, this.cancelFog() ? 0.0F : this.fogColorGreen, this.cancelFog() ? 0.0F : this.fogColorBlue, 0.0F
      );
   }

   private void setupFog(int startCoords, float partialTicks) {
      this.fogStandard = false;
      Entity entity = this.mc.getRenderViewEntity();
      this.func_191514_d(false);
      GlStateManager.glNormal3f(0.0F, -1.0F, 0.0F);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      IBlockState iblockstate = ActiveRenderInfo.getBlockStateAtEntityViewpoint(this.mc.world, entity, partialTicks);
      float f = -1.0F;
      if (Reflector.ForgeHooksClient_getFogDensity.exists()) {
         f = Reflector.callFloat(Reflector.ForgeHooksClient_getFogDensity, this, entity, iblockstate, partialTicks, 0.1F);
      }

      if (f >= 0.0F) {
         GlStateManager.setFogDensity(f);
      } else if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(MobEffects.BLINDNESS)) {
         float f2 = 5.0F;
         int i = ((EntityLivingBase)entity).getActivePotionEffect(MobEffects.BLINDNESS).getDuration();
         if (i < 20) {
            f2 = 5.0F + (this.farPlaneDistance - 5.0F) * (1.0F - (float)i / 20.0F);
         }

         if (Config.isShaders()) {
            Shaders.setFog(GlStateManager.FogMode.LINEAR);
         } else {
            GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
         }

         if (startCoords == -1) {
            GlStateManager.setFogStart(0.0F);
            GlStateManager.setFogEnd(f2 * 0.8F);
         } else {
            GlStateManager.setFogStart(f2 * 0.25F);
            GlStateManager.setFogEnd(f2);
         }

         if (GLContext.getCapabilities().GL_NV_fog_distance && Config.isFogFancy()) {
            GlStateManager.glFogi(34138, 34139);
         }
      } else if (this.cloudFog) {
         if (Config.isShaders()) {
            Shaders.setFog(GlStateManager.FogMode.EXP);
         } else {
            GlStateManager.setFog(GlStateManager.FogMode.EXP);
         }

         GlStateManager.setFogDensity(0.1F);
      } else if (iblockstate.getMaterial() == Material.WATER) {
         if (NoRender.get.actived && NoRender.get.LiquidOverlay.getBool()) {
            return;
         }

         if (Config.isShaders()) {
            Shaders.setFog(GlStateManager.FogMode.EXP);
         } else {
            GlStateManager.setFog(GlStateManager.FogMode.EXP);
         }

         if (entity instanceof EntityLivingBase) {
            if (((EntityLivingBase)entity).isPotionActive(MobEffects.WATER_BREATHING)) {
               GlStateManager.setFogDensity(0.01F);
            } else {
               GlStateManager.setFogDensity(0.1F - (float)EnchantmentHelper.getRespirationModifier((EntityLivingBase)entity) * 0.03F);
            }
         } else {
            GlStateManager.setFogDensity(0.1F);
         }

         if (Config.isClearWater()) {
            GlStateManager.setFogDensity(0.02F);
         }

         GlStateManager.setFogDensity(0.01F);
      } else if (iblockstate.getMaterial() == Material.LAVA) {
         if (NoRender.get.actived && NoRender.get.LiquidOverlay.getBool()) {
            return;
         }

         if (Config.isShaders()) {
            Shaders.setFog(GlStateManager.FogMode.EXP);
         } else {
            GlStateManager.setFog(GlStateManager.FogMode.EXP);
         }

         GlStateManager.setFogDensity(2.0F);
      } else {
         float f1 = this.farPlaneDistance;
         this.fogStandard = true;
         if (Config.isShaders()) {
            Shaders.setFog(GlStateManager.FogMode.LINEAR);
         } else {
            GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
         }

         if (startCoords == -1) {
            GlStateManager.setFogStart(0.0F);
            GlStateManager.setFogEnd(f1);
         } else {
            GlStateManager.setFogStart(f1 * Config.getFogStart());
            GlStateManager.setFogEnd(f1);
         }

         if (GLContext.getCapabilities().GL_NV_fog_distance) {
            if (Config.isFogFancy()) {
               GlStateManager.glFogi(34138, 34139);
            }

            if (Config.isFogFast()) {
               GlStateManager.glFogi(34138, 34140);
            }
         }

         if (this.mc.world.provider.doesXZShowFog((int)entity.posX, (int)entity.posZ) || this.mc.ingameGUI.getBossOverlay().shouldCreateFog()) {
            GlStateManager.setFogStart(f1 * 0.05F);
            GlStateManager.setFogEnd(f1);
         }

         if (Reflector.ForgeHooksClient_onFogRender.exists()) {
            Reflector.callVoid(Reflector.ForgeHooksClient_onFogRender, this, entity, iblockstate, partialTicks, startCoords, f1);
         }
      }

      GlStateManager.enableColorMaterial();
      GlStateManager.enableFog();
      GlStateManager.colorMaterial(1028, 4608);
   }

   public void func_191514_d(boolean p_191514_1_) {
      if (p_191514_1_) {
         GlStateManager.glFog(2918, this.setFogColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
      } else {
         GlStateManager.glFog(
            2918,
            this.setFogColorBuffer(
               this.cancelFog() ? 0.0F : this.fogColorRed,
               this.cancelFog() ? 0.0F : this.fogColorGreen,
               this.cancelFog() ? 0.0F : this.fogColorBlue,
               this.cancelFog() ? 0.0F : 1.0F
            )
         );
      }
   }

   private FloatBuffer setFogColorBuffer(float red, float green, float blue, float alpha) {
      if (Config.isShaders()) {
         Shaders.setFogColor(red, green, blue);
      }

      this.fogColorBuffer.clear();
      this.fogColorBuffer.put(red).put(green).put(blue).put(alpha);
      this.fogColorBuffer.flip();
      return this.fogColorBuffer;
   }

   public void func_190564_k() {
      this.field_190566_ab = null;
      this.theMapItemRenderer.clearLoadedMaps();
   }

   public MapItemRenderer getMapItemRenderer() {
      return this.theMapItemRenderer;
   }

   private void waitForServerThread() {
      this.serverWaitTimeCurrent = 0;
      if (!Config.isSmoothWorld() || !Config.isSingleProcessor()) {
         this.lastServerTime = 0L;
         this.lastServerTicks = 0;
      } else if (this.mc.isIntegratedServerRunning()) {
         IntegratedServer integratedserver = this.mc.getIntegratedServer();
         if (integratedserver != null) {
            boolean flag = this.mc.isGamePaused();
            if (!flag && !(this.mc.currentScreen instanceof GuiDownloadTerrain)) {
               if (this.serverWaitTime > 0) {
                  Lagometer.timerServer.start();
                  Config.sleep((long)this.serverWaitTime);
                  Lagometer.timerServer.end();
                  this.serverWaitTimeCurrent = this.serverWaitTime;
               }

               long i = System.nanoTime() / 1000000L;
               if (this.lastServerTime != 0L && this.lastServerTicks != 0) {
                  long j = i - this.lastServerTime;
                  if (j < 0L) {
                     this.lastServerTime = i;
                     j = 0L;
                  }

                  if (j >= 50L) {
                     this.lastServerTime = i;
                     int k = integratedserver.getTickCounter();
                     int l = k - this.lastServerTicks;
                     if (l < 0) {
                        this.lastServerTicks = k;
                        l = 0;
                     }

                     if (l < 1 && this.serverWaitTime < 100) {
                        this.serverWaitTime += 2;
                     }

                     if (l > 1 && this.serverWaitTime > 0) {
                        this.serverWaitTime--;
                     }

                     this.lastServerTicks = k;
                  }
               } else {
                  this.lastServerTime = i;
                  this.lastServerTicks = integratedserver.getTickCounter();
                  this.avgServerTickDiff = 1.0F;
                  this.avgServerTimeDiff = 50.0F;
               }
            } else {
               if (this.mc.currentScreen instanceof GuiDownloadTerrain) {
                  Config.sleep(20L);
               }

               this.lastServerTime = 0L;
               this.lastServerTicks = 0;
            }
         }
      }
   }

   private void frameInit() {
      if (!this.initialized) {
         TextureUtils.registerResourceListener();
         if (Config.getBitsOs() == 64 && Config.getBitsJre() == 32) {
            Config.setNotify64BitJava(true);
         }

         this.initialized = true;
      }

      Config.checkDisplayMode();
      World world = this.mc.world;
      if (world != null && Config.isNotify64BitJava()) {
         Config.setNotify64BitJava(false);
         TextComponentString textcomponentstring1 = new TextComponentString(I18n.format("of.message.java64Bit"));
         this.mc.ingameGUI.getChatGUI().printChatMessage(textcomponentstring1);
      }

      if (this.mc.currentScreen instanceof GuiMainMenu) {
         this.updateMainMenu((GuiMainMenu)this.mc.currentScreen);
      }

      if (this.updatedWorld != world) {
         RandomMobs.worldChanged(this.updatedWorld, world);
         Config.updateThreadPriorities();
         this.lastServerTime = 0L;
         this.lastServerTicks = 0;
         this.updatedWorld = world;
      }

      if (WorldRender.get.isActived() && WorldRender.get.AntiAliasing.getBool()) {
         int level = 2;
         String var3 = WorldRender.get.AAFactor.getMode();
         switch (var3) {
            case "AA-x2":
               int var8 = 2;
               this.setFxaaShader(2);
               break;
            case "AA-x4":
               int var7 = 4;
               this.setFxaaShader(4);
               break;
            case "AA-x8":
               int var6 = 8;
               this.setFxaaShader(8);
         }
      } else if (!this.setFxaaShader(Shaders.configAntialiasingLevel)) {
         Shaders.configAntialiasingLevel = 0;
      }
   }

   private void frameFinish(ScaledResolution sr, boolean pure) {
      if (this.mc.world != null) {
         long i = System.currentTimeMillis();
         if (i > this.lastErrorCheckTimeMs + 10000L) {
            this.lastErrorCheckTimeMs = i;
            int j = GlStateManager.glGetError();
            if (j != 0) {
               String s = GLU.gluErrorString(j);
               TextComponentString textcomponentstring = new TextComponentString(I18n.format("of.message.openglError", j, s));
               this.mc.ingameGUI.getChatGUI().printChatMessage(textcomponentstring);
               System.out.println(textcomponentstring);
            }
         }
      }
   }

   private void updateMainMenu(GuiMainMenu p_updateMainMenu_1_) {
      try {
         String s = null;
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(new Date());
         int i = calendar.get(5);
         int j = calendar.get(2) + 1;
         if (i == 8 && j == 4) {
            s = "Happy birthday, OptiFine!";
         }

         if (i == 14 && j == 8) {
            s = "Happy birthday, sp614x!";
         }

         if (s == null) {
            return;
         }

         Reflector.setFieldValue(p_updateMainMenu_1_, Reflector.GuiMainMenu_splashText, s);
      } catch (Throwable var6) {
      }
   }

   public boolean setFxaaShader(int p_setFxaaShader_1_) {
      if (!OpenGlHelper.isFramebufferEnabled()) {
         return false;
      } else if (this.theShaderGroup != null
         && this.theShaderGroup != this.fxaaShaders[2]
         && this.theShaderGroup != this.fxaaShaders[4]
         && this.theShaderGroup != this.fxaaShaders[8]) {
         return true;
      } else if (p_setFxaaShader_1_ != 2 && p_setFxaaShader_1_ != 4 && p_setFxaaShader_1_ != 8) {
         if (this.theShaderGroup == null) {
            return true;
         } else {
            this.theShaderGroup.deleteShaderGroup();
            this.theShaderGroup = null;
            return true;
         }
      } else if (this.theShaderGroup != null && this.theShaderGroup == this.fxaaShaders[p_setFxaaShader_1_]) {
         return true;
      } else if (this.mc.world == null) {
         return true;
      } else {
         this.loadShader(new ResourceLocation("shaders/post/fxaa_of_" + p_setFxaaShader_1_ + "x.json"));
         this.fxaaShaders[p_setFxaaShader_1_] = this.theShaderGroup;
         return this.useShader;
      }
   }

   private void checkLoadVisibleChunks(
      Entity p_checkLoadVisibleChunks_1_, float p_checkLoadVisibleChunks_2_, ICamera p_checkLoadVisibleChunks_3_, boolean p_checkLoadVisibleChunks_4_
   ) {
      int i = 201435902;
      if (this.loadVisibleChunks) {
         this.loadVisibleChunks = false;
         this.loadAllVisibleChunks(p_checkLoadVisibleChunks_1_, (double)p_checkLoadVisibleChunks_2_, p_checkLoadVisibleChunks_3_, p_checkLoadVisibleChunks_4_);
         this.mc.ingameGUI.getChatGUI().deleteChatLine(i);
      }

      if (Keyboard.isKeyDown(61) && Keyboard.isKeyDown(38)) {
         if (this.mc.gameSettings.field_194146_ao.getKeyCode() == 38) {
            if (this.mc.currentScreen instanceof GuiScreenAdvancements) {
               this.mc.displayGuiScreen(null);
            }

            while (Keyboard.next()) {
            }
         }

         if (this.mc.currentScreen != null) {
            return;
         }

         this.loadVisibleChunks = true;
         TextComponentString textcomponentstring = new TextComponentString(I18n.format("of.message.loadingVisibleChunks"));
         this.mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(textcomponentstring, i);
         Reflector.Minecraft_actionKeyF3.setValue(this.mc, Boolean.TRUE);
      }
   }

   private void loadAllVisibleChunks(
      Entity p_loadAllVisibleChunks_1_, double p_loadAllVisibleChunks_2_, ICamera p_loadAllVisibleChunks_4_, boolean p_loadAllVisibleChunks_5_
   ) {
      RenderGlobal renderglobal = Config.getRenderGlobal();
      int i = renderglobal.getCountLoadedChunks();
      long j = System.currentTimeMillis();
      Config.dbg("Loading visible chunks");
      long k = System.currentTimeMillis() + 5000L;
      int l = 0;
      boolean flag = false;

      do {
         flag = false;

         for (int i1 = 0; i1 < 100; i1++) {
            renderglobal.displayListEntitiesDirty = true;
            renderglobal.setupTerrain(
               p_loadAllVisibleChunks_1_, p_loadAllVisibleChunks_2_, p_loadAllVisibleChunks_4_, this.frameCount++, p_loadAllVisibleChunks_5_
            );
            if (!renderglobal.hasNoChunkUpdates()) {
               flag = true;
            }

            l += renderglobal.getCountChunksToUpdate();
            renderglobal.updateChunks(System.nanoTime() + 1000000000L);
            l -= renderglobal.getCountChunksToUpdate();
         }

         if (renderglobal.getCountLoadedChunks() != i) {
            flag = true;
            i = renderglobal.getCountLoadedChunks();
         }

         if (System.currentTimeMillis() > k) {
            Config.log("Chunks loaded: " + l);
            k = System.currentTimeMillis() + 5000L;
         }
      } while (flag);

      Config.log("Chunks loaded: " + l);
      Config.log("Finished loading visible chunks");
      RenderChunk.renderChunksUpdated = 0;
   }

   public static void drawNameplate(
      FontRenderer fontRendererIn,
      String str,
      float x,
      float y,
      float z,
      int verticalShift,
      float viewerYaw,
      float viewerPitch,
      boolean isThirdPersonFrontal,
      boolean isSneaking
   ) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y, z);
      GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(-viewerYaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate((float)(isThirdPersonFrontal ? -1 : 1) * viewerPitch, 1.0F, 0.0F, 0.0F);
      GlStateManager.scale(-0.025F, -0.025F, 0.025F);
      GlStateManager.disableLighting();
      GlStateManager.depthMask(false);
      if (!isSneaking) {
         GlStateManager.disableDepth();
      }

      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      int i = fontRendererIn.getStringWidth(str) / 2;
      GlStateManager.disableTexture2D();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
      bufferbuilder.pos((double)(-i - 1), (double)(-1 + verticalShift), 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
      bufferbuilder.pos((double)(-i - 1), (double)(8 + verticalShift), 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
      bufferbuilder.pos((double)(i + 1), (double)(8 + verticalShift), 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
      bufferbuilder.pos((double)(i + 1), (double)(-1 + verticalShift), 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
      tessellator.draw();
      GlStateManager.enableTexture2D();
      if (!isSneaking) {
         fontRendererIn.drawString(str, (float)(-fontRendererIn.getStringWidth(str) / 2), (double)verticalShift, 553648127);
         GlStateManager.enableDepth();
      }

      GlStateManager.depthMask(true);
      fontRendererIn.drawString(str, (float)(-fontRendererIn.getStringWidth(str) / 2), (double)verticalShift, isSneaking ? 553648127 : -1);
      GlStateManager.enableLighting();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   public void func_190565_a(ItemStack p_190565_1_) {
      this.field_190566_ab = p_190565_1_;
      this.field_190567_ac = 40;
      this.field_190568_ad = this.random.nextFloat() * 2.0F - 1.0F;
      this.field_190569_ae = this.random.nextFloat() * 2.0F - 1.0F;
   }

   private void func_190563_a(int p_1905631, int p_1905632, float p_1905633) {
      if (this.field_190566_ab != null && this.field_190567_ac > 0) {
         int i = 40 - this.field_190567_ac;
         float f = ((float)i + p_1905633) / 40.0F;
         float f1 = f * f;
         float f2 = f * f1;
         float f3 = 10.25F * f2 * f1 + -24.95F * f1 * f1 + 25.5F * f2 + -13.8F * f1 + 4.0F * f;
         float f4 = f3 * (float) Math.PI;
         float f5 = this.field_190568_ad * (float)(p_1905631 / 4);
         float f6 = this.field_190569_ae * (float)(p_1905632 / 4);
         GlStateManager.enableAlpha();
         GlStateManager.pushMatrix();
         GlStateManager.pushAttrib();
         GlStateManager.enableDepth();
         GlStateManager.disableCull();
         RenderHelper.enableStandardItemLighting();
         GlStateManager.translate(
            (float)(p_1905631 / 2) + f5 * MathHelper.abs(MathHelper.sin(f4 * 2.0F)),
            (float)(p_1905632 / 2) + f6 * MathHelper.abs(MathHelper.sin(f4 * 2.0F)),
            -50.0F
         );
         float f7 = 50.0F + 175.0F * MathHelper.sin(f4);
         GlStateManager.scale(f7, -f7, f7);
         GlStateManager.rotate(900.0F * MathHelper.abs(MathHelper.sin(f4)), 0.0F, 1.0F, 0.0F);
         GlStateManager.rotate(6.0F * MathHelper.cos(f * 8.0F), 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate(6.0F * MathHelper.cos(f * 8.0F), 0.0F, 0.0F, 1.0F);
         this.mc.getRenderItem().renderItem(this.field_190566_ab, ItemCameraTransforms.TransformType.FIXED);
         GlStateManager.popAttrib();
         GlStateManager.popMatrix();
         RenderHelper.disableStandardItemLighting();
         GlStateManager.enableCull();
         GlStateManager.disableDepth();
      }
   }

   public void runCfgSaveAnim() {
      if (this.mc.world != null) {
         this.cfgLinearTime.reset();
         this.cfgPC.setAnim(0.0F);
         this.cfgPC.to = 1.0F;
         this.cfgPC.speed = 0.02F;
      }
   }

   private ResourceLocation getResourceByPercent(float pc01) {
      int framesCount = Client.CONFIG_SAVE_ICONS.size();
      return Client.CONFIG_SAVE_ICONS.get((int)MathUtils.clamp((float)framesCount * (pc01 % 1.0F), 0.0F, (float)framesCount));
   }

   void drawConfigSaveAnimation(float alphaPC, ScaledResolution sr) {
      if (Minecraft.player != null && sr != null && alphaPC != 0.0F) {
         if (this.cfgPC.to == 1.0F || !this.cfgLinearTime.hasReached(3000.0) && (double)(alphaPC *= this.cfgPC.getAnim()) > 0.1) {
            this.setupOverlayRendering();
            float timePC = MathUtils.clamp((float)this.cfgLinearTime.getTime() / 1400.0F, 0.0F, 1.0F);
            if (timePC == 1.0F) {
               this.cfgPC.to = 0.0F;
               this.cfgPC.speed = 0.04F;
            }

            if ((double)(alphaPC = alphaPC * this.cfgPC.getAnim()) > 0.01) {
               int fps = 60;
               this.mc.getTextureManager().bindTexture(this.getResourceByPercent((float)this.cfgLinearTime.getTime() / 1300.0F % 1.0F));
               Tessellator tessellator = Tessellator.getInstance();
               BufferBuilder buffer = tessellator.getBuffer();
               GlStateManager.resetColor();
               GlStateManager.enableBlend();
               GlStateManager.enableTexture2D();
               float w = 62.0F;
               float x = (float)sr.getScaledWidth() - w;
               float h = 62.0F;
               float y = (float)sr.getScaledHeight() - 120.0F - h;
               int c = ColorUtils.swapAlpha(ColorUtils.getColor((int)(255.0F * alphaPC)), 255.0F * alphaPC);
               int c2 = ColorUtils.swapAlpha(ColorUtils.getColor((int)(255.0F * alphaPC)), 105.0F * alphaPC);
               buffer.begin(9, DefaultVertexFormats.POSITION_TEX_COLOR);
               buffer.pos((double)x, (double)y).tex(0.0, 0.0).color(c).endVertex();
               buffer.pos((double)x, (double)(y + h)).tex(0.0, 1.0).color(c).endVertex();
               buffer.pos((double)(x + w), (double)(y + h)).tex(1.0, 1.0).color(c).endVertex();
               buffer.pos((double)(x + w), (double)y).tex(1.0, 0.0).color(c).endVertex();
               GL11.glPushMatrix();
               RenderUtils.customScaledObject2D(
                  x, y, w, h, this.cfgPC.to == 0.0F ? (float)MathUtils.easeOutBack((double)alphaPC) : (float)MathUtils.easeOutElastic((double)alphaPC)
               );
               GL11.glBlendFunc(770, 1);
               GL11.glEnable(3008);
               GL11.glAlphaFunc(516, 0.0F);
               GL11.glTexParameteri(3553, 10240, 9729);
               tessellator.draw();
               GL11.glTexParameteri(3553, 10240, 9728);
               GL11.glAlphaFunc(516, 0.1F);
               GL11.glBlendFunc(770, 771);
               if ((float)ColorUtils.getAlphaFromColor(c) >= 33.0F) {
                  if (this.cfgPC.to == 0.0F) {
                     RenderUtils.customScaledObject2D(x, y, w, h, alphaPC);
                  }

                  String save = "SAVE";
                  Fonts.stylesicons_24
                     .drawString(
                        "K",
                        x + w / 2.0F - Fonts.stylesicons_24.getStringWidth("K") / 3.0F - 0.5F,
                        y + h / 2.0F - Fonts.stylesicons_24.getHeight() / 2.0F - 2.0F,
                        c2
                     );
                  Fonts.mntsb_10
                     .drawString(
                        save,
                        x + w / 2.0F - Fonts.mntsb_10.getStringWidth(save) / 3.0F - 1.0F,
                        y + h / 2.0F - Fonts.stylesicons_24.getHeight() / 2.0F + 8.0F,
                        c2
                     );
               }

               GL11.glPopMatrix();
            }
         }
      }
   }

   public class RunnableRenderTile {
      TileEntity tile;
      Runnable render;

      RunnableRenderTile(TileEntity tile, Runnable render) {
         this.tile = tile;
         this.render = render;
      }
   }
}
