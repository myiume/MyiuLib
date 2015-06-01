package me.myiume.myiulib.event.round;

import me.myiume.myiulib.api.Round;

/**
 * Called when an {@link Round MyiuLib round} is rolled back.
 *
 * @since 0.1.0
 */
public class MinigameRoundRollbackEvent extends MGRoundEvent {

	/**
	 * Creates a new instance of this event.
	 *
	 * @param round the round associated with this event
	 * @since 0.1.0
	 */
	public MinigameRoundRollbackEvent(Round round) {
		super(round);
	}

}
