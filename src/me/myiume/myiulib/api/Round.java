package me.myiume.myiulib.api;

import static me.myiume.myiulib.MGUtil.loadArenaYaml;
import static me.myiume.myiulib.MyiuLib.locale;
import me.myiume.myiulib.MGUtil;
import me.myiume.myiulib.MyiuLib;
import me.myiume.myiulib.RollbackManager;
import me.myiume.myiulib.UUIDFetcher;
import me.myiume.myiulib.event.player.PlayerHitArenaBorderEvent;
import me.myiume.myiulib.event.player.PlayerJoinMinigameRoundEvent;
import me.myiume.myiulib.event.player.PlayerLeaveMinigameRoundEvent;
import me.myiume.myiulib.event.round.MinigameRoundEndEvent;
import me.myiume.myiulib.event.round.MinigameRoundPrepareEvent;
import me.myiume.myiulib.event.round.MinigameRoundStageChangeEvent;
import me.myiume.myiulib.event.round.MinigameRoundStartEvent;
import me.myiume.myiulib.event.round.MinigameRoundTickEvent;
import me.myiume.myiulib.exception.InvalidLocationException;
import me.myiume.myiulib.exception.NoSuchArenaException;
import me.myiume.myiulib.exception.NoSuchPlayerException;
import me.myiume.myiulib.exception.PlayerOfflineException;
import me.myiume.myiulib.exception.PlayerPresentException;
import me.myiume.myiulib.exception.RoundFullException;
import me.myiume.myiulib.misc.JoinResult;
import me.myiume.myiulib.misc.Metadatable;
import me.myiume.myiulib.util.NmsUtil;

import com.google.common.collect.Lists;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Represents a round within a minigame.
 *
 * @since 0.1.0
 */
public class Round implements Metadatable {

	HashMap<String, Object> metadata = new HashMap<String, Object>();

	private int minPlayers;
	private int maxPlayers;
	private int prepareTime;
	private int roundTime;
	private Location3D exitLocation;

	private String plugin;
	private int time = 0;
	private Stage stage;

	private String world;
	private String arena;
	private String displayName;
	private List<Location> spawns = new ArrayList<Location>();
	private Location minBound;
	private Location maxBound;

	private HashMap<String, MGPlayer> players = new HashMap<String, MGPlayer>();

	private int timerHandle = -1;

	private boolean damage;
	private boolean pvp;
	private boolean rollback;

	/**
	 * Creates a new {@link Round} with the given parameters.
	 *
	 * <p><strong>Please use {@link Minigame#createRound(String)} unless you
	 * understand the implications of using this constructor.</strong></p>
	 *
	 * @param plugin the plugin which this round should be associated with
	 * @param arena  the name of the arena in which this round takes place in
	 * @throws NoSuchArenaException if the specified arena does not exist
	 */
	public Round(String plugin, String arena) throws NoSuchArenaException {
		MGYamlConfiguration y = loadArenaYaml(plugin);
		if (!y.contains(arena)) {
			throw new NoSuchArenaException();
		}
		ConfigurationSection cs = y.getConfigurationSection(arena); // make the code easier to read
		world = cs.getString("world"); // get the name of the world of the arena
		World w = Bukkit.getWorld(world); // convert it to a Bukkit world
		if (w == null) {
			w = Bukkit.createWorld(new WorldCreator(world).environment(MGUtil.getEnvironment(cs.getString("world"))));
		}
		if (w == null) { // but what if world is kill?
			throw new IllegalArgumentException("World " + world + " cannot be loaded!"); // then round is kill
		}
		for (String k : cs.getConfigurationSection("spawns").getKeys(false)) { // load spawns into round object
			Location l = new Location(
					w,
					cs.getDouble("spawns." + k + ".x"),
					cs.getDouble("spawns." + k + ".y"),
					cs.getDouble("spawns." + k + ".z")
			);
			if (cs.isSet(k + ".pitch")) {
				l.setPitch((float)cs.getDouble(cs.getCurrentPath() + ".spawns." + k + ".pitch"));
			}
			if (cs.isSet(k + ".yaw")) {
				l.setYaw((float)cs.getDouble(cs.getCurrentPath() + ".spawns." + k + ".yaw"));
			}
			spawns.add(l); // register spawn
		}
		if (cs.getBoolean("boundaries")) { // check if arena has boundaries defined
			minBound = new Location(w, cs.getDouble("minX"), cs.getDouble("minY"), cs.getDouble("minZ"));
			maxBound = new Location(w, cs.getDouble("maxX"), cs.getDouble("maxY"), cs.getDouble("maxZ"));
		}
		else {
			minBound = null;
			maxBound = null;
		}
		this.plugin = plugin; // set globals
		this.arena = arena.toLowerCase();
		this.displayName = cs.contains("displayname") ? cs.getString("displayname") : arena.toLowerCase();
		ConfigManager cm = getConfigManager();
		this.prepareTime = cm.getDefaultPreparationTime();
		this.roundTime = cm.getDefaultPlayingTime();
		this.minPlayers = cm.getMinPlayers();
		this.maxPlayers = cm.getMaxPlayers();
		this.exitLocation = MGUtil.fromBukkitLocation(cm.getDefaultExitLocation(), true);
		this.damage = cm.isDamageAllowed();
		this.pvp = cm.isPvPAllowed();
		this.rollback = cm.isRollbackEnabled();
		stage = Stage.WAITING; // default to waiting stage
		String[] defaultKeysA = new String[]{"world", "spawns", "minX", "minY", "minZ", "maxX", "maxY", "maxZ"};
		List<String> defaultKeys = Arrays.asList(defaultKeysA);
		for (String k : cs.getKeys(true)) {
			if (!defaultKeys.contains(k.split("\\.")[0])) {
				setMetadata(k, cs.get(k));
			}
		}
		Minigame.getMinigameInstance(plugin).getRounds().put(arena, this); // register round with minigame instance
	}

