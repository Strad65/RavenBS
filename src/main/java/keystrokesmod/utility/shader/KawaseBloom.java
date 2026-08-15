package keystrokesmod.utility.shader;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

public class KawaseBloom {
   private static final Minecraft mc = Minecraft.getMinecraft();
   public static ShaderUtils kawaseDown = new ShaderUtils("kawaseDownBloom");
   public static ShaderUtils kawaseUp = new ShaderUtils("kawaseUpBloom");
   public static Framebuffer framebuffer = new Framebuffer(1, 1, false);
   private static int currentIterations;
   private static final List<Framebuffer> framebufferList = new ArrayList<>();

   private static void initFramebuffers(float iterations) {
      for (Framebuffer framebuffer : framebufferList) {
         framebuffer.deleteFramebuffer();
      }

      framebufferList.clear();
      framebufferList.add(KawaseBloom.framebuffer = RenderUtils.createFrameBuffer(null, false));

      for (int i = 1; i <= iterations; i++) {
         Framebuffer currentBuffer = new Framebuffer((int)(mc.displayWidth / Math.pow(2.0, i)), (int)(mc.displayHeight / Math.pow(2.0, i)), false);
         currentBuffer.setFramebufferFilter(9729);
         GlStateManager.bindTexture(currentBuffer.framebufferTexture);
         GL11.glTexParameteri(3553, 10242, 33648);
         GL11.glTexParameteri(3553, 10243, 33648);
         GlStateManager.bindTexture(0);
         framebufferList.add(currentBuffer);
      }
   }

   public static void renderBlur(int framebufferTexture, int iterations, float offset) {
      if (currentIterations != iterations || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
         initFramebuffers(iterations);
         currentIterations = iterations;
      }

      RenderUtils.setAlphaLimit(0.0F);
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(1, 1);
      GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      float currentOffset = offset;
      renderFBO(framebufferList.get(1), framebufferTexture, kawaseDown, currentOffset);

      for (int i = 1; i < iterations; i++) {
         currentOffset = offset / (float)Math.pow(1.5, i);
         renderFBO(framebufferList.get(i + 1), framebufferList.get(i).framebufferTexture, kawaseDown, currentOffset);
      }

      for (int i = iterations; i > 1; i--) {
         currentOffset = offset / (float)Math.pow(1.5, i - 1);
         renderFBO(framebufferList.get(i - 1), framebufferList.get(i).framebufferTexture, kawaseUp, currentOffset);
      }

      Framebuffer lastBuffer = framebufferList.get(0);
      lastBuffer.framebufferClear();
      lastBuffer.bindFramebuffer(false);
      kawaseUp.init();
      kawaseUp.setUniformf("offset", offset, offset);
      kawaseUp.setUniformi("inTexture", 0);
      kawaseUp.setUniformi("check", 1);
      kawaseUp.setUniformi("textureToCheck", 16);
      kawaseUp.setUniformf("halfpixel", 1.0F / lastBuffer.framebufferWidth, 1.0F / lastBuffer.framebufferHeight);
      kawaseUp.setUniformf("iResolution", lastBuffer.framebufferWidth, lastBuffer.framebufferHeight);
      GlStateManager.setActiveTexture(34000);
      RenderUtils.bindTexture(framebufferTexture);
      GlStateManager.setActiveTexture(33984);
      RenderUtils.bindTexture(framebufferList.get(1).framebufferTexture);
      ShaderUtils.drawQuads();
      kawaseUp.unload();
      GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      mc.getFramebuffer().bindFramebuffer(false);
      RenderUtils.bindTexture(framebufferList.get(0).framebufferTexture);
      RenderUtils.setAlphaLimit(0.0F);
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      ShaderUtils.drawQuads();
      GlStateManager.bindTexture(0);
      RenderUtils.setAlphaLimit(0.0F);
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
   }

   private static void renderFBO(Framebuffer framebuffer, int framebufferTexture, ShaderUtils shader, float offset) {
      framebuffer.framebufferClear();
      framebuffer.bindFramebuffer(false);
      shader.init();
      RenderUtils.bindTexture(framebufferTexture);
      shader.setUniformf("offset", offset, offset);
      shader.setUniformi("inTexture", 0);
      shader.setUniformi("check", 0);
      shader.setUniformf("halfpixel", 1.0F / framebuffer.framebufferWidth, 1.0F / framebuffer.framebufferHeight);
      shader.setUniformf("iResolution", framebuffer.framebufferWidth, framebuffer.framebufferHeight);
      ShaderUtils.drawQuads();
      shader.unload();
   }
}
