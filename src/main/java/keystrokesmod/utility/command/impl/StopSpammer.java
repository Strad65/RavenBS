package keystrokesmod.utility.command.impl;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;

public class StopSpammer extends Command {
   public StopSpammer() {
      super("Stop Spammer");
   }

   @Override
   public void onExecute(String[] args) {
      if (args.length >= 1) {
         ModuleManager.spammer.reset();
         Utils.modulePrint("§cPaused spammer");
      } else {
         this.syntaxError();
      }
   }
}
