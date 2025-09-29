package com.example.pricing.service;


import com.example.pricing.model.PriceRecommendation;
import com.example.pricing.repository.PriceRecommendationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple pricing recommendation engine.
 *
 * Algorithm (explainable baseline):
 *  - Group products by cluster key (arrivalDate, roomType, noOfBeds, grade, privatePool)
 *  - For each cluster compute:
 *      - occupancy = bookings.size() / products.size()
 *      - avgPaid = average pricePaid from bookings (BigDecimal)
 *      - recommended factor = 1 + sensitivity * (occupancy - targetOccupancy)
 *      - recommendedPrice = avgPaid * recommended factor
 *  - If a cluster has no bookings, fallback to the product's current price if available.
 *
 * Notes:
 *  - All BigDecimal arithmetic uses scale 2 and HALF_UP rounding.
 *  - Sensitivity and targetOccupancy are configurable via constructor.
 */

@Service
public class PricingService {


    // Configurable parameters
    private final BigDecimal targetOccupancy; // e.g. 0.8 => 80%
    private final BigDecimal sensitivity;     // e.g. 0.25 => 25% of the occupancy gap applied to price
    private final int windowDays;             // historical window in days
    private final BigDecimal minMargin;       // minimum allowed price as a fraction of avgPaid (e.g. 0.7)
    private final BigDecimal maxIncreasePct;  // max allowed increase over avgPaid (e.g. 0.3 for +30%)
    private final BigDecimal smoothingAlpha;  // EMA smoothing factor (0..1)


    @Autowired(required = false)
    private PriceRecommendationRepository priceRecommendationRepository;

    // For unit testing: allow explicit injection of repository (can be null)
    public void setPriceRecommendationRepository(PriceRecommendationRepository repo) {
        this.priceRecommendationRepository = repo;
    }


    public PricingService() {
        this(BigDecimal.valueOf(0.8), BigDecimal.valueOf(0.25), 30, BigDecimal.valueOf(0.7), BigDecimal.valueOf(0.3), BigDecimal.valueOf(0.5));
    }

    public PricingService(BigDecimal targetOccupancy, BigDecimal sensitivity) {
        this(targetOccupancy, sensitivity, 30, BigDecimal.valueOf(0.7), BigDecimal.valueOf(0.3), BigDecimal.valueOf(0.5));
    }

    public PricingService(BigDecimal targetOccupancy, BigDecimal sensitivity, int windowDays, BigDecimal minMargin, BigDecimal maxIncreasePct, BigDecimal smoothingAlpha) {
        this.targetOccupancy = targetOccupancy;
        this.sensitivity = sensitivity;
        this.windowDays = windowDays;
        this.minMargin = minMargin;
        this.maxIncreasePct = maxIncreasePct;
        this.smoothingAlpha = smoothingAlpha;
    }

