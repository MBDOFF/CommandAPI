package dev.jorel.commandapi.nms;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_16_R2.CraftLootTable;
import org.bukkit.craftbukkit.v1_16_R2.CraftParticle;
import org.bukkit.craftbukkit.v1_16_R2.CraftServer;
import org.bukkit.craftbukkit.v1_16_R2.CraftSound;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R2.command.ProxiedNativeCommandSender;
import org.bukkit.craftbukkit.v1_16_R2.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.v1_16_R2.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R2.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.v1_16_R2.util.CraftChatMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.loot.LootTable;
import org.bukkit.potion.PotionEffectType;

import com.google.common.io.Files;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;

import de.tr7zw.nbtapi.NBTContainer;
import dev.jorel.commandapi.CommandAPIHandler;
import dev.jorel.commandapi.CommandAPIMain;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.ICustomProvidedArgument.SuggestionProviders;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.wrappers.FloatRange;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import dev.jorel.commandapi.wrappers.IntegerRange;
import dev.jorel.commandapi.wrappers.Location2D;
import dev.jorel.commandapi.wrappers.MathOperation;
import dev.jorel.commandapi.wrappers.Rotation;
import dev.jorel.commandapi.wrappers.ScoreboardSlot;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_16_R2.Advancement;
import net.minecraft.server.v1_16_R2.ArgumentBlockPredicate;
import net.minecraft.server.v1_16_R2.ArgumentChat;
import net.minecraft.server.v1_16_R2.ArgumentChatComponent;
import net.minecraft.server.v1_16_R2.ArgumentChatFormat;
import net.minecraft.server.v1_16_R2.ArgumentCriterionValue;
import net.minecraft.server.v1_16_R2.ArgumentDimension;
import net.minecraft.server.v1_16_R2.ArgumentEnchantment;
import net.minecraft.server.v1_16_R2.ArgumentEntity;
import net.minecraft.server.v1_16_R2.ArgumentEntitySummon;
import net.minecraft.server.v1_16_R2.ArgumentItemPredicate;
import net.minecraft.server.v1_16_R2.ArgumentItemStack;
import net.minecraft.server.v1_16_R2.ArgumentMathOperation;
import net.minecraft.server.v1_16_R2.ArgumentMinecraftKeyRegistered;
import net.minecraft.server.v1_16_R2.ArgumentMobEffect;
import net.minecraft.server.v1_16_R2.ArgumentNBTTag;
import net.minecraft.server.v1_16_R2.ArgumentParticle;
import net.minecraft.server.v1_16_R2.ArgumentPosition;
import net.minecraft.server.v1_16_R2.ArgumentProfile;
import net.minecraft.server.v1_16_R2.ArgumentRegistry;
import net.minecraft.server.v1_16_R2.ArgumentRotation;
import net.minecraft.server.v1_16_R2.ArgumentRotationAxis;
import net.minecraft.server.v1_16_R2.ArgumentScoreboardCriteria;
import net.minecraft.server.v1_16_R2.ArgumentScoreboardObjective;
import net.minecraft.server.v1_16_R2.ArgumentScoreboardSlot;
import net.minecraft.server.v1_16_R2.ArgumentScoreboardTeam;
import net.minecraft.server.v1_16_R2.ArgumentScoreholder;
import net.minecraft.server.v1_16_R2.ArgumentTag;
import net.minecraft.server.v1_16_R2.ArgumentTile;
import net.minecraft.server.v1_16_R2.ArgumentTime;
import net.minecraft.server.v1_16_R2.ArgumentUUID;
import net.minecraft.server.v1_16_R2.ArgumentVec2;
import net.minecraft.server.v1_16_R2.ArgumentVec2I;
import net.minecraft.server.v1_16_R2.ArgumentVec3;
import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.BlockPosition2D;
import net.minecraft.server.v1_16_R2.CommandListenerWrapper;
import net.minecraft.server.v1_16_R2.CompletionProviders;
import net.minecraft.server.v1_16_R2.CustomFunction;
import net.minecraft.server.v1_16_R2.CustomFunctionData;
import net.minecraft.server.v1_16_R2.CustomFunctionManager;
import net.minecraft.server.v1_16_R2.DataPackResources;
import net.minecraft.server.v1_16_R2.DedicatedServer;
import net.minecraft.server.v1_16_R2.Entity;
import net.minecraft.server.v1_16_R2.EnumDirection.EnumAxis;
import net.minecraft.server.v1_16_R2.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_16_R2.ICompletionProvider;
import net.minecraft.server.v1_16_R2.IRecipe;
import net.minecraft.server.v1_16_R2.IRegistry;
import net.minecraft.server.v1_16_R2.IReloadableResourceManager;
import net.minecraft.server.v1_16_R2.IVectorPosition;
import net.minecraft.server.v1_16_R2.LootTableRegistry;
import net.minecraft.server.v1_16_R2.MinecraftKey;
import net.minecraft.server.v1_16_R2.MinecraftServer;
import net.minecraft.server.v1_16_R2.ScoreboardScore;
import net.minecraft.server.v1_16_R2.ShapeDetectorBlock;
import net.minecraft.server.v1_16_R2.SystemUtils;
import net.minecraft.server.v1_16_R2.Unit;
import net.minecraft.server.v1_16_R2.Vec2F;
import net.minecraft.server.v1_16_R2.Vec3D;
import net.minecraft.server.v1_16_R2.WorldServer;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class NMS_1_16_R2 implements NMS {
	
	@Override
	public String convert(Sound sound) {
		return CraftSound.getSound(sound);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public String convert(PotionEffectType potion) {
		return IRegistry.MOB_EFFECT.getKey(IRegistry.MOB_EFFECT.fromId(potion.getId())).toString();
	}
	
	@Override
	public String convert(Particle particle) {
		return CraftParticle.toNMS(particle).a();
	}
	
	@Override
	public String convert(ItemStack is) {
		net.minecraft.server.v1_16_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(is);
		return is.getType().getKey().toString() + nmsItemStack.getOrCreateTag().asString();
	}

	@Override
	public void reloadDataPacks()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		CommandAPIMain.getLog().info("Reloading datapacks...");

		// Get the NMS server
		DedicatedServer server = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
		
		// Get previously declared recipes to be re-registered later
		Iterator<Recipe> recipes = Bukkit.recipeIterator();

		// Update the commandDispatcher with the current server's commandDispatcher
		DataPackResources datapackResources = server.dataPackResources;
		datapackResources.commandDispatcher = server.getCommandDispatcher();

		// Reflection doesn't need to be cached because this only executes once at
		// server startup
		Field i = DataPackResources.class.getDeclaredField("i");
		i.setAccessible(true);

		Field gField = CustomFunctionManager.class.getDeclaredField("g");
		gField.setAccessible(true);
		int g = (int) gField.get(datapackResources.a()); // Related to the permission required to run this function?

		// Update the CustomFunctionManager for the datapackResources which now has the
		// new commandDispatcher
		i.set(datapackResources, new CustomFunctionManager(g, datapackResources.commandDispatcher.a()));

		// Construct the new CompletableFuture that now uses datapackResources
		Field b = DataPackResources.class.getDeclaredField("b");
		b.setAccessible(true);
		IReloadableResourceManager reloadableResourceManager = (IReloadableResourceManager) b.get(datapackResources);
		Field a = DataPackResources.class.getDeclaredField("a");
		a.setAccessible(true);

		CompletableFuture<Unit> unit = (CompletableFuture<Unit>) a.get(null);
		CompletableFuture<Unit> unitCompletableFuture = reloadableResourceManager.a(SystemUtils.f(), Runnable::run,
				server.getResourcePackRepository().f(), unit);

		CompletableFuture<DataPackResources> completablefuture = unitCompletableFuture
				.whenComplete((Unit u, Throwable t) -> {
					if (t != null) {
						datapackResources.close();
					}

				}).thenApply((Unit u) -> {
					return datapackResources;
				});

		// Run the completableFuture and bind tags
		try {
			((DataPackResources) completablefuture.get()).i();
			
			// Register recipes again because reloading datapacks removes all non-vanilla recipes
			recipes.forEachRemaining(recipe -> {
				try {
					Bukkit.addRecipe(recipe);
					if(recipe instanceof Keyed) {
						CommandAPIMain.getLog().info("Re-registering recipe: " + ((Keyed) recipe).getKey());
					}
				} catch(Exception e) {
					// Can't re-register registered recipes. Not an error. 
				}
			});
			
			CommandAPIMain.getLog().info("Finished reloading datapacks");
		} catch (Exception e) {
			CommandAPIMain.getLog().log(Level.WARNING,
					"Failed to load datapacks, can't proceed with normal server load procedure. Try fixing your datapacks?",
					e);
		}
	}

	@Override
	public ArgumentType<?> _ArgumentAxis() {
		return ArgumentRotationAxis.a();
	}

	@Override
	public ArgumentType<?> _ArgumentBlockState() {
		return ArgumentTile.a();
	}

	@Override
	public ArgumentType<?> _ArgumentChat() {
		return ArgumentChat.a();
	}

	@Override
	public ArgumentType _ArgumentChatComponent() {
		return ArgumentChatComponent.a();
	}

	@Override
	public ArgumentType _ArgumentChatFormat() {
		return ArgumentChatFormat.a();
	}

	@Override
	public ArgumentType<?> _ArgumentDimension() {
		return ArgumentDimension.a();
	}

	@Override
	public ArgumentType _ArgumentEnchantment() {
		return ArgumentEnchantment.a();
	}

	@Override
	public ArgumentType _ArgumentEntity(EntitySelector selector) {
		switch (selector) {
		case MANY_ENTITIES:
			return ArgumentEntity.multipleEntities();
		case MANY_PLAYERS:
			return ArgumentEntity.d();
		case ONE_ENTITY:
			return ArgumentEntity.a();
		case ONE_PLAYER:
			return ArgumentEntity.c();
		}
		return null;
	}

	@Override
	public ArgumentType _ArgumentEntitySummon() {
		return ArgumentEntitySummon.a();
	}

	@Override
	public ArgumentType<?> _ArgumentFloatRange() {
		return new ArgumentCriterionValue.a();
	}

	@Override
	public ArgumentType<?> _ArgumentIntRange() {
		return new ArgumentCriterionValue.b();
	}

	@Override
	public ArgumentType _ArgumentItemStack() {
		return ArgumentItemStack.a();
	}

	@Override
	public ArgumentType<?> _ArgumentMathOperation() {
		return ArgumentMathOperation.a();
	}

	@Override
	public ArgumentType _ArgumentMinecraftKeyRegistered() {
		return ArgumentMinecraftKeyRegistered.a();
	}

	@Override
	public ArgumentType _ArgumentMobEffect() {
		return ArgumentMobEffect.a();
	}

	@Override
	public ArgumentType<?> _ArgumentNBTCompound() {
		return ArgumentNBTTag.a();
	}

	@Override
	public ArgumentType _ArgumentParticle() {
		return ArgumentParticle.a();
	}

	@Override
	public ArgumentType _ArgumentPosition() {
		return ArgumentPosition.a();
	}

	@Override
	public ArgumentType<?> _ArgumentPosition2D() {
		return ArgumentVec2I.a();
	}

	@Override
	public ArgumentType _ArgumentProfile() {
		return ArgumentProfile.a();
	}

	@Override
	public ArgumentType<?> _ArgumentRotation() {
		return ArgumentRotation.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreboardCriteria() {
		return ArgumentScoreboardCriteria.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreboardObjective() {
		return ArgumentScoreboardObjective.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreboardSlot() {
		return ArgumentScoreboardSlot.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreboardTeam() {
		return ArgumentScoreboardTeam.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreholder(boolean single) {
		return single ? ArgumentScoreholder.a() : ArgumentScoreholder.b();
	}

	@Override
	public ArgumentType _ArgumentTag() {
		return ArgumentTag.a();
	}

	@Override
	public ArgumentType<?> _ArgumentTime() {
		return ArgumentTime.a();
	}

	@Override
	public ArgumentType<?> _ArgumentVec2() {
		return ArgumentVec2.a();
	}

	@Override
	public ArgumentType _ArgumentVec3() {
		return ArgumentVec3.a();
	}

	@Override
	public String[] compatibleVersions() {
		return new String[] { "1.16.2" };
	}

	@Override
	public void createDispatcherFile(File file, CommandDispatcher dispatcher) throws IOException {
		Files.write((new GsonBuilder()).setPrettyPrinting().create()
				.toJson(ArgumentRegistry.a(dispatcher, dispatcher.getRoot())), file, StandardCharsets.UTF_8);
	}

	@Override
	public org.bukkit.advancement.Advancement getAdvancement(CommandContext cmdCtx, String key)
			throws CommandSyntaxException {
		return ArgumentMinecraftKeyRegistered.a(cmdCtx, key).bukkit;
	}

	@Override
	public EnumSet<Axis> getAxis(CommandContext cmdCtx, String key) {
		EnumSet<Axis> set = EnumSet.noneOf(Axis.class);
		EnumSet<EnumAxis> parsedEnumSet = ArgumentRotationAxis.a(cmdCtx, key);
		for (EnumAxis element : parsedEnumSet) {
			switch (element) {
			case X:
				set.add(Axis.X);
				break;
			case Y:
				set.add(Axis.Y);
				break;
			case Z:
				set.add(Axis.Z);
				break;
			}
		}
		return set;
	}

	@Override
	public Biome getBiome(CommandContext cmdCtx, String key) {
		MinecraftKey minecraftKey = (MinecraftKey) cmdCtx.getArgument(key, MinecraftKey.class);
		return Biome.valueOf(minecraftKey.getKey().toUpperCase());
	}

	@Override
	public BlockData getBlockState(CommandContext cmdCtx, String key) {
		return CraftBlockData.fromData(ArgumentTile.a(cmdCtx, key).a());
	}

	@Override
	public CommandDispatcher getBrigadierDispatcher(Object server) {
		return ((MinecraftServer) server).getCommandDispatcher().a();
	}

	@Override
	public BaseComponent[] getChat(CommandContext cmdCtx, String key) throws CommandSyntaxException {
		String resultantString = ChatSerializer.a(ArgumentChat.a(cmdCtx, key));
		return ComponentSerializer.parse(resultantString);
	}

	@Override
	public ChatColor getChatColor(CommandContext cmdCtx, String str) {
		return CraftChatMessage.getColor(ArgumentChatFormat.a(cmdCtx, str));
	}

	@Override
	public BaseComponent[] getChatComponent(CommandContext cmdCtx, String str) {
		String resultantString = ChatSerializer.a(ArgumentChatComponent.a(cmdCtx, str));
		return ComponentSerializer.parse((String) resultantString);
	}

	private CommandListenerWrapper getCLW(CommandContext cmdCtx) {
		return (CommandListenerWrapper) cmdCtx.getSource();
	}

	@Override
	public CommandSender getCommandSenderForCLW(Object clw) {
		try {
			return ((CommandListenerWrapper) clw).getBukkitSender();
		} catch (UnsupportedOperationException e) {
			return null;
		}
	}

	@Override
	public Environment getDimension(CommandContext cmdCtx, String key) throws CommandSyntaxException {
		WorldServer worldServer = ArgumentDimension.a(cmdCtx, key);
		return worldServer.getWorld().getEnvironment();
	}

	@Override
	public Enchantment getEnchantment(CommandContext cmdCtx, String str) {
		return new CraftEnchantment(ArgumentEnchantment.a(cmdCtx, str));
	}

	@Override
	public Object getEntitySelector(CommandContext cmdCtx, String str, EntitySelector selector)
			throws CommandSyntaxException {
		switch (selector) {
		case MANY_ENTITIES:
			try {
				return ArgumentEntity.c(cmdCtx, str).stream()
						.map(entity -> (org.bukkit.entity.Entity) ((Entity) entity).getBukkitEntity())
						.collect(Collectors.toList());
			} catch (CommandSyntaxException e) {
				return new ArrayList<org.bukkit.entity.Entity>();
			}
		case MANY_PLAYERS:
			try {
				return ArgumentEntity.d(cmdCtx, str).stream()
						.map(player -> (Player) ((Entity) player).getBukkitEntity()).collect(Collectors.toList());
			} catch (CommandSyntaxException e) {
				return new ArrayList<Player>();
			}
		case ONE_ENTITY:
			return (org.bukkit.entity.Entity) ArgumentEntity.a(cmdCtx, str).getBukkitEntity();
		case ONE_PLAYER:
			return (Player) ArgumentEntity.e(cmdCtx, str).getBukkitEntity();
		}
		return null;
	}

	@Override
	public EntityType getEntityType(CommandContext cmdCtx, String str, CommandSender sender)
			throws CommandSyntaxException {
		Entity entity = IRegistry.ENTITY_TYPE.get(ArgumentEntitySummon.a(cmdCtx, str))
				.a(((CraftWorld) NMS.getCommandSenderWorld(sender)).getHandle());
		return entity.getBukkitEntity().getType();
	}

	@Override
	public FloatRange getFloatRange(CommandContext cmdCtx, String key) {
		net.minecraft.server.v1_16_R2.CriterionConditionValue.FloatRange.FloatRange range = (net.minecraft.server.v1_16_R2.CriterionConditionValue.FloatRange.FloatRange) cmdCtx
				.getArgument(key, net.minecraft.server.v1_16_R2.CriterionConditionValue.FloatRange.FloatRange.class);
		float low = range.a() == null ? -Float.MAX_VALUE : range.a();
		float high = range.b() == null ? Float.MAX_VALUE : range.b();
		return new FloatRange(low, high);
	}

	@Override
	public FunctionWrapper[] getFunction(CommandContext cmdCtx, String str) throws CommandSyntaxException {
		Collection<CustomFunction> customFuncList = ArgumentTag.a(cmdCtx, str);

		FunctionWrapper[] result = new FunctionWrapper[customFuncList.size()];

		CustomFunctionData customFunctionData = getCLW(cmdCtx).getServer().getFunctionData();
		CommandListenerWrapper commandListenerWrapper = getCLW(cmdCtx).a().b(2);

		int count = 0;

		for (CustomFunction customFunction : customFuncList) {
			@SuppressWarnings("deprecation")
			NamespacedKey minecraftKey = new NamespacedKey(customFunction.a().getNamespace(),
					customFunction.a().getKey());
			ToIntBiFunction<CustomFunction, CommandListenerWrapper> obj = customFunctionData::a;
			ToIntFunction<CommandListenerWrapper> appliedObj = clw -> obj.applyAsInt(customFunction, clw);

			FunctionWrapper wrapper = new FunctionWrapper(minecraftKey, appliedObj, commandListenerWrapper,
					e -> {
						return (Object) getCLW(cmdCtx).a(((CraftEntity) e).getHandle());
					}, Arrays.stream(customFunction.b()).map(Object::toString).toArray(String[]::new));

			result[count] = wrapper;
			count++;
		}

		return result;
	}

	@Override
	public IntegerRange getIntRange(CommandContext cmdCtx, String key) {
		net.minecraft.server.v1_16_R2.CriterionConditionValue.IntegerRange range = ArgumentCriterionValue.b.a(cmdCtx,
				key);
		int low = range.a() == null ? Integer.MIN_VALUE : range.a();
		int high = range.b() == null ? Integer.MAX_VALUE : range.b();
		return new IntegerRange(low, high);
	}

	@Override
	public ItemStack getItemStack(CommandContext cmdCtx, String str) throws CommandSyntaxException {
		return CraftItemStack.asBukkitCopy(ArgumentItemStack.a(cmdCtx, str).a(1, false));
	}

	@Override
	public Location getLocation(CommandContext cmdCtx, String str, LocationType locationType, CommandSender sender)
			throws CommandSyntaxException {
		switch (locationType) {
		case BLOCK_POSITION:
			BlockPosition blockPos = ArgumentPosition.a(cmdCtx, str);
			return new Location(NMS.getCommandSenderWorld(sender), blockPos.getX(), blockPos.getY(), blockPos.getZ());
		case PRECISE_POSITION:
			Vec3D vecPos = ArgumentVec3.a(cmdCtx, str);
			return new Location(NMS.getCommandSenderWorld(sender), vecPos.x, vecPos.y, vecPos.z);
		}
		return null;
	}

	@Override
	public Location2D getLocation2D(CommandContext cmdCtx, String key, LocationType locationType2d,
			CommandSender sender) throws CommandSyntaxException {
		switch (locationType2d) {
		case BLOCK_POSITION:
			BlockPosition2D blockPos = ArgumentVec2I.a(cmdCtx, key);
			return new Location2D(NMS.getCommandSenderWorld(sender), blockPos.a, blockPos.b);
		case PRECISE_POSITION:
			Vec2F vecPos = ArgumentVec2.a(cmdCtx, key);
			return new Location2D(NMS.getCommandSenderWorld(sender), vecPos.i, vecPos.j);
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public LootTable getLootTable(CommandContext cmdCtx, String str) {
		MinecraftKey minecraftKey = ArgumentMinecraftKeyRegistered.e(cmdCtx, str);
		String namespace = minecraftKey.getNamespace();
		String key = minecraftKey.getKey();
		
		net.minecraft.server.v1_16_R2.LootTable lootTable = getCLW(cmdCtx).getServer().getLootTableRegistry()
				.getLootTable(minecraftKey);
		return new CraftLootTable(new NamespacedKey(namespace, key), lootTable);
	}

	@Override
	public MathOperation getMathOperation(CommandContext cmdCtx, String key) throws CommandSyntaxException {
		ArgumentMathOperation.a result = ArgumentMathOperation.a(cmdCtx, key);
		net.minecraft.server.v1_16_R2.Scoreboard board = new net.minecraft.server.v1_16_R2.Scoreboard();
		ScoreboardScore tester_left = new ScoreboardScore(board, null, null);
		ScoreboardScore tester_right = new ScoreboardScore(board, null, null);

		tester_left.setScore(6);
		tester_right.setScore(2);
		result.apply(tester_left, tester_right);

		switch (tester_left.getScore()) {
		case 8:
			return MathOperation.ADD;
		case 4:
			return MathOperation.SUBTRACT;
		case 12:
			return MathOperation.MULTIPLY;
		case 3:
			return MathOperation.DIVIDE;
		case 0:
			return MathOperation.MOD;
		case 6:
			return MathOperation.MAX;

		case 2: {
			if (tester_right.getScore() == 6)
				return MathOperation.SWAP;
			tester_left.setScore(2);
			tester_right.setScore(6);
			result.apply(tester_left, tester_right);
			if (tester_left.getScore() == 2)
				return MathOperation.MIN;
			return MathOperation.ASSIGN;
		}
		}
		return null;
	}

	@Override
	public NBTContainer getNBTCompound(CommandContext cmdCtx, String key) {
		return new NBTContainer(ArgumentNBTTag.a(cmdCtx, key));
	}

	@Override
	public String getObjective(CommandContext cmdCtx, String key, CommandSender sender)
			throws IllegalArgumentException, CommandSyntaxException {
		return ArgumentScoreboardObjective.a(cmdCtx, key).getName();
	}

	@Override
	public String getObjectiveCriteria(CommandContext cmdCtx, String key) {
		return ArgumentScoreboardCriteria.a(cmdCtx, key).getName();
	}

	@Override
	public Particle getParticle(CommandContext cmdCtx, String str) {
		return CraftParticle.toBukkit(ArgumentParticle.a(cmdCtx, str));
	}

	@Override
	public Player getPlayer(CommandContext cmdCtx, String str) throws CommandSyntaxException {
		Player target = Bukkit.getPlayer(((GameProfile) ArgumentProfile.a(cmdCtx, str).iterator().next()).getId());
		if (target == null) {
			throw ArgumentProfile.a.create();
		} else {
			return target;
		}
	}

	@Override
	public PotionEffectType getPotionEffect(CommandContext cmdCtx, String str) throws CommandSyntaxException {
		return new CraftPotionEffectType(ArgumentMobEffect.a(cmdCtx, str));
	}

	@Override
	public ComplexRecipe getRecipe(CommandContext cmdCtx, String key) throws CommandSyntaxException {
		IRecipe<?> recipe = ArgumentMinecraftKeyRegistered.b(cmdCtx, key);
		return new ComplexRecipe() {

			@SuppressWarnings("deprecation")
			@Override
			public NamespacedKey getKey() {
				return new NamespacedKey(recipe.getKey().getNamespace(), recipe.getKey().getKey());
			}

			@Override
			public ItemStack getResult() {
				return recipe.toBukkitRecipe().getResult();
			}
		};
	}

	@Override
	public Rotation getRotation(CommandContext cmdCtx, String key) {
		IVectorPosition pos = ArgumentRotation.a(cmdCtx, key);
		Vec2F vec = pos.b(getCLW(cmdCtx));
		return new Rotation(vec.i, vec.j);
	}

	@Override
	public ScoreboardSlot getScoreboardSlot(CommandContext cmdCtx, String key) {
		return new ScoreboardSlot(ArgumentScoreboardSlot.a(cmdCtx, key));
	}

	@Override
	public Collection<String> getScoreHolderMultiple(CommandContext cmdCtx, String key) throws CommandSyntaxException {
		return ArgumentScoreholder.b(cmdCtx, key);
	}

	@Override
	public String getScoreHolderSingle(CommandContext cmdCtx, String key) throws CommandSyntaxException {
		return ArgumentScoreholder.a(cmdCtx, key);
	}

	@Override
	public CommandSender getSenderForCommand(CommandContext cmdCtx) {
		CommandSender sender = getCLW(cmdCtx).getBukkitSender();

		Entity proxyEntity = getCLW(cmdCtx).getEntity();
		if (proxyEntity != null) {
			CommandSender proxy = ((Entity) proxyEntity).getBukkitEntity();

			if (!proxy.equals(sender)) {
				sender = new ProxiedNativeCommandSender(getCLW(cmdCtx), sender, proxy);
			}
		}

		return sender;
	}

	@Override
	public SimpleCommandMap getSimpleCommandMap() {
		return ((CraftServer) Bukkit.getServer()).getCommandMap();
	}

	@Override
	public Sound getSound(CommandContext cmdCtx, String key) {
		MinecraftKey minecraftKey = ArgumentMinecraftKeyRegistered.e(cmdCtx, key);
		for (CraftSound sound : CraftSound.values()) {
			try {
				if (CommandAPIHandler.getField(CraftSound.class, "minecraftKey").get(sound)
						.equals(minecraftKey.getKey())) {
					return Sound.valueOf(sound.name());
				}
			} catch (IllegalArgumentException | IllegalAccessException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public SuggestionProvider getSuggestionProvider(SuggestionProviders provider) {
		switch (provider) {
		case FUNCTION:
			return (context, builder) -> {
				CustomFunctionData functionData = getCLW(context).getServer().getFunctionData();
				ICompletionProvider.a(functionData.g(), builder, "#");
				return ICompletionProvider.a(functionData.f(), builder);
			};
		case RECIPES:
			return CompletionProviders.b;
		case SOUNDS:
			return CompletionProviders.c;
		case ADVANCEMENTS:
			return (cmdCtx, builder) -> {
				Collection<Advancement> advancements = ((CommandListenerWrapper) cmdCtx.getSource()).getServer()
						.getAdvancementData().getAdvancements();
				return ICompletionProvider.a(advancements.stream().map(Advancement::getName), builder);
			};
		case LOOT_TABLES:
			return (context, builder) -> {
				LootTableRegistry lootTables = getCLW(context).getServer().getLootTableRegistry();
				return ICompletionProvider.a(lootTables.a(), builder);
			};
		case BIOMES:
			return CompletionProviders.d;
		case ENTITIES:
			return CompletionProviders.e;
		default:
			return (context, builder) -> Suggestions.empty();
		}
	}

	@Override
	public String getTeam(CommandContext cmdCtx, String key, CommandSender sender) throws CommandSyntaxException {
		return ArgumentScoreboardTeam.a(cmdCtx, key).getName();
	}

	@Override
	public int getTime(CommandContext cmdCtx, String key) {
		return (Integer) cmdCtx.getArgument(key, Integer.class);
	}

	@Override
	public boolean isVanillaCommandWrapper(Command command) {
		return command instanceof VanillaCommandWrapper;
	}

	@Override
	public void resendPackets(Player player) {
		CraftPlayer craftPlayer = (CraftPlayer) player;
		CraftServer craftServer = (CraftServer) Bukkit.getServer();
		net.minecraft.server.v1_16_R2.CommandDispatcher nmsDispatcher = craftServer.getServer().getCommandDispatcher();
		nmsDispatcher.a(craftPlayer.getHandle());
	}

	@Override
	public ArgumentType<?> _ArgumentUUID() {
		return ArgumentUUID.a();
	}
	
	@Override
	public UUID getUUID(CommandContext cmdCtx, String key) {
		return ArgumentUUID.a(cmdCtx, key);
	}

	@Override
	public ArgumentType<?> _ArgumentItemPredicate() {
		return ArgumentItemPredicate.a();
	}

	@Override
	public Predicate<ItemStack> getItemStackPredicate(CommandContext cmdCtx, String key) throws CommandSyntaxException {
		Predicate<net.minecraft.server.v1_16_R2.ItemStack> predicate = ArgumentItemPredicate.a(cmdCtx, key);
		return (ItemStack item) -> predicate.test(CraftItemStack.asNMSCopy(item));
	}

	@Override
	public ArgumentType<?> _ArgumentBlockPredicate() {
		return ArgumentBlockPredicate.a();
	}

	@Override
	public Predicate<Block> getBlockPredicate(CommandContext cmdCtx, String key) throws CommandSyntaxException {
		Predicate<ShapeDetectorBlock> predicate = ArgumentBlockPredicate.a(cmdCtx, key);
		return (Block block) -> {
			return predicate.test(new ShapeDetectorBlock(getCLW(cmdCtx).getWorld(),
					new BlockPosition(block.getX(), block.getY(), block.getZ()), true));
		};
	}

	@Override
	public String getKeyedAsString(CommandContext cmdCtx, String key) throws CommandSyntaxException {
		MinecraftKey minecraftKey = ArgumentMinecraftKeyRegistered.e(cmdCtx, key);
		return minecraftKey.toString();
	}

}