package nl.tudelft.sem.template.example.domain.user;

import java.util.Optional;
import nl.tudelft.sem.template.model.Courier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class CourierService {

    private final CourierRepository courierRepo;

    @Autowired
    public CourierService(CourierRepository courierRepo) {
        this.courierRepo = courierRepo;
    }

    /**
     * Attempts to update the bossId of the courier.
     * Vendors use this.
     *
     * @param courierId the id of the courier
     * @param bossId    the new bossId of the courier
     * @return the optional of updated courier object, empty if the courier was not found
     */
    public Optional<Long> updateBossIdOfCourier(Long courierId, Long bossId) {
        Optional<Courier> courier = courierRepo.findById(courierId);

        if (courier.isEmpty()) {
            return Optional.empty();
        }

        Courier c = courier.get();
        c.setBossId(bossId);

        Courier saved = courierRepo.saveAndFlush(c);
        return Optional.of(saved.getBossId());
    }

    /**
     * Gets the courier based on id.
     *
     * @param courierId the id of the courier
     * @return empty optional if courier DNE, optional of Courier otherwise
     */
    public Optional<Courier> getCourierById(Long courierId) {
        return courierRepo.findById(courierId);
    }

    /**
     * Tries to add the given courier to the database.
     *
     * @param courier the courier to add
     * @return an optional of the added courier (or empty optional)
     */
    public Optional<Courier> makeCourier(Courier courier) {
        Courier saved = courierRepo.saveAndFlush(courier);
        return Optional.of(saved);
    }

    /**
     * Tries to add a new courier with given id to the database.
     *
     * @param courierId the id to create a courier with
     * @return an optional of the added courier (or empty optional)
     */
    public Optional<Courier> makeCourierById(Long courierId) {
        Courier courier = new Courier().id(courierId);
        Courier saved = courierRepo.saveAndFlush(courier);
        return Optional.of(saved);
    }

    /**
     * Check if id of courier exists.
     *
     * @param id id of courier to check
     * @return boolean true if courier with this id exists
     */
    public boolean existsCourier(Long id) {
        return courierRepo.existsById(id);
    }

}