    /**
     * Generate price recommendations for the given products using bookings and current prices.
     *
     * @param products  list of products to produce recommendations for
     * @param bookings  list of historical bookings (may include productId referencing products)
     * @param prices    map of current prices keyed by productId -> PriceInfo (currency + value)
     * @return list of PriceRecommendationDto (one per product in input)
     */
    public List<PriceRecommendationDto> recommendPrices(List<Product> products,
                                                        List<Booking> bookings,
                                                        Map<String, PriceInfo> prices) {
        if (products == null) products = Collections.emptyList();
        if (bookings == null) bookings = Collections.emptyList();
        if (prices == null) prices = Collections.emptyMap();

        // Only consider bookings in the last N days (windowing)
        LocalDate windowStart = LocalDate.now().minusDays(windowDays);
        List<Booking> windowedBookings = bookings.stream()
                .filter(b -> b.getArrivalDate() == null || !b.getArrivalDate().isBefore(windowStart))
                .collect(Collectors.toList());

        // Group products by cluster key
        Map<ClusterKey, List<Product>> clusters = products.stream()
                .collect(Collectors.groupingBy(this::clusterKeyOf));

        // Group bookings by productId for quick lookup
        Map<String, List<Booking>> bookingsByProduct = windowedBookings.stream()
                .collect(Collectors.groupingBy(Booking::getProductId));

        List<PriceRecommendationDto> recommendations = new ArrayList<>();
        LocalDate today = LocalDate.now();

    for (Map.Entry<ClusterKey, List<Product>> clusterEntry : clusters.entrySet()) {
            ClusterKey clusterKey = clusterEntry.getKey();
            List<Product> clusterProducts = clusterEntry.getValue();

            // Collect all bookings that belong to any product in this cluster
            List<String> productIds = clusterProducts.stream().map(Product::getId).collect(Collectors.toList());
            List<Booking> clusterBookings = productIds.stream()
                    .flatMap(pid -> bookingsByProduct.getOrDefault(pid, Collections.emptyList()).stream())
                    .collect(Collectors.toList());

            // Calculate number of days in window
            long days = windowDays;
            // True occupancy: bookings / (products * days)
            BigDecimal occupancy = computeTrueOccupancy(clusterBookings.size(), clusterProducts.size(), days);
            BigDecimal avgPaid = computeAveragePaid(clusterBookings);

            // compute cluster factor: 1 + sensitivity * (occupancy - targetOccupancy)
            BigDecimal occupancyDiff = occupancy.subtract(targetOccupancy);
            BigDecimal factor = BigDecimal.ONE.add(sensitivity.multiply(occupancyDiff));
            // avoid negative or zero factor (safety clamp)
            if (factor.compareTo(BigDecimal.valueOf(0.5)) < 0) {
                factor = BigDecimal.valueOf(0.5);
            }

            for (Product p : clusterProducts) {
                PriceInfo currentPrice = prices.get(p.getId());
                BigDecimal recommended;
                String currency = currentPrice != null ? currentPrice.getCurrency() : "USD";

                if (clusterBookings.isEmpty() || avgPaid == null) {
                    // fallback: if we have a current price use it, else no recommendation (null)
                    recommended = currentPrice != null ? currentPrice.getValue() : null;
                } else {
                    // use avgPaid * factor
                    recommended = avgPaid.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                    // Floor/ceiling
                    BigDecimal minPrice = avgPaid.multiply(minMargin).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal maxPrice = avgPaid.multiply(BigDecimal.ONE.add(maxIncreasePct)).setScale(2, RoundingMode.HALF_UP);
                    if (recommended.compareTo(minPrice) < 0) recommended = minPrice;
                    if (recommended.compareTo(maxPrice) > 0) recommended = maxPrice;

                    // Smoothing: EMA with previous recommendation if exists
                    BigDecimal prev = getPreviousRecommendation(p.getId());
                    if (prev != null) {
                        recommended = prev.multiply(BigDecimal.ONE.subtract(smoothingAlpha)).add(recommended.multiply(smoothingAlpha)).setScale(2, RoundingMode.HALF_UP);
                    }
                }

                // Persist recommendation if repository is available
                if (priceRecommendationRepository != null) {
                    PriceRecommendation entity = new PriceRecommendation();
                    entity.setProductId(p.getId());
                    entity.setCurrency(currency);
                    entity.setRecommendedValue(recommended);
                    entity.setRecommendedAt(LocalDateTime.now());
                    entity.setStatus("NEW");
                    priceRecommendationRepository.save(entity);
                }

                PriceRecommendationDto dto = new PriceRecommendationDto(
                        p.getId(),
                        currency,
                        recommended,
                        clusterKey,
                        occupancy,
                        avgPaid,
                        factor
                );
                recommendations.add(dto);
            }
        }

        return recommendations;
    }

    // Helper to get previous recommendation for smoothing
    private BigDecimal getPreviousRecommendation(String productId) {
        if (priceRecommendationRepository == null) return null;
        // Get the latest recommendation for this product
        return priceRecommendationRepository.findAll().stream()
                .filter(r -> productId.equals(r.getProductId()))
                .max(Comparator.comparing(PriceRecommendation::getRecommendedAt))
                .map(PriceRecommendation::getRecommendedValue)
                .orElse(null);
    }

    // True occupancy: bookings / (products * days)
    private BigDecimal computeTrueOccupancy(int bookingsCount, int productsCount, long days) {
        if (productsCount <= 0 || days <= 0) return BigDecimal.ZERO;
        BigDecimal occ = BigDecimal.valueOf(bookingsCount).divide(BigDecimal.valueOf(productsCount * days), 8, RoundingMode.HALF_UP);
        if (occ.compareTo(BigDecimal.ZERO) < 0) occ = BigDecimal.ZERO;
        if (occ.compareTo(BigDecimal.valueOf(2)) > 0) occ = BigDecimal.valueOf(2);
        return occ.setScale(4, RoundingMode.HALF_UP);
    }

    private ClusterKey clusterKeyOf(Product p) {
        return new ClusterKey(p.getArrivalDate(), p.getRoomType(), p.getNoOfBeds(), p.getGrade(), p.getPrivatePool());
    }

