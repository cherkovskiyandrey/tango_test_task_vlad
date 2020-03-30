package me.tango.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.tango.dto.TransactionDto;
import me.tango.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionsResourceTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    TransactionsResource transactionsResource;

    @MockBean
    TransactionService transactionService;
    @Captor
    ArgumentCaptor<TransactionDto> transactionDtoCaptor;

    @Test
    public void shouldSaveTransaction() throws Exception {
        var expectedDto = new TransactionDto(System.currentTimeMillis(), BigDecimal.ONE);
        mvc.perform(
                post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(expectedDto)))
                .andExpect(status().is2xxSuccessful());

        verify(transactionService).saveNewTransaction(transactionDtoCaptor.capture());

        var actualDto = transactionDtoCaptor.getValue();
        assertEquals(expectedDto, actualDto);
    }
}