	/**
	 * Gets the name of the minigame plugin associated with this {@link Round}.
	 *
	 * @return the name of the minigame plugin associated with this {@link
	 * Round}
	 * @since 0.1.0
	 */
	public String getPlugin() {
		return plugin;
	}

	/**
	 * Gets the instance of the MyiuLib API registered by the plugin associated
	 * with this {@link Round}.
	 *
	 * @return the instance of the MyiuLib API registered by the plugin associated
	 * with this {@link Round}
	 * @since 0.1.0
	 */
	public Minigame getMinigame() {
		return Minigame.getMinigameInstance(plugin);
	}

	/**
	 * Gets the name of the arena associated with this {@link Round}.
	 *
	 * @return the name of the arena associated with this {@link Round}
	 * @since 0.1.0
	 */
	public String getArena() {
		return arena;
	}

	/**
	 * Gets the current {@link Stage} of this {@link Round}.
	 *
	 * @return the current {@link Stage} of this {@link Round}
	 * @since 0.1.0
	 */
	public Stage getStage() {
		return stage;
	}

	/**
	 * Gets the current time in seconds of this {@link Round}, where 0
	 * represents the first second of it.
	 *
	 * @return the current time in seconds of this {@link Round}, where 0
	 * represents the first second of it
	 * @since 0.1.0
	 */
	public int getTime() {
		return time;
	}

	/**
	 * Gets the time remaining in this round.
	 *
	 * @return the time remaining in this round, or -1 if there is no time limit
	 * or if the {@link Stage stage} is not {@link Stage#PLAYING PLAYING} or
	 * {@link Stage#PREPARING PREPARING}
	 * @since 0.1.0
	 */
	public int getRemainingTime() {
		switch (this.getStage()) {
			case PREPARING:
				if (this.getPreparationTime() > 0) {
					return this.getPreparationTime() - this.getTime();
				}
				else {
					return -1;
				}
			case PLAYING:
				if (this.getPlayingTime() > 0) {
					return this.getPlayingTime() - this.getTime();
				}
				else {
					return -1;
				}
			default:
				return -1;
		}
	}

	/**
	 * Gets the round's preparation time.
	 *
	 * @return the round's preparation time
	 * @since 0.1.0
	 */
	public int getPreparationTime() {
		return prepareTime;
	}

	/**
	 * Gets the round's playing time.
	 *
	 * @return the round's playing time
	 * @since 0.1.0
	 */
	public int getPlayingTime() {
		return roundTime;
	}

	/**
	 * Gets the round's timer's task's handle, or -1 if a timer is not started.
	 *
	 * @return the round's timer's task's handle, or -1 if a timer is not
	 * started
	 * @since 0.1.0
	 */
	public int getTimerHandle() {
		return timerHandle;
	}

	/**
	 * Returns whether this round's timer is currently ticking.
	 *
	 * <p>This method simply checks whether {@link Round#getTimerHandle()}
	 * returns a number greater than or equal to <code>0</code>.</p>
	 *
	 * @return whether this round's timer is currently ticking.
	 * @since 0.4.0
	 */
	public boolean isTicking() {
		return this.getTimerHandle() >= 0;
	}

	/**
	 * Sets the associated arena of this {@link Round}.
	 *
	 * @param arena the arena to associate with this {@link Round}
	 * @since 0.1.0
	 */
	public void setArena(String arena) {
		this.arena = arena;
	}

	/**
	 * Sets the current stage of this {@link Round}. Note that this <em>will not
	 * </em> start or restart the round.
	 *
	 * @param stage      the stage to set this {@link Round} to
	 * @param resetTimer whether to reset the round timer (defaults to true if
	 *                   omitted)
	 * @since 0.4.0
	 */
	public void setStage(Stage stage, boolean resetTimer) {
		MinigameRoundStageChangeEvent event = new MinigameRoundStageChangeEvent(this, this.stage, stage);
		MGUtil.callEvent(event);
		if (!event.isCancelled()) {
			this.stage = stage;
			if (resetTimer) {
				setTime(0);
			}
		}
	}

	/**
	 * Sets the current stage of this {@link Round}.
	 *
	 * @param stage the stage to set this {@link Round} to
	 * @since 0.1.0
	 */
	public void setStage(Stage stage) {
		this.setStage(stage, true);
	}

