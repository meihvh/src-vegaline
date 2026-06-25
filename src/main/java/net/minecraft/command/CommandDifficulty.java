package net.minecraft.command;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumDifficulty;

public class CommandDifficulty extends CommandBase {
   @Override
   public String getCommandName() {
      return "difficulty";
   }

   @Override
   public int getRequiredPermissionLevel() {
      return 2;
   }

   @Override
   public String getCommandUsage(ICommandSender sender) {
      return "commands.difficulty.usage";
   }

   @Override
   public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
      if (args.length <= 0) {
         throw new WrongUsageException("commands.difficulty.usage");
      } else {
         EnumDifficulty enumdifficulty = this.getDifficultyFromCommand(args[0]);
         server.setDifficultyForAllWorlds(enumdifficulty);
         notifyCommandListener(
            sender, this, "commands.difficulty.success", new Object[]{new TextComponentTranslation(enumdifficulty.getDifficultyResourceKey())}
         );
      }
   }

   protected EnumDifficulty getDifficultyFromCommand(String difficultyString) throws CommandException, NumberInvalidException {
      if ("peaceful".equalsIgnoreCase(difficultyString) || "p".equalsIgnoreCase(difficultyString)) {
         return EnumDifficulty.PEACEFUL;
      } else if ("easy".equalsIgnoreCase(difficultyString) || "e".equalsIgnoreCase(difficultyString)) {
         return EnumDifficulty.EASY;
      } else if (!"normal".equalsIgnoreCase(difficultyString) && !"n".equalsIgnoreCase(difficultyString)) {
         return !"hard".equalsIgnoreCase(difficultyString) && !"h".equalsIgnoreCase(difficultyString)
            ? EnumDifficulty.getDifficultyEnum(parseInt(difficultyString, 0, 3))
            : EnumDifficulty.HARD;
      } else {
         return EnumDifficulty.NORMAL;
      }
   }

   @Override
   public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
      return args.length == 1 ? getListOfStringsMatchingLastWord(args, new String[]{"peaceful", "easy", "normal", "hard"}) : Collections.emptyList();
   }
}
