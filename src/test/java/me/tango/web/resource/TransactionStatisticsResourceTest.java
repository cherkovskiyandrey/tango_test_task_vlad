package me.tango.web.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.tango.model.TransactionStatistics;
import me.tango.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionStatisticsResourceTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    StatisticsResource statisticsResource;

    @MockBean
    StatisticsService statisticsService;

    @Test
    public void shouldSaveTransaction() throws Exception {
        var expectedDto = new TransactionStatistics(1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        when(statisticsService.getLastMinuteStatistics())
                .thenReturn(expectedDto);

        var contentAsString = mvc.perform(get("/statistics"))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var actualDto = mapper.readValue(contentAsString, TransactionStatistics.class);

        assertEquals(expectedDto, actualDto);
        verify(statisticsService).getLastMinuteStatistics();
    }
}