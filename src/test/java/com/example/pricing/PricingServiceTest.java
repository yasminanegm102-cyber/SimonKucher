
package com.example.pricing;

import com.example.pricing.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PricingService.
 */
class PricingServiceTest {

	@Test
	void testIncreasePriceWhenHighOccupancy() {
		PricingService svc = new PricingService(BigDecimal.valueOf(0.6), BigDecimal.valueOf(0.5));
		// Two products in same cluster
		PricingService.Product p1 = new PricingService.Product("p1", LocalDate.of(2025,10,1), "Std", 2, 3, false);
		PricingService.Product p2 = new PricingService.Product("p2", LocalDate.of(2025,10,1), "Std", 2, 3, false);

		// Three bookings (2 products -> occupancy = 3/2 = 1.5 > target 0.6)
	PricingService.Booking b1 = new PricingService.Booking("b1", "p1", 100.0, LocalDate.now());
	PricingService.Booking b2 = new PricingService.Booking("b2", "p1", 110.0, LocalDate.now());
	PricingService.Booking b3 = new PricingService.Booking("b3", "p2", 120.0, LocalDate.now());

		// current prices (will be ignored as bookings exist)
		Map<String, PricingService.PriceInfo> priceMap = Map.of(
				"p1", new PricingService.PriceInfo("USD", BigDecimal.valueOf(95.00)),
				"p2", new PricingService.PriceInfo("USD", BigDecimal.valueOf(95.00))
		);

		List<PricingService.PriceRecommendationDto> recs = svc.recommendPrices(
				List.of(p1, p2),
				List.of(b1,b2,b3),
				priceMap
		);

		assertEquals(2, recs.size());
		// avgPaid = (100 + 110 + 120) /3 = 110.00
		// occupancy = bookings/(products*days) = 3/(2*30) = 0.05, target=0.6 -> diff = -0.55
		// factor = 1 + sensitivity(0.5) * -0.55 = 1 - 0.275 = 0.725
		// recommended = 110 * 0.725 = 79.75
		BigDecimal expected = BigDecimal.valueOf(79.75).setScale(2);
		for (PricingService.PriceRecommendationDto dto : recs) {
			assertNotNull(dto.getRecommendedValue());
			assertEquals(0, expected.compareTo(dto.getRecommendedValue()));
		}
	}

	@Test
	void testDecreasePriceWhenLowOccupancy() {
		PricingService svc = new PricingService(BigDecimal.valueOf(0.8), BigDecimal.valueOf(0.25));
		// three products in cluster but only one booking => occupancy = 1/3 = 0.333...
		PricingService.Product p1 = new PricingService.Product("p1", LocalDate.of(2025,11,1), "Deluxe", 2, 4, true);
		PricingService.Product p2 = new PricingService.Product("p2", LocalDate.of(2025,11,1), "Deluxe", 2, 4, true);
		PricingService.Product p3 = new PricingService.Product("p3", LocalDate.of(2025,11,1), "Deluxe", 2, 4, true);

	PricingService.Booking b1 = new PricingService.Booking("b1", "p1", 200.0, LocalDate.now());

		Map<String, PricingService.PriceInfo> priceMap = Map.of(
				"p1", new PricingService.PriceInfo("USD", BigDecimal.valueOf(210.00)),
				"p2", new PricingService.PriceInfo("USD", BigDecimal.valueOf(210.00)),
				"p3", new PricingService.PriceInfo("USD", BigDecimal.valueOf(210.00))
		);

		List<PricingService.PriceRecommendationDto> recs = svc.recommendPrices(
				List.of(p1,p2,p3),
				List.of(b1),
				priceMap
		);
		assertEquals(3, recs.size());

	// avgPaid = 200
	// occupancy = bookings/(products*days) = 1/(3*30) = 0.0111, target 0.8 => diff = -0.7889
	// factor = 1 + 0.25 * (-0.7889) = 1 - 0.1972 = 0.8028
	// recommended = 200 * 0.8028 = 160.56
	BigDecimal expected = BigDecimal.valueOf(160.56).setScale(2);

		for (PricingService.PriceRecommendationDto dto : recs) {
			assertNotNull(dto.getRecommendedValue());
			assertEquals(0, expected.compareTo(dto.getRecommendedValue()));
		}
	}

	@Test
	void testFallbackToCurrentPriceWhenNoBookings() {
		PricingService svc = new PricingService(); // defaults
		PricingService.Product p1 = new PricingService.Product("p1", LocalDate.of(2025,12,1), "Suite", 3, 5, true);

		// no bookings
		Map<String, PricingService.PriceInfo> priceMap = Map.of(
				"p1", new PricingService.PriceInfo("EUR", BigDecimal.valueOf(500.00))
		);

		List<PricingService.PriceRecommendationDto> recs = svc.recommendPrices(
				List.of(p1),
				List.of(),
				priceMap
		);

		assertEquals(1, recs.size());
		PricingService.PriceRecommendationDto dto = recs.get(0);
		assertEquals("EUR", dto.getCurrency());
		assertEquals(0, BigDecimal.valueOf(500.00).setScale(2).compareTo(dto.getRecommendedValue()));
	}
}
