package ru.govno.client.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;

public class PushAttack extends Module {
   public static PushAttack get;
   public BoolSettings OnlyUsingSwing;
   public BoolSettings NCPBypass;

   public PushAttack() {
      super("PushAttack", 0, Module.Category.COMBAT);
      this.settings.add(this.OnlyUsingSwing = new BoolSettings("OnlyUsingSwing", false, this));
      this.settings.add(this.NCPBypass = new BoolSettings("NCPBypass", false, this, () -> !this.OnlyUsingSwing.getBool()));
      this.setDemand(0, 0);
      get = this;
   }

   @Override
   public void onMouseClick(int mouseButton) {
      if (mouseButton == 0
         && Minecraft.player != null
         && Minecraft.player.isHandActive()
         && mc.currentScreen == null
         && (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK)) {
         if (this.OnlyUsingSwing.getBool()) {
            Minecraft.player.swingArm();
         } else {
            mc.clickMouse();
         }
      }
   }

   @EventTarget
   public void onPacket(EventSendPacket event) {
      if (this.isActived()
         && event.getPacket() instanceof CPacketUseEntity useEntity
         && !this.OnlyUsingSwing.getBool()
         && useEntity.getEntityFromWorld(mc.world) != null
         && useEntity.getAction() == CPacketUseEntity.Action.ATTACK
         && this.NCPBypass.getBool()) {
         Minecraft.getMinecraft()
            .getConnection()
            .preSendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, new BlockPos(-1, -1, -1), EnumFacing.DOWN));
      }
   }
}
