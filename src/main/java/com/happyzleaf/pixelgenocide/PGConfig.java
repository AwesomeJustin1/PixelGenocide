package com.happyzleaf.pixelgenocide;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.enums.EnumPokemon;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PGConfig {
	private static ConfigurationLoader<CommentedConfigurationNode> loader;
	private static CommentedConfigurationNode node;
	private static File file;
	
	public static GameTime timer = new GameTime(10, TimeUnit.MINUTES);
	public static String messageTimer = "&4All non-special pixelmon will despawn in &c%timer%&4.";
	private static String messageCleaned = "&7%quantity% pixelmon have been cleaned.";
	private static int maxSpecialPlayerBlocks = 100;
	
	private static boolean keepLegendaries = true;
	private static boolean keepBosses = true;
	private static boolean keepShinies = true;
	private static boolean keepWithPokerus = true;
	private static boolean keepWithParticles = false;
	private static boolean keepWithinSpecialPlayer = false;
	private static List<String> whitelist = new ArrayList<>();
	private static List<String> blacklist = new ArrayList<>();
	
	static {
		whitelist.add(EnumPokemon.Pikachu.name);
		whitelist.add(EnumPokemon.Eevee.name);
		whitelist.add(EnumPokemon.Ditto.name);
		
		blacklist.add(EnumPokemon.Zubat.name);
		blacklist.add(EnumPokemon.Geodude.name);
		blacklist.add(EnumPokemon.Caterpie.name);
	}
	
	public static void init(ConfigurationLoader<CommentedConfigurationNode> loader, File file) {
		PGConfig.loader = loader;
		PGConfig.file = file;
		
		TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(GameTime.class), new GameTime.Serializer());
		
		loadConfig();
	}
	
	public static void loadConfig() {
		if (!file.exists()) {
			saveConfig();
		}
		
		load();
		
		ConfigurationNode miscellaneous = node.getNode("miscellaneous");
		try {
			timer = miscellaneous.getNode("timer").getValue(TypeToken.of(GameTime.class));
		} catch (ObjectMappingException e) {
			e.printStackTrace();
		}
		maxSpecialPlayerBlocks = miscellaneous.getNode("maxSpecialPlayerBlocks").getInt();
		
		ConfigurationNode message = miscellaneous.getNode("message");
		messageTimer = message.getNode("timer").getString();
		messageCleaned = message.getNode("cleaned").getString();
		
		CommentedConfigurationNode keep = node.getNode("keep");
		keepLegendaries = keep.getNode("legendaries").getBoolean();
		keepBosses = keep.getNode("bosses").getBoolean();
		keepShinies = keep.getNode("shinies").getBoolean();
		if (keep.getNode("withPokerus").isVirtual()) { //Forcing the new config to be generated. One day, i'll write a config library for these kind of things, but not today.
			keep.getNode("withPokerus").setValue(false);
			save();
			
			PixelGenocide.LOGGER.info("\"keep.withPokerus\" has been added to your config, please go take a look!");
		}
		keepWithPokerus = keep.getNode("withPokerus").getBoolean();
		keepWithParticles = keep.getNode("withParticles").getBoolean();
		if (keepWithParticles) {
			PluginContainer ep = Sponge.getPluginManager().getPlugin("entity-particles").orElse(null);
			if (ep == null) {
				PixelGenocide.LOGGER.info("entity-particles was not found, the support (most likely) won't work.");
			} else {
				if (ep.getVersion().orElse("").equals("2.1")) {
					PixelGenocide.LOGGER.info("entity-particles found, the support has been enabled.");
					keepWithParticles = false;
				} else {
					PixelGenocide.LOGGER.info("entity-particles found, but it's an untested version, please set \"keep.withParticles\" to \"false\" if you encounter any problem.");
				}
			}
		}
		keepWithinSpecialPlayer = keep.getNode("withinSpecialPlayer").getBoolean();
		try {
			whitelist = keep.getNode("whitelist").getList(TypeToken.of(String.class));
			blacklist = keep.getNode("blacklist").getList(TypeToken.of(String.class));
		} catch (ObjectMappingException e) {
			e.printStackTrace();
		}
	}
	
	public static void saveConfig() {
		load();
		
		CommentedConfigurationNode miscellaneous = node.getNode("miscellaneous");
		try {
			miscellaneous.getNode("timer").setValue(new TypeToken<GameTime>() {}, timer);
		} catch (ObjectMappingException e) {
			e.printStackTrace();
		}
		miscellaneous.getNode("maxSpecialPlayerBlocks").setComment("How many blocks the pixelmon will not be removed within a special player. See keep.withinSpecialPlayer for more details.").setValue(maxSpecialPlayerBlocks);
		
		CommentedConfigurationNode message = miscellaneous.getNode("message");
		message.getNode("timer").setComment("Placeholders: %timer%.").setValue(messageTimer);
		message.getNode("cleaned").setComment("Placeholders: %quantity%.").setValue(messageCleaned);
		
		CommentedConfigurationNode keep = node.getNode("keep").setComment("Whether the pixelmon should be kept.");
		keep.getNode("legendaries").setValue(keepLegendaries);
		keep.getNode("bosses").setValue(keepBosses);
		keep.getNode("shinies").setValue(keepShinies);
		keep.getNode("withPokerus").setValue(keepWithPokerus);
		keep.getNode("withParticles").setComment("You will need entity-particles for this to work.").setValue(keepWithParticles);
		keep.getNode("withinSpecialPlayer").setComment("The pixelmon will not be cleared if they're near a player with the permission '" + PixelGenocide.PLUGIN_ID + ".specialplayer'. WARNING: Could cost performance.").setValue(keepWithinSpecialPlayer);
		keep.getNode("whitelist").setComment("Keep these pixelmon regardless their specs.").setValue(whitelist);
		keep.getNode("blacklist").setComment("Remove these pixelmon regardless their specs.").setValue(blacklist);
		
		save();
	}
	
	private static void load() {
		try {
			node = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void save() {
		try {
			loader.save(node);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Text getMessageCleaned(int quantity) {
		return TextSerializers.FORMATTING_CODE.deserialize(PGConfig.messageCleaned.replace("%quantity%", "" + quantity).replace("HAVEHAS", quantity == 1 ? "have" : "has"));
	}
	
	public static boolean shouldKeepPokemon(EntityPixelmon pixelmon) {
		String name = pixelmon.getPokemonName();
		return whitelist.contains(name)
				|| !blacklist.contains(name)
				&& (keepLegendaries && EnumPokemon.legendaries.contains(name)
				|| keepBosses && pixelmon.isBossPokemon()
				|| keepShinies && pixelmon.getIsShiny()
				|| keepWithPokerus && pixelmon.getPokerus().isPresent()
				|| keepWithParticles && hasParticles((Entity) pixelmon)
				|| keepWithinSpecialPlayer && isWithinSpecialPlayer(pixelmon));
	}
	
	private static boolean hasParticles(Entity entity) {
		return entity.getKeys().stream().anyMatch(key -> key.getId().equals("entity-particles:id")); //Will provide support for "active" value later
	}
	
	private static boolean isWithinSpecialPlayer(net.minecraft.entity.Entity entity) {
		Vector3d pos = new Vector3d(entity.posX, entity.posY, entity.posZ);
		for (Player player : Sponge.getServer().getOnlinePlayers().stream().filter(player -> player.hasPermission(PixelGenocide.PLUGIN_ID + ".specialplayer")).collect(Collectors.toList())) {
			if (player.getLocation().getPosition().distance(pos) <= maxSpecialPlayerBlocks) { //Player#getPosition() drops the support for API 7.0.0
				return true;
			}
		}
		return false;
	}
}
