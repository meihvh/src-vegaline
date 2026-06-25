package ru.govno.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.model.ModelEnderCrystal;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class WallHack extends Module {
   public static WallHack get;
   public BoolSettings Players;
   public BoolSettings Friends;
   public BoolSettings Crystals;
   public BoolSettings Mobs;
   public BoolSettings Tiles;
   public BoolSettings RenderOnlyNotSeen;
   public BoolSettings CrystalExtraBloom;
   public ModeSettings PlayerRender;
   public ModeSettings PlayerColorMode;
   public ModeSettings CrysColorMode;
   public FloatSettings PlayerOpacity;
   public FloatSettings CrysOpacity;
   public FloatSettings OutlineWidth;
   public BoolSettings StippleOutline;
   public BoolSettings ApplySkinTexture;
   public ColorSettings PlayerPickColor;
   public ColorSettings FriendPickColor;
   public ColorSettings CrysPickColor;

   public WallHack() {
      super("WallHack", 0, Module.Category.RENDER);
      get = this;
      this.settings.add(this.Players = new BoolSettings("Players", true, this));
      this.settings.add(this.Friends = new BoolSettings("Friends", true, this));
      this.settings
         .add(
            this.PlayerRender = new ModeSettings(
               "PlayerRender",
               "WallHack",
               this,
               new String[]{"WallHack", "Fill", "Out", "Fill&Out", "Fill&Tex", "Out&Tex", "Fill&Out&Tex", "Out&Tex&ColTex", "Fill&Out&Tex&ColTex"},
               () -> this.Players.getBool() || this.Friends.getBool()
            )
         );
      this.settings
         .add(
            this.OutlineWidth = new FloatSettings(
               "OutlineWidth",
               0.5F,
               3.0F,
               0.25F,
               this,
               () -> (this.Players.getBool() || this.Friends.getBool()) && this.PlayerRender.currentMode.contains("Out")
            )
         );
      this.settings
         .add(
            this.StippleOutline = new BoolSettings(
               "StippleOutline", false, this, () -> (this.Players.getBool() || this.Friends.getBool()) && this.PlayerRender.currentMode.contains("Out")
            )
         );
      this.settings
         .add(
            this.ApplySkinTexture = new BoolSettings(
               "ApplySkinTexture", false, this, () -> (this.Players.getBool() || this.Friends.getBool()) && this.PlayerRender.currentMode.contains("Tex")
            )
         );
      this.settings
         .add(
            this.PlayerColorMode = new ModeSettings(
               "PlayerColorMode",
               "Picker",
               this,
               new String[]{"Client", "Picker"},
               () -> (this.Players.getBool() || this.Friends.getBool()) && !this.PlayerRender.getMode().equalsIgnoreCase("WallHack")
            )
         );
      this.settings
         .add(
            this.PlayerOpacity = new FloatSettings(
               "PlayerOpacity",
               0.7F,
               1.0F,
               0.05F,
               this,
               () -> (this.Players.getBool() || this.Friends.getBool())
                     && !this.PlayerRender.getMode().equalsIgnoreCase("WallHack")
                     && this.PlayerColorMode.currentMode.equalsIgnoreCase("Client")
            )
         );
      this.settings
         .add(
            this.PlayerPickColor = new ColorSettings(
               "PlayerPickColor",
               ColorUtils.getColor(255, 60, 170, 95),
               this,
               () -> this.Players.getBool()
                     && !this.PlayerRender.getMode().equalsIgnoreCase("WallHack")
                     && this.PlayerColorMode.currentMode.equalsIgnoreCase("Picker")
            )
         );
      this.settings
         .add(
            this.FriendPickColor = new ColorSettings(
               "FriendPickColor",
               ColorUtils.getColor(110, 255, 10, 95),
               this,
               () -> this.Friends.getBool()
                     && !this.PlayerRender.getMode().equalsIgnoreCase("WallHack")
                     && this.PlayerColorMode.currentMode.equalsIgnoreCase("Picker")
            )
         );
      this.settings.add(this.Mobs = new BoolSettings("Mobs", true, this));
      this.settings.add(this.Crystals = new BoolSettings("Crystals", true, this));
      this.settings.add(this.CrystalExtraBloom = new BoolSettings("CrystalExtraBloom", false, this, () -> this.Crystals.getBool()));
      this.settings
         .add(this.CrysColorMode = new ModeSettings("CrysColorMode", "Client", this, new String[]{"Client", "Picker"}, () -> this.Crystals.getBool()));
      this.settings
         .add(
            this.CrysOpacity = new FloatSettings(
               "CrysOpacity", 0.7F, 1.0F, 0.05F, this, () -> this.Crystals.getBool() && this.CrysColorMode.currentMode.equalsIgnoreCase("Client")
            )
         );
      this.settings
         .add(
            this.CrysPickColor = new ColorSettings(
               "CrysPickColor",
               ColorUtils.getColor(110, 160, 255, 225),
               this,
               () -> this.Crystals.getBool() && this.CrysColorMode.currentMode.equalsIgnoreCase("Picker")
            )
         );
      this.settings.add(this.Tiles = new BoolSettings("Tiles", true, this));
      this.settings.add(this.RenderOnlyNotSeen = new BoolSettings("RenderOnlyNotSeen", true, this));
      this.setDemand(0, 0);
   }

   @Override
   public void onUpdate() {
      if (!this.Players.getBool() && !this.Friends.getBool() && !this.Crystals.getBool() && !this.Mobs.getBool()) {
         this.toggle(false);
         Client.msg("§f§lModules:§r §7[§l" + this.getName() + "§r§7]: §7включите что-нибудь в настройках.", false);
      }
   }

   public boolean[] getEtypes() {
      return new boolean[]{this.Players.getBool(), this.Friends.getBool(), this.Crystals.getBool(), this.Mobs.getBool(), this.Tiles.getBool()};
   }

   public boolean isCurrent(boolean[] entTypes, Entity entity) {
      if (entity == null) {
         return false;
      } else {
         if (entity instanceof EntityLivingBase base
            && (
               base instanceof EntityOtherPlayerMP mp && entTypes[Client.friendManager.isFriend(mp.getName()) ? 1 : 0]
                  || base instanceof EntityLivingBase && !(base instanceof EntityPlayer) && entTypes[3]
            )) {
            return base.isEntityAlive() && (!this.RenderOnlyNotSeen.getBool() || !Minecraft.player.canEntityBeSeen(entity));
         }

         if (entity instanceof EntityEnderCrystal crystal && !crystal.isDead) {
            return entTypes[2];
         }

         return !(entity instanceof EntityMinecartContainer) && !(entity instanceof IProjectile) && !(entity instanceof EntityArmorStand)
            ? false
            : entTypes[4] && (!this.RenderOnlyNotSeen.getBool() || !Minecraft.player.canEntityBeSeen(entity));
      }
   }

   public boolean isCurrent(boolean[] entTypes, TileEntity tileEntity) {
      return entTypes[4];
   }

   private int getChamsColor(Entity entityIn) {
      int color = 0;
      if (entityIn instanceof EntityEnderCrystal) {
         String player = this.CrysColorMode.currentMode;
         switch (player) {
            case "Client":
               color = ClientColors.getColor1(Math.abs(entityIn.getEntityId()), this.CrysOpacity.getFloat());
               break;
            case "Picker":
               color = this.CrysPickColor.color;
         }
      }

      if (entityIn instanceof EntityPlayer player) {
         String var7 = this.PlayerColorMode.currentMode;
         switch (var7) {
            case "Client":
               color = ClientColors.getColor1(Math.abs(entityIn.getEntityId()), this.PlayerOpacity.getFloat());
               break;
            case "Picker":
               color = (Client.friendManager.isFriend(player.getName()) ? this.FriendPickColor : this.PlayerPickColor).getCol();
         }

         if (player.hurtTime > 0) {
            float hurtResist = (float)MathUtils.easeInOutQuadWave((double)(((float)(player.hurtTime + 1) - mc.getRenderPartialTicks()) / 10.0F));
            color = ColorUtils.toDark(color, 1.0F - hurtResist * hurtResist * 0.7F);
         }

         color = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * 0.3F);
      }

      return color;
   }

   private boolean[] chamsTypesEnabled(Entity entityIn) {
      return !(entityIn instanceof EntityPlayer)
         ? new boolean[6]
         : new boolean[]{
            this.PlayerRender.getMode().contains("Fill"),
            this.PlayerRender.getMode().contains("Out"),
            this.PlayerRender.getMode().contains("&Tex"),
            this.PlayerRender.getMode().contains("Col"),
            this.StippleOutline.getBool(),
            this.ApplySkinTexture.getBool() && this.PlayerRender.getMode().contains("&Tex")
         };
   }

   private void crystalPreChams(Runnable renderModel) {
      float hds = ((float)Minecraft.player.ticksExisted + mc.getRenderPartialTicks()) % 20.0F / 20.0F;
      hds = (float)MathUtils.easeInOutQuadWave((double)hds);
      float startScale = 1.025F;
      float endScale = 1.05F + (this.CrystalExtraBloom.getBool() ? 0.4F : 0.6F) * hds;
      float alphaStart = this.CrystalExtraBloom.getBool() ? 0.2F : 0.35F;
      float alphaEnd = 0.003921569F;
      int iterations = !this.CrystalExtraBloom.getBool() ? 1 + (int)(4.0F * hds) : 2 + (int)(9.0F * hds);
      GL11.glEnable(3042);
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA,
         GlStateManager.DestFactor.ONE,
         GlStateManager.SourceFactor.ONE_MINUS_CONSTANT_ALPHA,
         GlStateManager.DestFactor.ZERO
      );
      GL11.glDepthMask(false);
      GL11.glEnable(2884);
      mc.entityRenderer.disableLightmap();
      GlStateManager.disableLighting();
      GL11.glAlphaFunc(516, 0.003921569F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      renderModel.run();
      ModelEnderCrystal.cancelBase = true;

      for (int index = 0; index < iterations; index++) {
         float scale = MathUtils.lerp(startScale, endScale, (float)index / (float)iterations);
         float alphaPC = MathUtils.lerp(alphaStart, alphaEnd, (float)index / (float)iterations);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, alphaPC);
         float descale = 1.0F / scale;
         float append = 0.5F;
         GL11.glTranslated(0.0, (double)append, 0.0);
         GL11.glScaled((double)scale, (double)scale, (double)scale);
         GL11.glTranslated(0.0, (double)(-append), 0.0);
         renderModel.run();
         GL11.glTranslated(0.0, (double)append, 0.0);
         GL11.glScaled((double)descale, (double)descale, (double)descale);
         GL11.glTranslated(0.0, (double)(-append), 0.0);
      }

      ModelEnderCrystal.cancelBase = false;
      GL11.glAlphaFunc(516, 0.1F);
      GlStateManager.enableLighting();
      mc.entityRenderer.enableLightmap();
      GL11.glEnable(3553);
      GL11.glCullFace(1029);
      GL11.glDepthMask(true);
      GL11.glDisable(2884);
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      GlStateManager.resetColor();
   }

   private void renderChams(
      Entity baseIn,
      boolean crystalEntityRender,
      boolean fillChams,
      boolean lineChams,
      boolean textureForChams,
      boolean textureForChamsColorLine,
      boolean stippleLineFotChams,
      boolean applyTextureToTexForChams,
      float chamsLineWidth,
      boolean pre,
      int col,
      Runnable renderModel,
      boolean isRenderItems
   ) {
      Runnable renderModelFixed = () -> {
         GL11.glAlphaFunc(516, 0.1F);
         RenderLivingBase.unsetRenderArmorLayerAdditions = true;
         renderModel.run();
         RenderLivingBase.unsetRenderArmorLayerAdditions = false;
         GL11.glAlphaFunc(516, 0.003921569F);
      };
      boolean chamsMode = baseIn != null && (fillChams || lineChams) && col != 0 && chamsLineWidth > 0.0F && chamsLineWidth <= 11.25F;
      if (!crystalEntityRender && chamsMode) {
         if (isRenderItems) {
            if (pre) {
               GL11.glEnable(2929);
               GL11.glDepthRange(0.0, 0.01);
               GL11.glEnable(3553);
               GL11.glDisable(3042);
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
            } else {
               GL11.glDepthRange(0.0, 1.0);
            }
         } else if (pre) {
            float dHWFactor = Math.min((float)(mc.displayWidth * mc.displayHeight) / 9830400.0F * 1.2F, 1.0F);
            RenderLivingBase.unsetRenderCape = true;
            RenderLivingBase.unsetDistantAlphaLiving = true;
            Runnable glStart = () -> {
               GL11.glDisable(2929);
               mc.entityRenderer.disableLightmap();
               if (textureForChams && applyTextureToTexForChams) {
                  GL11.glEnable(3553);
               } else {
                  GL11.glDisable(3553);
               }

               GlStateManager.disableBlend();
               GlStateManager.enableBlend();
               GL11.glBlendFunc(770, 1);
               GL11.glAlphaFunc(516, 0.003921569F);
            };
            Runnable glStop = () -> {
               GL11.glEnable(2929);
               GL11.glEnable(3553);
               mc.entityRenderer.enableLightmap();
               GL11.glBlendFunc(770, 771);
               GL11.glColor4b((byte)1, (byte)1, (byte)1, (byte)1);
               GL11.glAlphaFunc(516, 0.1F);
               GlStateManager.disableBlendProfile(GlStateManager.Profile.TRANSPARENT_MODEL);
               GlStateManager.enableBlendProfile(GlStateManager.Profile.PLAYER_SKIN);
            };
            Runnable startPolygonFill = () -> {
               GL11.glEnable(32823);
               GlStateManager.glPolygonMode(1032, 6914);
            };
            Runnable startPolygonLine = () -> {
               GL11.glEnable(32823);
               GlStateManager.glPolygonMode(1032, 6913);
            };
            Runnable stopPolygons = () -> {
               GlStateManager.glPolygonMode(1032, 6914);
               GL11.glDisable(32823);
               GL11.glDisable(10754);
            };
            Runnable enableLineSmooth = () -> {
               GL11.glEnable(2848);
               GL11.glHint(3154, 4354);
            };
            Runnable disableLineSmooth = () -> {
               GL11.glDisable(2848);
               GL11.glHint(3154, 4352);
            };
            if (lineChams) {
               if (stippleLineFotChams) {
                  GL11.glEnable(2852);
                  GL11.glLineStipple(20, (short)-21846);
               }

               glStart.run();
               startPolygonLine.run();
               enableLineSmooth.run();
               RenderUtils.glColor(col);
               GL11.glLineWidth(chamsLineWidth * dHWFactor * 2.0F);
               renderModelFixed.run();
               if (textureForChamsColorLine) {
                  renderModelFixed.run();
               }

               GL11.glLineWidth(1.0F);
               disableLineSmooth.run();
               stopPolygons.run();
               glStop.run();
               if (stippleLineFotChams) {
                  GL11.glEnable(2852);
                  GL11.glLineStipple(10, (short)-21846);
               }

               float glowAPC = 0.2F;
               glStart.run();
               startPolygonLine.run();
               enableLineSmooth.run();
               RenderUtils.glColor(ColorUtils.toDark(col, glowAPC));
               GL11.glLineWidth(chamsLineWidth * 3.0F);
               renderModelFixed.run();
               GL11.glLineWidth(1.0F);
               disableLineSmooth.run();
               stopPolygons.run();
               glStop.run();
               if (stippleLineFotChams) {
                  GL11.glDisable(2852);
               }
            }

            if (fillChams) {
               glStart.run();
               startPolygonFill.run();
               RenderUtils.glColor(ColorUtils.toDark(col, 0.55F));
               renderModelFixed.run();
               stopPolygons.run();
               glStop.run();
            }

            if (textureForChams) {
               GL11.glBlendFunc(770, 771);
               renderModelFixed.run();
            }
         } else {
            RenderLivingBase.unsetRenderCape = false;
            RenderLivingBase.unsetDistantAlphaLiving = false;
         }
      } else {
         if (pre) {
            if (!isRenderItems && crystalEntityRender) {
               GlStateManager.enableBlend();
               GL11.glDisable(3553);
               GL11.glDisable(3008);
               GL11.glEnable(2884);
               GL11.glDisable(2896);
               RenderUtils.glColor(ColorUtils.swapAlpha(col, (float)ColorUtils.getAlphaFromColor(col) / 2.0F));
               GL11.glDepthMask(false);
               mc.entityRenderer.disableLightmap();
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
               );
            }

            if (isRenderItems) {
               GL11.glEnable(3553);
            } else {
               GL11.glDepthRange(0.0, 0.01);
            }

            renderModel.run();
            GL11.glDepthRange(0.0, 1.0);
            if (!isRenderItems && crystalEntityRender) {
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               mc.entityRenderer.enableLightmap();
               GL11.glEnable(2896);
               GL11.glDepthMask(true);
               GL11.glEnable(3553);
               GL11.glEnable(3008);
               GlStateManager.resetColor();
               GL11.glColor4b((byte)1, (byte)1, (byte)1, (byte)1);
            }
         }
      }
   }

   @Override
   public void preRenderLivingBase(Entity baseIn, Runnable renderModel, boolean isRenderItems) {
      if (this.isCurrent(this.getEtypes(), baseIn)) {
         mc.renderManager.renderShadow = false;
         boolean crystal = baseIn instanceof EntityEnderCrystal;
         boolean[] chamsTypes = this.chamsTypesEnabled(baseIn);
         if (crystal && !this.CrystalExtraBloom.getBool()) {
            this.renderChams(
               baseIn,
               crystal,
               chamsTypes[0],
               chamsTypes[1],
               chamsTypes[2],
               chamsTypes[3],
               chamsTypes[4],
               chamsTypes[5],
               this.OutlineWidth.getFloat(),
               true,
               this.getChamsColor(baseIn),
               renderModel,
               isRenderItems
            );
         }

         if (!isRenderItems && crystal && ModelEnderCrystal.canDeformate) {
            this.crystalPreChams(renderModel);
         }

         if (!crystal || this.CrystalExtraBloom.getBool()) {
            this.renderChams(
               baseIn,
               crystal,
               chamsTypes[0],
               chamsTypes[1],
               chamsTypes[2],
               chamsTypes[3],
               chamsTypes[4],
               chamsTypes[5],
               this.OutlineWidth.getFloat(),
               true,
               this.getChamsColor(baseIn),
               renderModel,
               isRenderItems
            );
         }
      }
   }

   @Override
   public void postRenderLivingBase(Entity baseIn, Runnable renderModel, boolean isRenderItems) {
      if (this.isCurrent(this.getEtypes(), baseIn)) {
         mc.renderManager.renderShadow = mc.gameSettings.entityShadows;
         boolean crystal = baseIn instanceof EntityEnderCrystal;
         boolean[] chamsTypes = this.chamsTypesEnabled(baseIn);
         if (!crystal || this.CrystalExtraBloom.getBool()) {
            this.renderChams(
               baseIn,
               crystal,
               chamsTypes[0],
               chamsTypes[1],
               chamsTypes[2],
               chamsTypes[3],
               chamsTypes[4],
               chamsTypes[5],
               this.OutlineWidth.getFloat(),
               false,
               this.getChamsColor(baseIn),
               renderModel,
               isRenderItems
            );
         }
      }
   }

   public void preRenderTileEntity(TileEntity tileIn, Runnable renderModel) {
      if (this.isCurrent(this.getEtypes(), tileIn)) {
         mc.renderManager.renderShadow = false;
         this.renderChams(null, false, false, false, false, false, false, false, 0.0F, true, 0, renderModel, false);
      }
   }

   public void postRenderTileEntity(TileEntity tileIn, Runnable renderModel) {
      if (this.isCurrent(this.getEtypes(), tileIn)) {
         mc.renderManager.renderShadow = mc.gameSettings.entityShadows;
         this.renderChams(null, false, false, false, false, false, false, false, 0.0F, true, 0, renderModel, false);
      }
   }
}
