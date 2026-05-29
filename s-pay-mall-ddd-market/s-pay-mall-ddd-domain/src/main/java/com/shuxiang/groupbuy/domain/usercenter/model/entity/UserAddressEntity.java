package com.shuxiang.groupbuy.domain.usercenter.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAddressEntity {

    private Long id;
    private String userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private boolean defaultAddress;

    public String regionText() {
        return province + " " + city + " " + district;
    }

}
