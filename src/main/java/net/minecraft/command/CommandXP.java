package net.minecraft.command;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class CommandXP extends CommandBase {
   @Override
   public String getCommandName() {
      return "xp";
   }

   @Override
   public int getRequiredPermissionLevel() {
      return 2;
   }

   @Override
   public String getCommandUsage(ICommandSender sender) {
      return "commands.xp.usage";
   }

   @Override
   public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
      if (args.length <= 0) {
         throw new WrongUsageException("commands.xp.usage");
      } else {
         String s = args[0];
         boolean flag = s.endsWith("l") || s.endsWith("L");
         if (flag && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
         }

         int i = parseInt(s);
         boolean flag1 = i < 0;
         if (flag1) {
            i *= -1;
         }

         EntityPlayer entityplayer = args.length > 1 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
         if (flag) {
            sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, entityplayer.experienceLevel);
            if (flag1) {
               entityplayer.addExperienceLevel(-i);
               notifyCommandListener(sender, this, "commands.xp.success.negative.levels", new Object[]{i, entityplayer.getName()});
            } else {
               entityplayer.addExperienceLevel(i);
               notifyCommandListener(sender, this, "commands.xp.success.levels", new Object[]{i, entityplayer.getName()});
            }
         } else {
            sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, entityplayer.experienceTotal);
            if (flag1) {
               throw new CommandException("commands.xp.failure.widthdrawXp");
            }

            entityplayer.addExperience(i);
            notifyCommandListener(sender, this, "commands.xp.success", new Object[]{i, entityplayer.getName()});
         }
      }
   }

   @Override
   public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
      return args.length == 2 ? getListOfStringsMatchingLastWord(args, server.getAllUsernames()) : Collections.emptyList();
   }

   @Override
   public boolean isUsernameIndex(String[] args, int index) {
      return index == 1;
   }
}
