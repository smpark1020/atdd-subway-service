package nextstep.subway.line.domain;

import nextstep.subway.station.domain.Station;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.*;

@Embeddable
public class Sections {
    public static final String EXISTS_SECTION_EXCEPTION_MESSAGE = "이미 등록된 구간 입니다.";
    public static final String NOT_EXISTS_ALL_STATIONS_EXCEPTION_MESSAGE = "등록할 수 없는 구간 입니다.";
    public static final String AT_LEAST_ONE_SECTION_EXCEPTION_MESSAGE = "구간은 최소 한개 이상이어야만 합니다.";

    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private final List<Section> sections;

    protected Sections() {
        sections = new ArrayList<>();
    }

    public Sections(List<Section> sections) {
        this.sections = sections;
    }

    public List<Station> getStations() {
        if (sections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Station> stations = new ArrayList<>();
        Station downStation = findUpStation();
        stations.add(downStation);

        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = sections.stream()
                    .filter(it -> it.getUpStation() == finalDownStation)
                    .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getDownStation();
            stations.add(downStation);
        }

        return stations;
    }

    private Station findUpStation() {
        Station downStation = sections.get(0).getUpStation();
        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = sections.stream()
                    .filter(it -> it.getDownStation() == finalDownStation)
                    .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getUpStation();
        }

        return downStation;
    }

    public void add(Section section) {
        List<Station> stations = getStations();
        boolean isUpStationExisted = stations.stream().anyMatch(section::isUpStationEqualsToStation);
        boolean isDownStationExisted = stations.stream().anyMatch(section::isDownStationEqualsToStation);

        if (stations.isEmpty()) {
            sections.add(section);
            return;
        }

        if (isUpStationExisted && isDownStationExisted) {
            throw new IllegalArgumentException(EXISTS_SECTION_EXCEPTION_MESSAGE);
        }

        if (!isUpStationExisted && !isDownStationExisted) {
            throw new IllegalArgumentException(NOT_EXISTS_ALL_STATIONS_EXCEPTION_MESSAGE);
        }

        if (isUpStationExisted) {
            sections.stream()
                    .filter(section::isUpStationEqualsToUpStationInSection)
                    .findFirst()
                    .ifPresent(it -> it.updateUpStation(section));
        }

        if (isDownStationExisted) {
            sections.stream()
                    .filter(section::isDownStationEqualsToDownStationInSection)
                    .findFirst()
                    .ifPresent(it -> it.updateDownStation(section));
        }

        sections.add(section);
    }

    public void removeSection(Line line, Station station) {
        if (sections.size() <= 1) {
            throw new IllegalArgumentException(AT_LEAST_ONE_SECTION_EXCEPTION_MESSAGE);
        }

        Optional<Section> upLineStation = sections.stream()
                .filter(it -> it.isUpStationEqualsToStation(station))
                .findFirst();
        Optional<Section> downLineStation = sections.stream()
                .filter(it -> it.isDownStationEqualsToStation(station))
                .findFirst();

        if (upLineStation.isPresent() && downLineStation.isPresent()) {
            Station newUpStation = downLineStation.get().getUpStation();
            Station newDownStation = upLineStation.get().getDownStation();
            Distance newDistance = Distance.valueOf(0);
            newDistance.plus(upLineStation.get().getDistance());
            newDistance.plus(downLineStation.get().getDistance());
            sections.add(new Section(line, newUpStation, newDownStation, newDistance));
        }

        upLineStation.ifPresent(sections::remove);
        downLineStation.ifPresent(sections::remove);
    }
}
