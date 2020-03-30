package me.tango.service.impl;

import lombok.RequiredArgsConstructor;
import me.tango.dto.TransactionDto;
import me.tango.service.TransactionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final StatisticsSlidingWindow statisticsSlidingWindow;

    @Override
    public void saveNewTransaction(TransactionDto dto) {
        statisticsSlidingWindow.processNewTransaction(dto);
    }
}
