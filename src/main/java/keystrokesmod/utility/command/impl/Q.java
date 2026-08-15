package keystrokesmod.utility.command.impl;

import java.util.HashMap;
import java.util.Map;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;

public class Q extends Command {
   Map<String, String> hypixelPlayCommands = new HashMap<>();
   String playCommand;
   String gameMode;

   public Q() {
      super("Q");
      this.hypixelPlayCommands.put("p", "bedwars_practice");
      this.hypixelPlayCommands.put("1", "bedwars_eight_one");
      this.hypixelPlayCommands.put("2", "bedwars_eight_two");
      this.hypixelPlayCommands.put("3", "bedwars_four_three");
      this.hypixelPlayCommands.put("4", "bedwars_four_four");
      this.hypixelPlayCommands.put("4v4", "bedwars_two_four");
      this.hypixelPlayCommands.put("2t", "bedwars_eight_two_tourney");
      this.hypixelPlayCommands.put("2un", "bedwars_eight_two_towerUnderworld");
      this.hypixelPlayCommands.put("4un", "bedwars_four_four_towerUnderworld");
      this.hypixelPlayCommands.put("2r", "bedwars_eight_two_rush");
      this.hypixelPlayCommands.put("4r", "bedwars_four_four_rush");
      this.hypixelPlayCommands.put("pit", "pit");
      this.hypixelPlayCommands.put("swsn", "solo_normal");
      this.hypixelPlayCommands.put("swsi", "solo_insane");
      this.hypixelPlayCommands.put("swtn", "teams_normal");
      this.hypixelPlayCommands.put("swti", "teams_insane");
      this.hypixelPlayCommands.put("bowduel", "duels_bow_duel");
      this.hypixelPlayCommands.put("classicduel", "duels_classic_duel");
      this.hypixelPlayCommands.put("opduel", "duels_op_duel");
      this.hypixelPlayCommands.put("uhcduel", "duels_uhc_duel");
      this.hypixelPlayCommands.put("bridgeduel", "duels_bridge_duel");
      this.hypixelPlayCommands.put("uhc", "uhc_solo");
      this.hypixelPlayCommands.put("uhcteams", "uhc_teams");
      this.hypixelPlayCommands.put("grinch", "arcade_grinch_simulator_v2");
      this.hypixelPlayCommands.put("grinchtourney", "arcade_grinch_simulator_v2_tourney");
      this.hypixelPlayCommands.put("mm", "murder_classic");
      this.hypixelPlayCommands.put("castle", "bedwars_castle");
      this.hypixelPlayCommands.put("ww", "wool_wool_wars_two_four");
      this.hypixelPlayCommands.put("ctw", "wool_capture_the_wool_two_twenty");
      this.playCommand = "";
      this.gameMode = "";
   }

   @Override
   public void onExecute(String[] args) {
      if (args.length > 1) {
         this.playCommand = this.hypixelPlayCommands.get(args[1].trim());
         if (this.playCommand != null) {
            this.mc.thePlayer.sendChatMessage("/play " + this.playCommand);
         } else {
            Utils.modulePrint("&cQueue failed. Invalid gamemode.");
         }
      }
   }
}
