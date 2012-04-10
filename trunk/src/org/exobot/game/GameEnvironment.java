package org.exobot.game;

import java.util.Map;

/**
 * @author Ramus
 */
public interface GameEnvironment {

	public String getArchive();

	public String getCode();

	public String getDirectGame();

	public String getGame();

	public String getParameter(final String name);

	public Map<String, String> getParameters();

	public int getWorld();
}