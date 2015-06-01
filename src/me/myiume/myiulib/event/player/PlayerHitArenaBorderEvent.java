package me.myiume.myiulib.event.player;

import me.myiume.myiulib.api.MGPlayer;

/**
 * Called when a player collides with the border of an arena.
 *
 * @since 0.3.0
 */
public class PlayerHitArenaBorderEvent extends MGPlayerEvent {

	public PlayerHitArenaBorderEvent(MGPlayer player) {
		super(player);
	}

}
