package nl.tudelft.sem.template.example.controllers;

import static nl.tudelft.sem.template.example.authorization.AuthorizationService.doesNotHaveAuthority;

import java.util.Optional;
import javax.validation.Valid;
import nl.tudelft.sem.template.api.UserApi;
import nl.tudelft.sem.template.example.authorization.AuthorizationService;
import nl.tudelft.sem.template.example.domain.user.CourierService;
import nl.tudelft.sem.template.example.domain.user.VendorService;
import nl.tudelft.sem.template.model.Courier;
import nl.tudelft.sem.template.model.Vendor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user")
public class UserController implements UserApi {

    public VendorService vendorService;
    public CourierService courierService;

    public AuthorizationService authorizationService;

    /**
     * User controller Constructor.
     *
     * @param vendorService service for vendors
     * @param courierService service for couriers
     * @param authorizationService authorization
     */
    public UserController(VendorService vendorService, CourierService courierService,
                          AuthorizationService authorizationService) {
        this.vendorService = vendorService;
        this.courierService = courierService;
        this.authorizationService = authorizationService;
    }

    /**
     * GET /user/courier/{courierId} : Retrieve a courier given the courier id.
     * return the courier corresponding to the id
     *
     * @param courierId     id of the courier to retrieve (required)
     * @param authorization the UserId to check if they have the rights to make this request (required)
     * @return Successful response, courier received (status code 200)
     *         or Unsuccessful, courier cannot be retrieved because of bad request (status code 400)
     *         or Unsuccessful, no courier was found (status code 404)
     *         or Unauthorized (status code 403)
     */
    @Override
    @GetMapping("/courier/{courierId}")
    public ResponseEntity<Courier> getCourier(
        @PathVariable(name = "courierId") Long courierId,
        @RequestParam(name = "authorization") Long authorization) {

        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Courier> courier = courierService.getCourierById(courierId);
        return courier.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET /user/vendor/radius : Retrieve the specific radius for any vendors.
     * Return the specific radius of the vendor.
     *
     * @param authorization the userId to check if they have the rights to make this request, also the vendor id to set.
     *                     (required)
     * @return Successful response, radius received. (status code 200)
     *         or Unsuccessful, specific radius cannot be retrieved because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to retrieve specific radius (status code 403)
     *         or Unsuccessful, no specific radius was found (status code 404)
     */
    @Override
    @GetMapping("/vendor/radius")
    public ResponseEntity<Double> getSpecificRadius(
        @RequestParam(name = "authorization") Long authorization
    ) {
        var auth =
            authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Double> ratingReceived = vendorService.getRadiusOfVendor(authorization);

        return ratingReceived.map(dub -> new ResponseEntity<>(dub, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * POST /user/courier/add-whole : Add a courier.
     * Add a courier to the database. One needs to provide the whole object. To be used by admin.
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param courier       (optional)
     * @return Successful response, courier added (status code 200)
     *         or Unsuccessful, courier cannot be added because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to add courier (status code 403)
     *         or Unsuccessful, no courier was found (status code 404)
     */
    @Override
    @PostMapping("/courier/add-whole")
    public ResponseEntity<Void> makeCourier(@RequestParam(value = "authorization") Long authorization,
                                            @RequestBody(required = false) Courier courier) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        if (courierService.existsCourier(courier.getId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Courier> saved = courierService.makeCourier(courier);

        if (saved.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * POST /user/courier/{courierId} : Add a courier by only using the id.
     * Add a courier to the database. Only input needed is the id. The other fields will be set to  a default value.
     * To be used by admin.
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param courierId     id of the courier to create (required)
     * @return Successful response, courier added (status code 200)
     *         or Unsuccessful, courier cannot be added because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to add courier (status code 403)
     *         or Unsuccessful, no courier was found (status code 404)
     */
    @Override
    @PostMapping("/courier/{courierId}")
    public ResponseEntity<Void> makeCourierById(@RequestParam(value = "authorization") Long authorization,
                                                @PathVariable(name = "courierId") Long courierId) {
        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        if (courierService.existsCourier(courierId)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Courier> saved = courierService.makeCourierById(courierId);

        if (saved.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * PUT /user/courier/{courierId}/{bossId} : Update the boss of a courier given the courier id and vendor/admin id.
     * Update the boss of the courier corresponding to the courierId. The boss can either be a vendor
     * (if the courier works for that vendor) or the admin (if not).
     *
     * @param courierId     id of the courier to update (required)
     * @param bossId        id of the vendor or admin that this courier works for (required)
     * @param authorization the UserId to check if they have the rights to make this request (required)
     * @return Successful response, courier received (status code 200)
     *         or Unsuccessful, courier updated be retrieved because of bad request (status code 400)
     *         or Unsuccessful, no courier was found (status code 404)
     *         or Unauthorized (status code 403)
     *         only for vendors
     */
    @Override
    @PutMapping("/courier/{courierId}/{bossId}")
    public ResponseEntity<Void> updateBossOfCourier(@PathVariable("courierId") Long courierId,
                                                    @PathVariable("bossId") Long bossId,
                                                    @RequestParam(value = "authorization", required = true)
                                                    Long authorization) {
        var auth = authorizationService.checkIfUserIsAuthorized(authorization,
                "updateBossOfCourier", bossId);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Long> newBossId = courierService.updateBossIdOfCourier(courierId, bossId);

        if (newBossId.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * PUT /user/vendor/radius : Update the specific radius for a vendor.
     * Update the specific radius of a vendor. To be used by that vendor.
     *
     * @param authorization The userId to check if they have the rights to make this request, also the vendor id to set.
     *                     (required)
     * @param body          Radius in meters to be set as a new value (required)
     * @return Successful response, specific radius updated. (status code 200)
     *         or Unsuccessful, specific radius cannot be updated because of a bad request (status code 400)
     *         or Unsuccessful, entity does not have access rights to update radius. (status code 403)
     *         or Unsuccessful, no specific radius was found. (status code 404)
     */
    @Override
    @PutMapping("/vendor/radius")
    public ResponseEntity<Void> updateSpecificRadius(
        @RequestParam(name = "authorization") Long authorization,
        @RequestBody @Valid Double body
    ) {
        var auth =
            authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        Optional<Vendor> vendor = vendorService.getVendor(authorization);

        if (vendor.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Optional<Double> newRadius = vendorService.updateRadiusOfVendor(authorization, body);

        if (newRadius.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }


    /**
     * Adds the given user to the database.
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param vendor        (optional)
     * @return the saved user
     */
    @Override
    @PostMapping("/vendor/add-whole")
    public ResponseEntity<Void> makeVendor(
        @RequestParam(name = "authorization") Long authorization,
        @RequestBody Vendor vendor) {

        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }

        if (vendorService.existsVendor(vendor.getId())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Vendor> saved = vendorService.makeVendor(vendor);

        if (saved.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Creates a new user with given id and adds it to database.
     *
     * @param authorization The userId to check if they have the rights to make this request (required)
     * @param vendorId      Id of the vendor to create (required)
     * @return the saved user
     */
    @Override
    @PostMapping("/vendor/{vendorId}")
    public ResponseEntity<Void> makeVendorById(
        @RequestParam(name = "authorization") Long authorization,
        @PathVariable(name = "vendorId") Long vendorId) {

        var auth = authorizationService.authorizeAdminOnly(authorization);
        if (doesNotHaveAuthority(auth)) {
            return auth.get();
        }


        if (vendorService.existsVendor(vendorId)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Vendor> saved = vendorService.makeVendorById(vendorId);

        if (saved.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
