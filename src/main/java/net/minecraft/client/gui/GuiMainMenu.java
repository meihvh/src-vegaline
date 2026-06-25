package net.minecraft.client.gui;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Runnables;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.SoundEvents;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec2f;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServerDemo;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import optifine.CustomPanorama;
import optifine.CustomPanoramaProperties;
import optifine.Reflector;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import ru.govno.client.Client;
import ru.govno.client.module.modules.ClickGui;
import ru.govno.client.module.modules.Hud;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.ui.login.GuiAltLogin;
import ru.govno.client.utils.ClientRP;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.NewYearUtil;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import viamcp.ViaMCP;

public class GuiMainMenu extends GuiScreen {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Random RANDOM = new Random();
   private final float updateCounter;
   private String splashText;
   private GuiButton buttonResetDemo;
   private float panoramaTimer;
   private DynamicTexture viewportTexture;
   private final Object threadLock = new Object();
   public static final String MORE_INFO_TEXT = "Please click " + TextFormatting.UNDERLINE + "here" + TextFormatting.RESET + " for more information.";
   private int openGLWarning2Width;
   private int openGLWarning1Width;
   private int openGLWarningX1;
   private int openGLWarningY1;
   private int openGLWarningX2;
   private int openGLWarningY2;
   private final String openGLWarning1;
   private final String openGLWarning2;
   private String openGLWarningLink;
   private static final ResourceLocation SPLASH_TEXTS = new ResourceLocation("texts/splashes.txt");
   private static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation("textures/gui/title/minecraft.png");
   private static final ResourceLocation field_194400_H = new ResourceLocation("textures/gui/title/edition.png");
   private static final ResourceLocation[] TITLE_PANORAMA_PATHS = new ResourceLocation[]{
      new ResourceLocation("textures/gui/title/background/panorama_0.png"),
      new ResourceLocation("textures/gui/title/background/panorama_1.png"),
      new ResourceLocation("textures/gui/title/background/panorama_2.png"),
      new ResourceLocation("textures/gui/title/background/panorama_3.png"),
      new ResourceLocation("textures/gui/title/background/panorama_4.png"),
      new ResourceLocation("textures/gui/title/background/panorama_5.png")
   };
   private ResourceLocation backgroundTexture;
   private GuiButton realmsButton;
   private boolean hasCheckedForRealmsNotification;
   private GuiScreen realmsNotification;
   private int field_193978_M;
   private int field_193979_N;
   private GuiButton modButton;
   private GuiScreen modUpdateNotification;
   private static boolean ee;
   private final List<GuiMainMenu.Part> particles1 = Lists.newArrayList();
   private final List<GuiMainMenu.Part> particles2 = Lists.newArrayList();
   private final List<GuiMainMenu.Part> particles3 = Lists.newArrayList();
   public static boolean qClicked = false;
   boolean clicked = true;
   static AnimationUtils st = new AnimationUtils(255.0F, 255.0F, 0.012F);
   public static AnimationUtils quit = new AnimationUtils(0.0F, 0.0F, 0.007F);
   public static AnimationUtils quit2 = new AnimationUtils(0.0F, 0.0F, 0.0275F);
   TimerHelper waitCloseResize = TimerHelper.TimerHelperReseted();
   boolean toClose = false;
   static float inter = 1.0F;
   static boolean f;
   static boolean n;
   static TimerHelper timeInter = TimerHelper.TimerHelperReseted();
   static AnimationUtils xNotifyAnim = new AnimationUtils(0.0F, 0.0F, 0.08F);
   static AnimationUtils setupSettingsAnim;
   boolean initClickGuiGuiimages;
   boolean clickedLC;
   boolean clickedRC;
   AnimationUtils hoverAnim = new AnimationUtils(0.0F, 0.0F, 0.04F);
   Random rand = new Random();

   public GuiMainMenu() {
      this.openGLWarning2 = MORE_INFO_TEXT;
      this.splashText = "missingno";
      if (Panic.stop) {
         IResource iresource = null;

         try {
            List<String> list = Lists.newArrayList();
            iresource = Minecraft.getMinecraft().getResourceManager().getResource(SPLASH_TEXTS);
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(iresource.getInputStream(), StandardCharsets.UTF_8));

            String s;
            while ((s = bufferedreader.readLine()) != null) {
               s = s.trim();
               if (!s.isEmpty()) {
                  list.add(s);
               }
            }

            if (!list.isEmpty()) {
               do {
                  this.splashText = list.get(RANDOM.nextInt(list.size()));
               } while (this.splashText.hashCode() == 125780783);
            }
         } catch (IOException var8) {
         } finally {
            IOUtils.closeQuietly(iresource);
         }
      }

