package org.omnibot.game;

import java.io.IOException;
import java.util.Map;

/**
 * @author Ramus
 */
public interface GameEnvironment {

	/**
	 * Gets the archive location.
	 *
	 * @return The archive location.
	 */
	public String getArchive();

	/**
	 * Gets the code class.
	 *
	 * @return The code class.
	 */
	public String getCode();

	/**
	 * Gets the game direct location.
	 *
	 * @return The direct location to a certain world.
	 */
	public String getDirectGame();

	/**
	 * The default game location.
	 *
	 * @return The default game location.
	 */
	public String getGame();

	/**
	 * Get a parameter value with a given key.
	 *
	 * @param name The parameter key.
	 * @return The parameter value with the matching key.
	 */
	public String getParameter(final String name);

	/**
	 * The <tt>Map</tt> of parameters.
	 *
	 * @return The game paramters.
	 */
	public Map<String, String> getParameters();

	/**
	 * Gets the world.
	 *
	 * @return Gets the current RuneScape world.
	 */
	public String getWorld();

	/**
	 * Loads the current environment.
	 */
	public void load() throws IOException;
}