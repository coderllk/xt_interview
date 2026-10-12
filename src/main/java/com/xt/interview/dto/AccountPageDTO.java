package com.xt.interview.dto;

import java.util.List;

public class AccountPageDTO {
    private List<AccountDTO> list;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    public AccountPageDTO() {}

    public List<AccountDTO> getList() { return list; }
    public void setList(List<AccountDTO> list) { this.list = list; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
