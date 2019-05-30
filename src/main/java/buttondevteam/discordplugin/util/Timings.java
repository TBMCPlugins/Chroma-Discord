package buttondevteam.discordplugin.util;

import buttondevteam.discordplugin.DPUtils;

public class Timings {
	private long start;

	public Timings() {
		start = System.nanoTime();
	}

	public void printElapsed(String message) {
		DPUtils.getLogger().info(message + " (" + (System.nanoTime() - start) / 1000000L + ")");
		start = System.nanoTime();
	}
}
