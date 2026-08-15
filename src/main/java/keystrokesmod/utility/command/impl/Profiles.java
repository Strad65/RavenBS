package keystrokesmod.utility.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;
import keystrokesmod.utility.profile.Profile;

public class Profiles extends Command {
   public Profiles() {
      super("profiles", new String[]{"profile", "p"});
   }

   @Override
   public void onExecute(String[] args) {
      if (args.length < 2) {
         List<Profile> profiles = keystrokesmod.Raven.profileManager.profiles;
         if (profiles.isEmpty()) {
            this.chatWithPrefix("&7No profiles found");
         } else {
            this.chatWithPrefix("&b" + profiles.size() + " &7profile" + (profiles.size() == 1 ? "" : "s") + " loaded.");

            for (Profile profile : profiles) {
               this.chatWithPrefix(" &7" + profile.getName() + (profile == keystrokesmod.Raven.currentProfile ? " &7(&bcurrent&7)" : ""));
            }
         }
      } else {
         String subCommand = args[1].toLowerCase();
         switch (subCommand) {
            case "save":
            case "s":
               if (args.length < 3) {
                  if (keystrokesmod.Raven.currentProfile != null) {
                     Utils.sendMessage("&7Saved profile: &b" + keystrokesmod.Raven.currentProfile);
                     keystrokesmod.Raven.profileManager.saveProfile(keystrokesmod.Raven.currentProfile);
                  } else {
                     this.syntaxError();
                  }

                  return;
               }

               String saveName = args[2];
               Profile newProfile = new Profile(saveName, 0);
               keystrokesmod.Raven.profileManager.saveProfile(newProfile);
               this.chatWithPrefix("&7Saved profile: &b" + saveName);
               keystrokesmod.Raven.profileManager.loadProfiles();
               break;
            case "load":
            case "l":
               if (args.length < 3) {
                  this.syntaxError();
                  return;
               }

               String loadName = args[2];
               if (keystrokesmod.Raven.profileManager.getProfile(loadName) == null) {
                  this.chatWithPrefix("&b" + loadName + " &7does not exist");
                  return;
               }

               keystrokesmod.Raven.profileManager.loadProfile(loadName);
               this.chatWithPrefix("&7Enabled profile: &b" + loadName);
               break;
            case "delete":
            case "remove":
            case "r":
               if (args.length < 3) {
                  this.syntaxError();
                  return;
               }

               String deleteName = args[2];
               if (keystrokesmod.Raven.profileManager.getProfile(deleteName) == null) {
                  this.chatWithPrefix("&cProfile &b" + deleteName + " &7does not exist");
                  return;
               }

               keystrokesmod.Raven.profileManager.deleteProfile(deleteName);
               this.chatWithPrefix("&7Removed profile: &b" + deleteName);
               keystrokesmod.Raven.profileManager.loadProfiles();
               break;
            case "rename":
               if (args.length < 4) {
                  this.syntaxError();
                  return;
               }

               String oldName = args[2];
               String newName = args[3];
               Profile oldProfile = keystrokesmod.Raven.profileManager.getProfile(oldName);
               if (oldProfile == null) {
                  this.chatWithPrefix("&b" + oldName + " &7does not exist");
                  return;
               }

               if (keystrokesmod.Raven.profileManager.getProfile(newName) != null) {
                  this.chatWithPrefix("&b" + newName + " &7already exists");
                  return;
               }

               Profile renamedProfile = new Profile(newName, oldProfile.getBind());
               keystrokesmod.Raven.profileManager.saveProfile(renamedProfile);
               keystrokesmod.Raven.profileManager.deleteProfile(oldName);
               this.chatWithPrefix("&b" + oldName + " &7renamed to &b" + newName);
               keystrokesmod.Raven.profileManager.loadProfiles();
               keystrokesmod.Raven.profileManager.loadProfile(newName);
               break;
            default:
               this.syntaxError();
         }
      }
   }

   @Override
   public List<String> tabComplete(String[] args) {
      if (args.length == 2) {
         return this.filterStartingWith(args[1], Arrays.asList("save", "s", "load", "l", "delete", "remove", "r", "rename"));
      }

      if (args.length == 3 && !args[1].equalsIgnoreCase("save") && !args[1].equalsIgnoreCase("s")) {
         List<String> profileNames = new ArrayList<>();

         for (Profile profile : keystrokesmod.Raven.profileManager.profiles) {
            profileNames.add(profile.getName());
         }

         return this.filterStartingWith(args[2], profileNames);
      } else {
         return new ArrayList<>();
      }
   }

   private List<String> filterStartingWith(String prefix, List<String> options) {
      List<String> filtered = new ArrayList<>();

      for (String option : options) {
         if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
            filtered.add(option);
         }
      }

      return filtered;
   }
}
