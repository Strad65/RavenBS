package keystrokesmod.utility.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.impl.Binds;
import keystrokesmod.utility.command.impl.Cname;
import keystrokesmod.utility.command.impl.Debug;
import keystrokesmod.utility.command.impl.Enemy;
import keystrokesmod.utility.command.impl.Friend;
import keystrokesmod.utility.command.impl.Help;
import keystrokesmod.utility.command.impl.Name;
import keystrokesmod.utility.command.impl.Ping;
import keystrokesmod.utility.command.impl.Profiles;
import keystrokesmod.utility.command.impl.Q;
import keystrokesmod.utility.command.impl.QList;
import keystrokesmod.utility.command.impl.SpammerUtil;
import keystrokesmod.utility.command.impl.Status;
import keystrokesmod.utility.command.impl.StopSpammer;

public class CommandManager {
   private List<Command> commands = new ArrayList<>();
   public String[] latestAutoComplete = new String[0];
   public static Status status;

   public CommandManager() {
      this.registerCommand(new Help());
      this.registerCommand(new Ping());
      this.registerCommand(new Name());
      this.registerCommand(new Binds());
      this.registerCommand(new Cname());
      this.registerCommand(new Debug());
      this.registerCommand(new Friend());
      this.registerCommand(new Enemy());
      this.registerCommand(new Profiles());
      this.registerCommand(new Q());
      this.registerCommand(new QList());
      this.registerCommand(new Status());
      this.registerCommand(new SpammerUtil());
      this.registerCommand(new StopSpammer());
   }

   public void executeCommand(String input) {
      String[] args = input.split(" ");

      for (Command command : this.commands) {
         if (args[0].equalsIgnoreCase("." + command.command)) {
            command.onExecute(args);
            return;
         }

         if (command.alias != null && command.alias.length != 0) {
            for (String alias : command.alias) {
               if (args[0].equalsIgnoreCase("." + alias)) {
                  command.onExecute(args);
                  return;
               }
            }
         }
      }

      Utils.sendMessage("§cUnknown command. Use .help for a list of commands.");
   }

   public boolean autoComplete(String input) {
      String[] completions = this.getCompletions(input);
      this.latestAutoComplete = completions != null ? completions : new String[0];
      return input.startsWith(".") && this.latestAutoComplete.length > 0;
   }

   private String[] getCompletions(String input) {
      if (!input.isEmpty() && input.charAt(0) == '.') {
         String[] args = input.split(" ");
         if (args.length <= 1) {
            String rawInput = input.substring(1);
            List<String> completions = new ArrayList<>();

            for (Command command : this.commands) {
               if (command.command.startsWith(rawInput) || Arrays.stream(command.alias).anyMatch(aliasx -> aliasx.startsWith(rawInput))) {
                  String alias = command.command.startsWith(rawInput)
                     ? command.command
                     : Arrays.stream(command.alias).filter(aliaz -> aliaz.startsWith(rawInput)).findFirst().orElse("");
                  completions.add("." + alias);
               }
            }

            return completions.toArray(new String[0]);
         }

         Command command = this.getCommand(args[0].substring(1));
         if (command != null) {
            List<String> tabCompletions = command.tabComplete(Arrays.copyOfRange(args, 1, args.length));
            return tabCompletions.toArray(new String[0]);
         }
      }

      return null;
   }

   private Command getCommand(String name) {
      for (Command cmd : this.commands) {
         if (cmd.command.equalsIgnoreCase(name) || Arrays.stream(cmd.alias).anyMatch(alias -> alias.equalsIgnoreCase(name))) {
            return cmd;
         }
      }

      return null;
   }

   public void registerCommand(Command command) {
      this.commands.add(command);
   }
}
