package com.shuxiang.groupbuy.domain.usercenter.service;

import com.shuxiang.groupbuy.domain.usercenter.model.entity.UserAddressEntity;

import java.util.List;

public interface IUserCenterService {

    List<UserAddressEntity> listAddress(String userId);

    Long saveAddress(UserAddressEntity entity);

    void deleteAddress(String userId, Long addressId);

    void setDefaultAddress(String userId, Long addressId);

    UserAddressEntity resolveOrderAddress(String userId, Long addressId);

    List<String> listFavoriteGoodsIds(String userId);

    boolean isFavorite(String userId, String goodsId);

    void addFavorite(String userId, String goodsId);

    void removeFavorite(String userId, String goodsId);

    List<String> listBrowseGoodsIds(String userId);

    void recordBrowse(String userId, String goodsId);

}
