package com.invoiceapp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {

    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int number;
    private int size;
}