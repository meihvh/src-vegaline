package net.minecraft.client.gui;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.IChatListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.chat.NormalChatListener;
import net.minecraft.client.gui.chat.OverlayChatListener;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.FoodStats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.border.WorldBorder;
import optifine.Config;
import optifine.CustomColors;
import optifine.CustomItems;
import optifine.Reflector;
import optifine.ReflectorForge;
import optifine.TextureAnimations;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.cfg.GuiConfig;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.event.events.EventRender2D;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.Bypass;
import ru.govno.client.module.modules.CaveFinder;
import ru.govno.client.module.modules.ClientColors;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.module.modules.Crosshair;
import ru.govno.client.module.modules.ESP;
import ru.govno.client.module.modules.Hud;
import ru.govno.client.module.modules.NameSecurity;
import ru.govno.client.module.modules.NoRender;
import ru.govno.client.module.modules.ProContainer;
import ru.govno.client.module.modules.ViewModel;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.DisplayCheck;
import ru.govno.client.utils.UProfiler;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.UControllers.DualsenseController;

public class GuiIngame extends Gui {
   private static final ResourceLocation VIGNETTE_TEX_PATH = new ResourceLocation("textures/misc/vignette.png");
   public static final ResourceLocation WIDGETS_TEX_PATH = new ResourceLocation("textures/gui/widgets.png");
   private static final ResourceLocation PUMPKIN_BLUR_TEX_PATH = new ResourceLocation("textures/misc/pumpkinblur.png");
   private final Random rand = new Random();
   private final Minecraft mc;
   private final RenderItem itemRenderer;
   private final GuiNewChat persistantChatGUI;
   private int updateCounter;
   private String recordPlaying = "";
   private int recordPlayingUpFor;
   private boolean recordIsPlaying;
   public float prevVignetteBrightness = 1.0F;
   private int remainingHighlightTicks;
   private ItemStack highlightingItemStack = ItemStack.field_190927_a;
   private final GuiOverlayDebug overlayDebug;
   private final GuiSubtitleOverlay overlaySubtitle;
   private final GuiSpectator spectatorGui;
   private final GuiPlayerTabOverlay overlayPlayerList;
   private final GuiBossOverlay overlayBoss;
   private int titlesTimer;
   private String displayedTitle = "";
   private String displayedSubTitle = "";
   private int titleFadeIn;
   private int titleDisplayTime;
   private int titleFadeOut;
   private int playerHealth;
   private int lastPlayerHealth;
   private long lastSystemTime;
   private long healthUpdateCounter;
   private final Map<ChatType, List<IChatListener>> field_191743_I = Maps.newHashMap();
   public static float sizer = 1.0F;
   public static boolean openedTab;
   public static float tabScale;
   public static float tabAlpha;
   public static float trottleScaff;
   private float x;
   private final AnimationUtils[] slotsScales = new AnimationUtils[]{
      new AnimationUtils(1.0F, 1.0F, 0.07F),
      new AnimationUtils(1.0F, 1.0F, 0.07F),
      new AnimationUtils(1.0F, 1.0F, 0.07F),
      new AnimationUtils(1.0F, 1.0F, 0.07F),
      new AnimationUtils(1.0F, 1.0F, 0.07F),
      new AnimationUtils(1.0F, 1.0F, 0.07F),
      new AnimationUtils(1.0F, 1.0F, 0.07F),
      new AnimationUtils(1.0F, 1.0F, 0.07F),
      new AnimationUtils(1.0F, 1.0F, 0.07F)
   };
   private final AnimationUtils[] curItemScales = new AnimationUtils[]{
      new AnimationUtils(1.0F, 1.0F, 0.1F),
      new AnimationUtils(1.0F, 1.0F, 0.1F),
      new AnimationUtils(1.0F, 1.0F, 0.1F),
      new AnimationUtils(1.0F, 1.0F, 0.1F),
      new AnimationUtils(1.0F, 1.0F, 0.1F),
      new AnimationUtils(1.0F, 1.0F, 0.1F),
      new AnimationUtils(1.0F, 1.0F, 0.1F),
      new AnimationUtils(1.0F, 1.0F, 0.1F),
      new AnimationUtils(1.0F, 1.0F, 0.1F)
   };
   private final AnimationUtils smoothSlot = new AnimationUtils(0.0F, 0.0F, 0.1F);
   private int prevSlot;
   private int lastSlot = 0;
   private final int[] prevStackSize = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1};
   private final int[] lastStackSize = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1};
   private final AnimationUtils offHandNew = new AnimationUtils(1.0F, 1.0F, 0.2F);
   private ItemStack prevOffHandStack;
   private ItemStack lastOffHandStack;

   public GuiIngame(Minecraft mcIn) {
      this.mc = mcIn;
      this.itemRenderer = mcIn.getRenderItem();
      this.overlayDebug = new GuiOverlayDebug(mcIn);
      this.spectatorGui = new GuiSpectator(mcIn);
      this.persistantChatGUI = new GuiNewChat(mcIn);
      this.overlayPlayerList = new GuiPlayerTabOverlay(mcIn, this);
      this.overlayBoss = new GuiBossOverlay(mcIn);
      this.overlaySubtitle = new GuiSubtitleOverlay(mcIn);

      for (ChatType chattype : ChatType.values()) {
         this.field_191743_I.put(chattype, Lists.newArrayList());
      }

      IChatListener ichatlistener = NarratorChatListener.field_193643_a;
      this.field_191743_I.get(ChatType.CHAT).add(new NormalChatListener(mcIn));
      this.field_191743_I.get(ChatType.CHAT).add(ichatlistener);
      this.field_191743_I.get(ChatType.SYSTEM).add(new NormalChatListener(mcIn));
      this.field_191743_I.get(ChatType.SYSTEM).add(ichatlistener);
      this.field_191743_I.get(ChatType.GAME_INFO).add(new OverlayChatListener(mcIn));
      this.setDefaultTitlesTimes();
   }

   public void setDefaultTitlesTimes() {
      this.titleFadeIn = 10;
      this.titleDisplayTime = 70;
      this.titleFadeOut = 20;
   }

   public void renderGameOverlay(float partialTicks) {
      UProfiler uProfiler = Client.uProfilers.getProfiler("2d objs");
      uProfiler.startCalc();
      GlStateManager.enableDepth();
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      ScaledResolution scaledresolution = new ScaledResolution(this.mc);
      if (DisplayCheck.isVisible()) {
         ESP.get.alwaysPreRender2D(partialTicks, scaledresolution);
      }

      uProfiler.addObj("ESP pre render silent pre");
      if (!Panic.stop && DisplayCheck.isVisible()) {
         Client.pointRenderer.render2D();
         uProfiler.addObj("PointRender render2d");
      }

      ViewModel.drawGlowHands();
      uProfiler.addObj("ViewModel drawGlowHands 2d");
      int i = scaledresolution.getScaledWidth();
      int j = scaledresolution.getScaledHeight();
      FontRenderer fontrenderer = this.getFontRenderer();
      GlStateManager.enableBlend();
      if (Config.isVignetteEnabled()) {
         this.renderVignette(Minecraft.player.getBrightness(), scaledresolution);
      } else {
         GlStateManager.enableDepth();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
      }

      ItemStack itemstack = Minecraft.player.inventory.armorItemInSlot(3);
      if (this.mc.gameSettings.thirdPersonView == 0 && itemstack.getItem() == Item.getItemFromBlock(Blocks.PUMPKIN)) {
         this.renderPumpkinOverlay(scaledresolution);
      }

      if (!Minecraft.player.isPotionActive(MobEffects.NAUSEA)) {
         float f = Minecraft.player.prevTimeInPortal + (Minecraft.player.timeInPortal - Minecraft.player.prevTimeInPortal) * partialTicks;
         if (f > 0.0F && (Panic.stop || !Bypass.get.isActived() || !Bypass.get.PortalGodmode.getBool() || !Bypass.cancelPortal)) {
            this.renderPortal(f, scaledresolution);
         }
      }

      if (this.mc.playerController.isSpectator()) {
         this.spectatorGui.renderTooltip(scaledresolution, partialTicks);
      } else if (Hud.get.isCustomHotbar()) {
         this.renderHotbarCustom(scaledresolution, partialTicks, Hud.get.getHotbarStyle());
      } else {
         this.renderHotbar(scaledresolution, partialTicks);
      }

      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      this.mc.getTextureManager().bindTexture(ICONS);
      GlStateManager.enableBlend();
      this.renderAttackIndicator(partialTicks, scaledresolution);
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      this.mc.mcProfiler.startSection("bossHealth");
      this.overlayBoss.renderBossHealth();
      this.mc.mcProfiler.endSection();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      this.mc.getTextureManager().bindTexture(ICONS);
      if (this.mc.playerController.shouldDrawHUD()) {
         this.renderPlayerStats(scaledresolution);
      }

      GL11.glDisable(2896);
      this.renderMountHealth(scaledresolution);
      GlStateManager.disableBlend();
      if (Minecraft.player.getSleepTimer() > 0) {
         this.mc.mcProfiler.startSection("sleep");
         GlStateManager.disableDepth();
         GlStateManager.disableAlpha();
         int j1 = Minecraft.player.getSleepTimer();
         float f1 = (float)j1 / 100.0F;
         if (f1 > 1.0F) {
            f1 = 1.0F - (float)(j1 - 100) / 10.0F;
         }

         int k = (int)(220.0F * f1) << 24 | 1052704;
         drawRect(0, 0.0, (double)i, (double)j, k);
         GlStateManager.enableAlpha();
         GlStateManager.enableDepth();
         this.mc.mcProfiler.endSection();
      }

      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      int k1 = i / 2 - 91;
      if (!Minecraft.player.isRidingHorse() || NoRender.get.actived && NoRender.get.ExpBar.getBool()) {
         if (this.mc.playerController.gameIsSurvivalOrAdventure() && (!NoRender.get.actived || !NoRender.get.ExpBar.getBool())) {
            this.renderExpBar(scaledresolution, k1);
         }
      } else {
         this.renderHorseJumpBar(scaledresolution, k1);
      }

      if (!this.mc.gameSettings.heldItemTooltips || this.mc.playerController.isSpectator() || NoRender.get.actived && NoRender.get.HeldTooltips.getBool()) {
         if (Minecraft.player.isSpectator()) {
            this.spectatorGui.renderSelectedItem(scaledresolution);
         }
      } else {
         this.renderSelectedItem(scaledresolution);
      }

      if (this.mc.isDemo()) {
         this.renderDemo(scaledresolution);
      }

      this.renderPotionEffects(scaledresolution);
      if (this.recordPlayingUpFor > 0) {
         this.mc.mcProfiler.startSection("overlayMessage");
         float f2 = (float)this.recordPlayingUpFor - partialTicks;
         int l1 = (int)(f2 * 255.0F / 20.0F);
         if (l1 > 255) {
            l1 = 255;
         }

         if (l1 > 8) {
            GlStateManager.pushMatrix();
            GlStateManager.translate((float)(i / 2), (float)(j - 68), 0.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            int l = 16777215;
            if (this.recordIsPlaying) {
               l = MathHelper.hsvToRGB(f2 / 50.0F, 0.7F, 0.6F) & 16777215;
            }

            fontrenderer.drawString(this.recordPlaying, (float)(-fontrenderer.getStringWidth(this.recordPlaying) / 2), -4.0, l + (l1 << 24 & 0xFF000000));
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
         }

         this.mc.mcProfiler.endSection();
      }

      if (!NoRender.get.actived || !NoRender.get.TitleScreen.getBool()) {
         this.overlaySubtitle.renderSubtitles(scaledresolution);
         if (this.titlesTimer > 0) {
            this.mc.mcProfiler.startSection("titleAndSubtitle");
            float f3 = (float)this.titlesTimer - partialTicks;
            int i2 = 255;
            if (this.titlesTimer > this.titleFadeOut + this.titleDisplayTime) {
               float f4 = (float)(this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut) - f3;
               i2 = (int)(f4 * 255.0F / (float)this.titleFadeIn);
            }

            if (this.titlesTimer <= this.titleFadeOut) {
               i2 = (int)(f3 * 255.0F / (float)this.titleFadeOut);
            }

            i2 = MathHelper.clamp(i2, 0, 255);
            if (i2 > 8) {
               GlStateManager.pushMatrix();
               GlStateManager.translate((float)(i / 2), (float)(j / 2), 0.0F);
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               GlStateManager.pushMatrix();
               GlStateManager.scale(4.0F, 4.0F, 4.0F);
               int j2 = i2 << 24 & 0xFF000000;
               fontrenderer.drawString(this.displayedTitle, (float)(-fontrenderer.getStringWidth(this.displayedTitle) / 2), -10.0F, 16777215 | j2, true);
               GlStateManager.popMatrix();
               GlStateManager.pushMatrix();
               GlStateManager.scale(2.0F, 2.0F, 2.0F);
               fontrenderer.drawString(this.displayedSubTitle, (float)(-fontrenderer.getStringWidth(this.displayedSubTitle) / 2), 5.0F, 16777215 | j2, true);
               GlStateManager.popMatrix();
               GlStateManager.disableBlend();
               GlStateManager.popMatrix();
            }

            this.mc.mcProfiler.endSection();
         }
      }

      Scoreboard scoreboard = this.mc.world.getScoreboard();
      ScoreObjective scoreobjective = null;
      ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(Minecraft.player.getName());
      if (scoreplayerteam != null) {
         int i1 = scoreplayerteam.getChatFormat().getColorIndex();
         if (i1 >= 0) {
            scoreobjective = scoreboard.getObjectiveInDisplaySlot(3 + i1);
         }
      }

      ScoreObjective scoreobjective1 = scoreobjective != null ? scoreobjective : scoreboard.getObjectiveInDisplaySlot(1);
      if (scoreobjective1 != null && (!NoRender.get.actived || !NoRender.get.ScoreBoard.getBool())) {
         this.renderScoreboard(scoreobjective1, scaledresolution);
      }

      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      GlStateManager.disableAlpha();
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0F, (float)(j - 48), 0.0F);
      this.mc.mcProfiler.startSection("chat");
      this.persistantChatGUI.drawChat(this.updateCounter);
      this.mc.mcProfiler.endSection();
      GlStateManager.popMatrix();
      scoreobjective1 = scoreboard.getObjectiveInDisplaySlot(0);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.disableLighting();
      GlStateManager.enableAlpha();
      uProfiler.addObj("GuiInGame overlay Minecraft");
      if (!Panic.stop && DisplayCheck.isVisible()) {
         ClientColors.alwaysColorUpdate();
         uProfiler.addObj("Client colors update");
         EventRender2D eventRender2D = new EventRender2D(scaledresolution, partialTicks);
         eventRender2D.call();
         uProfiler.addObj("EventRender2D");

         for (Module module : Client.moduleManager.modules) {
            if (!Panic.stop) {
               module.alwaysRender2D(scaledresolution);
               module.alwaysRender2D(partialTicks, scaledresolution);
               uProfiler.addObj(module.getName() + " AlwaysRender2D");
            }

            if (module.actived && !Panic.stop) {
               module.onRender2D(scaledresolution);
               module.onRenderUpdate();
               uProfiler.addObj(module.getName() + " OnRender2D");
            }
         }
      }

      GlStateManager.enableAlpha();
      DualsenseController.insertUiRenderPanel(scaledresolution);
      boolean renderTab = !Panic.stop && openedTab || (Panic.stop || !ComfortUi.get.isBetterTabOverlay()) && this.mc.gameSettings.keyBindPlayerList.isKeyDown();
      if (!Panic.stop && ComfortUi.get.isBetterTabOverlay()) {
         if (this.mc.gameSettings.keyBindPlayerList.isKeyDown() && tabScale > 1.0F || tabScale < 1.19F) {
            tabScale = MathUtils.lerp(tabScale, this.mc.gameSettings.keyBindPlayerList.isKeyDown() ? 0.99F : 1.2F, (float)Minecraft.frameTime * 0.02F);
         }

         if (this.mc.gameSettings.keyBindPlayerList.isKeyDown() && openedTab && (double)tabScale < 1.005) {
            tabScale = 1.0F;
         }

         if ((double)tabScale < 1.15 && !openedTab) {
            openedTab = true;
         }

         if ((double)tabScale > 1.15 && openedTab) {
            openedTab = false;
         }

         openedTab = tabAlpha > 1.0F;
         tabAlpha = MathUtils.harp(tabAlpha, (float)(this.mc.gameSettings.keyBindPlayerList.isKeyDown() ? 105 : -5), (float)Minecraft.frameTime * 0.02F);
         tabAlpha = MathUtils.clamp(tabAlpha, 0.0F, 90.0F);
      } else {
         tabScale = this.mc.gameSettings.keyBindPlayerList.isKeyDown() ? 1.15F : 0.0F;
      }

      try {
         if (renderTab && (!this.mc.isIntegratedServerRunning() || Minecraft.player.connection.getPlayerInfoMap().size() > 1 || scoreobjective1 != null)) {
            this.overlayPlayerList.updatePlayerList(true);
            GL11.glPushMatrix();
            if (!Panic.stop && ComfortUi.get.isBetterTabOverlay()) {
               this.overlayPlayerList.renderPlayerlist3(i, scoreboard, scoreobjective1);
            } else {
               this.overlayPlayerList.renderPlayerlist2(i, scoreboard, scoreobjective1);
            }

            GL11.glPopMatrix();
         } else {
            this.overlayPlayerList.updatePlayerList(false);
         }
      } catch (Exception var17) {
         var17.printStackTrace();
      }

      if (Panic.stop && !ComfortUi.get.isBetterDebugF3()) {
         if (sizer != 1.0F) {
            sizer = 1.0F;
         }
      } else if (this.mc.gameSettings.showDebugInfo) {
         if (MathUtils.getDifferenceOf(sizer, 0.0F) != 0.0F) {
            sizer = MathUtils.harp(sizer, 0.0F, (float)Minecraft.frameTime * 0.01F);
         }
      } else if (MathUtils.getDifferenceOf(sizer, 1.0F) != 0.0F) {
         sizer = MathUtils.harp(sizer, 1.0F, (float)Minecraft.frameTime * 0.0125F);
      }

      boolean render = (double)sizer < 0.95 || Panic.stop && this.mc.gameSettings.showDebugInfo;
      if (render) {
         this.overlayDebug.renderDebugInfo(scaledresolution);
      }

      if (!Panic.stop) {
         for (Module module : Client.moduleManager.modules) {
            if (module.actived) {
               module.onPostRender2D(scaledresolution);
            }
         }
      }

      Panic.onHasShowPanicCode((float)scaledresolution.getScaledWidth() / 2.0F, (float)scaledresolution.getScaledHeight() / 2.0F);
      if (CaveFinder.get != null) {
         CaveFinder.get.post2DDark(scaledresolution);
      }

      if (!Panic.stop && ComfortUi.get.isScreensDarking()) {
         uiSmoothness();
      }

      uProfiler.addObj("GuiInGame end renderGameOverlay 2d");
      uProfiler.endCalc(true);
      if (!Panic.stop && Client.uProfilers.isEnabled()) {
         Client.uProfilers.drawIn2D(new ScaledResolution(this.mc));
      }

      if (Panic.stop && Client.uProfilers.isEnabled()) {
         Client.uProfilers.stop(true, true, true);
      }

      if (!Minecraft.temporalImageSizeMoreThan16x) {
         float u = ((float)(Minecraft.player.ticksExisted - 1) + partialTicks) % 300.0F < 8.0F
            ? 1.0F - ((float)(Minecraft.player.ticksExisted - 1) + partialTicks) % 300.0F / 8.0F
            : 0.0F;
         RenderUtils.drawRect(0.0, 0.0, 10000.0, 10000.0, ColorUtils.getColor(255, 255, 255, 255.0F * u));
      }
   }

   public static void uiSmoothness() {
      Minecraft mc = Minecraft.getMinecraft();
      ScaledResolution scaled = new ScaledResolution(mc);
      float animSpeed = MathUtils.clamp(
            ((float)Minecraft.frameTime * 0.0075F + (float)Minecraft.frameTime * ((float)(ComfortUi.alphaTransition / 255) - 0.5F) / 500.0F)
               * (float)(mc.currentScreen == null ? 1 : 2),
            0.01F,
            1.0F
         )
         / 3.0F;
      if (ComfortUi.get.actived) {
         if (mc.currentScreen != null
            && !(mc.currentScreen instanceof ClickGuiScreen)
            && !(mc.currentScreen instanceof GuiChat)
            && !(mc.currentScreen instanceof GuiConfig)) {
            ComfortUi.alphaTransition = (int)MathUtils.lerp((float)ComfortUi.alphaTransition, 200.0F, animSpeed);
         } else if (ComfortUi.alphaTransition > 1) {
            ComfortUi.alphaTransition = (int)MathUtils.lerp((float)ComfortUi.alphaTransition, 0.0F, animSpeed);
         } else {
            ComfortUi.alphaTransition = 0;
         }
      } else if (ComfortUi.alphaTransition > 1) {
         ComfortUi.alphaTransition = (int)MathUtils.lerp((float)ComfortUi.alphaTransition, 0.0F, animSpeed);
      } else {
         ComfortUi.alphaTransition = 0;
      }

      if (ComfortUi.alphaTransition != 0) {
         RenderUtils.fixShadows();
         GlStateManager.disableDepth();
         GL11.glTranslated(0.0, 0.0, 1000.0);
         RenderUtils.drawAlphedRect(
            0.0, 0.0, (double)scaled.getScaledWidth(), (double)scaled.getScaledHeight(), ColorUtils.getColor(0, 0, 0, ComfortUi.alphaTransition)
         );
         GL11.glTranslated(0.0, 0.0, -1000.0);
         GlStateManager.enableDepth();
      }
   }

   private void renderAttackIndicator(float p_184045_1_, ScaledResolution p_184045_2_) {
      if (Crosshair.get == null || !Crosshair.get.actived || Panic.stop || this.mc.gameSettings.showDebugInfo) {
         GameSettings gamesettings = this.mc.gameSettings;
         if (gamesettings.thirdPersonView == 0) {
            if (this.mc.playerController.isSpectator() && this.mc.pointedEntity == null) {
               RayTraceResult raytraceresult = this.mc.objectMouseOver;
               if (raytraceresult == null || raytraceresult.typeOfHit != RayTraceResult.Type.BLOCK) {
                  return;
               }

               BlockPos blockpos = raytraceresult.getBlockPos();
               IBlockState iblockstate = this.mc.world.getBlockState(blockpos);
               if (!ReflectorForge.blockHasTileEntity(iblockstate) || !(this.mc.world.getTileEntity(blockpos) instanceof IInventory)) {
                  return;
               }
            }

            int l = p_184045_2_.getScaledWidth();
            int i1 = p_184045_2_.getScaledHeight();
            if (gamesettings.showDebugInfo && !gamesettings.hideGUI && !Minecraft.player.hasReducedDebug() && !gamesettings.reducedDebugInfo) {
               GlStateManager.pushMatrix();
               GlStateManager.translate((float)(l / 2), (float)(i1 / 2), this.zLevel);
               Entity entity = this.mc.getRenderViewEntity();
               GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * p_184045_1_, -1.0F, 0.0F, 0.0F);
               GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * p_184045_1_, 0.0F, 1.0F, 0.0F);
               GlStateManager.scale(-1.0F, -1.0F, -1.0F);
               OpenGlHelper.renderDirections(10);
               GlStateManager.popMatrix();
            } else {
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               GlStateManager.enableAlpha();
               this.drawTexturedModalRect(l / 2 - 7, i1 / 2 - 7, 0, 0, 16, 16);
               if (this.mc.gameSettings.attackIndicator == 1) {
                  float f = Minecraft.player.getCooledAttackStrength(0.0F);
                  boolean flag = false;
                  if (this.mc.pointedEntity != null && this.mc.pointedEntity instanceof EntityLivingBase && f >= 1.0F) {
                     flag = Minecraft.player.getCooldownPeriod() > 5.0F;
                     flag &= this.mc.pointedEntity.isEntityAlive();
                  }

                  int i = i1 / 2 - 7 + 16;
                  int j = l / 2 - 8;
                  if (flag) {
                     this.drawTexturedModalRect(j, i, 68, 94, 16, 16);
                  } else if (f < 1.0F) {
                     int k = (int)(f * 17.0F);
                     this.drawTexturedModalRect(j, i, 36, 94, 16, 4);
                     this.drawTexturedModalRect(j, i, 52, 94, k, 4);
                  }
               }

               if (Crosshair.get != null && Crosshair.get.actived) {
                  RenderUtils.resetColor();
               }
            }
         }
      }
   }

   protected void renderPotionEffects(ScaledResolution resolution) {
      Collection<PotionEffect> collection = Minecraft.player.getActivePotionEffects();
      if (!collection.isEmpty() && !Hud.get.isPotsCustom()) {
         this.mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
         GlStateManager.enableBlend();
         int i = 0;
         int j = 0;

         for (PotionEffect potioneffect : Ordering.natural().reverse().sortedCopy(collection)) {
            Potion potion = potioneffect.getPotion();
            boolean flag = potion.hasStatusIcon();
            if (Reflector.ForgePotion_shouldRenderHUD.exists()) {
               if (!Reflector.callBoolean(potion, Reflector.ForgePotion_shouldRenderHUD, potioneffect)) {
                  continue;
               }

               this.mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
               flag = true;
            }

            if (flag && potioneffect.doesShowParticles()) {
               int k = resolution.getScaledWidth();
               int l = 1;
               if (this.mc.isDemo()) {
                  l += 15;
               }

               int i1 = potion.getStatusIconIndex();
               if (potion.isBeneficial()) {
                  i++;
                  k -= 25 * i;
               } else {
                  j++;
                  k -= 25 * j;
                  l += 26;
               }

               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               float f = 1.0F;
               if (potioneffect.getIsAmbient()) {
                  this.drawTexturedModalRect(k, l, 165, 166, 24, 24);
               } else {
                  this.drawTexturedModalRect(k, l, 141, 166, 24, 24);
                  if (potioneffect.getDuration() <= 200) {
                     int j1 = 10 - potioneffect.getDuration() / 20;
                     f = MathHelper.clamp((float)potioneffect.getDuration() / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
                        + MathHelper.cos((float)potioneffect.getDuration() * (float) Math.PI / 5.0F) * MathHelper.clamp((float)j1 / 10.0F * 0.25F, 0.0F, 0.25F);
                  }
               }

               GlStateManager.color(1.0F, 1.0F, 1.0F, f);
               if (Reflector.ForgePotion_renderHUDEffect.exists()) {
                  if (potion.hasStatusIcon()) {
                     this.drawTexturedModalRect(k + 3, l + 3, i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18);
                  }

                  Reflector.call(potion, Reflector.ForgePotion_renderHUDEffect, k, l, potioneffect, this.mc, f);
               } else {
                  this.drawTexturedModalRect(k + 3, l + 3, i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18);
               }
            }
         }
      }
   }

   protected void renderHotbar(ScaledResolution sr, float partialTicks) {
      if (this.mc.getRenderViewEntity() instanceof EntityPlayer entityplayer) {
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.mc.getTextureManager().bindTexture(WIDGETS_TEX_PATH);
         ItemStack itemstack = entityplayer.getHeldItemOffhand();
         EnumHandSide enumhandside = entityplayer.getPrimaryHand().opposite();
         int i = sr.getScaledWidth() / 2;
         RenderHelper.disableStandardItemLighting();
         float curX = (float)(i - 90 + entityplayer.inventory.currentItem * 20);
         if (!Panic.stop && ComfortUi.get.isUsement()) {
            if (MathUtils.getDifferenceOf(curX, this.x) != 0.0F) {
               this.x = !(this.x < (float)(i - 90)) && !(this.x > (float)(i - 90 + 160))
                  ? MathUtils.harp(this.x, curX, (float)Minecraft.frameTime * 0.075F)
                  : curX;
            }
         } else {
            this.x = (float)(i - 90 + entityplayer.inventory.currentItem * 20);
         }

         float f = this.zLevel;
         int j = 182;
         int k = 91;
         this.zLevel = -90.0F;
         this.drawTexturedModalRect(i - 91, sr.getScaledHeight() - 22, 0, 0, 182, 22);
         this.drawTexturedModalRect(this.x - 2.0F, (float)(sr.getScaledHeight() - 22 - 1), 0, 22, 24, 22);
         if (!itemstack.func_190926_b()) {
            if (enumhandside == EnumHandSide.LEFT) {
               this.drawTexturedModalRect(i - 91 - 29, sr.getScaledHeight() - 23, 24, 22, 29, 24);
            } else {
               this.drawTexturedModalRect(i + 91, sr.getScaledHeight() - 23, 53, 22, 29, 24);
            }
         }

         this.zLevel = f;
         GlStateManager.enableRescaleNormal();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
         RenderHelper.enableGUIStandardItemLighting();
         CustomItems.setRenderOffHand(false);
         GL11.glDepthMask(true);

         for (int l = 0; l < 9; l++) {
            int i1 = i - 90 + l * 20 + 2;
            int j1 = sr.getScaledHeight() - 16 - 3;
            this.renderHotbarItem(i1, j1, partialTicks, entityplayer, entityplayer.inventory.getStackInSlot(l));
            ProContainer.get.injectPostDrawStack(entityplayer.inventory.getStackInSlot(l), l, (float)i1, (float)j1, 1.0F);
         }

         if (!itemstack.func_190926_b()) {
            CustomItems.setRenderOffHand(true);
            int l1 = sr.getScaledHeight() - 16 - 3;
            if (enumhandside == EnumHandSide.LEFT) {
               this.renderHotbarItem(i - 91 - 26, l1, partialTicks, entityplayer, itemstack);
               ProContainer.get.injectPostDrawStack(itemstack, 45, (float)(i - 91 - 26), (float)l1, 1.0F);
            } else {
               this.renderHotbarItem(i + 91 + 10, l1, partialTicks, entityplayer, itemstack);
               ProContainer.get.injectPostDrawStack(itemstack, 45, (float)(i + 91 + 10), (float)l1, 1.0F);
            }

            CustomItems.setRenderOffHand(false);
         }

         if (this.mc.gameSettings.attackIndicator == 2) {
            float f1 = Minecraft.player.getCooledAttackStrength(0.0F);
            if (f1 < 1.0F) {
               int i2 = sr.getScaledHeight() - 20;
               int j2 = i + 91 + 6;
               if (enumhandside == EnumHandSide.RIGHT) {
                  j2 = i - 91 - 22;
               }

               GL11.glDepthMask(true);
               this.mc.getTextureManager().bindTexture(Gui.ICONS);
               int k1 = (int)(f1 * 19.0F);
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               this.drawTexturedModalRect(j2, i2, 0, 94, 18, 18);
               this.drawTexturedModalRect(j2, i2 + 18 - k1, 18, 112 - k1, 18, k1);
            }
         }

         RenderHelper.disableStandardItemLighting();
         GlStateManager.disableRescaleNormal();
         GlStateManager.disableBlend();
      }
   }

   private AnimationUtils[] slotsScales(int currentSlot) {
      for (int i = 0; i < 9; i++) {
         boolean toOn = i == currentSlot;
         this.slotsScales[i].to = toOn ? 2.0F : 1.0F;
         this.slotsScales[i].speed = toOn ? 0.0825F : 0.035F;
      }

      return this.slotsScales;
   }

   private AnimationUtils[] curItemScales() {
      return this.curItemScales;
   }

   protected void renderHotbarCustom(ScaledResolution sr, float partialTicks, String mode) {
      if (this.mc.getRenderViewEntity() instanceof EntityPlayer entityplayer) {
         RenderHelper.disableStandardItemLighting();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         ItemStack itemstack = entityplayer.getHeldItemOffhand();
         EnumHandSide enumhandside = entityplayer.getPrimaryHand().opposite();
         if (mode.equalsIgnoreCase("Pure")) {
            AnimationUtils[] curItemScales = this.curItemScales();
            List<Float> slotsScales = new ArrayList<>();
            AnimationUtils[] F9 = this.slotsScales(entityplayer.inventory.currentItem);

            for (int i = 0; i < F9.length; i++) {
               AnimationUtils F1 = F9[i];
               slotsScales.add(Math.min(F1.getAnim() + curItemScales[i].anim, 5.0F));
            }

            float itemScaleBase = 16.0F;
            float itemStepX = 2.0F;
            float centerX = (float)sr.getScaledWidth() / 2.0F;
            float hotbarW = 0.0F;

            for (Float slotScale : slotsScales) {
               hotbarW += (itemScaleBase + (slotScale == slotsScales.get(slotsScales.size() - 1) ? 0.0F : itemStepX)) * slotScale;
            }

            float x = centerX - hotbarW / 2.0F;
            float y = (float)sr.getScaledHeight() - 20.0F;
            float offItemXOffset = 10.0F;
            float offItemScaleBase = 16.0F;
            float offItemX;
            if (enumhandside == EnumHandSide.LEFT) {
               offItemX = x - offItemXOffset - offItemScaleBase;
            } else {
               offItemX = x + hotbarW + offItemXOffset;
            }

            if (this.lastOffHandStack == null
               || this.lastOffHandStack.getItem() != itemstack.getItem()
               || this.lastOffHandStack.stackSize != itemstack.stackSize
               || this.lastOffHandStack.getItemDamage() != itemstack.getItemDamage()
               || !this.lastOffHandStack.getDisplayName().equalsIgnoreCase(itemstack.getDisplayName())) {
               boolean hasSafeSingle = this.lastOffHandStack != null
                  && this.lastOffHandStack.getItem() == itemstack.getItem()
                  && this.lastOffHandStack.getItemDamage() == itemstack.getItemDamage()
                  && this.lastOffHandStack.getDisplayName().equalsIgnoreCase(itemstack.getDisplayName());
               this.prevOffHandStack = hasSafeSingle ? null : this.lastOffHandStack;
               this.lastOffHandStack = itemstack;
               if (!hasSafeSingle) {
                  this.offHandNew.setAnim(0.0F);
                  this.offHandNew.speed = 0.035F;
               }
            }

            float offHandNew = this.offHandNew.getAnim();
            offHandNew = (float)MathUtils.easeInOutExpo((double)offHandNew);
            float offHandFadeExpand = 10.0F;
            if (this.lastOffHandStack != null && !this.lastOffHandStack.isEmpty() && this.prevOffHandStack != null) {
               float animOut = 1.0F - offHandNew;
               GL11.glPushMatrix();
               GL11.glTranslatef(offItemX, y + offHandFadeExpand - animOut * offHandFadeExpand, 0.0F);
               float scale = (float)MathUtils.easeOutBack((double)animOut);
               RenderUtils.customScaledObject2D(0.0F, 0.0F, offItemScaleBase, offItemScaleBase, scale);
               float f0 = this.itemRenderer.zLevel;
               this.itemRenderer.zLevel = -200.0F;
               RenderUtils.enableGUIStandardItemLighting();
               this.itemRenderer.renderItemAndEffectIntoGUI(this.prevOffHandStack, 0, 0);
               this.itemRenderer.renderItemOverlays(Fonts.comfortaa_18, this.prevOffHandStack, 0, 1);
               RenderUtils.disableStandardItemLighting();
               this.itemRenderer.zLevel = f0;
               RenderUtils.drawItemWarnIfLowDur(this.prevOffHandStack, 0.0F, 0.0F, 1.0F, 1.0F);
               ProContainer.get.injectPostDrawStack(this.prevOffHandStack, 45, 0.0F, 0.0F, scale);
               GlStateManager.enableDepth();
               GL11.glPopMatrix();
            }

            if (offHandNew < 0.99F) {
               float waveOffHandNew = MathUtils.valWave01(offHandNew);
               RenderUtils.drawRect(
                  (double)offItemX,
                  (double)(y - offItemScaleBase / 2.0F * waveOffHandNew + offItemScaleBase * offHandNew),
                  (double)(offItemX + offItemScaleBase),
                  (double)(y + offItemScaleBase / 2.0F * waveOffHandNew + offItemScaleBase * offHandNew),
                  ColorUtils.getColor(255, 255, 255, 255.0F * waveOffHandNew)
               );
            }

            if (this.lastOffHandStack != null && !this.lastOffHandStack.isEmpty()) {
               GL11.glPushMatrix();
               GL11.glTranslatef(offItemX, y + offHandNew * offHandFadeExpand - offHandFadeExpand, 0.0F);
               float scale = (float)MathUtils.easeOutBack((double)offHandNew);
               RenderUtils.customScaledObject2D(0.0F, 0.0F, offItemScaleBase, offItemScaleBase, scale);
               float f0 = this.itemRenderer.zLevel;
               this.itemRenderer.zLevel = -200.0F;
               RenderUtils.enableGUIStandardItemLighting();
               this.itemRenderer.renderItemAndEffectIntoGUI(this.lastOffHandStack, 0, 0);
               this.itemRenderer.renderItemOverlays(Fonts.comfortaa_18, this.lastOffHandStack, 0, 1);
               RenderUtils.disableStandardItemLighting();
               this.itemRenderer.zLevel = f0;
               RenderUtils.drawItemWarnIfLowDur(this.lastOffHandStack, 0.0F, 0.0F, 1.0F, 1.0F);
               ProContainer.get.injectPostDrawStack(this.lastOffHandStack, 45, 0.0F, 0.0F, scale);
               GlStateManager.enableDepth();
               GL11.glPopMatrix();
            }

            float itemX = x;
            float itemY = y;
            int index = 0;
            float[] itemSlotsRenderX = new float[9];

            for (Float slotScale : slotsScales) {
               float slotScaleState = slotScale - curItemScales[index].anim;
               ItemStack stack = entityplayer.inventory.mainInventory.get(index);
               if (stack != null) {
                  if (!stack.isEmpty()) {
                     Item item = stack.getItem();
                     GL11.glEnable(2929);
                     GL11.glDepthMask(true);
                     GL11.glPushMatrix();
                     GL11.glTranslated((double)itemX, (double)(itemY - itemScaleBase / 2.9F * (slotScale - 1.0F)), 0.0);
                     RenderUtils.customScaledObject2D(-itemScaleBase / 2.0F, 0.0F, itemScaleBase, itemScaleBase, slotScale);
                     float rotate = (float)MathUtils.easeOutCubic((double)(MathUtils.valWave01(slotScaleState - 1.0F) * slotScaleState))
                        * (
                           index == this.lastSlot
                              ? Math.min(Math.abs(this.smoothSlot.anim - (float)index) / hotbarW / 2.0F, 1.0F)
                                 * (this.prevSlot < this.lastSlot ? -1.0F : 1.0F)
                                 * 25.0F
                              : (index < this.lastSlot ? -15.0F : 15.0F)
                        );
                     float tempRotate = 0.0F;
                     if (index == this.lastSlot) {
                        if (item instanceof ItemSword || item instanceof ItemTool || item instanceof ItemShears || item instanceof ItemHoe) {
                           float swing = Math.min(entityplayer.getSwingProgress(partialTicks), 1.0F);
                           tempRotate += (float)MathUtils.easeInOutQuadWave((double)MathUtils.lerp(MathUtils.lerp(swing, 1.0F, swing), 1.0F, swing)) * 20.0F;
                        } else if (item instanceof ItemFood && entityplayer.isHandActive() && entityplayer.getActiveHand() == EnumHand.MAIN_HAND) {
                           float eat1_1P6 = Math.max(
                              Math.min((float)entityplayer.getItemInUseMaxCount() - 1.0F + partialTicks, (float)item.getMaxItemUseDuration(stack)), 0.0F
                           );
                           eat1_1P6 = MathUtils.valWave01(eat1_1P6 % 6.0F / 6.0F * 1.2F);
                           tempRotate += eat1_1P6 * 10.0F;
                        }
                     }

                     RenderUtils.customRotatedObject2D(0.0F, 0.0F, itemScaleBase, itemScaleBase, (double)(rotate + tempRotate));
                     float f0 = this.itemRenderer.zLevel;
                     this.itemRenderer.zLevel = -200.0F;
                     RenderUtils.enableGUIStandardItemLighting();
                     this.itemRenderer.renderItemAndEffectIntoGUI(stack, 0, 0);
                     if (index == entityplayer.inventory.currentItem) {
                        GL11.glBlendFunc(768, 1);
                        this.itemRenderer.renderItemAndEffectIntoGUI(stack, 0, 0);
                        GL11.glBlendFunc(770, 771);
                     }

                     RenderUtils.customRotatedObject2D(0.0F, 0.0F, itemScaleBase, itemScaleBase, (double)(-tempRotate));
                     this.itemRenderer.renderItemOverlayIntoGUI(Fonts.comfortaa_18, stack, 0, 1, stack.getCount());
                     RenderUtils.disableStandardItemLighting();
                     this.itemRenderer.zLevel = f0;
                     RenderUtils.drawItemWarnIfLowDur(stack, 0.0F, 0.0F, 1.0F, 1.0F);
                     ProContainer.get.injectPostDrawStack(stack, index, 0.0F, 0.0F, 2.0F - slotScaleState);
                     GL11.glPopMatrix();
                     GlStateManager.enableDepth();
                  } else {
                     GL11.glPushMatrix();
                     GL11.glTranslated((double)itemX, (double)itemY, 0.0);
                     RenderUtils.customScaledObject2D(-itemScaleBase / 2.0F, 0.0F, itemScaleBase, itemScaleBase, slotScale);
                     float rotatex = (float)MathUtils.easeInOutQuadWave((double)(slotScaleState - 1.0F)) * (float)(this.prevSlot <= this.lastSlot ? -20 : 20);
                     float tempRotatex = 0.0F;
                     RenderUtils.customRotatedObject2D(0.0F, 0.0F, itemScaleBase, itemScaleBase, (double)(rotatex + tempRotatex));
                     float pcScaleOut = slotScaleState - 1.0F;
                     int colRect = ColorUtils.getOverallColorFrom(ColorUtils.getColor(255, 30), ColorUtils.getColor(255, 95), pcScaleOut);
                     int colFont = ColorUtils.getOverallColorFrom(ColorUtils.getColor(255, 90), ColorUtils.getColor(0, 100), pcScaleOut);
                     float rectScaleBase = 7.5F - 3.0F * pcScaleOut;
                     float round = rectScaleBase / 2.0F;
                     float sh = rectScaleBase / 8.0F * (1.0F + pcScaleOut * pcScaleOut);
                     RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                        -rectScaleBase / 2.0F + itemScaleBase / 2.0F,
                        -rectScaleBase / 2.0F + itemScaleBase / 2.0F,
                        rectScaleBase / 2.0F + itemScaleBase / 2.0F,
                        rectScaleBase / 2.0F + itemScaleBase / 2.0F,
                        round,
                        sh,
                        colRect,
                        colRect,
                        colRect,
                        colRect,
                        false,
                        true,
                        true
                     );
                     GL11.glScaled(0.5, 0.5, 1.0);
                     Fonts.mntsb_20
                        .drawString(
                           String.valueOf(index),
                           -Fonts.mntsb_20.getStringWidth(String.valueOf(index)) / 2.0F + itemScaleBase,
                           itemScaleBase - Fonts.mntsb_20.getHeight() / 2.0F,
                           colFont
                        );
                     GL11.glScaled(2.0, 2.0, 1.0);
                     GL11.glPopMatrix();
                  }

                  RenderUtils.disableStandardItemLighting();
               }

               this.itemRenderer.zLevel = 200.0F;
               itemSlotsRenderX[index] = itemX;
               itemX += itemScaleBase * slotScale + (index == 8 ? 0.0F : itemStepX);
               index++;
            }

            float curItemScale = slotsScales.get(entityplayer.inventory.currentItem);
            this.smoothSlot.to = itemSlotsRenderX[entityplayer.inventory.currentItem]
               + slotsScales.get(entityplayer.inventory.currentItem) * itemScaleBase / 2.0F;
            this.smoothSlot.speed = Math.abs(this.prevSlot - entityplayer.inventory.currentItem) > 2 ? 0.35F : 0.2F;
            float slotAnim = this.smoothSlot.getAnim();
            GL11.glDepthMask(false);
            float selectScale = itemScaleBase * curItemScale;
            float selX = slotAnim - selectScale / 2.0F;
            float selY = itemY + itemScaleBase / 2.0F - selectScale / 2.0F;
            float selX2 = selX + selectScale;
            float selY2 = (float)sr.getScaledHeight();
            int selCol1 = ColorUtils.getColor(255, (int)(55.0F + 200.0F * (curItemScale - 1.0F) * curItemScales[entityplayer.inventory.currentItem].anim));
            int selCol2 = ColorUtils.getColor(255, 0);
            RenderUtils.drawWaveGradient(selX, selY, selX2, selY2, 1.0F, selCol2, selCol1, selCol1, selCol2, true, true);
            GL11.glDepthMask(true);
            if (this.lastSlot != entityplayer.inventory.currentItem) {
               this.prevSlot = this.lastSlot;
               this.lastSlot = entityplayer.inventory.currentItem;
            }

            for (int slot = 0; slot < 9; slot++) {
               ItemStack slotStack = entityplayer.inventory.mainInventory.get(slot);
               int slotStackSize = slotStack.isEmpty() ? 0 : slotStack.stackSize;
               boolean slotStackSizeChanged = false;
               if (this.lastStackSize[slot] != slotStackSize) {
                  this.prevStackSize[slot] = this.lastStackSize[slot];
                  this.lastStackSize[slot] = slotStackSize;
                  slotStackSizeChanged = true;
               }

               if (slotStackSizeChanged) {
                  curItemScales[slot].to = this.lastStackSize[slot] > this.prevStackSize[slot] ? 0.2F : 0.5F;
                  curItemScales[slot].setAnim(curItemScales[slot].anim * 0.66666F);
               } else if (curItemScales[slot].to > 0.0F && curItemScales[slot].anim >= curItemScales[slot].to * 0.95F) {
                  curItemScales[slot].setAnim(curItemScales[slot].to);
                  curItemScales[slot].to = 0.0F;
               }

               if (slotStackSize == 0 && this.prevStackSize[slot] != 0) {
                  this.prevStackSize[slot] = 0;
                  this.lastStackSize[slot] = 0;
                  curItemScales[slot].to = 0.5F;
               }

               curItemScales[slot].getAnim();
            }

            GL11.glEnable(2884);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
         } else {
            this.mc.getTextureManager().bindTexture(WIDGETS_TEX_PATH);
            int i = sr.getScaledWidth() / 2;
            float curX = (float)(entityplayer.inventory.currentItem * 20);
            if (MathUtils.getDifferenceOf(curX, this.x) != 0.0F) {
               this.x = !(this.x < 0.0F) && !(this.x > 160.0F)
                  ? MathUtils.harp(this.x, curX, (float)Minecraft.frameTime * 0.055F / (mode.equalsIgnoreCase("Sleek") ? 1.0F : 2.0F))
                  : curX;
            }

            float f = this.zLevel;
            int j = 182;
            int k = 91;
            this.zLevel = -90.0F;
            if (mode.equalsIgnoreCase("Sleek")) {
               int ccc1 = ClientColors.getColor1(50);
               int ccc2 = ClientColors.getColor2(150);
               int ccc3 = ClientColors.getColor1(200);
               int ccc4 = ClientColors.getColor2(100);
               int c1 = ColorUtils.swapAlpha(ccc1, (float)ColorUtils.getAlphaFromColor(ccc1) / 1.8F);
               int c2 = ColorUtils.swapAlpha(ccc2, (float)ColorUtils.getAlphaFromColor(ccc2) / 1.8F);
               int c4 = ColorUtils.swapAlpha(ccc3, (float)ColorUtils.getAlphaFromColor(ccc3) / 4.4F);
               int c3 = ColorUtils.swapAlpha(ccc4, (float)ColorUtils.getAlphaFromColor(ccc4) / 4.4F);
               RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                  (float)(i - 90),
                  (float)(sr.getScaledHeight() - 22),
                  (float)(i + 90),
                  (float)(sr.getScaledHeight() - 2),
                  4.0F,
                  1.25F,
                  c1,
                  c2,
                  c3,
                  c4,
                  true,
                  false,
                  true
               );
               int c1q = ColorUtils.swapAlpha(ccc1, (float)ColorUtils.getAlphaFromColor(ccc1) / 2.2F / 4.0F);
               int c2q = ColorUtils.swapAlpha(ccc2, (float)ColorUtils.getAlphaFromColor(ccc2) / 2.2F / 4.0F);
               RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                  (float)(i - 90),
                  (float)(sr.getScaledHeight() - 22),
                  (float)(i + 90),
                  (float)(sr.getScaledHeight() - 2),
                  4.0F,
                  1.0F,
                  c1q,
                  c2q,
                  c3,
                  c4,
                  true,
                  true,
                  false
               );
               int cBG = ColorUtils.getColor(7, 7, 7, 110);
               RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                  (float)(sr.getScaledWidth() / 2 - 86) + this.x - 1.0F,
                  (float)(sr.getScaledHeight() - 19),
                  (float)(sr.getScaledWidth() / 2 - 86) + this.x + 13.0F,
                  (float)(sr.getScaledHeight() - 5),
                  3.0F,
                  2.0F,
                  cBG,
                  cBG,
                  cBG,
                  cBG,
                  false,
                  true,
                  true
               );
               RenderUtils.drawInsideFullRoundedFullGradientShadowRectWithBloomBool(
                  (float)(sr.getScaledWidth() / 2 - 87) + this.x - 3.0F,
                  (float)(sr.getScaledHeight() - 22),
                  (float)(sr.getScaledWidth() / 2 - 87) + this.x + 17.0F,
                  (float)(sr.getScaledHeight() - 2),
                  4.0F,
                  1.0F,
                  ccc1,
                  ccc2,
                  ccc3,
                  ccc4,
                  false
               );
               RenderUtils.drawRoundedFullGradientOutsideShadow(
                  (float)(sr.getScaledWidth() / 2 - 87) + this.x - 3.0F,
                  (float)(sr.getScaledHeight() - 22),
                  (float)(sr.getScaledWidth() / 2 - 87) + this.x + 17.0F,
                  (float)(sr.getScaledHeight() - 2),
                  5.0F,
                  0.75F,
                  ccc1,
                  ccc2,
                  ccc3,
                  ccc4,
                  false
               );
               if (!itemstack.func_190926_b()) {
                  if (enumhandside == EnumHandSide.LEFT) {
                     RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                        (float)(i - 91 - 26),
                        (float)(sr.getScaledHeight() - 20),
                        (float)(i - 91 - 26 + 17),
                        (float)(sr.getScaledHeight() - 3),
                        3.0F,
                        2.0F,
                        c1,
                        c2,
                        c3,
                        c4,
                        false,
                        true,
                        true
                     );
                  } else {
                     RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                        (float)(i + 91 + 27 - 17),
                        (float)(sr.getScaledHeight() - 20),
                        (float)(i + 91 + 27),
                        (float)(sr.getScaledHeight() - 3),
                        3.0F,
                        2.0F,
                        c1,
                        c2,
                        c3,
                        c4,
                        false,
                        true,
                        true
                     );
                  }
               }

               GlStateManager.enableDepth();
               GL11.glDepthMask(true);
               this.zLevel = f;
               GlStateManager.enableRescaleNormal();
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               RenderHelper.enableGUIStandardItemLighting();
               CustomItems.setRenderOffHand(false);

               for (int l = 0; l < 9; l++) {
                  int i1 = i - 90 + l * 20 + 2;
                  int j1 = sr.getScaledHeight() - 16 - 4;

                  try {
                     this.renderHotbarItem(i1, j1, partialTicks, entityplayer, entityplayer.inventory.mainInventory.get(l));
                     ProContainer.get.injectPostDrawStack(entityplayer.inventory.mainInventory.get(l), l, (float)i1, (float)j1, 1.0F);
                  } catch (Exception var39) {
                  }
               }

               if (!itemstack.func_190926_b()) {
                  CustomItems.setRenderOffHand(true);
                  int l1 = sr.getScaledHeight() - 16 - 3;
                  if (enumhandside == EnumHandSide.LEFT) {
                     this.renderHotbarItem(i - 91 - 26, l1, partialTicks, entityplayer, itemstack);
                     ProContainer.get.injectPostDrawStack(itemstack, 45, (float)(i - 91 - 26), (float)l1, 1.0F);
                  } else {
                     this.renderHotbarItem(i + 91 + 10, l1, partialTicks, entityplayer, itemstack);
                     ProContainer.get.injectPostDrawStack(itemstack, 45, (float)(i + 91 + 10), (float)l1, 1.0F);
                  }

                  CustomItems.setRenderOffHand(false);
               }
            } else {
               int cl = ColorUtils.getOverallColorFrom(
                  -1,
                  ColorUtils.getColor(50, 255, 50),
                  MathUtils.clamp(Math.abs(this.x - (float)(entityplayer.inventory.currentItem * 20)) / 16.0F, 0.0F, 1.0F)
               );
               int bgC = ColorUtils.getColor(21, 21, 21, 190);
               int bgCOut = ColorUtils.getColor(9, 50);
               RenderUtils.drawRoundOutline((float)(i - 91), (float)sr.getScaledHeight() - 23.5F, 182.0F, 22.5F, 4.0F, 0.25F, bgC, bgCOut, sr);
               GL11.glEnable(3042);
               GL11.glDisable(2929);
               GL11.glDisable(2896);
               this.mc.entityRenderer.disableLightmap();
               if (!itemstack.func_190926_b()) {
                  if (enumhandside == EnumHandSide.LEFT) {
                     RenderUtils.drawRoundOutline((float)(i - 91 - 27), (float)(sr.getScaledHeight() - 20), 19.0F, 19.0F, 4.0F, 0.25F, bgC, bgCOut, sr);
                  } else {
                     RenderUtils.drawRoundOutline((float)(i + 91 + 26 - 17), (float)(sr.getScaledHeight() - 20), 19.0F, 19.0F, 4.0F, 0.25F, bgC, bgCOut, sr);
                  }
               }

               GL11.glEnable(2929);
               GlStateManager.enableDepth();
               GL11.glDepthMask(true);
               this.zLevel = f;
               GlStateManager.enableRescaleNormal();
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               RenderHelper.enableGUIStandardItemLighting();
               CustomItems.setRenderOffHand(false);
               int cef1 = this.x > (float)(entityplayer.inventory.currentItem * 20) ? 0 : cl;
               int cef2 = this.x > (float)entityplayer.inventory.currentItem * 20.0F ? cl : 0;
               float cmX = (float)(entityplayer.inventory.currentItem * 20 + 8);
               float cmX2 = this.x + 8.0F;
               float cgX = cmX2 > cmX ? cmX : cmX2;
               float cgX2 = cmX2 > cmX ? cmX2 : cmX;
               GL11.glDisable(2896);
               RenderUtils.drawAlphedSideways(
                  (double)((float)(i - 90) + cgX),
                  (double)((float)sr.getScaledHeight() - 5.0F),
                  (double)((float)(i - 90) + cgX2),
                  (double)((float)sr.getScaledHeight() - 3.5F),
                  cef1,
                  cef2,
                  true
               );
               RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                  (float)(i - 88) + cmX2 - 5.0F,
                  (float)sr.getScaledHeight() - 5.0F,
                  (float)(i - 88) + cmX2 + 5.0F,
                  (float)sr.getScaledHeight() - 4.0F,
                  0.5F,
                  1.0F,
                  cl,
                  cl,
                  cl,
                  cl,
                  false,
                  true,
                  true
               );
               GL11.glDepthMask(true);
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               int es = 0;

               for (int l = 0; l < 9; l++) {
                  if (!(entityplayer.inventory.mainInventory.get(l).getItem() instanceof ItemAir)) {
                     es = l;
                  }
               }

               this.renderHotbarItem(-10000, -10000, partialTicks, entityplayer, entityplayer.inventory.mainInventory.get(es));

               for (int lx = 0; lx < 9; lx++) {
                  int i1 = i - 90 + lx * 20 + 2;
                  int j1 = sr.getScaledHeight() - 16 - 4;
                  double diffX = (double)MathUtils.getDifferenceOf((float)(lx * 20 + 8), this.x + 8.0F);
                  double hPC = MathUtils.clamp(1.0 - diffX / 34.0, 0.0, 1.0);
                  hPC *= hPC * hPC;

                  try {
                     GL11.glTranslated(0.0, -hPC * 2.0, 0.0);
                     if (Math.abs(this.x - (float)(lx * 20)) > 0.0F) {
                        RenderUtils.customRotatedObject2D(
                           (float)i1,
                           (float)((double)j1 - hPC * 2.0),
                           16.0F,
                           16.0F,
                           (double)(
                              -(this.x - (float)(lx * 20))
                                 * 5.0F
                                 * (float)hPC
                                 * (float)(Minecraft.player.inventory.getCurrentItem().getItem() instanceof ItemAir ? 0 : 1)
                           )
                        );
                     }

                     this.renderHotbarItem(i1, j1, partialTicks, entityplayer, entityplayer.inventory.mainInventory.get(lx));
                     this.itemRenderer.renderItemOverlays(Fonts.comfortaa_18, entityplayer.inventory.mainInventory.get(lx), i1, j1);
                     ProContainer.get.injectPostDrawStack(entityplayer.inventory.mainInventory.get(lx), lx, (float)i1, (float)j1, 1.0F);
                     if (Math.abs(this.x - (float)(lx * 20)) > 0.0F) {
                        RenderUtils.customRotatedObject2D(
                           (float)i1,
                           (float)((double)j1 - hPC * 2.0),
                           16.0F,
                           16.0F,
                           (double)(
                              (this.x - (float)(lx * 20))
                                 * 5.0F
                                 * (float)hPC
                                 * (float)(Minecraft.player.inventory.getCurrentItem().getItem() instanceof ItemAir ? 0 : 1)
                           )
                        );
                     }

                     GL11.glTranslated(0.0, hPC * 2.0, 0.0);
                  } catch (Exception var38) {
                  }
               }

               if (!itemstack.func_190926_b()) {
                  CustomItems.setRenderOffHand(true);
                  int l1 = sr.getScaledHeight() - 16 - 3;
                  if (enumhandside == EnumHandSide.LEFT) {
                     this.renderHotbarItem(i - 91 - 26, l1, partialTicks, entityplayer, itemstack);
                     ProContainer.get.injectPostDrawStack(itemstack, 45, (float)(i - 91 - 26), (float)l1, 1.0F);
                  } else {
                     this.renderHotbarItem(i + 91 + 10, l1, partialTicks, entityplayer, itemstack);
                     ProContainer.get.injectPostDrawStack(itemstack, 45, (float)(i + 91 + 10), (float)l1, 1.0F);
                  }

                  CustomItems.setRenderOffHand(false);
               }
            }
         }

         RenderHelper.disableStandardItemLighting();
         GlStateManager.disableRescaleNormal();
         GlStateManager.disableBlend();
      }
   }

   public void renderHorseJumpBar(ScaledResolution scaledRes, int x) {
      if (!NoRender.get.actived || !NoRender.get.ExpBar.getBool()) {
         this.mc.mcProfiler.startSection("jumpBar");
         this.mc.getTextureManager().bindTexture(Gui.ICONS);
         float f = Minecraft.player.getHorseJumpPower();
         int i = 182;
         int j = (int)(f * 183.0F);
         int k = scaledRes.getScaledHeight() - 32 + 3;
         this.drawTexturedModalRect(x, k, 0, 84, 182, 5);
         if (j > 0) {
            this.drawTexturedModalRect(x, k, 0, 89, j, 5);
         }

         this.mc.mcProfiler.endSection();
      }
   }

   public void renderExpBar(ScaledResolution scaledRes, int x) {
      this.mc.mcProfiler.startSection("expBar");
      this.mc.getTextureManager().bindTexture(Gui.ICONS);
      int i = Minecraft.player.xpBarCap();
      GlStateManager.enableAlpha();
      GlStateManager.enableBlend();
      if (i > 0) {
         int j = 182;
         int k = (int)(Minecraft.player.experience * 183.0F);
         int l = scaledRes.getScaledHeight() - 32 + 3;
         this.drawTexturedModalRect(x, l, 0, 64, 182, 5);
         if (k > 0) {
            this.drawTexturedModalRect(x, l, 0, 69, k, 5);
         }
      }

      this.mc.mcProfiler.endSection();
      if (Minecraft.player.experienceLevel > 0) {
         this.mc.mcProfiler.startSection("expLevel");
         int j1 = 8453920;
         if (Config.isCustomColors()) {
            j1 = CustomColors.getExpBarTextColor(j1);
         }

         String s = Minecraft.player.experienceLevel + "";
         int k1 = (scaledRes.getScaledWidth() - this.getFontRenderer().getStringWidth(s)) / 2;
         int i1 = scaledRes.getScaledHeight() - 31 - 4;
         this.getFontRenderer().drawString(s, (float)(k1 + 1), (double)i1, 0);
         this.getFontRenderer().drawString(s, (float)(k1 - 1), (double)i1, 0);
         this.getFontRenderer().drawString(s, (float)k1, (double)(i1 + 1), 0);
         this.getFontRenderer().drawString(s, (float)k1, (double)(i1 - 1), 0);
         this.getFontRenderer().drawString(s, (float)k1, (double)i1, j1);
         this.mc.mcProfiler.endSection();
      }
   }

   public void renderSelectedItem(ScaledResolution scaledRes) {
      this.mc.mcProfiler.startSection("selectedItemName");
      if (this.remainingHighlightTicks > 0 && !this.highlightingItemStack.func_190926_b()) {
         String s = this.highlightingItemStack.getDisplayName();
         if (this.highlightingItemStack.hasDisplayName()) {
            s = TextFormatting.ITALIC + s;
         }

         int i = (scaledRes.getScaledWidth() - this.getFontRenderer().getStringWidth(s)) / 2;
         int j = scaledRes.getScaledHeight() - 59;
         if (!this.mc.playerController.shouldDrawHUD()) {
            j += 14;
         }

         int k = (int)((float)this.remainingHighlightTicks * 256.0F / 10.0F);
         if (k > 255) {
            k = 255;
         }

         if (k > 0) {
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            this.getFontRenderer().drawStringWithShadow(s, (float)i, (float)j, 16777215 + (k << 24));
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
         }
      }

      this.mc.mcProfiler.endSection();
   }

   public void renderDemo(ScaledResolution scaledRes) {
      this.mc.mcProfiler.startSection("demo");
      String s;
      if (this.mc.world.getTotalWorldTime() >= 120500L) {
         s = I18n.format("demo.demoExpired");
      } else {
         s = I18n.format("demo.remainingTime", StringUtils.ticksToElapsedTime((int)(120500L - this.mc.world.getTotalWorldTime())));
      }

      int i = this.getFontRenderer().getStringWidth(s);
      this.getFontRenderer().drawStringWithShadow(s, (float)(scaledRes.getScaledWidth() - i - 10), 5.0F, 16777215);
      this.mc.mcProfiler.endSection();
   }

   private void renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes) {
      Scoreboard scoreboard = objective.getScoreboard();
      Collection<Score> collection = scoreboard.getSortedScores(objective);
      List<Score> list = Lists.newArrayList(Iterables.filter(collection, new Predicate<Score>() {
         public boolean apply(@Nullable Score p_apply_1_) {
            return p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#");
         }
      }));
      if (list.size() > 15) {
         collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
      } else {
         collection = list;
      }

      int i = this.getFontRenderer().getStringWidth(objective.getDisplayName());

      for (Score score : collection) {
         ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
         String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + TextFormatting.RED + score.getScorePoints();
         i = Math.max(i, this.getFontRenderer().getStringWidth(s));
      }

      int i1 = collection.size() * this.getFontRenderer().FONT_HEIGHT;
      int j1 = scaledRes.getScaledHeight() / 2 + i1 / 3;
      int k1 = 3;
      int l1 = scaledRes.getScaledWidth() - i - 3;
      int j = 0;

      for (Score score1 : collection) {
         j++;
         ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
         String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
         if ((!NameSecurity.get.actived || j != 8 || !this.mc.isSingleplayer())
            && this.mc.getCurrentServerData().serverIP.contains("reallyworld")
            && s1.contains("Ник")) {
            s1 = TextFormatting.GRAY + "║" + TextFormatting.WHITE + " Ник: " + TextFormatting.RED + NameSecurity.replacedName();
         }

         if ((!NameSecurity.get.actived || j != 8 || !this.mc.isSingleplayer())
            && this.mc.getCurrentServerData().serverIP.contains("reallyworld")
            && s1.contains("Рилликов")) {
            s1 = TextFormatting.GRAY + "║" + TextFormatting.WHITE + " Рилликов: " + TextFormatting.GOLD + "18721" + TextFormatting.GRAY + " ℜ";
         }

         if ((!NameSecurity.get.actived || j != 8 || !this.mc.isSingleplayer())
            && this.mc.getCurrentServerData().serverIP.contains("reallyworld")
            && s1.contains("Ранг")) {
            s1 = TextFormatting.GRAY
               + "║"
               + TextFormatting.WHITE
               + " Ранг: "
               + TextFormatting.DARK_AQUA
               + TextFormatting.BOLD
               + "MODER"
               + TextFormatting.GOLD
               + TextFormatting.BOLD
               + "+";
         }

         if ((!NameSecurity.get.actived || j != 8 || !this.mc.isSingleplayer())
            && this.mc.getCurrentServerData().serverIP.contains("reallyworld")
            && !s1.contains("Сервер:")
            && s1.contains("Сервер")) {
            s1 = TextFormatting.GRAY + "║" + TextFormatting.WHITE + " Сервер: " + TextFormatting.GOLD + "GRIEF-0";
         }

         if (!NameSecurity.get.actived) {
            s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
         }

         String s2 = "" + TextFormatting.RED + score1.getScorePoints();
         int k = j1 - j * this.getFontRenderer().FONT_HEIGHT;
         int l = scaledRes.getScaledWidth() - 3 + 2;
         drawRect(l1 - 2, (double)k, (double)l, (double)(k + this.getFontRenderer().FONT_HEIGHT), 1342177280);
         this.mc.fontRendererObj.drawString(s1, (float)l1, (double)k, 553648127);
         this.mc.fontRendererObj.drawString(s2, (float)(l - this.getFontRenderer().getStringWidth(s2)), (double)k, 553648127);
         if (j == collection.size()) {
            String s3 = objective.getDisplayName();
            drawRect(l1 - 2, (double)(k - this.getFontRenderer().FONT_HEIGHT - 1), (double)l, (double)(k - 1), 1610612736);
            drawRect(l1 - 2, (double)(k - 1), (double)l, (double)k, 1342177280);
            this.getFontRenderer()
               .drawString(s3, (float)(l1 + i / 2 - this.getFontRenderer().getStringWidth(s3) / 2), (double)(k - this.getFontRenderer().FONT_HEIGHT), 553648127);
         }
      }
   }

   private void renderPlayerStats(ScaledResolution scaledRes) {
      int g = !Panic.stop && NoRender.get.actived && NoRender.get.ExpBar.getBool() ? 33 : 39;
      if (this.mc.getRenderViewEntity() instanceof EntityPlayer entityplayer) {
         int i = MathHelper.ceil(entityplayer.getHealth());
         boolean flag = this.healthUpdateCounter > (long)this.updateCounter && (this.healthUpdateCounter - (long)this.updateCounter) / 3L % 2L == 1L;
         if (i < this.playerHealth && entityplayer.hurtResistantTime > 0) {
            this.lastSystemTime = Minecraft.getSystemTime();
            this.healthUpdateCounter = (long)(this.updateCounter + 20);
         } else if (i > this.playerHealth && entityplayer.hurtResistantTime > 0) {
            this.lastSystemTime = Minecraft.getSystemTime();
            this.healthUpdateCounter = (long)(this.updateCounter + 10);
         }

         if (Minecraft.getSystemTime() - this.lastSystemTime > 1000L) {
            this.playerHealth = i;
            this.lastPlayerHealth = i;
            this.lastSystemTime = Minecraft.getSystemTime();
         }

         this.playerHealth = i;
         int j = this.lastPlayerHealth;
         this.rand.setSeed((long)this.updateCounter * 312871L);
         FoodStats foodstats = entityplayer.getFoodStats();
         int foodStat = foodstats.getFoodLevel();
         float foodSaturation = foodstats.getSaturationLevel();
         IAttributeInstance iattributeinstance = entityplayer.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
         int l = scaledRes.getScaledWidth() / 2 - 91;
         int i1 = scaledRes.getScaledWidth() / 2 + 91;
         int j1 = scaledRes.getScaledHeight() - g;
         float f = (float)iattributeinstance.getAttributeValue();
         int k1 = MathHelper.ceil(entityplayer.getAbsorptionAmount());
         int l1 = MathHelper.ceil((f + (float)k1) / 2.0F / 10.0F);
         int i2 = Math.max(10 - (l1 - 2), 3);
         int j2 = j1 - (l1 - 1) * i2 - 10;
         int k2 = j1 - 10;
         int l2 = k1;
         int i3 = entityplayer.getTotalArmorValue();
         int j3 = -1;
         if (entityplayer.isPotionActive(MobEffects.REGENERATION)) {
            j3 = this.updateCounter % MathHelper.ceil(f + 5.0F);
         }

         if (!Hud.get.isArmorHud()) {
            this.mc.mcProfiler.startSection("armor");
            boolean vanillaStyledClientArmorHud = !Panic.stop
               && Hud.get != null
               && Hud.get.isActived()
               && Hud.get.ArmorHUD.getBool()
               && Hud.get.ArmorHUDStyle.getMode().equalsIgnoreCase("Vanilla")
               && entityplayer != null
               && entityplayer.inventory != null
               && entityplayer.inventory.armorInventory != null;
            if (!vanillaStyledClientArmorHud) {
               for (int k3 = 0; k3 < 10; k3++) {
                  if (i3 > 0) {
                     int l3 = l + k3 * 8;
                     if (k3 * 2 + 1 < i3) {
                        this.drawTexturedModalRect(l3, j2, 34, 9, 9, 9);
                     }

                     if (k3 * 2 + 1 == i3) {
                        this.drawTexturedModalRect(l3, j2, 25, 9, 9, 9);
                     }

                     if (k3 * 2 + 1 > i3) {
                        this.drawTexturedModalRect(l3, j2, 16, 9, 9, 9);
                     }
                  }
               }
            } else {
               float xArmor = (float)l;
               float yArmor = (float)j2 + 1.0F;
               float scale = 0.5F;
               float scaleOverlayEmpty = 0.75F;
               float xStep = 8.0F;

               for (int armorIndex = 0; armorIndex < entityplayer.inventory.armorInventory.size(); armorIndex++) {
                  ItemStack stack = entityplayer.inventory.armorInventory.get(armorIndex);
                  if (stack != null) {
                     if (!stack.isEmpty()) {
                        GL11.glEnable(2929);
                        GL11.glDepthMask(true);
                        GL11.glPushMatrix();
                        GL11.glTranslated((double)xArmor, (double)yArmor, 0.0);
                        GL11.glScaled((double)scale, (double)scale, 1.0);
                        float f0 = this.itemRenderer.zLevel;
                        this.itemRenderer.zLevel = 200.0F;
                        RenderUtils.enableGUIStandardItemLighting();
                        this.itemRenderer.renderItemAndEffectIntoGUI(stack, 0, 0);
                        this.itemRenderer.renderItemOverlayIntoGUI(this.mc.fontRendererObj, stack, 0, 0, stack.getCount());
                        RenderUtils.disableStandardItemLighting();
                        this.itemRenderer.zLevel = f0;
                        RenderUtils.drawItemWarnIfLowDur(stack, 0.0F, 0.0F, 1.0F, 1.0F);
                        ProContainer.get.injectPostDrawStack(stack, armorIndex, 0.0F, 0.0F, 1.0F);
                        GL11.glPopMatrix();
                        GlStateManager.enableDepth();
                     } else {
                        String iconName;
                        if (stack.func_190926_b()
                           && (iconName = Minecraft.player.inventoryContainer.inventorySlots.get(8 - armorIndex).getSlotTexture()) != null) {
                           this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                           TextureAtlasSprite textureSprite = this.mc.getTextureMapBlocks().getAtlasSprite(iconName);
                           RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                           RenderUtils.buffer
                              .pos((double)xArmor, (double)(yArmor + 16.0F * scale))
                              .tex((double)textureSprite.getMinU(), (double)textureSprite.getMaxV())
                              .color(-1)
                              .endVertex();
                           RenderUtils.buffer
                              .pos((double)(xArmor + 16.0F * scale), (double)(yArmor + 16.0F * scale))
                              .tex((double)textureSprite.getMaxU(), (double)textureSprite.getMaxV())
                              .color(-1)
                              .endVertex();
                           RenderUtils.buffer
                              .pos((double)(xArmor + 16.0F * scale), (double)yArmor)
                              .tex((double)textureSprite.getMaxU(), (double)textureSprite.getMinV())
                              .color(-1)
                              .endVertex();
                           RenderUtils.buffer
                              .pos((double)xArmor, (double)yArmor)
                              .tex((double)textureSprite.getMinU(), (double)textureSprite.getMinV())
                              .color(-1)
                              .endVertex();
                           GL11.glEnable(3042);
                           GL11.glDisable(2929);
                           GL11.glDisable(3008);
                           RenderUtils.tessellator.draw();
                           GL11.glEnable(3008);
                           GL11.glEnable(2929);
                        }
                     }

                     RenderUtils.disableStandardItemLighting();
                  }

                  xArmor += xStep;
               }

               this.mc.getTextureManager().bindTexture(ICONS);
            }
         }

         this.mc.mcProfiler.endStartSection("health");

         for (int j5 = MathHelper.ceil((f + (float)k1) / 2.0F) - 1; j5 >= 0; j5--) {
            int k5 = 16;
            if (entityplayer.isPotionActive(MobEffects.POISON)) {
               k5 += 36;
            } else if (entityplayer.isPotionActive(MobEffects.WITHER)) {
               k5 += 72;
            }

            int i4 = 0;
            if (flag) {
               i4 = 1;
            }

            int j4 = MathHelper.ceil((float)(j5 + 1) / 10.0F) - 1;
            int k4 = l + j5 % 10 * 8;
            int l4 = j1 - j4 * i2;
            if (i <= 4) {
               l4 += this.rand.nextInt(2);
            }

            if (l2 <= 0 && j5 == j3) {
               l4 -= 2;
            }

            int i5 = 0;
            if (entityplayer.world.getWorldInfo().isHardcoreModeEnabled()) {
               i5 = 5;
            }

            this.drawTexturedModalRect(k4, l4, 16 + i4 * 9, 9 * i5, 9, 9);
            if (flag) {
               if (j5 * 2 + 1 < j) {
                  this.drawTexturedModalRect(k4, l4, k5 + 54, 9 * i5, 9, 9);
               }

               if (j5 * 2 + 1 == j) {
                  this.drawTexturedModalRect(k4, l4, k5 + 63, 9 * i5, 9, 9);
               }
            }

            if (l2 > 0) {
               if (l2 == k1 && k1 % 2 == 1) {
                  this.drawTexturedModalRect(k4, l4, k5 + 153, 9 * i5, 9, 9);
                  l2--;
               } else {
                  this.drawTexturedModalRect(k4, l4, k5 + 144, 9 * i5, 9, 9);
                  l2 -= 2;
               }
            } else {
               if (j5 * 2 + 1 < i) {
                  this.drawTexturedModalRect(k4, l4, k5 + 36, 9 * i5, 9, 9);
               }

               if (j5 * 2 + 1 == i) {
                  this.drawTexturedModalRect(k4, l4, k5 + 45, 9 * i5, 9, 9);
               }
            }
         }

         Entity entity = entityplayer.getRidingEntity();
         if (entity == null || !(entity instanceof EntityLivingBase)) {
            this.mc.mcProfiler.endStartSection("food");
            boolean satureShow = Hud.get.showSaturationStats();

            for (int l5 = 0; l5 < 10; l5++) {
               int j6 = j1;
               int l6 = 16;
               int j7 = 0;
               if (entityplayer.isPotionActive(MobEffects.HUNGER)) {
                  l6 += 36;
                  j7 = 13;
               }

               if (entityplayer.getFoodStats().getSaturationLevel() <= 0.0F && this.updateCounter % (foodStat * 3 + 1) == 0) {
                  j6 = j1 + (this.rand.nextInt(3) - 1);
               }

               int l7 = i1 - l5 * 8 - 9;
               this.drawTexturedModalRect(l7, j6, 16 + j7 * 9, 27, 9, 9);
               if (l5 * 2 + 1 < foodStat) {
                  this.drawTexturedModalRect(l7, j6, l6 + 36, 27, 9, 9);
               }

               if (l5 * 2 + 1 == foodStat) {
                  this.drawTexturedModalRect(l7, j6, l6 + 45, 27, 9, 9);
               }

               if (satureShow) {
                  int xCount = 4;
                  double extXS = 0.5;
                  float aPCD = 0.125F;
                  GL11.glBlendFunc(770, 32772);

                  for (int is = 0; is < xCount; is++) {
                     GL11.glTranslated(0.0, -extXS, 0.0);
                     if ((float)(l5 * 2 + 1) < foodSaturation) {
                        this.drawTexturedModalRectAlphed(l7, j6, l6 + 36, 27, 9, 9, (1.0F - (float)is / 5.0F) * aPCD);
                     }

                     if ((float)(l5 * 2 + 1) == foodSaturation) {
                        this.drawTexturedModalRectAlphed(l7, j6, l6 + 45, 27, 9, 9, (1.0F - (float)is / 5.0F) * aPCD);
                     }
                  }

                  GL11.glTranslated(0.0, extXS * (double)xCount, 0.0);

                  for (int is = 0; is < xCount; is++) {
                     GL11.glTranslated(0.0, extXS, 0.0);
                     if ((float)(l5 * 2 + 1) < foodSaturation) {
                        this.drawTexturedModalRectAlphed(l7, j6, l6 + 36, 27, 9, 9, (1.0F - (float)is / 5.0F) * aPCD);
                     }

                     if ((float)(l5 * 2 + 1) == foodSaturation) {
                        this.drawTexturedModalRectAlphed(l7, j6, l6 + 45, 27, 9, 9, (1.0F - (float)is / 5.0F) * aPCD);
                     }
                  }

                  GL11.glTranslated(0.0, -extXS * (double)xCount, 0.0);

                  for (int is = 0; is < xCount; is++) {
                     GL11.glTranslated(extXS, 0.0, 0.0);
                     if ((float)(l5 * 2 + 1) < foodSaturation) {
                        this.drawTexturedModalRectAlphed(l7, j6, l6 + 36, 27, 9, 9, (1.0F - (float)is / 5.0F) * aPCD);
                     }

                     if ((float)(l5 * 2 + 1) == foodSaturation) {
                        this.drawTexturedModalRectAlphed(l7, j6, l6 + 45, 27, 9, 9, (1.0F - (float)is / 5.0F) * aPCD);
                     }
                  }

                  GL11.glTranslated(-extXS * (double)xCount, 0.0, 0.0);

                  for (int is = 0; is < xCount; is++) {
                     GL11.glTranslated(-extXS, 0.0, 0.0);
                     if ((float)(l5 * 2 + 1) < foodSaturation) {
                        this.drawTexturedModalRectAlphed(l7, j6, l6 + 36, 27, 9, 9, (1.0F - (float)is / 5.0F) * aPCD);
                     }

                     if ((float)(l5 * 2 + 1) == foodSaturation) {
                        this.drawTexturedModalRectAlphed(l7, j6, l6 + 45, 27, 9, 9, (1.0F - (float)is / 5.0F) * aPCD);
                     }
                  }

                  GL11.glTranslated(extXS * (double)xCount, 0.0, 0.0);
                  GL11.glBlendFunc(770, 771);
               }
            }
         }

         this.mc.mcProfiler.endStartSection("air");
         if (entityplayer.isInsideOfMaterial(Material.WATER)) {
            int i6 = Minecraft.player.getAir();
            int k6 = MathHelper.ceil((double)(i6 - 2) * 10.0 / 300.0);
            int i7 = MathHelper.ceil((double)i6 * 10.0 / 300.0) - k6;

            for (int k7 = 0; k7 < k6 + i7; k7++) {
               if (k7 < k6) {
                  this.drawTexturedModalRect(i1 - k7 * 8 - 9, k2, 16, 18, 9, 9);
               } else {
                  this.drawTexturedModalRect(i1 - k7 * 8 - 9, k2, 25, 18, 9, 9);
               }
            }
         }

         this.mc.mcProfiler.endSection();
      }
   }

   private void renderMountHealth(ScaledResolution p_184047_1_) {
      if (this.mc.getRenderViewEntity() instanceof EntityPlayer entityplayer && entityplayer.getRidingEntity() instanceof EntityLivingBase entitylivingbase) {
         this.mc.mcProfiler.endStartSection("mountHealth");
         int i = (int)Math.ceil((double)entitylivingbase.getHealth());
         float f = entitylivingbase.getMaxHealth();
         int j = (int)(f + 0.5F) / 2;
         if (j > 30) {
            j = 30;
         }

         int k = p_184047_1_.getScaledHeight() - 39;
         int l = p_184047_1_.getScaledWidth() / 2 + 91;
         int i1 = k;
         int j1 = 0;

         for (boolean flag = false; j > 0; j1 += 20) {
            int k1 = Math.min(j, 10);
            j -= k1;

            for (int l1 = 0; l1 < k1; l1++) {
               int i2 = 52;
               int j2 = 0;
               int k2 = l - l1 * 8 - 9;
               this.drawTexturedModalRect(k2, i1, 52 + j2 * 9, 9, 9, 9);
               if (l1 * 2 + 1 + j1 < i) {
                  this.drawTexturedModalRect(k2, i1, 88, 9, 9, 9);
               }

               if (l1 * 2 + 1 + j1 == i) {
                  this.drawTexturedModalRect(k2, i1, 97, 9, 9, 9);
               }
            }

            i1 -= 10;
         }
      }
   }

   private void renderPumpkinOverlay(ScaledResolution scaledRes) {
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.disableAlpha();
      this.mc.getTextureManager().bindTexture(PUMPKIN_BLUR_TEX_PATH);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
      bufferbuilder.pos(0.0, (double)scaledRes.getScaledHeight(), -90.0).tex(0.0, 1.0).endVertex();
      bufferbuilder.pos((double)scaledRes.getScaledWidth(), (double)scaledRes.getScaledHeight(), -90.0).tex(1.0, 1.0).endVertex();
      bufferbuilder.pos((double)scaledRes.getScaledWidth(), 0.0, -90.0).tex(1.0, 0.0).endVertex();
      bufferbuilder.pos(0.0, 0.0, -90.0).tex(0.0, 0.0).endVertex();
      tessellator.draw();
      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderVignette(float lightLevel, ScaledResolution scaledRes) {
      if (!Config.isVignetteEnabled()) {
         GlStateManager.enableDepth();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
      } else {
         lightLevel = 1.0F - lightLevel;
         lightLevel = MathHelper.clamp(lightLevel, 0.0F, 1.0F);
         WorldBorder worldborder = this.mc.world.getWorldBorder();
         float f = (float)worldborder.getClosestDistance(Minecraft.player);
         double d0 = Math.min(
            worldborder.getResizeSpeed() * (double)worldborder.getWarningTime() * 1000.0, Math.abs(worldborder.getTargetSize() - worldborder.getDiameter())
         );
         double d1 = Math.max((double)worldborder.getWarningDistance(), d0);
         if ((double)f < d1) {
            f = 1.0F - (float)((double)f / d1);
         } else {
            f = 0.0F;
         }

         this.prevVignetteBrightness = (float)((double)this.prevVignetteBrightness + (double)(lightLevel - this.prevVignetteBrightness) * 0.01);
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
         );
         if (f > 0.0F) {
            GlStateManager.color(0.0F, f, f, 1.0F);
         } else {
            GlStateManager.color(this.prevVignetteBrightness, this.prevVignetteBrightness, this.prevVignetteBrightness, 1.0F);
         }

         this.mc.getTextureManager().bindTexture(VIGNETTE_TEX_PATH);
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder bufferbuilder = tessellator.getBuffer();
         bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
         bufferbuilder.pos(0.0, (double)scaledRes.getScaledHeight(), -90.0).tex(0.0, 1.0).endVertex();
         bufferbuilder.pos((double)scaledRes.getScaledWidth(), (double)scaledRes.getScaledHeight(), -90.0).tex(1.0, 1.0).endVertex();
         bufferbuilder.pos((double)scaledRes.getScaledWidth(), 0.0, -90.0).tex(1.0, 0.0).endVertex();
         bufferbuilder.pos(0.0, 0.0, -90.0).tex(0.0, 0.0).endVertex();
         tessellator.draw();
         GlStateManager.depthMask(true);
         GlStateManager.enableDepth();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
      }
   }

   private void renderPortal(float timeInPortal, ScaledResolution scaledRes) {
      if (timeInPortal < 1.0F) {
         timeInPortal *= timeInPortal;
         timeInPortal *= timeInPortal;
         timeInPortal = timeInPortal * 0.8F + 0.2F;
      }

      GlStateManager.disableAlpha();
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      GlStateManager.color(1.0F, 1.0F, 1.0F, timeInPortal);
      this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
      TextureAtlasSprite textureatlassprite = this.mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.PORTAL.getDefaultState());
      float f = textureatlassprite.getMinU();
      float f1 = textureatlassprite.getMinV();
      float f2 = textureatlassprite.getMaxU();
      float f3 = textureatlassprite.getMaxV();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
      bufferbuilder.pos(0.0, (double)scaledRes.getScaledHeight(), -90.0).tex((double)f, (double)f3).endVertex();
      bufferbuilder.pos((double)scaledRes.getScaledWidth(), (double)scaledRes.getScaledHeight(), -90.0).tex((double)f2, (double)f3).endVertex();
      bufferbuilder.pos((double)scaledRes.getScaledWidth(), 0.0, -90.0).tex((double)f2, (double)f1).endVertex();
      bufferbuilder.pos(0.0, 0.0, -90.0).tex((double)f, (double)f1).endVertex();
      tessellator.draw();
      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderHotbarItem(int p_184044_1_, int p_184044_2_, float p_184044_3_, EntityPlayer player, ItemStack stack) {
      if (!stack.func_190926_b()) {
         float f = (float)stack.func_190921_D() - p_184044_3_;
         if (f > 0.0F) {
            GlStateManager.pushMatrix();
            float f1 = 1.0F + f / 5.0F;
            GlStateManager.translate((float)(p_184044_1_ + 8), (float)(p_184044_2_ + 12), 0.0F);
            GlStateManager.scale(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
            GlStateManager.translate((float)(-(p_184044_1_ + 8)), (float)(-(p_184044_2_ + 12)), 0.0F);
         }

         this.itemRenderer.renderItemAndEffectIntoGUI(player, stack, p_184044_1_, p_184044_2_);
         if (f > 0.0F) {
            GlStateManager.popMatrix();
         }

         if (Panic.stop) {
            this.itemRenderer.renderItemOverlays(this.mc.fontRendererObj, stack, p_184044_1_, p_184044_2_);
         } else {
            this.itemRenderer.renderItemOverlays(Fonts.comfortaa_18, stack, p_184044_1_, p_184044_2_);
         }
      }
   }

   public void updateTick() {
      if (this.mc.world == null) {
         TextureAnimations.updateAnimations();
      }

      if (this.recordPlayingUpFor > 0) {
         this.recordPlayingUpFor--;
      }

      if (this.titlesTimer > 0) {
         this.titlesTimer--;
         if (this.titlesTimer <= 0) {
            this.displayedTitle = "";
            this.displayedSubTitle = "";
         }
      }

      this.updateCounter++;
      if (Minecraft.player != null) {
         ItemStack itemstack = Minecraft.player.inventory.getCurrentItem();
         if (itemstack.func_190926_b()) {
            this.remainingHighlightTicks = 0;
         } else if (!this.highlightingItemStack.func_190926_b()
            && itemstack.getItem() == this.highlightingItemStack.getItem()
            && ItemStack.areItemStackTagsEqual(itemstack, this.highlightingItemStack)
            && (itemstack.isItemStackDamageable() || itemstack.getMetadata() == this.highlightingItemStack.getMetadata())) {
            if (this.remainingHighlightTicks > 0) {
               this.remainingHighlightTicks--;
            }
         } else {
            this.remainingHighlightTicks = 40;
         }

         this.highlightingItemStack = itemstack;
      }
   }

   public void setRecordPlayingMessage(String recordName) {
      this.setRecordPlaying(I18n.format("record.nowPlaying", recordName), true);
   }

   public void setRecordPlaying(String message, boolean isPlaying) {
      this.recordPlaying = message;
      this.recordPlayingUpFor = 60;
      this.recordIsPlaying = isPlaying;
   }

   public void displayTitle(String title, String subTitle, int timeFadeIn, int displayTime, int timeFadeOut) {
      if (title == null && subTitle == null && timeFadeIn < 0 && displayTime < 0 && timeFadeOut < 0) {
         this.displayedTitle = "";
         this.displayedSubTitle = "";
         this.titlesTimer = 0;
      } else if (title != null) {
         this.displayedTitle = title;
         this.titlesTimer = this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut;
      } else if (subTitle != null) {
         this.displayedSubTitle = subTitle;
      } else {
         if (timeFadeIn >= 0) {
            this.titleFadeIn = timeFadeIn;
         }

         if (displayTime >= 0) {
            this.titleDisplayTime = displayTime;
         }

         if (timeFadeOut >= 0) {
            this.titleFadeOut = timeFadeOut;
         }

         if (this.titlesTimer > 0) {
            this.titlesTimer = this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut;
         }
      }
   }

   public void setRecordPlaying(ITextComponent component, boolean isPlaying) {
      this.setRecordPlaying(component.getUnformattedText(), isPlaying);
   }

   public void func_191742_a(ChatType p_191742_1_, ITextComponent p_191742_2_) {
      for (IChatListener ichatlistener : this.field_191743_I.get(p_191742_1_)) {
         ichatlistener.func_192576_a(p_191742_1_, p_191742_2_);
      }
   }

   public GuiNewChat getChatGUI() {
      return this.persistantChatGUI;
   }

   public int getUpdateCounter() {
      return this.updateCounter;
   }

   public FontRenderer getFontRenderer() {
      return this.mc.fontRendererObj;
   }

   public GuiSpectator getSpectatorGui() {
      return this.spectatorGui;
   }

   public GuiPlayerTabOverlay getTabList() {
      return this.overlayPlayerList;
   }

   public void resetPlayersOverlayFooterHeader() {
      this.overlayPlayerList.resetFooterHeader();
      this.overlayBoss.clearBossInfos();
      this.mc.func_193033_an().func_191788_b();
   }

   public GuiBossOverlay getBossOverlay() {
      return this.overlayBoss;
   }
}
