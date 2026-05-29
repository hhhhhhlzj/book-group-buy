package com.shuxiang.groupbuy.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserFavorite {

    private Long id;
    private String userId;
    private String goodsId;
    private Date createTime;

}
