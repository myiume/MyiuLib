package me.myiume.myiulib.event.player;

import me.myiume.myiulib.api.MGPlayer;
import me.myiume.myiulib.event.MyiuLibEvent;

/**
 * Called when an event involving a {@link MGPlayer player} occurs.
 *
 * @since 0.1.0
 */
public class MGPlayerEvent extends MyiuLibEvent {

	protected MGPlayer player;

	/**
	 * Creates a new instance of this event.
	 *
	 * @param player the {@link MGPlayer player} involved in this {@link
	 *               MGPlayerEvent event}
	 * @since 0.1.0
	 */
	public MGPlayerEvent(MGPlayer player) {
		super(player.getPlugin());
		this.player = player;
	}

	/**
	 * Retrieves the {@link MGPlayer player} involved in this {@link
	 * MGPlayerEvent event}.
	 *
	 * @return the {@link MGPlayer player} involved in this {@link MGPlayerEvent
	 * event}
	 * @since 0.1.0
	 */
	public MGPlayer getPlayer() {
		return player;
	}

}
