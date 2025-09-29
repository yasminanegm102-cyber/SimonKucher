package com.example.pricing.config;
import org.springframework.batch.core.repository.JobRepository;
import com.example.pricing.dto.ProductCsv;
import com.example.pricing.dto.BookingCsv;
import com.example.pricing.dto.PriceCsv;
import com.example.pricing.dto.BuildingCsv;
import com.example.pricing.model.Product;
import com.example.pricing.model.Booking;
import com.example.pricing.model.Price;
import com.example.pricing.model.PriceId;
import com.example.pricing.model.Building;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.time.LocalDate;

@Configuration
public class BatchConfig {
    private final JobRepository jobRepository;

    public BatchConfig(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }
    // --- Booking ingestion ---
    @Bean
    public FlatFileItemReader<BookingCsv> bookingReader(@Value("${batch.bookings.file:classpath:sample/bookings.csv}") Resource resource) {
        return new FlatFileItemReaderBuilder<BookingCsv>()
                .name("bookingCsvReader")
                .resource(resource)
                .delimited()
                .names(new String[]{"id","productId","arrivalDate","nights","pricePaid"})
                .fieldSetMapper(new BeanWrapperFieldSetMapper<BookingCsv>() {{ setTargetType(BookingCsv.class); }})
                .build();
    }

    @Bean
    public ItemProcessor<BookingCsv, Booking> bookingProcessor() {
        return csv -> {
            Booking b = new Booking();
            b.setId(csv.getId());
            b.setProductId(csv.getProductId());
            b.setArrivalDate(LocalDate.parse(csv.getArrivalDate()));
            b.setNights(Integer.parseInt(csv.getNights()));
            b.setPricePaid(Double.parseDouble(csv.getPricePaid()));
            return b;
        };
    }