	/**
	 * Sets the remaining time of this {@link Round}.
	 *
	 * @param t the time to set this {@link Round} to
	 * @since 0.1.0
	 */
	public void setTime(int t) {
		time = t;
	}

	/**
	 * Sets the round's preparation time.
	 *
	 * @param t the number of seconds to set the preparation time to. Use -1 for
	 *          no limit, or 0 for no preparation phase.
	 * @since 0.1.0
	 */
	public void setPreparationTime(int t) {
		prepareTime = t;
	}

	/**
	 * Sets the round's playing time.
	 *
	 * @param t the number of seconds to set the preparation time to. Use -1 for
	 *          no limit
	 * @since 0.1.0
	 */
	public void setPlayingTime(int t) {
		roundTime = t;
	}

	/**
	 * Decrements the time remaining in the round by 1.
	 *
	 * <p><strong>Please do not call this method from your plugin unless you
	 * understand the implications. Let MyiuLib handle the timer.</strong></p>
	 *
	 * @since 0.1.0
	 */
	public void tick() {
		time += 1;
	}

	/**
	 * Subtracts <code>t</code> seconds from the elapsed time in the round.
	 *
	 * @param t the number of seconds to subtract
	 * @since 0.1.0
	 */
	public void subtractTime(int t) {
		time -= t;
	}

	/**
	 * Adds <code>t</code> seconds to the elapsed time in the round.
	 *
	 * @param t the number of seconds to add
	 * @since 0.1.0
	 */
	public void addTime(int t) {
		time += t;
	}

	/**
	 * Destroys this {@link Round}.
	 *
	 * <p><strong>Please do not call this method from your plugin unless you
	 * understand the implications.</strong></p>
	 *
	 * @since 0.1.0
	 */
	public void destroy() {
		Minigame.getMinigameInstance(plugin).getRounds().remove(this.getArena());
	}

	/**
	 * Retrieves a list of {@link MGPlayer MGPlayers} in this round.
	 *
	 * @return a list of {@link MGPlayer MGPlayers} in this round
	 * @since 0.1.0
	 */
	public List<MGPlayer> getPlayerList() {
		return Lists.newArrayList(players.values());
	}

	/**
	 * Retrieves a {@link HashMap} of players in this round.
	 *
	 * @return a {@link HashMap} mapping the names of players in the round to
	 * their respective {@link MGPlayer} objects
	 * @since 0.1.0
	 */
	public HashMap<String, MGPlayer> getPlayers() {
		return players;
	}

	/**
	 * Retrieves a {@link HashMap} of all players on a given team.
	 *
	 * @param team the team to retrieve players from
	 * @return a {@link HashMap} mapping the names of players on a given team to
	 * their respective {@link MGPlayer} objects.
	 * @since 0.3.0
	 */
	public HashMap<String, MGPlayer> getTeam(String team) {
		HashMap<String, MGPlayer> t = new HashMap<String, MGPlayer>();
		for (MGPlayer p : getPlayerList()) {
			if (p.getTeam() != null && p.getTeam().equals(team)) {
				t.put(p.getName(), p);
			}
		}
		return t;
	}

	/**
	 * Retrieves a list of non-spectating {@link MGPlayer MGPlayers} in this
	 * round.
	 *
	 * @return a list of non-spectating {@link MGPlayer MGPlayers} in this round
	 * @since 0.2.0
	 */
	public List<MGPlayer> getAlivePlayerList() {
		List<MGPlayer> list = new ArrayList<MGPlayer>();
		for (MGPlayer p : players.values()) {
			if (!p.isSpectating()) {
				list.add(p);
			}
		}
		return list;
	}

	/**
	 * Retrieves a list of spectating {@link MGPlayer MGPlayers} in this {@link
	 * Round}.
	 *
	 * @return a list of spectating {@link MGPlayer MGPlayers} in this {@link
	 * Round}
	 * @since 0.2.0
	 */
	public List<MGPlayer> getSpectatingPlayerList() {
		List<MGPlayer> list = new ArrayList<MGPlayer>();
		for (MGPlayer p : players.values()) {
			if (p.isSpectating()) {
				list.add(p);
			}
		}
		return list;
	}

	/**
	 * Retrieves the number of {@link MGPlayer MGPlayers} in this {@link
	 * Round}.
	 *
	 * @return the number of {@link MGPlayer MGPlayers} in this {@link Round}
	 * @since 0.2.0
	 */
	public int getPlayerCount() {
		return players.size();
	}

	/**
	 * Retrieves the number of in-game (non-spectating) {@link MGPlayer
	 * MGPlayers} in this {@link Round}.
	 *
	 * @return the number of in-game (non-spectating) {@link MGPlayer MGPlayers}
	 * in this {@link Round}
	 * @since 0.2.0
	 */
	public int getAlivePlayerCount() {
		int count = 0;
		for (MGPlayer p : players.values()) {
			if (!p.isSpectating()) {
				count += 1;
			}
		}
		return count;
	}