      this.updateCounter = RANDOM.nextFloat();
      this.openGLWarning1 = "";
   }

   private boolean areRealmsNotificationsEnabled() {
      return Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && this.realmsNotification != null;
   }

   @Override
   public void updateScreen() {
      if (this.areRealmsNotificationsEnabled()) {
         this.realmsNotification.updateScreen();
      }

      if (Panic.stop) {
         this.particles1.clear();
         this.particles2.clear();
         this.particles3.clear();
      } else {
         try {
            if (this.mc == null) {
               return;
            }

            ScaledResolution sr = new ScaledResolution(this.mc);
            if (sr != null) {
               for (int i = 0; i < 5; i++) {
                  this.particles1.add(new GuiMainMenu.Part(sr, 6000.0F));
                  this.particles2.add(new GuiMainMenu.Part(sr, 6000.0F));
                  this.particles3.add(new GuiMainMenu.Part(sr, 6000.0F));
               }
            }

            this.particles1.removeIf(GuiMainMenu.Part::wantToRemove);
            this.particles1.forEach(GuiMainMenu.Part::update);
            this.particles2.removeIf(GuiMainMenu.Part::wantToRemove);
            this.particles2.forEach(GuiMainMenu.Part::update);
            this.particles3.removeIf(GuiMainMenu.Part::wantToRemove);
            this.particles3.forEach(GuiMainMenu.Part::update);
         } catch (Exception var3) {
            var3.printStackTrace();
            Sys.alert("Unknown err:", "pd8");
         }
      }
   }

   @Override
   public boolean doesGuiPauseGame() {
      return false;
   }

   @Override
   protected void keyTyped(char typedChar, int keyCode) throws IOException {
   }

   @Override
   public void initGui() {
      if (!Panic.stop) {
         ClientRP.getInstance().getDiscordRP().update("В главном меню игры", "Бездействует");
      }

      this.viewportTexture = new DynamicTexture(256, 256);
      this.backgroundTexture = this.mc.getTextureManager().getDynamicTextureLocation("background", this.viewportTexture);
      Calendar var1 = Calendar.getInstance();
      var1.setTime(new Date());
      if (var1.get(2) + 1 == 11 && var1.get(5) == 9) {
         this.splashText = "Happy birthday, ez!";
      } else if (var1.get(2) + 1 == 6 && var1.get(5) == 1) {
         this.splashText = "Happy birthday, Notch!";
      } else if (var1.get(2) + 1 == 12 && var1.get(5) == 24) {
         this.splashText = "Merry X-mas!";
      } else if (var1.get(2) + 1 == 1 && var1.get(5) == 1) {
         this.splashText = "Happy new year!";
      } else if (var1.get(2) + 1 == 10 && var1.get(5) == 31) {
         this.splashText = "OOoooOOOoooo! Spooky!";
      }

      int var3 = height / 2 - 78;
      if (this.mc.isDemo()) {
         this.addDemoButtons(var3, 24);
      } else {
         this.addSingleplayerMultiplayerButtons(var3, 24);
      }

      if (Panic.stop) {
         this.buttonList.add(new GuiButton(0, width / 2 - 100, var3 + 72 + 20, 98, 20, I18n.format("menu.options")));
         this.buttonList.add(new GuiButton(4, width / 2 + 2, var3 + 72 + 20, 98, 20, I18n.format("menu.quit")));
         if (Panic.stop) {
            this.buttonList.add(new GuiButtonLanguage(5, width / 2 - 124, var3 + 72 + 12));
         }
      }
   }

   private void addSingleplayerMultiplayerButtons(int p_73969_1_, int p_73969_2_) {
      if (Panic.stop) {
         this.buttonList.add(new GuiButton(1, width / 2 - 100, p_73969_1_ + 20, I18n.format("menu.singleplayer")));
         this.buttonList.add(new GuiButton(2, width / 2 - 100, p_73969_1_ + p_73969_2_ + 20, I18n.format("menu.multiplayer")));
         if (Reflector.GuiModList_Constructor.exists()) {
            this.realmsButton = this.addButton(
               new GuiButton(14, width / 2, p_73969_1_ + p_73969_2_ * 2, 98, 20, I18n.format("menu.online").replace("Minecraft", "").trim())
            );
            this.buttonList.add(this.modButton = new GuiButton(6, width / 2 - 100, p_73969_1_ + p_73969_2_ * 2, 98, 20, I18n.format("fml.menu.mods")));
         } else {
            this.realmsButton = this.addButton(
               new GuiButton(14, width / 2 - 100, p_73969_1_ + p_73969_2_ * 2 + 20, I18n.format(Panic.stop ? "Minecraft Realms" : "Аккаунты"))
            );
         }
      }
   }

   private void addDemoButtons(int p_73972_1_, int p_73972_2_) {
      this.buttonList.add(new GuiButton(11, width / 2 - 100, p_73972_1_, I18n.format("menu.playdemo")));
      this.buttonResetDemo = this.addButton(new GuiButton(12, width / 2 - 100, p_73972_1_ + p_73972_2_, I18n.format("menu.resetdemo")));
      ISaveFormat isaveformat = this.mc.getSaveLoader();
      WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");
      if (worldinfo == null) {
         this.buttonResetDemo.enabled = false;
      }
   }

   @Override
   public void actionPerformed(GuiButton button) throws IOException {
      if (button.id == 0) {
         this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
      }

      if (button.id == 1) {
         this.mc.displayGuiScreen(new GuiWorldSelection(this));
      }

      if (button.id == 2) {
         this.mc.displayGuiScreen(new GuiMultiplayer(this));
      }

      if (button.id == 14 && !Panic.stop) {
         this.mc.displayGuiScreen(new GuiAltLogin(this));
      }

      if (button.id == 4) {
         this.mc.shutdown();
      }

      if (button.id == 6 && Reflector.GuiModList_Constructor.exists()) {
         this.mc.displayGuiScreen((GuiScreen)Reflector.newInstance(Reflector.GuiModList_Constructor, this));
      }

      if (button.id == 11) {
         this.mc.launchIntegratedServer("Demo_World", "Demo_World", WorldServerDemo.DEMO_WORLD_SETTINGS);
      }

      if (button.id == 12) {
         ISaveFormat isaveformat = this.mc.getSaveLoader();
         WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");
         if (worldinfo != null) {
            this.mc
               .displayGuiScreen(
                  new GuiYesNo(
                     this,
                     I18n.format("selectWorld.deleteQuestion"),
                     "'" + worldinfo.getWorldName() + "' " + I18n.format("selectWorld.deleteWarning"),
                     I18n.format("selectWorld.deleteButton"),
                     I18n.format("gui.cancel"),
                     12
                  )
               );
         }
      }

      if (button.id == 69) {
      }
   }

   private void switchToRealms() {
      RealmsBridge realmsbridge = new RealmsBridge();
      realmsbridge.switchToRealms(this);
   }

   @Override
   public void confirmClicked(boolean result, int id) {
      if (result && id == 12) {
         ISaveFormat isaveformat = this.mc.getSaveLoader();
         isaveformat.flushCache();
         isaveformat.deleteWorldDirectory("Demo_World");
         this.mc.displayGuiScreen(this);
      } else if (id == 12) {
         this.mc.displayGuiScreen(this);
      } else if (id == 13) {
         if (result) {
            try {
               Class<?> oclass = Class.forName("java.awt.Desktop");
               Object object = oclass.getMethod("getDesktop").invoke(null);
               oclass.getMethod("browse", URI.class).invoke(object, new URI(this.openGLWarningLink));
            } catch (Throwable var5) {
               LOGGER.error("Couldn't open link", var5);
            }
         }

         this.mc.displayGuiScreen(this);
      }
   }

   private void drawPanorama(int mouseX, int mouseY, float partialTicks) {
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      GlStateManager.matrixMode(5889);
      GlStateManager.pushMatrix();
      GlStateManager.loadIdentity();
      Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
      GlStateManager.matrixMode(5888);
      GlStateManager.pushMatrix();
      GlStateManager.loadIdentity();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
      GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager.enableBlend();
      GlStateManager.disableAlpha();
      GlStateManager.disableCull();
      GlStateManager.depthMask(false);
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      int i = 8;
      int j = 64;
      CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();
      if (custompanoramaproperties != null) {
         j = custompanoramaproperties.getBlur1();
      }

      for (int k = 0; k < j; k++) {
         GlStateManager.pushMatrix();
         float f = ((float)(k % 8) / 8.0F - 0.5F) / 64.0F;
         float f1 = ((float)(k / 8) / 8.0F - 0.5F) / 64.0F;
         float f2 = 0.0F;
         GlStateManager.translate(f, f1, 0.0F);
         GlStateManager.rotate(MathHelper.sin(this.panoramaTimer / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate(-this.panoramaTimer * 0.1F, 0.0F, 1.0F, 0.0F);

         for (int l = 0; l < 6; l++) {
            GlStateManager.pushMatrix();
            if (l == 1) {
               GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            }

            if (l == 2) {
               GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            }

            if (l == 3) {
               GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            }

            if (l == 4) {
               GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            }

            if (l == 5) {
               GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
            }

            ResourceLocation[] aresourcelocation = TITLE_PANORAMA_PATHS;
            if (custompanoramaproperties != null) {
               aresourcelocation = custompanoramaproperties.getPanoramaLocations();
            }

            this.mc.getTextureManager().bindTexture(aresourcelocation[l]);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            int i1 = 255 / (k + 1);
            float f3 = 0.0F;
            bufferbuilder.pos(-1.0, -1.0, 1.0).tex(0.0, 0.0).color(255, 255, 255, i1).endVertex();
            bufferbuilder.pos(1.0, -1.0, 1.0).tex(1.0, 0.0).color(255, 255, 255, i1).endVertex();
            bufferbuilder.pos(1.0, 1.0, 1.0).tex(1.0, 1.0).color(255, 255, 255, i1).endVertex();
            bufferbuilder.pos(-1.0, 1.0, 1.0).tex(0.0, 1.0).color(255, 255, 255, i1).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
         }

         GlStateManager.popMatrix();
         GlStateManager.colorMask(true, true, true, false);
      }

      bufferbuilder.setTranslation(0.0, 0.0, 0.0);
      GlStateManager.colorMask(true, true, true, true);
      GlStateManager.matrixMode(5889);
      GlStateManager.popMatrix();
      GlStateManager.matrixMode(5888);
      GlStateManager.popMatrix();
      GlStateManager.depthMask(true);
      GlStateManager.enableCull();
      GlStateManager.enableDepth();
   }

   private void rotateAndBlurSkybox() {
      this.mc.getTextureManager().bindTexture(this.backgroundTexture);
      GlStateManager.glTexParameteri(3553, 10241, 9729);
      GlStateManager.glTexParameteri(3553, 10240, 9729);
      GlStateManager.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, 256, 256);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      GlStateManager.colorMask(true, true, true, false);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      GlStateManager.disableAlpha();
      int i = 3;
      int j = 3;
      CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();
      if (custompanoramaproperties != null) {
         j = custompanoramaproperties.getBlur2();
      }

      for (int k = 0; k < j; k++) {
         float f = 1.0F / (float)(k + 1);
         int l = width;
         int i1 = height;
         float f1 = (float)(k - 1) / 256.0F;
         bufferbuilder.pos((double)l, (double)i1, (double)this.zLevel).tex((double)(0.0F + f1), 1.0).color(1.0F, 1.0F, 1.0F, f).endVertex();
         bufferbuilder.pos((double)l, 0.0, (double)this.zLevel).tex((double)(1.0F + f1), 1.0).color(1.0F, 1.0F, 1.0F, f).endVertex();
         bufferbuilder.pos(0.0, 0.0, (double)this.zLevel).tex((double)(1.0F + f1), 0.0).color(1.0F, 1.0F, 1.0F, f).endVertex();
         bufferbuilder.pos(0.0, (double)i1, (double)this.zLevel).tex((double)(0.0F + f1), 0.0).color(1.0F, 1.0F, 1.0F, f).endVertex();
      }

      tessellator.draw();
      GlStateManager.enableAlpha();
      GlStateManager.colorMask(true, true, true, true);
   }

   private void renderSkybox(int mouseX, int mouseY, float partialTicks) {
      this.mc.getFramebuffer().unbindFramebuffer();
      GlStateManager.viewport(0, 0, 256, 256);
      this.drawPanorama(mouseX, mouseY, partialTicks);
      this.rotateAndBlurSkybox();
      int i = 3;
      CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();
      if (custompanoramaproperties != null) {
         i = custompanoramaproperties.getBlur3();
      }

      for (int j = 0; j < i; j++) {
         this.rotateAndBlurSkybox();
         this.rotateAndBlurSkybox();
      }

      this.mc.getFramebuffer().bindFramebuffer(true);
      GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
      float f2 = 120.0F / (float)(width > height ? width : height);
      float f = (float)height * f2 / 256.0F;
      float f1 = (float)width * f2 / 256.0F;
      int k = width;
      int l = height;
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      bufferbuilder.pos(0.0, (double)l, (double)this.zLevel).tex((double)(0.5F - f), (double)(0.5F + f1)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      bufferbuilder.pos((double)k, (double)l, (double)this.zLevel).tex((double)(0.5F - f), (double)(0.5F - f1)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      bufferbuilder.pos((double)k, 0.0, (double)this.zLevel).tex((double)(0.5F + f), (double)(0.5F - f1)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      bufferbuilder.pos(0.0, 0.0, (double)this.zLevel).tex((double)(0.5F + f), (double)(0.5F + f1)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      tessellator.draw();
   }

   List<String> buttons() {
      ArrayList<String> buttons = new ArrayList<>();
      buttons.add("Quit");
      buttons.add("Single");
      buttons.add("Multy");
      buttons.add("Altmgr");
      buttons.add("Settings");
      return buttons;
   }

   public void clickI(int i) {
      if (!qClicked) {
         inter = 0.0F;
         if (i == 0) {
            qClicked = true;
            if (Panic.stop) {
               this.mc.shutdown();
            } else {
               MusicHelper.playSound("main_quit.wav");
               quit2.to = 1.0F;
            }
         }

         if (i == 1) {
            this.mc.displayGuiScreen(new GuiWorldSelection(this));
         }

         if (i == 2) {
            this.mc.displayGuiScreen(new GuiMultiplayer(this));
         }

         if (i == 3) {
            this.mc.displayGuiScreen(new GuiAltLogin(this));
         }

         if (i == 4) {
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
         }

         if (i != 0 || Panic.stop) {
            this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }
      }
   }

   private float drawChangelogY2GET(float x, float y, ScaledResolution sr) {
      GL11.glTranslated(0.0, 0.0, 1.0);
      float hoverAnim = Math.min(this.hoverAnim.getAnim() * 1.05F, 1.0F);
      float hoverAnimString = Math.min(hoverAnim * 1.1F, 1.0F);
      String main = "Что нового в 092?";
      String main2 = MathUtils.getStringPercent("В кратце:", hoverAnimString);
      String descForOpen = "навестись что-бы узнать";
      String[] changes = new String[]{
         "добавлен новый чек 'GlaresMixSharpen'' в 'ESP'' для чека 'Targets'' с выбором 'Targets mode'' -> 'Glare''",
         "добавлен новый чек 'NormalizeDirectrion'' в 'ElytraBoost'' для для выбора 'Mode'' -> 'Firework'' с чеками 'StrafeDirs'' + 'StaticYMotions'' (обход меты)",
         "добавлен новый чек 'RemoveLagEnts'' -> в 'AntiBot'' (способен значительно повысить фпс на серверах загрязнённых копиями не стандартных существ)",
         "добавлен новый мод 'Penis'' (рисует хрен с физикой для вашего персонажа, можно избивать им таргета)",
         "предпринято много действий для устранения неполадок предыдущено обновления"
      };
      String[] dop = new String[]{"'фиксы''", "'немного бонусов''"};
      float padding = 3.0F + 2.5F * hoverAnim;
      int col1 = ColorUtils.getColor(255, (int)(100.0F + 155.0F * hoverAnimString));
      int col2 = ColorUtils.getColor(125, (int)(165.0F * hoverAnimString));
      if (hoverAnim > 0.0F) {
         Fonts.comfortaaRegular_22
            .drawStringWithBloom(
               main,
               x - Fonts.comfortaaRegular_22.getStringWidth(main) - 3.0F,
               y + (2.0F - 2.0F * hoverAnimString) - 1.0F,
               ColorUtils.swapAlpha(-1, 0.0F),
               hoverAnim * 0.3F,
               (int)(1.0F + 9.0F * MathUtils.valWave01((float)(System.currentTimeMillis() % 3000L) / 3000.0F))
            );
         Fonts.comfortaaRegular_22
            .drawStringWithOutline(main, x - Fonts.comfortaaRegular_22.getStringWidth(main) - 3.0F, y + (2.0F - 2.0F * hoverAnimString) - 1.0F, col1);
      } else {
         Fonts.comfortaaRegular_22
            .drawStringWithOutline(main, x - Fonts.comfortaaRegular_22.getStringWidth(main) - 3.0F, y + (2.0F - 2.0F * hoverAnimString) - 1.0F, col1);
      }

      int descCol = ColorUtils.swapAlpha(col1, (float)ColorUtils.getAlphaFromColor(col1) * (1.0F - hoverAnim));
      if (ColorUtils.getAlphaFromColor(descCol) >= 33) {
         float anim = Math.max(1.5F - (float)(System.currentTimeMillis() % 2000L) / 2000.0F - (float)(System.currentTimeMillis() % 666L) / 666.0F / 1.5F, 0.0F);
         anim = (float)MathUtils.easeInOutExpo((double)anim);
         Fonts.mntsb_7
            .drawStringWithBloomAndShadow(
               descForOpen,
               x
                  - Fonts.comfortaaRegular_22.getStringWidth(main)
                  - 3.0F
                  + 8.0F
                  + (this.hoverAnim.to == 1.0F ? hoverAnim * 24.0F : hoverAnim * hoverAnim * 24.0F),
               y + (2.0F - 2.0F * hoverAnimString) - 0.5F,
               descCol,
               anim,
               7
            );
      }

      y += 16.0F * hoverAnim;
      float minXText = x;
      if ((float)ColorUtils.getAlphaFromColor(col2) >= 32.0F) {
         for (String str : changes) {
            str = MathUtils.getStringPercent(str, hoverAnimString);
            if (str.length() != 0) {
               str = str.replace("(", "§7(").replace(")", "§7)§r").replace("[", "§c[").replace("]", "§c]§r").replace("''", "§r").replace("'", "§b");
               float var34 = x - Fonts.mntsb_13.getStringWidth(str);
               if (minXText > var34) {
                  minXText = var34;
               }

               Fonts.mntsb_13.addCachedrawStringWithShadow(str, var34, y, col2);
               y += padding;
            }
         }

         y += padding;
      }

      if (main2 == null) {
         return y;
      } else {
         col1 = ColorUtils.swapAlpha(col1, (float)ColorUtils.getAlphaFromColor(col1) * hoverAnim);
         if (ColorUtils.getAlphaFromColor(col1) >= 50) {
            Fonts.comfortaaRegular_22.drawStringWithOutline(main2, x - Fonts.comfortaaRegular_22.getStringWidth(main2) - 3.0F, y, col1);
         }

         y += 16.0F;
         if ((float)ColorUtils.getAlphaFromColor(col2) >= 32.0F) {
            for (String strx : dop) {
               strx = MathUtils.getStringPercent(strx, hoverAnimString);
               if (strx.length() != 0) {
                  strx = strx.replace("(", "§7(").replace(")", "§7)§r").replace("[", "§c[").replace("]", "§c]§r").replace("''", "§r").replace("'", "§b");
                  float var35 = x - Fonts.mntsb_13.getStringWidth(strx);
                  Fonts.mntsb_13.addCachedrawStringWithShadow(strx, var35, y, col2);
                  if (minXText > var35) {
                     minXText = var35;
                  }

                  y += padding;
               }
            }

            y += padding;
         }

         float panX = x + 3.5F;
         float panY = y - 2.0F;
         float panX2 = x + 4.0F;
         float panY2 = y - 4.0F;
         float hoverExpand = 8.0F;
         boolean hovered = RenderUtils.isHovered(
            (float)staticMouseX,
            (float)staticMouseY,
            x - Fonts.comfortaaRegular_22.getStringWidth(main) - hoverExpand,
            y - hoverExpand,
            x + hoverExpand,
            y + hoverExpand + Fonts.comfortaaRegular_22.getHeight()
         );
         int hoverTo = hovered ? 1 : 0;
         if (this.hoverAnim.to != (float)hoverTo && (hoverTo == 1 || this.hoverAnim.anim == 1.0F)) {
            this.hoverAnim.to = (float)hoverTo;

            try {
               MusicHelper.playSoundInstant("changelog" + (hovered ? "open" : "close") + ".wav", 0.1F);
            } catch (Exception var29) {
               var29.printStackTrace();
            }
         }

         this.hoverAnim.speed = this.hoverAnim.to == 0.0F ? 0.06F : 0.035F;
         RenderUtils.drawLightContureRectSmooth((double)panX, (double)panY, (double)panX2, (double)panY2, ColorUtils.swapAlpha(-1, 100.0F + 155.0F * hoverAnim));
         if (hoverAnim * 255.0F >= 1.0F) {
            int fillCol = ColorUtils.getColor(255, (int)(100.0F * hoverAnim));
            RenderUtils.drawAlphedRect((double)panX, (double)panY, (double)panX2, (double)panY2, fillCol);
         }

         if (hoverAnim == 0.0F && System.currentTimeMillis() % 400L < 200L) {
            RenderUtils.drawRect((double)panX, (double)panY, (double)panX2, (double)panY2, -1);
         }

         if (minXText < x - 3.0F) {
            int bgCol0 = ColorUtils.getColor(0, (int)(150.0F * hoverAnim * hoverAnim));
            int bgCol1 = ColorUtils.getColor(0, (int)(10.0F * hoverAnim * hoverAnim));
            int bgOutCol = ColorUtils.swapAlpha(0, (float)((int)(255.0F * hoverAnim * hoverAnim)));
            int bgOutSh = ColorUtils.swapAlpha(0, (float)((int)(135.0F * hoverAnim * hoverAnim)));
            RenderUtils.drawAlphedSideways((double)(minXText - 3.0F), (double)panY, (double)(x + 1.0F), (double)panY2, bgCol0, bgCol1);
            RenderUtils.drawLightContureRect((double)(minXText - 3.0F), (double)panY, (double)(x + 1.0F), (double)panY2, bgOutCol);
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               minXText - 3.0F, panY, x + 1.0F, panY2, 0.0F, 15.0F * hoverAnim, bgOutSh, bgOutSh, bgOutSh, bgOutSh, false, false, true
            );
         }

         Fonts.mntsb_13.drawAllCaches();
         GL11.glTranslated(0.0, 0.0, -1.0);
         return y;
      }
   }

   void k0warn(int w) {
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      try {
         try {
            if (!ee && this.mc != null) {
               ee = true;
            }
         } catch (Exception var33) {
            Sys.alert("Unknown err", "-ek6");
         }

         if (!this.initClickGuiGuiimages && ClickGui.instance != null && ClickGui.instance.Images.getBool()) {
            Arrays.asList(((ModeSettings)ClickGui.instance.getSetting("Image")).modes).forEach(another -> {
               if (!another.equalsIgnoreCase("playstationsfw") && !another.equalsIgnoreCase("playstationnsfw")) {
                  this.mc.getTextureManager().bindTexture(new ResourceLocation("vegaline/modules/clickgui/images/" + another.toLowerCase() + ".png"));
               } else {
                  String assetStore = "vegaline/modules/clickgui/images/playstation/" + another.toLowerCase();
                  this.mc.getTextureManager().bindTexture(new ResourceLocation(assetStore + "_base.png"));
                  this.mc.getTextureManager().bindTexture(new ResourceLocation(assetStore + "_overlay.png"));
               }
            });
            this.initClickGuiGuiimages = true;
         }

         if (!Panic.stop && (double)quit.getAnim() > 0.88) {
            ClientRP.getInstance().getDiscordRP().shutdown();
            this.mc.shutdown();
         }

         ScaledResolution sr = new ScaledResolution(this.mc);
         if (GuiMainMenu.st.getAnim() > 0.0F) {
            GuiMainMenu.st.to = -1.0F;
         }

         if (Panic.stop) {
            this.panoramaTimer += partialTicks;
            GlStateManager.disableAlpha();
            this.renderSkybox(mouseX, mouseY, partialTicks);
            GlStateManager.enableAlpha();
            int i = 274;
            int j = GuiMainMenu.width / 2 - 137;
            int k = 30;
            int l = -2130706433;
            int i1 = 16777215;
            int j1 = 0;
            int k1 = Integer.MIN_VALUE;
            CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();
            if (custompanoramaproperties != null) {
               l = custompanoramaproperties.getOverlay1Top();
               i1 = custompanoramaproperties.getOverlay1Bottom();
               j1 = custompanoramaproperties.getOverlay2Top();
               k1 = custompanoramaproperties.getOverlay2Bottom();
            }

            if (l != 0 || i1 != 0) {
               this.drawGradientRect(0, 0, GuiMainMenu.width, height, l, i1);
            }

            if (j1 != 0 || k1 != 0) {
               this.drawGradientRect(0, 0, GuiMainMenu.width, height, j1, k1);
            }

            this.mc.getTextureManager().bindTexture(MINECRAFT_TITLE_TEXTURES);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if ((double)this.updateCounter < 1.0E-4) {
               this.drawTexturedModalRect(j, 30, 0, 0, 99, 44);
               this.drawTexturedModalRect(j + 99, 30, 129, 0, 27, 44);
               this.drawTexturedModalRect(j + 99 + 26, 30, 126, 0, 3, 44);
               this.drawTexturedModalRect(j + 99 + 26 + 3, 30, 99, 0, 26, 44);
               this.drawTexturedModalRect(j + 155, 30, 0, 45, 155, 44);
            } else {
               this.drawTexturedModalRect(j, 30, 0, 0, 155, 44);
               this.drawTexturedModalRect(j + 155, 30, 0, 45, 155, 44);
            }

            this.mc.getTextureManager().bindTexture(field_194400_H);
            drawModalRectWithCustomSizedTexture(j + 88, 67, 0.0F, 0.0F, 98, 14, 128.0F, 16.0F);
            if (Reflector.ForgeHooksClient_renderMainMenu.exists()) {
               this.splashText = Reflector.callString(
                  Reflector.ForgeHooksClient_renderMainMenu, this, this.fontRendererObj, GuiMainMenu.width, height, this.splashText
               );
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate((float)(GuiMainMenu.width / 2 + 90), 70.0F, 0.0F);
            GlStateManager.rotate(-20.0F, 0.0F, 0.0F, 1.0F);
            float f = 1.8F - MathHelper.abs(MathHelper.sin((float)(Minecraft.getSystemTime() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
            f = f * 100.0F / (float)(this.fontRendererObj.getStringWidth(this.splashText) + 32);
            GlStateManager.scale(f, f, f);
            this.drawCenteredString(this.fontRendererObj, this.splashText, 0, -8, -256);
            GlStateManager.popMatrix();
            String s = ClientBrandRetriever.getFalseClientPrefix().trim();
            if (this.mc.isDemo()) {
               s = s + " Demo";
            } else {
               s = s + ("release".equalsIgnoreCase(this.mc.getVersionType()) ? "" : "/" + this.mc.getVersionType());
            }

            if (Reflector.FMLCommonHandler_getBrandings.exists()) {
               Object object = Reflector.call(Reflector.FMLCommonHandler_instance);
               List<String> list = Lists.reverse((List<String>)Reflector.call(object, Reflector.FMLCommonHandler_getBrandings, true));

               for (int l1 = 0; l1 < list.size(); l1++) {
                  String s1 = list.get(l1);
                  if (!Strings.isNullOrEmpty(s1)) {
                     this.drawString(this.fontRendererObj, s1, 2, height - (10 + l1 * (this.fontRendererObj.FONT_HEIGHT + 1)), 16777215);
                  }
               }
            } else {
               this.drawString(this.fontRendererObj, s, 2, height - 40, -1);
               this.drawString(this.fontRendererObj, "MCP 9.40", 2, height - 30, -1);
               this.drawString(this.fontRendererObj, "Powered by Forge 14.21.1.2443", 2, height - 20, -1);
               this.drawString(this.fontRendererObj, "4 mods loaded, 4 mods active", 2, height - 10, -1);
            }

            this.drawString(
               this.fontRendererObj,
               "Copyright Mojang AB. Do not distribute!",
               GuiMainMenu.width - this.mc.fontRendererObj.getStringWidth("Copyright Mojang AB. Do not distribute!") - 2,
               height - 10,
               -1
            );
            if (mouseX > this.field_193979_N
               && mouseX < this.field_193979_N + this.field_193978_M
               && mouseY > height - 10
               && mouseY < height
               && Mouse.isInsideWindow()) {
               drawRect(this.field_193979_N, (double)(height - 1), (double)(this.field_193979_N + this.field_193978_M), (double)height, -1);
            }

            if (this.openGLWarning1 != null && !this.openGLWarning1.isEmpty()) {
               drawRect(
                  this.openGLWarningX1 - 2,
                  (double)(this.openGLWarningY1 - 2),
                  (double)(this.openGLWarningX2 + 2),
                  (double)(this.openGLWarningY2 - 1),
                  1428160512
               );
               this.drawString(this.fontRendererObj, this.openGLWarning1, this.openGLWarningX1, this.openGLWarningY1, -1);
               this.drawString(
                  this.fontRendererObj, this.openGLWarning2, (GuiMainMenu.width - this.openGLWarning2Width) / 2, this.buttonList.get(0).yPosition - 12, -1
               );
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
            if (this.areRealmsNotificationsEnabled()) {
               this.realmsNotification.drawScreen(mouseX, mouseY, partialTicks);
            }

            if (this.modUpdateNotification != null) {
               this.modUpdateNotification.drawScreen(mouseX, mouseY, partialTicks);
            }
         } else {
            if (Mouse.isButtonDown(0)) {
               if (!this.clickedLC) {
                  if (ViaMCP.INSTANCE() != null) {
                     ViaMCP.INSTANCE().getViaPanel().mouseClick(mouseX, mouseY, 0);
                  }

                  if (mouseX >= sr.getScaledWidth() - 16 && mouseY >= sr.getScaledHeight() - 16) {
                     try {
                        Desktop.getDesktop().browse(URI.create("https://discord.gg/AWeGKFh7Dq"));
                        MusicHelper.playSound("browseurl.wav", 0.6F);
                     } catch (Exception var32) {
                     }
                  }
               }

               this.clickedLC = true;
            } else {
               this.clickedLC = false;
            }

            if (Mouse.isButtonDown(1)) {
               if (!this.clickedRC && ViaMCP.INSTANCE() != null) {
                  ViaMCP.INSTANCE().getViaPanel().mouseClick(mouseX, mouseY, 1);
               }

               this.clickedRC = true;
            } else {
               this.clickedRC = false;
            }

            if (inter < 1.0F) {
               RenderUtils.drawScreenShaderBackground(sr, mouseX, mouseY);

               for (int ix = 0; ix < 3; ix++) {
                  List<GuiMainMenu.Part> particles = new ArrayList<>();
                  float point = 1.0F;
                  switch (ix) {
                     case 1:
                        particles = this.particles1;
                        point = 3.0F;
                        break;
                     case 2:
                        particles = this.particles2;
                        point = 6.5F;
                        break;
                     case 3:
                        particles = this.particles3;
                        point = 12.0F;
                  }

                  if (!particles.isEmpty()) {
                     GL11.glEnable(3042);
                     GL11.glDisable(3553);
                     GL11.glEnable(2848);
                     GL11.glShadeModel(7425);
                     GL11.glDisable(3008);
                     GL11.glPointSize(point);
                     Tessellator tessellator = Tessellator.getInstance();
                     BufferBuilder bufferbuilder = tessellator.getBuffer();
                     bufferbuilder.begin(0, DefaultVertexFormats.POSITION_COLOR);
                     particles.forEach(
                        part -> {
                           float[] pos = part.drawPos(this.mc.getRenderPartialTicks());
                           float[] prev = part.drawPosPrev(this.mc.getRenderPartialTicks());
                           int color = ColorUtils.swapAlpha(
                              ColorUtils.getColor(0), 255.0F * (float)Math.min(MathUtils.easeInOutQuadWave((double)part.getAlphaPC()) * 2.0, 1.0)
                           );
                           bufferbuilder.pos((double)pos[0], (double)pos[1]).color(color).endVertex();
                           bufferbuilder.pos((double)prev[0], (double)prev[1]).color(color).endVertex();
                        }
                     );
                     tessellator.draw();
                     GL11.glPointSize(1.0F);
                     GL11.glEnable(3008);
                     GL11.glEnable(3553);
                     GL11.glPointSize(1.0F);
                  }
               }

               if (!this.toClose && !qClicked) {
                  ScaledResolution sr1 = new ScaledResolution(this.mc);
                  if (this.mc.gameSettings.guiScale != 2) {
                     GL11.glPushMatrix();
                     RenderUtils.customScaledObject2D((float)sr.getScaledWidth(), 0.0F, 0.0F, 0.0F, 1.0F / (float)ScaledResolution.getScaleFactor() * 2.0F);
                  }

                  this.drawChangelogY2GET((float)sr1.getScaledWidth() - 8.0F, 8.0F, sr1);
                  if (this.mc.gameSettings.guiScale > 2) {
                     GL11.glPopMatrix();
                  }
               }

               float midUPX = (float)sr.getScaledWidth() / 2.0F;
               float midUPY = (float)sr.getScaledHeight() / 2.0F - 64.0F;
               float texW = 256.0F;
               float texH = 64.0F;
               ResourceLocation logo = new ResourceLocation("vegaline/ui/mainmenu/logo.png");
               GL11.glPushMatrix();
               RenderUtils.drawImageWithAlpha(logo, midUPX - texW / 2.0F - 1.0F, midUPY - texH / 2.0F + 2.0F, texW, texH, 0, 80);
               RenderUtils.drawImageWithAlpha(logo, midUPX - texW / 2.0F - 1.0F, midUPY - texH / 2.0F + 4.0F, texW, texH, 0, 150);
               RenderUtils.drawImageWithAlpha(logo, midUPX - texW / 2.0F, midUPY - texH / 2.0F, texW, texH, -1, 255);
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               RenderUtils.drawImageWithAlpha(
                  logo, midUPX - texW / 2.0F, midUPY - texH / 2.0F - 0.5F / (float)ScaledResolution.getScaleFactor(), texW, texH, -1, 85
               );
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               NewYearUtil.insertRenderMainMenuLogo(
                  midUPX - texW / 2.0F + texW * 0.345F, midUPY - texH / 2.0F - 0.5F / (float)ScaledResolution.getScaleFactor() + 14.5F
               );
               if (mouseX >= sr.getScaledWidth() - 16 && mouseY >= sr.getScaledHeight() - 16) {
                  GL11.glTranslated(-6.0, -6.0, 0.0);
                  GL11.glPushMatrix();
                  float timePC = (float)(System.currentTimeMillis() % 1200L) / 1200.0F;
                  float timePC2 = ((double)timePC > 0.5 ? 1.0F - timePC : timePC) * 2.0F;
                  RenderUtils.customRotatedObject2D((float)(sr.getScaledWidth() - 8), (float)(sr.getScaledHeight() - 8), 0.0F, 0.0F, (double)(timePC * 360.0F));
                  RenderUtils.drawCircledTHud((float)(sr.getScaledWidth() - 8), (double)(sr.getScaledHeight() - 8), 8.0F, timePC2, -1, 255.0F, 1.0F);
                  RenderUtils.drawCircledTHud((float)(sr.getScaledWidth() - 8), (double)(sr.getScaledHeight() - 8), 10.0F, 1.0F - timePC2, -1, 255.0F, 1.0F);
                  float time2PC = (float)((System.currentTimeMillis() + 600L) % 1000L) / 1000.0F;
                  float time2PC2 = ((double)time2PC > 0.5 ? 1.0F - time2PC : time2PC) * 2.0F;
                  RenderUtils.drawCircledTHud(
                     (float)(sr.getScaledWidth() - 8),
                     (double)(sr.getScaledHeight() - 8),
                     9.0F + 6.0F * time2PC,
                     1.0F,
                     -1,
                     155.0F * time2PC2,
                     1.0F + time2PC2 * 3.0F
                  );
                  GL11.glPopMatrix();
               }

               RenderUtils.drawImageWithAlpha(
                  new ResourceLocation("vegaline/ui/mainmenu/icons/urlicons/nightsquadlink.png"),
                  (float)(sr.getScaledWidth() - 16),
                  (float)(sr.getScaledHeight() - 16),
                  16.0F,
                  16.0F,
                  -1,
                  255
               );
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE,
                  GlStateManager.SourceFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.DestFactor.ZERO
               );
               GL11.glPopMatrix();
               int textC = ColorUtils.swapAlpha(-1, 65.0F);
               int fps = Minecraft.getDebugFPS();
               if (Hud.get != null) {
                  Hud.get.frameCounter.renderThreadRead((int)MathUtils.clamp(Hud.get.frameCounter.getFps() / 3.33333F, 10.0, 50.0));
                  fps = (int)Hud.get.frameCounter.getFps();
               }

               Fonts.mntsb_12.drawStringWithShadow("uid: " + Minecraft.unique_Index, 3.0F, (float)(sr.getScaledHeight() - 32), textC);
               Fonts.mntsb_12.drawStringWithShadow("fps: " + fps, 3.0F, (float)(sr.getScaledHeight() - 24), textC);
               Fonts.mntsb_12.drawStringWithShadow("ver: " + Client.version.replace("#00", "0"), 3.0F, (float)(sr.getScaledHeight() - 16), textC);
               Fonts.mntsb_12.drawStringWithShadow("author: luluv1", 3.0F, (float)(sr.getScaledHeight() - 8), textC);
               int eki = sr.getScaledWidth() <= 600 ? 7 : 0;
               int size = sr.getScaledWidth() > 600 ? 64 : 32;
               float step = (float)(sr.getScaledWidth() > 600 ? 76 : 36);
               int alphaIcon = 255;
               int width = 0;
               int ix = 0;

               for (String button : this.buttons()) {
                  width = (int)((float)width + step);
               }

               int y = sr.getScaledHeight() / 2;
               int x = sr.getScaledWidth() / 2 - width / 2;

               try {
                  for (String button : this.buttons()) {
                     ix = (mouseX - x) / (int)step;
                  }
               } catch (Exception var34) {
               }

               x = (int)((float)x - step);
               int stepDelta = 0;
               float xStep = 0.0F;
               int tesed = 2;
               float xNotify = -1000.0F;
               float yNotify = -1000.0F;
               String buttonOnMouse = null;
               boolean click = false;

               for (String button : this.buttons()) {
                  if (RenderUtils.isHovered((float)mouseX, (float)mouseY, (float)x + step, (float)y, (float)(x + width + 4 - x), (float)y + step - (float)y)
                     && ix >= 0
                     && ix <= 4
                     && this.buttons().get(ix) != null) {
                     buttonOnMouse = this.buttons().get(ix);
                     if ((float)ix >= 0.0F && (float)ix <= 4.0F) {
                        xNotify = (float)x + (float)size / 2.0F + step * (float)ix + step + 2.0F;
                        yNotify = (float)sr.getScaledHeight() / 2.0F - step / 3.0F + 20.0F - (float)((int)((float)eki * 1.5F));
                     }
                  }

                  stepDelta++;

                  for (int st = 0; st < stepDelta; st++) {
                     xStep += step / (float)stepDelta;
                  }

                  String res = button.toLowerCase();
                  ResourceLocation icon = new ResourceLocation("vegaline/ui/mainmenu/icons/buttonicons/" + res + ".png");
                  float ext = buttonOnMouse != null
                     ? (float)(
                        (int)(-3.0F * (1.0F - Math.min(MathUtils.getDifferenceOf(xNotifyAnim.getAnim(), (float)x + xStep + step / 2.0F + 2.0F) / 48.0F, 1.0F)))
                     )
                     : 0.0F;
                  if (buttonOnMouse != null && buttonOnMouse.equalsIgnoreCase(button)) {
                     click = true;
                  }

                  GL11.glTranslated(0.0, (double)ext, 0.0);
                  GL11.glEnable(3042);
                  GL11.glEnable(3553);
                  GlStateManager.tryBlendFuncSeparate(
                     GlStateManager.SourceFactor.SRC_ALPHA,
                     GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                     GlStateManager.SourceFactor.ONE,
                     GlStateManager.DestFactor.ZERO
                  );
                  RenderUtils.drawImageWithAlpha(
                     icon, (float)x + xStep + (float)tesed - 1.5F, (float)(y + tesed) + 1.0F, (float)size, (float)size, 0, (int)((float)alphaIcon / 1.5F)
                  );
                  RenderUtils.drawImageWithAlpha(
                     icon, (float)x + xStep + (float)tesed - 1.0F, (float)(y + tesed) + 3.0F, (float)size, (float)size, 0, (int)((float)alphaIcon / 4.0F)
                  );
                  GlStateManager.tryBlendFuncSeparate(
                     GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
                  );
                  RenderUtils.drawImageWithAlpha(
                     icon, (float)x + xStep + (float)tesed, (float)(y + tesed), (float)size, (float)size, ColorUtils.getColor(200), alphaIcon
                  );
                  GlStateManager.tryBlendFuncSeparate(
                     GlStateManager.SourceFactor.SRC_ALPHA,
                     GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                     GlStateManager.SourceFactor.ONE,
                     GlStateManager.DestFactor.ZERO
                  );
                  GL11.glTranslated(0.0, (double)(-ext), 0.0);
               }

               RenderUtils.resetBlender();
               GlStateManager.enableBlend();
               ArrayList<Vec2f> vecs = new ArrayList<>();
               if (xNotify != -1000.0F && yNotify != -1000.0F) {
                  if (xNotifyAnim.getAnim() == 0.0F) {
                     xNotifyAnim.setAnim(xNotify);
                  }

                  xNotifyAnim.to = xNotify;
               } else {
                  xNotifyAnim.setAnim((float)mouseX);
               }

               vecs.add(new Vec2f(xNotifyAnim.getAnim(), yNotify));
               vecs.add(new Vec2f(xNotifyAnim.getAnim() - 5.0F, yNotify - 7.5F));
               vecs.add(new Vec2f(xNotifyAnim.getAnim() + 5.0F, yNotify - 7.5F));
               int notifyCol = ColorUtils.swapAlpha(-1, 65.0F);
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
               );
               RenderUtils.drawSome(vecs, notifyCol);
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               vecs.clear();
               if (yNotify > 0.0F) {
                  RenderUtils.fullRoundFG(
                     xNotifyAnim.getAnim() - Fonts.noise_24.getStringWidth(buttonOnMouse) / 2.0F - 4.0F,
                     yNotify - 26.0F,
                     xNotifyAnim.anim + Fonts.noise_24.getStringWidth(buttonOnMouse) / 2.0F + 4.0F,
                     yNotify - 7.5F,
                     10.0F,
                     notifyCol,
                     notifyCol,
                     notifyCol,
                     notifyCol,
                     true
                  );
                  Fonts.noise_24.drawString(buttonOnMouse, xNotifyAnim.anim - Fonts.noise_24.getStringWidth(buttonOnMouse) / 2.0F, yNotify - 22.0F, -1);
               }

               if (!Panic.stop && ViaMCP.INSTANCE() != null) {
                  ViaMCP.INSTANCE().getViaPanel().drawPanel(1.0F - inter, mouseX, mouseY);
               }

               if (quit.to != 1.0F) {
                  super.drawScreen(mouseX, mouseY, partialTicks);
               }

               if (Mouse.isButtonDown(0) && (setupSettingsAnim == null || !(setupSettingsAnim.anim * 255.0F > 1.0F) && setupSettingsAnim.to == 0.0F)) {
                  if (!this.clicked) {
                     return;
                  }

                  if (click) {
                     this.clickI(ix);
                  }

                  this.clicked = false;
               } else {
                  this.clicked = true;
               }
            }
         }

         if (this.toClose && this.waitCloseResize.hasReached(850.0)) {
            MusicHelper.playSound("main_quit.wav");
            quit2.to = 1.0F;
            this.toClose = false;
         }

         if (quit2.getAnim() > 0.0F) {
            float quitKachel = (float)MathUtils.easeOutCubic(MathUtils.easeInOutQuadWave((double)quit2.anim));
            int solveColor = ColorUtils.swapAlpha(-1, 255.0F * quitKachel);
            float scale = (float)sr.getScaledWidth() / 1.75F * quit2.anim;
            float x = (float)sr.getScaledWidth() / 2.0F;
            float y = (float)sr.getScaledHeight() / 2.0F;
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               x - scale, y - scale, x + scale, y + scale, scale, scale, solveColor, solveColor, solveColor, solveColor, true, true, true
            );
            if ((double)quit2.anim > 0.97) {
               quit.to = 1.0F;
            }
         }

         if (quit.getAnim() > 0.0F) {
            float q = Math.min(MathUtils.lerp(quit.anim, 1.0F - (float)(1.0 - Math.pow((double)quit.anim, 5.0)), 0.4F) * 1.25F, 1.0F);
            ClientRP.getInstance().getDiscordRP().update("Выходит из игры", "Покидает мир кубов");
            RenderUtils.drawAlphedRect(
               0.0,
               0.0,
               (double)sr.getScaledWidth(),
               (double)sr.getScaledHeight(),
               ColorUtils.getColor(
                  (int)(26.0F * (1.0F - q)), (int)(26.0F * (1.0F - q)), (int)(34.0F * (1.0F - q)), MathUtils.clamp(255.0F * q * 2.0F, 0.0F, 255.0F)
               )
            );
            if (quit.to != 0.0F && q * 255.0F >= 1.0F) {
               float gap = ((double)q > 0.5 ? 1.0F - q : q) * 2.5F;
               float var46;
               int c = ColorUtils.getOverallColorFrom(0, -1, var46 = MathUtils.clamp(gap, 0.0F, 1.0F));
               c = ColorUtils.swapAlpha(c, (float)ColorUtils.getAlphaFromColor(c) * var46);
               if (ColorUtils.getAlphaFromColor(c) >= 33) {
                  Fonts.neverlose500_18
                     .drawString(
                        "До скорой встречи",
                        (float)sr.getScaledWidth() / 2.0F - Fonts.neverlose500_18.getStringWidth("До скорой встречи") / 2.0F,
                        (float)sr.getScaledHeight() / 2.0F + 120.0F - 120.0F * q,
                        c
                     );
               }
            }
         }

         if (inter != 0.0F && (timeInter.hasReached(700.0) || GuiMainMenu.f)) {
            if (!GuiMainMenu.f) {
               this.mc.updateFramebufferSize();
               MusicHelper.playSound("main_init.wav", 0.6F);
               timeInter.reset();
               GuiMainMenu.f = true;
            } else if (!n) {
               this.mc.updateFramebufferSize();
               n = true;
            }

            inter = MathUtils.clamp(1.0F - (float)MathUtils.easeOutCubic((double)((float)(timeInter.getTime() - 700L) / 3000.0F)), 0.0F, 1.0F);
         } else if (inter == 1.0F) {
            timeInter.reset();
         }

         int aPC = (int)(inter * 255.0F);
         if (aPC >= 1) {
            GL11.glDisable(2929);
            int bgColor = ColorUtils.getColor(27, 27, 33, aPC);
            int bgColor2 = ColorUtils.getColor(27, 27, 33, (int)(MathUtils.easeInOutQuadWave((double)(1.0F - inter * inter)) * 255.0));
            int textCx = ColorUtils.getColor(255, aPC);
            RenderUtils.drawAlphedRect(0.0, 0.0, (double)sr.getScaledWidth(), (double)sr.getScaledHeight(), bgColor);
            if (aPC >= 33) {
               Fonts.neverlose500_18
                  .drawStringWithShadow(
                     "Вегуля любимый :)",
                     (float)sr.getScaledWidth() / 2.0F - Fonts.neverlose500_18.getStringWidth("Вегуля любимый :)") / 2.0F,
                     (float)sr.getScaledHeight() / 2.0F + (1.0F - inter) * 30.0F,
                     textCx
                  );
            }

            float scale = (float)sr.getScaledWidth() / 1.75F * (1.0F - inter * inter);
            float x = (float)sr.getScaledWidth() / 2.0F;
            float y = (float)sr.getScaledHeight() / 2.0F;
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               x - scale, y - scale, x + scale, y + scale, scale, scale, bgColor2, bgColor2, bgColor2, bgColor2, false, false, true
            );
            RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
               x - scale, y - scale, x + scale, y + scale, scale / 2.0F, scale / 2.0F, 0.0F, bgColor2, bgColor2, bgColor2, bgColor2, false, true, false
            );
            if (Client.isFirstStarting && setupSettingsAnim == null) {
               setupSettingsAnim = new AnimationUtils(0.0F, 1.0F, 0.012F);
               this.mc.gameSettings.keyBindSprint.setKeyCode(this.mc.gameSettings.keyBindForward.getKeyCode());
               this.mc.gameSettings.ofFastRender = false;
               this.mc.gameSettings.ofTrees = 2;
               this.mc.gameSettings.ofLagometer = false;
               this.mc.gameSettings.fancyGraphics = false;
               this.mc.gameSettings.renderDistanceChunks = 7;
               this.mc.gameSettings.autoJump = false;
               this.mc.gameSettings.useVbo = true;
               this.mc.gameSettings.ofFastMath = true;
               this.mc.gameSettings.mipmapLevels = 0;
               this.mc.gameSettings.ofAaLevel = 0;
               this.mc.gameSettings.limitFramerate = (int)GameSettings.Options.FRAMERATE_LIMIT.getValueMax();
               this.mc.gameSettings.enableVsync = false;
            }
         }

         RenderUtils.fixShadows();
         if (setupSettingsAnim != null) {
            GL11.glDisable(2929);
            setupSettingsAnim.getAnim();
            if ((double)setupSettingsAnim.anim > 0.9992) {
               setupSettingsAnim.to = 0.0F;
               setupSettingsAnim.speed = 0.03F;
            }

            if (setupSettingsAnim.anim * 255.0F >= 1.0F) {
               String msg = "Производится первичная настройка игры...";
               float tw = 20.0F + Fonts.noise_24.getStringWidth(msg);
               float th = 36.0F;
               float tx = (float)sr.getScaledWidth() / 2.0F - tw / 2.0F;
               float ty = (float)sr.getScaledHeight() / 2.0F - th / 2.0F;
               float aTPC = (float)MathUtils.easeInOutQuad((double)setupSettingsAnim.anim);
               int bgTCol = ColorUtils.getColor(11, 11, 28, 190.0F * aTPC);
               int textTCol = ColorUtils.getColor(255, 255, 255, 190.0F * aTPC);
               RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                  tx, ty, tx + tw, ty + th, 6.0F, 3.0F, bgTCol, bgTCol, bgTCol, bgTCol, false, true, true
               );
               if (ColorUtils.getAlphaFromColor(textTCol) >= 26) {
                  Fonts.noise_24.drawStringWithShadow(msg, tx + tw / 2.0F - Fonts.noise_24.getStringWidth(msg) / 2.0F, ty + th / 2.0F - 5.0F, textTCol);
               }
            }
         }
      } catch (Exception var35) {
         Sys.alert("Unknown err", "-sr0");
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      synchronized (this.threadLock) {
         if (!this.openGLWarning1.isEmpty()
            && !StringUtils.isNullOrEmpty(this.openGLWarningLink)
            && mouseX >= this.openGLWarningX1
            && mouseX <= this.openGLWarningX2
            && mouseY >= this.openGLWarningY1
            && mouseY <= this.openGLWarningY2) {
            GuiConfirmOpenLink guiconfirmopenlink = new GuiConfirmOpenLink(this, this.openGLWarningLink, 13, true);
            guiconfirmopenlink.disableSecurityWarning();
            this.mc.displayGuiScreen(guiconfirmopenlink);
         }
      }

      if (this.areRealmsNotificationsEnabled()) {
         this.realmsNotification.mouseClicked(mouseX, mouseY, mouseButton);
      }

      if (mouseX > this.field_193979_N && mouseX < this.field_193979_N + this.field_193978_M && mouseY > height - 10 && mouseY < height) {
         this.mc.displayGuiScreen(new GuiWinGame(false, Runnables.doNothing()));
      }
   }

   @Override
   public void onGuiClosed() {
      if (this.realmsNotification != null) {
         this.realmsNotification.onGuiClosed();
      }
   }

   private class Part {
      float maxTime;
      Vec2f pos;
      Vec2f prevPos;
      Vec2f motion;
      TimerHelper timer = TimerHelper.TimerHelperReseted();

      public Part(ScaledResolution sr, float maxTime) {
         this.pos = new Vec2f(
            GuiMainMenu.this.rand.nextFloat((float)sr.getScaledWidth()),
            (float)sr.getScaledHeight() / 2.0F + GuiMainMenu.this.rand.nextFloat((float)sr.getScaledHeight() / 2.0F)
         );
         this.prevPos = new Vec2f(this.pos.x, this.pos.y);
         this.motion = new Vec2f(-4.0F + GuiMainMenu.this.rand.nextFloat(24.0F), -GuiMainMenu.this.rand.nextFloat(3.0F, 15.0F));
         this.maxTime = maxTime;
         this.timer.reset();
      }

      private void update() {
         this.prevPos.x = this.pos.x;
         this.prevPos.y = this.pos.y;
         this.pos.x = this.pos.x + this.motion.x;
         this.pos.y = this.pos.y + this.motion.y;
         this.motion.y -= 0.5F;
         this.motion.y = (float)((double)this.motion.y * 0.97);
         if (this.motion.y < -40.0F) {
            this.motion.y = -40.0F;
         }

         this.motion.x *= 0.98F;
      }

      public float[] drawPos(float partialTicks) {
         return new float[]{MathUtils.lerp(this.prevPos.x, this.pos.x, partialTicks), MathUtils.lerp(this.prevPos.y, this.pos.y, partialTicks)};
      }

      public float[] drawPosPrev(float partialTicks) {
         float spdExt = (float)Math.sqrt((double)(this.motion.x * this.motion.x + this.motion.y * this.motion.y)) / 2.0F;
         return new float[]{
            MathUtils.lerp(this.prevPos.x, this.pos.x, partialTicks) - this.motion.x / spdExt,
            MathUtils.lerp(this.prevPos.y, this.pos.y, partialTicks) - this.motion.y / spdExt
         };
      }

      public float getAlphaPC() {
         float ms = (float)this.timer.getTime();
         return Math.max(1.0F - ms / this.maxTime, 0.0F);
      }

      public boolean wantToRemove() {
         return this.getAlphaPC() <= 0.0F
            || this.pos.x < 0.0F
            || this.pos.x > (float)GuiMainMenu.this.mc.displayWidth / 2.0F
            || this.pos.y > (float)GuiMainMenu.this.mc.displayHeight / 2.0F;
      }
   }
}
