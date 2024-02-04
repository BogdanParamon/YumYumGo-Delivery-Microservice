package nl.tudelft.sem.template.example.externalservices;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import nl.tudelft.sem.template.model.Location;
import nl.tudelft.sem.template.model.Time;

public class NavigationMock {

    public NavigationMock() {
    }

    /**
     * this is a mock, so it does not actually do anything with the time values or the id.
      */

    public OffsetDateTime getEta(Long orderId, Time timeValues) {

        return OffsetDateTime.of(2024, 1, 9,
            11, 45, 0, 0, ZoneOffset.ofTotalSeconds(0));
    }

    /**
     * This is a mock, so it does not actually calculate anything.
     */
    public Float getDistance(Location courier, Location order) {


        return 3.0F;
    }
}
