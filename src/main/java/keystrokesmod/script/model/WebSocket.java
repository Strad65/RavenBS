package keystrokesmod.script.model;

import java.net.URI;
import java.util.Map;
import keystrokesmod.script.Manager;
import keystrokesmod.utility.Utils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocket {
   private final WebSocketClient client;

   public WebSocket(String serverURI) {
      this.client = this.createWebSocketClient(serverURI);
   }

   public WebSocket(String serverUri, Map<String, String> httpHeaders) {
      this.client = this.createWebSocketClient(serverUri, httpHeaders);
   }

   private WebSocketClient createWebSocketClient(String serverUri) {
      return new WebSocketClient(toURI(serverUri)) {
         public void onOpen(ServerHandshake handshakedata) {
            WebSocket.this.onOpen(handshakedata.getHttpStatus(), handshakedata.getHttpStatusMessage());
         }

         public void onMessage(String message) {
            WebSocket.this.onMessage(message);
         }

         public void onClose(int code, String reason, boolean remote) {
            WebSocket.this.onClose(code, reason, remote);
         }

         public void onError(Exception ex) {
            WebSocket.this.onError(ex);
         }
      };
   }

   private WebSocketClient createWebSocketClient(String serverUri, Map<String, String> httpHeaders) {
      return new WebSocketClient(toURI(serverUri), httpHeaders) {
         public void onOpen(ServerHandshake handshakedata) {
            WebSocket.this.onOpen(handshakedata.getHttpStatus(), handshakedata.getHttpStatusMessage());
         }

         public void onMessage(String message) {
            WebSocket.this.onMessage(message);
         }

         public void onClose(int code, String reason, boolean remote) {
            WebSocket.this.onClose(code, reason, remote);
         }

         public void onError(Exception ex) {
            WebSocket.this.onError(ex);
         }
      };
   }

   private static URI toURI(String uri) {
      try {
         return new URI(uri);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public void onOpen(short status, String message) {
   }

   public void onMessage(String message) {
   }

   public void onClose(int code, String reason, boolean remote) {
   }

   public void onError(Exception ex) {
   }

   public boolean connect(boolean block) {
      if (!Manager.enableWebSockets.isToggled()) {
         Utils.sendMessage("&cFailed to connect to websockets, websockets are not enabled.");
         return false;
      }

      if (block) {
         try {
            return this.client.connectBlocking();
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
      } else {
         this.client.connect();
         return true;
      }
   }

   public void send(String message) {
      this.client.send(message);
   }

   public void close(boolean block) {
      if (block) {
         try {
            this.client.closeBlocking();
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
      } else {
         this.client.close();
      }
   }

   public boolean isOpen() {
      return this.client == null ? false : this.client.isOpen();
   }
}
