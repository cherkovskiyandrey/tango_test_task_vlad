package me.tango.service.impl;

import me.tango.dto.TransactionDto;
import me.tango.model.TransactionStatistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * http://hirzels.com/martin/papers/encyc18-sliding-window.pdf
 */
@Component
public class StatisticsSlidingWindow {

    //TODO: window could be cycle buffer what more effective
    private final Map<Long, TransactionStatistics> window = new ConcurrentHashMap<>();
    private final Map<Long, List<TransactionDto>> intervalToSortedTransactions = new ConcurrentHashMap<>();
    private final NavigableSet<Long> aggregationKeysStream = new ConcurrentSkipListSet<>(); //TODO: redundant data structure, absolute time could be along with TransactionStatistics in window

    @Value("${statistics.window.interval.millis}")
    private int aggregationIntervalDurationMillis;
    @Value("${statistics.window.size}")
    private int windowIntervalsCount;

    private volatile Long lastWindowFetchedExclusiveLowerBound;

    private static TransactionStatistics reduceStatistics(TransactionStatistics s1, TransactionStatistics s2) {
        return TransactionStatistics.builder()
                .count(s1.getCount() + s2.getCount())
                .sum(s1.getSum().add(s2.getSum()))
                .min(s1.getMin().min(s2.getMin()))
                .max(s1.getMax().max(s2.getMax()))
                .build();
    }

    public TransactionStatistics getWindowStatistics() {
        var currentMillis = System.currentTimeMillis();
        lastWindowFetchedExclusiveLowerBound = currentMillis - windowIntervalsCount * aggregationIntervalDurationMillis; //TODO: actually race could be here

        var currentAggregationSecond = currentMillis - currentMillis % aggregationIntervalDurationMillis; //TODO: why not just davide 1000
        var boundaryAggregationSecondWithActualData = currentAggregationSecond - (windowIntervalsCount - 1) * aggregationIntervalDurationMillis;
        var lastMinuteStatisticsBySecond = getIntervalStatistics(currentAggregationSecond, boundaryAggregationSecondWithActualData);

        var boundaryAggregationSecondWithStaleAndActualData = boundaryAggregationSecondWithActualData - aggregationIntervalDurationMillis;
        var boundaryIntervalStatistics = getBoundaryDirtyIntervalStatistics(currentMillis, boundaryAggregationSecondWithStaleAndActualData);
        boundaryIntervalStatistics.ifPresent(lastMinuteStatisticsBySecond::add);

        return aggregateIntervalsToWindowStatistics(lastMinuteStatisticsBySecond);
    }

    public void processNewTransaction(TransactionDto transaction) {
        var currentTimeMillis = System.currentTimeMillis();
        var currentAggregationSecond = currentTimeMillis - currentTimeMillis % aggregationIntervalDurationMillis; //TODO: InMs
        var transactionAggregationSecond = transaction.getTimestamp() - transaction.getTimestamp() % aggregationIntervalDurationMillis;

        if (isTransactionOutOfWindow(currentAggregationSecond, transactionAggregationSecond)) {
            return;
        }

        if (aggregationKeysStream.add(transactionAggregationSecond)) {
            removeStaleTransactions(currentAggregationSecond);
        }
        //TODO: your memory is going to blow up under high load where amount of events during conciliation interval will be a lot.
        //In this case aggregation result in one second seems to be enough
        putTransactionWithinSortedRangeAtInterval(transaction, transactionAggregationSecond);

        var statisticsDelta = TransactionStatistics.builder()
                .count(1L)
                .max(transaction.getAmount())
                .min(transaction.getAmount())
                .sum(transaction.getAmount())
                .build();
        window.merge(transactionAggregationSecond, statisticsDelta, StatisticsSlidingWindow::reduceStatistics);
    }

    public Long getLastWindowFetchedExclusiveLowerBound() {
        return lastWindowFetchedExclusiveLowerBound;
    }

