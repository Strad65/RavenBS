package keystrokesmod.utility.shader;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class KawaseBlur {
   private static final Minecraft mc = Minecraft.getMinecraft();
   public static ShaderUtils kawaseDown = new ShaderUtils("kawaseDown");
   public static ShaderUtils kawaseUp = new ShaderUtils("kawaseUp");
   public static Framebuffer framebuffer = new Framebuffer(1, 1, false);
   private static int currentIterations;
   private static final List<Framebuffer> framebufferList = new ArrayList<>();

   private static void initFrameBuffers(float iterations) {
      for (Framebuffer framebuffer : framebufferList) {
         framebuffer.deleteFramebuffer();
      }

      framebufferList.clear();
      framebufferList.add(KawaseBlur.framebuffer = RenderUtils.createFrameBuffer(null));

      for (int i = 1; i <= iterations; i++) {
         Framebuffer currentBuffer = new Framebuffer((int)(mc.displayWidth / Math.pow(3.0, i)), (int)(mc.displayHeight / Math.pow(3.0, i)), false);
         currentBuffer.setFramebufferFilter(9729);
         GlStateManager.bindTexture(currentBuffer.framebufferTexture);
         GL11.glTexParameteri(3553, 10242, 33648);
         GL11.glTexParameteri(3553, 10243, 33648);
         GlStateManager.bindTexture(0);
         framebufferList.add(currentBuffer);
      }
   }

   public static void renderBlur(int stencilFrameBufferTexture, int iterations, float offset) {
      if (currentIterations != iterations || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
         initFrameBuffers(iterations);
         currentIterations = iterations;
      }

      renderFBO(framebufferList.get(1), mc.getFramebuffer().framebufferTexture, kawaseDown, offset);

      for (int i = 1; i < iterations; i++) {
         renderFBO(framebufferList.get(i + 1), framebufferList.get(i).framebufferTexture, kawaseDown, offset);
      }

      for (int i = iterations; i > 1; i--) {
         renderFBO(framebufferList.get(i - 1), framebufferList.get(i).framebufferTexture, kawaseUp, offset);
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
      GL13.glActiveTexture(34000);
      RenderUtils.bindTexture(stencilFrameBufferTexture);
      GL13.glActiveTexture(33984);
      RenderUtils.bindTexture(framebufferList.get(1).framebufferTexture);
      ShaderUtils.drawQuads();
      kawaseUp.unload();
      mc.getFramebuffer().bindFramebuffer(true);
      RenderUtils.bindTexture(framebufferList.get(0).framebufferTexture);
      RenderUtils.setAlphaLimit(0.0F);
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      ShaderUtils.drawQuads();
      GlStateManager.bindTexture(0);
      GlStateManager.disableBlend();
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
