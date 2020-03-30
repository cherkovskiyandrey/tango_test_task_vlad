package me.tango.service;

import me.tango.dto.TransactionDto;

public interface TransactionService {

    void saveNewTransaction(TransactionDto dto);
}
