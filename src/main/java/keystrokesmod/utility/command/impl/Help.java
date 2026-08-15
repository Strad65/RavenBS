package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.command.Command;

public class Help extends Command {
   public Help() {
      super("help");
   }

   @Override
   public void onExecute(String[] args) {
      this.chatWithPrefix("&7Chat commands - &dGeneral");
      this.chat(" &b.ign/name &7Copy your username.");
      this.chat(" &b.ping &7Estimate your ping.");
      this.chat(" &b.friend/enemy [name/clear] &7Adds as enemy/friend.");
      this.chat(" &b.q [mode] &7Queues a gamemode.");
      this.chat(" &b.qlist &7Shows the modes you can queue with the \".q\" command");
      this.chat(" &b.status [player] &7Checks if a player is online or not. (Bypasses filters)");
      this.chatWithPrefix("&7Chat commands - &dModules");
      this.chat(" &b.cname [name] &7Set name hider name.");
      this.chat(" &b.binds (key) &7List module binds.");
      this.chat(" &b.spammer <message> &7Set spammer message");
      this.chat(" &b.stop spammer &7Stops spammer");
      this.chatWithPrefix("&7Chat commands - &dProfiles");
      this.chat(" &b.profiles &7List loaded profiles.");
      this.chat(" &b.profiles save (name) &7Save current settings as a profile.");
      this.chat(" &b.profiles load [name] &7Load a profile.");
      this.chat(" &b.profiles delete [name] &7Delete a profile.");
      this.chat(" &b.profiles rename [oldname] [newname] &7Rename a profile.");
   }
}
