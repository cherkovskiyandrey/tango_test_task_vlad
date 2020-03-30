package me.tango.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDto {

    private Long count;
    private BigDecimal sum;
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal avg;
}
