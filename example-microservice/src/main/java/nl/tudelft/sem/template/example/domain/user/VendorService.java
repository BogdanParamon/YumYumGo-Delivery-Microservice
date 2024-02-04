package nl.tudelft.sem.template.example.domain.user;

import java.util.Optional;
import nl.tudelft.sem.template.model.Vendor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class VendorService {

    private final VendorRepository vendorRepo;

    @Autowired
    public VendorService(VendorRepository vendorRepo) {
        this.vendorRepo = vendorRepo;
    }

    /**
     * Tries to add the given vendor to the database.
     *
     * @param vendor the vendor to add
     * @return an optional of the added vendor (or empty optional)
     */
    public Optional<Vendor> makeVendor(Vendor vendor) {
        Vendor saved = vendorRepo.saveAndFlush(vendor);
        return Optional.of(saved);
    }

    /**
     * Tries to add a new vendor with given id to the database.
     *
     * @param vendorId the id to create a vendor with
     * @return an optional of the added vendor (or empty optional)
     */
    public Optional<Vendor> makeVendorById(Long vendorId) {
        Vendor vendor = new Vendor().id(vendorId);
        Vendor saved = vendorRepo.saveAndFlush(vendor);
        return Optional.of(saved);
    }

    /**
     * Check if id of vendor exists.
     *
     * @param id id of vendor to check
     * @return boolean true if vendor with this id exists
     */
    public boolean existsVendor(Long id) {
        return vendorRepo.existsById(id);
    }

    /**
     * Gets the radius of vendor.
     *
     * @param id id of vendor
     * @return Optional of Double - radius
     */
    public Optional<Double> getRadiusOfVendor(Long id) {
        Optional<Vendor> vendor = vendorRepo.findById(id);

        if (vendor.isEmpty()) {
            return Optional.empty();
        }
        Vendor v = vendor.get();
        return Optional.of(v.getRadius());
    }

    /**
     * Updates the radius of vendor.
     *
     * @param id id of vendor
     * @param body the new radius
     * @return Optional of Double - new radius
     */
    public Optional<Double> updateRadiusOfVendor(Long id, Double body) {
        Optional<Vendor> vendor = vendorRepo.findById(id);

        if (vendor.isEmpty()) {
            return Optional.empty();
        }
        Vendor v = vendor.get();
        v.setRadius(body);

        vendorRepo.saveAndFlush(vendor.get());
        return Optional.of(v.getRadius());
    }

    /**
     * Gets vendor.
     *
     * @param id id of vendor to get
     * @return Optional of Vendor
     */
    public Optional<Vendor> getVendor(Long id) {
        return vendorRepo.findById(id);
    }
}
