package ru.govno.client.module.modules;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventRenderBlock;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class BedwarsHelper extends Module {
   public static BedwarsHelper get;
   private final BoolSettings TeamsSync;
   private final BoolSettings HighlightResource;
   private final ResourceLocation CYLINDER_TEXTURE = new ResourceLocation("vegaline/modules/bedwarshelper/cylinder.jpg");
   private final ResourceLocation GOLD_TEXTURE = new ResourceLocation("vegaline/modules/bedwarshelper/gold_highlight.png");
   private final ResourceLocation IRON_TEXTURE = new ResourceLocation("vegaline/modules/bedwarshelper/iron_highlight.png");
   private final ResourceLocation DIAMOND_TEXTURE = new ResourceLocation("vegaline/modules/bedwarshelper/diamond_highlight.png");
   private final ResourceLocation EMERALD_TEXTURE = new ResourceLocation("vegaline/modules/bedwarshelper/emerald_highlight.png");
   private boolean canUpdateChunks = false;
   private final Set<BlockPos> resourcesSpawnersList = new CopyOnWriteArraySet<>();

   public BedwarsHelper() {
      super("BedwarsHelper", 0, Module.Category.MISC);
      this.settings.add(this.TeamsSync = new BoolSettings("Teams", true, this));
      this.settings.add(this.HighlightResource = new BoolSettings("HighlightResource", true, this));
      get = this;
   }

   @Override
   public boolean isLocked() {
      return true;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   @Override
   public void onUpdate() {
      if (!this.HighlightResource.getBool()) {
         if (!this.resourcesSpawnersList.isEmpty()) {
            this.resourcesSpawnersList.clear();
            this.canUpdateChunks = true;
         }
      } else if (this.canUpdateChunks) {
         mc.renderGlobal.loadRenderers();
         this.canUpdateChunks = false;
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (!actived) {
         this.resourcesSpawnersList.clear();
         this.canUpdateChunks = true;
      }

      super.onToggled(actived);
   }

   @Override
   public void alwaysRender3D(float partialTicks) {
      if (this.isActived()) {
         if (!this.resourcesSpawnersList.isEmpty()) {
            for (BlockPos renderBlockPos : this.resourcesSpawnersList) {
               this.renderResourcePos(renderBlockPos);
            }
         }
      }
   }

   private static String getStringColorCode(String inString) {
      inString = inString.replaceAll("§r", "");
      inString = inString.replaceAll(" ", "");
       String finalInString = inString;
       return Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f")
         .stream()
         .filter(colorCode -> finalInString.startsWith("§" + colorCode))
         .findFirst()
         .orElse(null);
   }

   public static boolean isTeam(String playerName) {
      if (playerName != null && get != null && get.isActived() && mc.getConnection() != null && get.TeamsSync.getBool()) {
         NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(playerName);
         if (info != null) {
            NetworkPlayerInfo infoMe = mc.getConnection().getPlayerInfo(mc.getSession().getUsername());
            if (infoMe != null) {
               ITextComponent name = info.getDisplayName();
               if (name != null) {
                  ITextComponent nameMe = infoMe.getDisplayName();
                  if (nameMe != null) {
                     String sName = name.getFormattedText();
                     String colorCode = getStringColorCode(sName);
                     String sNameMe = nameMe.getFormattedText();
                     String colorCodeMe = getStringColorCode(sNameMe);
                     if (colorCode != null && colorCodeMe != null) {
                        System.out.println(sName + " | " + sNameMe);
                        System.out.println(colorCode + " / " + colorCodeMe);
                     }

                     return colorCode != null && colorCodeMe != null && sName.equalsIgnoreCase(sNameMe);
                  }
               }
            }
         }
      }

      return false;
   }

   @EventTarget
   public void onRenderBlock(EventRenderBlock event) {
      BlockPos posEvent = event.getPos();
      if (posEvent != null && event.getState() != null && this.HighlightResource.getBool()) {
         Block block = event.getState().getBlock();
         if (block != Blocks.IRON_BLOCK && block != Blocks.GOLD_BLOCK && block != Blocks.DIAMOND_BLOCK && block != Blocks.EMERALD_BLOCK) {
            this.resourcesSpawnersList.remove(posEvent);
         } else if (mc.world != null) {
            BlockPos up = posEvent.up();
            if (up != null && up.getY() < 256) {
               if (mc.world.isAirBlock(up)) {
                  this.resourcesSpawnersList.add(posEvent);
               } else {
                  this.resourcesSpawnersList.remove(posEvent);
               }
            }
         } else {
            this.resourcesSpawnersList.add(posEvent);
         }
      }
   }

   private void renderResourcePos(BlockPos pos) {
      IBlockState state = mc.world.getBlockState(pos);
      if (state != null) {
         Item item = null;
         Block block = state.getBlock();
         int color;
         ResourceLocation HIGHLIGHT_TEXTURE;
         if (block == Blocks.IRON_BLOCK) {
            color = ColorUtils.getColor(225, 225, 225);
            item = Items.IRON_INGOT;
            HIGHLIGHT_TEXTURE = this.IRON_TEXTURE;
         } else if (block == Blocks.GOLD_BLOCK) {
            color = ColorUtils.getColor(255, 201, 80);
            item = Items.GOLD_INGOT;
            HIGHLIGHT_TEXTURE = this.GOLD_TEXTURE;
         } else if (block == Blocks.DIAMOND_BLOCK) {
            color = ColorUtils.getColor(80, 243, 255);
            item = Items.DIAMOND;
            HIGHLIGHT_TEXTURE = this.DIAMOND_TEXTURE;
         } else if (block == Blocks.EMERALD_BLOCK) {
            color = ColorUtils.getColor(80, 255, 121);
            item = Items.EMERALD;
            HIGHLIGHT_TEXTURE = this.EMERALD_TEXTURE;
         } else {
            HIGHLIGHT_TEXTURE = this.CYLINDER_TEXTURE;
            color = 0;
         }

         if (color != 0) {
            Vec3d cenVec = new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 1.0, (double)pos.getZ() + 0.5);
            int nearableCurrentResourcesCount = 0;
            int maxDistance = 3;

            for (Entity entity : mc.world.getLoadedEntityList()) {
               if (entity instanceof EntityItem) {
                  EntityItem itemEntity = (EntityItem)entity;
                  if (entity.getDistance(cenVec.xCoord, cenVec.yCoord, cenVec.zCoord) < (double)maxDistance) {
                     ItemStack stack = itemEntity.getItem();
                     if (stack.getItem() == item) {
                        nearableCurrentResourcesCount += stack.stackSize;
                     }
                  }
               }
            }

            Tessellator tessellator = RenderUtils.tessellator;
            BufferBuilder buffer = tessellator.getBuffer();
            float radiusMin = 0.8F;
            float radiusMax = 1.0F;
            float height = 0.6F;
            float yExpand = MathUtils.lerp(0.15F, 0.2F, (float)MathUtils.easeInOutQuadWave((double)((float)System.nanoTime() / 1000000.0F % 2000.0F / 2000.0F)));
            float rotateAngle = (float)System.nanoTime() / 1000000.0F % 4000.0F / 4000.0F * 360.0F;
            int iterationsY = 12;
            int iterationsXZ = 90;
            int cylColor = ColorUtils.swapAlpha(color, (float)ColorUtils.getAlphaFromColor(color) * 0.6F);
            BedwarsHelper.Vec3d2f[] vecsAll = new BedwarsHelper.Vec3d2f[iterationsY * iterationsXZ];
            int vecIndex = 0;

            for (int itY = 0; itY < iterationsY; itY++) {
               float itYPC = (float)itY / (float)(iterationsY - 1);
               float yOffset = height * itYPC;
               float radius = radiusMin
                  + (radiusMax - radiusMin) * (float)Math.sqrt(1.0 - Math.pow((double)(1.0F - (itYPC > 0.5F ? 1.0F - itYPC : itYPC) * 2.0F), 2.0));

               for (int itXZ = 0; itXZ < iterationsXZ; itXZ++) {
                  float itXZPC = (float)itXZ / (float)(iterationsXZ - 1);
                  float yawR = MathHelper.toRadians(360.0F * itXZPC + rotateAngle);
                  float sinRadius = -MathHelper.sin(yawR) * radius;
                  float cosRadius = MathHelper.cos(yawR) * radius;
                  double x = cenVec.xCoord + (double)sinRadius;
                  double y = cenVec.yCoord + (double)yExpand + (double)yOffset;
                  double z = cenVec.zCoord + (double)cosRadius;
                  vecsAll[vecIndex] = new BedwarsHelper.Vec3d2f(x, y, z, 1.0F - itXZPC, 1.0F - itYPC);
                  vecIndex++;
               }
            }

            Runnable drawCylinder = () -> {
               if (vecsAll.length > 1) {
                  buffer.begin(8, DefaultVertexFormats.POSITION_TEX_COLOR);

                  for (int vecIndexx = 0; vecIndexx < vecsAll.length; vecIndexx++) {
                     int nextIndex = vecIndexx - iterationsXZ;
                     if (nextIndex < 0) {
                        nextIndex += iterationsXZ;
                     }

                     BedwarsHelper.Vec3d2f v0 = vecsAll[vecIndexx];
                     BedwarsHelper.Vec3d2f v1 = vecsAll[nextIndex];
                     buffer.pos(v0.x, v0.y, v0.z).tex((double)v0.u, (double)v0.v).color(cylColor).endVertex();
                     buffer.pos(v1.x, v1.y, v1.z).tex((double)v1.u, (double)v1.v).color(cylColor).endVertex();
                  }

                  tessellator.draw();
               }
            };
            if (nearableCurrentResourcesCount > 0) {
               String count = nearableCurrentResourcesCount == 0 ? "-" : "x" + nearableCurrentResourcesCount;
               CFontRenderer font = Fonts.noise_24;
               float scale = 0.5F / font.getHeight();
               RenderUtils.setup3dForBlockPos(() -> {
                  GL11.glEnable(2929);
                  GL11.glEnable(3553);
                  GL11.glDisable(2884);
                  GL11.glDepthMask(false);
                  GL11.glTranslated(cenVec.xCoord, cenVec.yCoord, cenVec.zCoord);
                  float[] rotateToCamera = RotationUtil.getVecNeeded(Minecraft.player.getPositionEyes(mc.getRenderPartialTicks()), cenVec);
                  GL11.glNormal3d(1.0, 1.0, 1.0);
                  GL11.glRotated((double)rotateToCamera[0], 0.0, -1.0, 0.0);
                  GL11.glRotated((double)rotateToCamera[1], 1.0, 0.0, 0.0);
                  GL11.glTranslatef(0.0F, 0.4F, 0.0F);
                  GL11.glScalef(scale, -scale, scale);
                  float[] glowRanges = new float[]{0.5F, 1.0F, 2.0F};
                  int strGlowCol = ColorUtils.toDark(color, 0.07F);

                  for (float glowRange : glowRanges) {
                     for (int yaw = 0; yaw < 360; yaw = (int)((float)yaw + 45.0F)) {
                        GL11.glPushMatrix();
                        float radYaw = MathHelper.toRadians((float)yaw);
                        GL11.glTranslated((double)(-MathHelper.sin(radYaw) * glowRange), (double)(MathHelper.cos(radYaw) * glowRange), 0.0);
                        GL11.glTexParameteri(3553, 10240, 9729);
                        GL11.glTexParameteri(3553, 10240, 9729);
                        font.drawString(count, -font.getStringWidth(count) / 2.0F, -font.getHeight() / 2.0F, strGlowCol);
                        GL11.glPopMatrix();
                     }
                  }

                  GL11.glBlendFunc(770, 771);
                  GL11.glTexParameteri(3553, 10240, 9729);
                  GL11.glTexParameteri(3553, 10240, 9729);
                  font.drawStringWithOutline(count, -font.getStringWidth(count) / 2.0F, -font.getHeight() / 2.0F, color);
               }, true);
            }

            RenderUtils.setup3dForBlockPos(() -> {
               GL11.glEnable(2929);
               GL11.glEnable(3553);
               GL11.glDisable(2884);
               GL11.glDepthMask(false);
               if (mc.getTextureManager().getTexture(HIGHLIGHT_TEXTURE) != null) {
                  mc.getTextureManager().getTexture(HIGHLIGHT_TEXTURE).setBlurMipmap(true, false);
               }

               mc.getTextureManager().bindTexture(HIGHLIGHT_TEXTURE);
               drawCylinder.run();
            }, true);
         }
      }
   }

   private class Vec3d2f {
      private final double x;
      private final double y;
      private final double z;
      private final float u;
      private final float v;

      public Vec3d2f(double x, double y, double z, float u, float v) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.u = u;
         this.v = v;
      }
   }
}
