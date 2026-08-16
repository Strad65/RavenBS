package keystrokesmod.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import keystrokesmod.module.impl.client.ChatCommands;
import keystrokesmod.module.impl.client.CommandLine;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.impl.combat.AutoClicker;
import keystrokesmod.module.impl.combat.Backtrack;
import keystrokesmod.module.impl.combat.ClickAssist;
import keystrokesmod.module.impl.combat.FakeLag;
import keystrokesmod.module.impl.combat.HitBox;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.combat.LagRange;
import keystrokesmod.module.impl.combat.Reach;
import keystrokesmod.module.impl.combat.Reduce;
import keystrokesmod.module.impl.combat.RodAimbot;
import keystrokesmod.module.impl.combat.TPAura;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.impl.combat.WTap;
import keystrokesmod.module.impl.fun.Fun;
import keystrokesmod.module.impl.minigames.AutoRequeue;
import keystrokesmod.module.impl.minigames.AutoWho;
import keystrokesmod.module.impl.minigames.BedWars;
import keystrokesmod.module.impl.minigames.BridgeInfo;
import keystrokesmod.module.impl.minigames.CTWFly;
import keystrokesmod.module.impl.minigames.DuelsStats;
import keystrokesmod.module.impl.minigames.MurderMystery;
import keystrokesmod.module.impl.minigames.SkyWars;
import keystrokesmod.module.impl.minigames.SpeedBuilders;
import keystrokesmod.module.impl.minigames.SumoFences;
import keystrokesmod.module.impl.movement.Bhop;
import keystrokesmod.module.impl.movement.FastFall;
import keystrokesmod.module.impl.movement.Fly;
import keystrokesmod.module.impl.movement.InvMove;
import keystrokesmod.module.impl.movement.KeepSprint;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.movement.Momentum;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.movement.Sprint;
import keystrokesmod.module.impl.movement.TargetStrafe;
import keystrokesmod.module.impl.movement.Teleport;
import keystrokesmod.module.impl.movement.Timer;
import keystrokesmod.module.impl.movement.VClip;
import keystrokesmod.module.impl.other.Anticheat;
import keystrokesmod.module.impl.other.ChatBypass;
import keystrokesmod.module.impl.other.DebugAC;
import keystrokesmod.module.impl.other.FakeChat;
import keystrokesmod.module.impl.other.LatencyAlerts;
import keystrokesmod.module.impl.other.NameHider;
import keystrokesmod.module.impl.other.Spammer;
import keystrokesmod.module.impl.other.Test;
import keystrokesmod.module.impl.other.Timers;
import keystrokesmod.module.impl.other.ViewPackets;
import keystrokesmod.module.impl.player.AntiAFK;
import keystrokesmod.module.impl.player.AntiFireball;
import keystrokesmod.module.impl.player.AntiVoid;
import keystrokesmod.module.impl.player.AutoPlace;
import keystrokesmod.module.impl.player.AutoSwap;
import keystrokesmod.module.impl.player.AutoTool;
import keystrokesmod.module.impl.player.BedAura;
import keystrokesmod.module.impl.player.Blink;
import keystrokesmod.module.impl.player.DelayRemover;
import keystrokesmod.module.impl.player.Disabler;
import keystrokesmod.module.impl.player.FastMine;
import keystrokesmod.module.impl.player.FastPlace;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.player.InvManager;
import keystrokesmod.module.impl.player.NoFall;
import keystrokesmod.module.impl.player.NoRotate;
import keystrokesmod.module.impl.player.Safewalk;
import keystrokesmod.module.impl.player.Scaffold;
import keystrokesmod.module.impl.player.Tower;
import keystrokesmod.module.impl.player.WaterBucket;
import keystrokesmod.module.impl.render.AntiDebuff;
import keystrokesmod.module.impl.render.AntiShuffle;
import keystrokesmod.module.impl.render.Arrows;
import keystrokesmod.module.impl.render.BedESP;
import keystrokesmod.module.impl.render.BreakProgress;
import keystrokesmod.module.impl.render.Chams;
import keystrokesmod.module.impl.render.ChestESP;
import keystrokesmod.module.impl.render.ExtendCamera;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.impl.render.Indicators;
import keystrokesmod.module.impl.render.ItemESP;
import keystrokesmod.module.impl.render.MobESP;
import keystrokesmod.module.impl.render.Nametags;
import keystrokesmod.module.impl.render.NoCameraClip;
import keystrokesmod.module.impl.render.NoHurtCam;
import keystrokesmod.module.impl.render.PlayerESP;
import keystrokesmod.module.impl.render.Radar;
import keystrokesmod.module.impl.render.Shaders;
import keystrokesmod.module.impl.render.TargetInfo;
import keystrokesmod.module.impl.render.Tracers;
import keystrokesmod.module.impl.render.Trajectories;
import keystrokesmod.module.impl.render.Xray;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.impl.world.Weather;
import keystrokesmod.script.Manager;
import keystrokesmod.utility.Utils;

