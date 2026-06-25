package ru.govno.client.module.modules;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.BossInfo;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.friendsystem.Friend;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.ReplaceStrUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class FriendsSLink extends Module {
   private final ModeSettings lsSendType;
   private final FloatSettings maxSendCount;
   private final FloatSettings sendDelayLS;
   private final FloatSettings sendDelayChat;
   public final FloatSettings FLX;
   public final FloatSettings FLY;
   private final BoolSettings useChatToo;
   private final BoolSettings chatOnlyLocal200m;
   private final BoolSettings showWithoutInfo;
   private final BoolSettings sendToNeared;
   private final BoolSettings addPointTraces;
   private final BoolSettings showCoordsOnPlate;
   public static FriendsSLink get;
   public static boolean SHOW_GUIDE_ON_FIRST_START;
   public static float flPosX = 15.0F;
   public static float flPosY = 350.0F;
   public static float flWidth = 16.0F;
   public static float flHeight = 16.0F;
   private final AnimationUtils friendsInfoListHeight = new AnimationUtils(this.getFriendInfoHudHeight(null), this.getFriendInfoHudHeight(null), 0.35F);
   private final String[] ruAlphabet = new String[]{"абвгдеёжзийклмнопрстуфхцчшщъыьэюя", "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"};
   private final String[] enAlphabet = new String[]{"abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"};
   private final TimerHelper sendLinksTimerLS = new TimerHelper();
   private final TimerHelper sendLinksTimerChat = new TimerHelper();
   private static final CopyOnWriteArrayList<FriendsSLink.Link> aliveFriendLinksList = new CopyOnWriteArrayList<>();

   public FriendsSLink() {
      super("FriendsSLink", 0, Module.Category.MISC);
      this.settings.add(this.lsSendType = new ModeSettings("LSSendType", "/t", this, new String[]{"/msg", "/tell", "/m", "/t"}));
      this.settings.add(this.useChatToo = new BoolSettings("UseChatToo", false, this));
      this.settings.add(this.chatOnlyLocal200m = new BoolSettings("ChatOnlyLocal200m", false, this, () -> this.useChatToo.getBool()));
      this.settings.add(this.sendDelayLS = new FloatSettings("SendDelayLS", 4000.0F, 7000.0F, 300.0F, this));
      this.settings.add(this.sendDelayChat = new FloatSettings("SendDelayChat", 4000.0F, 7000.0F, 300.0F, this, () -> this.useChatToo.getBool()));
      this.settings.add(this.maxSendCount = new FloatSettings("MaxSendCount", 1.0F, 3.0F, 1.0F, this));
      this.settings.add(this.showWithoutInfo = new BoolSettings("ShowWithoutInfo", true, this));
      this.settings.add(this.sendToNeared = new BoolSettings("SendToNeared", true, this));
      this.settings.add(this.addPointTraces = new BoolSettings("AddPointTraces", false, this));
      this.settings.add(this.showCoordsOnPlate = new BoolSettings("ShowCoordsOnPlate", true, this));
      this.settings.add(this.FLX = new FloatSettings("FLX", 0.008F, 1.0F, 0.0F, this, () -> false));
      this.settings.add(this.FLY = new FloatSettings("FLY", 0.6F, 1.0F, 0.0F, this, () -> false));
      this.setDemand(0, 3);
      get = this;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   public static int getPVPTimeSecInt() {
      if (!GuiBossOverlay.mapBossInfos2.isEmpty()) {
         String anyPVPInfoLine = null;

         for (String infoLowerCase : GuiBossOverlay.mapBossInfos2
            .values()
            .stream()
            .map(BossInfo::getName)
            .map(ITextComponent::getUnformattedText)
            .map(String::toLowerCase)
            .toList()) {
            if (infoLowerCase.contains("pvp") || infoLowerCase.contains("пвп") || infoLowerCase.contains("сек.")) {
               anyPVPInfoLine = infoLowerCase;
               break;
            }
         }

         if (anyPVPInfoLine != null) {
            String numeric = "";

            for (char c : anyPVPInfoLine.toCharArray()) {
               if (Arrays.stream(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}).anyMatch(integer -> String.valueOf(integer).equalsIgnoreCase(String.valueOf(c)))) {
                  numeric = numeric + c;
               } else if (numeric.length() > 0) {
                  break;
               }
            }

            if (numeric.length() > 0) {
               return Integer.parseInt(numeric);
            }
         }
      }

      return 0;
   }

   @Override
   public void onToggled(boolean actived) {
      aliveFriendLinksList.clear();
      this.sendLinksTimerLS.reset();
      this.sendLinksTimerChat.reset();
      if (actived) {
         this.stateAnim.to = 0.0F;
         this.stateAnim.setAnim(0.0F);
      }

      super.onToggled(actived);
   }

   @Override
   public void onUpdate() {
      if (SHOW_GUIDE_ON_FIRST_START && mc.world != null && Minecraft.player != null && Minecraft.player.ticksExisted > 40) {
         Client.msg("§f§lModules:§r §7[§l" + this.name + "§r§7]: гайд по использованию:", false);
         Client.msg("§7Для использования этой функции вам нужен", false);
         Client.msg("§7друг с такой-же функцией на том-же сервере", false);
         Client.msg("§7на котором вы находитесь, задержки чата стоит", false);
         Client.msg("§7использовать минимально-рабочие для сервера.", false);
         Client.msg("§7UseChatToo не стоит использовать на серверах", false);
         Client.msg("§7где вам могут выдать мут за спам.", false);
         Client.msg("§ChatOnlyLocal200m можно использовать в случаях риска мута на сервере,", false);
         Client.msg("§что бы информация отправлялась только в неаре а не глобал.", false);
         Client.msg("§7EncryptNumbers нужен для шифрования координат,", false);
         Client.msg("§7здоровья и фпс. AddPointTraces отрисует синим цветом", false);
         Client.msg("§7примерное местоположение друга системой поинтов.", false);
         Client.msg("§7SendToNeared разрешает взаимный обмен уникальной", false);
         Client.msg("§7информацией при находжении друга в поле зрения,", false);
         Client.msg("§7то есть ФПС или кт и прочее что будет добавлено в будущем.", false);
         Client.msg("§7Для начала исползования надо верифнуть друга для", false);
         Client.msg("§7отправки сведений ему, это делается так:", false);
         Client.msg("§7.f link (имя друга к которому хотите подключиться).", false);
         SHOW_GUIDE_ON_FIRST_START = false;
      }

      this.updateFriendsLinksList();
      this.linksSendingOnUpdate(
         this.lsSendType,
         this.maxSendCount.getInt(),
         (long)this.sendDelayLS.getInt(),
         this.useChatToo.getBool(),
         this.chatOnlyLocal200m.getBool(),
         (long)this.sendDelayChat.getInt()
      );
      super.onUpdate();
   }

   public boolean onPreReceivePacketEventWasCancel(Packet<?> packetIn) {
      if (this.actived && packetIn instanceof SPacketChat chatPacket && !Client.friendManager.getFriends().isEmpty()) {
         if (this.cancelSChatPacketIfReceiveFriendEncodedLink(chatPacket, 7100L)
            || aliveFriendLinksList.stream().anyMatch(link -> link.isChatCompetitor(chatPacket))) {
            return true;
         }

         if (chatPacket.getChatComponent() != null
            && chatPacket.getChatComponent().getUnformattedText() != null
            && new FriendsSLink.Link("", 0L).canReadAsData(chatPacket.getChatComponent().getUnformattedText())) {
            Client.msg("§f§lModules:§r §7[§l" + this.name + "§r§7]: SLink данные не обработаны получател(ем/ями).", false);
            return true;
         }
      }

      return false;
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      if (this.isActived()) {
         this.maxSendCount.setFloat((float)((int)(this.maxSendCount.getFloat() + 0.5F)));
         List<FriendsSLink.LinkObj> friendsInfoList = this.getFriendLinksAsDisplayStrings(this.showWithoutInfo.getBool(), this.showCoordsOnPlate.getBool());
         float curX = this.FLX.getFloat() * (float)sr.getScaledWidth();
         float curY = this.FLY.getFloat() * (float)sr.getScaledHeight();
         this.friendsInfoListHeight.to = this.getFriendInfoHudHeight(friendsInfoList);
         float x = flPosX;
         float y = flPosY;
         float w = this.getFriendInfoHudWidth(friendsInfoList) + 5.0F;
         CFontRenderer fontFL = Fonts.mntsb_12;
         float h = this.friendsInfoListHeight.getAnim();
         GL11.glPushMatrix();
         GL11.glDepthMask(false);
         RenderUtils.hudRectWithString(x, y, x + w, y + h, "Friends info", Hud.get.HudRectMode.getMode(), Hud.get.ManyGlows.getBool(), 16);
         float yp = y + 25.0F - 10.0F;
         if (friendsInfoList.size() == 0) {
            String lots = TextFormatting.DARK_GRAY + "Friends is empty";
            fontFL.drawStringWithShadow(lots, x + 3.0F, yp + 2.5F, -1);
         } else {
            int rd = 200;

            for (FriendsSLink.LinkObj infoString : friendsInfoList) {
               float textX = x + 4.0F;
               if (infoString.getSkin() != null) {
                  mc.getTextureManager()
                     .bindTexture(
                        WorldRender.get
                           .updatedResourceSkin(
                              infoString.getSkin(),
                              mc.world
                                 .playerEntities
                                 .stream()
                                 .filter(player -> player != null && infoString.getText().contains(player.getName()))
                                 .findAny()
                                 .orElse(null)
                           )
                     );
                  GL11.glPushMatrix();
                  RenderUtils.glColor(-1);
                  GL11.glDisable(3008);
                  GL11.glEnable(3553);
                  GL11.glDisable(2929);
                  float headScale = 8.0F;
                  float headExtOverlay = 0.5F;
                  GL11.glTranslated((double)(x + 3.5F), (double)yp, 0.0);
                  Gui.drawScaledCustomSizeModalRect(0.0F, 0.0F, 8.0F, 8.0F, 8.0F, 8.0F, headScale, headScale, 64.0F, 64.0F);
                  Gui.drawScaledCustomSizeModalRect(
                     -headExtOverlay,
                     -headExtOverlay,
                     39.0F,
                     8.0F,
                     10.0F,
                     8.0F,
                     headScale + headExtOverlay * 2.0F,
                     headScale + headExtOverlay * 2.0F,
                     64.0F,
                     64.0F
                  );
                  GL11.glEnable(3008);
                  GL11.glEnable(2929);
                  GlStateManager.resetColor();
                  GL11.glPopMatrix();
                  textX += 9.0F;
               }

               fontFL.drawStringWithShadow(infoString.getText(), textX, yp + 3.0F, ColorUtils.getColor(255, 255, 255, Math.max((float)rd, 33.0F)));
               float timePC10 = infoString.getTimePC10();
               if (timePC10 != 0.0F) {
                  float durrCircleSize = 6.5F;
                  float cx = x + w - 3.0F - durrCircleSize / 2.0F;
                  float cy = yp + durrCircleSize / 2.0F + 1.0F;
                  RenderUtils.drawSmoothCircle((double)cx, (double)cy, durrCircleSize / 2.0F + 0.5F, ColorUtils.swapAlpha(0, 60.0F));
                  RenderUtils.drawClientCircleWithOverallToColor(
                     cx,
                     (double)cy,
                     durrCircleSize / 2.0F - 1.0F,
                     360.0F * timePC10,
                     1.5F,
                     0.45F,
                     ColorUtils.getOverallColorFrom(ColorUtils.getProgressColor(timePC10).getRGB(), -1),
                     1.0F
                  );
               }

               yp += 9.0F;
            }
         }

         GL11.glDepthMask(true);
         GL11.glEnable(2929);
         GL11.glPopMatrix();
         float speedAnim = (float)Minecraft.frameTime * 0.04F;
         flPosX = MathUtils.harp(flPosX, curX, speedAnim);
         flPosY = MathUtils.harp(flPosY, curY, speedAnim);
         flWidth = MathUtils.harp(flWidth, w, speedAnim);
         flHeight = MathUtils.harp(flHeight, h, speedAnim);
      }
   }

   private float getFriendInfoHudWidth(List<FriendsSLink.LinkObj> friendsInfoList) {
      float w = 75.0F;

      for (FriendsSLink.LinkObj info : friendsInfoList) {
         float tempW = Fonts.mntsb_12.getStringWidth(info.getText()) + (info.getSkin() == null ? 0.0F : 9.0F) + (info.getTimePC10() == 0.0F ? 0.0F : 9.0F);
         if (tempW + 2.0F > w) {
            w = tempW + 2.0F;
         }
      }

      return MathUtils.clamp(w, 75.0F, 250.0F);
   }

   private float getFriendInfoHudHeight(List<FriendsSLink.LinkObj> friendsInfoList) {
      float h = 16.0F;
      float h2 = 0.0F;
      if (friendsInfoList != null && !friendsInfoList.isEmpty()) {
         for (FriendsSLink.LinkObj info : friendsInfoList) {
            h2 += 9.0F;
         }
      } else {
         h2 += 9.0F;
      }

      if (h2 < 9.0F) {
         h2 = 9.0F;
      }

      return h + h2;
   }

   public boolean isHoveredFriendsInfoHUD(int mouseX, int mouseY) {
      return this.isActived() && RenderUtils.isHovered((float)mouseX, (float)mouseY, flPosX, flPosY, flWidth, flHeight);
   }

   private List<String> getToLinkFriendsNamesSend(boolean nearedFilter) {
      try {
         List<NetworkPlayerInfo> listTab = GuiPlayerTabOverlay.ENTRY_ORDERING.sortedCopy(mc.getConnection().getPlayerInfoMap());
         List<String> namesCollection = new ArrayList<>();

         for (NetworkPlayerInfo info : listTab) {
            GameProfile profile;
            String name;
            if (info != null
               && (profile = info.getGameProfile()) != null
               && (name = profile.getName()) != null
               && !name.equalsIgnoreCase(mc.getSession().getUsername())
               && Client.friendManager.isFriend(name)
               && Client.friendManager.getFriend(name).isLinked()) {
               boolean handle = !name.contains(mc.getSession().getUsername())
                  && (
                     !nearedFilter
                        || mc.world == null
                        || mc.world.playerEntities.stream().noneMatch(player -> player != null && player.isEntityAlive() && player.getName().contains(name))
                  );
               if (handle) {
                  namesCollection.add(name);
               }

               return namesCollection;
            }
         }
      } catch (Exception var9) {
         var9.printStackTrace();
      }

      return new ArrayList<>();
   }

   private String getLinkMessageSplit() {
      return "sKIp";
   }

   private String getNumericSplit() {
      return "nUMb";
   }

   private String getStringLineReplace() {
      return "LiNE";
   }

   private String getStringUnderLineReplace() {
      return "UlIne";
   }

   private String getStringSpaceReplace() {
      return "sTeP";
   }

   private String transStringSymbols(String stringIn, boolean toRuLang) {
      return StringUtils.replaceChars(
         StringUtils.replaceChars(stringIn, toRuLang ? this.enAlphabet[0] : this.ruAlphabet[0], toRuLang ? this.ruAlphabet[0] : this.enAlphabet[0]),
         toRuLang ? this.enAlphabet[1] : this.ruAlphabet[1],
         toRuLang ? this.ruAlphabet[1] : this.enAlphabet[1]
      );
   }

   private String stringEncodeAndDecode(String stringIn, boolean encode) {
      String numSwap = this.getNumericSplit();
      String spaceSwap = this.getStringSpaceReplace();
      String underLineSwap = this.getStringUnderLineReplace();
      String lineSwap = this.getStringLineReplace();
      if (encode) {
         String encoded = "";

         for (char c : stringIn.toCharArray()) {
            String charString = String.valueOf(c);
            switch (charString) {
               case " ":
                  encoded = encoded + spaceSwap;
                  break;
               case "_":
                  encoded = encoded + underLineSwap;
                  break;
               case "-":
                  encoded = encoded + lineSwap;
                  break;
               case "0":
                  encoded = encoded + numSwap + "А";
                  break;
               case "1":
                  encoded = encoded + numSwap + "Б";
                  break;
               case "2":
                  encoded = encoded + numSwap + "В";
                  break;
               case "3":
                  encoded = encoded + numSwap + "Г";
                  break;
               case "4":
                  encoded = encoded + numSwap + "Д";
                  break;
               case "5":
                  encoded = encoded + numSwap + "Е";
                  break;
               case "6":
                  encoded = encoded + numSwap + "Ё";
                  break;
               case "7":
                  encoded = encoded + numSwap + "Ж";
                  break;
               case "8":
                  encoded = encoded + numSwap + "З";
                  break;
               case "9":
                  encoded = encoded + numSwap + "И";
                  break;
               default:
                  encoded = encoded + c;
            }
         }

         stringIn = encoded;
      }

      stringIn = this.transStringSymbols(stringIn, encode);
      if (!encode) {
         stringIn = stringIn.replaceAll(this.transStringSymbols(spaceSwap, false), " ")
            .replaceAll(this.transStringSymbols(underLineSwap, false), "_")
            .replaceAll(this.transStringSymbols(lineSwap, false), "-")
            .replaceAll(this.transStringSymbols(numSwap + "А", false), "0")
            .replaceAll(this.transStringSymbols(numSwap + "Б", false), "1")
            .replaceAll(this.transStringSymbols(numSwap + "В", false), "2")
            .replaceAll(this.transStringSymbols(numSwap + "Г", false), "3")
            .replaceAll(this.transStringSymbols(numSwap + "Д", false), "4")
            .replaceAll(this.transStringSymbols(numSwap + "Е", false), "5")
            .replaceAll(this.transStringSymbols(numSwap + "Ё", false), "6")
            .replaceAll(this.transStringSymbols(numSwap + "Ж", false), "7")
            .replaceAll(this.transStringSymbols(numSwap + "З", false), "8")
            .replaceAll(this.transStringSymbols(numSwap + "И", false), "9");
      }

      return stringIn;
   }

   private String createLinkStringToSend(String sendToPlayerName) {
      String split = this.getLinkMessageSplit();
      Object[] infos = new Object[]{
         mc.getSession().getUsername(),
         (int)Minecraft.player.posX,
         (int)Minecraft.player.posY,
         (int)Minecraft.player.posZ,
         (int)(Minecraft.player.getHealth() + Minecraft.player.getAbsorptionAmount()),
         Minecraft.getDebugFPS(),
         getPVPTimeSecInt()
      };
      String start = sendToPlayerName == null ? "" : " " + sendToPlayerName + " ";
      String infoString = "";

      for (Object info : infos) {
         infoString = infoString + split;
         infoString = infoString + (info instanceof Integer ? (Integer)info : (String)info);
      }

      infoString = this.stringEncodeAndDecode(infoString, true);
      return start + infoString;
   }

   private boolean cancelSChatPacketIfReceiveFriendEncodedLink(SPacketChat chatPacket, long maxWaitLink) {
      if (chatPacket != null && chatPacket.getChatComponent() != null) {
         ITextComponent messageComponent = chatPacket.getChatComponent();
         String message = ReplaceStrUtils.fixString(messageComponent.getFormattedText());
         FriendsSLink.Link newLink = new FriendsSLink.Link(message, maxWaitLink);
         if (newLink.isDead()) {
            return false;
         } else {
            FriendsSLink.Link searchedContain = aliveFriendLinksList.stream().filter(Link -> Link.containsLink(newLink)).findFirst().orElse(null);
            if (searchedContain != null) {
               searchedContain.updateAsChatReceive(message);
            } else {
               aliveFriendLinksList.add(newLink);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void updateFriendsLinksList() {
      if (!aliveFriendLinksList.isEmpty()) {
         aliveFriendLinksList.forEach(FriendsSLink.Link::updateObj);
      }

      if (mc.world != null) {
         List<EntityPlayer> friendPlayers = mc.world
            .playerEntities
            .stream()
            .filter(player -> player != null && player.isEntityAlive())
            .filter(player -> Client.friendManager.isFriend(player.getName()))
            .toList();
         if (!friendPlayers.isEmpty()) {
            for (EntityPlayer friendPlayer : friendPlayers) {
               FriendsSLink.Link searchedLink = aliveFriendLinksList.stream().filter(link -> link.containsPlayer(friendPlayer)).findFirst().orElse(null);
               if (searchedLink == null) {
                  aliveFriendLinksList.add(new FriendsSLink.Link(friendPlayer, 7100L));
               } else {
                  searchedLink.updateAsPlayer(friendPlayer);
               }
            }
         }
      }

      if (!aliveFriendLinksList.isEmpty()) {
         aliveFriendLinksList.removeIf(FriendsSLink.Link::isDead);
      }
   }

   private void linksSendingOnUpdate(
      ModeSettings commandMSGModeSetting, int countMessagesInTickLimit, long sendDelayLS, boolean useChatToo, boolean chatOnlyLocal200m, long sendDelayChat
   ) {
      if (mc.world != null) {
         boolean onlyNear200mChat = false;
         if (chatOnlyLocal200m
            && (aliveFriendLinksList.isEmpty() || !(onlyNear200mChat = aliveFriendLinksList.stream().anyMatch(link -> link.getDistance() <= 200.0)))) {
            useChatToo = onlyNear200mChat;
         }

         if (this.sendLinksTimerLS.hasReached((double)((float)sendDelayLS / (useChatToo ? 2.0F / ((float)sendDelayChat / (float)sendDelayLS) : 1.0F)))
            || useChatToo && this.sendLinksTimerChat.hasReached((double)sendDelayChat)) {
            List<String> onlineFriendsNames = this.getToLinkFriendsNamesSend(!this.sendToNeared.getBool());
            if (!onlineFriendsNames.isEmpty()) {
               List<String> onlineFriendsNamesSorted = (List<String>)(aliveFriendLinksList.isEmpty() ? onlineFriendsNames : new ArrayList<>());
               if (!onlineFriendsNamesSorted.isEmpty()) {
                  Collections.reverse(onlineFriendsNamesSorted);
               }

               List<String> tempFriendsNames = new ArrayList<>();
               if (!aliveFriendLinksList.isEmpty()) {
                  List<FriendsSLink.Link> searchedLinksToSort = new ArrayList<>();

                  for (String friendName : onlineFriendsNames) {
                     FriendsSLink.Link tempSearchedLink = aliveFriendLinksList.stream()
                        .filter(link -> link.name.equalsIgnoreCase(friendName))
                        .findAny()
                        .orElse(null);
                     if (tempSearchedLink != null) {
                        searchedLinksToSort.add(tempSearchedLink);
                     } else {
                        tempFriendsNames.add(friendName);
                     }
                  }

                  onlineFriendsNamesSorted.addAll(tempFriendsNames);
                  if (searchedLinksToSort.size() > 1) {
                     searchedLinksToSort.sort(
                        Comparator.comparingLong(
                           link -> link.getLastDelayUpdate() > link.delayUpdateMax ? link.getLastDelayUpdate() : -link.getLastDelayUpdate()
                        )
                     );
                  }

                  onlineFriendsNamesSorted.addAll(searchedLinksToSort.stream().map(link -> link.name).collect(Collectors.toList()));
               }

               int sendCounter = 0;
               if (this.sendLinksTimerLS.hasReached((double)sendDelayLS)) {
                  for (String friendNamex : onlineFriendsNamesSorted) {
                     String toSendString = commandMSGModeSetting.getMode() + this.createLinkStringToSend(friendNamex);
                     mc.getConnection().preSendPacket(new CPacketChatMessage(toSendString));
                     this.sendLinksTimerLS.reset();
                     if (++sendCounter >= countMessagesInTickLimit) {
                        break;
                     }
                  }
               }

               if ((useChatToo || onlyNear200mChat)
                  && this.sendLinksTimerLS.hasReached((double)((float)(sendDelayLS / 2L) / ((float)sendDelayChat / (float)sendDelayLS)))
                  && this.sendLinksTimerChat.hasReached((double)sendDelayChat)) {
                  for (String friendNamexx : onlineFriendsNamesSorted) {
                     FriendsSLink.Link tempSearchedLink = aliveFriendLinksList.stream()
                        .filter(link -> link.name.equalsIgnoreCase(friendNamexx))
                        .findAny()
                        .orElse(null);
                     String globalPrefix = (tempSearchedLink == null || tempSearchedLink.nearedCheckTicks == 0) && !onlyNear200mChat ? "!" : "";
                     String toSendString = globalPrefix + this.createLinkStringToSend(null);
                     mc.getConnection().preSendPacket(new CPacketChatMessage(toSendString));
                     this.sendLinksTimerChat.reset();
                     if (++sendCounter >= 1) {
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   public List<FriendsSLink.LinkObj> getFriendLinksAsDisplayStrings(boolean showWithoutInfo, boolean showCoords) {
      List<FriendsSLink.LinkObj> lines = new ArrayList<>();
      if (Client.friendManager.getFriends().isEmpty() && aliveFriendLinksList.isEmpty()) {
         return lines;
      } else {
         boolean canCheckOnline = mc.getConnection() != null
            && mc.getConnection().getPlayerInfoMap() != null
            && !mc.getConnection().getPlayerInfoMap().isEmpty();

         for (Friend friend : Client.friendManager.getFriends()) {
            if (friend != null && !friend.getName().equalsIgnoreCase(mc.getSession().getUsername())) {
               FriendsSLink.Link tempLinkFriend = aliveFriendLinksList.stream().filter(link -> link.containsName(friend.getName())).findAny().orElse(null);
               boolean showOnline = canCheckOnline && mc.getConnection().getPlayerInfo(friend.getName()) != null;
               if (tempLinkFriend != null || showWithoutInfo || showOnline) {
                  lines.add(
                     tempLinkFriend != null
                        ? new FriendsSLink.LinkObj(tempLinkFriend.getDisplayString(showCoords), tempLinkFriend.getSkin(), tempLinkFriend.getTimeOut10())
                        : new FriendsSLink.LinkObj(
                           TextFormatting.GRAY + friend.getName() + (showOnline ? TextFormatting.GREEN + " online" : TextFormatting.DARK_GRAY + " offline"),
                           null,
                           0.0F
                        )
                  );
               }
            }
         }

         return lines;
      }
   }

   public List<PointTrace> getPointTraceListAppendAtFriendLinks(List<PointTrace> pointTraceList) {
      if (this != null && !aliveFriendLinksList.isEmpty() && this.addPointTraces.getBool()) {
         List<PointTrace> var4 = new ArrayList<>(pointTraceList);
         boolean handleNeared = false;
         List<PointTrace> linkTraces = aliveFriendLinksList.stream()
            .map(link -> link.getPointTrace(handleNeared))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
         if (linkTraces.isEmpty()) {
            return var4;
         } else {
            var4.addAll(linkTraces);
            return var4;
         }
      } else {
         return pointTraceList;
      }
   }

   private class Link {
      private String name;
      private int prevXPos;
      private int prevYPos;
      private int prevZPos;
      private int xPos;
      private int yPos;
      private int zPos;
      private int hp;
      private int fps;
      private boolean successRead;
      private final long delayUpdateMax;
      private long lastDelay = 0L;
      private final TimerHelper timeOut = new TimerHelper();
      private String lastInputString = null;
      private int nearedCheckTicks;
      private ResourceLocation skinLoc;
      private int downlerpTicksUpdateKTSec = 20;
      private int currentKTSec;

      public Link(String input, long delayUpdateMax) {
         this.updateAsChatReceive(input);
         this.delayUpdateMax = delayUpdateMax;
      }

      public Link(EntityPlayer friendPlayer, long delayUpdateMax) {
         this.updateAsPlayer(friendPlayer);
         this.delayUpdateMax = delayUpdateMax;
      }

      private boolean canReadAsData(String input) {
         return input.split(FriendsSLink.this.getLinkMessageSplit()).length >= 6
            || input.split(FriendsSLink.this.stringEncodeAndDecode(FriendsSLink.this.getLinkMessageSplit(), true)).length >= 6;
      }

      public void updateAsChatReceive(String input) {
         if (this.canReadAsData(input)) {
            try {
               input = FriendsSLink.this.stringEncodeAndDecode(input, false);
               this.lastInputString = input;
               int moveIndex = 1;
               String split = FriendsSLink.this.getLinkMessageSplit();
               String[] splitted = input.split(split);
               this.name = splitted[moveIndex];
               this.updatePreviousPos(true);
               this.xPos = Integer.parseInt(splitted[moveIndex + 1]);
               this.yPos = Integer.parseInt(splitted[moveIndex + 2]);
               this.zPos = Integer.parseInt(splitted[moveIndex + 3]);
               this.updatePreviousPos(false);

               for (int i = 0; i < 10; i++) {
                  if (splitted[moveIndex + 4].contains(i + "")) {
                     this.hp = Integer.parseInt(splitted[moveIndex + 4]);
                     break;
                  }
               }

               this.fps = Integer.parseInt(splitted[moveIndex + 5]);
               this.receiveKTSec(Integer.parseInt(splitted[moveIndex + 6]));
               this.successRead = true;
               this.updateInitSkin();
               this.lastDelay = this.timeOut.getTime();
               this.timeOut.reset();
            } catch (Exception var6) {
               var6.printStackTrace();
               this.successRead = false;
            }
         }
      }

      private void updatePreviousPos(boolean pre) {
         if (pre || this.prevXPos == 0 && this.prevYPos == 0 && this.prevZPos == 0) {
            this.prevXPos = this.xPos;
            this.prevYPos = this.yPos;
            this.prevZPos = this.zPos;
         }
      }

      public void updateAsPlayer(EntityPlayer friendPlayer) {
         if (friendPlayer != null && friendPlayer.isEntityAlive()) {
            this.name = friendPlayer.getName();
            this.updatePreviousPos(true);
            this.xPos = (int)friendPlayer.posX;
            this.yPos = (int)friendPlayer.posY;
            this.zPos = (int)friendPlayer.posZ;
            this.updatePreviousPos(false);
            this.hp = (int)(friendPlayer.getHealth() + friendPlayer.getAbsorptionAmount());
            this.successRead = true;
            this.lastDelay = this.timeOut.getTime();
            this.timeOut.reset();
            this.nearedCheckTicks = 3;
         }
      }

      public void updateObj() {
         if (this.nearedCheckTicks > 0) {
            this.nearedCheckTicks--;
         }

         this.updateInitSkin();
         this.updateKTSec();
      }

      public boolean isDead() {
         return !this.successRead || this.timeOut.hasReached((double)this.delayUpdateMax) || !Client.friendManager.isFriend(this.name);
      }

      public long getLastDelayUpdate() {
         return this.lastDelay;
      }

      private float getCustomPartialElapse() {
         return Math.min((float)this.timeOut.getTime() / (float)Math.max(this.getLastDelayUpdate(), 1L), 1.0F);
      }

      public Vec3d getRenderPointPosition() {
         float partialElapse = this.getCustomPartialElapse();
         float var3 = partialElapse * 2.0F;
         float var4 = Math.min(var3, 1.0F);
         float var5 = (float)MathUtils.easeInOutQuad((double)var4);
         partialElapse = MathUtils.lerp(partialElapse, var5, 0.75F);
         return new Vec3d(
            (double)MathUtils.lerp((float)this.prevXPos, (float)this.xPos, partialElapse),
            (double)MathUtils.lerp((float)this.prevYPos, (float)this.yPos, partialElapse),
            (double)MathUtils.lerp((float)this.prevZPos, (float)this.zPos, partialElapse)
         );
      }

      @Nullable
      public PointTrace getPointTrace(boolean handleNeared) {
         if (!handleNeared && this.nearedCheckTicks != 0) {
            return null;
         } else {
            String pointName = this.name;
            Vec3d pointPosition = this.getRenderPointPosition();
            return PointTrace.getAsLink(pointName, pointPosition);
         }
      }

      public String getDisplayString(boolean showCoords) {
         boolean neared = this.nearedCheckTicks > 0;
         boolean outdated = this.getTimeOut10() <= 0.175F;
         int distance = (int)this.getDistance();
         int ktSec = this.getKTSecInt();
         String prefix = (neared ? TextFormatting.YELLOW + "[N]" : (outdated ? TextFormatting.GRAY + "[-]" : "")) + TextFormatting.RESET + " ";
         String name = (outdated ? TextFormatting.DARK_GRAY : TextFormatting.GRAY) + this.name + TextFormatting.RESET + " ";
         String coordsAndDST = (
               showCoords
                  ? TextFormatting.DARK_GRAY + "[" + TextFormatting.WHITE + this.xPos + " " + this.yPos + " " + this.zPos + TextFormatting.DARK_GRAY + "] "
                  : ""
            )
            + (distance < 20 ? "" : (neared ? "" : TextFormatting.YELLOW + "~") + TextFormatting.GOLD + (int)this.getDistance() + "m ")
            + TextFormatting.RESET;
         String hp = ""
            + (this.hp > 16 ? TextFormatting.GREEN : (this.hp > 12 ? TextFormatting.YELLOW : (this.hp > 7 ? TextFormatting.RED : TextFormatting.DARK_RED)))
            + this.hp
            + TextFormatting.GRAY
            + (this.fps == 0 ? "hp" : "hp ");
         String fps = this.fps == 0 ? "" : "" + TextFormatting.AQUA + this.fps + TextFormatting.GRAY + (ktSec > 0 ? "fps " : "fps");
         String kt = ktSec == 0
            ? ""
            : ""
               + (
                  ktSec < 7 && this.downlerpTicksUpdateKTSec % 4 <= 1
                     ? (this.downlerpTicksUpdateKTSec % 4 == 1 ? TextFormatting.DARK_PURPLE : TextFormatting.LIGHT_PURPLE)
                     : TextFormatting.RED
               )
               + ktSec
               + TextFormatting.GRAY
               + "kt";
         return prefix + name + coordsAndDST + hp + fps + kt;
      }

      private void updateInitSkin() {
         if (this.skinLoc == null) {
            try {
               ResourceLocation skin = Module.mc.getConnection().getPlayerInfo(this.name).getLocationSkin();
               if (skin != null) {
                  this.skinLoc = skin;
               }
            } catch (Exception var2) {
            }
         }
      }

      private void updateKTSec() {
         if (this.currentKTSec > 0 && this.currentKTSec <= 27) {
            if (this.downlerpTicksUpdateKTSec == 0) {
               this.currentKTSec--;
               this.downlerpTicksUpdateKTSec = 20;
            }

            if (this.downlerpTicksUpdateKTSec > 0) {
               this.downlerpTicksUpdateKTSec--;
            }
         }
      }

      private void receiveKTSec(int secondsInt) {
         this.currentKTSec = secondsInt;
         this.downlerpTicksUpdateKTSec = 20;
      }

      public int getKTSecInt() {
         return this.currentKTSec;
      }

      public float getKTSecFloat() {
         if (this.currentKTSec > 27) {
            return (float)this.currentKTSec;
         } else {
            return this.currentKTSec > 0 ? (float)(this.currentKTSec - 1) + (float)this.downlerpTicksUpdateKTSec / 20.0F : 0.0F;
         }
      }

      public ResourceLocation getSkin() {
         return this.skinLoc;
      }

      public float getTimeOut10() {
         return Math.max(1.0F - (float)this.timeOut.getTime() / (float)this.delayUpdateMax, 0.0F);
      }

      public boolean containsLink(FriendsSLink.Link otherLink) {
         return this.name.equalsIgnoreCase(otherLink.name);
      }

      public boolean containsPlayer(EntityPlayer linkPlayer) {
         return linkPlayer != null && this.name.equalsIgnoreCase(linkPlayer.getName());
      }

      public boolean containsName(String name) {
         return this.name.equalsIgnoreCase(name);
      }

      public double getDistance() {
         return Minecraft.player.getDistance((double)this.xPos, (double)this.yPos, (double)this.zPos);
      }

      public boolean isChatCompetitor(SPacketChat packetChat) {
         if (this.lastInputString != null && packetChat != null && packetChat.getChatComponent() != null) {
            String message = ReplaceStrUtils.fixString(packetChat.getChatComponent().getFormattedText());
            return this.canReadAsData(message);
         } else {
            return false;
         }
      }
   }

   public class LinkObj {
      private final String text;
      private final ResourceLocation skin;
      private final float timePC10;

      public LinkObj(String text, ResourceLocation skin, float timePC10) {
         this.text = text;
         this.skin = skin;
         this.timePC10 = timePC10;
      }

      public String getText() {
         return this.text;
      }

      public ResourceLocation getSkin() {
         return this.skin;
      }

      public float getTimePC10() {
         return this.timePC10;
      }
   }
}
