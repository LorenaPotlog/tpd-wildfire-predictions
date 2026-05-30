package ro.upb.wildfire.streaming;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;
import ro.upb.wildfire.config.AppConfig;
import ro.upb.wildfire.geo.ZoneResolver;
import ro.upb.wildfire.model.FireHotspotEvent;
import ro.upb.wildfire.model.RiskPrediction;
import ro.upb.wildfire.model.WeatherObservation;
import ro.upb.wildfire.model.ZoneMatch;
import ro.upb.wildfire.scoring.RiskScoringService;

public final class RiskPredictionCoProcessFunction
        extends KeyedCoProcessFunction<String, FireHotspotEvent, WeatherObservation, RiskPrediction> {

    private final AppConfig config;
    private final RiskScoringService scoringService;

    private transient ValueState<WeatherObservation> latestWeatherState;
    private transient ValueState<FireHotspotEvent> previousFireState;
    private transient ZoneResolver zoneResolver;

    public RiskPredictionCoProcessFunction(AppConfig config) {
        this.config = config;
        this.scoringService = new RiskScoringService();
    }

    @Override
    public void open(Configuration parameters) {
        latestWeatherState = getRuntimeContext().getState(new ValueStateDescriptor<>("latest-weather", WeatherObservation.class));
        previousFireState = getRuntimeContext().getState(new ValueStateDescriptor<>("previous-fire", FireHotspotEvent.class));
        zoneResolver = ZoneResolver.load(config);
    }

    @Override
    public void processElement1(
            FireHotspotEvent fireEvent,
            KeyedCoProcessFunction<String, FireHotspotEvent, WeatherObservation, RiskPrediction>.Context context,
            Collector<RiskPrediction> collector
    ) throws Exception {
        WeatherObservation weather = latestWeatherState.value();
        boolean fallbackUsed = weather == null;
        WeatherObservation effectiveWeather = fallbackUsed ? WeatherObservation.fallbackFor(fireEvent) : weather;
        FireHotspotEvent previousFire = previousFireState.value();
        ZoneMatch zone = zoneResolver.resolve(fireEvent.latitude(), fireEvent.longitude());

        collector.collect(scoringService.score(fireEvent, previousFire, effectiveWeather, zone, fallbackUsed));
        previousFireState.update(fireEvent);
    }

    @Override
    public void processElement2(
            WeatherObservation weatherObservation,
            KeyedCoProcessFunction<String, FireHotspotEvent, WeatherObservation, RiskPrediction>.Context context,
            Collector<RiskPrediction> collector
    ) throws Exception {
        latestWeatherState.update(weatherObservation);
    }
}
