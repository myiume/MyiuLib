package me.myiume.myiulib.api;

/**
 * A platform-independent enumeration of available chat colors in Minecraft.
 *
 * @since 0.4.0
 */
public enum Color {

	BLACK('0'),
	DARK_BLUE('1'),
	DARK_GREEN('2'),
	DARK_AQUA('3'),
	DARK_RED('4'),
	DARK_PURPLE('5'),
	GOLD('6'),
	GRAY('7'),
	DARK_GRAY('8'),
	BLUE('9'),
	GREEN('a'),
	AQUA('b'),
	RED('c'),
	LIGHT_PURPLE('d'),
	YELLOW('e'),
	WHITE('f'),
	MAGIC('k'),
	BOLD('l'),
	STRIKETHROUGH('m'),
	UNDERLINE('n'),
	ITALIC('o'),
	RESET('r');

	private char code;

	Color(char code) {
		this.code = code;
	}

	/**
	 * Gets the code associated with this {@link Color}.
	 * @return the code associated with this {@link Color}
	 * @since 0.4.0
	 */
	public char getCode() {
		return code;
	}

	/**
	 * Returns the color associated with the given char code, or null if not
	 * found.
	 * @param code the code to lookup
	 * @return the {@link Color} color associated with the code
	 * @since 0.4.0
	 */
	public static Color fromCode(char code) {
		for (Color c : Color.values()) {
			if (c.getCode() == code) {
				return c;
			}
		}
		return null;
	}

}
