package me.tango;

import com.google.common.collect.TreeMultiset;
import me.tango.dto.TransactionDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Collectors;

public class TestTest {

    @Test
    public void testSet() {
        var treeMultiset = TreeMultiset.create(Comparator.comparingLong(TransactionDto::getTimestamp));

        var dto = new TransactionDto(1L, BigDecimal.ONE);
        treeMultiset.add(dto);
        treeMultiset.add(dto);

        System.out.println(treeMultiset.stream().collect(Collectors.toList()));
    }
}
