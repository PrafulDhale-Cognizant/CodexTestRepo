package com.flagship.claimcheck.service;

import com.flagship.claimcheck.model.ClaimDecision.Status;
import com.flagship.claimcheck.model.ClaimRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DuplicateClaimConcurrencyTest {
    @Autowired AdjudicationService service;

    @Test
    void simultaneousIdenticalSubmissionsAcceptNoMoreThanOne() throws Exception {
        int submissions = 12;
        var ready = new CountDownLatch(submissions);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<Status>>();
        try (var pool = Executors.newFixedThreadPool(submissions)) {
            for (int i = 0; i < submissions; i++) {
                int sequence = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return service.adjudicate(claim("CLM-3%05d".formatted(sequence)), null).status();
                }));
            }
            ready.await();
            start.countDown();
            List<Status> statuses = new ArrayList<>();
            for (Future<Status> future : futures) statuses.add(future.get());
            assertThat(statuses).filteredOn(Status.APPROVED::equals).hasSize(1);
            assertThat(statuses).filteredOn(Status.DENIED::equals).hasSize(submissions - 1);
        }
    }

    @Test
    void concurrentRetriesWithOneIdempotencyKeyReturnTheAcceptedDecision() throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(
                pool.submit(() -> { start.await(); return service.adjudicate(retryClaim(), "retry-400001"); }),
                pool.submit(() -> { start.await(); return service.adjudicate(retryClaim(), "retry-400001"); })
            );
            start.countDown();
            assertThat(tasks.get(0).get().status()).isEqualTo(Status.APPROVED);
            assertThat(tasks.get(1).get().status()).isEqualTo(Status.APPROVED);
        }
    }

    private ClaimRequest claim(String id) {
        return new ClaimRequest(id, "MBR-33001", "prv-race", "99215", LocalDate.of(2026, 7, 20),
            new BigDecimal("123.40"));
    }

    private ClaimRequest retryClaim() {
        return new ClaimRequest("CLM-400001", "MBR-33002", "prv-race", "99215", LocalDate.of(2026, 7, 20),
            new BigDecimal("123.40"));
    }
}