    @Bean
    public JpaItemWriter<Booking> bookingWriter(EntityManagerFactory emf) {
        JpaItemWriter<Booking> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);
        return writer;
    }

    @Bean
    public Step bookingIngestStep(PlatformTransactionManager transactionManager,
                                  FlatFileItemReader<BookingCsv> reader,
                                  ItemProcessor<BookingCsv, Booking> processor,
                                  JpaItemWriter<Booking> writer) {
        return new StepBuilder("bookingIngestStep", jobRepository)
            .<BookingCsv, Booking>chunk(500, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean
    public Job bookingIngestJob(Step bookingIngestStep) {
        return new JobBuilder("bookingIngestJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(bookingIngestStep)
            .build();
    }

    // --- Price ingestion ---
    @Bean
    public FlatFileItemReader<PriceCsv> priceReader(@Value("${batch.prices.file:classpath:sample/prices.csv}") Resource resource) {
        return new FlatFileItemReaderBuilder<PriceCsv>()
                .name("priceCsvReader")
                .resource(resource)
                .delimited()
                .names(new String[]{"productId","currency","value","lastUpdated"})
                .fieldSetMapper(new BeanWrapperFieldSetMapper<PriceCsv>() {{ setTargetType(PriceCsv.class); }})
                .build();
    }

    @Bean
    public ItemProcessor<PriceCsv, Price> priceProcessor() {
        return csv -> {
            Price p = new Price();
            p.setId(new PriceId(csv.getProductId(), csv.getCurrency()));
            p.setValue(new java.math.BigDecimal(csv.getValue()));
            p.setLastUpdated(java.time.LocalDateTime.parse(csv.getLastUpdated()));
            return p;
        };
    }

    @Bean
    public JpaItemWriter<Price> priceWriter(EntityManagerFactory emf) {
        JpaItemWriter<Price> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);
        return writer;
    }

    @Bean
    public Step priceIngestStep(PlatformTransactionManager transactionManager,
                                FlatFileItemReader<PriceCsv> reader,
                                ItemProcessor<PriceCsv, Price> processor,
                                JpaItemWriter<Price> writer) {
        return new StepBuilder("priceIngestStep", jobRepository)
            .<PriceCsv, Price>chunk(500, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean
    public Job priceIngestJob(Step priceIngestStep) {
        return new JobBuilder("priceIngestJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(priceIngestStep)
            .build();
    }

    // --- Building ingestion ---
    @Bean
    public FlatFileItemReader<BuildingCsv> buildingReader(@Value("${batch.buildings.file:classpath:sample/buildings.csv}") Resource resource) {
        return new FlatFileItemReaderBuilder<BuildingCsv>()
                .name("buildingCsvReader")
                .resource(resource)
                .delimited()
                .names(new String[]{"id","name","type"})
                .fieldSetMapper(new BeanWrapperFieldSetMapper<BuildingCsv>() {{ setTargetType(BuildingCsv.class); }})
                .build();
    }

    @Bean
    public ItemProcessor<BuildingCsv, Building> buildingProcessor() {
        return csv -> {
            Building b = new Building();
            b.setId(csv.getId());
            b.setName(csv.getName());
            b.setType(csv.getType());
            return b;
        };
    }

    @Bean
    public JpaItemWriter<Building> buildingWriter(EntityManagerFactory emf) {
        JpaItemWriter<Building> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);
        return writer;
    }

    @Bean
    public Step buildingIngestStep(PlatformTransactionManager transactionManager,
                                   FlatFileItemReader<BuildingCsv> reader,
                                   ItemProcessor<BuildingCsv, Building> processor,
                                   JpaItemWriter<Building> writer) {
        return new StepBuilder("buildingIngestStep", jobRepository)
            .<BuildingCsv, Building>chunk(500, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean
    public Job buildingIngestJob(Step buildingIngestStep) {
        return new JobBuilder("buildingIngestJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(buildingIngestStep)
            .build();
    }

    @Bean
    public FlatFileItemReader<ProductCsv> productReader(@Value("${batch.products.file:classpath:sample/products.csv}") Resource resource) {
        return new FlatFileItemReaderBuilder<ProductCsv>()
                .name("productCsvReader")
                .resource(resource)
                .delimited()
                .names(new String[]{"id","buildingId","roomName","arrivalDate","noOfBeds","roomType","grade","privatePool"})
                .fieldSetMapper(new BeanWrapperFieldSetMapper<ProductCsv>() {{ setTargetType(ProductCsv.class); }})
                .build();
    }

    @Bean
    public ItemProcessor<ProductCsv, Product> productProcessor() {
        return csv -> {
            Product p = new Product();
            p.setId(csv.getId());
            p.setBuildingId(csv.getBuildingId());
            p.setRoomName(csv.getRoomName());
            if (csv.getArrivalDate() != null && !csv.getArrivalDate().isBlank()) {
                p.setArrivalDate(LocalDate.parse(csv.getArrivalDate()));
            }
            p.setNoOfBeds(csv.getNoOfBeds() == null || csv.getNoOfBeds().isBlank() ? null : Integer.valueOf(csv.getNoOfBeds()));
            p.setRoomType(csv.getRoomType());
            p.setGrade(csv.getGrade() == null || csv.getGrade().isBlank() ? null : Integer.valueOf(csv.getGrade()));
            p.setPrivatePool(csv.getPrivatePool() == null ? false : Boolean.valueOf(csv.getPrivatePool()));
            return p;
        };
    }

    @Bean
    public JpaItemWriter<Product> productWriter(EntityManagerFactory emf) {
        JpaItemWriter<Product> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);
        return writer;
    }

    @Bean
    public Step productIngestStep(PlatformTransactionManager transactionManager,
                                  FlatFileItemReader<ProductCsv> reader,
                                  ItemProcessor<ProductCsv, Product> processor,
                                  JpaItemWriter<Product> writer) {
        return new StepBuilder("productIngestStep", jobRepository)
            .<ProductCsv, Product>chunk(500, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean
    public Job productIngestJob(Step productIngestStep) {
        return new JobBuilder("productIngestJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(productIngestStep)
            .build();
    }
}
