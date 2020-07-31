package juuxel.bytecodetweaker;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import net.fabricmc.stitch.util.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TweakerConfig {
    final Map<String, Set<String>> bridges;

    public TweakerConfig(Map<String, Set<String>> bridges) {
        this.bridges = bridges;
    }

    private static String asString(JsonElement json) {
        return ((JsonPrimitive) json).asString();
    }

    public static TweakerConfig fromJson(JsonObject json) {
        Map<String, Set<String>> bridges = Collections.emptyMap();

        if (json.containsKey("bridges")) {
            bridges = json.getObject("bridges").entrySet().stream()
                .map(entry -> Pair.of(
                    entry.getKey(),
                    ((JsonArray) entry.getValue()).stream().map(TweakerConfig::asString).collect(Collectors.toSet()))
                )
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        }

        return new TweakerConfig(bridges);
    }
}
