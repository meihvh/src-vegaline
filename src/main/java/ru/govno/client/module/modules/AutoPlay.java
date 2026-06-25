package ru.govno.client.module.modules;

import java.util.Comparator;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventMoveKeys;
import ru.govno.client.module.Module;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;

public class AutoPlay extends Module {
   public static AutoPlay get;
   private EntityLivingBase target;
   private boolean botActive;
   private Vec3d walkVec;
   private boolean jumping;
   private AutoPlay.Demanour currentDemanour;

   public AutoPlay() {
      super("AutoPlay", 0, Module.Category.COMBAT);
      this.setDemand(0, 1);
      get = this;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   @Override
   public void onUpdate() {
      this.walkVec = null;
      this.jumping = false;
      if (!this.botActive && Minecraft.player.isSneaking()) {
         this.botActive = true;
         Client.msg("§f§lModules:§r §7[§lAutoPlay§r§7]: Бот активирован.", false);
      }

      if (!this.botActive || Minecraft.player != null && mc.world != null) {
         if (this.botActive) {
            this.updateDemanour();
            this.target = this.getNearestTarget();
            if (this.target != null) {
               float distance = Minecraft.player.getDistanceToEntity(this.target);
               if (distance < 12.0F && distance > 8.0F) {
                  if (!HitAura.get.isActived()) {
                     HitAura.get.toggle();
                  }
               } else if (distance > 15.0F && HitAura.get.isActived()) {
                  HitAura.get.toggle();
               }

               if (distance > 1.3F) {
                  this.walkVec = this.target.getPositionVector();
               }

               float predictValueMove = 2.0F;
               Vec3d predPos = Minecraft.player
                  .getPositionVector()
                  .addVector(
                     (Minecraft.player.posX - Minecraft.player.lastTickPosX) * (double)predictValueMove,
                     0.0,
                     (Minecraft.player.posZ - Minecraft.player.lastTickPosZ) * (double)predictValueMove
                  );
               AxisAlignedBB aabbMovePre = new AxisAlignedBB(
                  predPos.addVector((double)(-Minecraft.player.width) / 2.0, 0.0, (double)(-Minecraft.player.width) / 2.0),
                  predPos.addVector((double)Minecraft.player.width / 2.0, 1.0, (double)Minecraft.player.width / 2.0)
               );
               this.jumping = distance < 9.0F || !mc.world.getCollisionBoxes(Minecraft.player, aabbMovePre).isEmpty();
               mc.gameSettings.keyBindJump.pressed = this.jumping || Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
               if (this.walkVec != null && (HitAura.TARGET_ROTS != null || Minecraft.player.ticksExisted % 9 == 2)) {
                  boolean[] wasd = this.walkToCoordKeys(this.walkVec);
                  mc.gameSettings.keyBindForward.pressed = wasd[0] || Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
                  mc.gameSettings.keyBindRight.pressed = wasd[1] || Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
                  mc.gameSettings.keyBindBack.pressed = wasd[2] || Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
                  mc.gameSettings.keyBindLeft.pressed = wasd[3] || Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
               }

               if (HitAura.TARGET_ROTS != null && Minecraft.player.getHeldItemOffhand().getItem() == Items.SHIELD) {
                  mc.gameSettings.keyBindUseItem.pressed = true;
               }
            }
         }
      } else {
         this.botActive = false;
         this.target = null;
         this.walkVec = null;
         this.currentDemanour = null;
         Client.msg("§f§lModules:§r §7[§lAutoPlay§r§7]: Для работы бота тапни шифт.", false);
         mc.gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
         mc.gameSettings.keyBindRight.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
         mc.gameSettings.keyBindBack.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
         mc.gameSettings.keyBindLeft.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
         mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
         mc.gameSettings.keyBindUseItem.pressed = Mouse.isButtonDown(1) && mc.currentScreen == null;
      }
   }

   @EventTarget
   public void onMoveKeys(EventMoveKeys eventMoveKeys) {
      if (this.actived) {
      }
   }

   private boolean[] walkToCoordKeys(Vec3d coord) {
      if (coord == null) {
         return new boolean[4];
      } else {
         float statYawToCoord = RotationUtil.getNeededFacing(coord, false, Minecraft.player, false)[0];
         float keysYaw = MathUtils.wrapAngleTo180_float(statYawToCoord - Minecraft.player.rotationYaw);
         boolean[] WASD = new boolean[4];
         if (RotationUtil.getAngleDifference(0.0F, keysYaw) <= 60.0F) {
            WASD[0] = true;
         }

         if (RotationUtil.getAngleDifference(90.0F, keysYaw) <= 60.0F) {
            WASD[1] = true;
         }

         if (RotationUtil.getAngleDifference(180.0F, keysYaw) <= 60.0F) {
            WASD[2] = true;
         }

         if (RotationUtil.getAngleDifference(270.0F, keysYaw) <= 60.0F) {
            WASD[3] = true;
         }

         return WASD;
      }
   }

   private EntityLivingBase getNearestTarget() {
      if (HitAura.TARGET_ROTS != null) {
         return HitAura.TARGET_ROTS;
      } else {
         return mc.world != null
            ? mc.world
               .getLoadedEntityList()
               .stream()
               .map(Entity::getOtherPlayerOf)
               .filter(Objects::nonNull)
               .filter(player -> player.isEntityAlive() && !Client.friendManager.isFriend(player.getName()) && !Client.summit(player))
               .sorted(Comparator.comparing(obj -> -obj.getDistanceToEntity(Minecraft.player)))
               .findAny()
               .orElse(null)
            : null;
      }
   }

   private void updateDemanour() {
      this.currentDemanour = AutoPlay.Demanour.ATTACK;
      if (this.target != null && this.target.getHealth() + 4.0F < Minecraft.player.getHealth()) {
         this.currentDemanour = AutoPlay.Demanour.TROLL_ATTACK;
      }

      if (Minecraft.player.getHealth() < 7.0F) {
         this.currentDemanour = AutoPlay.Demanour.RUN;
      }
   }

   @Override
   public void onToggled(boolean actived) {
      this.target = null;
      this.botActive = false;
      this.walkVec = null;
      this.jumping = false;
      super.onToggled(actived);
   }

   private static enum Demanour {
      ATTACK,
      RUN,
      TROLL_ATTACK;
   }
}