	/**
	 * Retrieves the number of spectating {@link MGPlayer MGPlayers} in this
	 * {@link Round}.
	 *
	 * @return the number of spectating {@link MGPlayer MGPlayers} in this
	 * {@link Round}
	 * @since 0.2.0
	 */
	public int getSpectatingPlayerCount() {
		int count = 0;
		for (MGPlayer p : players.values()) {
			if (p.isSpectating()) {
				count += 1;
			}
		}
		return count;
	}

	/**
	 * Begins the round and starts its timer. If the round's current stage is
	 * {@link Stage#PREPARING}, it will be set to {@link Stage#PLAYING} and the
	 * timer will be reset when it reaches 0. Otherwise, its stage will be set
	 * to {@link Stage#PREPARING} and it will begins its preparation stage.
	 *
	 * <p>After it finishes its preparation, it will begin as it would if this
	 * method were called again (don't actually call it again though, or you'll
	 * trigger an exception).</p>
	 *
	 * @throws IllegalStateException if the stage is already {@link
	 *                               Stage#PLAYING}
	 * @since 0.1.0
	 */
	public void start() {
		final Round r = this;
		if (stage == Stage.WAITING || stage == Stage.PREPARING) { // make sure the round isn't already started
			if (r.getPreparationTime() > 0 && stage == Stage.WAITING) {
				MinigameRoundPrepareEvent event = new MinigameRoundPrepareEvent(r);
				MGUtil.callEvent(event);
				if (event.isCancelled()) {
					return;
				}
				r.setTime(0); // reset time
				r.setStage(Stage.PREPARING); // set stage to preparing
			}
			else {
				MinigameRoundStartEvent event = new MinigameRoundStartEvent(r);
				MGUtil.callEvent(event);
				if (event.isCancelled()) {
					return;
				}
				r.setTime(0); // reset timer
				r.setStage(Stage.PLAYING);
			}
			if (time != -1) { // I'm pretty sure this is wrong, but I'm also pretty tired
				timerHandle = Bukkit.getScheduler().runTaskTimer(MGUtil.getPlugin(), new Runnable() {
					public void run() {
						int oldTime = r.getTime();
						boolean stageChange = false;
						int limit = r.getStage() == Stage.PLAYING ? r.getPlayingTime() : r.getPreparationTime();
						if (r.getTime() >= limit && limit > 0) { // timer reached its limit
							if (r.getStage() == Stage.PREPARING) { // if we're still preparing...
								MinigameRoundStartEvent event = new MinigameRoundStartEvent(r);
								MGUtil.callEvent(event);
								if (event.isCancelled()) {
									return;
								}
								r.setStage(Stage.PLAYING); // ...set stage to playing
								stageChange = true;
								r.setTime(0); // reset timer
							}
							else { // we're playing and the round just ended
								end(true);
								stageChange = true;
							}
						}
						if (!stageChange) {
							r.tick();
						}
						//TODO: Allow for a grace period upon player disconnect
						if (r.getMinBound() != null) {
							// this whole bit handles keeping player inside the arena
							//TODO: Possibly make an event for when a player wanders out of an arena
							for (MGPlayer p : r.getPlayerList()) {
								@SuppressWarnings("deprecation")
								Player pl = Bukkit.getPlayer(p.getName());
								Location l = pl.getLocation();
								boolean event = true;
								boolean toggleFlip = false;
								if (!getMinigame().getConfigManager().isTeleportationAllowed()) {
									getMinigame().getConfigManager().setTeleportationAllowed(true);
									toggleFlip = true;
								}
								if (l.getX() < r.getMinBound().getX()) {
									pl.teleport(new Location(
											l.getWorld(), r.getMinBound().getX(), l.getY(), l.getZ()
									), TeleportCause.PLUGIN);
								}
								else if (l.getX() > r.getMaxBound().getX()) {
									pl.teleport(new Location(
											l.getWorld(), r.getMaxBound().getX(), l.getY(), l.getZ()
									), TeleportCause.PLUGIN);
								}
								else if (l.getY() < r.getMinBound().getY()) {
									pl.teleport(new Location(
											l.getWorld(), l.getX(), r.getMinBound().getY(), l.getZ()
									), TeleportCause.PLUGIN);
								}
								else if (l.getY() > r.getMaxBound().getY()) {
									pl.teleport(new Location(
											l.getWorld(), l.getX(), r.getMinBound().getY(), l.getZ()
									), TeleportCause.PLUGIN);
								}
								else if (l.getZ() < r.getMinBound().getZ()) {
									pl.teleport(new Location(
											l.getWorld(), l.getX(), l.getY(), r.getMinBound().getZ()
									), TeleportCause.PLUGIN);
								}
								else if (l.getZ() > r.getMaxBound().getZ()) {
									pl.teleport(new Location(
											l.getWorld(), l.getX(), l.getY(), r.getMinBound().getZ()
									), TeleportCause.PLUGIN);
								}
								else {
									event = false;
								}
								if (toggleFlip) {
									getMinigame().getConfigManager().setTeleportationAllowed(false);
								}
								if (event) {
									MGUtil.callEvent(new PlayerHitArenaBorderEvent(p));
								}
							}
						}
						if (r.getStage() == Stage.PLAYING || r.getStage() == Stage.PREPARING) {
							MGUtil.callEvent(new MinigameRoundTickEvent(r, oldTime, stageChange));
						}
					}
				}, 0L, 20L).getTaskId(); // iterates once per second
			}
		}
		else {
			throw new IllegalStateException(Bukkit.getPluginManager().getPlugin(plugin) +
					" attempted to start a round which had already been started.");
		}
	}

