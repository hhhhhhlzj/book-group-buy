package com.shuxiang.groupbuy.domain.order.model.aggregate;

import com.shuxiang.groupbuy.domain.order.model.entity.OrderEntity;
import com.shuxiang.groupbuy.domain.order.model.entity.ProductEntity;
import com.shuxiang.groupbuy.domain.order.model.valobj.OrderStatusVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderAggregate {

    private String userId;

    private ProductEntity productEntity;

    private OrderEntity orderEntity;

    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverRegion;
    private String receiverDetail;

    public static OrderEntity buildOrderEntity(String productId, String productName, Integer marketType){
        return OrderEntity.builder()
                .productId(productId)
                .productName(productName)
                .orderId(RandomStringUtils.randomNumeric(12))
                .orderTime(new Date())
                .orderStatusVO(OrderStatusVO.CREATE)
                .marketType(marketType)
                .build();
    }

}
