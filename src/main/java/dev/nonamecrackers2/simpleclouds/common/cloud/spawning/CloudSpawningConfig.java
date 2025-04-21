package dev.nonamecrackers2.simpleclouds.common.cloud.spawning;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;

//TODO: Add a max of each cloud type in region option?
public class CloudSpawningConfig
{
	public static final CloudSpawningConfig EMPTY = new CloudSpawningConfig(ConstantInt.ZERO, 0, 0, ImmutableMap.of());
	private final IntProvider spawnInterval;
	private final int maxCloudRegions;
	private final int maxInitialRegions;
	private final Map<ResourceLocation, CloudSpawningConfig.Info> weightsPerType;
	private final WeightedRandomList<CloudSpawningConfig.Info> weights;
	
	private CloudSpawningConfig(IntProvider spawnInterval, int maxCloudRegions, int maxInitialRegions, Map<ResourceLocation, CloudSpawningConfig.Info> weightsPerType)
	{
		this.spawnInterval = spawnInterval;
		this.maxCloudRegions = maxCloudRegions;
		this.maxInitialRegions = maxInitialRegions;
		this.weightsPerType = weightsPerType;
		this.weights = WeightedRandomList.create(Lists.newArrayList(weightsPerType.values()));
	}
	
	public static CloudSpawningConfig of(IntProvider spawnInterval, int maxCloudRegions, int maxInitialRegions, ImmutableMap<ResourceLocation, CloudSpawningConfig.Info> weightsPerType)
	{
		if (weightsPerType.isEmpty())
			return EMPTY;
		return new CloudSpawningConfig(spawnInterval, maxCloudRegions, maxInitialRegions, weightsPerType);
	}
	
	public static CloudSpawningConfig fromJson(CloudTypeSource typeValidator, JsonObject object, ImmutableMap<ResourceLocation, CloudSpawningConfig.Info> entries) throws JsonSyntaxException, NullPointerException, IllegalArgumentException
	{
		if (entries.isEmpty())
			return EMPTY;
		IntProvider spawnInterval = IntProvider.NON_NEGATIVE_CODEC.parse(JsonOps.INSTANCE, Objects.requireNonNull(object.get("spawn_interval"))).resultOrPartial(e -> {
			throw new JsonSyntaxException(e);
		}).get();
		int maxCloudRegions = GsonHelper.getAsInt(object, "max_formations");
		int maxInitialRegions = GsonHelper.getAsInt(object, "max_initial_formations");
		if (maxCloudRegions > SimpleCloudsConstants.MAX_CLOUD_FORMATIONS || maxInitialRegions > SimpleCloudsConstants.MAX_CLOUD_FORMATIONS)
			throw new IllegalArgumentException("Maximum cloud formations is " + SimpleCloudsConstants.MAX_CLOUD_FORMATIONS);
//		ImmutableMap.Builder<ResourceLocation, CloudSpawningConfig.Info> values = ImmutableMap.builder();
//		JsonArray entries = GsonHelper.getAsJsonArray(object, "entries");
//		for (JsonElement element : entries)
//		{
//			JsonObject entry = GsonHelper.convertToJsonObject(element, "entry");
//			CloudSpawningConfig.Info info = readInfo(typeValidator, entry);
//			values.put(info.cloudType, info);
//		}
//		ImmutableMap<ResourceLocation, CloudSpawningConfig.Info> map = values.buildOrThrow();
		return new CloudSpawningConfig(spawnInterval, maxCloudRegions, maxInitialRegions, entries);
	}
	
