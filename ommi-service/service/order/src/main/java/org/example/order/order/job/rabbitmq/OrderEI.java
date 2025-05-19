package org.example.order.order.job.rabbitmq;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

@ImportExportModel("order")
public class OrderEI {

    public static final String HEADER_GROUP = "headerGroup";
    public static final String HEADER = "header";
    public static final String ORDER_STATUS = "orderStatus";
    public static final String FINANCIAL_STATUS = "financialStatus";
    public static final String FULFILLMENT_STATUS = "fulfillmentStatus";
    public static final String FULFILLMENT_ITEM_STATUS = "fulfillmentItemStatus";
    public static final String TRUE_FALSE_NAME = "trueFalseName";

    @ImportExportResource(HEADER_GROUP)
    public static final Map<String, String> headerGroup = ImmutableMap.of(
            "order", "Đơn hàng",
            "payment", "Thanh toán",
            "fulfillment", "Xử lý vận chuyển",
            "customer", "Khách hàng",
            "lineItem", "Sản phẩm mua",
            "shippingAddress", "Địa chỉ giao hàng",
            "addingInformation", "Thông tin bổ sung"
    );

    public static final List<String> headerOverview = ImmutableList.of(
            "order.index",
            "order.name",
            "order.createdOn",
            "order.locationName",
            "order.sourceName",
            "order.user",
            "order.status",
            "customer.name",
            "order.lineItemQuantity",
            "order.totalPrice",
            "payment.totalReceived"
    );
    public static final List<String> headerOverviewByProduct = ImmutableList.of(
            "order.index",
            "order.name",
            "order.createdOn",
            "order.locationName",
            "order.sourceName",
            "order.user",
            "order.status",
            "customer.name",
            "lineItem.sku",
            "lineItem.name",
            "lineItem.discountedTotal"
    );

    @ImportExportResource(HEADER)
    public static final Map<String, String> header = ImmutableMap.<String, String>builder()
            .put("order.index", "STT")
            .put("order.id", "ID đơn hàng")
            .put("order.name", "Mã đơn hàng")
            .put("order.createdOn", "Ngày tạo đơn")
            .put("order.status", "Trạng thái đơn hàng")
            .put("order.sourceName", "Nguồn")
            .put("order.discountCodes", "Mã khuyến mãi")
            .put("order.currency", "Tiền tệ")
            .put("order.cancelledOn", "Hủy đơn hàng lúc")
            .put("order.subTotalPrice", "Tạm tính")
            .put("order.totalDiscount", "Tiền khuyến mãi")
            .put("order.totalShippingPrice", "Phí vận chuyển")
            .put("order.lineItemQuantity", "Tổng số lượng sản phẩm")
            .put("order.totalTax", "Thuế")
            .put("order.totalPrice", "Tổng tiền")
            .put("order.locationName", "Chi nhánh")
            .put("order.user", "Nhân viên tạo đơn")
            .put("payment.financialStatus", "Trạng thái thanh toán")
            .put("payment.paidAt", "Thanh toán lúc")
            .put("payment.paymentGatewayNames", "Phương thức thanh toán")
            .put("payment.totalReceived", "Khách đã trả")
            .put("payment.totalRefunded", "Tiền hoàn trả")
            .put("payment.totalOutstanding", "Khách còn phải trả")
            .put("fulfillment.fulfillmentStatus", "Trạng thái xử lý")
            .put("fulfillment.fulfilledAt", "Xử lý lúc")
            .put("fulfillment.shippingMethod", "Phương thức vận chuyển")
            .put("customer.name", "Tên khách hàng")
            .put("customer.phone", "Số điện thoại")
            .put("customer.email", "Email")
            .put("customer.buyerAcceptsMarketing", "Nhận quảng cáo")
            .put("lineItem.name", "Tên sản phẩm")
            .put("lineItem.quantity", "Số lượng sản phẩm")
            .put("lineItem.price", "Giá sản phẩm")
            .put("lineItem.lineItemDiscount", "Giảm giá trên sản phẩm")
            .put("lineItem.discountedTotal", "Tổng tiền hàng")
            .put("lineItem.lineItemCompareAtPrice", "Giá so sánh")
            .put("lineItem.sku", "Mã SKU")
            .put("lineItem.requiresShipping", "Sản phẩm yêu cầu vận chuyển")
            .put("lineItem.taxable", "Có thuế")
            .put("lineItem.fulfillmentStatus", "Trạng thái giao hàng của sản phẩm")
            .put("lineItem.vendor", "Nhà sản xuất")
            .put("shippingAddress.firstName", "Tên người nhận hàng")
            .put("shippingAddress.address1", "Địa chỉ 1")
            .put("shippingAddress.address2", "Địa chỉ 2")
            .put("shippingAddress.city", "Thành phố")
            .put("shippingAddress.province", "Tỉnh thành")
            .put("shippingAddress.zip", "Shipping Zip")
            .put("shippingAddress.district", "Quận huyện")
            .put("shippingAddress.ward", "Phường xã")
            .put("shippingAddress.country", "Quốc gia")
            .put("shippingAddress.phone", "Số điện thoại giao hàng")
            .put("addingInformation.note", "Ghi chú")
            .put("addingInformation.noteAttributes", "Chú thích")
            .put("addingInformation.tags", "Tags")
            .put("addingInformation.landingSite", "Landing Site")
            .put("addingInformation.landingSiteRef", "Landing Site Ref")
            .put("addingInformation.referringSite", "Referring Site")
            .build();

    @ImportExportResource(ORDER_STATUS)
    public static final Map<String, String> orderStatus = ImmutableMap.of(
            "cancelled", "Đã Hủy",
            "open", "Đang giao dịch",
            "closed", "Lữu trữ"
    );

    @ImportExportResource(FINANCIAL_STATUS)
    public static final Map<String, String> financialStatus = ImmutableMap.of(
            "pending", "Chưa thanh toán",
            "authorized", "Chờ xác nhận",
            "partially_paid", "Thanh toán một phần",
            "paid", "Đã thanh toán",
            "partially_refunded", "Hoàn trả một phần",
            "refunded", "Hoàn trả toàn bộ",
            "voided", "Đã hủy"
    );

    @ImportExportResource(FULFILLMENT_STATUS)
    public static final Map<String, String> fulfillmentStatus = ImmutableMap.of(
            "unfulfilled", "Chưa xử lý",
            "fulfilled", "Đã xử lý",
            "partial", "Đã xử lý một phần",
            "restocked", "Đã hoàn kho"
    );

    @ImportExportResource(FULFILLMENT_ITEM_STATUS)
    public static final Map<String, String> fulfillmentItemStatus = ImmutableMap.of(
            "unfulfilled", "Chưa xử lý",
            "fulfilled", "Đã xử lý",
            "partial", "Đã xử lý một phần",
            "restocked", "Đã hoàn kho"
    );

    @ImportExportResource(TRUE_FALSE_NAME)
    public static final Map<Boolean, String> trueFalseName = ImmutableMap.of(
            true, "Có",
            false, "không"
    );
}