    private BigDecimal computeAveragePaid(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) return null;
        BigDecimal sum = bookings.stream()
                .map(b -> BigDecimal.valueOf(b.getPricePaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(bookings.size()), 8, RoundingMode.HALF_UP);
        return avg.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeOccupancy(int bookingsCount, int productsCount) {
        if (productsCount <= 0) return BigDecimal.ZERO;
        // occupancy = bookingsCount / productsCount (value between 0..n, typically >1 possible if multiple bookings per product)
        BigDecimal occ = BigDecimal.valueOf(bookingsCount).divide(BigDecimal.valueOf(productsCount), 8, RoundingMode.HALF_UP);
        // clamp to reasonable range [0, 2] to avoid huge swings
        if (occ.compareTo(BigDecimal.ZERO) < 0) occ = BigDecimal.ZERO;
        if (occ.compareTo(BigDecimal.valueOf(2)) > 0) occ = BigDecimal.valueOf(2);
        return occ.setScale(4, RoundingMode.HALF_UP);
    }

    // ----- DTOs and small domain classes ----- //

    public static class Product {
        private final String id;
        private final LocalDate arrivalDate;
        private final String roomType;
        private final Integer noOfBeds;
        private final Integer grade;
        private final Boolean privatePool;

        public Product(String id, LocalDate arrivalDate, String roomType, Integer noOfBeds, Integer grade, Boolean privatePool) {
            this.id = id;
            this.arrivalDate = arrivalDate;
            this.roomType = roomType;
            this.noOfBeds = noOfBeds;
            this.grade = grade;
            this.privatePool = privatePool;
        }

        public String getId() { return id; }
        public LocalDate getArrivalDate() { return arrivalDate; }
        public String getRoomType() { return roomType; }
        public Integer getNoOfBeds() { return noOfBeds; }
        public Integer getGrade() { return grade; }
        public Boolean getPrivatePool() { return privatePool; }
    }

    public static class Booking {
        private final String id;
        private final String productId;
        private final double pricePaid;
        private final LocalDate arrivalDate;

        public Booking(String id, String productId, double pricePaid) {
            this(id, productId, pricePaid, null);
        }

        public Booking(String id, String productId, double pricePaid, LocalDate arrivalDate) {
            this.id = id;
            this.productId = productId;
            this.pricePaid = pricePaid;
            this.arrivalDate = arrivalDate;
        }

        public String getId() { return id; }
        public String getProductId() { return productId; }
        public double getPricePaid() { return pricePaid; }
        public LocalDate getArrivalDate() { return arrivalDate; }
    }

    public static class PriceInfo {
        private final String currency;
        private final BigDecimal value;

        public PriceInfo(String currency, BigDecimal value) {
            this.currency = currency;
            this.value = value;
        }

        public String getCurrency() { return currency; }
        public BigDecimal getValue() { return value; }
    }

    public static class PriceRecommendationDto {
        private final String productId;
        private final String currency;
        private final BigDecimal recommendedValue; // null means no recommendation
        private final ClusterKey cluster;
        private final BigDecimal occupancy;
        private final BigDecimal avgPaid;
        private final BigDecimal factor;

        public PriceRecommendationDto(String productId, String currency, BigDecimal recommendedValue,
                                      ClusterKey cluster, BigDecimal occupancy, BigDecimal avgPaid, BigDecimal factor) {
            this.productId = productId;
            this.currency = currency;
            this.recommendedValue = recommendedValue;
            this.cluster = cluster;
            this.occupancy = occupancy;
            this.avgPaid = avgPaid;
            this.factor = factor;
        }

        public String getProductId() { return productId; }
        public String getCurrency() { return currency; }
        public BigDecimal getRecommendedValue() { return recommendedValue; }
        public ClusterKey getCluster() { return cluster; }
        public BigDecimal getOccupancy() { return occupancy; }
        public BigDecimal getAvgPaid() { return avgPaid; }
        public BigDecimal getFactor() { return factor; }
    }

    public static class ClusterKey {
        private final LocalDate arrivalDate;
        private final String roomType;
        private final Integer noOfBeds;
        private final Integer grade;
        private final Boolean privatePool;

        public ClusterKey(LocalDate arrivalDate, String roomType, Integer noOfBeds, Integer grade, Boolean privatePool) {
            this.arrivalDate = arrivalDate;
            this.roomType = roomType;
            this.noOfBeds = noOfBeds;
            this.grade = grade;
            this.privatePool = privatePool;
        }

        public LocalDate getArrivalDate() { return arrivalDate; }
        public String getRoomType() { return roomType; }
        public Integer getNoOfBeds() { return noOfBeds; }
        public Integer getGrade() { return grade; }
        public Boolean getPrivatePool() { return privatePool; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClusterKey)) return false;
            ClusterKey that = (ClusterKey) o;
            return Objects.equals(arrivalDate, that.arrivalDate) &&
                    Objects.equals(roomType, that.roomType) &&
                    Objects.equals(noOfBeds, that.noOfBeds) &&
                    Objects.equals(grade, that.grade) &&
                    Objects.equals(privatePool, that.privatePool);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arrivalDate, roomType, noOfBeds, grade, privatePool);
        }

        @Override
        public String toString() {
            return "ClusterKey{" +
                    "arrivalDate=" + arrivalDate +
                    ", roomType='" + roomType + '\'' +
                    ", noOfBeds=" + noOfBeds +
                    ", grade=" + grade +
                    ", privatePool=" + privatePool +
                    '}';
        }
    }
}