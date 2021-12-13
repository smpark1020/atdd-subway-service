package nextstep.subway.path.application;

import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.path.domain.PathFinder;
import nextstep.subway.path.domain.PriceCalculator;
import nextstep.subway.path.domain.ShortestPathResponse;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PathService {

    private final StationRepository stationRepository;
    private final LineRepository lineRepository;

    public PathService(StationRepository stationRepository, LineRepository lineRepository) {
        this.stationRepository = stationRepository;
        this.lineRepository = lineRepository;
    }

    public ShortestPathResponse findShortestPath(Long sourceStationId, Long targetStationId) {
        Station sourceStation = findStation(sourceStationId);
        Station targetStation = findStation(targetStationId);

        List<Line> lines = lineRepository.findAll();

        PathFinder pathFinder = PathFinder.of(lines);
        List<Station> stations = pathFinder.findShortestPath(sourceStation, targetStation);
        int shortestDistance = pathFinder.findShortestDistance(sourceStation, targetStation);
        int price = PriceCalculator.calculate(shortestDistance);

        return ShortestPathResponse.of(stations, shortestDistance, price);
    }

    private Station findStation(Long sourceStationId) {
        return stationRepository.findById(sourceStationId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지하철역입니다."));
    }
}
