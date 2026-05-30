package ro.upb.wildfire.serialization;

import org.apache.flink.api.common.functions.MapFunction;
import ro.upb.wildfire.model.FireHotspotEvent;

public final class FireEventJsonMapper implements MapFunction<String, FireHotspotEvent> {

    @Override
    public FireHotspotEvent map(String value) throws Exception {
        return JsonSerde.mapper().readValue(value, FireHotspotEvent.class);
    }
}

