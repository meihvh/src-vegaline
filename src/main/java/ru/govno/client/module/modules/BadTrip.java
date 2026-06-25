package ru.govno.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.clickgui.GuiMusicTuner;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class BadTrip extends Module {
   public static BadTrip get;
   public BoolSettings CrazySpider;
   public BoolSettings PsichoTnt;
   public BoolSettings SpreadCreeper;
   public BoolSettings FlattenPlayersHurt;
   public BoolSettings SteveChiken;
   public BoolSettings VillagerNose;
   public BoolSettings HumpPlayers;
   public BoolSettings SelfBuff;
   public BoolSettings AmbientSound;
   public BoolSettings Alkash;
   public BoolSettings DirtKakish;
   public BoolSettings Ma4o3DBoom;
   FloatSettings SoundVolume;
   public ModeSettings HumpStrengh;
   public ModeSettings Sound;
   public static ResourceLocation STEVE = new ResourceLocation("textures/entity/steve.png");
   public final GuiMusicTuner ambientTuner;
   private ResourceLocation[] loadedBoomTextures = new ResourceLocation[0];
   private boolean boomTexturesLoaded = false;
   private int boomWaiterOverlayLoadFramesPreLoad;
   private final TimerHelper timerBoom = TimerHelper.TimerHelperReseted();
   private int endBoomAnimTime;
   private boolean drawBoomState;
   private boolean hasTargetMinHP;
   private boolean hasMeOverSpeed;
   private boolean hasMeOverTimeJumping;
   private boolean hasMeOverFallDist;
   private int ticksMeJumping;
   private final Vec3d lastMePos = new Vec3d(0.0, 0.0, 0.0);
   private boolean oldSneak;
   private static boolean onDisableAmbientSoundIsEnabled;

   public BadTrip() {
      super("BadTrip", 0, Module.Category.MISC);
      this.settings.add(this.CrazySpider = new BoolSettings("CrazySpider", true, this));
      this.settings.add(this.PsichoTnt = new BoolSettings("PsichoTnt", true, this));
      this.settings.add(this.SpreadCreeper = new BoolSettings("SpreadCreeper", true, this));
      this.settings.add(this.FlattenPlayersHurt = new BoolSettings("FlattenPlayersHurt", true, this));
      this.settings.add(this.SteveChiken = new BoolSettings("SteveChiken", true, this));
      this.settings.add(this.VillagerNose = new BoolSettings("VillagerNose", true, this));
      this.settings.add(this.HumpPlayers = new BoolSettings("HumpPlayers", false, this));
      this.settings
         .add(this.HumpStrengh = new ModeSettings("HumpStrengh", "Middle", this, new String[]{"Lower", "Middle", "Highest"}, () -> this.HumpPlayers.getBool()));
      this.settings.add(this.SelfBuff = new BoolSettings("SelfBuff", false, this));
      this.settings.add(this.AmbientSound = new BoolSettings("AmbientSound", false, this));
      this.settings.add(this.Sound = new ModeSettings("Sound", "Toilet", this, new String[]{"Toilet", "HiFi-Forest"}, () -> this.AmbientSound.getBool()));
      this.settings.add(this.SoundVolume = new FloatSettings("SoundVolume", 40.0F, 200.0F, 5.0F, this, () -> this.AmbientSound.getBool()));
      this.ambientTuner = new GuiMusicTuner("ambient" + (this.Sound.currentMode.equalsIgnoreCase("Toilet") ? "1" : "2"), 0.2F);
      this.settings.add(this.Alkash = new BoolSettings("Alkash", true, this));
      this.settings.add(this.DirtKakish = new BoolSettings("DirtKakish", true, this));
      this.settings.add(this.Ma4o3DBoom = new BoolSettings("Ma4o3DBoom", false, this));
      this.setDemand(1, 3);
      get = this;
   }

   @Override
   public void onRender2D(ScaledResolution sr) {
      this.doDrawBoomOverlayWhileState(sr, 1.0F);
   }

   @Override
   public void onUpdateLimitedDelay() {
      if (this.Ma4o3DBoom.getBool()) {
         if (!this.boomTexturesLoaded) {
            if (this.boomWaiterOverlayLoadFramesPreLoad < 10) {
               return;
            }

            this.getLoadedBoomTextures();
         }

         this.updateBoomEvents();
      }
   }

   private int countOfBoomTextures() {
      return 49;
   }

   private ResourceLocation[] getLoadedBoomTextures() {
      if (!this.boomTexturesLoaded) {
         this.boomTexturesLoaded = true;
         int count = this.countOfBoomTextures();
         this.loadedBoomTextures = new ResourceLocation[count];
         String resPrefix = "vegaline/modules/badtrip/boomsprites/boom3dma4o_";
         String format = ".jpg";

         for (int i = 0; i < count; i++) {
            ResourceLocation res = new ResourceLocation(resPrefix + (i + 1) + format);
            System.out.println(resPrefix + (i + 1) + format);
            this.loadedBoomTextures[i] = res;
            mc.getTextureManager().bindTexture(res);
         }
      }

      return this.loadedBoomTextures;
   }

   private ResourceLocation getBoomFrame(float pc01Cicle) {
      ResourceLocation[] resS = this.getLoadedBoomTextures();
      int min = 1;
      int max = resS.length;
      int indexByPC = (int)MathUtils.clamp((float)(max - min) * pc01Cicle * (1.0F + (float)min / (float)max), (float)min, (float)max);
      return resS[indexByPC % resS.length];
   }

   private void callBoomDraw(int endBoomAnimTime) {
      if (!this.drawBoomState) {
         this.endBoomAnimTime = endBoomAnimTime;
         this.timerBoom.reset();
         this.drawBoomState = true;
      }
   }

   private void doDrawBoomOverlayWhileState(ScaledResolution sr, float alphaPC) {
      if (this.boomWaiterOverlayLoadFramesPreLoad < 20 && !this.boomTexturesLoaded && this.actived && this.Ma4o3DBoom.getBool()) {
         this.boomWaiterOverlayLoadFramesPreLoad++;
         CFontRenderer font = Fonts.noise_24;
         String warningStr = "BadTrip -> Ma4o3DBoom: Loading is in process";
         float strWD2 = font.getStringWidth(warningStr) / 2.0F;
         float ext = 4.0F;
         RenderUtils.drawRect(
            (double)((float)sr.getScaledWidth() / 2.0F - strWD2 - ext),
            (double)((float)sr.getScaledHeight() / 2.0F - 50.0F - ext),
            (double)((float)sr.getScaledWidth() / 2.0F + strWD2 + ext),
            (double)((float)sr.getScaledHeight() / 2.0F - 50.0F + font.getHeight() + 3.0F + ext),
            -1
         );
         ext++;
         RenderUtils.drawLightContureRect(
            (double)((float)sr.getScaledWidth() / 2.0F - strWD2 - ext),
            (double)((float)sr.getScaledHeight() / 2.0F - 50.0F - ext),
            (double)((float)sr.getScaledWidth() / 2.0F + strWD2 + ext),
            (double)((float)sr.getScaledHeight() / 2.0F - 50.0F + font.getHeight() + 3.0F + ext),
            -1
         );
         ext++;
         RenderUtils.drawLightContureRect(
            (double)((float)sr.getScaledWidth() / 2.0F - strWD2 - ext),
            (double)((float)sr.getScaledHeight() / 2.0F - 50.0F - ext),
            (double)((float)sr.getScaledWidth() / 2.0F + strWD2 + ext),
            (double)((float)sr.getScaledHeight() / 2.0F - 50.0F + font.getHeight() + 3.0F + ext),
            -1
         );
         ext++;
         RenderUtils.drawLightContureRect(
            (double)((float)sr.getScaledWidth() / 2.0F - strWD2 - ext),
            (double)((float)sr.getScaledHeight() / 2.0F - 50.0F - ext),
            (double)((float)sr.getScaledWidth() / 2.0F + strWD2 + ext),
            (double)((float)sr.getScaledHeight() / 2.0F - 50.0F + font.getHeight() + 3.0F + ext),
            -1
         );
         font.drawStringWithOutline(
            warningStr, (float)sr.getScaledWidth() / 2.0F - strWD2, (float)sr.getScaledHeight() / 2.0F - 50.0F, ColorUtils.getColor(255, 70, 70)
         );
      } else if (this.drawBoomState && !(alphaPC > 1.0F) && !(alphaPC <= 0.0F)) {
         float pc01 = (float)this.timerBoom.getTime() / (float)this.endBoomAnimTime;
         if (pc01 > 1.0F) {
            this.drawBoomState = false;
         } else if (!(pc01 < 0.0F)) {
            int col = ColorUtils.getColor(255, (int)(255.0F * alphaPC * (0.25F + 0.75F * (float)MathUtils.easeOutCubic((double)MathUtils.valWave01(pc01)))));
            float w = (float)sr.getScaledWidth();
            float h = (float)sr.getScaledHeight();
            ResourceLocation currentFrameTex = this.getBoomFrame(pc01);
            RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            RenderUtils.buffer.pos(0.0, 0.0).tex(0.0, 0.0).color(col).endVertex();
            RenderUtils.buffer.pos((double)w, 0.0).tex(1.0, 0.0).color(col).endVertex();
            RenderUtils.buffer.pos((double)w, (double)h).tex(1.0, 1.0).color(col).endVertex();
            RenderUtils.buffer.pos(0.0, (double)h).tex(0.0, 1.0).color(col).endVertex();
            RenderUtils.glRenderStart();
            GlStateManager.resetColor();
            GL11.glEnable(3553);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 1);
            mc.getTextureManager().getTexture(currentFrameTex).setBlurMipmap(true, false);
            mc.getTextureManager().bindTexture(currentFrameTex);
            RenderUtils.tessellator.draw();
            GL11.glBlendFunc(770, 771);
            RenderUtils.glRenderStop();
         }
      }
   }

   private void onTargetMinHPBOOM() {
      this.callBoomDraw(700);
   }

   private void onMeOverFallDistBOOM() {
      this.callBoomDraw(700);
   }

   private void onPerTimeBOOM() {
      this.callBoomDraw(1100);
   }

   private void onMeOverSpeedBOOM() {
      this.callBoomDraw(1800);
   }

   private void onMeOverTimeJumpingBOOM() {
      this.callBoomDraw(1200);
   }

   private void updateBoomEvents() {
      if (HitAura.TARGET == null || !(HitAura.TARGET.getHealth() + HitAura.TARGET.getAbsorptionAmount() < 8.0F)) {
         this.hasTargetMinHP = false;
      } else if (!this.hasTargetMinHP) {
         this.hasTargetMinHP = true;
         this.onTargetMinHPBOOM();
      }

      if (Minecraft.player != null && Minecraft.player.ticksExisted > 10) {
         if (this.lastMePos.distanceTo(Minecraft.player.getPositionVector()) > 30.0) {
            if (!this.hasMeOverSpeed) {
               this.hasMeOverSpeed = true;
               this.onMeOverSpeedBOOM();
            }
         } else {
            this.hasMeOverSpeed = false;
         }

         this.lastMePos.xCoord = Minecraft.player.posX;
         this.lastMePos.yCoord = Minecraft.player.posY;
         this.lastMePos.zCoord = Minecraft.player.posZ;
      } else {
         this.hasMeOverSpeed = false;
      }

      if (Minecraft.player != null && Minecraft.player.isJumping()) {
         this.ticksMeJumping++;
         if (this.ticksMeJumping > 121 && !this.hasMeOverTimeJumping) {
            this.hasMeOverTimeJumping = true;
            this.onMeOverTimeJumpingBOOM();
         }
      } else {
         this.ticksMeJumping = 0;
         this.hasMeOverTimeJumping = false;
      }

      if (Minecraft.player == null || !(Minecraft.player.fallDistance > 20.0F)) {
         this.hasMeOverFallDist = false;
      } else if (!this.hasMeOverFallDist) {
         this.hasMeOverFallDist = true;
         this.onMeOverFallDistBOOM();
      }

      if (this.timerBoom.hasReached(20000.0)) {
         this.onPerTimeBOOM();
      }
   }

   private void updateAmbient() {
      if (this.AmbientSound.getBool()) {
         if (RadioPlayer.get.isActived() && !onDisableAmbientSoundIsEnabled) {
            onDisableAmbientSoundIsEnabled = this.AmbientSound.getBool();
            Client.msg("§f§lModules:§r §7[§l" + this.getName() + "§r§7]: AmbientSound недоступен", false);
            Client.msg("§f§lModules:§r §7[§l" + this.getName() + "§r§7]: В ClientTune включен RadioPlayer", false);
            this.AmbientSound.setBool(false);
         }
      } else if (onDisableAmbientSoundIsEnabled && !RadioPlayer.get.isActived() && !this.AmbientSound.getBool()) {
         Client.msg("§f§lModules:§r §7[§l" + this.getName() + "§r§7]: AmbientSound снова доступен и работает", false);
         this.AmbientSound.setBool(onDisableAmbientSoundIsEnabled);
         onDisableAmbientSoundIsEnabled = false;
      }

      if (this.AmbientSound.getBool()) {
         boolean isToilet = this.Sound.currentMode.equalsIgnoreCase("Toilet");
         if (this.ambientTuner.isPlaying()) {
            this.ambientTuner.setTrackName("ambient" + (isToilet ? "1" : "2"));
         } else {
            this.ambientTuner.setTrackNameForce("ambient" + (isToilet ? "1" : "2"));
         }

         this.ambientTuner.setMaxVolume(this.SoundVolume.getFloat() / 100.0F * (float)(isToilet ? 1 : 2));
      }

      this.ambientTuner.setPlaying(this.AmbientSound.getBool());
   }

   public void offAmbient() {
      this.ambientTuner.setPlaying(false);
   }

   @Override
   public void onToggled(boolean actived) {
      if (!actived) {
         this.offAmbient();
      }

      super.onToggled(actived);
   }

   private void kakish() {
      int slot = -1;
      if (Minecraft.player.openContainer instanceof ContainerPlayer container) {
         for (Slot iSlot : container.inventorySlots) {
            ItemStack stack = iSlot.getStack();
            if (stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.DIRT)) {
               slot = iSlot.slotNumber;
               break;
            }
         }
      }

      if (slot != -1) {
         Minecraft.player.rotationYaw = Minecraft.player.rotationYaw + (-1.0F + 2.0F * (float)Math.random()) * 0.01F;
         mc.getConnection()
            .preSendPacket(new CPacketPlayer.Rotation(Minecraft.player.renderYawOffset + 180.0F, 70.0F + (float)Math.random(), Minecraft.player.onGround));
         mc.playerController.windowClick(Minecraft.player.inventoryContainer.windowId, slot, 0, ClickType.THROW, Minecraft.player);
         mc.getConnection().preSendPacket(new CPacketPlayer.Rotation(Minecraft.player.rotationYaw, Minecraft.player.rotationPitch, Minecraft.player.onGround));
      }
   }

   @Override
   public void onUpdate() {
      this.updateAmbient();
      if (this.DirtKakish.getBool()) {
         if (!this.oldSneak && Minecraft.player.isSneaking()) {
            this.kakish();
         }

         this.oldSneak = Minecraft.player.isSneaking();
         if (this.oldSneak && Minecraft.player.ticksExisted % 17 == 0) {
            this.oldSneak = false;
         }
      }
   }

   public boolean isCrazySpider() {
      return this.actived && this.CrazySpider.getBool();
   }

   public boolean isSpreadCreeper() {
      return this.actived && this.SpreadCreeper.getBool();
   }

   public boolean isFlattenPlayersHurt() {
      return this.actived && this.FlattenPlayersHurt.getBool();
   }

   public boolean isSteveChiken() {
      return this.actived && this.SteveChiken.getBool();
   }

   public boolean isVillagerNose() {
      return this.actived && this.VillagerNose.getBool();
   }

   public boolean isPsichoTnt() {
      return this.actived && this.PsichoTnt.getBool();
   }

   public int getPlayerHumpLevel() {
      if (this.actived && this.HumpPlayers.canBeRender()) {
         for (int modeI = 0; modeI < this.HumpStrengh.modes.length; modeI++) {
            if (this.HumpStrengh.modes[modeI].equalsIgnoreCase(this.HumpStrengh.currentMode)) {
               return modeI + 1;
            }
         }
      }

      return 0;
   }

   public boolean isSelfBuff() {
      return this.actived && this.SelfBuff.canBeRender();
   }

   public boolean isAlkash() {
      return this.actived && this.Alkash.canBeRender();
   }
}