    private List<TransactionStatistics> getIntervalStatistics(long currentAggregationSecond, long boundaryAggregationSecondWithActualData) {
        var lastMinuteStatisticsBySecond = new ArrayList<TransactionStatistics>(windowIntervalsCount);
        for (var i = currentAggregationSecond; i >= boundaryAggregationSecondWithActualData; i -= aggregationIntervalDurationMillis) {
            var statistics = window.get(i);
            if (null != statistics) {
                lastMinuteStatisticsBySecond.add(statistics);
            }
        }
        return lastMinuteStatisticsBySecond;
    }

    private Optional<TransactionStatistics> getBoundaryDirtyIntervalStatistics(
            long currentMillis, long boundaryAggregationSecondWithStaleAndActualData) {

        var staleAndActualTransactions = intervalToSortedTransactions.get(boundaryAggregationSecondWithStaleAndActualData);
        if (null != staleAndActualTransactions) {

            var boundaryMillisForActualData = currentMillis - windowIntervalsCount * aggregationIntervalDurationMillis;
            return Optional.of(staleAndActualTransactions.stream()
                    .filter(x -> x.getTimestamp() > boundaryMillisForActualData)
                    .reduce(
                            TransactionStatistics.builder().sum(BigDecimal.ZERO).count(0L).build(),

                            (statistics, transaction) -> adjustStatisticsWithTransactionData(transaction, statistics),

                            StatisticsSlidingWindow::reduceStatistics
                    ));
        }
        return Optional.empty();
    }

    private TransactionStatistics aggregateIntervalsToWindowStatistics(List<TransactionStatistics> lastMinuteStatisticsBySecond) {
        var statisticsOptional = lastMinuteStatisticsBySecond.stream().reduce(StatisticsSlidingWindow::reduceStatistics);
        statisticsOptional.ifPresent(s -> s.setAvg(s.getSum().divide(BigDecimal.valueOf(s.getCount()), RoundingMode.HALF_UP)));
        return statisticsOptional.orElseGet(() -> TransactionStatistics.builder()
                .count(0L)
                .sum(BigDecimal.ZERO)
                .min(BigDecimal.ZERO)
                .max(BigDecimal.ZERO)
                .avg(BigDecimal.ZERO)
                .build()
        );
    }

    private boolean isTransactionOutOfWindow(long currentAggregationSecond, long transactionAggregationSecond) {
        var millisBetweenTransactionAndNow = currentAggregationSecond - transactionAggregationSecond;
        return millisBetweenTransactionAndNow > windowIntervalsCount * aggregationIntervalDurationMillis;
    }

    private void putTransactionWithinSortedRangeAtInterval(TransactionDto transaction, long transactionAggregationSecond) {
        intervalToSortedTransactions.compute(transactionAggregationSecond, (key, list) -> {
            if (null == list) {
                list = new LinkedList<>();
            }
            list.add(transaction);
            return list;
        });
    }

    private void removeStaleTransactions(long currentAggregationSecond) {
        var staleWindowDataAggregationSecond = currentAggregationSecond - windowIntervalsCount * aggregationIntervalDurationMillis;
        var staleWindowPart = aggregationKeysStream.headSet(staleWindowDataAggregationSecond);

        staleWindowPart.forEach(key -> {
            window.remove(key);
            intervalToSortedTransactions.remove(key);
            aggregationKeysStream.remove(key);
        });
    }

    private TransactionStatistics adjustStatisticsWithTransactionData(TransactionDto transaction, TransactionStatistics statistics) {
        return TransactionStatistics.builder()
                .count(statistics.getCount() + 1L)
                .max(statistics.getMax() == null ? transaction.getAmount() : transaction.getAmount().max(statistics.getMax()))
                .min(statistics.getMin() == null ? transaction.getAmount() : transaction.getAmount().min(statistics.getMin()))
                .sum(transaction.getAmount().add(statistics.getSum()))
                .build();
    }
}
