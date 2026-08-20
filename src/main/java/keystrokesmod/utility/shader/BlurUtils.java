package keystrokesmod.utility.shader;

import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.shader.Framebuffer;

public class BlurUtils {
   private static Framebuffer stencilFrameBufferBlur = new Framebuffer(1, 1, false);
   private static Framebuffer stencilFrameBufferBloom = new Framebuffer(1, 1, false);

   public static void prepareBlur() {
      stencilFrameBufferBlur = RenderUtils.createFrameBuffer(stencilFrameBufferBlur);
      stencilFrameBufferBlur.framebufferClear();
      stencilFrameBufferBlur.bindFramebuffer(false);
   }

   public static void prepareBloom() {
      stencilFrameBufferBloom = RenderUtils.createFrameBuffer(stencilFrameBufferBloom);
      stencilFrameBufferBloom.framebufferClear();
      stencilFrameBufferBloom.bindFramebuffer(false);
   }

   public static void blurEnd(int passes, float radius) {
      stencilFrameBufferBlur.unbindFramebuffer();
      KawaseBlur.renderBlur(stencilFrameBufferBlur.framebufferTexture, passes, radius);
   }

   public static void bloomEnd(int passes, float radius) {
      stencilFrameBufferBloom.unbindFramebuffer();
      KawaseBloom.renderBlur(stencilFrameBufferBloom.framebufferTexture, passes, radius);
   }

   /**
    * Renders a blurred background rectangle (no rounded corners).
    * The blurred area shows the game scene blurred, masked to the given rect.
    *
    * @param x      left edge in GUI coordinates
    * @param y      top edge in GUI coordinates
    * @param width  width in GUI coordinates
    * @param height height in GUI coordinates
    * @param passes Kawase blur pass count (2 is typical)
    * @param radius blur radius in pixels
    */
   public static void blurRect(float x, float y, float width, float height, int passes, float radius) {
      if (radius <= 0) return;
      prepareBlur();
      RoundedUtils.drawRound(x, y, width, height, 0.0F, new java.awt.Color(0, 0, 0, 255));
      blurEnd(passes, radius);
   }

   /**
    * Renders a bloom/shadow effect around a rectangle (no rounded corners).
    *
    * @param x      left edge in GUI coordinates
    * @param y      top edge in GUI coordinates
    * @param width  width in GUI coordinates
    * @param height height in GUI coordinates
    * @param passes Kawase bloom pass count (3 is typical)
    * @param radius bloom radius in pixels
    * @param color  bloom color (typically semi-transparent black for shadow)
    */
   public static void bloomRect(float x, float y, float width, float height, int passes, float radius, java.awt.Color color) {
      if (radius <= 0) return;
      prepareBloom();
      RoundedUtils.drawRound(x, y, width, height, 0.0F, true, color);
      bloomEnd(passes, radius);
   }
}
