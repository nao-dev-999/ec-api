package com.example.ecapi.batch.job;

import com.example.ecapi.repository.CustomerOrderRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

/**
 * 顧客IDではなく CustomerOrder.id のレンジをパーティションキーとする（14.5節参照）。
 * 顧客IDだと注文の時間的発生ムラの影響を受けやすいため、ID範囲の方が均等な負荷分散になりやすい。
 */
public class OrderAggregationPartitioner implements Partitioner {

    private final CustomerOrderRepository customerOrderRepository;

    public OrderAggregationPartitioner(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long maxId = customerOrderRepository.findMaxId();
        long rangeSize = Math.max(maxId / gridSize, 1);

        Map<String, ExecutionContext> partitions = new HashMap<>();
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minId", i * rangeSize);
            ctx.putLong("maxId", i == gridSize - 1 ? maxId : (i + 1) * rangeSize);
            partitions.put("partition" + i, ctx);
        }
        return partitions;
    }
}
