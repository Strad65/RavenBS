package keystrokesmod.script;

import keystrokesmod.event.AntiCheatFlagEvent;
import keystrokesmod.event.AttackEvent;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.GuiUpdateEvent;
import keystrokesmod.event.KeyPressEvent;
import keystrokesmod.event.PlayerMoveEvent;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.script.model.Entity;
import keystrokesmod.script.model.MovementInput;
import keystrokesmod.script.model.PlayerState;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.script.packet.clientbound.SPacket;
import keystrokesmod.script.packet.serverbound.CPacket;
import keystrokesmod.script.packet.serverbound.PacketHandler;
import keystrokesmod.utility.Utils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class ScriptEvents {
   public Module module;

   public ScriptEvents(Module module) {
      this.module = module;
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onChat(ClientChatReceivedEvent e) {
      if (e.type != 2 && Utils.nullCheck()) {
         if (!Utils.stripColor(e.message.getUnformattedText()).isEmpty()) {
            if (keystrokesmod.Raven.scriptManager.invokeBoolean("onChat", this.module, e.message.getUnformattedText()) == 0) {
               e.setCanceled(true);
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onSendPacket(SendPacketEvent e) {
      if (!e.isCanceled() && e.getPacket() != null) {
         if (!e.getPacket().getClass().getSimpleName().startsWith("S")) {
            CPacket packet = PacketHandler.convertServerBound(e.getPacket());
            if (packet != null && keystrokesmod.Raven.scriptManager.invokeBoolean("onPacketSent", this.module, packet) == 0) {
               e.setCanceled(true);
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onReceivePacket(ReceivePacketEvent e) {
      if (!e.isCanceled() && e.getPacket() != null) {
         SPacket packet = PacketHandler.convertClientBound(e.getPacket());
         if (packet != null && keystrokesmod.Raven.scriptManager.invokeBoolean("onPacketReceived", this.module, packet) == 0) {
            e.setCanceled(true);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onAttack(AttackEvent e) {
      if (!e.isCanceled()) {
         Entity target = Entity.convert(e.target);
         Entity attacker = Entity.convert(e.attacker);
         if (keystrokesmod.Raven.scriptManager.invokeBoolean("onAttackEntity", this.module, target, attacker) == 0) {
            e.setCanceled(true);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onClientRotations(ClientRotationEvent e) {
      Float[] rotations = keystrokesmod.Raven.scriptManager.invokeFloatArray("getRotations", this.module);
      if (rotations != null && rotations.length != 0 && rotations.length <= 2) {
         if (rotations[0] != null) {
            e.yaw = rotations[0];
         }

         if (rotations.length == 2 && rotations[1] != null) {
            e.pitch = rotations[1];
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPrePlayerMovementInput(PrePlayerInputEvent e) {
      MovementInput input = new MovementInput(e, (byte)0);
      keystrokesmod.Raven.scriptManager.invoke("onPrePlayerInput", this.module, input);
      if (!e.isEquals(input)) {
         e.setForward(input.forward);
         e.setSneak(input.sneak);
         e.setJump(input.jump);
         e.setStrafe(input.strafe);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onKeyTyped(KeyPressEvent e) {
      if (!e.isCanceled()) {
         if (keystrokesmod.Raven.scriptManager.invokeBoolean("onKeyPress", this.module, e.typedChar, e.keyCode) == 0) {
            e.setCanceled(true);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onRenderWorldLast(RenderWorldLastEvent e) {
      if (Utils.nullCheck()) {
         keystrokesmod.Raven.scriptManager.invoke("onRenderWorld", this.module, e.partialTicks);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPreUpdate(PreUpdateEvent e) {
      keystrokesmod.Raven.scriptManager.invoke("onPreUpdate", this.module);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPostUpdate(PostUpdateEvent e) {
      keystrokesmod.Raven.scriptManager.invoke("onPostUpdate", this.module);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onRenderTick(RenderTickEvent e) {
      if (e.phase == Phase.END && Utils.nullCheck()) {
         keystrokesmod.Raven.scriptManager.invoke("onRenderTick", this.module, e.renderTickTime);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onAntiCheatFlag(AntiCheatFlagEvent e) {
      keystrokesmod.Raven.scriptManager.invoke("onAntiCheatFlag", this.module, e.flag, Entity.convert(e.entity));
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onGuiUpdate(GuiUpdateEvent e) {
      if (e.guiScreen != null) {
         keystrokesmod.Raven.scriptManager.invoke("onGuiUpdate", this.module, e.guiScreen.getClass().getSimpleName(), e.opened);
      }
   }

   @SubscribeEvent
   public void onDisconnect(ClientDisconnectionFromServerEvent e) {
      keystrokesmod.Raven.scriptManager.invoke("onDisconnect", this.module);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPreMotion(PreMotionEvent e) {
      PlayerState playerState = new PlayerState(e, (byte)0);
      keystrokesmod.Raven.scriptManager.invoke("onPreMotion", this.module, playerState);
      if (!e.isEquals(playerState)) {
         if (e.getYaw() != playerState.yaw) {
            e.setYaw(playerState.yaw);
         }

         e.setPitch(playerState.pitch);
         e.setPosX(playerState.x);
         e.setPosY(playerState.y);
         e.setPosZ(playerState.z);
         e.setOnGround(playerState.onGround);
         e.setSprinting(playerState.isSprinting);
         e.setSneaking(playerState.isSneaking);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (e.entity != null) {
         keystrokesmod.Raven.scriptManager.invoke("onWorldJoin", this.module, Entity.convert(e.entity));
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPostInput(PostPlayerInputEvent e) {
      keystrokesmod.Raven.scriptManager.invoke("onPostPlayerInput", this.module);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPostMotion(PostMotionEvent e) {
      keystrokesmod.Raven.scriptManager.invoke("onPostMotion", this.module);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onMouse(MouseEvent e) {
      if (keystrokesmod.Raven.scriptManager.invokeBoolean("onMouse", this.module, e.button, e.buttonstate) == 0) {
         e.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPlayerMove(PlayerMoveEvent e) {
      keystrokesmod.Raven.scriptManager.invoke("onPlayerMove", this.module, new Vec3(e.x, e.y, e.z));
   }
}
