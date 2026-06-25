package ru.govno.client.module.modules;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Command.impl.Panic;

public class AntiBot extends Module {
   public static AntiBot get;
   BoolSettings RemoveBots;
   ModeSettings Modes;
   public BoolSettings RemoveLagEnts;
   private int tempFrameEntityNoNamed;
   private int tempEmptySigns;

   public AntiBot() {
      super("AntiBot", 0, Module.Category.COMBAT);
      this.settings.add(this.RemoveBots = new BoolSettings("RemoveBots", false, this));
      this.settings
         .add(this.Modes = new ModeSettings("Modes", "Matrix", this, new String[]{"Matrix", "Matrix2", "WellMore", "Buzz"}, () -> this.RemoveBots.getBool()));
      this.settings.add(this.RemoveLagEnts = new BoolSettings("RemoveLagEnts", false, this));
      get = this;
      this.setDemand(0, 2);
   }

   @Override
   public String getDisplayName() {
      return this.getDisplayByMode(this.Modes.currentMode);
   }

   @Override
   public void onUpdate() {
      String mode = this.Modes.currentMode;
      if (mc.world != null && Minecraft.player != null) {
         if (this.RemoveBots.getBool()) {
            try {
               mc.world
                  .getLoadedEntityList()
                  .stream()
                  .map(Entity::getLivingBaseOf)
                  .filter(Objects::nonNull)
                  .filter(base -> this.entityIsBot(mode, base))
                  .forEach(bot -> this.processingEntity(mode, bot, this.actived));
            } catch (Exception var5) {
               var5.printStackTrace();
               System.out.println(this.name + " module error!");
            }
         }

         if (this.RemoveLagEnts.getBool()) {
            try {
               List<Entity> ents = mc.world.getLoadedEntityList().stream().filter(Objects::nonNull).toList();
               List<TileEntity> tiles = mc.world.getLoadedTileEntityList().stream().filter(Objects::nonNull).toList();
               ents.stream().filter(ent -> this.entityIsLag(ent, true)).forEach(ent -> this.processingEntity(null, ent, this.actived));
               tiles.stream().filter(tile -> this.entityIsLag(tile, true)).forEach(tile -> this.processingEntity(null, tile, this.actived));
               ents.stream().filter(ent -> this.entityIsLag(ent, false)).forEach(ent -> this.processingEntity(null, ent, this.actived));
               tiles.stream().filter(tile -> this.entityIsLag(tile, false)).forEach(tile -> this.processingEntity(null, tile, this.actived));
               this.resetMatchesLag();
            } catch (Exception var4) {
               var4.printStackTrace();
               System.out.println(this.name + " module error!");
            }
         }
      }
   }

   private int capFrameEntityNoNamed() {
      return 80;
   }

   private int capEmptySigns() {
      return 20;
   }

   private void updateMatchesLag(Entity entity) {
      if (entity instanceof EntityItemFrame frame && this.tempFrameEntityNoNamed < this.capFrameEntityNoNamed()) {
         ITextComponent textComponent = frame.getDisplayName();
         if (textComponent != null) {
            String name = textComponent.getFormattedText();
            if (name.contains("entity.ItemFrame.name")) {
               this.tempFrameEntityNoNamed++;
            }
         }
      }
   }

   private void updateMatchesLag(TileEntity tile) {
      if (tile instanceof TileEntitySign sign && (sign.signText[0] == null || sign.signText[0].getUnformattedText().isEmpty())) {
         this.tempEmptySigns++;
      }
   }

   private boolean isLagMatch(Entity entity) {
      if (entity instanceof EntityOtherPlayerMP mp) {
         AxisAlignedBB aabb = mp.getEntityBoundingBox();
         if (aabb != null && Math.abs(aabb.maxY - aabb.minY) <= 0.25) {
            return true;
         }
      }

      return entity instanceof EntityItemFrame && this.tempFrameEntityNoNamed > this.capFrameEntityNoNamed();
   }

   private boolean isLagMatch(TileEntity tile) {
      return tile.isInvalid() ? true : tile instanceof TileEntitySign && this.tempEmptySigns > this.capEmptySigns();
   }

   private void resetMatchesLag() {
      this.tempFrameEntityNoNamed = 0;
      this.tempEmptySigns = 0;
   }

   public static boolean renderGlobalEntityInsert(Entity entity) {
      return entity != null && entity instanceof EntityOtherPlayerMP && !Panic.stop && get != null && get.isActived() && get.RemoveLagEnts.getBool()
         ? mc.world != null && mc.world.getEntityByID(entity.getEntityId()) == null
         : false;
   }

