package me.tango.service;

import me.tango.model.TransactionStatistics;

public interface StatisticsService {

    TransactionStatistics getLastMinuteStatistics();
}
