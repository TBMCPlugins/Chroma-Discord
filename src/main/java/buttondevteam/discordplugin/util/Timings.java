package buttondevteam.discordplugin.util;

import buttondevteam.discordplugin.listeners.CommonListeners;

public class Timings {
	private long start;

	public Timings() {
		start = System.nanoTime();
	}

	public void printElapsed(String message) {
		CommonListeners.debug(message + " (" + (System.nanoTime() - start) / 1000000L + ")");
		start = System.nanoTime();
	}
}
