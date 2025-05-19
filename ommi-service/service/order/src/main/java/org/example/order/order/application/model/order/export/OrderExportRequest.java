package org.example.order.order.application.model.order.export;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderExportRequest {

    private Integer limit;
    private Integer page;

    private @NotNull ExportFilterType filterType;

    private @NotNull ExportType type;

    private String searchType;

    private List<String> exportFields;

    public enum ExportFilterType {
        allOrder,
        currentPage,
        selectedOrder,
        currentSearch
    }

    public enum ExportType {
        orderOverview,
        overviewByProduct,
        detail
    }
}
