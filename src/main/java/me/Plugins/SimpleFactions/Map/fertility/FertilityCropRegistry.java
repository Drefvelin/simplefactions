package me.Plugins.SimpleFactions.Map.fertility;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

import org.bukkit.Material;

public final class FertilityCropRegistry {
    public static final FertilityCropRegistry EMPTY = new FertilityCropRegistry(Map.of(), Map.of());

    private final Map<Material, Double> vanillaWeights;
    private final Map<String, Double> customWeights;

    private FertilityCropRegistry(Map<Material, Double> vanillaWeights, Map<String, Double> customWeights) {
        this.vanillaWeights = Map.copyOf(vanillaWeights);
        this.customWeights = Map.copyOf(customWeights);
    }

    public OptionalDouble weightFor(Material material) {
        if (material == null) {
            return OptionalDouble.empty();
        }
        Double weight = vanillaWeights.get(material);
        return weight == null ? OptionalDouble.empty() : OptionalDouble.of(weight);
    }

    public OptionalDouble weightForCustom(String cropId) {
        if (cropId == null || cropId.isBlank()) {
            return OptionalDouble.empty();
        }
        Double weight = customWeights.get(cropId.toLowerCase(Locale.ROOT));
        return weight == null ? OptionalDouble.empty() : OptionalDouble.of(weight);
    }

    public boolean isEmpty() {
        return vanillaWeights.isEmpty() && customWeights.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Material, Double> vanilla = new HashMap<>();
        private final Map<String, Double> custom = new HashMap<>();

        public Builder vanilla(Material material, double weight) {
            FertilityGrowthChance.validateWeight(weight);
            vanilla.put(material, weight);
            return this;
        }

        public Builder custom(String cropId, double weight) {
            FertilityGrowthChance.validateWeight(weight);
            if (cropId == null || cropId.isBlank()) {
                throw new IllegalArgumentException("cropId must not be blank");
            }
            custom.put(cropId.toLowerCase(Locale.ROOT), weight);
            return this;
        }

        public Builder vanillaAll(Map<Material, Double> weights) {
            if (weights != null) {
                for (Map.Entry<Material, Double> entry : weights.entrySet()) {
                    vanilla(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public Builder customAll(Map<String, Double> weights) {
            if (weights != null) {
                for (Map.Entry<String, Double> entry : weights.entrySet()) {
                    custom(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public FertilityCropRegistry build() {
            return new FertilityCropRegistry(vanilla, custom);
        }
    }
}
