package keystrokesmod.module.impl.render;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Shaders extends Module {
   private SliderSetting shader;
   private String[] shaderNames;
   private ResourceLocation[] shaderLocations = ((IAccessorEntityRenderer)mc.entityRenderer).getShaderResourceLocations();
   private boolean resetShader;

   public Shaders() {
      super("Shaders", Module.category.render);
      if (this.shaderLocations != null) {
         this.shaderNames = new String[this.shaderLocations.length];

         for (int i = 0; i < this.shaderLocations.length; i++) {
            this.shaderNames[i] = this.shaderLocations[i].getResourcePath().replaceFirst("shaders/post/", "").split("\\.json")[0].toUpperCase();
         }

         this.registerSetting(this.shader = new SliderSetting("Shader", 0, this.shaderNames));
      }
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (mc.thePlayer != null && e.getPacket() instanceof S08PacketPlayerPosLook) {
         this.resetShader = true;
      }
   }

   @Override
   public void onUpdate() {
      if (this.resetShader) {
         mc.entityRenderer.stopUseShader();
         this.resetShader = false;
      }

      if (Utils.nullCheck() && mc.entityRenderer != null && this.shaderLocations != null) {
         try {
            if (((IAccessorEntityRenderer)mc.entityRenderer).getShaderIndex() != (int)this.shader.getInput()) {
               ((IAccessorEntityRenderer)mc.entityRenderer).setShaderIndex((int)this.shader.getInput());
               ((IAccessorEntityRenderer)mc.entityRenderer).callLoadShader(this.shaderLocations[(int)this.shader.getInput()]);
            } else if (!((IAccessorEntityRenderer)mc.entityRenderer).getUseShader()) {
               ((IAccessorEntityRenderer)mc.entityRenderer).setUseShader(true);
            }
         } catch (Exception ex) {
            ex.printStackTrace();
            Utils.sendMessage("&cError loading shader.");
            this.disable();
         }
      }
   }

   @Override
   public void onDisable() {
      mc.entityRenderer.stopUseShader();
   }

   @Override
   public void onEnable() {
      if (!OpenGlHelper.shadersSupported) {
         Utils.sendMessage("&cShaders not supported.");
         this.disable();
      }
   }
}
