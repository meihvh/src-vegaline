package ru.govno.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBow;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Mouse;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;

public class BowSpam extends Module {
   FloatSettings Charge;
   BoolSettings OnlyTightly;

   public BowSpam() {
      super("BowSpam", 0, Module.Category.COMBAT);
      this.settings.add(this.Charge = new FloatSettings("Charge", 4.0F, 20.0F, 2.0F, this));
      this.settings.add(this.OnlyTightly = new BoolSettings("OnlyTightly", true, this));
      this.setDemand(0, 0);
   }

   @Override
   public void onUpdateMovement() {
      if (Minecraft.player.isBowing()
         && Mouse.isButtonDown(1)
         && (!this.OnlyTightly.getBool() || mc.pointedEntity != null && (double)Minecraft.player.getDistanceToEntity(mc.pointedEntity) <= 2.9)
         && (double)Minecraft.player.getItemInUseMaxCount() * mc.timer.speed > (double)this.Charge.getFloat()) {
         EnumHand active = Minecraft.player.getActiveHand();
         mc.playerController.onStoppedUsingItem(Minecraft.player);
         Minecraft.player
            .connection
            .sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Minecraft.player.getHorizontalFacing()));
         if (active != null) {
            Minecraft.player.connection.sendPacket(new CPacketPlayerTryUseItem(active));
            if (Minecraft.player.getHeldItemMainhand().getItem() instanceof ItemBow) {
               Minecraft.player.setActiveHand(EnumHand.MAIN_HAND);
            } else if (Minecraft.player.getHeldItemOffhand().getItem() instanceof ItemBow) {
               Minecraft.player.setActiveHand(EnumHand.OFF_HAND);
            }
         }
      }
   }
}
