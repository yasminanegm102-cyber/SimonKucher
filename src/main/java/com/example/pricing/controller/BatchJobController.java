package com.example.pricing.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
public class BatchJobController {
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job productIngestJob;
    @Autowired
    private Job bookingIngestJob;
    @Autowired
    private Job priceIngestJob;
    @Autowired
    private Job buildingIngestJob;

    @PostMapping("/run")
    public ResponseEntity<String> runJob(@RequestParam String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            switch (jobName) {
                case "product":
                    jobLauncher.run(productIngestJob, params);
                    break;
                case "booking":
                    jobLauncher.run(bookingIngestJob, params);
                    break;
                case "price":
                    jobLauncher.run(priceIngestJob, params);
                    break;
                case "building":
                    jobLauncher.run(buildingIngestJob, params);
                    break;
                default:
                    return ResponseEntity.badRequest().body("Unknown job: " + jobName);
            }
            return ResponseEntity.ok("Job " + jobName + " started successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Job failed: " + e.getMessage());
        }
    }
}
