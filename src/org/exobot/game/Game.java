package org.exobot.game;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.exobot.util.io.Internet;

/**
 * @author Ramus
 */
public class Game implements GameEnvironment {

	private static interface Patterns {

		public static final Pattern ARCHIVE = Pattern.compile("archive=(.*)  code");
		public static final Pattern CODE = Pattern.compile("code=(.*) mayscript");
		public static final Pattern DIRECT_GAME = Pattern.compile("src=\"(.*)\" ");
		public static final Pattern GAME = Pattern.compile("\",\"(.*)\",\"\",\"\"");
		public static final Pattern PARAMETER = Pattern.compile("<param name=\"([^\\s]+)\"\\s+value=\"([^>]*)\">");
	}

	public static final String HOME = "http://runescape.com/";

	private final Map<String, String> parameters;
	private final String game;
	private final String directGame;
	private final String archive;
	private final String code;
	private final int world;

	public Game() {
		try {
			game = parseGame();
			directGame = parseDirectGame();
			archive = parseArchive();
			parameters = parseParameters();
			code = parseCode();
			world = Integer.parseInt(directGame.substring(directGame.indexOf("world") + 5, directGame.indexOf(".runescape")));
		} catch (final IOException e) {
			throw new RuntimeException("Unable to initialize game environment.");
		}
	}

	@Override
	public String getArchive() {
		return archive;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getDirectGame() {
		return directGame;
	}

	@Override
	public String getGame() {
		return game;
	}

	@Override
	public String getParameter(final String name) {
		return parameters.get(name);
	}

	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public int getWorld() {
		return world;
	}

	private String parseArchive() throws IOException {
		final Matcher matcher = Patterns.ARCHIVE.matcher(Internet.downloadContent(directGame));
		if (!matcher.find()) {
			return null;
		}
		return matcher.group(1);
	}

	private String parseCode() throws IOException {
		final Matcher matcher = Patterns.CODE.matcher(Internet.downloadContent(directGame));
		if (!matcher.find()) {
			return null;
		}
		final String code = matcher.group(1);
		return code == null ? null : code.substring(0, code.length() - 6);
	}

	private String parseDirectGame() throws IOException {
		final Matcher matcher = Patterns.DIRECT_GAME.matcher(Internet.downloadContent(game));
		if (!matcher.find()) {
			return null;
		}
		return matcher.group(1);
	}

	private String parseGame() throws IOException {
		final Matcher matcher = Patterns.GAME.matcher(Internet.downloadContent(HOME));
		if (!matcher.find()) {
			return null;
		}
		return matcher.group(1);
	}

	private Map<String, String> parseParameters() throws IOException {
		final Map<String, String> parameters = new LinkedHashMap<String, String>();
		final Matcher matcher = Patterns.PARAMETER.matcher(Internet.downloadContent(directGame));
		while (matcher.find()) {
			final String key = matcher.group(1);
			final String value = matcher.group(2);
			parameters.put(key, value);
		}
		parameters.put("haveie6", "false");
		return parameters;
	}
}