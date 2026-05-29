package com.shuxiang.groupbuy.api.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class AddressResponseDTO {

    private List<AddressItem> list;

    @Data
    public static class AddressItem {
        private Long id;
        private String receiverName;
        private String receiverPhone;
        private String province;
        private String city;
        private String district;
        private String detailAddress;
        private Boolean isDefault;
    }

}
