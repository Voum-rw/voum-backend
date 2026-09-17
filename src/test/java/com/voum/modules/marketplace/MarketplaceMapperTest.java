package com.voum.modules.marketplace;
import com.voum.modules.marketplace.mapper.MarketplaceMapper;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.users.Motari;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MarketplaceMapperTest {
    @Test void offerIncludesActualMotorcyclePlate() {
        Motari motari = new Motari();
        motari.setFirstName("John");
        motari.setMotoPlateNumber("RA 123 B");
        var response = new MarketplaceMapper().toOfferResponse(RideOffer.builder().offeredPrice(1000.0).build(), motari, 1);
        assertEquals("RA 123 B", response.getPlateNumber());
        assertEquals("John", response.getFirstName());
    }
}
