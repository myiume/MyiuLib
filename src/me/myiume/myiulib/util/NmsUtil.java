package me.myiume.myiulib.util;

import me.myiume.myiulib.MGUtil;
import me.myiume.myiulib.MyiuLib;
import me.myiume.myiulib.api.LogLevel;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

/**
 * Utility methods for accessing NMS resources. This is not an API class and
 * thus is subject to non-backwards-compatible changes. It is <em>strongly</em>
 * advised that hooking plugins do not reference this class or its methods.
 *
 * @since 0.4.0
 */
public class NmsUtil {

	private static final String VERSION_STRING;
	private static final boolean NMS_SUPPORT;
	public static final boolean SPECTATOR_SUPPORT;

	// general classes for sending packets
	public static Method craftPlayer_getHandle;
	public static Field playerConnection;
	public static Method playerConnection_sendPacket;
	public static Method playerConnection_a_packetPlayInClientCommand;

	// for respawning players automatically
	public static Object clientCommandPacketInstance;

	private static Method getOnlinePlayers;
	public static boolean newOnlinePlayersMethod = false;

	static {
		boolean ss;
		try {
			GameMode.valueOf("SPECTATOR");
			ss = true;
		} catch (IllegalArgumentException ex) {
			ss = false;
		}
		SPECTATOR_SUPPORT = ss;
		boolean nmsException = false;
		String[] array = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
		VERSION_STRING = array.length == 4 ? array[3] + "." : "";
		try {
			getOnlinePlayers = Bukkit.class.getMethod("getOnlinePlayers");
			if (getOnlinePlayers.getReturnType() == Collection.class) {
				newOnlinePlayersMethod = true;
			}

			// get method for recieving CraftPlayer's EntityPlayer
			craftPlayer_getHandle = getCraftClass("entity.CraftPlayer").getMethod("getHandle");
			// get the PlayerConnection of the EntityPlayer
			playerConnection = getNmsClass("EntityPlayer").getDeclaredField("playerConnection");
			// method to send the packet
			playerConnection_sendPacket = getNmsClass("PlayerConnection").getMethod("sendPacket", getNmsClass("Packet"));
			playerConnection_a_packetPlayInClientCommand = getNmsClass("PlayerConnection")
					.getMethod("a", getNmsClass("PacketPlayInClientCommand"));

			try {
				try {
					@SuppressWarnings("unchecked")
					Object performRespawn = Enum.valueOf(
							(Class<? extends Enum>)getNmsClass("EnumClientCommand"), "PERFORM_RESPAWN"
					);
					clientCommandPacketInstance = getNmsClass("PacketPlayInClientCommand")
							.getConstructor(performRespawn.getClass())
							.newInstance(performRespawn);
				}
				catch (ClassNotFoundException ex) {
					clientCommandPacketInstance = getNmsClass("Packet205ClientCommand").getConstructor().newInstance();
					clientCommandPacketInstance.getClass().getDeclaredField("a").set(clientCommandPacketInstance, 1);
				}
			}
			catch (ClassNotFoundException ex) {
				MyiuLib.log("plugin.alert.nms.client-command", LogLevel.WARNING);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			MyiuLib.log(MyiuLib.locale.getMessage("plugin.alert.nms.fail"), LogLevel.WARNING);
			nmsException = true;
		}

		NMS_SUPPORT = !nmsException;
	}

	/**
	 * Retrieves a class by the given name from the package
	 * <code>net.minecraft.server</code>.
	 *
	 * @param name the class to retrieve
	 * @return the class object from the package
	 * <code>net.minecraft.server</code>
	 * @throws ClassNotFoundException if the class does not exist in the
	 * package
	 */
	public static Class<?> getNmsClass(String name) throws ClassNotFoundException {
		String className = "net.minecraft.server." + VERSION_STRING + name;
		return Class.forName(className);
	}

	/**
	 * Retrieves a class by the given name from the package
	 * <code>org.bukkit.craftbukkit</code>.
	 *
	 * @param name the class to retrieve
	 * @return the class object from the package
	 * <code>org.bukkit.craftbukkit</code>
	 * @throws ClassNotFoundException if the class does not exist in the
	 * package
	 */
	public static Class<?> getCraftClass(String name) throws ClassNotFoundException {
		String className = "org.bukkit.craftbukkit." + VERSION_STRING + name;
		return Class.forName(className);
	}

	/**
	 * Sends a PlayInClientCommand packet to the given player.
	 *
	 * @param player the {@link Player} to send the packet to
	 * @throws Exception if an exception occurs while sending the packet
	 */
	public static void sendRespawnPacket(Player player) throws Exception {
		if (NMS_SUPPORT) {
			Object nmsPlayer = NmsUtil.craftPlayer_getHandle.invoke(player);
			Object conn = NmsUtil.playerConnection.get(nmsPlayer);
			NmsUtil.playerConnection_a_packetPlayInClientCommand.invoke(conn, NmsUtil.clientCommandPacketInstance);
		}
	}

	/**
	 * Version-independent getOnlinePlayers() method.
	 *
	 * @return a list of online players
	 * @since 0.4.0
	 */
	@SuppressWarnings("unchecked")
	public static Collection<? extends Player> getOnlinePlayers() {
		try {
			if (newOnlinePlayersMethod) {
				return (Collection<? extends Player>)getOnlinePlayers.invoke(null);
			}
			else {
				return Arrays.asList((Player[])getOnlinePlayers.invoke(null));
			}
		}
		catch (IllegalAccessException ex) {
			ex.printStackTrace();
			MyiuLib.log(MyiuLib.locale.getMessage("plugin.alert.nms.online-players"), LogLevel.SEVERE);
		}
		catch (InvocationTargetException ex) {
			ex.printStackTrace();
			MyiuLib.log(MyiuLib.locale.getMessage("plugin.alert.nms.online-players"), LogLevel.SEVERE);
		}
		return null;
	}

	/**
	 * Unsets all static objects in this class.
	 *
	 * @throws UnsupportedOperationException if MyiuLib is not currently disabling
	 *
	 * @since 0.4.0
	 */
	public static void uninitialize() {
		MGUtil.verifyDisablingStatus();
		craftPlayer_getHandle = null;
		playerConnection = null;
		playerConnection_sendPacket = null;
	}
}