public class ModuleManager {
   public static List<Module> modules = new ArrayList<>();
   public static List<Module> organizedModules = new ArrayList<>();
   public static Module nameHider;
   public static Module fastPlace;
   public static MurderMystery murderMystery;
   public static InvMove invmove;
   public static SkyWars skyWars;
   public static AntiFireball antiFireball;
   public static AutoSwap autoSwap;
   public static BedAura bedAura;
   public static FastMine fastMine;
   public static Module antiShuffle;
   public static Module commandLine;
   public static Module antiBot;
   public static NoSlow noSlow;
   public static KillAura killAura;
   public static Module autoClicker;
   public static Module hitBox;
   public static Module reach;
   public static BedESP bedESP;
   public static Chams chams;
   public static HUD hud;
   public static Module timer;
   public static Fly fly;
   public static Module wTap;
   public static TargetInfo targetInfo;
   public static NoFall noFall;
   public static Disabler disabler;
   public static NoRotate noRotate;
   public static PlayerESP playerESP;
   public static Module chrisESP;
   public static Module reduce;
   public static Safewalk safeWalk;
   public static Module keepSprint;
   public static ExtendCamera extendCamera;
   public static InvManager invManager;
   public static Tower tower;
   public static NoCameraClip noCameraClip;
   public static BedWars bedwars;
   public static Bhop bhop;
   public static NoHurtCam noHurtCam;
   public static Scaffold scaffold;
   public static AutoTool autoTool;
   public static Sprint sprint;
   public static Weather weather;
   public static Arrows arrows;
   public static ChatCommands chatCommands;
   public static LongJump LongJump;
   public static CTWFly ctwFly;
   public static Blink blink;
   public static Velocity velocity;
   public static TargetStrafe targetStrafe;
   public static FastFall fastFall;
   public static AntiVoid antiVoid;
   public static Spammer spammer;
   public static AntiDebuff antiDebuff;
   public static Timers timers;
   public static LagRange lagRange;
   public static Momentum momentum;

