package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.command.Command;

public class QList extends Command {
   public QList() {
      super("QList");
   }

   @Override
   public void onExecute(String[] args) {
      this.chat(" &7-------------------------------------&r");
      this.chat(" &7Q List:");
      this.chat(" &7 ");
      this.chat(" &7p - Bedwars Practice");
      this.chat(" &71 - Bedwars Solos");
      this.chat(" &72 - Bedwars Doubles");
      this.chat(" &73 - Bedwars Threes");
      this.chat(" &74 - Bedwars Fours");
      this.chat(" &74v4 - Bedwars 4v4");
      this.chat(" &72t - Bedwars Doubles Tourney");
      this.chat(" &72un - Bedwars Doubles Tower Underworld");
      this.chat(" &74un - Bedwars Fours Tower Underworld");
      this.chat(" &72r - Bedwars Doubles Rush");
      this.chat(" &74r - Bedwars Fours Rush");
      this.chat(" &7pit - The Pit");
      this.chat(" &7swsn - Skywars Solo Normal");
      this.chat(" &7swsi - Skywars Solo Insane");
      this.chat(" &7swtn - Skywars Teams Normal");
      this.chat(" &7swti - Skywars Teams Insane");
      this.chat(" &7bowduel - Bow Duel");
      this.chat(" &7classicduel - Classic Duel");
      this.chat(" &7opduel - OP Duel");
      this.chat(" &7uhcduel - UHC Duel");
      this.chat(" &7bridgeduel - Bridge Duel");
      this.chat(" &7uhc - UHC Solos");
      this.chat(" &7uhcteams - UHC Teams");
      this.chat(" &7grinch - Grinch Simulator");
      this.chat(" &7grinchtourney - Grinch Simulator Tournament");
      this.chat(" &7mm - Murder Mystery Classic");
      this.chat(" &7castle - Bedwars Castle");
      this.chat(" &7ww - Wool Wars");
      this.chat(" &7ctw - Capture The Wool");
      this.chat(" &7-------------------------------------");
   }
}
