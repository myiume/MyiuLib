package me.myiume.myiulib.event.round;

import me.myiume.myiulib.api.LobbySign;
import me.myiume.myiulib.api.Round;
import me.myiume.myiulib.event.player.PlayerJoinMinigameRoundEvent;
import me.myiume.myiulib.misc.JoinResult;

/**
 * Called when a player clicks a lobby sign in order to join a minigame round.
 *
 * <p><strong>Note:</strong> If you
 * wish to cancel this event, you must instead listen to the {@link PlayerJoinMinigameRoundEvent} and cancel that.</p>
 *
 * @since 0.3.0
 */
public class LobbyClickEvent extends MGRoundEvent {

	private String player;
	private LobbySign lobbySign;
	private JoinResult result;

	/**
	 * Creates a new instance of this event.
	 *
	 * @param player    the name of the player who clicked a lobby sign
	 * @param round     the round which the lobby sign is linked to
	 * @param lobbySign the {@link LobbySign lobby sign} which has been clicked
	 * @param result    the result of the player being added to the round
	 * @since 0.3.0
	 */
	public LobbyClickEvent(String player, Round round, LobbySign lobbySign, JoinResult result) {
		super(round);
		this.player = player;
		this.lobbySign = lobbySign;
		this.result = result;
	}

	/**
	 * Retrieves the name of the player involved in this event.
	 *
	 * @return the name of the player involved in this event
	 * @since 0.3.0
	 */
	public String getPlayer() {
		return player;
	}

	/**
	 * Retrieves the {@link LobbySign lobby sign} object involved in this event.
	 *
	 * @return the {@link LobbySign lobby sign} object involved in this event
	 * @since 0.3.0
	 */
	public LobbySign getLobbySign() {
		return lobbySign;
	}

	/**
	 * Retrieves the result of the event.
	 *
	 * @return the result of this event
	 * @since 0.3.0
	 */
	public JoinResult getResult() {
		return result;
	}

}
