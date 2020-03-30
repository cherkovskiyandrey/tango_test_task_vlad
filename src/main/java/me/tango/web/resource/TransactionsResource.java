package me.tango.web.resource;

import lombok.RequiredArgsConstructor;
import me.tango.dto.TransactionDto;
import me.tango.service.TransactionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionsResource {

    private final TransactionService transactionService;

    @PostMapping
    public void postTransaction(@RequestBody TransactionDto dto) {
        transactionService.saveNewTransaction(dto);
    }
}
