package net.minecraft.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import ru.govno.client.event.events.EventMoveKeys;
import ru.govno.client.event.events.EventMovementInput;
import ru.govno.client.event.events.EventSetSneak;
import ru.govno.client.event.events.EventSlowSneak;
import ru.govno.client.module.modules.MoveHelper;
import ru.govno.client.utils.UControllers.DualsenseController;
import ru.govno.client.utils.UControllers.DualsenseSettings;

public class MovementInputFromOptions extends MovementInput {
   private final GameSettings gameSettings;

   public MovementInputFromOptions(GameSettings gameSettingsIn) {
      this.gameSettings = gameSettingsIn;
   }

   @Override
   public void updatePlayerMoveState() {
      moveStrafe = 0.0F;
      moveForward = 0.0F;
      boolean forward = this.gameSettings.keyBindForward.isKeyDown();
      boolean back = this.gameSettings.keyBindBack.isKeyDown();
      boolean left = this.gameSettings.keyBindLeft.isKeyDown();
      boolean right = this.gameSettings.keyBindRight.isKeyDown();
      EventMoveKeys eventPress = new EventMoveKeys(forward, back, left, right);
      eventPress.call();
      if (eventPress.isCancelled()) {
         forward = false;
         back = false;
         left = false;
         right = false;
      } else {
         forward = eventPress.isForwardKeyDown();
         back = eventPress.isBackKeyDown();
         left = eventPress.isLeftKeyDown();
         right = eventPress.isRightKeyDown();
      }

      if (forward) {
         moveForward++;
         this.forwardKeyDown = true;
      } else {
         this.forwardKeyDown = false;
      }

      if (back) {
         moveForward--;
         this.backKeyDown = true;
      } else {
         this.backKeyDown = false;
      }

      if (left) {
         moveStrafe++;
         this.leftKeyDown = true;
      } else {
         this.leftKeyDown = false;
      }

      if (right) {
         moveStrafe--;
         this.rightKeyDown = true;
      } else {
         this.rightKeyDown = false;
      }

      this.jump = this.gameSettings.keyBindJump.isKeyDown()
         || DualsenseController.CROSS_PRESSED
         || DualsenseSettings.moveStickMode().contains("Jump")
            && DualsenseController.SenseAxis.LEFT_STICK.getAxisMove(DualsenseController.SenseAxis.LEFT_STICK.getInputValues()) > 0.95F;
      EventSetSneak eventSetSneak = new EventSetSneak(this.sneak);
      eventSetSneak.call();
      this.sneak = this.gameSettings.keyBindSneak.isKeyDown()
         || Minecraft.player.hasNewVersionMoves && Minecraft.player.isNewSneak
         || MoveHelper.instance.isPreCrouchSpoof()
         || eventSetSneak.isCancelled()
         || eventSetSneak.isSneaking();
      EventSlowSneak sneakEvent = new EventSlowSneak(0.3);
      sneakEvent.call();
      if (this.sneak && !sneakEvent.isCancelled()) {
         moveStrafe = (float)((double)moveStrafe * sneakEvent.getSlowFactor());
         moveForward = (float)((double)moveForward * sneakEvent.getSlowFactor());
      }

      EventMovementInput event = new EventMovementInput(moveForward, moveStrafe, this.jump, this.sneak && !sneakEvent.isCancelled(), sneakEvent.getSlowFactor());
      event.call();
      moveForward = event.getForward();
      moveStrafe = event.getStrafe();
      this.jump = event.isJump();
   }
}
