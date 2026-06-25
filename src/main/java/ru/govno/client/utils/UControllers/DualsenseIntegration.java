package ru.govno.client.utils.UControllers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import ru.govno.client.Client;
import ru.govno.client.cfg.GuiConfig;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.event.EventManager;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventInput;
import ru.govno.client.event.events.EventMoveKeys;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventRender2D;
import ru.govno.client.module.modules.ClickGui;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.module.modules.InvWalk;
import ru.govno.client.module.modules.Notifications;
import ru.govno.client.utils.DisplayCheck;
import ru.govno.client.utils.Math.MathUtils;

public class DualsenseIntegration {
   private final Minecraft mc = Minecraft.getMinecraft();
   private boolean tempCheckActive;
   private boolean tempCheckClickMouseLeft;
   private boolean tempCheckClickMouseRight;
   private boolean tempCheckClickMouseLeft2;
   private boolean tempCheckClickMouseRight2;
   private boolean tempCheckMovementInput;
   private boolean tempCheckJumpOrSneak;
   private boolean tempCheckClickE;
   private boolean tempCheckClickESC;
   private boolean tempCheckPS;
   private boolean tempScrollLeft;
   private boolean tempScrollRight;
   private boolean tempFPressed;
   private boolean tempF5Pressed;

   public void updateEventsState() {
      DualsenseController.findAndUpdateDualsense();
      if (DualsenseController.CONTROLLER_LOADED && !this.tempCheckActive) {
         EventManager.register(this);
         if (ClientTune.get != null) {
            ClientTune.get.playDualsenseSong("connect");
         }

         Notifications.Notify.spawnNotify("Dualsense подключен!", Notifications.type.ENABLE);
         this.tempCheckActive = true;
      } else if (!DualsenseController.CONTROLLER_LOADED && this.tempCheckActive) {
         EventManager.unregister(this);
         if (ClientTune.get != null) {
            ClientTune.get.playDualsenseSong("disconnect");
         }

         Notifications.Notify.spawnNotify("Dualsense отключен!", Notifications.type.DISABLE);
         this.tempCheckActive = false;
      }
   }

   @EventTarget
   public void onMovementInput(EventInput event) {
      DualsenseController.onEventKeyInput(event);
   }

   @EventTarget
   public void onMovementInput(EventMoveKeys event) {
      if (DualsenseController.CONTROLLER_USED && this.canWalkInput()) {
         boolean[] wasd = DualsenseController.getWasdInputFromSenseAxis(true);
         if (DualsenseController.SenseAxis.LEFT_STICK.hasAxisMove()) {
            event.setForwardKeyDown(wasd[0]);
            event.setBackKeyDown(wasd[2]);
            event.setLeftKeyDown(wasd[1]);
            event.setRightKeyDown(wasd[3]);
            this.mc.gameSettings.keyBindForward.pressed = event.isForwardKeyDown();
            this.mc.gameSettings.keyBindBack.pressed = event.isBackKeyDown();
            this.mc.gameSettings.keyBindLeft.pressed = event.isLeftKeyDown();
            this.mc.gameSettings.keyBindRight.pressed = event.isRightKeyDown();
            this.tempCheckMovementInput = true;
         } else if (this.tempCheckMovementInput) {
            this.mc.gameSettings.keyBindForward.pressed = false;
            this.mc.gameSettings.keyBindBack.pressed = false;
            this.mc.gameSettings.keyBindLeft.pressed = false;
            this.mc.gameSettings.keyBindRight.pressed = false;
            this.tempCheckMovementInput = false;
         }
      }
   }

   private void guiClick(GuiScreen screen, int mouse, boolean press) {
      if (screen != null) {
         screen.handleClickDualsense(press, mouse);
      }
   }