	public static CloudSpawningConfig.Info readInfo(CloudTypeSource typeValidator, JsonObject object) throws JsonSyntaxException, NullPointerException, IllegalArgumentException
	{
		String rawId = GsonHelper.getAsString(object, "type");
		ResourceLocation id = ResourceLocation.read(rawId).resultOrPartial(e -> {
			throw new IllegalArgumentException(e);
		}).get();
		if (!typeValidator.doesCloudTypeExist(id))
			throw new IllegalArgumentException("Unknown cloud type with id '" + id + "'");
		
		Weight weight = Weight.CODEC.parse(JsonOps.INSTANCE, object.get("weight")).resultOrPartial(e -> {
			throw new JsonSyntaxException(e);
		}).get();
		
		FloatProvider speed = FloatProvider.codec(0.0F, 10.0F).parse(JsonOps.INSTANCE, object.get("speed")).resultOrPartial(e -> {
			throw new JsonSyntaxException(e);
		}).get();
		
		IntProvider radius = IntProvider.NON_NEGATIVE_CODEC.parse(JsonOps.INSTANCE, object.get("radius")).resultOrPartial(e -> {
			throw new JsonSyntaxException(e);
		}).get();
		
		IntProvider existTicks = IntProvider.NON_NEGATIVE_CODEC.parse(JsonOps.INSTANCE, object.get("exist_ticks")).resultOrPartial(e -> {
			throw new JsonSyntaxException(e);
		}).get();
		
		IntProvider growTicks = IntProvider.codec(0, existTicks.getMaxValue()).parse(JsonOps.INSTANCE, object.get("grow_ticks")).resultOrPartial(e -> {
			throw new JsonSyntaxException(e);
		}).get();
		
		FloatProvider stretchFactor;
		if (object.has("stretch_factor"))
		{
			stretchFactor = FloatProvider.codec(0.01F, Float.MAX_VALUE).parse(JsonOps.INSTANCE, object.get("stretch_factor")).resultOrPartial(e -> {
				throw new JsonSyntaxException(e);
			}).orElse(ConstantFloat.of(1.0F));
		}
		else
		{
			stretchFactor = ConstantFloat.of(1.0F);
		}
		
		boolean movesToPlayer = false;
		if (object.has("moves_to_player"))
			movesToPlayer = GsonHelper.getAsBoolean(object, "moves_to_player");
		
		int orderWeight = GsonHelper.getAsInt(object, "order_weight");
		if (orderWeight <= 0)
			throw new IllegalArgumentException("Order weight must be >= 1");
		
		return new CloudSpawningConfig.Info(id, weight, speed, radius, existTicks, growTicks, stretchFactor, movesToPlayer, orderWeight);
	}
	
	public boolean isEmpty()
	{
		return this.weightsPerType.isEmpty(); 
	}
	
	public IntProvider getSpawnInterval()
	{
		return this.spawnInterval;
	}
	
	public int getMaxRegions()
	{
		return this.maxCloudRegions;
	}
	
	public int getMaxInitialRegions()
	{
		return this.maxInitialRegions;
	}
	
	public @Nullable CloudSpawningConfig.Info getWeightInfo(ResourceLocation cloudType)
	{
		return this.weightsPerType.get(cloudType);
	}
	
	public Optional<CloudSpawningConfig.Info> getRandom(RandomSource random)
	{
		return this.weights.getRandom(random);
	}
	
	public static record Info(ResourceLocation cloudType, Weight weight, FloatProvider speed, IntProvider radius, IntProvider existTicks, IntProvider growTicks, FloatProvider stretchFactor, boolean movesToPlayer, int orderWeight) implements WeightedEntry
	{
		@Override
		public Weight getWeight()
		{
			return this.weight;
		}
		
		public JsonObject toJson() throws IllegalArgumentException
		{
			JsonObject object = new JsonObject();
			
			object.addProperty("type", this.cloudType.toString());
			object.add("weight", Weight.CODEC.encodeStart(JsonOps.INSTANCE, this.weight).resultOrPartial(e -> {
				throw new IllegalArgumentException(e);
			}).get());
			object.add("speed", FloatProvider.codec(0.0F, 10.0F).encodeStart(JsonOps.INSTANCE, this.speed).resultOrPartial(e -> {
				throw new IllegalArgumentException(e);
			}).get());
			object.add("radius", IntProvider.NON_NEGATIVE_CODEC.encodeStart(JsonOps.INSTANCE, this.radius).resultOrPartial(e -> {
				throw new IllegalArgumentException(e);
			}).get());
			object.add("exist_ticks", IntProvider.NON_NEGATIVE_CODEC.encodeStart(JsonOps.INSTANCE, this.existTicks).resultOrPartial(e -> {
				throw new IllegalArgumentException(e);
			}).get());
			object.add("grow_ticks", IntProvider.codec(0, this.existTicks.getMaxValue()).encodeStart(JsonOps.INSTANCE, this.radius).resultOrPartial(e -> {
				throw new IllegalArgumentException(e);
			}).get());
			object.add("stretch_factor", FloatProvider.codec(0.01F, Float.MAX_VALUE).encodeStart(JsonOps.INSTANCE, this.stretchFactor).resultOrPartial(e -> {
				throw new IllegalArgumentException(e);
			}).get());
			object.addProperty("moves_to_player", this.movesToPlayer);
			if (this.orderWeight <= 0)
				throw new IllegalArgumentException("Order weight must be >= 1");
			object.addProperty("order_weight", this.orderWeight);
			
			return object;
		}
	}
}
