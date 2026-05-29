package com.shuxiang.groupbuy.infrastructure.dao;

import com.shuxiang.groupbuy.infrastructure.dao.po.UserAddress;
import com.shuxiang.groupbuy.infrastructure.dao.po.UserBrowseHistory;
import com.shuxiang.groupbuy.infrastructure.dao.po.UserFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IUserCenterDao {

    List<UserAddress> listAddressByUserId(@Param("userId") String userId);

    UserAddress queryAddressById(@Param("id") Long id);

    UserAddress queryDefaultAddress(@Param("userId") String userId);

    void insertAddress(UserAddress address);

    void updateAddress(UserAddress address);

    void deleteAddress(@Param("id") Long id, @Param("userId") String userId);

    void clearDefaultAddress(@Param("userId") String userId);

    void setDefaultAddress(@Param("id") Long id, @Param("userId") String userId);

    List<UserFavorite> listFavorites(@Param("userId") String userId);

    int countFavorites(@Param("userId") String userId);

    UserFavorite queryFavorite(@Param("userId") String userId, @Param("goodsId") String goodsId);

    void insertFavorite(UserFavorite favorite);

    void deleteFavorite(@Param("userId") String userId, @Param("goodsId") String goodsId);

    List<UserBrowseHistory> listBrowseHistory(@Param("userId") String userId, @Param("limit") int limit);

    void upsertBrowseHistory(@Param("userId") String userId, @Param("goodsId") String goodsId);

    void trimBrowseHistory(@Param("userId") String userId, @Param("keep") int keep);

}
