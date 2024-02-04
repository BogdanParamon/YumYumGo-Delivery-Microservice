package nl.tudelft.sem.template.example.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import nl.tudelft.sem.template.model.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class VendorServiceTest {

    private VendorRepository vendorRepo;
    private Vendor vendor1;
    private VendorService vendorService;

    @BeforeEach
    void setUp() {
        this.vendorRepo = mock(VendorRepository.class);
        this.vendorService = new VendorService(vendorRepo);
        this.vendor1 = new Vendor().id(2L);
    }

    @Test
    void makeVendorWorks() {
        Mockito.when(vendorRepo.saveAndFlush(any())).thenReturn(vendor1);

        Optional<Vendor> res = vendorService.makeVendor(vendor1);
        assert (res.isPresent());
        assertEquals(res.get().getId(), vendor1.getId());
    }

    @Test
    void makeVendorByIdWorks() {
        Mockito.when(vendorRepo.saveAndFlush(any())).thenReturn(vendor1);
        Optional<Vendor> res = vendorService.makeVendorById(vendor1.getId());
        assert (res.isPresent());
        assertEquals(res.get().getId(), vendor1.getId());
    }

    @Test
    void existsVendorTrue() {
        Mockito.when(vendorRepo.existsById(1L)).thenReturn(true);
        boolean res = vendorService.existsVendor(1L);
        assertTrue(res);
    }

    @Test
    void existsVendorFalse() {
        Mockito.when(vendorRepo.existsById(1L)).thenReturn(false);
        boolean res = vendorService.existsVendor(1L);
        assertFalse(res);
    }

    @Test
    void getRadiusOfVendorWorks() {
        vendor1.radius(3.0);
        Mockito.when(vendorRepo.findById(vendor1.getId())).thenReturn(Optional.of(vendor1));

        var res = vendorService.getRadiusOfVendor(vendor1.getId());
        assertEquals(Optional.of(3.0), res);
    }

    @Test
    void getRadiusOfVendorFails() {
        Mockito.when(vendorRepo.findById(anyLong())).thenReturn(Optional.empty());

        var res = vendorService.getRadiusOfVendor(vendor1.getId());
        assertEquals(Optional.empty(), res);
    }

    @Test
    void updateRadiusOfVendorWorks200() {
        vendor1.radius(3.0);
        Vendor vendor11 = new Vendor().id(2L).radius(5.0);
        Mockito.when(vendorRepo.findById(vendor1.getId())).thenReturn(Optional.of(vendor1));
        Mockito.lenient().when(vendorRepo.saveAndFlush(vendor1)).thenReturn(vendor11);

        var res = vendorService.updateRadiusOfVendor(vendor1.getId(), 5.0);
        assertEquals(Optional.of(vendor11.getRadius()), res);
    }

    @Test
    void updateRadiusOfVendorFails404() {
        Mockito.when(vendorRepo.findById(anyLong())).thenReturn(Optional.empty());

        var res = vendorService.updateRadiusOfVendor(vendor1.getId(), 5.0);
        assertEquals(Optional.empty(), res);
    }

    @Test
    void getVendorWorks() {
        Mockito.when(vendorRepo.findById(2L)).thenReturn(Optional.ofNullable(vendor1));
        var res = vendorService.getVendor(2L);
        assertTrue(res.isPresent());
        assertEquals(vendor1, res.get());
    }
}
