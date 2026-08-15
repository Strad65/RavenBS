package keystrokesmod.script.model;

import com.google.common.collect.Iterables;
import com.mojang.authlib.properties.Property;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class NetworkPlayer {
   private NetworkPlayerInfo playerInfo;
   private static HashMap<String, NetworkPlayer> cache = new HashMap<>();

   public NetworkPlayer(NetworkPlayerInfo playerInfo) {
      this.playerInfo = playerInfo;
   }

   public String getCape() {
      return this.playerInfo.getLocationCape().getResourcePath();
   }

   public String getDisplayName() {
      return this.playerInfo.getGameProfile() == Minecraft.getMinecraft().thePlayer.getGameProfile()
         ? Minecraft.getMinecraft().thePlayer.getDisplayName().getUnformattedText()
         : ScorePlayerTeam.formatPlayerName(this.playerInfo.getPlayerTeam(), this.getName());
   }

   public String getName() {
      return this.playerInfo == null ? "" : this.playerInfo.getGameProfile().getName();
   }

   public int getPing() {
      return this.playerInfo == null ? 0 : this.playerInfo.getResponseTime();
   }

   public String getSkinData() {
      Property texture = (Property)Iterables.getFirst(this.playerInfo.getGameProfile().getProperties().get("textures"), null);
      return texture == null ? null : new String(Base64.getDecoder().decode(texture.getValue().getBytes(StandardCharsets.UTF_8)));
   }

   public String getUUID() {
      return this.playerInfo == null ? "" : this.playerInfo.getGameProfile().getId().toString();
   }

   public static NetworkPlayer convert(NetworkPlayerInfo networkPlayerInfo) {
      if (networkPlayerInfo == null) {
         return null;
      }

      String id = networkPlayerInfo.getGameProfile().getId().toString();
      NetworkPlayer cachedEntity = cache.get(id);
      if (cachedEntity == null) {
         cachedEntity = new NetworkPlayer(networkPlayerInfo);
         cache.put(id, cachedEntity);
      }

      return cachedEntity;
   }

   public static void clearCache() {
      cache.clear();
   }
}
