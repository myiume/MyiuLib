package me.myiume.myiulib.api;

/**
 * A platform-independent enumeration of available gamemodes.
 *
 * @since 0.4.0
 */
public enum GameMode {

	SURVIVAL,
	CREATIVE,
	ADVENTURE,
	SPECTATOR;

	/**
	 * Returns {@link GameMode#valueOf(String) Gamemode.valueOf(name)}, or
	 * {@link GameMode#SURVIVAL} if said call fails.
	 * @param name the name of the value to get
	 * @return {@link GameMode#valueOf(String) Gamemode.valueOf(name)}, or
	 * {@link GameMode#SURVIVAL} if said call fails.
	 */
	public static GameMode getGameMode(String name) {
		try {
			return GameMode.valueOf(name.toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			return GameMode.SURVIVAL;
		}
	}

}
