package keystrokesmod.script.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import keystrokesmod.script.Manager;
import keystrokesmod.utility.Utils;

public class Request {
   public String method;
   public String url;
   public List<String[]> headers = new ArrayList<>();
   public String userAgent;
   public int connectionTimeout;
   public int readTimeout;
   public String content = "";

   public Request(String method, String URL) {
      if (!method.equals("POST") && !method.equals("GET")) {
         this.method = "GET";
      } else {
         this.method = method;
      }

      this.url = URL;
      this.userAgent = "";
      this.readTimeout = 5000;
      this.connectionTimeout = 5000;
   }

   public void addHeader(String header, String value) {
      if (this.headers == null) {
         this.headers = new ArrayList<>();
      }

      this.headers.add(new String[]{header, value});
   }

   public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
   }

   public void setConnectTimeout(int timeout) {
      this.connectionTimeout = timeout;
   }

   public void setReadTimeout(int timeout) {
      this.readTimeout = timeout;
   }

   public void setContent(String content) {
      this.content = content;
   }

   public Response fetch() {
      if (!Manager.enableHttpRequests.isToggled()) {
         Utils.sendMessage("&cFailed to send http request, http requests are not enabled.");
         return new Response(404, "");
      }

      if (!this.url.isEmpty()) {
         HttpURLConnection con = null;

         try {
            URL url = new URL(this.url);
            con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod(this.method);
            con.setConnectTimeout(this.connectionTimeout);
            con.setReadTimeout(this.readTimeout);
            con.setRequestProperty(
               "User-Agent",
               this.userAgent.isEmpty()
                  ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                  : this.userAgent
            );
            if (this.headers != null && !this.headers.isEmpty()) {
               for (String[] header : this.headers) {
                  con.setRequestProperty(header[0], header[1]);
               }
            }

            if (this.method.equals("POST") && !this.content.isEmpty()) {
               con.setDoOutput(true);
               byte[] out = this.content.getBytes(StandardCharsets.UTF_8);
               con.setFixedLengthStreamingMode(out.length);
               con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
               con.connect();
               OutputStream os = con.getOutputStream();

               try {
                  os.write(out);
                  if (os != null) {
                     os.close();
                  }
               } catch (Throwable t) {
                  if (os != null) {
                     try {
                        os.close();
                     } catch (Throwable t2) {
                        t.addSuppressed(t2);
                     }
                  }

                  throw t;
               }
            }

            String contents = "";

            try {
               BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

               try {
                  StringBuilder sb = new StringBuilder();

                  String input;
                  while ((input = br.readLine()) != null) {
                     sb.append(input);
                  }

                  contents = sb.toString();
                  br.close();
               } catch (Throwable t3) {
                  try {
                     br.close();
                  } catch (Throwable t4) {
                     t3.addSuppressed(t4);
                  }

                  throw t3;
               }
            } catch (IOException er1) {
               InputStream errorStream = con.getErrorStream();
               if (errorStream != null) {
                  try {
                     BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));

                     try {
                        StringBuilder sb2 = new StringBuilder();

                        String input2;
                        while ((input2 = errorReader.readLine()) != null) {
                           sb2.append(input2);
                        }

                        contents = sb2.toString();
                        errorReader.close();
                     } catch (Throwable t5) {
                        try {
                           errorReader.close();
                        } catch (Throwable t6) {
                           t5.addSuppressed(t6);
                        }

                        throw t5;
                     }
                  } catch (IOException var25) {
                  }
               }
            }

            return new Response(con.getResponseCode(), contents);
         } catch (IOException var28) {
         } finally {
            if (con != null) {
               con.disconnect();
            }
         }
      }

      return null;
   }

   @Override
   public String toString() {
      return "Request(" + this.method + "," + this.url + ")";
   }
}
