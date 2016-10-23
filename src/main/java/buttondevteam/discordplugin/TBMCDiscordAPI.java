package buttondevteam.discordplugin;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.exception.ExceptionUtils;

public final class TBMCDiscordAPI {
	public static void SendException(Exception e, String sourcemessage) {
		try {
			//System.out.println("A");
			// ByteArrayOutputStream baos = new ByteArrayOutputStream();
			// PrintStream str = new PrintStream(baos, true, "UTF-8");
			// PrintStream str = new PrintStream(baos);
			StringBuilder sb = new StringBuilder();
			//System.out.println("B");
			sb.append(sourcemessage).append("\n");
			sb.append("```").append("\n");
			// e.printStackTrace(str);
			sb.append(ExceptionUtils.getStackTrace(e)).append("\n");
			sb.append("```");
			// str.flush();
			// str.close();
			//System.out.println("C");
			//System.out.println("D");
			// final String string = baos.toString(StandardCharsets.UTF_8);
			//System.out.println("E");
			DiscordPlugin.issuechannel.sendMessage(sb.toString());
			//System.out.println("F");
		} catch (Exception ex) {
			//System.out.println("EX");
			//System.out.println(ex);
			ex.printStackTrace();
		}
		//System.out.println("G");
	}
}
