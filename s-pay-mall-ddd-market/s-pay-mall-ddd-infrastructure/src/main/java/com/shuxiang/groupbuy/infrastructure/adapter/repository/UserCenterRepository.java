package com.shuxiang.groupbuy.infrastructure.adapter.repository;

import com.shuxiang.groupbuy.domain.usercenter.adapter.repository.IUserCenterRepository;
import com.shuxiang.groupbuy.domain.usercenter.model.entity.UserAddressEntity;
import com.shuxiang.groupbuy.infrastructure.dao.IUserCenterDao;
import com.shuxiang.groupbuy.infrastructure.dao.po.UserAddress;
import com.shuxiang.groupbuy.infrastructure.dao.po.UserBrowseHistory;
import com.shuxiang.groupbuy.infrastructure.dao.po.UserFavorite;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserCenterRepository implements IUserCenterRepository {

    private static final int BROWSE_LIMIT = 5;

    @Resource
    private IUserCenterDao userCenterDao;

    @Override
    public List<UserAddressEntity> listAddress(String userId) {
        return userCenterDao.listAddressByUserId(userId).stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public UserAddressEntity queryAddress(Long addressId) {
        UserAddress po = userCenterDao.queryAddressById(addressId);
        return po == null ? null : toEntity(po);
    }

    @Override
    public UserAddressEntity queryDefaultAddress(String userId) {
        UserAddress po = userCenterDao.queryDefaultAddress(userId);
        return po == null ? null : toEntity(po);
    }

    @Override
    public Long saveAddress(UserAddressEntity entity) {
        UserAddress po = toPo(entity);
        if (entity.getId() == null) {
            userCenterDao.insertAddress(po);
            return po.getId();
        }
        userCenterDao.updateAddress(po);
        return entity.getId();
    }

    @Override
    public void deleteAddress(String userId, Long addressId) {
        userCenterDao.deleteAddress(addressId, userId);
    }

    @Override
    public void setDefaultAddress(String userId, Long addressId) {
        userCenterDao.clearDefaultAddress(userId);
        if (addressId != null) {
            userCenterDao.setDefaultAddress(addressId, userId);
        }
    }

    @Override
    public List<String> listFavoriteGoodsIds(String userId) {
        return userCenterDao.listFavorites(userId).stream().map(UserFavorite::getGoodsId).collect(Collectors.toList());
    }

    @Override
    public boolean isFavorite(String userId, String goodsId) {
        return userCenterDao.queryFavorite(userId, goodsId) != null;
    }

    @Override
    public void addFavorite(String userId, String goodsId) {
        UserFavorite favorite = UserFavorite.builder().userId(userId).goodsId(goodsId).build();
        userCenterDao.insertFavorite(favorite);
    }

    @Override
    public void removeFavorite(String userId, String goodsId) {
        userCenterDao.deleteFavorite(userId, goodsId);
    }

    @Override
    public int countFavorites(String userId) {
        return userCenterDao.countFavorites(userId);
    }

    @Override
    public List<String> listBrowseGoodsIds(String userId) {
        return userCenterDao.listBrowseHistory(userId, BROWSE_LIMIT).stream()
                .map(UserBrowseHistory::getGoodsId).collect(Collectors.toList());
    }

    @Override
    public void recordBrowse(String userId, String goodsId) {
        userCenterDao.upsertBrowseHistory(userId, goodsId);
        userCenterDao.trimBrowseHistory(userId, BROWSE_LIMIT);
    }

    private UserAddressEntity toEntity(UserAddress po) {
        return UserAddressEntity.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .receiverName(po.getReceiverName())
                .receiverPhone(po.getReceiverPhone())
                .province(po.getProvince())
                .city(po.getCity())
                .district(po.getDistrict())
                .detailAddress(po.getDetailAddress())
                .defaultAddress(po.getIsDefault() != null && po.getIsDefault() == 1)
                .build();
    }

    private UserAddress toPo(UserAddressEntity entity) {
        return UserAddress.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .receiverName(entity.getReceiverName())
                .receiverPhone(entity.getReceiverPhone())
                .province(entity.getProvince())
                .city(entity.getCity())
                .district(entity.getDistrict())
                .detailAddress(entity.getDetailAddress())
                .isDefault(entity.isDefaultAddress() ? 1 : 0)
                .build();
    }

}
