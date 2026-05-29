package com.shuxiang.groupbuy.api.dto.user;

import lombok.Data;

@Data
public class AddressSaveRequestDTO {

    private String userId;
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;

}
