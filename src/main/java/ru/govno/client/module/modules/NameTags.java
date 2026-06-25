package ru.govno.client.module.modules;

import com.mojang.realmsclient.gui.ChatFormatting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.vecmath.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.ReplaceStrUtils;
import ru.govno.client.utils.Render.BloomUtil;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.ScopeEntityModelTo2D;

public class NameTags extends Module {
   public static NameTags get;
   public ModeSettings RenderSpace;
   public ModeSettings NameDrawMode;
   public FloatSettings TagScale3d;
   public BoolSettings Items;
   public BoolSettings Armor;
   public BoolSettings Enchants;
   public BoolSettings Shadow;
   public BoolSettings TextsBloom;
   public BoolSettings Potions;
   public BoolSettings HealthLine;
   public BoolSettings SkinsPreview;
   public BoolSettings TotemPops;
   public BoolSettings ShowSelf;
   public BoolSettings OutfovMarkers;
   private static boolean temp3dRenderingState;
   private final List<NameTags.EntityPlayerWithUpdatedName> updatedListPlayers = new ArrayList<>();
   private final Tessellator tesellator = Tessellator.getInstance();
   private final BufferBuilder buffer = this.tesellator.getBuffer();
   protected static final ResourceLocation TAG_MARK_BASE = new ResourceLocation("vegaline/modules/nametags/tagmarkbase.png");
   protected static final ResourceLocation TAG_MARK_OVERLAY = new ResourceLocation("vegaline/modules/nametags/tagmarkoverlay.png");
   private final Vector4f ZERO_Vector4f = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);

   public NameTags() {
      super("NameTags", 0, Module.Category.RENDER);
      get = this;
      this.settings.add(this.RenderSpace = new ModeSettings("RenderSpace", "ScreenSpace", this, new String[]{"ScreenSpace", "World"}));
      this.settings.add(this.TagScale3d = new FloatSettings("TagScale3d", 1.0F, 2.0F, 0.25F, this, () -> this.RenderSpace.getMode().equalsIgnoreCase("World")));
      this.settings.add(this.NameDrawMode = new ModeSettings("NameDrawMode", "DisplayName", this, new String[]{"DisplayName", "NameOnly"}));
      this.settings.add(this.Items = new BoolSettings("Items", true, this));
      this.settings.add(this.Armor = new BoolSettings("Armor", false, this, () -> this.Items.getBool()));
      this.settings.add(this.Enchants = new BoolSettings("Enchants", false, this, () -> this.Items.getBool()));
      this.settings.add(this.Shadow = new BoolSettings("Shadow", true, this));
      this.settings.add(this.TextsBloom = new BoolSettings("TextsBloom", false, this));
      this.settings.add(this.Potions = new BoolSettings("Potions", false, this));
      this.settings.add(this.HealthLine = new BoolSettings("HealthLine", true, this));
      this.settings.add(this.SkinsPreview = new BoolSettings("SkinsPreview", false, this));
      this.settings.add(this.TotemPops = new BoolSettings("TotemPops", true, this));
      this.settings.add(this.ShowSelf = new BoolSettings("ShowSelf", false, this));
      this.settings.add(this.OutfovMarkers = new BoolSettings("OutfovMarkers", true, this));
      this.setDemand(3, 2);
   }

   private boolean is3dRender() {
      return this.RenderSpace.getMode().equalsIgnoreCase("World");
   }

   public static String getEntityName(EntityLivingBase eBase, boolean displayName) {
      if (eBase == null) {
         return "";
      } else if (displayName) {
         NetworkPlayerInfo info;
         if (eBase instanceof EntityPlayer player && (info = mc.getConnection().getPlayerInfo(player.getName())) != null) {
            return info.getDisplayName() == null ? eBase.getName() + "§r" : ReplaceStrUtils.fixString(info.getDisplayName().getFormattedText()).trim();
         }

         return eBase.getDisplayName().getFormattedText();
      } else {
         return eBase.getName() + "§r";
      }
   }

   private static String getName(EntityLivingBase entity, boolean displayName) {
      boolean summit = false;
      StringBuilder sBuilder = new StringBuilder();
      NetworkPlayerInfo info = null;
      if (Client.friendManager.isFriend(entity.getName())) {
         sBuilder.append("§aДруг§7 | §r");
      }

      if (entity instanceof EntityPlayer player
         && player == mc.pointedEntity
         && !(summit = Client.summit(entity))
         && (info = mc.getConnection().getPlayerInfo(player.getName())) != null) {
         GameType gameType = info.getGameType();
         switch (gameType) {
            case SURVIVAL:
               sBuilder.append("GM0§8 | §r");
               break;
            case CREATIVE:
               sBuilder.append("GM1§8 | §r");
               break;
            case ADVENTURE:
               sBuilder.append("GM2§8 | §r");
               break;
            case SPECTATOR:
               sBuilder.append("GM3§8 | §r");
               break;
            case NOT_SET:
               sBuilder.append("GM-1§8 | §r");
         }
      }

      sBuilder.append("§7");
      sBuilder.append(getEntityName(entity, displayName));
      if (entity.isEntityAlive()) {
         float hp = entity.getSmoothHealth();
         float absp = entity.getAbsorptionAmount();
         sBuilder.append(" §f");
         sBuilder.append(String.format("%.1f", hp).replace(".0", ""));
         if (absp > 0.0F && absp < 1000.0F) {
            sBuilder.append("§7+§e");
            sBuilder.append(String.format("%.1f", absp).replace(".0", ""));
         }

         sBuilder.append("§7ХП");
         if (entity instanceof EntityPlayer player
            && player == mc.pointedEntity
            && (!summit || !Client.summit(entity))
            && (info != null || (info = mc.getConnection().getPlayerInfo(player.getName())) != null)) {
            sBuilder.append("§8 | §r");
            sBuilder.append(info.getResponseTime());
            sBuilder.append("§7ms");
         }
      }

      return sBuilder.toString();
   }

   @Override
   public void onToggled(boolean actived) {
      this.stateAnim.setAnim(actived ? 0.0F : 1.0F);
      this.stateAnim.to = actived ? 1.0F : 0.0F;
      super.onToggled(actived);
   }

   @Override
   public void alwaysRender3D(float partialTicks) {
      if (this.actived && (double)this.stateAnim.getAnim() < 0.01) {
         this.stateAnim.to = 1.0F;
      }

      float alphaPC;
      if ((alphaPC = this.stateAnim.getAnim()) * 255.0F < 1.0F) {
         if (!this.updatedListPlayers.isEmpty()) {
            this.updatedListPlayers.clear();
         }
      } else {
         ScaledResolution.isPushScale2 = UiScaleControl.doSetGuiDefaultScale;
         boolean render3dMode = this.is3dRender();
         this.updateIn3dNameTags();
         if (render3dMode && !this.updatedListPlayers.isEmpty()) {
            RenderItem itemRender = mc.getRenderItem();
            CFontRenderer fontNameTag = Fonts.comfortaaBold_13;
            CFontRenderer effectFont = Fonts.neverlose500_18;
            CFontRenderer markFont = Fonts.mntsb_10;
            CFontRenderer itemStackFont = Fonts.comfortaaRegular_22;
            CFontRenderer skullFont = Fonts.mntsb_18;
            CFontRenderer enchFont = Fonts.comfortaaBold_15;
            CFontRenderer totemPopsFont = Fonts.comfortaaBold_15;
            boolean items = this.Items.getBool();
            boolean armor = items && this.Armor.getBool();
            boolean enchants = this.Enchants.getBool();
            boolean skinPreview = this.SkinsPreview.getBool();
            boolean potions = this.Potions.getBool();
            boolean shadow = this.Shadow.getBool();
            boolean healthLine = this.HealthLine.getBool();
            boolean totemPops = this.TotemPops.getBool();
            boolean textsBloom = this.TextsBloom.canBeRender();
            float scale = this.TagScale3d.getAnimation();
            float scaleTag = scale * 0.02F;
            float yOffsetTag = 0.15F + 0.075F * scale;
            this.prepareSetup3dRenderingForNameTags(
               () -> {
                  ScaledResolution sr1 = new ScaledResolution(mc);
                  this.updatedListPlayers
                     .forEach(
                        playerObj -> {
                           EntityPlayer player = playerObj.getPlayer();
                           String playerNameForDraw = playerObj.getPlayerDrawName();
                           ScopeEntityModelTo2D scope = player.getScope2d();
                           if (scope.canUseCoords(sr1)) {
                              this.setup3dRenderingForNameTag(
                                 player,
                                 scaleTag,
                                 yOffsetTag,
                                 () -> {
                                    this.drawEntity2DTag(
                                       playerNameForDraw,
                                       fontNameTag,
                                       effectFont,
                                       itemStackFont,
                                       skullFont,
                                       enchFont,
                                       totemPopsFont,
                                       player,
                                       alphaPC,
                                       items,
                                       armor,
                                       enchants,
                                       skinPreview,
                                       potions,
                                       shadow,
                                       healthLine && player.getHealth() < player.getMaxHealth(),
                                       totemPops,
                                       null,
                                       itemRender,
                                       null,
                                       partialTicks
                                    );
                                    GL11.glDisable(2929);
                                    CFontRenderer.instantSetVLAA = true;
                                    fontNameTag.drawAllCaches(!textsBloom);
                                    effectFont.drawAllCaches(!textsBloom);
                                    markFont.drawAllCaches(!textsBloom);
                                    itemStackFont.drawAllCaches(!textsBloom);
                                    skullFont.drawAllCaches(!textsBloom);
                                    enchFont.drawAllCaches(!textsBloom);
                                    totemPopsFont.drawAllCaches(!textsBloom);
                                    if (textsBloom) {
                                       float textBloomAPC = this.TextsBloom.getAnimation();
                                       float radiusEase = Math.max(Math.min(Minecraft.player.getSmoothDistanceToEntity(player) / 100.0F, 1.0F), 0.0F);
                                       radiusEase = (1.0F - (float)MathUtils.easeOutCubic((double)radiusEase)) * textBloomAPC;
                                       float radius = radiusEase * radiusEase * 10.0F * scale;
                                       if (radius >= 1.0F) {
                                          GL11.glBlendFunc(770, 771);
                                          GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                                          GL11.glDisable(2896);
                                          GL11.glDisable(2929);
                                          BloomUtil.update(BloomUtil.framebuffer1);
                                          BloomUtil.framebuffer1.framebufferClear();
                                          BloomUtil.framebuffer1.bindFramebuffer(false);
                                          fontNameTag.drawAllCaches();
                                          effectFont.drawAllCaches();
                                          markFont.drawAllCaches();
                                          itemStackFont.drawAllCaches();
                                          skullFont.drawAllCaches();
                                          enchFont.drawAllCaches();
                                          totemPopsFont.drawAllCaches();
                                          BloomUtil.framebuffer1.unbindFramebuffer();
                                          mc.entityRenderer.setupOverlayRendering();
                                          BloomUtil.renderBlur(
                                             BloomUtil.framebuffer1.framebufferTexture,
                                             radius,
                                             0,
                                             -1,
                                             0.75F + (float)Math.sqrt((double)radiusEase) * 1.5F,
                                             false
                                          );
                                          mc.entityRenderer.setupCameraTransformCompactCalcMatrix(partialTicks);
                                          GL11.glEnable(2929);
                                          GL11.glBlendFunc(770, 771);
                                       }
                                    }

                                    fontNameTag.drawAllCaches();
                                    effectFont.drawAllCaches();
                                    markFont.drawAllCaches();
                                    itemStackFont.drawAllCaches();
                                    skullFont.drawAllCaches();
                                    enchFont.drawAllCaches();
                                    totemPopsFont.drawAllCaches();
                                    CFontRenderer.instantSetVLAA = false;
                                    GL11.glEnable(2929);
                                 }
                              );
                           }
                        }
                     );
               }
            );
         }

         ScaledResolution.isPushScale2 = false;
      }
   }

   private void prepareSetup3dRenderingForNameTags(Runnable insertRendersActions) {
      GL11.glEnable(2929);
      GL11.glDepthRange(0.0, 0.01);
      GL11.glDisable(2896);
      GL11.glEnable(3042);
      GL11.glAlphaFunc(516, 0.003921569F);
      GL11.glDisable(2884);
      mc.entityRenderer.disableLightmap();
      insertRendersActions.run();
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glDepthRange(0.0, 1.0);
      GL11.glEnable(2929);
      GL11.glEnable(2884);
      GL11.glDepthMask(true);
   }

   private void setup3dRenderingForNameTag(EntityLivingBase entity, float scale, float yOffsetUp, Runnable insertRenderActions) {
      Vec3d entityPos = entity.getPositionVector()
         .add(
            new Vec3d(entity.posX - entity.lastTickPosX, entity.posY - entity.lastTickPosY, entity.posZ - entity.lastTickPosZ)
               .scale((double)(-1.0F + mc.getRenderPartialTicks()))
         )
         .addVector(
            0.0,
            (entity.boundingBox != null ? entity.boundingBox.maxY - entity.boundingBox.minY : (double)entity.height) * (double)(entity.isChild() ? 0.5F : 1.0F)
               + (double)yOffsetUp,
            0.0
         );
      if (EntityBox.isRenderModelSyncPosAABB() && !(entity instanceof EntityPlayerSP)) {
         Vec3d offsetAtPos = EntityBox.hitboxModAddVec(entity, EntityBox.hitboxModPredictSize(entity));
         if (offsetAtPos.distanceTo(Vec3d.ZERO) > 0.0) {
            entityPos = entityPos.add(offsetAtPos);
         }
      }

      Vec3d renderPosTranslate = entityPos.addVector(-RenderManager.viewerPosX, -RenderManager.viewerPosY, -RenderManager.viewerPosZ);
      temp3dRenderingState = true;
      GL11.glPushMatrix();
      GL11.glTranslated(renderPosTranslate.xCoord, renderPosTranslate.yCoord, renderPosTranslate.zCoord);
      GL11.glNormal3f(1.0F, 1.0F, 1.0F);
      int sideRotYaw = mc.gameSettings.thirdPersonView == 2 ? -1 : 1;
      float[] orientsPlus = WorldRender.get.getLastCameraOrients();
      GL11.glRotatef(mc.getRenderManager().playerViewY + WorldRender.get.offYawOrient + orientsPlus[0], 0.0F, -1.0F, 0.0F);
      GL11.glRotatef(mc.getRenderManager().playerViewX + WorldRender.get.offPitchOrient + orientsPlus[1], (float)sideRotYaw, 0.0F, 0.0F);
      GL11.glScalef(-scale, -scale, scale);
      insertRenderActions.run();
      GL11.glPopMatrix();
      temp3dRenderingState = false;
   }

   @Override
   public void onUpdate() {
      this.updateInUpdateNameTags();
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      if (!(this.stateAnim.anim * 255.0F < 1.0F)) {
         float alphaPC = this.stateAnim.anim;
         if (!this.updatedListPlayers.isEmpty() && !(alphaPC * 255.0F < 1.0F)) {
            boolean render3dMode = this.is3dRender();
            boolean outFovMarkers = this.OutfovMarkers.getBool();
            if (!render3dMode || outFovMarkers) {
               ScaledResolution.setTempStandardScale(
                  () -> {
                     ScaledResolution sr1 = new ScaledResolution(mc);
                     RenderManager renderManager = mc.getRenderManager();
                     RenderItem itemRender = mc.getRenderItem();
                     int markOffset = 10;
                     float partialTicks = mc.getRenderPartialTicks();
                     float selfYaw = Minecraft.player.rotationYaw;
                     CFontRenderer fontNameTag = Fonts.comfortaaBold_12;
                     CFontRenderer effectFont = Fonts.minecraftia_16;
                     CFontRenderer markFont = Fonts.mntsb_10;
                     CFontRenderer itemStackFont = Fonts.minecraftia_16;
                     CFontRenderer skullFont = Fonts.mntsb_18;
                     CFontRenderer enchFont = Fonts.smallestpixel_20;
                     CFontRenderer totemPopsFont = Fonts.comfortaaBold_15;
                     boolean items = this.Items.getBool();
                     boolean armor = items && this.Armor.getBool();
                     boolean enchants = this.Enchants.getBool();
                     boolean skinPreview = this.SkinsPreview.getBool();
                     boolean potions = this.Potions.getBool();
                     boolean shadow = this.Shadow.getBool();
                     boolean healthLine = this.HealthLine.getBool();
                     boolean totemPops = this.TotemPops.getBool();
                     boolean textsBloom = this.TextsBloom.canBeRender();
                     this.updatedListPlayers
                        .forEach(
                           playerObj -> {
                              EntityPlayer player = playerObj.getPlayer();
                              String playerNameForDraw = playerObj.getPlayerDrawName();
                              ScopeEntityModelTo2D scope = player.getScope2d();
                              if (scope.canUseCoords(sr1)) {
                                 if (!render3dMode) {
                                    this.drawEntity2DTag(
                                       playerNameForDraw,
                                       fontNameTag,
                                       effectFont,
                                       itemStackFont,
                                       skullFont,
                                       enchFont,
                                       totemPopsFont,
                                       player,
                                       alphaPC,
                                       items,
                                       armor,
                                       enchants,
                                       skinPreview,
                                       potions,
                                       shadow,
                                       healthLine && player.getHealth() < player.getMaxHealth(),
                                       totemPops,
                                       scope,
                                       itemRender,
                                       sr1,
                                       partialTicks
                                    );
                                 }
                              } else if (outFovMarkers) {
                                 this.drawEntity2DMark(playerNameForDraw, markFont, player, 10, sr1, selfYaw, partialTicks, renderManager, skinPreview, alphaPC);
                              }
                           }
                        );
                     if (textsBloom) {
                        float textBloomAPC = this.TextsBloom.getAnimation();
                        GL11.glBlendFunc(770, 771);
                        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                        GL11.glDisable(2896);
                        GL11.glDisable(2929);
                        BloomUtil.update(BloomUtil.framebuffer1);
                        BloomUtil.framebuffer1.framebufferClear();
                        BloomUtil.framebuffer1.bindFramebuffer(false);
                        fontNameTag.drawAllCaches(false);
                        effectFont.drawAllCaches(false);
                        markFont.drawAllCaches(false);
                        itemStackFont.drawAllCaches(false);
                        skullFont.drawAllCaches(false);
                        enchFont.drawAllCaches(false);
                        totemPopsFont.drawAllCaches(false);
                        BloomUtil.framebuffer1.unbindFramebuffer();
                        BloomUtil.renderBlur(BloomUtil.framebuffer1.framebufferTexture, 5.0F + 10.0F * textBloomAPC, 0, -1, 1.0F + 0.9F * textBloomAPC, false);
                        GL11.glEnable(2929);
                        GL11.glBlendFunc(770, 771);
                     }

                     fontNameTag.drawAllCaches();
                     effectFont.drawAllCaches();
                     markFont.drawAllCaches();
                     itemStackFont.drawAllCaches();
                     skullFont.drawAllCaches();
                     enchFont.drawAllCaches();
                     totemPopsFont.drawAllCaches();
                     GL11.glEnable(2929);
                     GL11.glDepthMask(true);
                  }
               );
            }
         }
      }
   }

   private List<NameTags.EntityPlayerWithUpdatedName> getShowedLivings(boolean players, boolean self) {
      boolean displayName = get.NameDrawMode.getMode().equalsIgnoreCase("DisplayName");
      return mc.world
         .getLoadedEntityList()
         .stream()
         .map(Entity::getPlayerOf)
         .filter(Objects::nonNull)
         .filter(
            entity -> {
               if ((!players || !(entity instanceof EntityOtherPlayerMP otherPMP))
                  && (!self || !(entity instanceof EntityPlayerSP) || mc.gameSettings.thirdPersonView == 0)) {
                  return false;
               }

               return true;
            }
         )
         .filter(Objects::nonNull)
         .map(entityPlayer -> new NameTags.EntityPlayerWithUpdatedName(entityPlayer, displayName))
         .toList();
   }

   private boolean[] hasLinkWithESP() {
      boolean players = false;
      boolean self = false;
      if (ESP.get.isActived()) {
         if (ESP.get.Players.getBool() && (ESP.get.PlayerMode.getMode().equalsIgnoreCase("2D") || ESP.get.PlayerMode.getMode().equalsIgnoreCase("All"))) {
            players = true;
         }

         if (ESP.get.Self.getBool() && (ESP.get.SelfMode.getMode().equalsIgnoreCase("2D") || ESP.get.SelfMode.getMode().equalsIgnoreCase("All"))) {
            self = true;
         }
      }

      return new boolean[]{players, self};
   }

   private boolean hasLinkWithESP(EntityPlayer player, boolean[] linksWithESP) {
      return player instanceof EntityOtherPlayerMP && linksWithESP[0] || player instanceof EntityPlayerSP && linksWithESP[1];
   }

   private void updateIn3dNameTags() {
      if (!this.updatedListPlayers.isEmpty()) {
         boolean[] linksWithESP = this.hasLinkWithESP();
         Vec3d moveTagOfUpAABB = new Vec3d(0.0, 0.05, 0.0);
         int fpsUpdateTag = MathUtils.clamp(Display.getDesktopDisplayMode().getFrequency(), 60, 360);
         this.updatedListPlayers.forEach(playerObj -> {
            EntityPlayer player = playerObj.getPlayer();
            if (!this.hasLinkWithESP(player, linksWithESP)) {
               ScopeEntityModelTo2D scope = player.getScope2d();
               if (scope != null) {
                  scope.setFpsLimit(fpsUpdateTag);
                  scope.updateTagCenterRenderScope2dDataIn3d(moveTagOfUpAABB);
               }
            }
         });
      }
   }

   private void updateInUpdateNameTags() {
      if (!this.updatedListPlayers.isEmpty()) {
         this.updatedListPlayers.clear();
      }

      List<NameTags.EntityPlayerWithUpdatedName> players = this.getShowedLivings(true, this.ShowSelf.getBool());
      if (!players.isEmpty()) {
         this.updatedListPlayers.addAll(players);
      }
   }

   private void drawEntity2DMark(
      String playerNameForDraw,
      CFontRenderer markFont,
      EntityLivingBase base,
      int markOffset,
      ScaledResolution sr,
      float selfYaw,
      float partialTicks,
      RenderManager rmngr,
      boolean drawSkin,
      float alphaPC
   ) {
      Vec3d posEnt = new Vec3d(
         base.lastTickPosX + (base.posX - base.lastTickPosX) * (double)partialTicks,
         base.lastTickPosY + (base.posY - base.lastTickPosY) * (double)partialTicks,
         base.lastTickPosZ + (base.posZ - base.lastTickPosZ) * (double)partialTicks
      );
      Vec3d selfVec = new Vec3d(rmngr.getRenderPosX(), rmngr.getRenderPosY(), rmngr.getRenderPosZ());
      float radian = RotationUtil.getFacePosRemote(selfVec, posEnt)[0] - 90.0F - selfYaw;
      float[] xyOfRads = this.getPointOfRadian(0.0F, 0.0F, (float)sr.getScaledWidth(), (float)sr.getScaledHeight(), radian, (float)markOffset);
      float x = xyOfRads[0];
      float y = xyOfRads[1];
      ResourceLocation skin = base instanceof EntityPlayer player ? player.skinTexture : null;
      boolean hasDrawSkin = drawSkin && skin != null;
      float markStature = hasDrawSkin ? 8.0F : 6.0F;
      float fadeSpeed = 0.3F;
      int textColor = ColorUtils.swapAlpha(ColorUtils.getColor(215), 255.0F * alphaPC);
      GL11.glPushMatrix();
      float rotAngle = radian % 90.0F / 90.0F > 0.5F ? 1.0F - radian % 90.0F / 90.0F : radian % 90.0F / 90.0F;
      rotAngle *= 2.0F;
      rotAngle *= rotAngle;
      rotAngle *= 45.0F;
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3008);
      if (hasDrawSkin && skin != null) {
         GL11.glEnable(3553);
         GL11.glShadeModel(7424);
         GL11.glColor3f(1.0F, 1.0F, 1.0F);
         mc.getTextureManager().bindTexture(WorldRender.get.updatedResourceSkin(skin, base));
         GL11.glPushMatrix();
         RenderUtils.setupColor(-1, 215.0F * alphaPC);
         GL11.glDisable(3008);
         GL11.glEnable(3553);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         float headScale = 8.0F;
         float headExtOverlay = 0.5F;
         GL11.glTranslated((double)(x - markStature / 2.0F), (double)(y - markStature / 2.0F), 0.0);
         Gui.drawScaledCustomSizeModalRect(0.0F, 0.0F, 8.0F, 8.0F, 8.0F, 8.0F, headScale, headScale, 64.0F, 64.0F);
         Gui.drawScaledCustomSizeModalRect(
            -headExtOverlay, -headExtOverlay, 39.0F, 8.0F, 10.0F, 8.0F, headScale + headExtOverlay * 2.0F, headScale + headExtOverlay * 2.0F, 64.0F, 64.0F
         );
         GL11.glEnable(3008);
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GlStateManager.resetColor();
         GL11.glPopMatrix();
      } else {
         GL11.glEnable(3553);
         GL11.glShadeModel(7425);
         GL11.glBlendFunc(770, 32772);
         int white = ColorUtils.swapAlpha(-1, 255.0F * alphaPC);
         int colorize = Client.friendManager.isFriend(base.getName())
            ? ColorUtils.getColor(40, 255, 60, 255.0F * alphaPC)
            : ColorUtils.getColor(255, 0, 0, 255.0F * alphaPC);
         int markC1 = ColorUtils.fadeColor(colorize, white, fadeSpeed, (int)(0.0F / fadeSpeed));
         int markC2 = ColorUtils.fadeColor(colorize, white, fadeSpeed, (int)(90.0F / fadeSpeed));
         int markC3 = ColorUtils.fadeColor(colorize, white, fadeSpeed, (int)(180.0F / fadeSpeed));
         int markC4 = ColorUtils.fadeColor(colorize, white, fadeSpeed, (int)(240.0F / fadeSpeed));
         mc.getTextureManager().bindTexture(TAG_MARK_BASE);
         this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         this.buffer.pos((double)(x - markStature / 2.0F), (double)(y - markStature / 2.0F)).tex(0.0, 0.0).color(markC1).endVertex();
         this.buffer.pos((double)(x - markStature / 2.0F), (double)(y + markStature / 2.0F)).tex(0.0, 1.0).color(markC2).endVertex();
         this.buffer.pos((double)(x + markStature / 2.0F), (double)(y + markStature / 2.0F)).tex(1.0, 1.0).color(markC3).endVertex();
         this.buffer.pos((double)(x + markStature / 2.0F), (double)(y - markStature / 2.0F)).tex(1.0, 0.0).color(markC4).endVertex();
         this.tesellator.draw();
         float animTimeMax = 500.0F;
         float timePC = (float)((System.currentTimeMillis() + (long)((int)(rotAngle * (animTimeMax / 90.0F)))) % (long)((int)animTimeMax)) / animTimeMax;
         float animAlphaPC = (float)MathUtils.easeInOutQuadWave((double)timePC);
         float markStature2 = markStature * (1.0F + timePC);
         mc.getTextureManager().bindTexture(TAG_MARK_OVERLAY);
         this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         this.buffer
            .pos((double)(x - markStature2 / 2.0F), (double)(y - markStature2 / 2.0F))
            .tex(0.0, 0.0)
            .color(ColorUtils.swapAlpha(markC1, (float)ColorUtils.getAlphaFromColor(markC1) * animAlphaPC))
            .endVertex();
         this.buffer
            .pos((double)(x - markStature2 / 2.0F), (double)(y + markStature2 / 2.0F))
            .tex(0.0, 1.0)
            .color(ColorUtils.swapAlpha(markC2, (float)ColorUtils.getAlphaFromColor(markC2) * animAlphaPC))
            .endVertex();
         this.buffer
            .pos((double)(x + markStature2 / 2.0F), (double)(y + markStature2 / 2.0F))
            .tex(1.0, 1.0)
            .color(ColorUtils.swapAlpha(markC3, (float)ColorUtils.getAlphaFromColor(markC3) * animAlphaPC))
            .endVertex();
         this.buffer
            .pos((double)(x + markStature2 / 2.0F), (double)(y - markStature2 / 2.0F))
            .tex(1.0, 0.0)
            .color(ColorUtils.swapAlpha(markC4, (float)ColorUtils.getAlphaFromColor(markC4) * animAlphaPC))
            .endVertex();
         this.tesellator.draw();
         GL11.glBlendFunc(770, 771);
         GL11.glShadeModel(7424);
      }

      GL11.glPopMatrix();
      float textW = markFont.getStringWidth(playerNameForDraw);
      float textX = MathUtils.clamp(x - textW / 2.0F, (float)markOffset - markStature, (float)(sr.getScaledWidth() - markOffset) - textW + markStature + 2.0F);
      float textY = y - 10.0F;
      if (ColorUtils.getAlphaFromColor(textColor) >= 33) {
         markFont.addCachedrawStringWithShadow(playerNameForDraw, textX, textY, textColor);
      }
   }

   private void drawEntity2DTag(
      String playerNameForDraw,
      CFontRenderer fontName,
      CFontRenderer effectFont,
      CFontRenderer itemStackFont,
      CFontRenderer skullFont,
      CFontRenderer enchFont,
      CFontRenderer totemPopsFont,
      EntityLivingBase base,
      float alphaPC,
      boolean items,
      boolean armor,
      boolean enchants,
      boolean heads,
      boolean potions,
      boolean shadow,
      boolean healthLine,
      boolean totemPops,
      ScopeEntityModelTo2D scope,
      RenderItem renderItem,
      ScaledResolution sr,
      float partialTicks
   ) {
      if (sr == null) {
         sr = new ScaledResolution(mc);
      }

      Vector4f bounds = scope == null ? this.ZERO_Vector4f : scope.getBounds2dUpdated();
      float x = temp3dRenderingState ? 0.0F : (float)Math.ceil((double)((bounds.getX() + (bounds.getZ() - bounds.getX()) / 2.0F) * 2.0F)) / 2.0F;
      float y = temp3dRenderingState ? 0.0F : bounds.getY() - 3.0F;
      alphaPC *= base.getDeathAlpha();
      alphaPC *= Math.min(((float)base.ticksExisted + mc.getRenderPartialTicks()) / 12.0F, 1.0F);
      if (base.getHealth() == 0.0F) {
         healthLine = false;
      }

      if (healthLine) {
         y -= 3.0F;
      }

      int texColor = ColorUtils.swapAlpha(-1, 255.0F * alphaPC);
      int bgColor = ColorUtils.getColor(0, (int)(75.0F * alphaPC));
      int bgOutColor = ColorUtils.getColor(0, (int)(100.0F * alphaPC));
      if (heads && !(base instanceof EntityPlayer) || base == FakePlayer.fakePlayer || base == FreeCam.fakePlayer) {
         heads = false;
      }

      float xExtOfHeads = heads ? 10.0F : 0.0F;
      x -= xExtOfHeads / 2.0F;
      float w = fontName.getStringWidth(playerNameForDraw);
      float extXYWH = alphaPC * (temp3dRenderingState ? 2.5F : 2.0F);
      float rectX = x - w / 2.0F - extXYWH - 1.0F;
      float rectX2 = rectX + w + extXYWH * 2.0F + xExtOfHeads + 1.0F;
      float rectY = y - 8.0F - extXYWH;
      float rectY2 = y + extXYWH + (healthLine ? 3.0F : 0.0F) - 1.0F;
      if (temp3dRenderingState) {
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            rectX, rectY, rectX2, rectY2, 3.0F, 0.5F, bgColor, bgColor, bgColor, bgColor, false, true, shadow
         );
         if (shadow) {
            RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
               rectX,
               rectY,
               rectX2,
               rectY2,
               2.0F,
               1.0F,
               3.5F,
               ClientColors.getColor1(45, 0.2F * alphaPC),
               ClientColors.getColor2(0, 0.2F * alphaPC),
               ClientColors.getColor2(45, 0.2F * alphaPC),
               ClientColors.getColor1(0, 0.2F * alphaPC),
               true,
               true,
               true
            );
         } else {
            RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
               rectX, rectY, rectX2, rectY2, 2.0F, 1.0F, 0.5F, bgOutColor, bgOutColor, bgOutColor, bgOutColor, false, true, true
            );
         }
      } else {
         RenderUtils.drawRoundOutline(
            rectX,
            rectY,
            rectX2 - rectX,
            rectY2 - rectY,
            2.0F,
            0.125F * ScaledResolution.lpSCFactor(),
            bgColor,
            shadow ? ColorUtils.swapAlpha(-1, (float)ColorUtils.getAlphaFromColor(bgColor) / 4.0F) : bgOutColor,
            sr
         );
         if (shadow) {
            RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBool(
               rectX,
               rectY,
               rectX2,
               rectY2,
               3.0F,
               ClientColors.getColor1(45, 0.2F * alphaPC),
               ClientColors.getColor2(0, 0.2F * alphaPC),
               ClientColors.getColor2(45, 0.2F * alphaPC),
               ClientColors.getColor1(0, 0.2F * alphaPC),
               true
            );
         }
      }

      if (totemPops && base instanceof EntityPlayer player) {
         this.drawTotemPopsPostTagIfCan(
            totemPopsFont,
            player,
            x - w / 2.0F - 2.0F - extXYWH + w + 4.0F + extXYWH * 2.0F - (-1.0F + 1.5F * alphaPC) + xExtOfHeads + 3.0F,
            y - 9.0F + (healthLine ? 3.0F : 0.0F) / 2.0F,
            partialTicks,
            bgColor,
            bgOutColor,
            texColor,
            alphaPC
         );
      }

      if (healthLine) {
         float hpPercent = MathUtils.clamp(base.getSmoothHealth() / base.getMaxHealth(), 0.0F, 1.0F);
         float hpExtX = 3.5F;
         float hpExtY = 1.0F;
         float hpX1 = x - w / 2.0F - 2.0F - extXYWH + 3.5F;
         float hpW = w + 4.0F + extXYWH * 2.0F - 7.0F + xExtOfHeads - 1.0F;
         float hpX2 = hpX1 + hpW * hpPercent;
         float hpX3 = hpX1 + hpW;
         int hpCol1 = ColorUtils.getProgressColor(0.25F).getRGB();
         hpCol1 = ColorUtils.swapAlpha(hpCol1, 255.0F * alphaPC);
         int hpCol2 = ColorUtils.getProgressColor(0.25F + hpPercent * 0.75F).getRGB();
         hpCol2 = ColorUtils.swapAlpha(hpCol2, 255.0F * alphaPC);
         float hpBGOff = -0.5F;
         float lastHpPercent = MathUtils.clamp(base.getPreHurtSmoothHealth() / base.getMaxHealth(), 0.0F, 1.0F);
         float hpX4 = hpX1 + (w + 4.0F + extXYWH * 2.0F - 7.0F + xExtOfHeads) * lastHpPercent;
         RenderUtils.drawAlphedRect(
            (double)(hpX1 + hpBGOff), (double)(y + hpExtY + hpBGOff), (double)(hpX3 - hpBGOff), (double)(y + 1.0F + hpExtY - hpBGOff), bgOutColor
         );
         RenderUtils.drawFullGradientRectPro(hpX1, y + hpExtY, hpX2, y + 1.0F + hpExtY, hpCol1, hpCol2, hpCol2, hpCol1, false);
         if (lastHpPercent > hpPercent) {
            float diffAPC = Math.min((lastHpPercent - hpPercent) * base.getMaxHealth(), 1.0F);
            int hpCol3 = ColorUtils.swapAlpha(-1, 255.0F * alphaPC * diffAPC);
            RenderUtils.drawAlphedSideways(
               (double)hpX2, (double)(y + hpExtY), (double)hpX4, (double)(y + 1.0F + hpExtY), ColorUtils.toDark(hpCol2, 0.4F), hpCol3
            );
            RenderUtils.drawAlphedGradient(
               (double)(hpX4 - diffAPC), (double)(y - 1.5F + hpExtY), (double)hpX4, (double)(y + hpExtY), ColorUtils.toDark(hpCol3, 0.2F), hpCol3
            );
            RenderUtils.drawAlphedGradient(
               (double)(hpX4 - diffAPC), (double)(y + 1.0F + hpExtY), (double)hpX4, (double)(y + 2.5F + hpExtY), hpCol3, ColorUtils.toDark(hpCol3, 0.2F)
            );
         }
      }

      if (RenderUtils.alpha(texColor) >= 32) {
         fontName.addCachedrawStringWithShadow(playerNameForDraw, x - w / 2.0F + xExtOfHeads - 0.5F, y - 6.0F, texColor);
      }

      ResourceLocation head;
      if (heads && base instanceof EntityPlayer player && (head = WorldRender.get.updatedResourceSkin(player.skinTexture, player)) != null) {
         mc.getTextureManager().bindTexture(head);
         GL11.glPushMatrix();
         RenderUtils.setupColor(-1, 215.0F * alphaPC);
         GL11.glDisable(3008);
         GL11.glEnable(3553);
         GL11.glEnable(3042);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         float headScale = 8.0F;
         float headExtOverlay = 0.5F;
         GL11.glTranslated((double)(x - w / 2.0F), (double)(y - 8.5F), 0.0);
         Gui.drawScaledCustomSizeModalRect(0.0F, 0.0F, 8.0F, 8.0F, 8.0F, 8.0F, headScale, headScale, 64.0F, 64.0F);
         Gui.drawScaledCustomSizeModalRect(
            -headExtOverlay, -headExtOverlay, 39.0F, 8.0F, 10.0F, 8.0F, headScale + headExtOverlay * 2.0F, headScale + headExtOverlay * 2.0F, 64.0F, 64.0F
         );
         GL11.glEnable(3008);
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GlStateManager.resetColor();
         GL11.glPopMatrix();
      }

      y += temp3dRenderingState ? -1.5F : 2.0F;
      if (potions && base.getActivePotionEffects() != null) {
         List<PotionEffect> activeEffects = base.getActivePotionEffects().stream().filter(Objects::nonNull).filter(effect -> effect.getDuration() > 0).toList();
         if (!activeEffects.isEmpty()) {
            y -= this.drawPotionEffectsReturningHeight(effectFont, activeEffects, x, y - 11.0F, alphaPC) * alphaPC;
         }
      }

      if ((items || armor) && base instanceof EntityPlayer player) {
         ItemStack hOffHand = player.getHeldItemOffhand();
         ItemStack aHelmet = player.inventory.armorInventory.get(0);
         ItemStack aChestplate = player.inventory.armorInventory.get(1);
         ItemStack aLeggings = player.inventory.armorInventory.get(2);
         ItemStack aFeet = player.inventory.armorInventory.get(3);
         ItemStack hMainHand = player.getHeldItemMainhand();
         List<ItemStack> stacks = (items && armor
               ? Arrays.asList(hOffHand, aHelmet, aChestplate, aLeggings, aFeet, hMainHand)
               : (items ? Arrays.asList(hOffHand, hMainHand) : Arrays.asList(aHelmet, aChestplate, aLeggings, aFeet)))
            .stream()
            .filter(Objects::nonNull)
            .filter(stack -> !stack.func_190926_b())
            .toList();
         if (!stacks.isEmpty()) {
            float itemScale = 0.5F * alphaPC;
            y -= 26.0F * itemScale + 1.0F;
            this.drawItemStackList(itemStackFont, skullFont, enchFont, stacks, enchants, x, y - 9.0F, alphaPC, renderItem, itemScale);
         }
      }
   }

   private String getPotionEffectString(PotionEffect potionEffect) {
      String power = "";
      ChatFormatting potionColor = null;
      int duration = potionEffect.getDuration();
      if (duration != 0) {
         int level = potionEffect.getAmplifier() == 0 ? 0 : potionEffect.getAmplifier() + 1;
         power = TextFormatting.GRAY + I18n.format("enchantment.level." + level);
         power = power.replace("enchantment.level.0", "");
         power = power.replace("enchantment.level.", "");
         if (duration > 1000) {
            potionColor = ChatFormatting.GREEN;
         }

         if (duration < 800) {
            potionColor = ChatFormatting.YELLOW;
         }

         if (duration < 600) {
            potionColor = ChatFormatting.GOLD;
         }

         if (duration < 200) {
            potionColor = System.currentTimeMillis() % 700L > 350L ? ChatFormatting.RED : ChatFormatting.DARK_RED;
         }

         if (potionEffect.getIsPotionDurationMax()) {
            potionColor = ChatFormatting.LIGHT_PURPLE;
         }
      }

      return (I18n.format(potionEffect.getPotion().getName())
            + " "
            + power
            + TextFormatting.GRAY
            + " "
            + potionColor
            + Potion.getPotionDurationString(potionEffect, 1.0F))
         .replace("  ", " ")
         .replace("null", "");
   }

   private float drawPotionEffectsReturningHeight(CFontRenderer effectFont, List<PotionEffect> potionEffects, float x, float y, float alphaPC) {
      float iXP = (float)potionEffects.stream().filter(effectx -> effectx.getPotion().hasStatusIcon()).count() * -4.5F;

      for (PotionEffect effect : potionEffects) {
         String durStr = Potion.getPotionDurationString(effect, 1.0F);
         float strW = effectFont.getStringWidth(durStr);
         iXP -= strW / 4.0F * alphaPC;
      }

      float iYP = -9.5F;

      for (PotionEffect effect : potionEffects) {
         if (this.onDoDrawPotionEffectIcon(true, x + iXP, y + iYP, 9, effect.getPotion())) {
            String durStr = Potion.getPotionDurationString(effect, 1.0F);
            float strW = effectFont.getStringWidth(durStr);
            String level = "Lv" + (effect.getAmplifier() + 1);
            int durColor = ColorUtils.swapAlpha(
               ColorUtils.getOverallColorFrom(
                  ColorUtils.getOverallColorFrom(
                     ColorUtils.getColor(255, 0, 0), ColorUtils.getColor(0, 255, 0), MathUtils.clamp((float)effect.getDuration() / 1000.0F, 0.0F, 1.0F)
                  ),
                  -1
               ),
               255.0F * alphaPC
            );
            if (ColorUtils.getAlphaFromColor(durColor) >= 33) {
               int lvColor = ColorUtils.swapAlpha(ColorUtils.getOverallColorFrom(ColorUtils.getColor(175, 175, 175), durColor), 255.0F * alphaPC);
               effectFont.addCachedrawString(
                  durStr, (x + iXP + 9.0F) * 2.0F, (y + iYP + 5.0F) * 2.0F, durColor, () -> GL11.glScaled(0.5, 0.5, 1.0), () -> GL11.glScaled(2.0, 2.0, 1.0)
               );
               if (ColorUtils.getAlphaFromColor(lvColor) >= 33) {
                  effectFont.addCachedrawString(
                     level, (x + iXP + 9.0F) * 2.0F, (y + iYP) * 2.0F, lvColor, () -> GL11.glScaled(0.5, 0.5, 1.0), () -> GL11.glScaled(2.0, 2.0, 1.0)
                  );
               }
            }

            iXP += (10.0F + strW / 2.0F) * alphaPC;
         }
      }

      return potionEffects.size() == 0 ? 0.0F : 10.0F * alphaPC;
   }

   private void drawItemStackList(
      CFontRenderer itemStackFont,
      CFontRenderer skullFont,
      CFontRenderer enchFont,
      List<ItemStack> stacks,
      boolean showEnchants,
      float x,
      float y,
      float alphaPC,
      RenderItem renderItem,
      float scale
   ) {
      scale *= alphaPC;
      float itemPixScale = 16.0F * scale;
      x -= (float)stacks.size() * (itemPixScale / 2.0F);
      float prevZLevel = renderItem.zLevel;
      int index = 0;
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDepthMask(true);
      int stackNumber = 0;
      int texColor = ColorUtils.swapAlpha(-65537, 245.0F * alphaPC * alphaPC);

      for (ItemStack stack : stacks) {
         stackNumber++;
         if (!temp3dRenderingState) {
            GL11.glDepthRange(0.0, 0.01);
         }

         renderItem.zLevel = temp3dRenderingState ? -150.0F : 300.0F;
         GL11.glPushMatrix();
         GlStateManager.enableDepth();
         RenderHelper.enableGUIStandardItemLighting();
         GL11.glTranslated((double)x, (double)y, 0.0);
         RenderUtils.customScaledObject2D(0.0F, 0.0F, itemPixScale, itemPixScale, scale);
         renderItem.renderItemAndEffectIntoGUI(stack, 0, 0);
         if (alphaPC * 255.0F >= 32.0F) {
            renderItem.renderItemOverlayIntoGUI(itemStackFont, stack, 0, 0, stack.getCount());
         }

         RenderUtils.drawItemWarnIfLowDur(stack, 0.0F, 0.0F, 1.0F, 1.0F, 3);
         ProContainer.get.injectPostDrawStack(stack, stackNumber, 0.0F, 0.0F, alphaPC);
         if ((stackNumber == 1 || stackNumber == stacks.size()) && OffHand.get.stackIsBall(stack)) {
            String skullDisplay = stack.getDisplayName()
               .replace("§r", "")
               .replace("§l", "")
               .replace("§k", "")
               .replace("§n", "")
               .replace("§n", "")
               .replace("§o", "");
            float headTextX = stackNumber == 0
               ? -skullFont.getStringWidth(skullDisplay) / 2.0F + 8.0F
               : (stackNumber == stacks.size() ? 18.0F : -skullFont.getStringWidth(skullDisplay) - 2.0F);
            float finalX = x;
             float finalScale = scale;
             skullFont.addCachedrawStringWithShadow(
               skullDisplay, headTextX, stacks.size() == 1 ? -5.0F : 6.0F, ColorUtils.swapAlpha(-1, 255.0F * alphaPC * alphaPC), () -> {
                  GL11.glPushMatrix();
                  GlStateManager.enableDepth();
                  GL11.glTranslated((double)finalX, (double)y, 0.0);
                  RenderUtils.customScaledObject2D(0.0F, 0.0F, itemPixScale, itemPixScale, finalScale);
                  RenderHelper.disableStandardItemLighting();
                  GL11.glDepthMask(false);
               }, () -> {
                  GL11.glDepthMask(true);
                  RenderHelper.enableGUIStandardItemLighting();
                  GL11.glPopMatrix();
               }
            );
            GL11.glDepthMask(true);
            RenderHelper.enableGUIStandardItemLighting();
         }

         RenderUtils.customScaledObject2D(0.0F, 0.0F, itemPixScale, itemPixScale, 1.0F / scale);
         GL11.glTranslated((double)(-x), (double)(-y), 0.0);
         RenderHelper.disableStandardItemLighting();
         if (!temp3dRenderingState) {
            GL11.glDepthRange(0.0, 1.0);
         }

         if (showEnchants) {
            GL11.glDepthMask(false);
            float texY = y - 2.0F;
            index = 0;

            for (String enchStr : this.getEnchantNamesOfEnchantsMap(stack)) {
               if (index <= 6) {
                  float strW = enchFont.getStringWidth(enchStr);
                  float texX = x + (16.0F * scale - strW / 2.0F) * scale;
                  if (RenderUtils.alpha(texColor) >= 33) {
                     enchFont.addCachedrawStringWithShadow(
                        enchStr, (texX + 2.5F) * 2.0F, texY * 2.0F, texColor, () -> GL11.glScaled(0.5, 0.5, 1.0), () -> GL11.glScaled(2.0, 2.0, 1.0)
                     );
                  }

                  texY -= 4.5F * alphaPC;
                  index++;
               }
            }
         }

         GlStateManager.enableDepth();
         GL11.glDepthMask(true);
         GL11.glPopMatrix();
         x += itemPixScale;
         index++;
      }

      renderItem.zLevel = prevZLevel;
   }

   List<String> getEnchantNamesOfEnchantsMap(ItemStack stack) {
      List<String> list = new ArrayList<>();

      for (Enchantment enchantment : EnchantmentHelper.getEnchantments(stack).keySet()) {
         String translated = enchantment.getTranslatedName(-228).replace(" enchantment.level.-228", "");
         if (translated.length() > 3) {
            translated = translated.replace("Защита от снарядов", "§7З")
               .replace("Защита", "§3З")
               .replace("Огнеупорность", "§7О")
               .replace("Невесомость", "§bН")
               .replace("Взрывоустойчивость", "§7В")
               .replace("Защита от снарядов", "§7З")
               .replace("Подводное дыхание", "§1П")
               .replace("Подводник", "§1П")
               .replace("Шипы", "§2Ш")
               .replace("Подводная ходьба", "§1П")
               .replace("Ледоход", "§bЛ")
               .replace("Проклятие несъёмности", "§4П")
               .replace("Острота", "§cО")
               .replace("Небесная кара", "§2К")
               .replace("Бич членистоногих", "§2Б")
               .replace("Отдача", "§7О")
               .replace("Заговор огня", "§6З")
               .replace("Добыча", "§2Д")
               .replace("Разящий клинок", "§7Р")
               .replace("Прочность", "§7П")
               .replace("Сила", "§cС")
               .replace("Откидывание", "§7О")
               .replace("Горящая стрела", "§6Г")
               .replace("Бесконечность", "§8Б")
               .replace("Починка", "§aП")
               .replace("Проклятие утраты", "§4У")
               .replace("Эффективность", "§2Э")
               .replace("Шёлковое касание", "§2Ш")
               .replace("Удача", "§2У")
               .replace("Везучий рыбак", "§2В")
               .replace("Приманка", "§2П")
               .replace("Долговечность", "§7П")
               .replace("Огонь", "§6О")
               .replace("Невесомка", "§bH");
            translated = translated + EnchantmentHelper.getEnchantmentLevel(enchantment, stack);
            list.add(translated);
         }
      }

      return list;
   }

   public float[] getPointOfRadian(float x, float y, float x2, float y2, float radian, float offset) {
      x += offset;
      y += offset;
      x2 -= offset;
      y2 -= offset;
      offset /= 2.0F;
      float w = x2 - x;
      float h = y2 - y;
      float xPos = x + w / 2.0F;
      float yPos = y + h / 2.0F;
      xPos += MathHelper.cos(MathHelper.toRadians(radian)) * w / 1.443333F / 1.01F;
      yPos += MathHelper.sin(MathHelper.toRadians(radian)) * h / 1.443333F / 1.01F;
      xPos = MathUtils.clamp(xPos, x + offset, x2 - offset);
      yPos = MathUtils.clamp(yPos, y + offset, y2 - offset);
      return new float[]{xPos, yPos};
   }

   public boolean onDoDrawPotionEffectIcon(boolean bindTex, float x, float y, int size, Potion potion) {
      if (potion == null) {
         return false;
      } else if (potion.hasStatusIcon()) {
         if (bindTex) {
            mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
         }

         int indexTex = potion.getStatusIconIndex();
         GL11.glPushMatrix();
         GlStateManager.disableLighting();
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glTranslated((double)x, (double)y, 0.0);
         GL11.glScaled(0.05555555555555555, 0.05555555555555555, 1.0);
         GL11.glScaled((double)size, (double)size, 1.0);
         new Gui().drawTexturedModalRect(0, 0, indexTex % 8 * 18, 198 + indexTex / 8 * 18, 18, 18);
         GL11.glPopMatrix();
         return true;
      } else {
         return false;
      }
   }

   public static boolean canCancelHandleWorldPotionEffects(boolean prevCancelled) {
      if (prevCancelled && get != null && get.isActived() && get.Potions.getBool()) {
         prevCancelled = false;
      }

      return prevCancelled;
   }

   private void drawTotemPopsPostTagIfCan(
      CFontRenderer totemPopsFont, EntityPlayer player, float x, float y, float partialTicks, int baseBgCol, int baseOutCol, int baseTextCol, float alphaPC
   ) {
      if (player != null && player.totemsPopped > 0) {
         float outPopPC = MathUtils.clamp((float)player.totemTickOutHurt - partialTicks, 0.0F, 10.0F) / 10.0F;
         float shadeCol01 = Math.min(outPopPC * 3.0F, 1.0F);
         int col = ColorUtils.getOverallColorFrom(
            ColorUtils.getColor(140, 140, 140, 100.0F * alphaPC), ColorUtils.getColor(255, 0, 0, 255.0F * alphaPC), shadeCol01
         );
         int bgCol = ColorUtils.getOverallColorFrom(baseBgCol, ColorUtils.swapAlpha(col, (float)ColorUtils.getAlphaFromColor(col) * 0.4F), shadeCol01);
         int outCol = ColorUtils.getOverallColorFrom(baseOutCol, ColorUtils.swapAlpha(col, (float)ColorUtils.getAlphaFromColor(col) * 0.8F), shadeCol01);
         int textCol = ColorUtils.getOverallColorFrom(
            ColorUtils.swapAlpha(baseTextCol, (float)ColorUtils.getAlphaFromColor(baseTextCol) * 0.3F),
            ColorUtils.swapAlpha(col, (float)ColorUtils.getAlphaFromColor(col)),
            shadeCol01
         );
         float strW = totemPopsFont.getStringWidth("-" + player.totemsPopped) + 1.0F;
         RenderUtils.drawAlphedRect((double)x, (double)y, (double)(x + strW + 7.5F), (double)(y + 8.0F), bgCol);
         RenderUtils.drawLightContureRectSmooth((double)x, (double)y, (double)(x + strW + 7.5F), (double)(y + 8.0F), outCol);
         GL11.glDisable(2929);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         float extUp = 0.0F;
         float deAPCText = 1.0F;
         if (player.totemsPopped > 1 && outPopPC > 0.0F) {
            deAPCText = 1.0F - outPopPC;
            float stepAnimYText = totemPopsFont.getHeight() / 2.0F;
            float extDown = stepAnimYText * (float)MathUtils.easeOutCubic((double)deAPCText);
            extUp = (stepAnimYText - extDown) * 2.0F;
            int oldTextCol = ColorUtils.swapAlpha(-1, (float)ColorUtils.getAlphaFromColor(baseTextCol) * 0.4F * outPopPC);
            if ((float)ColorUtils.getAlphaFromColor(oldTextCol) >= 33.0F) {
               totemPopsFont.addCachedrawString("-" + (player.totemsPopped - 1), x + 0.5F, y + 2.0F + extDown, oldTextCol, () -> {
                  GL11.glDisable(2929);
                  GL11.glEnable(3042);
                  GL11.glBlendFunc(770, 771);
               }, () -> {
               });
            }

            textCol = ColorUtils.swapAlpha(textCol, (float)ColorUtils.getAlphaFromColor(textCol) * deAPCText);
         }

         if ((float)ColorUtils.getAlphaFromColor(textCol) >= 33.0F) {
            totemPopsFont.addCachedrawString("-" + player.totemsPopped, x + 0.5F, y + 2.0F - extUp, textCol, () -> {
               GL11.glDisable(2929);
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
            }, () -> {
            });
         }

         float xPostText = x + strW;
         RenderUtils.drawAlphedGradient((double)xPostText, (double)y, (double)(xPostText + 0.5F), (double)(y + 4.0F), outCol, 0);
         RenderUtils.drawAlphedGradient((double)xPostText, (double)(y + 4.0F), (double)(xPostText + 0.5F), (double)(y + 8.0F), 0, outCol);
         float xPostLine = xPostText + 0.5F;
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         ItemStack stack = new ItemStack(net.minecraft.init.Items.TOTEM, 1);
         RenderItem itemRender = mc.getRenderItem();
         RenderHelper.enableGUIStandardItemLighting();
         GlStateManager.enableDepth();
         GL11.glPushMatrix();
         GL11.glTranslatef(xPostLine - 0.5F, y, 0.0F);
         GL11.glScalef(0.5F, 0.5F, 1.0F);
         float prevZLevel = itemRender.zLevel;
         itemRender.zLevel = temp3dRenderingState ? -150.0F : 200.0F;
         itemRender.renderItemAndEffectIntoGUI(stack, 0, 0);
         RenderUtils.drawItemWarnIfLowDur(stack, 0.0F, 0.0F, 1.0F, 1.0F);
         itemRender.zLevel = prevZLevel;
         GL11.glPopMatrix();
         RenderUtils.resetBlender();
         RenderUtils.fixShadows();
      }
   }

   private class EntityPlayerWithUpdatedName {
      private final EntityPlayer player;
      private final String name;

      public EntityPlayerWithUpdatedName(EntityPlayer player, boolean displayName) {
         this.player = player;
         this.name = NameTags.getName(player, displayName);
      }

      public EntityPlayer getPlayer() {
         return this.player;
      }

      public String getPlayerDrawName() {
         return this.name;
      }
   }
}
