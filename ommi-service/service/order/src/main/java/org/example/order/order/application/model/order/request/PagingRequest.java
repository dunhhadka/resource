package org.example.order.order.application.model.order.request;

import lombok.Getter;

@Getter
public class PagingRequest {
    private static final int PAGE_LIMIT = 250;

    private int limit;
    private int page;

    public void setLimit(int limit) {
        if (limit <= 0) this.limit = PAGE_LIMIT;
        else this.limit = Math.min(limit, PAGE_LIMIT);
    }

    public void setPage(int page) {
        if (page <= 0) this.page = 1;
        else this.page = page;
    }
}