   private boolean entityIsLag(Entity entity, boolean isUpdate) {
      if (isUpdate) {
         this.updateMatchesLag(entity);
         return false;
      } else {
         if (entity instanceof EntityFallingBlock block && (block.motionX != 0.0 || block.motionY > 0.0 || block.motionZ != 0.0)) {
            return true;
         }

         if (entity.getClass() == null) {
            return true;
         } else {
            if (entity instanceof EntityArmorStand stand
               && (
                  NoRender.get.isActived() && NoRender.get.Holograms.getBool() && stand.hasCustomName()
                     || Math.abs(stand.posX) <= 16.0
                     || Math.abs(stand.posZ) <= 16.0
                     || stand.posY > 255.0
                     || stand.posY < 0.0
               )) {
               return true;
            }

            return this.isLagMatch(entity);
         }
      }
   }

   private boolean entityIsLag(TileEntity tile, boolean isUpdate) {
      if (tile == null) {
         return false;
      } else if (isUpdate) {
         this.updateMatchesLag(tile);
         return false;
      } else {
         return this.isLagMatch(tile);
      }
   }

   private boolean entityIsBot(String mode, Entity entity) {
      if (entity == null && (entity.getDisplayName() == null || !entity.getDisplayName().getFormattedText().contains("entity.ItemFrame.name"))) {
         if (entity.getEntityId() != 462462998 && entity.getEntityId() != 462462999 && !entity.getName().toLowerCase().contains("npc")) {
            if (mode.equalsIgnoreCase("Matrix")) {
               return entity instanceof EntityOtherPlayerMP
                  && Minecraft.player.getDistanceToEntity(entity) <= 25.0F
                  && entity.noClip
                  && entity.getCustomNameTag().isEmpty()
                  && ((EntityOtherPlayerMP)entity).isSwingInProgress
                  && entity != FreeCam.fakePlayer;
            } else if (mode.equalsIgnoreCase("Matrix2")) {
               if (!entity.getUniqueID().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + entity.getName()).getBytes(StandardCharsets.UTF_8)))
                  && entity instanceof EntityOtherPlayerMP MP
                  && !MP.onGround) {
                  return true;
               }

               return false;
            } else if (mode.equalsIgnoreCase("Wellmore")) {
               return entity instanceof EntityOtherPlayerMP && ((EntityOtherPlayerMP)entity).inventory.armorInventory.isEmpty();
            } else if (!mode.equalsIgnoreCase("Buzz")) {
               return false;
            } else {
               ArrayList<EntityZombie> bi4ariki = new ArrayList<>();
               ArrayList<EntityOtherPlayerMP> normPacani = new ArrayList<>();
               ArrayList<EntityZombie> bots = new ArrayList<>();

               for (Entity entities : mc.world.getLoadedEntityList()) {
                  if (entities != null && entities instanceof EntityZombie zombie && zombie.isInvisible()) {
                     bi4ariki.add(zombie);
                  }

                  if (entities != null && entities instanceof EntityOtherPlayerMP entityOtherPlayerMP) {
                     normPacani.add(entityOtherPlayerMP);
                  }
               }

               for (EntityOtherPlayerMP bro : normPacani) {
                  for (EntityZombie bi4 : bi4ariki) {
                     if ((double)bi4.getDistanceToEntity(bro) < 2.2 && Minecraft.player.getDistanceToEntity(bi4) < 4.0F && bi4.ticksExisted < 400) {
                        bots.add(bi4);
                     }
                  }
               }

               EntityZombie bot = bi4ariki.stream().findAny().orElse(null);
               return bot != null && entity == bot;
            }
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   private void processingEntity(String mode, Entity entity, boolean isActive) {
      if (isActive) {
         if (mode != null && mode.equalsIgnoreCase("Buzz")) {
            mc.getConnection().preSendPacket(new CPacketPlayer(Minecraft.player.onGround));
            mc.playerController.attackEntity(Minecraft.player, entity);
            Minecraft.player.swingArm(EnumHand.MAIN_HAND);
         }

         mc.world.removeEntityFromWorld(entity.getEntityId());
      }
   }

   private void processingEntity(String mode, TileEntity tile, boolean isActive) {
      if (isActive) {
         mc.world.removeTileEntity(tile.getPos());
      }
   }
}
