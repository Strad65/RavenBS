package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;

public class Status extends Command {
   public static String ign;
   public static String modeString;
   public static int currentMode;
   public static int lastMode;
   public static int cooldown;
   public static int displayNumber;
   public static boolean start;

   public Status() {
      super("status");
   }

   @Override
   public void onExecute(String[] args) {
      if (args.length == 2) {
         ign = args[1];
         if (cooldown != 0) {
            Utils.modulePrint("§dcurrently on cooldown for " + cooldown + "s");
         } else {
            currentMode++;
            this.getModeString();
            String msg = "/tip " + ign + modeString;
            this.mc.thePlayer.sendChatMessage(msg);
            lastMode = currentMode;
            displayNumber = lastMode + 1;
            start = true;
            cooldown = 7;
         }
      }
   }

   private void getModeString() {
      if (currentMode > 9) {
         currentMode = 0;
      }

      if (currentMode == 0) {
         modeString = " skywars";
      } else if (currentMode == 1) {
         modeString = " tnt";
      } else if (currentMode == 2) {
         modeString = " classic";
      } else if (currentMode == 3) {
         modeString = " blitz";
      } else if (currentMode == 4) {
         modeString = " mega";
      } else if (currentMode == 5) {
         modeString = " uhc";
      } else if (currentMode == 6) {
         modeString = " arcade";
      } else if (currentMode == 7) {
         modeString = " warlords";
      } else if (currentMode == 8) {
         modeString = " smash";
      } else if (currentMode == 9) {
         modeString = " cops";
      }
   }
}