   public void register() {
      this.addModule(autoClicker = new AutoClicker());
      this.addModule(LongJump = new LongJump());
      this.addModule(new AimAssist());
      this.addModule(new Backtrack());
      this.addModule(weather = new Weather());
      this.addModule(chatCommands = new ChatCommands());
      this.addModule(new ClickAssist());
      this.addModule(tower = new Tower());
      this.addModule(skyWars = new SkyWars());
      this.addModule(new DebugAC());
      this.addModule(new DelayRemover());
      this.addModule(new FakeLag());
      this.addModule(hitBox = new HitBox());
      this.addModule(new Radar());
      this.addModule(new Settings());
      this.addModule(reach = new Reach());
      this.addModule(extendCamera = new ExtendCamera());
      this.addModule(new RodAimbot());
      this.addModule(velocity = new Velocity());
      this.addModule(bhop = new Bhop());
      this.addModule(invManager = new InvManager());
      this.addModule(new ChatBypass());
      this.addModule(scaffold = new Scaffold());
      this.addModule(blink = new Blink());
      this.addModule(new AutoRequeue());
      this.addModule(new AntiAFK());
      this.addModule(autoTool = new AutoTool());
      this.addModule(noHurtCam = new NoHurtCam());
      this.addModule(new SpeedBuilders());
      this.addModule(new Teleport());
      this.addModule(fly = new Fly());
      this.addModule(invmove = new InvMove());
      this.addModule(new TPAura());
      this.addModule(new Trajectories());
      this.addModule(autoSwap = new AutoSwap());
      this.addModule(keepSprint = new KeepSprint());
      this.addModule(bedAura = new BedAura());
      this.addModule(noSlow = new NoSlow());
      this.addModule(new Indicators());
      this.addModule(new LatencyAlerts());
      this.addModule(noCameraClip = new NoCameraClip());
      this.addModule(sprint = new Sprint());
      this.addModule(timer = new Timer());
      this.addModule(new VClip());
      this.addModule(new AutoPlace());
      this.addModule(fastPlace = new FastPlace());
      this.addModule(new Freecam());
      this.addModule(noFall = new NoFall());
      this.addModule(disabler = new Disabler());
      this.addModule(safeWalk = new Safewalk());
      this.addModule(reduce = new Reduce());
      this.addModule(antiBot = new AntiBot());
      this.addModule(antiShuffle = new AntiShuffle());
      this.addModule(chams = new Chams());
      this.addModule(new ChestESP());
      this.addModule(new Nametags());
      this.addModule(playerESP = new PlayerESP());
      this.addModule(new Tracers());
      this.addModule(hud = new HUD());
      this.addModule(new Anticheat());
      this.addModule(new BreakProgress());
      this.addModule(wTap = new WTap());
      this.addModule(new Xray());
      this.addModule(new BridgeInfo());
      this.addModule(targetInfo = new TargetInfo());
      this.addModule(new DuelsStats());
      this.addModule(antiFireball = new AntiFireball());
      this.addModule(bedESP = new BedESP());
      this.addModule(murderMystery = new MurderMystery());
      this.addModule(new Manager());
      this.addModule(new SumoFences());
      this.addModule(new Fun.ExtraBobbing());
      this.addModule(killAura = new KillAura());
      this.addModule(new Fun.FlameTrail());
      this.addModule(new Fun.SlyPort());
      this.addModule(new ItemESP());
      this.addModule(new MobESP());
      this.addModule(new Fun.Spin());
      this.addModule(noRotate = new NoRotate());
      this.addModule(new FakeChat());
      this.addModule(nameHider = new NameHider());
      this.addModule(new Test());
      this.addModule(new WaterBucket());
      this.addModule(commandLine = new CommandLine());
      this.addModule(bedwars = new BedWars());
      this.addModule(fastMine = new FastMine());
      this.addModule(arrows = new Arrows());
      this.addModule(new keystrokesmod.utility.profile.Manager());
      this.addModule(new ViewPackets());
      this.addModule(new AutoWho());
      this.addModule(new Gui());
      this.addModule(new Shaders());
      this.addModule(ctwFly = new CTWFly());
      this.addModule(targetStrafe = new TargetStrafe());
      this.addModule(fastFall = new FastFall());
      this.addModule(antiVoid = new AntiVoid());
      this.addModule(spammer = new Spammer());
      this.addModule(antiDebuff = new AntiDebuff());
      this.addModule(timers = new Timers());
      this.addModule(lagRange = new LagRange());
      this.addModule(momentum = new Momentum());
      antiBot.enable();
      Collections.sort(modules, Comparator.comparing(Module::getName));
   }

   public void addModule(Module m) {
      modules.add(m);
   }

   public List<Module> getModules() {
      return modules;
   }

   public List<Module> inCategory(Module.category categ) {
      ArrayList<Module> categML = new ArrayList<>();

      for (Module mod : this.getModules()) {
         if (mod.moduleCategory().equals(categ)) {
            categML.add(mod);
         }
      }

      return categML;
   }

   public Module getModule(String moduleName) {
      for (Module module : modules) {
         if (module.getName().equals(moduleName)) {
            return module;
         }
      }

      return null;
   }

   public Module getModule(Class clazz) {
      for (Module module : modules) {
         if (module.getClass().equals(clazz)) {
            return module;
         }
      }

      return null;
   }

   public static void sort() {
      if (HUD.alphabeticalSort.isToggled()) {
         Collections.sort(organizedModules, Comparator.comparing(Module::getNameInHud));
      } else {
         organizedModules.sort(
            (o1, o2) -> Utils.mc
                  .fontRendererObj
                  .getStringWidth(o2.getNameInHud() + (HUD.showInfo.isToggled() && !o2.getInfo().isEmpty() ? " " + o2.getInfo() : ""))
               - Utils.mc.fontRendererObj.getStringWidth(o1.getNameInHud() + (HUD.showInfo.isToggled() && !o1.getInfo().isEmpty() ? " " + o1.getInfo() : ""))
         );
      }
   }

   public static boolean canExecuteChatCommand() {
      return chatCommands != null && chatCommands.isEnabled();
   }

   public static boolean lowercaseChatCommands() {
      return chatCommands != null && chatCommands.isEnabled() && chatCommands.lowercase();
   }
}
