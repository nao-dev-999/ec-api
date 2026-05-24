package com.example.ecapi.controller.order.mapper;

import com.example.ecapi.controller.order.dto.OrderRequest;
import com.example.ecapi.controller.order.dto.OrderResponse;
import com.example.ecapi.service.order.dto.CreateOrder;
import com.example.ecapi.service.order.dto.OrderResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderApiMapper {

    CreateOrder toCreateOrder(OrderRequest request);

    /**
     * OrderResult (Service DTO) から OrderResponse (Controller DTO) へマッピングします。 version フィールドも含まれます。
     *
     * @param result マッピング元の OrderResult
     * @return マッピング先の OrderResponse
     */
    OrderResponse toOrderResponse(OrderResult result);

    /**
     * OrderResult のリストから OrderResponse のリストへマッピングします。
     *
     * @param results マッピング元の OrderResult のリスト
     * @return マッピング先の OrderResponse のリスト
     */
    List<OrderResponse> toOrderResponseList(List<OrderResult> results);
}
