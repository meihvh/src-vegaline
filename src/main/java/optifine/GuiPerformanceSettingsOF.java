package optifine;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Render.RenderUtils;

public class GuiPerformanceSettingsOF extends GuiScreen {
   private final GuiScreen prevScreen;
   protected String title;
   private final GameSettings settings;
   private static final GameSettings.Options[] enumOptions = new GameSettings.Options[]{
      GameSettings.Options.SMOOTH_FPS,
      GameSettings.Options.SMOOTH_WORLD,
      GameSettings.Options.FAST_RENDER,
      GameSettings.Options.FAST_MATH,
      GameSettings.Options.CHUNK_UPDATES,
      GameSettings.Options.CHUNK_UPDATES_DYNAMIC,
      GameSettings.Options.LAZY_CHUNK_LOADING
   };
   private final TooltipManager tooltipManager = new TooltipManager(this);

   public GuiPerformanceSettingsOF(GuiScreen p_i52_1_, GameSettings p_i52_2_) {
      this.prevScreen = p_i52_1_;
      this.settings = p_i52_2_;
   }

   @Override
   public void initGui() {
      this.title = I18n.format("of.options.performanceTitle");
      this.buttonList.clear();

      for (int i = 0; i < enumOptions.length; i++) {
         GameSettings.Options gamesettings$options = enumOptions[i];
         int j = width / 2 - 155 + i % 2 * 160;
         int k = height / 6 + 21 * (i / 2) - 12;
         if (!gamesettings$options.getEnumFloat()) {
            this.buttonList
               .add(
                  new GuiOptionButtonOF(gamesettings$options.returnEnumOrdinal(), j, k, gamesettings$options, this.settings.getKeyBinding(gamesettings$options))
               );
         } else {
            this.buttonList.add(new GuiOptionSliderOF(gamesettings$options.returnEnumOrdinal(), j, k, gamesettings$options));
         }
      }

      this.buttonList.add(new GuiButton(200, width / 2 - 100, height / 6 + 168 + 11, I18n.format("gui.done")));
   }

   @Override
   public void actionPerformed(GuiButton button) {
      if (button.enabled) {
         if (button.id < 200 && button instanceof GuiOptionButton) {
            this.settings.setOptionValue(((GuiOptionButton)button).returnEnumOptions(), 1);
            button.displayString = this.settings.getKeyBinding(GameSettings.Options.getEnumOptions(button.id));
         }

         if (button.id == 200) {
            this.mc.gameSettings.saveOptions();
            this.mc.displayGuiScreen(this.prevScreen);
         }
      }
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      if (!Panic.stop
         && this.prevScreen instanceof GuiVideoSettings vid
         && vid.parentGuiScreen instanceof GuiOptions op
         && op.lastScreen instanceof GuiMainMenu) {
         RenderUtils.drawScreenShaderBackground(new ScaledResolution(this.mc), mouseX, mouseY);
      }

      this.drawCenteredString(this.fontRendererObj, this.title, width / 2, 15, 16777215);
      super.drawScreen(mouseX, mouseY, partialTicks);
      this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttonList);
   }
}
