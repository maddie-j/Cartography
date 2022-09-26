package lordfokas.cartography.feature.data.climate;

import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.event.DimensionChangedEvent;
import com.eerussianguy.blazemap.api.event.ServerJoinedEvent;
import com.eerussianguy.blazemap.api.mapping.Layer;
import com.eerussianguy.blazemap.api.markers.MapLabel;
import com.eerussianguy.blazemap.engine.BlazeMapEngine;
import lordfokas.cartography.Cartography;
import lordfokas.cartography.CartographyReferences;
import lordfokas.cartography.data.ClusterStore;
import lordfokas.cartography.data.IClusterConsumer;
import lordfokas.cartography.feature.mapping.RainfallIsolinesLayer;
import lordfokas.cartography.feature.mapping.TemperatureIsolinesLayer;
import lordfokas.cartography.utils.Colors;
import lordfokas.cartography.utils.ImageHandler;

public class ClimateClusterStore extends ClusterStore {
    private static final HashMap<ResourceKey<Level>, HashMap<String, ClimateDataPool>> RAINFALL = new HashMap<>();
    private static final HashMap<ResourceKey<Level>, HashMap<String, ClimateDataPool>> TEMPERATURE = new HashMap<>();
    private static final ClimateConsumer RAIN_CONSUMER = new ClimateConsumer(CartographyReferences.Layers.RAINFALL_ISO, Colors.argb2abgr(RainfallIsolinesLayer.RAINFALL_BLUE), "rainfall");
    private static final ClimateConsumer TEMP_CONSUMER = new ClimateConsumer(CartographyReferences.Layers.TEMPERATURE_ISO, Colors.argb2abgr(TemperatureIsolinesLayer.TEMPERATURE_RED), "temperature");

    @SubscribeEvent
    public static void onServerJoined(ServerJoinedEvent event) {
        RAINFALL.clear();
        TEMPERATURE.clear();
    }

    @SubscribeEvent
    public static void onDimensionChanged(DimensionChangedEvent event) {
        foreach(ClusterType.RAINFALL, value -> BlazeMapEngine.async().runOnDataThread(() -> getRainfallPool(event.dimension, value)));
        foreach(ClusterType.TEMPERATURE, value -> BlazeMapEngine.async().runOnDataThread(() -> getTemperaturePool(event.dimension, value)));
    }

    public static synchronized ClimateDataPool getRainfallPool(ResourceKey<Level> dimension, String value) {
        return RAINFALL
            .computeIfAbsent(dimension, $ -> new HashMap<>())
            .computeIfAbsent(value, $ -> new ClimateDataPool(
                storage(), getClusterNode(ClusterType.RAINFALL, value),
                new ClimateClusterRealm(BlazeMapEngine.cruncher().getThreadAsserter(), RAIN_CONSUMER),
                value, "mm"));
    }

    public static synchronized ClimateDataPool getTemperaturePool(ResourceKey<Level> dimension, String value) {
        return TEMPERATURE
            .computeIfAbsent(dimension, $ -> new HashMap<>())
            .computeIfAbsent(value, $ -> new ClimateDataPool(
                storage(), getClusterNode(ClusterType.TEMPERATURE, value),
                new ClimateClusterRealm(BlazeMapEngine.cruncher().getThreadAsserter(), TEMP_CONSUMER),
                value, "*C"));
    }

    private static ResourceLocation clusterID(ClimateCluster cluster, String type) {
        return Cartography.resource("clusters/"+type+"/" + cluster.hashCode());
    }

    private static final class ClimateConsumer implements IClusterConsumer<ClimateCluster> {
        private final BlazeRegistry.Key<Layer> layer;
        private final int color;
        private final String type;

        private ClimateConsumer(BlazeRegistry.Key<Layer> layer, int color, String type) {
            this.layer = layer;
            this.color = color;
            this.type = type;
        }

        @Override
        public void pushCluster(ClimateCluster cluster) {
            Isoline.Curve center = cluster.getCenterPoint();
            if(center == null) return;
            ResourceKey<Level> dimension = Minecraft.getInstance().level.dimension();
            Isoline isoline = cluster.getData();

            String text = String.format(" %s %s ", isoline.value, isoline.unit);
            ImageHandler.DynamicLabel dynamicLabel = ImageHandler.getLabel(text);
            MapLabel label = new MapLabel(
                clusterID(cluster, type),
                dimension,
                center.chunk.getBlockAt(center.mx, 0, center.my),
                layer,
                text,
                dynamicLabel.path,
                dynamicLabel.image.getWidth(),
                dynamicLabel.image.getHeight(),
                color,
                center.angle,
                true
            );
            BlazeMapEngine.async().runOnGameThread(() -> {
                var labels = labels();
                if(labels.has(label)) {
                    labels.remove(label);
                }
                labels.add(label);
            });
        }

        @Override
        public void dropCluster(ClimateCluster cluster) {
            BlazeMapEngine.async().runOnGameThread(() -> labels().remove(clusterID(cluster, type), layer));
        }
    }
}
