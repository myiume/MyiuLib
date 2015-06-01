package me.myiume.myiulib.event.round;

import me.myiume.myiulib.api.Round;
import me.myiume.myiulib.api.Stage;

import org.bukkit.event.Cancellable;

/**
 * Called when the stage of an {@link me.myiume.myiulib.api.Round MyiuLib
 * round} changes.
 *
 * @since 0.3.0
 */
public class MinigameRoundStageChangeEvent extends MGRoundEvent implements Cancellable {

	private Stage before;
	private Stage after;

	private boolean cancelled = false;

	/**
	 * Creates a new instance of this event.
	 *
	 * @param round  the round associated with this event
	 * @param before the stage before the change
	 * @param after  the stage after the change
	 * @since 0.3.0
	 */
	public MinigameRoundStageChangeEvent(Round round, Stage before, Stage after) {
		super(round);
		this.before = before;
		this.after = after;
	}

	/**
	 * Gets the stage of the round before the event.
	 *
	 * @return the stage of the round before the event
	 * @since 0.3.0
	 */
	public Stage getStageBefore() {
		return before;
	}

	/**
	 * Gets the stage of the round after the event.
	 *
	 * @return the stage of the round after the event
	 * @since 0.3.0
	 */
	public Stage getStageAfter() {
		return after;
	}

	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}
