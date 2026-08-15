package keystrokesmod.script.model;

import net.minecraft.client.Minecraft;

public class Simulation {
   private final SimulatedPlayer raw;

   private Simulation(SimulatedPlayer raw) {
      this.raw = raw;
   }

   public static Simulation create() {
      Minecraft mc = Minecraft.getMinecraft();
      net.minecraft.util.MovementInput input = mc.thePlayer.movementInput;
      SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(input);
      return new Simulation(sim);
   }

   public void setForward(float forward) {
      this.raw.movementInput.moveForward = forward;
   }

   public void setStrafe(float strafe) {
      this.raw.movementInput.moveStrafe = strafe;
   }

   public void setJump(boolean jump) {
      this.raw.movementInput.jump = jump;
   }

   public void setSneak(boolean sneak) {
      this.raw.movementInput.sneak = sneak;
   }

   public void setYaw(float yaw) {
      this.raw.rotationYaw = yaw;
   }

   public void tick() {
      this.raw.tick();
   }

   public Vec3 getPosition() {
      return this.raw.getPos();
   }

   public Vec3 getMotion() {
      return new Vec3(this.raw.motionX, this.raw.motionY, this.raw.motionZ);
   }

   public boolean onGround() {
      return this.raw.onGround;
   }
}
