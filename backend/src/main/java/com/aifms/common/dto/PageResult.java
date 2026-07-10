package com.aifms.common.dto;

import java.util.List;

/**
 * Paginated response wrapper.
 * Use for list endpoints that return paged data.
 *
 * @param <T> the type of items in the page
 */
public class PageResult<T> {

    private List<T> items;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public PageResult(List<T> items, int page, int size, long total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }

    // ── Getters ──

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotal() { return total; }
    public int getTotalPages() { return totalPages; }
}
