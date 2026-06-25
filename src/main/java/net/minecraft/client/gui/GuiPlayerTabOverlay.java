package net.minecraft.client.gui;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.friendsystem.Friend;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.ReplaceStrUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class GuiPlayerTabOverlay extends Gui {
   public static final Ordering<NetworkPlayerInfo> ENTRY_ORDERING = Ordering.from(new GuiPlayerTabOverlay.PlayerComparator());
   private final Minecraft mc;
   private final GuiIngame guiIngame;
   private ITextComponent footer;
   private ITextComponent header;
   private long lastTimeOpened;
   private boolean isBeingRendered;
   public static String staffname;

   public GuiPlayerTabOverlay(Minecraft mcIn, GuiIngame guiIngameIn) {
      this.mc = mcIn;
      this.guiIngame = guiIngameIn;
   }

   public static List<EntityPlayer> getPlayers() {
      List<NetworkPlayerInfo> list = ENTRY_ORDERING.sortedCopy(Minecraft.player.connection.getPlayerInfoMap());
      List<EntityPlayer> players = new ArrayList<>();

      for (NetworkPlayerInfo player : list) {
         if (player != null) {
            players.add(Minecraft.getMinecraft().world.getPlayerEntityByName(player.getGameProfile().getName()));
         }
      }

      return players;
   }

   public static List<EntityPlayer> getPlayers2() {
      List<NetworkPlayerInfo> list = ENTRY_ORDERING.sortedCopy(Minecraft.player.connection.getPlayerInfoMap());
      ArrayList<EntityPlayer> players = new ArrayList<>();

      for (NetworkPlayerInfo player : list) {
         if (player != null) {
            players.add(Minecraft.getMinecraft().world.getPlayerEntityByName(player.getGameProfile().getName()));
         }
      }

      return players;
   }

   public String getPlayerName(NetworkPlayerInfo networkPlayerInfoIn) {
      return ScorePlayerTeam.formatPlayerName(networkPlayerInfoIn.getPlayerTeam(), networkPlayerInfoIn.getGameProfile().getName());
   }

   public void updatePlayerList(boolean willBeRendered) {
      if (willBeRendered && !this.isBeingRendered) {
         this.lastTimeOpened = Minecraft.getSystemTime();
      }

      this.isBeingRendered = willBeRendered;
   }

   public void renderPlayerlist(int width, Scoreboard scoreboardIn, @Nullable ScoreObjective scoreObjectiveIn) {
      if (Panic.stop) {
         NetHandlerPlayClient nethandlerplayclient = Minecraft.player.connection;
         List<NetworkPlayerInfo> list = ENTRY_ORDERING.sortedCopy(nethandlerplayclient.getPlayerInfoMap());
         int i = 0;
         int j = 0;

         for (NetworkPlayerInfo networkplayerinfo : list) {
            int k = this.mc.fontRendererObj.getStringWidth(this.getPlayerName(networkplayerinfo));
            i = Math.max(i, k);
            if (scoreObjectiveIn != null && scoreObjectiveIn.getRenderType() != IScoreCriteria.EnumRenderType.HEARTS) {
               k = this.mc
                  .fontRendererObj
                  .getStringWidth(" " + scoreboardIn.getOrCreateScore(networkplayerinfo.getGameProfile().getName(), scoreObjectiveIn).getScorePoints());
               j = Math.max(j, k);
            }
         }

         list = list.subList(0, Math.min(list.size(), 80));
         int l3 = list.size();
         int i4 = l3;

         int j4;
         for (j4 = 1; i4 > 20; i4 = (l3 + j4 - 1) / j4) {
            j4++;
         }

         boolean flag = this.mc.isIntegratedServerRunning() || this.mc.getConnection().getNetworkManager().isEncrypted();
         int l;
         if (scoreObjectiveIn != null) {
            if (scoreObjectiveIn.getRenderType() == IScoreCriteria.EnumRenderType.HEARTS) {
               l = 90;
            } else {
               l = j;
            }
         } else {
            l = 0;
         }

         int i1 = Math.min(j4 * ((flag ? 9 : 0) + i + l + 13), width - 50) / j4;
         int j1 = width / 2 - (i1 * j4 + (j4 - 1) * 5) / 2;
         int k1 = 10;
         int l1 = i1 * j4 + (j4 - 1) * 5;
         List<String> list1 = null;
         if (this.header != null) {
            list1 = this.mc.fontRendererObj.listFormattedStringToWidth(this.header.getFormattedText(), width - 50);

            for (String s : list1) {
               l1 = Math.max(l1, this.mc.fontRendererObj.getStringWidth(s));
            }
         }

         List<String> list2 = null;
         if (this.footer != null) {
            list2 = this.mc.fontRendererObj.listFormattedStringToWidth(this.footer.getFormattedText(), width - 50);

            for (String s1 : list2) {
               l1 = Math.max(l1, this.mc.fontRendererObj.getStringWidth(s1));
            }
         }

         if (list1 != null) {
            drawRect(
               width / 2 - l1 / 2 - 1,
               (double)(k1 - 1),
               (double)(width / 2 + l1 / 2 + 1),
               (double)(k1 + list1.size() * this.mc.fontRendererObj.FONT_HEIGHT),
               Integer.MIN_VALUE
            );

            for (String s2 : list1) {
               int i2 = this.mc.fontRendererObj.getStringWidth(s2);
               this.mc.fontRendererObj.drawStringWithShadow(s2, (float)(width / 2 - i2 / 2), (float)k1, -1);
               k1 += this.mc.fontRendererObj.FONT_HEIGHT;
            }

            k1++;
         }

         drawRect(width / 2 - l1 / 2 - 1, (double)(k1 - 1), (double)(width / 2 + l1 / 2 + 1), (double)(k1 + i4 * 9), Integer.MIN_VALUE);

         for (int k4 = 0; k4 < l3; k4++) {
            int l4 = k4 / i4;
            int i5 = k4 % i4;
            int j2 = j1 + l4 * i1 + l4 * 5;
            int k2 = k1 + i5 * 9;
            drawRect(j2, (double)k2, (double)(j2 + i1), (double)(k2 + 8), 553648127);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            if (k4 < list.size()) {
               NetworkPlayerInfo networkplayerinfo1 = list.get(k4);
               GameProfile gameprofile = networkplayerinfo1.getGameProfile();
               if (flag) {
                  EntityPlayer entityplayer = this.mc.world.getPlayerEntityByUUID(gameprofile.getId());
                  boolean flag1 = entityplayer != null
                     && entityplayer.isWearing(EnumPlayerModelParts.CAPE)
                     && ("Dinnerbone".equals(gameprofile.getName()) || "Grumm".equals(gameprofile.getName()));
                  this.mc.getTextureManager().bindTexture(networkplayerinfo1.getLocationSkin());
                  int l2 = 8 + (flag1 ? 8 : 0);
                  int i3 = 8 * (flag1 ? -1 : 1);
                  Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 8.0F, (float)l2, 8.0F, (float)i3, 8.0F, 8.0F, 64.0F, 64.0F);
                  if (entityplayer != null && entityplayer.isWearing(EnumPlayerModelParts.HAT)) {
                     int j3 = 8 + (flag1 ? 8 : 0);
                     int k3 = 8 * (flag1 ? -1 : 1);
                     Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 40.0F, (float)j3, 8.0F, (float)k3, 8.0F, 8.0F, 64.0F, 64.0F);
                  }

                  j2 += 9;
               }

               String s4 = this.getPlayerName(networkplayerinfo1);
               if (networkplayerinfo1.getGameType() == GameType.SPECTATOR) {
                  this.mc.fontRendererObj.drawStringWithShadow(TextFormatting.ITALIC + s4, (float)j2, (float)k2, -1862270977);
               } else {
                  this.mc.fontRendererObj.drawStringWithShadow(s4, (float)j2, (float)k2, -1);
               }

               if (scoreObjectiveIn != null && networkplayerinfo1.getGameType() != GameType.SPECTATOR) {
                  int k5 = j2 + i + 1;
                  int l5 = k5 + l;
                  if (l5 - k5 > 5) {
                     this.drawScoreboardValues(scoreObjectiveIn, k2, gameprofile.getName(), k5, l5, networkplayerinfo1);
                  }
               }

               this.drawPing2(i1, j2 - (flag ? 9 : 0), k2, networkplayerinfo1, 255);
            }
         }

         if (list2 != null) {
            k1 = k1 + i4 * 9 + 1;
            drawRect(
               width / 2 - l1 / 2 - 1,
               (double)(k1 - 1),
               (double)(width / 2 + l1 / 2 + 1),
               (double)(k1 + list2.size() * this.mc.fontRendererObj.FONT_HEIGHT),
               Integer.MIN_VALUE
            );

            for (String s3 : list2) {
               int j5 = this.mc.fontRendererObj.getStringWidth(s3);
               this.mc.fontRendererObj.drawStringWithShadow(s3, (float)(width / 2 - j5 / 2), (float)k1, -1);
               k1 += this.mc.fontRendererObj.FONT_HEIGHT;
            }
         }
      } else {
         NetHandlerPlayClient nethandlerplayclient = Minecraft.player.connection;
         List<NetworkPlayerInfo> list = ENTRY_ORDERING.sortedCopy(nethandlerplayclient.getPlayerInfoMap());
         int i = 0;
         int j = 0;

         for (NetworkPlayerInfo networkplayerinfox : list) {
            int k = this.mc.fontRendererObj.getStringWidth("                        " + this.getPlayerName(networkplayerinfox));
            i = Math.max(i, k);
            if (scoreObjectiveIn != null && scoreObjectiveIn.getRenderType() != IScoreCriteria.EnumRenderType.HEARTS) {
               k = (int)Fonts.mntsb_15
                  .getStringWidth(
                     "                        "
                        + scoreboardIn.getOrCreateScore(networkplayerinfox.getGameProfile().getName(), scoreObjectiveIn).getScorePoints()
                  );
               j = Math.max(j, k);
            }
         }

         list = list.subList(0, Math.min(list.size(), 180));
         int l3 = list.size();
         int i4 = l3;

         int j4x;
         for (j4x = 1; i4 > 40; i4 = (l3 + j4x - 1) / j4x) {
            j4x++;
         }

         boolean flagx = this.mc.isIntegratedServerRunning() || this.mc.getConnection().getNetworkManager().isEncrypted();
         int lx;
         if (scoreObjectiveIn != null) {
            if (scoreObjectiveIn.getRenderType() == IScoreCriteria.EnumRenderType.HEARTS) {
               lx = 90;
            } else {
               lx = j;
            }
         } else {
            lx = 0;
         }

         int i1x = Math.min(j4x * ((flagx ? 9 : 0) + i + lx + 13), width - 50) / j4x;
         int j1x = width / 2 - (i1x * j4x + (j4x - 1) * 5) / 2;
         int k1x = 10;
         int l1x = i1x * j4x + (j4x - 1) * 5;
         List<String> list1x = null;
         if (this.header != null) {
            for (String s : this.mc.fontRendererObj.listFormattedStringToWidth(this.header.getFormattedText(), width - 50)) {
               l1x = Math.max(l1x, this.mc.fontRendererObj.getStringWidth(s));
            }
         }

         List<String> list2x = null;
         if (this.footer != null) {
            list2x = Fonts.neverlose500_17.listFormattedStringToWidth(this.footer.getFormattedText(), width - 50);

            for (String s1 : list2x) {
               l1x = (int)Math.max((float)l1x, Fonts.neverlose500_17.getStringWidth(s1));
            }
         }

         float x1 = (float)(width / 2 - l1x / 2 - 1 + 15);
         float y1 = (float)(k1x + 20);
         float p1 = (float)(k1x + i4 * 9 + 1);
         float x2 = (float)(width / 2 + l1x / 2) + 9.5F - 15.0F;
         float y2 = 0.0F;
         if (list2x != null) {
            y2 = p1 + (float)(list2x.size() * this.mc.fontRendererObj.FONT_HEIGHT) + (float)(k1x + 2 - k1x - 2);
         }

         int alpher = (int)(GuiIngame.tabAlpha * 1.25F);
         int color6 = ColorUtils.swapAlpha(ColorUtils.getColor(255, 255, 255), MathUtils.clamp((float)alpher * 1.25F, 26.0F, 255.0F));
         if (GuiIngame.tabAlpha > 26.0F) {
            if (getPlayers().size() != 1) {
               Fonts.mntsb_20.drawStringWithShadow("Online: " + getPlayers().size(), x1 - 10.0F, y1 - 8.0F, color6);
            }

            if (!this.mc.isSingleplayer()) {
               Fonts.mntsb_20
                  .drawStringWithShadow(
                     "Ip: " + this.mc.getCurrentServerData().serverIP,
                     x2 - Fonts.mntsb_20.getStringWidth("Ip: " + this.mc.getCurrentServerData().serverIP) + 6.0F,
                     y1 - 8.0F,
                     color6
                  );
            }
         }

         k1x += 25;

         for (int k4x = 0; k4x < l3; k4x++) {
            int l4 = k4x / i4;
            int i5 = k4x % i4;
            int j2 = j1x + l4 * i1x + l4 * 5 + 4;
            int k2 = k1x + i5 * 9;
            NetworkPlayerInfo networkplayerinfo1x = list.get(k4x);
            GameProfile gameprofilex = networkplayerinfo1x.getGameProfile();
            EntityPlayer entityplayer = this.mc.world.getPlayerEntityByUUID(gameprofilex.getId());
            if (GuiIngame.tabAlpha > 16.0F && entityplayer == Minecraft.player) {
               RenderUtils.drawAlphedRect(
                  (double)j2, (double)k2, (double)(j2 + i1x - 4), (double)(k2 + 8), ColorUtils.getColor(160, 0, 255, (int)GuiIngame.tabAlpha)
               );
            }

            for (Friend friend : Client.friendManager.getFriends()) {
               if (friend != null
                  && networkplayerinfo1x != null
                  && networkplayerinfo1x.getDisplayName() != null
                  && !networkplayerinfo1x.getDisplayName().getUnformattedText().isEmpty()
                  && GuiIngame.tabAlpha > 16.0F
                  && friend.getName() != Minecraft.player.getName()
                  && networkplayerinfo1x != null
                  && networkplayerinfo1x.getDisplayName().getUnformattedText().contains(friend.getName())) {
                  RenderUtils.drawAlphedRect(
                     (double)j2, (double)k2, (double)(j2 + i1x - 4), (double)(k2 + 8), ColorUtils.getColor(20, 255, 120, (int)GuiIngame.tabAlpha)
                  );
               }
            }

            if (entityplayer != Minecraft.player) {
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            if (k4x < list.size()) {
               boolean flag1 = entityplayer != null
                  && entityplayer.isWearing(EnumPlayerModelParts.CAPE)
                  && ("Dinnerbone".equals(gameprofilex.getName()) || "Grumm".equals(gameprofilex.getName()));
               this.mc.getTextureManager().bindTexture(networkplayerinfo1x.getLocationSkin());
               int l2 = 8 + (flag1 ? 8 : 0);
               int i3 = 8 * (flag1 ? -1 : 1);
               RenderUtils.setupColor(-1, (float)((int)((double)GuiIngame.tabAlpha * 2.55)));
               GL11.glDisable(3008);
               Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 8.0F, (float)l2, 8.0F, (float)i3, 8.0F, 8.0F, 64.0F, 64.0F);
               GL11.glEnable(3008);
               GlStateManager.resetColor();
               RenderUtils.fixShadows();
               if (entityplayer != null && entityplayer.isWearing(EnumPlayerModelParts.HAT) && GuiIngame.tabAlpha > 26.0F) {
                  int j3 = 8 + (flag1 ? 8 : 0);
                  int k3 = 8 * (flag1 ? -1 : 1);
                  Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 40.0F, (float)j3, 8.0F, (float)k3, 8.0F, 8.0F, 64.0F, 64.0F);
               }

               String s4x = this.getPlayerName(networkplayerinfo1x);
               s4x = ReplaceStrUtils.fixString(s4x);
               if (s4x != null && !s4x.isEmpty() && s4x != "") {
                  String gamemode = networkplayerinfo1x.getGameType() == GameType.SPECTATOR
                     ? TextFormatting.AQUA + "Gm 3 | "
                     : (
                        networkplayerinfo1x.getGameType() == GameType.CREATIVE
                           ? TextFormatting.LIGHT_PURPLE + "Gm 1 | "
                           : (
                              networkplayerinfo1x.getGameType() == GameType.SURVIVAL
                                 ? TextFormatting.GREEN + "Gm 0 | "
                                 : (
                                    networkplayerinfo1x.getGameType() == GameType.ADVENTURE
                                       ? TextFormatting.YELLOW + "Gm 2 | "
                                       : (networkplayerinfo1x.getGameType() == GameType.NOT_SET ? TextFormatting.LIGHT_PURPLE + "Gm -1 | " : "? | ")
                                 )
                           )
                     );

                  try {
                     gamemode = TextFormatting.GRAY + gamemode.replace("|", TextFormatting.WHITE + "|" + TextFormatting.RESET);
                     String isMe = entityplayer == Minecraft.player ? " §r§f|§r §dЭто я§r" : "";
                     String isFriend = "";

                     for (Friend friendx : Client.friendManager.getFriends()) {
                        if (entityplayer != null && entityplayer.getName().contains(friendx.getName())) {
                           isFriend = " §r§f|§r §aЭто друг§r";
                        }
                     }

                     String neared = (entityplayer != null ? TextFormatting.GREEN + "~ " : "") + TextFormatting.RESET;
                     if (GuiIngame.tabAlpha * 2.8333333F >= 5.0F) {
                        Fonts.mntsb_15
                           .drawString(
                              neared + gamemode + s4x.trim() + isMe + isFriend,
                              (float)j2 + 1.0F + 10.0F,
                              (float)k2 + 2.0F,
                              ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), GuiIngame.tabAlpha * 2.8333333F)
                           );
                     }
                  } catch (Exception var43) {
                     System.out.println(gamemode + s4x.trim());
                  }
               }

               if (scoreObjectiveIn != null && networkplayerinfo1x.getGameType() != GameType.SPECTATOR) {
                  int k5 = j2 + i + 1;
                  int l5 = k5 + lx;
                  if (l5 - k5 > 5) {
                     this.drawScoreboardValues(scoreObjectiveIn, k2, gameprofilex.getName(), k5, l5, networkplayerinfo1x);
                  }
               }

               if ((double)GuiIngame.tabAlpha * 2.83333333333 >= 5.0) {
                  this.drawPing(i1x, j2 - (flagx ? 9 : 0), k2, networkplayerinfo1x);
               }

               this.drawPing2(i1x, j2 - (flagx ? 9 : 0) - 16, k2, networkplayerinfo1x, (int)(GuiIngame.tabAlpha * 2.8333333F));
            }

            RenderUtils.resetBlender();
         }
      }
   }

   public void renderPlayerlist2(int width, Scoreboard scoreboardIn, @Nullable ScoreObjective scoreObjectiveIn) {
      NetHandlerPlayClient nethandlerplayclient = Minecraft.player.connection;
      List<NetworkPlayerInfo> list = ENTRY_ORDERING.sortedCopy(nethandlerplayclient.getPlayerInfoMap());
      int i = 0;
      int j = 0;

      for (NetworkPlayerInfo networkplayerinfo : list) {
         int k = this.mc.fontRendererObj.getStringWidth(this.getPlayerName(networkplayerinfo));
         i = Math.max(i, k);
         if (scoreObjectiveIn != null && scoreObjectiveIn.getRenderType() != IScoreCriteria.EnumRenderType.HEARTS) {
            k = this.mc
               .fontRendererObj
               .getStringWidth(" " + scoreboardIn.getOrCreateScore(networkplayerinfo.getGameProfile().getName(), scoreObjectiveIn).getScorePoints());
            j = Math.max(j, k);
         }
      }

      list = list.subList(0, Math.min(list.size(), 80));
      int l3 = list.size();
      int i4 = l3;

      int j4;
      for (j4 = 1; i4 > 20; i4 = (l3 + j4 - 1) / j4) {
         j4++;
      }

      boolean flag = this.mc.isIntegratedServerRunning() || this.mc.getConnection().getNetworkManager().isEncrypted();
      int l;
      if (scoreObjectiveIn != null) {
         if (scoreObjectiveIn.getRenderType() == IScoreCriteria.EnumRenderType.HEARTS) {
            l = 90;
         } else {
            l = j;
         }
      } else {
         l = 0;
      }

      int i1 = Math.min(j4 * ((flag ? 9 : 0) + i + l + 13), width - 50) / j4;
      int j1 = width / 2 - (i1 * j4 + (j4 - 1) * 5) / 2;
      int k1 = 10;
      int l1 = i1 * j4 + (j4 - 1) * 5;
      List<String> list1 = null;
      if (this.header != null) {
         list1 = this.mc.fontRendererObj.listFormattedStringToWidth(this.header.getFormattedText(), width - 50);

         for (String s : list1) {
            l1 = Math.max(l1, this.mc.fontRendererObj.getStringWidth(s));
         }
      }

      List<String> list2 = null;
      if (this.footer != null) {
         list2 = this.mc.fontRendererObj.listFormattedStringToWidth(this.footer.getFormattedText(), width - 50);

         for (String s1 : list2) {
            l1 = Math.max(l1, this.mc.fontRendererObj.getStringWidth(s1));
         }
      }

      if (list1 != null) {
         drawRect(
            width / 2 - l1 / 2 - 1,
            (double)(k1 - 1),
            (double)(width / 2 + l1 / 2 + 1),
            (double)(k1 + list1.size() * this.mc.fontRendererObj.FONT_HEIGHT),
            Integer.MIN_VALUE
         );

         for (String s2 : list1) {
            int i2 = this.mc.fontRendererObj.getStringWidth(s2);
            this.mc.fontRendererObj.drawStringWithShadow(s2, (float)(width / 2 - i2 / 2), (float)k1, -1);
            k1 += this.mc.fontRendererObj.FONT_HEIGHT;
         }

         k1++;
      }

      drawRect(width / 2 - l1 / 2 - 1, (double)(k1 - 1), (double)(width / 2 + l1 / 2 + 1), (double)(k1 + i4 * 9), Integer.MIN_VALUE);

      for (int k4 = 0; k4 < l3; k4++) {
         int l4 = k4 / i4;
         int i5 = k4 % i4;
         int j2 = j1 + l4 * i1 + l4 * 5;
         int k2 = k1 + i5 * 9;
         drawRect(j2, (double)k2, (double)(j2 + i1), (double)(k2 + 8), 553648127);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.enableAlpha();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
         if (k4 < list.size()) {
            NetworkPlayerInfo networkplayerinfo1 = list.get(k4);
            GameProfile gameprofile = networkplayerinfo1.getGameProfile();
            if (flag) {
               EntityPlayer entityplayer = this.mc.world.getPlayerEntityByUUID(gameprofile.getId());
               boolean flag1 = entityplayer != null
                  && entityplayer.isWearing(EnumPlayerModelParts.CAPE)
                  && ("Dinnerbone".equals(gameprofile.getName()) || "Grumm".equals(gameprofile.getName()));
               this.mc.getTextureManager().bindTexture(networkplayerinfo1.getLocationSkin());
               int l2 = 8 + (flag1 ? 8 : 0);
               int i3 = 8 * (flag1 ? -1 : 1);
               Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 8.0F, (float)l2, 8.0F, (float)i3, 8.0F, 8.0F, 64.0F, 64.0F);
               if (entityplayer != null && entityplayer.isWearing(EnumPlayerModelParts.HAT)) {
                  int j3 = 8 + (flag1 ? 8 : 0);
                  int k3 = 8 * (flag1 ? -1 : 1);
                  Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 40.0F, (float)j3, 8.0F, (float)k3, 8.0F, 8.0F, 64.0F, 64.0F);
               }

               j2 += 9;
            }

            String s4 = this.getPlayerName(networkplayerinfo1);
            if (networkplayerinfo1.getGameType() == GameType.SPECTATOR) {
               this.mc.fontRendererObj.drawStringWithShadow(TextFormatting.ITALIC + s4, (float)j2, (float)k2, -1862270977);
            } else {
               this.mc.fontRendererObj.drawStringWithShadow(s4, (float)j2, (float)k2, -1);
            }

            if (scoreObjectiveIn != null && networkplayerinfo1.getGameType() != GameType.SPECTATOR) {
               int k5 = j2 + i + 1;
               int l5 = k5 + l;
               if (l5 - k5 > 5) {
                  this.drawScoreboardValues(scoreObjectiveIn, k2, gameprofile.getName(), k5, l5, networkplayerinfo1);
               }
            }

            this.drawPing2(i1, j2 - (flag ? 9 : 0), k2, networkplayerinfo1, 255);
         }
      }

      if (list2 != null) {
         k1 = k1 + i4 * 9 + 1;
         drawRect(
            width / 2 - l1 / 2 - 1,
            (double)(k1 - 1),
            (double)(width / 2 + l1 / 2 + 1),
            (double)(k1 + list2.size() * this.mc.fontRendererObj.FONT_HEIGHT),
            Integer.MIN_VALUE
         );

         for (String s3 : list2) {
            int j5 = this.mc.fontRendererObj.getStringWidth(s3);
            this.mc.fontRendererObj.drawStringWithShadow(s3, (float)(width / 2 - j5 / 2), (float)k1, -1);
            k1 += this.mc.fontRendererObj.FONT_HEIGHT;
         }
      }
   }

   public void renderPlayerlist3(int width, Scoreboard scoreboardIn, @Nullable ScoreObjective scoreObjectiveIn) {
      int alpher = (int)(GuiIngame.tabAlpha * 1.25F);
      NetHandlerPlayClient nethandlerplayclient = Minecraft.player.connection;
      List<NetworkPlayerInfo> list = ENTRY_ORDERING.sortedCopy(nethandlerplayclient.getPlayerInfoMap());
      int i = 0;
      int j = 0;

      for (NetworkPlayerInfo networkplayerinfo : list) {
         if (networkplayerinfo != null) {
            String s2 = networkplayerinfo.getDisplayName() == null
               ? "Unknown display name"
               : ReplaceStrUtils.fixString(networkplayerinfo.getDisplayName().getFormattedText());
            if (s2 != null && !s2.isEmpty() && s2 != "") {
               String gamemode = networkplayerinfo.getGameType() == GameType.SPECTATOR
                  ? TextFormatting.AQUA + "Gm 3 | "
                  : (
                     networkplayerinfo.getGameType() == GameType.CREATIVE
                        ? TextFormatting.LIGHT_PURPLE + "Gm 1 | "
                        : (
                           networkplayerinfo.getGameType() == GameType.SURVIVAL
                              ? TextFormatting.GREEN + "Gm 0 | "
                              : (
                                 networkplayerinfo.getGameType() == GameType.ADVENTURE
                                    ? TextFormatting.YELLOW + "Gm 2 | "
                                    : (networkplayerinfo.getGameType() == GameType.NOT_SET ? TextFormatting.LIGHT_PURPLE + "Gm -1 | " : "? | ")
                              )
                        )
                  );

               try {
                  gamemode = TextFormatting.GRAY + gamemode.replace("|", TextFormatting.WHITE + "|" + TextFormatting.RESET);
                  GameProfile gameprofile = networkplayerinfo.getGameProfile();
                  String sss = this.getPlayerName(networkplayerinfo);
                  EntityPlayer entityplayer = this.mc.world.getPlayerEntityByUUID(gameprofile.getId());
                  String isMe = entityplayer == Minecraft.player ? " §r§f|§r §dЭто я§r" : "";
                  String isFriend = "";

                  for (Friend friend : Client.friendManager.getFriends()) {
                     if (friend != null && sss.contains(friend.getName())) {
                        isFriend = " §r§f|§r §aЭто друг§r";
                        break;
                     }
                  }

                  String neared = (entityplayer != null ? TextFormatting.GREEN + "~ " : "") + TextFormatting.RESET;
                  s2 = s2 + isMe + isFriend + gamemode + neared + "00000";
               } catch (Exception var41) {
                  System.out.println(gamemode + s2.trim());
               }
            }

            int k = (int)Fonts.mntsb_12.getStringWidth(s2);
            i = Math.max(i, k);
            if (scoreObjectiveIn != null && scoreObjectiveIn.getRenderType() != IScoreCriteria.EnumRenderType.HEARTS) {
               k = this.mc
                  .fontRendererObj
                  .getStringWidth(" " + scoreboardIn.getOrCreateScore(networkplayerinfo.getGameProfile().getName(), scoreObjectiveIn).getScorePoints());
               j = Math.max(j, k);
            }
         }
      }

      list = list.subList(0, Math.min(list.size(), 600));
      int l3 = list.size();
      int i4 = l3;

      int j4;
      for (j4 = 1; i4 > 50; i4 = (l3 + j4 - 1) / j4) {
         j4++;
      }

      boolean flag = this.mc.isIntegratedServerRunning() || this.mc.getConnection().getNetworkManager().isEncrypted();
      int l;
      if (scoreObjectiveIn != null) {
         if (scoreObjectiveIn.getRenderType() == IScoreCriteria.EnumRenderType.HEARTS) {
            l = 90;
         } else {
            l = j;
         }
      } else {
         l = 0;
      }

      int i1 = Math.min(j4 * ((flag ? 9 : 0) + i + l + 13), width - 50) / j4;
      int j1 = width / 2 - (i1 * j4 + (j4 - 1) * 5) / 2;
      int k1 = 10;
      int l1 = i1 * j4 + (j4 - 1) * 5;
      List<String> list1 = null;
      if (this.header != null) {
         list1 = this.mc.fontRendererObj.listFormattedStringToWidth(this.header.getFormattedText(), width - 50);

         for (String s : list1) {
            l1 = Math.max(l1, this.mc.fontRendererObj.getStringWidth(s));
         }
      }

      List<String> list2 = null;
      if (this.footer != null) {
         list2 = this.mc.fontRendererObj.listFormattedStringToWidth(this.footer.getFormattedText(), width - 50);

         for (String s1 : list2) {
            l1 = Math.max(l1, this.mc.fontRendererObj.getStringWidth(s1));
         }
      }

      if (list1 != null && list2 != null) {
         float w = (float)(l1 + 61);
         if (w < 355.0F) {
            w = 355.0F;
         }

         float x1 = (float)(width / 2) - w / 2.0F - 1.0F;
         float y1 = (float)(k1 - 1);
         float x2 = (float)(width / 2) + w / 2.0F + 1.0F;
         float t = (float)(k1 + i4 * 9 + 1);
         float y2 = (float)(i4 * 9 + k1 + list1.size() * this.mc.fontRendererObj.FONT_HEIGHT + list2.size() * this.mc.fontRendererObj.FONT_HEIGHT);
         RenderUtils.customScaledObject2D(x1, y1, x2 - x1, y2 - y1, 1.0F + (1.0F - GuiIngame.tabScale));
         GL11.glTranslated(0.0, (double)(-(1.0F - GuiIngame.tabAlpha / 90.0F) * (y2 / 2.0F - y1 / 2.0F)), 0.0);
         int c = ColorUtils.getColor(0, 0, 0, alpher);
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x1 - 5.0F, y1 - 1.0F, x2 + 5.0F, y2 + 1.0F, 20.0F, 5.0F, c, c, c, c, false, true, true
         );
         RenderUtils.fixShadows();
         if ((int)((float)alpher * 2.25F) > 26) {
            Fonts.mntsb_20
               .drawStringWithShadow(
                  "Online: " + getPlayers().size(),
                  x1 + 5.0F,
                  y1 + 9.0F,
                  ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), (float)((int)((float)alpher * 2.25F)))
               );
            Fonts.mntsb_12
               .drawStringWithShadow(
                  "ServerIp: " + this.mc.getCurrentServerData().serverIP,
                  x1 + 5.0F,
                  y1 + 20.0F,
                  ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), (float)((int)((float)alpher * 2.25F)))
               );
         }
      }

      if (list1 != null) {
         for (String s2x : list1) {
            int i2 = (int)Fonts.comfortaaRegular_17.getStringWidth(s2x);
            if ((float)alpher * 2.25F >= 30.0F) {
               Fonts.comfortaaRegular_17.drawString(s2x, (float)(width / 2 - i2 / 2), (float)k1, ColorUtils.swapAlpha(-1, (float)alpher * 2.25F));
            }

            k1 += this.mc.fontRendererObj.FONT_HEIGHT;
         }

         k1++;
      }

      for (int k4 = 0; k4 < l3; k4++) {
         int l4 = k4 / i4;
         int i5 = k4 % i4;
         int j2 = j1 + l4 * i1 + l4 * 5;
         int k2 = k1 + i5 * 9;
         boolean me = false;
         boolean fr = false;
         if (k4 < list.size()) {
            NetworkPlayerInfo networkplayerinfo1 = list.get(k4);
            GameProfile gameprofile = networkplayerinfo1.getGameProfile();
            String sss = this.getPlayerName(networkplayerinfo1);
            EntityPlayer entityplayer = this.mc.world.getPlayerEntityByUUID(gameprofile.getId());
            if (entityplayer == Minecraft.player) {
               me = true;
            }

            for (Friend friendx : Client.friendManager.getFriends()) {
               if (friendx != null && sss.contains(friendx.getName())) {
                  fr = true;
                  break;
               }
            }
         }

         if (me || fr) {
            RenderUtils.drawLightContureRect(
               (double)j2,
               (double)k2,
               (double)(j2 + i1),
               (double)(k2 + 8),
               ColorUtils.swapAlpha(me ? ColorUtils.getColor(255, 80, 255) : (fr ? ColorUtils.getColor(80, 255, 80) : 553648127), (float)alpher * 2.25F)
            );
         }

         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.enableAlpha();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
         );
         if (k4 < list.size()) {
            NetworkPlayerInfo networkplayerinfo1 = list.get(k4);
            GameProfile gameprofile = networkplayerinfo1.getGameProfile();
            EntityPlayer entityplayer = this.mc.world.getPlayerEntityByUUID(gameprofile.getId());
            boolean flag1 = entityplayer != null
               && entityplayer.isWearing(EnumPlayerModelParts.CAPE)
               && ("Dinnerbone".equals(gameprofile.getName()) || "Grumm".equals(gameprofile.getName()));
            this.mc.getTextureManager().bindTexture(networkplayerinfo1.getLocationSkin());
            int l2 = 8 + (flag1 ? 8 : 0);
            int i3 = 8 * (flag1 ? -1 : 1);
            RenderUtils.setupColor(-1, (float)((int)((double)GuiIngame.tabAlpha * 2.55)));
            GL11.glDisable(3008);
            Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 8.0F, (float)l2, 8.0F, (float)i3, 8.0F, 8.0F, 64.0F, 64.0F);
            GL11.glEnable(3008);
            GlStateManager.resetColor();
            RenderUtils.fixShadows();
            if (entityplayer != null && entityplayer.isWearing(EnumPlayerModelParts.HAT) && GuiIngame.tabAlpha > 26.0F) {
               int j3 = 8 + (flag1 ? 8 : 0);
               int k3 = 8 * (flag1 ? -1 : 1);
               Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 40.0F, (float)j3, 8.0F, (float)k3, 8.0F, 8.0F, 64.0F, 64.0F);
            }

            if (flag) {
               this.mc.getTextureManager().bindTexture(networkplayerinfo1.getLocationSkin());
               Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 8.0F, (float)l2, 8.0F, (float)i3, 8.0F, 8.0F, 64.0F, 64.0F);
               if (entityplayer != null && entityplayer.isWearing(EnumPlayerModelParts.HAT)) {
                  int j3 = 8 + (flag1 ? 8 : 0);
                  int k3 = 8 * (flag1 ? -1 : 1);
                  Gui.drawScaledCustomSizeModalRect((float)j2, (float)k2, 40.0F, (float)j3, 8.0F, (float)k3, 8.0F, 8.0F, 64.0F, 64.0F);
               }

               j2 += 9;
            }

            String s4 = networkplayerinfo1.getDisplayName() == null
               ? this.getPlayerName(networkplayerinfo1)
               : ReplaceStrUtils.fixString(networkplayerinfo1.getDisplayName().getFormattedText());
            if (s4 != null && !s4.isEmpty() && s4 != "") {
               String gamemode = networkplayerinfo1.getGameType() == GameType.SPECTATOR
                  ? TextFormatting.AQUA + "Gm 3 |"
                  : (
                     networkplayerinfo1.getGameType() == GameType.CREATIVE
                        ? TextFormatting.LIGHT_PURPLE + "Gm 1 |"
                        : (
                           networkplayerinfo1.getGameType() == GameType.SURVIVAL
                              ? TextFormatting.GREEN + "Gm 0 |"
                              : (
                                 networkplayerinfo1.getGameType() == GameType.ADVENTURE
                                    ? TextFormatting.YELLOW + "Gm 2 |"
                                    : (networkplayerinfo1.getGameType() == GameType.NOT_SET ? TextFormatting.LIGHT_PURPLE + "Gm -1| " : "? |")
                              )
                        )
                  );

               try {
                  gamemode = TextFormatting.GRAY + gamemode.replace("|", TextFormatting.WHITE + "|" + TextFormatting.RESET);
                  String isMe = entityplayer == Minecraft.player ? " §r§f|§r §dЭто я§r" : "";
                  String isFriend = "";
                  String sss = this.getPlayerName(networkplayerinfo1);

                  for (Friend friendxx : Client.friendManager.getFriends()) {
                     if (friendxx != null && sss.contains(friendxx.getName())) {
                        isFriend = " §r§f|§r §aЭто друг§r";
                        break;
                     }
                  }

                  String neared = (entityplayer != null ? TextFormatting.GREEN + "~ " : "") + TextFormatting.RESET;
                  if (GuiIngame.tabAlpha * 2.8333333F >= 5.0F) {
                     Fonts.mntsb_12
                        .addCachedrawString(
                           neared + gamemode + s4 + isMe + isFriend,
                           (float)j2 + 1.0F + 10.0F,
                           (float)k2 + 3.0F,
                           ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), GuiIngame.tabAlpha * 2.8333333F)
                        );
                  }
               } catch (Exception var40) {
                  System.out.println(gamemode + s4.trim());
               }
            }

            if (scoreObjectiveIn != null && networkplayerinfo1.getGameType() != GameType.SPECTATOR) {
               int k5 = j2 + i + 1;
               int l5 = k5 + l;
               if (l5 - k5 > 5) {
                  this.drawScoreboardValues(scoreObjectiveIn, k2, gameprofile.getName(), k5, l5, networkplayerinfo1);
               }
            }

            this.drawPing2(i1, j2 - (flag ? 9 : 0), k2, networkplayerinfo1, (int)((double)alpher * 2.25));
            if ((int)((double)alpher * 2.25) >= 30) {
               GlStateManager.disableDepth();
               this.drawPing(i1 - 10, j2 - (flag ? 9 : 0), k2, networkplayerinfo1);
               GlStateManager.enableDepth();
            }
         }
      }

      Fonts.mntsb_12.drawAllCaches();
      Fonts.comfortaa_12.drawAllCaches();
      if (list2 != null) {
         k1 = k1 + i4 * 9 + 1;

         for (String s3 : list2) {
            s3 = s3.replace("  ", " ").replace("§l", "").replace("[]", "").replace("§k", "").replace("§m", "").replace("§n", "").replace("§o", "");
            s3.replace(" ", "");
            String var71 = s3.replace("Ａ", "A");
            var71 = var71.replace("Ｂ", "B");
            var71 = var71.replace("Ｃ", "C");
            var71 = var71.replace("Ｄ", "D");
            var71 = var71.replace("Ｅ", "E");
            var71 = var71.replace("Ｆ", "F");
            var71 = var71.replace("Ｇ", "G");
            var71 = var71.replace("Ｈ", "H");
            var71 = var71.replace("Ｉ", "I");
            var71 = var71.replace("Ｊ", "J");
            var71 = var71.replace("Ｋ", "K");
            var71 = var71.replace("Ｌ", "L");
            var71 = var71.replace("Ｍ", "M");
            var71 = var71.replace("Ｎ", "N");
            var71 = var71.replace("Ｏ", "O");
            var71 = var71.replace("Ｐ", "P");
            var71 = var71.replace("Ｑ", "Q");
            var71 = var71.replace("Ｒ", "R");
            var71 = var71.replace("Ｓ", "S");
            var71 = var71.replace("Ｔ", "T");
            var71 = var71.replace("Ｕ", "U");
            var71 = var71.replace("Ｖ", "V");
            var71 = var71.replace("Ｗ", "W");
            var71 = var71.replace("Ｘ", "X");
            var71 = var71.replace("Ｙ", "Y");
            var71 = var71.replace("Ｚ", "Z");
            var71 = var71.replace("▷", ">");
            var71 = var71.replace("◁", "<");
            int j5 = (int)Fonts.comfortaaRegular_17.getStringWidth(var71);
            if ((float)alpher * 2.25F >= 30.0F) {
               Fonts.comfortaaRegular_17.drawString(var71, (float)(width / 2 - j5 / 2), (float)k1, ColorUtils.swapAlpha(-1, (float)alpher * 2.25F));
            }

            k1 += this.mc.fontRendererObj.FONT_HEIGHT;
         }
      }

      RenderUtils.fixShadows();
   }

   protected void drawPing(int p_175245_1_, int p_175245_2_, int p_175245_3_, NetworkPlayerInfo networkPlayerInfoIn) {
      int color = -1;
      if (networkPlayerInfoIn.getResponseTime() < 50000) {
         color = ColorUtils.getColor(255, 40, 40);
      }

      if (networkPlayerInfoIn.getResponseTime() < 400) {
         color = ColorUtils.getColor(255, 255, 0);
      } else if (networkPlayerInfoIn.getResponseTime() < 300) {
         color = ColorUtils.getColor(255, 160, 0);
      }

      if (networkPlayerInfoIn.getResponseTime() < 200) {
         color = ColorUtils.getColor(225, 255, 25);
      }

      if (networkPlayerInfoIn.getResponseTime() < 100) {
         color = ColorUtils.getColor(40, 255, 40);
      }

      int p = networkPlayerInfoIn.getResponseTime();
      String ping = networkPlayerInfoIn.getResponseTime() + "";
      if (p > 15000) {
         ping = "SPOOF";
      }

      GL11.glPushMatrix();
      GL11.glEnable(3042);
      Fonts.comfortaa_12
         .addCachedrawString(
            ping,
            (float)(p_175245_2_ + p_175245_1_)
               - (ping.equalsIgnoreCase("SPOOF") ? 14.0F : 8.5F)
               - Fonts.comfortaa_12.getStringWidth(networkPlayerInfoIn.getResponseTime() + "") / 2.0F,
            (float)p_175245_3_ + 2.5F,
            ColorUtils.swapAlpha(color, GuiIngame.tabAlpha * 2.8333333F)
         );
      GL11.glPopMatrix();
   }

   protected void drawPing2(int p_175245_1_, int p_175245_2_, int p_175245_3_, NetworkPlayerInfo networkPlayerInfoIn, int alpha) {
      if (networkPlayerInfoIn.getResponseTime() <= 15000 || Panic.stop) {
         RenderUtils.setupColor(ColorUtils.getFixedWhiteColor(), (float)alpha);
         this.mc.getTextureManager().bindTexture(ICONS);
         int j;
         if (networkPlayerInfoIn.getResponseTime() < 0) {
            j = 5;
         } else if (networkPlayerInfoIn.getResponseTime() < 150) {
            j = 0;
         } else if (networkPlayerInfoIn.getResponseTime() < 300) {
            j = 1;
         } else if (networkPlayerInfoIn.getResponseTime() < 600) {
            j = 2;
         } else if (networkPlayerInfoIn.getResponseTime() < 1000) {
            j = 3;
         } else {
            j = 4;
         }

         GL11.glEnable(3042);
         GL11.glDisable(3008);
         this.zLevel += 100.0F;
         this.drawTexturedModalRect(p_175245_2_ + p_175245_1_ - 11, p_175245_3_, 0, 176 + j * 8, 10, 8);
         this.zLevel -= 100.0F;
         GL11.glEnable(3008);
         GlStateManager.resetColor();
      }
   }

   private void drawScoreboardValues(ScoreObjective objective, int p_175247_2_, String name, int p_175247_4_, int p_175247_5_, NetworkPlayerInfo info) {
      int i = objective.getScoreboard().getOrCreateScore(name, objective).getScorePoints();
      if (objective.getRenderType() == IScoreCriteria.EnumRenderType.HEARTS) {
         this.mc.getTextureManager().bindTexture(ICONS);
         if (this.lastTimeOpened == info.getRenderVisibilityId()) {
            if (i < info.getLastHealth()) {
               info.setLastHealthTime(Minecraft.getSystemTime());
               info.setHealthBlinkTime((long)(this.guiIngame.getUpdateCounter() + 20));
            } else if (i > info.getLastHealth()) {
               info.setLastHealthTime(Minecraft.getSystemTime());
               info.setHealthBlinkTime((long)(this.guiIngame.getUpdateCounter() + 10));
            }
         }

         if (Minecraft.getSystemTime() - info.getLastHealthTime() > 1000L || this.lastTimeOpened != info.getRenderVisibilityId()) {
            info.setLastHealth(i);
            info.setDisplayHealth(i);
            info.setLastHealthTime(Minecraft.getSystemTime());
         }

         info.setRenderVisibilityId(this.lastTimeOpened);
         info.setLastHealth(i);
         int j = MathHelper.ceil((float)Math.max(i, info.getDisplayHealth()) / 2.0F);
         int k = Math.max(MathHelper.ceil((float)(i / 2)), Math.max(MathHelper.ceil((float)(info.getDisplayHealth() / 2)), 10));
         boolean flag = info.getHealthBlinkTime() > (long)this.guiIngame.getUpdateCounter()
            && (info.getHealthBlinkTime() - (long)this.guiIngame.getUpdateCounter()) / 3L % 2L == 1L;
         if (j > 0) {
            float f = Math.min((float)(p_175247_5_ - p_175247_4_ - 4) / (float)k, 9.0F);
            if (f > 3.0F) {
               for (int l = j; l < k; l++) {
                  this.drawTexturedModalRect((float)p_175247_4_ + (float)l * f, (float)p_175247_2_, flag ? 25 : 16, 0, 9, 9);
               }

               for (int j1 = 0; j1 < j; j1++) {
                  this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, flag ? 25 : 16, 0, 9, 9);
                  if (flag) {
                     if (j1 * 2 + 1 < info.getDisplayHealth()) {
                        this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, 70, 0, 9, 9);
                     }

                     if (j1 * 2 + 1 == info.getDisplayHealth()) {
                        this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, 79, 0, 9, 9);
                     }
                  }

                  if (j1 * 2 + 1 < i) {
                     this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, j1 >= 10 ? 160 : 52, 0, 9, 9);
                  }

                  if (j1 * 2 + 1 == i) {
                     this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, j1 >= 10 ? 169 : 61, 0, 9, 9);
                  }
               }
            } else {
               float f1 = MathHelper.clamp((float)i / 20.0F, 0.0F, 1.0F);
               int i1 = (int)((1.0F - f1) * 255.0F) << 16 | (int)(f1 * 255.0F) << 8;
               String s = (float)i / 2.0F + "";
               if (p_175247_5_ - this.mc.fontRendererObj.getStringWidth(s + "hp") >= p_175247_4_) {
                  s = s + "hp";
               }

               this.mc
                  .fontRendererObj
                  .drawStringWithShadow(s, (float)((p_175247_5_ + p_175247_4_) / 2 - this.mc.fontRendererObj.getStringWidth(s) / 2), (float)p_175247_2_, i1);
            }
         }
      } else {
         String s1 = "" + TextFormatting.YELLOW + i;
         this.mc.fontRendererObj.drawStringWithShadow(s1, (float)(p_175247_5_ - this.mc.fontRendererObj.getStringWidth(s1)), (float)p_175247_2_, 16777215);
      }
   }

   public void setFooter(@Nullable ITextComponent footerIn) {
      this.footer = footerIn;
   }

   public void setHeader(@Nullable ITextComponent headerIn) {
      this.header = headerIn;
   }

   public void resetFooterHeader() {
      this.header = null;
      this.footer = null;
   }

   public static class PlayerComparator implements Comparator<NetworkPlayerInfo> {
      public int compare(NetworkPlayerInfo p_compare_1_, NetworkPlayerInfo p_compare_2_) {
         ScorePlayerTeam scoreplayerteam = p_compare_1_.getPlayerTeam();
         ScorePlayerTeam scoreplayerteam1 = p_compare_2_.getPlayerTeam();
         return ComparisonChain.start()
            .compareTrueFirst(p_compare_1_.getGameType() != GameType.SPECTATOR, p_compare_2_.getGameType() != GameType.SPECTATOR)
            .compare(scoreplayerteam != null ? scoreplayerteam.getRegisteredName() : "", scoreplayerteam1 != null ? scoreplayerteam1.getRegisteredName() : "")
            .compare(p_compare_1_.getGameProfile().getName(), p_compare_2_.getGameProfile().getName())
            .result();
      }
   }
}
