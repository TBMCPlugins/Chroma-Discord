package buttondevteam.DiscordPlugin;

import buttondevteam.discordplugin.DiscordConnectedPlayer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

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
		Player dcp = DiscordConnectedPlayer.createTest();

		double h = dcp.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue(); ; ;
		System.out.println(h);
	}
}
