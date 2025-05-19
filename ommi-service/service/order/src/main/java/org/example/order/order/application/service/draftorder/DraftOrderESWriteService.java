package org.example.order.order.application.service.draftorder;

import com.google.common.base.Stopwatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.infrastructure.data.dao.DraftOrderDao;
import org.example.order.order.infrastructure.data.dto.DraftOrderDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftOrderESWriteService {

    private static final int TAKE = 1000;

    private final DraftOrderDao draftOrderDao;

    public void reIndexDraftOrders(int startId) {
        CompletableFuture.runAsync(() -> {
            log.info("REINDEX DRAFT ORDER START");

            boolean condition = true;
            int lastId = startId;
            int i = 0;

            var globalWatch = Stopwatch.createStarted();
            var watch = Stopwatch.createUnstarted();

            do {
                watch.start();

                try {
                    var listData = getForElastic(lastId, TAKE);

                    var size = listData.size();
                    if (size == 0) {
                        break;
                    }

                    pushDataToES(listData);

                    watch.stop();
                    var processTime = watch.elapsed(TimeUnit.MILLISECONDS);
                    log.info("Send time from {} to {} take: {} ms", i, i + size, processTime);

                    log.info("Last Draft Order Id : {}", listData.get(listData.size() - 1).getId());

                    condition = size == TAKE;
                    watch.reset();
                    lastId = listData.get(listData.size() - 1).getId();
                } catch (Exception e) {
                    watch.stop();
                    log.error("", e);
                }

            } while (condition);
        });
    }

    private void pushDataToES(List<DraftOrderModel> listData) {
        // push data to elasticsearch
    }

    private List<DraftOrderModel> getForElastic(int lastId, int take) {
        var draftOrders = this.draftOrderDao.getForReIndexES(lastId, take);

        var storeIds = draftOrders.stream().map(DraftOrderDto::getStoreId).distinct().toList();
        var draftOrderIds = draftOrders.stream().map(DraftOrderDto::getId).distinct().toList();

        // fill all info to DraftOrderModel

        return List.of();
    }

}