   @EventTarget
   public void onUpdate(EventPlayerMotionUpdate event) {
      if (DualsenseController.CONTROLLER_USED) {
         if (this.canRotateCamera()) {
            float[] inputValues = DualsenseController.SenseAxis.RIGHT_STICK.getInputValues();
            float speedRot = (float)DualsenseController.SenseAxis.RIGHT_STICK.getAxisMove(inputValues);
            speedRot = 1.0F - (float)MathUtils.easeOutCirc((double)(1.0F - Math.min(speedRot, 1.0F)));
            speedRot *= 22.0F;
            float radian = MathHelper.toRadians((float)DualsenseController.SenseAxis.RIGHT_STICK.getsAxisDir(inputValues, 0.0F));
            float sin = MathHelper.sin(radian);
            float cos = MathHelper.cos(radian);
            Minecraft.player.rotationYaw += sin * speedRot;
            Minecraft.player.rotationPitch += cos * speedRot / (float) Math.PI * 2.0F;
            Minecraft.player.rotationPitch = MathUtils.clamp(Minecraft.player.rotationPitch, -90.0F, 90.0F);
         }

         if (this.canInteractInput()) {
            DualsenseController.mc.gameSettings.keyBindAttack.pressed = DualsenseController.R2_PRESSED;
            if (DualsenseController.R2_PRESSED) {
               if (!this.tempCheckClickMouseLeft2) {
                  this.mc.clickMouse();
                  this.tempCheckClickMouseLeft2 = true;
               }
            } else if (this.tempCheckClickMouseLeft2) {
               this.tempCheckClickMouseLeft2 = false;
            }

            DualsenseController.mc.gameSettings.keyBindUseItem.pressed = DualsenseController.L2_PRESSED;
            if (DualsenseController.L2_PRESSED) {
               if (!this.tempCheckClickMouseRight2) {
                  this.mc.rightClickMouse();
                  this.tempCheckClickMouseRight2 = true;
               }
            } else if (this.tempCheckClickMouseRight2) {
               this.tempCheckClickMouseRight2 = false;
            }
         }

         if (this.canWalkInput()) {
            this.mc.gameSettings.keyBindJump.pressed = DualsenseController.CROSS_PRESSED
               || (this.mc.currentScreen == null || InvWalk.get.isActived()) && Keyboard.isKeyDown(this.mc.gameSettings.keyBindJump.getKeyCode());
            this.mc.gameSettings.keyBindSneak.pressed = DualsenseController.CIRCLE_PRESSED
               || (this.mc.currentScreen == null || InvWalk.get.isActived()) && Keyboard.isKeyDown(this.mc.gameSettings.keyBindSneak.getKeyCode());
            this.tempCheckJumpOrSneak = true;
         } else if (this.tempCheckJumpOrSneak) {
            this.mc.gameSettings.keyBindJump.pressed = false;
            this.mc.gameSettings.keyBindSneak.pressed = false;
            this.tempCheckJumpOrSneak = false;
         }

         if (this.canRotateCamera() && Minecraft.player != null && Minecraft.player.inventory != null && this.mc.currentScreen == null) {
            if (DualsenseController.LEFT_PRESSED) {
               if (!this.tempScrollLeft) {
                  Minecraft.player.inventory.currentItem = (Minecraft.player.inventory.currentItem + 8) % 9;
                  this.tempScrollLeft = true;
               }
            } else {
               this.tempScrollLeft = false;
            }

            if (DualsenseController.RIGHT_PRESSED) {
               if (!this.tempScrollRight) {
                  Minecraft.player.inventory.currentItem = (Minecraft.player.inventory.currentItem + 1) % 9;
                  this.tempScrollRight = true;
               }
            } else {
               this.tempScrollRight = false;
            }

            if (DualsenseController.L1_PRESSED) {
               if (!this.tempFPressed) {
                  this.mc.playerController.syncCurrentPlayItem();
                  this.mc.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.SWAP_HELD_ITEMS, BlockPos.ORIGIN, EnumFacing.DOWN));
                  this.tempFPressed = true;
               }
            } else if (this.tempFPressed) {
               this.tempFPressed = false;
            }

            if (DualsenseController.R3_PRESSED) {
               if (!this.tempF5Pressed) {
                  this.mc.gameSettings.keyBindTogglePerspective.pressed = true;
                  this.mc.gameSettings.thirdPersonView++;
                  this.mc.gameSettings.thirdPersonView %= 3;
                  this.tempF5Pressed = true;
               }
            } else {
               this.mc.gameSettings.keyBindTogglePerspective.pressed = false;
               this.tempF5Pressed = false;
            }
         }
      }
   }

   @EventTarget
   public void onRender2DEvent(EventRender2D event) {
      DualsenseController.onEventRender2D(event);
      if (DualsenseController.CONTROLLER_USED) {
         if (this.canMoveMouseCursor()) {
            int mouseX = Mouse.getX();
            int mouseY = Mouse.getY();
            float[] inputValues;
            if (DualsenseController.SenseAxis.RIGHT_STICK.hasAxisMove(inputValues = DualsenseController.SenseAxis.RIGHT_STICK.getInputValues())) {
               float radian = MathHelper.toRadians((float)DualsenseController.SenseAxis.RIGHT_STICK.getsAxisDir(inputValues, 0.0F));
               float inputPC = (float)DualsenseController.SenseAxis.RIGHT_STICK.getAxisMove(inputValues);
               float dHWFactor = Math.min((float)(this.mc.displayWidth * this.mc.displayHeight) / 9830400.0F * 1.2F, 1.0F);
               float speed = (this.mc.currentScreen != null ? 3.333333F : 10.0F)
                  * dHWFactor
                  * (float)Minecraft.frameTime
                  * (float)MathUtils.easeInCircle((double)Math.min(inputPC, 1.0F));
               float sin = -MathHelper.sin(radian) * (float)(this.mc.currentScreen != null ? -1 : 1);
               float cos = -MathHelper.cos(radian);
               Mouse.setCursorPosition((int)((float)mouseX + sin * speed + 0.5F), (int)((float)mouseY + cos * speed + 0.5F));
            }
         }

         if (this.canClickMouseScreenInput()) {
            if (DualsenseController.R2_PRESSED) {
               if (!this.tempCheckClickMouseLeft) {
                  if (this.mc.currentScreen != null) {
                     try {
                        this.guiClick(this.mc.currentScreen, 0, true);
                     } catch (Exception var14) {
                        var14.fillInStackTrace();
                     }
                  } else {
                     this.mc.clickMouse();
                  }

                  this.tempCheckClickMouseLeft = true;
               }
            } else if (this.tempCheckClickMouseLeft) {
               try {
                  this.guiClick(this.mc.currentScreen, 0, false);
               } catch (Exception var13) {
                  var13.fillInStackTrace();
               }

               this.tempCheckClickMouseLeft = false;
            }

            if (DualsenseController.L2_PRESSED) {
               if (!this.tempCheckClickMouseRight) {
                  if (this.mc.currentScreen != null) {
                     try {
                        this.guiClick(this.mc.currentScreen, 1, true);
                     } catch (Exception var12) {
                        var12.fillInStackTrace();
                     }
                  } else {
                     this.mc.rightClickMouse();
                  }

                  this.tempCheckClickMouseRight = true;
               }
            } else if (this.tempCheckClickMouseRight) {
               try {
                  this.guiClick(this.mc.currentScreen, 1, false);
               } catch (Exception var11) {
                  var11.fillInStackTrace();
               }

               this.tempCheckClickMouseRight = false;
            }
         }

         if (this.canBindContainerInput()) {
            if (DualsenseController.TRIANGLE_PRESSED) {
               this.mc.gameSettings.keyBindInventory.pressed = true;
               if (!this.tempCheckClickE) {
                  if (this.mc.currentScreen == null) {
                     this.mc.displayGuiScreen(new GuiInventory(Minecraft.player));
                  } else if (this.mc.currentScreen instanceof GuiContainer) {
                     this.mc.displayGuiScreen(null);
                  }

                  this.tempCheckClickE = true;
               }
            } else if (this.tempCheckClickE) {
               this.mc.gameSettings.keyBindInventory.pressed = false;
               this.tempCheckClickE = false;
            }
         } else if (this.tempCheckClickE) {
            this.tempCheckClickE = false;
         }

         if (this.canEscapeInput()) {
            if (!DualsenseController.OPTIONS_PRESSED && !DualsenseController.CIRCLE_PRESSED) {
               if (this.tempCheckClickESC) {
                  this.tempCheckClickESC = false;
               }
            } else {
               if (!this.tempCheckClickESC) {
                  if (this.mc.currentScreen != null) {
                     if (this.mc.currentScreen instanceof ClickGuiScreen) {
                        ClickGui.instance.toggle(false);
                     } else if (this.mc.currentScreen instanceof GuiConfig guiConfig) {
                        guiConfig.colose = true;
                        GuiConfig.cfgScale.to = 1.0F;
                     } else {
                        this.mc.displayGuiScreen(null);
                     }
                  } else if (this.mc.currentScreen == null) {
                     if (!DualsenseController.CIRCLE_PRESSED) {
                        this.mc.displayGuiScreen(new GuiIngameMenu());
                     }
                  } else {
                     this.mc.displayGuiScreen(null);
                  }
               }

               this.tempCheckClickESC = true;
            }
         } else if (this.tempCheckClickESC) {
            this.tempCheckClickESC = false;
         }

         if (DualsenseController.PS_PRESSED) {
            if (!this.tempCheckPS) {
               if (this.mc.currentScreen == null || this.mc.currentScreen instanceof ClickGuiScreen) {
                  if (!ClickGui.instance.isActived() && this.mc.currentScreen == null) {
                     ClickGui.instance.toggle(true);
                     Client.clickGuiScreen.initGui();
                     this.mc.displayGuiScreen(Client.clickGuiScreen);
                  } else if (DualsenseController.mc.currentScreen instanceof ClickGuiScreen) {
                     ClickGui.instance.toggle();
                  }
               }

               this.tempCheckPS = true;
            }
         } else if (this.tempCheckPS) {
            this.tempCheckPS = false;
         }
      }
   }

   private boolean hasCurrentDisplay() {
      return DisplayCheck.isVisible();
   }

   private boolean canWalkInput() {
      return (this.mc.currentScreen == null || InvWalk.get.isActived()) && this.hasCurrentDisplay();
   }

   private boolean canInteractInput() {
      return this.mc.currentScreen == null && this.hasCurrentDisplay();
   }

   private boolean canRotateCamera() {
      return this.mc.currentScreen == null && Minecraft.player != null && this.hasCurrentDisplay();
   }

   private boolean canMoveMouseCursor() {
      return this.mc.currentScreen != null && this.hasCurrentDisplay();
   }

   private boolean canClickMouseInput() {
      return this.mc.currentScreen == null && this.hasCurrentDisplay();
   }

   private boolean canClickMouseScreenInput() {
      return this.mc.currentScreen != null && this.hasCurrentDisplay();
   }

   private boolean canBindContainerInput() {
      return (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiContainer) && this.hasCurrentDisplay() && Minecraft.player != null;
   }

   private boolean canEscapeInput() {
      return this.hasCurrentDisplay();
   }
}
