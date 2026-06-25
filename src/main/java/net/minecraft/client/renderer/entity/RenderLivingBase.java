package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.optifine.entity.model.CustomEntityModels;
import optifine.Config;
import optifine.Reflector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.BadTrip;
import ru.govno.client.module.modules.DistantAlpha;
import ru.govno.client.module.modules.EntityBox;
import ru.govno.client.module.modules.NameTags;
import ru.govno.client.module.modules.NoRender;
import ru.govno.client.module.modules.StormHVHHelper;
import ru.govno.client.module.modules.WallHack;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import shadersmod.client.Shaders;

public abstract class RenderLivingBase<T extends EntityLivingBase> extends Render<T> {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final DynamicTexture TEXTURE_BRIGHTNESS = new DynamicTexture(16, 16);
   public ModelBase mainModel;
   protected FloatBuffer brightnessBuffer = GLAllocation.createDirectFloatBuffer(4);
   protected List<LayerRenderer<T>> layerRenderers = Lists.newArrayList();
   public boolean renderMarker;
   public static float NAME_TAG_RANGE = 64.0F;
   public static float NAME_TAG_RANGE_SNEAK = 32.0F;
   public float renderLimbSwing;
   public float renderLimbSwingAmount;
   public float renderAgeInTicks;
   public float renderHeadYaw;
   public float renderScaleFactor;
   public static final boolean animateModelLiving = Boolean.getBoolean("animate.model.living");
   public static boolean silentMode;
   public static boolean unsetDistantAlphaLiving;
   public static boolean unsetRenderCape;
   public static boolean unsetRenderArmorLayerAdditions;

   public RenderLivingBase(RenderManager renderManagerIn, ModelBase modelBaseIn, float shadowSizeIn) {
      super(renderManagerIn);
      this.mainModel = modelBaseIn;
      this.shadowSize = shadowSizeIn;
   }

   public <V extends EntityLivingBase, U extends LayerRenderer<V>> boolean addLayer(U layer) {
      return this.layerRenderers.add((LayerRenderer)layer);
   }

   public ModelBase getMainModel() {
      return this.mainModel;
   }

   protected float interpolateRotation(float prevYawOffset, float yawOffset, float partialTicks) {
      float f = yawOffset - prevYawOffset;

      while (f < -180.0F) {
         f += 360.0F;
      }

      while (f >= 180.0F) {
         f -= 360.0F;
      }

      return prevYawOffset + partialTicks * f;
   }

