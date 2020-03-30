package me.tango.service.impl;

import lombok.RequiredArgsConstructor;
import me.tango.model.TransactionStatistics;
import me.tango.service.StatisticsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsSlidingWindow window;

    @Override
    public TransactionStatistics getLastMinuteStatistics() {
        return window.getWindowStatistics();
    }
}
