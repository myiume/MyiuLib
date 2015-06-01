package me.myiume.myiulib.event.player;

import me.myiume.myiulib.api.MGPlayer;
import me.myiume.myiulib.api.Round;
import me.myiume.myiulib.event.round.MGRoundEvent;

import org.bukkit.event.Cancellable;

/**
 * Called when a {@link MGPlayer player} joins an {@link Round MyiuLib round}.
 *
 * @since 0.1.0
 */
public class PlayerJoinMinigameRoundEvent extends MGRoundEvent implements Cancellable {

	protected MGPlayer player;
	private boolean cancelled;

	/**
	 * Creates a new instance of this event.
	 *
	 * @param round  the round the player has joined
	 * @param player the player involved in this event
	 * @since 0.1.0
	 */
	public PlayerJoinMinigameRoundEvent(Round round, MGPlayer player) {
		super(round);
		this.player = player;
	}

	/**
	 * Returns the {@link Round round} involved in this event.
	 *
	 * @return the {@link Round round} involved in this event
	 * @since 0.1.0
	 */
	public MGPlayer getPlayer() {
		return player;
	}

	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

}
