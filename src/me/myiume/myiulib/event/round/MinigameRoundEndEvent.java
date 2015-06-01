package me.myiume.myiulib.event.round;

import me.myiume.myiulib.api.Round;

import org.bukkit.event.Cancellable;

/**
 * Called when an {@link Round MyiuLib round} ends.
 *
 * @since 0.1.0
 */
public class MinigameRoundEndEvent extends MGRoundEvent implements Cancellable {

	private boolean outOfTime;
	private boolean cancelled;

	/**
	 * Creates a new instance of this event.
	 *
	 * @param round     the {@link Round} which has ended
	 * @param outOfTime whether the round ended because its timer reached 0
	 * @since 0.1.0
	 */
	public MinigameRoundEndEvent(Round round, boolean outOfTime) {
		super(round);
		this.outOfTime = outOfTime;
	}

	/**
	 * Gets whether the round ended because its timer reached 0.
	 *
	 * @return whether the round ended because its timer reached 0
	 * @since 0.1.0
	 */
	public boolean wasOutOfTime() {
		return outOfTime;
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
