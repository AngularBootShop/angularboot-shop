package com.shop.order.mapper;

import com.shop.order.dto.OrderItemResponse;
import com.shop.order.dto.OrderResponse;
import com.shop.order.entity.Order;
import com.shop.order.entity.OrderItem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

  @Mapping(target = "items", source = "items")
  OrderResponse toResponse(Order order);

  List<OrderResponse> toResponseList(List<Order> orders);

  OrderItemResponse toItemResponse(OrderItem item);

  List<OrderItemResponse> toItemResponseList(List<OrderItem> items);
}
