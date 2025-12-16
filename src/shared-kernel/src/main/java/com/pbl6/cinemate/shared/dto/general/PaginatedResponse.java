package com.pbl6.cinemate.shared.dto.general;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PaginatedResponse<T> {

    private List<T> content;
    private int currentPage;
    private int pageSize;
    private int totalPages;

    public PaginatedResponse(List<T> content, int currentPage, int pageSize, int totalPages) {
        this.content = content;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

}