   public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
      if (!StormHVHHelper.cancelRenderEntityLivingBase(entity)) {
         if (!Reflector.RenderLivingEvent_Pre_Constructor.exists()
            || !Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Pre_Constructor, entity, this, partialTicks, x, y, z)) {
            if (animateModelLiving) {
               entity.limbSwingAmount = 1.0F;
            }

            GlStateManager.pushMatrix();
            GlStateManager.disableCull();
            this.mainModel.swingProgress = this.getSwingProgress(entity, partialTicks);
            this.mainModel.isRiding = entity.isRiding();
            if (Reflector.ForgeEntity_shouldRiderSit.exists()) {
               this.mainModel.isRiding = entity.isRiding()
                  && entity.getRidingEntity() != null
                  && Reflector.callBoolean(entity.getRidingEntity(), Reflector.ForgeEntity_shouldRiderSit);
            }

            this.mainModel.isChild = entity.isChild();

            try {
               float f = this.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
               float f1 = this.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
               float f2 = f1 - f;
               if (this.mainModel.isRiding && entity.getRidingEntity() instanceof EntityLivingBase entitylivingbase) {
                  f = this.interpolateRotation(entitylivingbase.prevRenderYawOffset, entitylivingbase.renderYawOffset, partialTicks);
                  f2 = f1 - f;
                  float f3 = MathHelper.wrapDegrees(f2);
                  if (f3 < -85.0F) {
                     f3 = -85.0F;
                  }

                  if (f3 >= 85.0F) {
                     f3 = 85.0F;
                  }

                  f = f1 - f3;
                  if (f3 * f3 > 2500.0F) {
                     f += f3 * 0.2F;
                  }

                  f2 = f1 - f;
               }

               float f7 = entity instanceof EntityPlayerSP
                  ? entity.prevRotationPitchHead + (entity.rotationPitchHead - entity.prevRotationPitchHead) * partialTicks
                  : entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
               this.renderLivingAt(entity, x, y, z);
               float f8 = this.handleRotationFloat(entity, partialTicks);
               this.rotateCorpse(entity, f8, f, partialTicks);
               float f4 = this.prepareScale(entity, partialTicks);
               float f5 = 0.0F;
               float f6 = 0.0F;
               if (!entity.isRiding()) {
                  f5 = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
                  if (this.mainModel.isChild) {
                     f5 /= 2.0F;
                  }

                  f6 = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
                  if (entity.isChild()) {
                     f6 *= 3.0F;
                  }

                  if (f5 > 1.0F) {
                     f5 = 1.0F;
                  }
               }

               GlStateManager.enableAlpha();
               this.mainModel.setLivingAnimations(entity, f6, f5, partialTicks);
               this.mainModel.setRotationAngles(f6, f5, f8, f2, f7, f4, entity);
               if (CustomEntityModels.isActive()) {
                  this.renderLimbSwing = f6;
                  this.renderLimbSwingAmount = f5;
                  this.renderAgeInTicks = f8;
                  this.renderHeadYaw = f2;
                  this.renderScaleFactor = f4;
               }

               if (entity.isLay) {
                  double hs = (double)entity.getEyeHeight() / (entity.isChild() ? 2.4 : 1.0);
                  if (entity.isChild()) {
                     hs *= 5.3;
                  }

                  GL11.glTranslated(0.0, hs * (entity.isChild() ? 2.2 : 2.95), 0.0);
                  GL11.glTranslated(0.0, hs / 2.0, 0.0);
                  GL11.glRotated(90.0, 1.0, 0.0, 0.0);
                  GL11.glTranslated(0.0, -hs / 2.0, 0.0);
                  if (entity.isChild()) {
                     GL11.glTranslated(0.0, -hs * 1.8, 0.0);
                  }
               } else if (entity instanceof EntityOtherPlayerMP player && BadTrip.get.isFlattenPlayersHurt()) {
                  float smoothHurtPC = MathUtils.clamp((float)player.hurtTime - partialTicks, 0.0F, 9.0F) / 9.0F;
                  if (smoothHurtPC != 0.0F) {
                     float wavePC = MathUtils.valWave01(smoothHurtPC);
                     double animWavePC = 1.0 - MathUtils.easeOutBack(1.0 - MathUtils.easeInOutQuad((double)wavePC));
                     double appliedScaleY = 1.0 - animWavePC / 3.5;
                     double appliedScaleXZ = 1.0 + animWavePC / 4.0;
                     GL11.glTranslated(0.0, (double)(player.height / 1.75F), 0.0);
                     GL11.glScaled(appliedScaleXZ, appliedScaleY, appliedScaleXZ);
                     GL11.glTranslated(0.0, (double)(-player.height / 1.75F), 0.0);
                  }
               }

               if (this.renderOutlines) {
                  boolean flag1 = this.setScoreTeamColor(entity);
                  GlStateManager.enableColorMaterial();
                  GlStateManager.enableOutlineMode(this.getTeamColor(entity));
                  if (!unsetDistantAlphaLiving) {
                     DistantAlpha.get.onRenderEntity(entity, x, y, z, entityYaw, partialTicks, (byte)0);
                     DistantAlpha.get.onRenderEntity(entity, x, y, z, entityYaw, partialTicks, (byte)1);
                  } else {
                     this.unsetBrightness();
                  }

                  if (!this.renderMarker) {
                     this.renderModel(entity, f6, f5, f8, f2, f7, f4);
                  }

                  if (!unsetDistantAlphaLiving) {
                     DistantAlpha.get.onRenderEntity(entity, x, y, z, entityYaw, partialTicks, (byte)2);
                  }

                  if ((!(entity instanceof EntityPlayer) || !((EntityPlayer)entity).isSpectator()) && !entity.noRenderArms) {
                     this.renderLayers(entity, f6, f5, partialTicks, f8, f2, f7, f4);
                  }

                  GlStateManager.disableOutlineMode();
                  GlStateManager.disableColorMaterial();
                  if (flag1) {
                     this.unsetScoreTeamColor();
                  }
               } else {
                  float finalF = f6;
                  float finalF1 = f5;
                  float finalF2 = f2;
                  Runnable renderModel = () -> {
                     boolean flag = unsetDistantAlphaLiving || this.setDoRenderBrightness(entity, partialTicks);
                     if (!flag) {
                        DistantAlpha.get.onRenderEntity(entity, x, y, z, entityYaw, partialTicks, (byte)0);
                        DistantAlpha.get.onRenderEntity(entity, x, y, z, entityYaw, partialTicks, (byte)1);
                     }

                     this.renderModel(entity, finalF, finalF1, f8, finalF2, f7, f4);
                     if (flag) {
                        this.unsetBrightness();
                     }

                     GlStateManager.depthMask(true);
                     if (!(entity instanceof EntityPlayer) || !((EntityPlayer)entity).isSpectator()) {
                        this.renderLayers(entity, finalF, finalF1, partialTicks, f8, finalF2, f7, f4);
                     }

                     DistantAlpha.get.onRenderEntity(entity, x, y, z, entityYaw, partialTicks, (byte)2);
                  };
                  List<Module> mods = Client.moduleManager.modules.stream().filter(m -> m.actived).collect(Collectors.toList());
                  if (!silentMode) {
                     mods.forEach(m -> m.preRenderLivingBase(entity, renderModel, false));
                  }

                  renderModel.run();
                  if (!silentMode) {
                     mods.forEach(m -> m.postRenderLivingBase(entity, renderModel, false));
                  }
               }

               GlStateManager.disableRescaleNormal();
            } catch (Exception var27) {
               LOGGER.error("Couldn't render entity", (Throwable)var27);
            }

            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
            if (Reflector.RenderLivingEvent_Post_Constructor.exists()) {
               Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Post_Constructor, entity, this, partialTicks, x, y, z);
            }
         }
      }
   }

   public float prepareScale(T entitylivingbaseIn, float partialTicks) {
      GlStateManager.enableRescaleNormal();
      GlStateManager.scale(-1.0F, -1.0F, 1.0F);
      this.preRenderCallback(entitylivingbaseIn, partialTicks);
      GlStateManager.translate(0.0F, -1.501F, 0.0F);
      return 0.0625F;
   }

   protected boolean setScoreTeamColor(T entityLivingBaseIn) {
      GlStateManager.disableLighting();
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.disableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      return true;
   }

   protected void unsetScoreTeamColor() {
      GlStateManager.enableLighting();
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.enableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
   }

   public void doRenderT(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
      if (!Reflector.RenderLivingEvent_Pre_Constructor.exists()
         || !Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Pre_Constructor, entity, this, partialTicks, x, y, z)) {
         if (animateModelLiving) {
            entity.limbSwingAmount = 1.0F;
         }

         GlStateManager.pushMatrix();
         this.mainModel.isChild = entity.isChild();

         try {
            float f = this.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
            float f1 = this.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
            float f2 = f1 - f;
            if (this.mainModel.isRiding && entity.getRidingEntity() instanceof EntityLivingBase entitylivingbase) {
               f = this.interpolateRotation(entitylivingbase.prevRenderYawOffset, entitylivingbase.renderYawOffset, partialTicks);
               f2 = f1 - f;
               float f3 = MathHelper.wrapDegrees(f2);
               if (f3 < -85.0F) {
                  f3 = -85.0F;
               }

               if (f3 >= 85.0F) {
                  f3 = 85.0F;
               }

               f = f1 - f3;
               if (f3 * f3 > 2500.0F) {
                  f += f3 * 0.2F;
               }

               f2 = f1 - f;
            }

            float f7;
            if (entity == this.renderManager.renderViewEntity) {
               f7 = entity.prevRotationPitchHead + (entity.rotationPitchHead - entity.prevRotationPitchHead) * partialTicks;
            } else {
               f7 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            }

            this.renderLivingAt(entity, x, y, z);
            float f8 = this.handleRotationFloat(entity, partialTicks);
            this.rotateCorpse(entity, f8, f, partialTicks);
            float f4 = this.prepareScale(entity, partialTicks);
            float f5 = 0.0F;
            float f6 = 0.0F;
            if (!entity.isRiding()) {
               f5 = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
               f6 = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
               if (entity.isChild()) {
                  f6 *= 3.0F;
               }

               if (f5 > 1.0F) {
                  f5 = 1.0F;
               }
            }

            GlStateManager.enableAlpha();
            this.mainModel.setLivingAnimations(entity, f6, f5, partialTicks);
            this.mainModel.setRotationAngles(f6, f5, f8, f2, f7, f4, entity);
            this.renderModel(entity, f6, f5, f8, entityYaw, f7, f4);
         } catch (Exception var18) {
            LOGGER.error("Couldn't render entity", (Throwable)var18);
         }

         GlStateManager.enableTexture2D();
         GlStateManager.popMatrix();
         if (Reflector.RenderLivingEvent_Post_Constructor.exists()) {
            Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Post_Constructor, entity, this, partialTicks, x, y, z);
         }
      }
   }

   protected void renderModel(
      T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor
   ) {
      boolean flag = this.func_193115_c(entitylivingbaseIn);
      boolean babka = !entitylivingbaseIn.isInvisibleToPlayer(Minecraft.player);
      if (!babka && !(entitylivingbaseIn instanceof EntityArmorStand) && NoRender.get.actived && NoRender.get.VanishEffect.getBool()) {
         babka = true;
      }

      boolean flag1 = !flag && babka;
      if (flag || flag1) {
         if (!this.bindEntityTexture(entitylivingbaseIn)) {
            return;
         }

         if (flag1 && !silentMode) {
            GlStateManager.enableBlendProfile(GlStateManager.Profile.TRANSPARENT_MODEL);
         }

         this.mainModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
         if (flag1 && !silentMode) {
            GlStateManager.disableBlendProfile(GlStateManager.Profile.TRANSPARENT_MODEL);
         }
      }
   }

   public static void setupColor(int color) {
      float f3 = (float)(color >> 24 & 0xFF) / 255.0F;
      float f = (float)(color >> 16 & 0xFF) / 255.0F;
      float f1 = (float)(color >> 8 & 0xFF) / 255.0F;
      float f2 = (float)(color & 0xFF) / 255.0F;
      GL11.glColor4f(f, f1, f2, f3);
   }

   protected boolean func_193115_c(T p_193115_1_) {
      return !p_193115_1_.isInvisible() || this.renderOutlines;
   }

   protected boolean setDoRenderBrightness(T entityLivingBaseIn, float partialTicks) {
      return this.setBrightness(entityLivingBaseIn, partialTicks, true);
   }

   protected boolean setBrightness(T entitylivingbaseIn, float partialTicks, boolean combineTextures) {
      boolean cancelBright = WallHack.get != null
            && WallHack.get.isActived()
            && entitylivingbaseIn instanceof EntityPlayer
            && (WallHack.get.Players.getBool() || WallHack.get.Friends.getBool())
            && !WallHack.get.PlayerColorMode.getMode().contains("WallHack")
            && !WallHack.get.PlayerColorMode.getMode().contains("Tex")
         || DistantAlpha.get.dontSetEntityBrightness(entitylivingbaseIn);
      float f = entitylivingbaseIn.getBrightness();
      int i = this.getColorMultiplier(entitylivingbaseIn, f, partialTicks);
      boolean flag = (i >> 24 & 0xFF) > 0;
      boolean flag1 = entitylivingbaseIn.hurtTime > 0 || entitylivingbaseIn.deathTime > 0;
      if (!flag && !flag1) {
         return false;
      } else if (!flag && !combineTextures) {
         return false;
      } else {
         if (!cancelBright) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.defaultTexUnit);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PRIMARY_COLOR);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.defaultTexUnit);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, OpenGlHelper.GL_INTERPOLATE);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_CONSTANT);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE2_RGB, OpenGlHelper.GL_CONSTANT);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND2_RGB, 770);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
            this.brightnessBuffer.position(0);
         }

         if (flag1) {
            float hurtResist = NoRender.get.actived && NoRender.get.EntityHurt.getBool()
               ? 0.0F
               : (Panic.stop ? 0.3F : 0.4F * (float)MathUtils.easeInOutQuadWave((double)(((float)(entitylivingbaseIn.hurtTime + 1) - partialTicks) / 10.0F)));
            float[] rgba = new float[]{1.0F, 0.0F, 0.0F, hurtResist};
            if (entitylivingbaseIn.getHealth() == 0.0F && !Panic.stop) {
               float pc = ((float)(entitylivingbaseIn.deathTime - 1) + partialTicks) / 19.0F;
               rgba = new float[]{1.0F, 0.0F, 0.0F, (float)MathUtils.easeInOutQuadWave((double)pc) / 2.0F};
            }

            if (!cancelBright) {
               this.brightnessBuffer.put(rgba[0]);
               this.brightnessBuffer.put(rgba[1]);
               this.brightnessBuffer.put(rgba[2]);
               this.brightnessBuffer.put(rgba[3]);
               if (Config.isShaders()) {
                  Shaders.setEntityColor(rgba[0], rgba[1], rgba[2], rgba[3]);
               }
            } else {
               float toRed = rgba[3];
               DistantAlpha.get.setTempEntityBrightness(new float[]{1.0F, 1.0F - toRed, 1.0F - toRed, 1.0F});
            }
         } else {
            float f1 = (float)(i >> 24 & 0xFF) / 255.0F;
            float f2 = (float)(i >> 16 & 0xFF) / 255.0F;
            float f3 = (float)(i >> 8 & 0xFF) / 255.0F;
            float f4 = (float)(i & 0xFF) / 255.0F;
            if (!cancelBright) {
               this.brightnessBuffer.put(f2);
               this.brightnessBuffer.put(f3);
               this.brightnessBuffer.put(f4);
               this.brightnessBuffer.put(1.0F - f1);
               if (Config.isShaders()) {
                  Shaders.setEntityColor(f2, f3, f4, 1.0F - f1);
               }
            } else {
               DistantAlpha.get.setTempEntityBrightness(f1, f2, f3, f4);
            }
         }

         if (!cancelBright) {
            this.brightnessBuffer.flip();
            GlStateManager.glTexEnv(8960, 8705, this.brightnessBuffer);
            GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(TEXTURE_BRIGHTNESS.getGlTextureId());
            GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_PREVIOUS);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.lightmapTexUnit);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         }

         return !cancelBright;
      }
   }

   protected void unsetBrightness() {
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      GlStateManager.enableTexture2D();
      GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.defaultTexUnit);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PRIMARY_COLOR);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 8448);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.defaultTexUnit);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_ALPHA, OpenGlHelper.GL_PRIMARY_COLOR);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_ALPHA, 770);
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, 5890);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 8448);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, 5890);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
      GlStateManager.disableTexture2D();
      GlStateManager.bindTexture(0);
      GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, 5890);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 8448);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
      GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, 5890);
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      if (Config.isShaders()) {
         Shaders.setEntityColor(0.0F, 0.0F, 0.0F, 0.0F);
      }

      DistantAlpha.get.unsetTempEntityBrightness();
   }

   protected void renderLivingAt(T entityLivingBaseIn, double x, double y, double z) {
      if (EntityBox.isRenderModelSyncPosAABB() && entityLivingBaseIn != null && !(entityLivingBaseIn instanceof EntityPlayerSP)) {
         Vec3d offsetAtPos = EntityBox.hitboxModAddVec(entityLivingBaseIn, EntityBox.hitboxModPredictSize(entityLivingBaseIn));
         if (offsetAtPos.distanceTo(Vec3d.ZERO) > 0.0) {
            x += offsetAtPos.xCoord;
            y += offsetAtPos.yCoord;
            z += offsetAtPos.zCoord;
         }
      }

      GlStateManager.translate((float)x, (float)y, (float)z);
   }

   protected void rotateCorpse(T entityLiving, float p_77043_2_, float p_77043_3_, float partialTicks) {
      GlStateManager.rotate(180.0F - p_77043_3_, 0.0F, 1.0F, 0.0F);
      if (entityLiving.deathTime > 0) {
         float f = ((float)entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
         f = MathHelper.sqrt(f);
         if (f > 1.0F) {
            f = 1.0F;
         }

         GlStateManager.rotate(f * this.getDeathMaxRotation(entityLiving), 0.0F, 0.0F, 1.0F);
      } else {
         String s = TextFormatting.getTextWithoutFormattingCodes(entityLiving.getName());
         if (("Dinnerbone".equals(s) || "Grumm".equals(s))
            && entityLiving instanceof EntityPlayer entityPlayer
            && entityPlayer.isWearing(EnumPlayerModelParts.CAPE)) {
            GlStateManager.translate(0.0F, entityLiving.height + 0.1F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
         }
      }
   }

   protected float getSwingProgress(T livingBase, float partialTickTime) {
      return livingBase.getSwingProgress(partialTickTime);
   }

   protected float handleRotationFloat(T livingBase, float partialTicks) {
      return (float)livingBase.ticksExisted + partialTicks;
   }

   protected void renderLayers(
      T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn
   ) {
      for (LayerRenderer<T> layerrenderer : this.layerRenderers) {
         boolean flag = !silentMode && this.setBrightness(entitylivingbaseIn, partialTicks, layerrenderer.shouldCombineTextures());
         layerrenderer.doRenderLayer(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scaleIn);
         if (flag) {
            this.unsetBrightness();
         }
      }
   }

   protected float getDeathMaxRotation(T entityLivingBaseIn) {
      return 90.0F;
   }

   protected int getColorMultiplier(T entitylivingbaseIn, float lightBrightness, float partialTickTime) {
      return 0;
   }

   protected void preRenderCallback(T entitylivingbaseIn, float partialTickTime) {
   }

   public void renderName(T entity, double x, double y, double z) {
      if (!Reflector.RenderLivingEvent_Specials_Pre_Constructor.exists()
         || !Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Specials_Pre_Constructor, entity, this, x, y, z)) {
         if (this.canRenderName(entity)) {
            double d0 = entity.getDistanceSqToEntity(this.renderManager.renderViewEntity);
            float f = entity.isSneaking() ? NAME_TAG_RANGE_SNEAK : NAME_TAG_RANGE;
            if (d0 < (double)(f * f)) {
               String s = entity.getDisplayName().getFormattedText();
               GlStateManager.alphaFunc(516, 0.1F);
               this.renderEntityName(entity, x, y, z, s, d0);
            }
         }

         if (Reflector.RenderLivingEvent_Specials_Post_Constructor.exists()) {
            Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Specials_Post_Constructor, entity, this, x, y, z);
         }
      }
   }

   protected boolean canRenderName(T entity) {
      EntityPlayerSP entityplayersp = Minecraft.player;
      boolean flag = !entity.isInvisibleToPlayer(entityplayersp);
      if (entity != entityplayersp) {
         Team team = entity.getTeam();
         Team team1 = entityplayersp.getTeam();
         if (team != null) {
            Team.EnumVisible team$enumvisible = team.getNameTagVisibility();
            switch (team$enumvisible) {
               case ALWAYS:
                  return flag;
               case NEVER:
                  return false;
               case HIDE_FOR_OTHER_TEAMS:
                  return team1 == null ? flag : team.isSameTeam(team1) && (team.getSeeFriendlyInvisiblesEnabled() || flag);
               case HIDE_FOR_OWN_TEAM:
                  return team1 == null ? flag : !team.isSameTeam(team1) && flag;
               default:
                  return true;
            }
         }
      }

      return Minecraft.isGuiEnabled()
         && entity != this.renderManager.renderViewEntity
         && flag
         && !entity.isBeingRidden()
         && (!(entity instanceof EntityPlayer) || !NameTags.get.actived);
   }

   public List<LayerRenderer<T>> getLayerRenderers() {
      return this.layerRenderers;
   }

   static {
      int[] aint = TEXTURE_BRIGHTNESS.getTextureData();

      for (int i = 0; i < 256; i++) {
         aint[i] = -1;
      }

      TEXTURE_BRIGHTNESS.updateDynamicTexture();
   }
}