	/**
	 * Ends the round and resets its timer. The stage will also be set to {@link
	 * Stage#WAITING}.
	 *
	 * @param timeUp whether the round was ended due to its timer expiring. This
	 *               will default to false if omitted.
	 * @throws IllegalStateException if the timer has not been started
	 * @since 0.1.0
	 */
	public void end(boolean timeUp) {
		Stage prevStage = this.getStage();
		this.setStage(Stage.RESETTING);
		MinigameRoundEndEvent event = new MinigameRoundEndEvent(this, timeUp);
		MGUtil.callEvent(event);
		if (event.isCancelled()) {
			this.setStage(prevStage);
			return;
		}
		this.setTime(-1);
		if (this.getTimerHandle() != -1) {
			Bukkit.getScheduler().cancelTask(this.getTimerHandle()); // cancel the round's timer task
		}
		this.timerHandle = -1; // reset timer handle since the task no longer exists
		for (MGPlayer mp : getPlayerList()) { // iterate and remove players
			try {
				removePlayer(mp.getName());
			}
			catch (Exception ex) {
				// I don't care if this happens
			}
		}
		if (getConfigManager().isRollbackEnabled()) { // check if rollbacks are enabled
			getRollbackManager().rollback(getArena()); // roll back arena
		}
		setStage(Stage.WAITING);
	}

	/**
	 * Ends the round and resets its timer. The stage will also be set to {@link
	 * Stage#WAITING}.
	 *
	 * @throws IllegalStateException if the timer has not been started
	 * @since 0.1.0
	 */
	public void end() {
		end(false);
	}

	/**
	 * Retrieves whether this round has been ended.
	 *
	 * @return whether this round has been ended
	 * @since 0.3.0
	 * @deprecated Returns true only when {@link Round#getStage()} is equal to
	 * {@link Stage#RESETTING}. This comparison should be used instead.
	 */
	@Deprecated
	public boolean hasEnded() {
		return this.getStage() == Stage.RESETTING;
	}

	/**
	 * Retrieves the location representing the minimum boundary on all three
	 * axes of the arena this round takes place in.
	 *
	 * @return the location representing the minimum boundary on all three axes
	 * of the arena this round takes place in, or null if the arena does not
	 * have boundaries.
	 * @since 0.1.0
	 */
	//TODO: deprecate
	public Location getMinBound() {
		return minBound;
	}

	/**
	 * Retrieves the location representing the maximum boundary on all three
	 * axes of the arena this round takes place in.
	 *
	 * @return the location representing the maximum boundary on all three axes
	 * of the arena this round takes place in, or null if the arena does not
	 * have boundaries.
	 * @since 0.1.0
	 */
	//TODO: deprecate
	public Location getMaxBound() {
		return maxBound;
	}

	/**
	 * Sets the minimum boundary on all three axes of this round object.
	 *
	 * @param x the minimum x-value
	 * @param y the minimum y-value
	 * @param z the minimum z-value
	 * @since 0.1.0
	 */
	public void setMinBound(double x, double y, double z) {
		this.minBound = new Location(this.minBound.getWorld(), x, y, z);
	}

	/**
	 * Sets the maximum boundary on all three axes of this round object.
	 *
	 * @param x the maximum x-value
	 * @param y the maximum y-value
	 * @param z the maximum z-value
	 * @since 0.1.0
	 */
	public void setMaxBound(double x, double y, double z) {
		this.minBound = new Location(this.minBound.getWorld(), x, y, z);
	}

	/**
	 * Retrieves a list of possible spawns for this round's arena.
	 *
	 * @return a list of possible spawns for this round's arena
	 * @since 0.1.0
	 */
	//TODO: deprecate
	public List<Location> getSpawns() {
		return spawns;
	}

	/**
	 * Returns the {@link MGPlayer} in this round associated with the given
	 * username.
	 *
	 * @param player the username to search for
	 * @return the {@link MGPlayer} in this round associated with the given
	 * username, or <code>null</code> if none is found
	 * @since 0.1.0
	 */
	public MGPlayer getMGPlayer(String player) {
		return players.get(player);
	}

	/**
	 * Retrieves the world of this arena.
	 *
	 * @return the name of the world containing this arena
	 * @since 0.1.0
	 */
	public String getWorld() {
		return world;
	}

	/**
	 * Adds a player by the given name to this {@link Round round}.
	 *
	 * @param name the player to add to this {@link Round round}. (will default
	 *             to random/sequential (depending on configuration) if out of
	 *             bounds).
	 * @return the {@link JoinResult result} of the player being added to the
	 * round
	 * @throws PlayerOfflineException if the player is not online
	 * @throws PlayerPresentException if the player is already in a round
	 * @throws RoundFullException     if the round is full
	 * @since 0.1.0
	 */
	public JoinResult addPlayer(String name) throws PlayerOfflineException, PlayerPresentException, RoundFullException {
		return addPlayer(name, -1);
	}

