package ro.unibuc.bpd.wildfire.geo;

import com.fasterxml.jackson.core.type.TypeReference;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import ro.unibuc.bpd.wildfire.config.AppConfig;
import ro.unibuc.bpd.wildfire.model.ZoneDefinition;
import ro.unibuc.bpd.wildfire.model.ZoneMatch;
import ro.unibuc.bpd.wildfire.serialization.JsonSerde;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ZoneResolver implements Serializable {
    private final List<ZoneFeature> admin1Zones;
    private final List<ZoneFeature> admin0Zones;
    private final List<ZoneDefinition> fallbackZones;
    private transient GeometryFactory geometryFactory;

    public ZoneResolver(List<ZoneFeature> admin1Zones, List<ZoneFeature> admin0Zones, List<ZoneDefinition> fallbackZones) {
        this.admin1Zones = admin1Zones;
        this.admin0Zones = admin0Zones;
        this.fallbackZones = fallbackZones;
    }

    public static ZoneResolver load(AppConfig config) {
        List<ZoneFeature> admin0 = loadShapefile(
                config.naturalEarthAdmin0Path(),
                new String[]{"ADMIN", "NAME_EN", "NAME", "SOVEREIGNT"},
                new String[]{"ADMIN", "NAME_EN", "NAME", "SOVEREIGNT"}
        );
        List<ZoneFeature> admin1 = loadShapefile(
                config.naturalEarthAdmin1Path(),
                new String[]{"name", "name_en", "geonunit"},
                new String[]{"admin", "adm0_name", "geonunit"}
        );
        return new ZoneResolver(admin1, admin0, loadFallbackZones());
    }

    public static ZoneResolver loadDefault() {
        return new ZoneResolver(List.of(), List.of(), loadFallbackZones());
    }

    private static List<ZoneDefinition> loadFallbackZones() {
        try (InputStream stream = ZoneResolver.class.getResourceAsStream("/zones/zones.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing zones catalog resource");
            }
            return JsonSerde.mapper().readValue(stream, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load zone catalog", exception);
        }
    }

    public ZoneMatch resolve(double latitude, double longitude) {
        if (!admin1Zones.isEmpty() || !admin0Zones.isEmpty()) {
            if (geometryFactory == null) {
                geometryFactory = new GeometryFactory();
            }
            Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
            ZoneMatch admin1Match = resolveFromFeatures(admin1Zones, point);
            if (admin1Match != null) {
                return admin1Match;
            }
            ZoneMatch admin0Match = resolveFromFeatures(admin0Zones, point);
            if (admin0Match != null) {
                return admin0Match;
            }
        }

        return fallbackZones.stream()
                .filter(zone -> zone.contains(latitude, longitude))
                .findFirst()
                .map(zone -> new ZoneMatch(zone.zoneName(), zone.country()))
                .orElseGet(ZoneMatch::unknown);
    }

    private static ZoneMatch resolveFromFeatures(List<ZoneFeature> zones, Point point) {
        return zones.stream()
                .filter(zone -> zone.geometry().covers(point))
                .findFirst()
                .map(zone -> new ZoneMatch(zone.zoneName(), zone.country()))
                .orElse(null);
    }

    private static List<ZoneFeature> loadShapefile(String shapefilePath, String[] zoneFieldCandidates, String[] countryFieldCandidates) {
        if (shapefilePath == null || shapefilePath.isBlank()) {
            return List.of();
        }

        File file = new File(shapefilePath);
        if (!file.exists()) {
            throw new IllegalStateException("Missing Natural Earth shapefile: " + shapefilePath);
        }

        List<ZoneFeature> zones = new ArrayList<>();
        try {
            FileDataStore store = new ShapefileDataStore(file.toURI().toURL());
            ((ShapefileDataStore) store).setCharset(StandardCharsets.UTF_8);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            try (FeatureIterator<SimpleFeature> iterator = featureSource.getFeatures().features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    Object geometry = feature.getDefaultGeometry();
                    if (!(geometry instanceof Geometry polygon)) {
                        continue;
                    }

                    String zoneName = firstAttribute(feature, zoneFieldCandidates);
                    String country = firstAttribute(feature, countryFieldCandidates);
                    if (zoneName.isBlank()) {
                        continue;
                    }

                    zones.add(new ZoneFeature(
                            zoneName,
                            country.isBlank() ? zoneName : country,
                            polygon
                    ));
                }
            } finally {
                store.dispose();
            }
            return zones;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load shapefile " + shapefilePath, exception);
        }
    }

    private static String firstAttribute(SimpleFeature feature, String[] candidates) {
        for (String candidate : candidates) {
            Object value = feature.getAttribute(candidate);
            if (value == null) {
                value = feature.getAttribute(candidate.toUpperCase(Locale.ROOT));
            }
            if (value == null) {
                value = feature.getAttribute(candidate.toLowerCase(Locale.ROOT));
            }
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return "";
    }

    private record ZoneFeature(String zoneName, String country, Geometry geometry) implements Serializable {
    }
}
