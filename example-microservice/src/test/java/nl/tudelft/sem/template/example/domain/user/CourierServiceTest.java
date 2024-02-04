package nl.tudelft.sem.template.example.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import nl.tudelft.sem.template.model.Courier;
import nl.tudelft.sem.template.model.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CourierServiceTest {

    public CourierRepository courierRepo;
    Courier courier1;
    private CourierService courierService;

    @BeforeEach
    void setUp() {
        this.courierRepo = mock(CourierRepository.class);
        this.courierService = new CourierService(courierRepo);
        this.courier1 = new Courier().id(100L).bossId(5L).currentLocation(new Location().latitude(0F).longitude(0F));
    }

    @Test
    void updateBossIdOfCourier200() {
        Courier courier11 = new Courier().id(100L).bossId(6L)
            .currentLocation(new Location().latitude(0F).longitude(0F));

        Mockito.when(courierRepo.findById(anyLong())).thenReturn(Optional.of(courier1));
        Mockito.lenient().when(courierRepo.saveAndFlush(courier1)).thenReturn(courier11);

        assertEquals(5L, courier1.getBossId());

        var res = courierService.updateBossIdOfCourier(100L, 6L);
        assertEquals(courier1.getBossId(), courier11.getBossId());
        assertTrue(res.isPresent());
        assertEquals(res.get(), 6L);
    }

    @Test
    void updateBossIdOfCourier404() {
        Mockito.when(courierRepo.getOne(anyLong())).thenThrow(new javax.persistence.EntityNotFoundException());

        Optional<Long> ret = courierService.updateBossIdOfCourier(courier1.getId(), 6L);
        assertTrue(ret.isEmpty());
    }

    @Test
    void getCourierById200() {
        Mockito.when(courierRepo.findById(courier1.getId())).thenReturn(Optional.of(courier1));

        Optional<Courier> ret = courierService.getCourierById(courier1.getId());
        assertEquals(ret.get(), courier1);
    }

    @Test
    void getCourierById404() {
        Mockito.when(courierRepo.findById(courier1.getId())).thenReturn(Optional.empty());

        Optional<Courier> ret = courierService.getCourierById(courier1.getId());
        assertTrue(ret.isEmpty());
    }

    @Test
    void makeCourierWorks() {
        Mockito.when(courierRepo.saveAndFlush(any())).thenReturn(courier1);

        Optional<Courier> res = courierService.makeCourier(courier1);
        assert (res.isPresent());
        assertEquals(res.get().getId(), courier1.getId());
    }

    @Test
    void makeCourierByIdWorks() {
        Mockito.when(courierRepo.saveAndFlush(any())).thenReturn(courier1);

        Optional<Courier> res = courierService.makeCourierById(courier1.getId());
        assert (res.isPresent());
        assertEquals(res.get().getId(), courier1.getId());
    }

    @Test
    void existsCourierTrue() {
        Mockito.when(courierRepo.existsById(1L)).thenReturn(true);
        boolean res = courierService.existsCourier(1L);
        assertTrue(res);
    }

    @Test
    void existsCourierFalse() {
        Mockito.when(courierRepo.existsById(1L)).thenReturn(false);
        boolean res = courierService.existsCourier(1L);
        assertFalse(res);
    }

}