	/**
	 * Adds a player by the given name to this {@link Round round}.
	 *
	 * @param name  the player to add to this {@link Round round}
	 * @param spawn the spawn number to teleport the player to (will default to
	 *              random/sequential (depending on configuration) if out of
	 *              bounds).
	 * @return the {@link JoinResult result} of the player being added to the
	 * round
	 * @throws PlayerOfflineException if the player is not online
	 * @throws PlayerPresentException if the player is already in a round
	 * @throws RoundFullException     if the round is full
	 * @since 0.3.0
	 */
	public JoinResult addPlayer(String name, int spawn)
			throws PlayerOfflineException, PlayerPresentException, RoundFullException {
		@SuppressWarnings("deprecation")
		final Player p = Bukkit.getPlayer(name);
		if (p == null) { // check that the specified player is online
			throw new PlayerOfflineException();
		}
		MGPlayer mp = Minigame.getMinigameInstance(plugin).getMGPlayer(name);
		if (mp == null) {
			if (this.getMinigame().customPlayerClass) {
				try {
					@SuppressWarnings("deprecation")
					Constructor<?> con = getConfigManager().getPlayerClass()
							.getDeclaredConstructor(String.class, String.class, String.class);
					mp = (MGPlayer)con.newInstance(plugin, name, arena.toLowerCase());
				}
				catch (NoSuchMethodException ex) { // thrown when the required constructor does not exist
					MyiuLib.log.severe(locale.getMessage("plugin.alert.bad-constructor", plugin));
					ex.printStackTrace();
					return JoinResult.INTERNAL_ERROR;
				}
				catch (InvocationTargetException ex) { // any error thrown from the called constructor
					ex.getTargetException().printStackTrace();
					return JoinResult.INTERNAL_ERROR;
				}
				catch (SecurityException ex) { // I have no idea why this would happen.
					ex.printStackTrace();
					return JoinResult.INTERNAL_ERROR;
				}
				catch (InstantiationException ex) { // if this happens then the overriding plugin screwed something up
					MyiuLib.log.severe(locale.getMessage("plugin.alert.bad-constructor", plugin));
					ex.printStackTrace();
					return JoinResult.INTERNAL_ERROR;
				}
				catch (IllegalAccessException ex) { // thrown if the called method is not public
					MyiuLib.log.severe(locale.getMessage("plugin.alert.invisible-constructor", plugin));
					ex.printStackTrace();
					return JoinResult.INTERNAL_ERROR;
				}
			}
			else {
				mp = new MGPlayer(plugin, name, arena.toLowerCase());
			}
		}
		else if (mp.getArena() == null) {
			mp.setArena(arena.toLowerCase());
		}
		else {
			throw new PlayerPresentException();
		}
		if (getPlayerCount() >= getMaxPlayers() && getMaxPlayers() > 0) {
			throw new RoundFullException();
		}
		if (getStage() == Stage.PREPARING) {
			if (!getConfigManager().getAllowJoinRoundWhilePreparing()) {
				p.sendMessage(ChatColor.RED + locale.getMessage("alert.personal.already-preparing"));
				return JoinResult.ROUND_PREPARING;
			}
		}
		else if (getStage() == Stage.PLAYING) {
			if (!getConfigManager().getAllowJoinRoundInProgress()) {
				p.sendMessage(ChatColor.RED + locale.getMessage("alert.personal.already-playing"));
				return JoinResult.ROUND_PLAYING;
			}
		}
		PlayerJoinMinigameRoundEvent event = new PlayerJoinMinigameRoundEvent(this, mp);
		MGUtil.callEvent(event);
		if (event.isCancelled()) {
			return JoinResult.CANCELLED;
		}
		ItemStack[] contents = p.getInventory().getContents();
		PlayerInventory pInv = p.getInventory();
		ItemStack helmet = pInv.getHelmet();
		ItemStack chestplate = pInv.getChestplate();
		ItemStack leggings = pInv.getLeggings();
		ItemStack boots = pInv.getBoots();
		try {
			File invDir = new File(MGUtil.getPlugin().getDataFolder(), "inventories");
			File invF = new File(MGUtil.getPlugin().getDataFolder() + File.separator +
					"inventories" + File.separator +
					UUIDFetcher.getUUIDOf(p.getName()) + ".dat");
			if (!invF.exists()) {
				invDir.mkdirs();
				invF.createNewFile();
			}
			YamlConfiguration invY = new YamlConfiguration();
			invY.load(invF);
			for (int i = 0; i < contents.length; i++) {
				invY.set(Integer.toString(i), contents[i]);
			}
			invY.set("h", helmet);
			invY.set("c", chestplate);
			invY.set("l", leggings);
			invY.set("b", boots);
			invY.save(invF);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			p.sendMessage(ChatColor.RED + locale.getMessage("error.personal.inv-save-fail"));
			return JoinResult.INVENTORY_SAVE_ERROR;
		}
		p.getInventory().clear();
		p.getInventory().setArmorContents(new ItemStack[4]);
		p.updateInventory();
		for (PotionEffect pe : p.getActivePotionEffects()) {
			p.removePotionEffect(pe.getType()); // remove any potion effects before adding the player
		}
		mp.setPrevGameMode(GameMode.getGameMode(p.getGameMode().name()));
		if ((getStage() == Stage.PREPARING || getStage() == Stage.PLAYING) && getConfigManager().getSpectateOnJoin()) {
			mp.setSpectating(true);
		}
		p.setGameMode(org.bukkit.GameMode.valueOf(getConfigManager().getDefaultGameMode().name()));
		players.put(name, mp); // register player with round object
		// update everyone's tablist
		// this needs to be called before the player is teleported
		List<Player> toAdd = new ArrayList<Player>();
		List<Player> toRemove = new ArrayList<Player>();

		mp.spawnIn(spawn);
		if (getStage() == Stage.WAITING && getPlayerCount() >= getMinPlayers() && getPlayerCount() > 0) {
			start();
		}
		return JoinResult.SUCCESS;
	}

