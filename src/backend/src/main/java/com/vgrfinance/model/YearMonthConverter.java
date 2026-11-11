package com.vgrfinance.model;

import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda o mês de referência como a data do dia 1 (coluna DATE do banco). RN27. */
@Converter
public class YearMonthConverter implements AttributeConverter<YearMonth, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(YearMonth mes) {
        return mes == null ? null : mes.atDay(1);
    }

    @Override
    public YearMonth convertToEntityAttribute(LocalDate data) {
        return data == null ? null : YearMonth.from(data);
    }
}
