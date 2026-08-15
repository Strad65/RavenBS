package keystrokesmod.script;

import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import keystrokesmod.utility.Utils;

public class ScriptDiagnosticListener implements DiagnosticListener<JavaFileObject> {
   @Override
   public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
      String message = diagnostic.getMessage(null);
      if (!message.contains("SpongePowered")) {
         if (diagnostic.getSource() != null) {
            Utils.sendDebugMessage("§cError loading script §b" + Utils.extractFileName(((JavaSourceFromString)diagnostic.getSource()).name));
         }

         JavaFileObject javaFileObject = diagnostic.getSource();
         if (javaFileObject != null) {
            int indentIndex = message.indexOf("\n");
            String error = diagnostic.getMessage(Locale.getDefault());
            Utils.sendDebugMessage(" §7err: §c" + (indentIndex == -1 ? error : error.substring(0, indentIndex)));
            Utils.sendDebugMessage(" §7line: §c" + (diagnostic.getLineNumber() - ((JavaSourceFromString)diagnostic.getSource()).extraLines));
            String sourceContent = ((JavaSourceFromString)diagnostic.getSource()).getCharContent(true).toString();
            int startPos = (int)diagnostic.getStartPosition();
            int endPos = (int)diagnostic.getEndPosition();
            int srcIndentIndex = sourceContent.indexOf("\n", startPos);
            if (srcIndentIndex != -1) {
               Utils.sendDebugMessage(" §7src: §c" + sourceContent.substring(startPos, srcIndentIndex));
            } else {
               Utils.sendDebugMessage(" §7src: §c" + sourceContent.substring(startPos, endPos));
            }
         }
      }
   }
}
