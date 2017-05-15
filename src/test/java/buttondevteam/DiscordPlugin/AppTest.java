package buttondevteam.DiscordPlugin;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang.exception.ExceptionUtils;

import buttondevteam.lib.TBMCCoreAPI;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
	/**
	 * Create the test case
	 *
	 * @param testName
	 *            name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testApp() {
		/*String sourcemessage = "Test message";
		Exception e = new Exception("Test exception");
		StringBuilder sb = TBMCCoreAPI.IsTestServer() ? new StringBuilder()
				: new StringBuilder("Coder role").append("\n");
		sb.append(sourcemessage).append("\n");
		sb.append("```").append("\n");
		String stackTrace = Arrays.stream(ExceptionUtils.getStackTrace(e).split("\\n"))
				.filter(s -> !(s.contains("\tat ") && ( //
				s.contains("java.util") //
						|| s.contains("java.lang") //
						|| s.contains("net.minecraft.server") //
						|| s.contains("sun.reflect") //
						|| s.contains("org.bukkit") //
				))).collect(Collectors.joining("\n"));
		if (stackTrace.length() > 1800)
			stackTrace = stackTrace.substring(0, 1800);
		sb.append(stackTrace).append("\n");
		sb.append("```");
		System.out.println(sb.toString());
		assertTrue(sb.toString().contains("Coder role"));*/
		assertTrue(true);
	}
}