	/**
	 * Removes a given player from this {@link Round round} and teleports them
	 * to the given location.
	 *
	 * @param name     the player to remove from this {@link Round round}
	 * @param location the location to teleport the player to
	 * @throws PlayerOfflineException if the player is not online
	 * @throws NoSuchPlayerException  if the player are not in this round
	 * @since 0.1.0
	 */
	public void removePlayer(String name, Location3D location) throws PlayerOfflineException, NoSuchPlayerException {
		@SuppressWarnings("deprecation")
		final Player p = Bukkit.getPlayer(name);
		MGPlayer mp = players.get(name);
		if (mp == null) {
			throw new NoSuchPlayerException();
		}
		if (p != null) {
			PlayerLeaveMinigameRoundEvent event = new PlayerLeaveMinigameRoundEvent(this, mp);
			MGUtil.callEvent(event);
			if (event.isCancelled()) {
				return;
			}
			mp.setSpectating(false); // make sure they're not spectating when they join a new round
			players.remove(name); // remove player from round
			p.setGameMode(org.bukkit.GameMode.valueOf(mp.getPrevGameMode().name())); // restore the player's gamemode
			mp.setArena(null); // they're not in an arena anymore
			mp.reset(location); // reset the object and send the player to the exit point
			if (this.getPlayerCount() < this.getMinPlayers()) {
				this.setStage(Stage.WAITING);
			}
		}
	}

	/**
	 * Removes a given player from this {@link Round round} and teleports them
	 * to the given location.
	 *
	 * @param name     the player to remove from this {@link Round round}
	 * @param location the location to teleport the player to
	 * @throws PlayerOfflineException if the player is not online
	 * @throws NoSuchPlayerException  if the player are not in this round
	 * @deprecated Use {@link Round#removePlayer(String, Location3D)}
	 * @since 0.1.0
	 */
	@Deprecated
	public void removePlayer(String name, Location location) throws PlayerOfflineException, NoSuchPlayerException {
		removePlayer(name, MGUtil.fromBukkitLocation(location, true));
	}

	/**
	 * Removes a given player from this {@link Round round} and teleports them
	 * to the round or plugin's default exit location (defaults to the main
	 * world's spawn point).
	 *
	 * @param name the player to remove from this {@link Round round}
	 * @throws NoSuchPlayerException  if the given player is not in this round
	 * @throws PlayerOfflineException if the given player is offline
	 * @since 0.1.0
	 */
	@SuppressWarnings("deprecation")
	public void removePlayer(String name) throws PlayerOfflineException, NoSuchPlayerException {
		removePlayer(name, MGUtil.fromBukkitLocation(getConfigManager().getDefaultExitLocation(), true));
	}

	/**
	 * Retrieves the minimum number of players required to automatically start
	 * the round.
	 *
	 * @return the minimum number of players required to automatically start the
	 * round
	 * @since 0.2.0
	 */
	public int getMinPlayers() {
		return minPlayers;
	}

	/**
	 * Sets the minimum number of players required to automatically start the
	 * round.
	 *
	 * @param players the minimum number of players required to automatically
	 *                start the round
	 * @since 0.2.0
	 */
	public void setMinPlayers(int players) {
		this.minPlayers = players;
	}

	/**
	 * Retrieves the maximum number of players allowed in a round at once.
	 *
	 * @return the maximum number of players allowed in a round at once
	 * @since 0.1.0
	 */
	public int getMaxPlayers() {
		return maxPlayers;
	}

	/**
	 * Sets the maximum number of players allowed in a round at once.
	 *
	 * @param players the maximum number of players allowed in a round at once
	 * @since 0.1.0
	 */
	public void setMaxPlayers(int players) {
		this.maxPlayers = players;
	}

	/**
	 * Creates a new LobbySign to be managed.
	 *
	 * @param location the location to create the sign at
	 * @param type     the type of the sign ("status" or "players")
	 * @param index    the number of the sign (applicable only for "players"
	 *                 signs)
	 * @throws NoSuchArenaException      if the specified arena does not exist
	 * @throws InvalidLocationException  if the specified location does not
	 *                                   contain a sign
	 * @throws IndexOutOfBoundsException if the specified index for a player
	 *                                   sign is less than 1
	 * @since 0.1.0
	 */
	public void addSign(Location location, LobbyType type, int index)
			throws NoSuchArenaException, InvalidLocationException, IndexOutOfBoundsException {
		this.getMinigame().getLobbyManager().add(location, this.getArena(), type, index);
	}

