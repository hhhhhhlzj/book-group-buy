package com.shuxiang.groupbuy.domain.usercenter.adapter.repository;

import com.shuxiang.groupbuy.domain.usercenter.model.entity.UserAddressEntity;

import java.util.List;

public interface IUserCenterRepository {

    List<UserAddressEntity> listAddress(String userId);

    UserAddressEntity queryAddress(Long addressId);

    UserAddressEntity queryDefaultAddress(String userId);

    Long saveAddress(UserAddressEntity entity);

    void deleteAddress(String userId, Long addressId);

    void setDefaultAddress(String userId, Long addressId);

    List<String> listFavoriteGoodsIds(String userId);

    boolean isFavorite(String userId, String goodsId);

    void addFavorite(String userId, String goodsId);

    void removeFavorite(String userId, String goodsId);

    int countFavorites(String userId);

    List<String> listBrowseGoodsIds(String userId);

    void recordBrowse(String userId, String goodsId);

}
