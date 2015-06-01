package me.myiume.myiulib.event.round;

import me.myiume.myiulib.MGUtil;
import me.myiume.myiulib.MyiuLib;
import me.myiume.myiulib.api.Round;
import me.myiume.myiulib.event.MyiuLibEvent;

import org.bukkit.Bukkit;

/**
 * Called when an event involving an active {@link Round minigame round} occurs.
 *
 * @since 0.2.0
 */
public class MGRoundEvent extends MyiuLibEvent {

	protected Round round;

	/**
	 * Creates a new instance of this event.
	 *
	 * @param round the {@link Round} associated with this event
	 * @since 0.2.0
	 */
	public MGRoundEvent(final Round round) {
		super(round.getPlugin());
		this.round = round;
		if (MGUtil.getPlugin().isEnabled()) {
			Bukkit.getScheduler().runTaskLater(MGUtil.getPlugin(), new Runnable() {
				public void run() {
					round.getMinigame().getLobbyManager().update(round.getArena());
				}
			}, 2L);
		}
	}

	/**
	 * Retrieves the {@link Round} associated with this event.
	 *
	 * @return the {@link Round} associated with this event
	 * @since 0.2.0
	 */
	public Round getRound() {
		return round;
	}

}
