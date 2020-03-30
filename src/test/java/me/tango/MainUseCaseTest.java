package me.tango;

import me.tango.dto.TransactionDto;
import me.tango.model.TransactionStatistics;
import me.tango.service.impl.StatisticsSlidingWindow;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MainUseCaseTest {

    @LocalServerPort
    int serverPort;
    @Autowired
    StatisticsSlidingWindow window;

    @Value("${statistics.window.interval.millis}")
    private int aggregationIntervalDurationMillis;
    @Value("${statistics.window.size}")
    private int windowIntervalsCount;

    RestTemplate restTemplate = new RestTemplate();

    Random random = new Random();

    @Test
    @Order(0)
    public void shouldReturnEmptyStatisticsOnNoTransactions() throws Exception {
        var getStatisticsUrl = String.format("http://localhost:%s/statistics", serverPort);
        var getStatisticsResponse = restTemplate.getForEntity(getStatisticsUrl, TransactionStatistics.class);
        assertEquals(HttpStatus.OK, getStatisticsResponse.getStatusCode());

        var actualStatistics = getStatisticsResponse.getBody();
        var expectedStatistics = TransactionStatistics.builder()
                .count(0L)
                .sum(BigDecimal.ZERO)
                .min(BigDecimal.ZERO)
                .max(BigDecimal.ZERO)
                .avg(BigDecimal.ZERO)
                .build();

        assertEquals(expectedStatistics, actualStatistics);
    }

    @Test
    @Order(1)
    public void shouldReturnAggregatedStatisticsForTheLastMinuteTransactions() throws Exception {
        var transactions = IntStream.range(0, 10_000)
                .parallel()
                .mapToObj(x -> postTransaction())
                .sorted(Comparator.comparing(TransactionDto::getTimestamp))
                .collect(Collectors.toList());

        var getStatisticsUrl = String.format("http://localhost:%s/statistics", serverPort);
        var getStatisticsResponse = restTemplate.getForEntity(getStatisticsUrl, TransactionStatistics.class);
        assertEquals(HttpStatus.OK, getStatisticsResponse.getStatusCode());

        var actualStatistics = getStatisticsResponse.getBody();

        var expectedStatistics = TransactionStatistics.builder()
                .sum(BigDecimal.ZERO)
                .min(BigDecimal.valueOf(Long.MAX_VALUE))
                .max(BigDecimal.valueOf(Long.MIN_VALUE))
                .build();

        var transactionsWithinLastFetchedWindow = transactions.stream()
                .filter(x -> x.getTimestamp() > window.getLastWindowFetchedExclusiveLowerBound())
                .collect(Collectors.toList());
        transactionsWithinLastFetchedWindow.forEach(x -> {
                    expectedStatistics.setSum(expectedStatistics.getSum().add(x.getAmount()));
                    expectedStatistics.setMax(expectedStatistics.getMax().max(x.getAmount()));
                    expectedStatistics.setMin(expectedStatistics.getMin().min(x.getAmount()));
                });
        expectedStatistics.setCount((long)transactionsWithinLastFetchedWindow.size());
        expectedStatistics.setAvg(expectedStatistics.getSum().divide(BigDecimal.valueOf(expectedStatistics.getCount()), RoundingMode.HALF_UP));

        assertEquals(expectedStatistics, actualStatistics);
    }

    private TransactionDto postTransaction() {
        var postTransactionUrl = String.format("http://localhost:%s/transactions", serverPort);
        var request = new TransactionDto(
                System.currentTimeMillis() - random.nextInt(windowIntervalsCount * 2) * aggregationIntervalDurationMillis,
                new BigDecimal(String.format("%d", random.nextInt(1000), random.nextInt(1000)))
        );
        var postTransactionResponse = restTemplate.postForEntity(postTransactionUrl, request, null);
        assertEquals(HttpStatus.OK, postTransactionResponse.getStatusCode());

        return request;
    }
}