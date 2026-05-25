package ro.unibuc.bpd.wildfire.serialization;

import org.apache.flink.api.common.functions.MapFunction;
import ro.unibuc.bpd.wildfire.model.WeatherObservation;

public final class WeatherObservationJsonMapper implements MapFunction<String, WeatherObservation> {

    @Override
    public WeatherObservation map(String value) throws Exception {
        return JsonSerde.mapper().readValue(value, WeatherObservation.class);
    }
}

