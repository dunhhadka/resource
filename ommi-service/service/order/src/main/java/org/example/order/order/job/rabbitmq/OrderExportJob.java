package org.example.order.order.job.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.util.CellRangeAddress;
import org.example.AdminClient;
import org.example.location.LocationFilter;
import org.example.order.order.application.model.order.export.OrderExportRequest;
import org.example.order.order.application.model.order.response.OrderResponse;
import org.example.order.order.application.service.order.OrderReadService;
import org.example.order.order.application.utils.ExcelUtils;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.infrastructure.configuration.messagebroker.RabbitMqConfiguration;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderExportJob {

    private static final Integer BATCH_SIZE = 250;

    private final AdminClient adminClient;

    private final ImportExportMessageSource importExportMessageSource;
    private final Map<String, Map<Object, String>> messageResourceMap;

    private final OrderReadService orderReadService;

    public OrderExportJob(AdminClient adminClient, ImportExportMessageSource importExportMessageSource, OrderReadService orderReadService) {
        this.adminClient = adminClient;
        this.importExportMessageSource = importExportMessageSource;
        this.orderReadService = orderReadService;
        this.messageResourceMap = this.importExportMessageSource.getMessageMap(OrderEI.class);
    }

    @RabbitListener(queues = RabbitMqConfiguration.QueueName.EXPORT_ORDER)
    public void executeExport(OrderJobHandler.OrderJobDetail jobDetail) {
        var storeId = jobDetail.storeId();
        var locationPermissionIds = jobDetail.locationIds();
        var request = jobDetail.request();

        if (CollectionUtils.isEmpty(request.getExportFields())) {
            request.setExportFields(List.of("order.name"));
        } else if (!request.getExportFields().contains("order.name")) {
            request.getExportFields().add("order.name");
        }

        var excelUtilModel = ExcelUtils.genSpreadSheetWriter("don hang");

        var exportUserName = "full name"; // get full name from author

        genSheetHeader(excelUtilModel, exportUserName, request);

        switch (request.getType()) {
            case detail -> genOrderDetail(excelUtilModel, jobDetail);
        }
    }

    private void genOrderDetail(ExcelUtils.ExcelUtilModel excelUtilModel, OrderJobHandler.OrderJobDetail jobDetail) {
        var request = jobDetail.request();
        var sw = excelUtilModel.getSpreadsheetWriters().get(0);

        // get common styles để sử dụng
        Map<String, Short> styles = excelUtilModel.getStyles();

        var mapHeaderGroup = this.messageResourceMap.get(OrderEI.HEADER_GROUP);
        var mapHeader = this.messageResourceMap.get(OrderEI.HEADER);
        var exportFieldValid = mapHeader.keySet().stream().map(String::valueOf).filter(request.getExportFields()::contains).toList();

        sw.addRow(0);
        int colIndexHeaderGroup = 0;

        Map<String, List<String>> headerGroupedField = new HashMap<>();

        for (var entry : mapHeaderGroup.entrySet()) {
            var headerGroup = exportFieldValid.stream()
                    .filter(field -> field.startsWith(entry.getKey() + "."))
                    .toList();
            if (headerGroup.isEmpty()) {
                continue;
            }

            headerGroupedField.put(entry.getKey() + "", headerGroup);

            sw.writeValues(6, colIndexHeaderGroup, entry.getValue(), styles.get(ExcelUtils.StylesDefault.headerTable.getName()));

            if (headerGroup.size() > 1) {
                // merged cell
                var mergedCell = excelUtilModel.getMergedCells();
                var start = new CellRangeAddress(5, 5, colIndexHeaderGroup, colIndexHeaderGroup + headerGroup.size() - 1);
                for (int i = 0; i < headerGroup.size(); i++) {
                    sw.writeValues(6, colIndexHeaderGroup + i, "", styles.get(ExcelUtils.StylesDefault.headerTable.name()));
                }
            }

            colIndexHeaderGroup += headerGroup.size();
        }

        sw.addRow(7);
        int colIndex = 0;

        var headerGroupedFieldValues = headerGroupedField.values().stream()
                .flatMap(Collection::stream)
                .toList();
        for (var field : headerGroupedFieldValues) {
            // Thêm style
            sw.writeValues(7, colIndex++, field);
        }

        this.genDataDetail(excelUtilModel, headerGroupedFieldValues, jobDetail);
    }

    private void genDataDetail(
            ExcelUtils.ExcelUtilModel excelUtilModel,
            List<String> headerGroupedFieldValues,
            OrderJobHandler.OrderJobDetail jobDetail
    ) {
        var sw = excelUtilModel.getSpreadsheetWriters().get(0);
        var storeId = jobDetail.storeId();
        var request = jobDetail.request();
        var locationIds = jobDetail.locationIds();

        int indexLineItem = 0;

        var locationPermissionIds = this.getLocationPermissionIds(storeId, locationIds);

        var orderList = this.getBatchOrder(storeId, locationPermissionIds, request);

        for (var orders : orderList) {
            if (CollectionUtils.isEmpty(orders)) {
                continue;
            }

            var orderIds = orders.stream().map(OrderResponse::getId).toList();

            Pair<Integer, List<OrderExportModel>> resultModel = null;
            switch (request.getType()) {
                case orderOverview -> resultModel = genExportModelOrderOverview(orders);
            }

            if (resultModel == null) {
                continue;
            }

            var items = resultModel.getValue();
            indexLineItem = resultModel.getKey();
            for (var item : items) {
                sw.addRow(0);
                var model = this.convertValue(item);
                int colIndex = 0;
                for (var field : headerGroupedFieldValues) {
                    var value = this.getValueFromModel(model, field);
                    sw.writeValues(0, colIndex++, value);
                }
            }
        }
    }

    private Object getValueFromModel(Map<String, Object> model, String field) {
        if (StringUtils.isEmpty(field)) {
            return null;
        }

        var splitField = field.split("\\.");
        var value = model.get(splitField[0]);
        if (splitField.length == 1) {
            return value;
        }

        return this.getValueFromModel((Map<String, Object>) value, field.substring(field.indexOf(".") + 1));
    }

    private static final ObjectMapper export_mapper;
    private static final MapType map_ref;

    static {
        export_mapper = new ObjectMapper();
        map_ref = export_mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
    }

    private Map<String, Object> convertValue(Object object) {
        return export_mapper.convertValue(object, map_ref);
    }


    // map response to model
    private Pair<Integer, List<OrderExportModel>> genExportModelOrderOverview(List<OrderResponse> orders) {
        return null;
    }

    public static class OrderExportModel {

    }

    /**
     * Mỗi batch => List<OrderResponse>
     **/
    private List<List<OrderResponse>> getBatchOrder(int storeId, List<Integer> locationIds, OrderExportRequest request) {
        var locationPermission = new LocationPermission(locationIds);

        OrderIterator iterator = switch (request.getFilterType()) {
            case allOrder -> {
                request = new OrderExportRequest();
                request.setLimit(BATCH_SIZE);
                yield new OrderIterator(storeId, locationPermission, request, orderReadService);
            }
            case currentPage -> new OrderIterator(storeId, locationPermission, request, orderReadService);
            case selectedOrder, currentSearch -> {
                request.setLimit(BATCH_SIZE);
                yield new OrderIterator(storeId, locationPermission, request, orderReadService);
            }
        };

        return List.of();
    }

    public static class OrderIterator implements Iterator<List<OrderResponse>> {

        private final int storeId;
        private final LocationPermission locationPermission;
        private final OrderExportRequest request;

        private final OrderReadService orderReadService;

        private int page;
        private int lineCount;
        private List<OrderResponse> current;
        private boolean stop;

        public OrderIterator(
                int storeId,
                LocationPermission locationPermission,
                OrderExportRequest request,
                OrderReadService orderReadService
        ) {
            this.storeId = storeId;
            this.locationPermission = locationPermission;
            this.request = request;

            this.orderReadService = orderReadService;

            this.page = 1;
            this.lineCount = 0;
            this.stop = false;
        }

        @Override
        public boolean hasNext() {
            return (current == null || !current.isEmpty()) && !stop;
        }

        @Override
        public List<OrderResponse> next() {
            List<OrderResponse> result = new ArrayList<>();
            var filterRequest = createFilterRequest();
            var searchType = filterRequest.getSearchType();
            var searchResult = orderReadService.search(storeId, locationPermission, filterRequest, searchType);
            for (var orderResponse : searchResult) {
                var orderLineCount = orderResponse.getLineItems().size();
                if ((this.lineCount + orderLineCount) > 1000) {
                    this.stop = true;
                    break;
                }
                this.lineCount += orderLineCount;
                result.add(orderResponse);
            }

            if (filterRequest.getFilterType() == OrderExportRequest.ExportFilterType.currentPage) {
                this.stop = true;
            }

            this.current = result;
            page++;

            return result;
        }

        private OrderExportRequest createFilterRequest() {
            this.request.setPage(this.page);
            return this.request;
        }
    }

    @Getter
    public static class LocationPermission {
        private final List<Integer> locationIds;

        public LocationPermission(List<Integer> locationIds) {
            this.locationIds = locationIds;
        }
    }

    private List<Integer> getLocationPermissionIds(int storeId, List<Integer> locationIds) {
        var locationFilter = LocationFilter.builder().ids(locationIds).build();

        var locations = this.adminClient.locationFilter(storeId, locationFilter).stream()
                .collect(Collectors.toMap(
                        location -> (int) location.getId(),
                        Function.identity())
                );

        var locationNotFound = locationIds.stream()
                .filter(id -> !locations.containsKey(id))
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (!locationNotFound.isEmpty()) {
            throw new ConstrainViolationException("location", "location not found");
        }

        return locations.keySet().stream().toList();
    }

    private void genSheetHeader(ExcelUtils.ExcelUtilModel excelUtilModel, String exportUserName, OrderExportRequest request) {
        var sw = excelUtilModel.getSpreadsheetWriters().get(0);

        sw.addRow(1);
        sw.writeValues(1, 1, "Ngày xuất:");
    }

}
