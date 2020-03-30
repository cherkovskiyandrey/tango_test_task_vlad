package me.tango.web.resource;

import lombok.RequiredArgsConstructor;
import me.tango.model.TransactionStatistics;
import me.tango.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsResource {

    private final StatisticsService statisticsService;

    @GetMapping
    public TransactionStatistics getTransactionStatistics() {
        return statisticsService.getLastMinuteStatistics();
    }
}