	/**
	 * Updates all lobby signs linked to this round's arena.
	 *
	 * @since 0.1.0
	 */
	public void updateSigns() {
		this.getMinigame().getLobbyManager().update(arena);
	}

	/**
	 * Retrieves this round's exit location.
	 *
	 * @return this round's exit location
	 * @deprecated Depends on Bukkit
	 * @since 0.1.0
	 */
	@Deprecated
	public Location getExitLocation() {
		return MGUtil.toBukkitLocation(this.exitLocation);
	}

	/**
	 * Sets this round's exit location.
	 *
	 * @param location the new exit location for this round
	 * @since 0.1.0
	 */
	public void setExitLocation(Location3D location) {
		this.exitLocation = location;
	}

	/**
	 * Sets this round's exit location.
	 *
	 * @param location the new exit location for this round
	 * @deprecated Depends on Bukkit. Use {@link Round#setExitLocation(Location3D)}.
	 * @since 0.1.0
	 */
	@Deprecated
	public void setExitLocation(Location location) {
		setExitLocation(MGUtil.fromBukkitLocation(location, true));
	}

	/**
	 * Retrieves whether PvP is allowed.
	 *
	 * @return whether PvP is allowed
	 * @since 0.1.0
	 */
	public boolean isPvPAllowed() {
		return pvp;
	}

	/**
	 * Sets whether PvP is allowed.
	 *
	 * @param allowed whether PvP is allowed
	 * @since 0.1.0
	 */
	public void setPvPAllowed(boolean allowed) {
		this.pvp = allowed;
	}

	/**
	 * Retrieves whether players in rounds may receive damage. (default:
	 * <code>true</code>)
	 *
	 * @return whether players in rounds may receive damage
	 * @since 0.1.0
	 */
	public boolean isDamageAllowed() {
		return damage;
	}

	/**
	 * Sets whether players in rounds may receive damage. (default:
	 * <code>false</code>)
	 *
	 * @param allowed whether players in rounds may receive damage
	 * @since 0.1.0
	 */
	public void setDamageAllowed(boolean allowed) {
		this.damage = allowed;
	}

	/**
	 * Retrieves whether rollback is enabled in this round.
	 *
	 * @return whether rollback is enabled in this round
	 * @since 0.2.0
	 */
	public boolean isRollbackEnabled() {
		return rollback;
	}

	/**
	 * Sets whether rollback is enabled by default.
	 *
	 * @param enabled whether rollback is enabled by default
	 * @since 0.2.0
	 */
	public void setRollbackEnabled(boolean enabled) {
		this.rollback = enabled;
	}

	/**
	 * Retrieves the {@link ConfigManager} of the plugin owning this round.
	 *
	 * @return the {@link ConfigManager} of the plugin owning this round
	 * @since 0.2.0
	 */
	public ConfigManager getConfigManager() {
		return getMinigame().getConfigManager();
	}

	/**
	 * Retrieves the {@link RollbackManager} of the plugin owning this round.
	 *
	 * @return the {@link RollbackManager} of the plugin owning this round
	 * @since 0.2.0
	 */
	public RollbackManager getRollbackManager() {
		return getMinigame().getRollbackManager();
	}

	/**
	 * Broadcasts a message to all players in this round.
	 *
	 * @param message               the message to broadcast
	 * @param broadcastToSpectators whether the message should be broadcast to
	 *                              spectators
	 * @since 0.2.0
	 */
	public void broadcast(String message, boolean broadcastToSpectators) {
		for (MGPlayer p : players.values()) {
			@SuppressWarnings("deprecation")
			Player bP = Bukkit.getPlayer(p.getName());
			if ((!p.isSpectating() || broadcastToSpectators) && bP != null) {
				bP.sendMessage(message);
			}
		}
	}

	/**
	 * Broadcasts a message to all players in this round.
	 *
	 * @param message the message to broadcast
	 * @since 0.2.0
	 */
	public void broadcast(String message) {
		broadcast(message, true);
	}

	/**
	 * Retrieves the display name of the round's arena.
	 *
	 * @return the display name of the round's arena
	 * @since 0.3.0
	 */
	public String getDisplayName() {
		return displayName;
	}

	public Object getMetadata(String key) {
		return metadata.get(key);
	}

	public void setMetadata(String key, Object value) {
		metadata.put(key, value);
	}

	public void removeMetadata(String key) {
		metadata.remove(key);
	}

	public boolean hasMetadata(String key) {
		return metadata.containsKey(key);
	}

	public HashMap<String, Object> getAllMetadata() {
		return metadata;
	}

	public boolean equals(Object p) {
		return p instanceof Round && arena.equals(((Round)p).getArena());
	}

	public int hashCode() {
		return 41 * (plugin.hashCode() + arena.hashCode() + 41);
	}
}
