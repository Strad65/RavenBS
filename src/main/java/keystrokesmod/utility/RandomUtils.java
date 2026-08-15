package keystrokesmod.utility;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RandomUtils {
   private static SecureRandom secureRandom;

   public static double getRandom(double multiplier) {
      if (multiplier == 0.0) {
         return 0.0;
      }

      byte[] bytes = new byte[512];
      secureRandom.nextBytes(bytes);
      double firstRandom = secureRandom.nextDouble();
      int seedByteCount = 10;
      byte[] seed = secureRandom.generateSeed(seedByteCount);
      secureRandom.setSeed(seed);
      return (secureRandom.nextDouble() - firstRandom) * multiplier;
   }

   static {
      try {
         secureRandom = SecureRandom.getInstance("SHA1PRNG");
      } catch (NoSuchAlgorithmException e) {
         e.printStackTrace();
      }
   }
}
