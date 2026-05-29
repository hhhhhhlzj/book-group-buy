package com.shuxiang.groupbuy.domain.usercenter.service;

import com.shuxiang.groupbuy.domain.usercenter.adapter.repository.IUserCenterRepository;
import com.shuxiang.groupbuy.domain.usercenter.model.entity.UserAddressEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserCenterService implements IUserCenterService {

    private static final int FAVORITE_LIMIT = 5;
    private static final int BROWSE_LIMIT = 5;

    @Resource
    private IUserCenterRepository userCenterRepository;

    @Override
    public List<UserAddressEntity> listAddress(String userId) {
        return userCenterRepository.listAddress(userId);
    }

    @Override
    public Long saveAddress(UserAddressEntity entity) {
        validateAddress(entity);
        Long id = userCenterRepository.saveAddress(entity);
        if (entity.isDefaultAddress()) {
            userCenterRepository.setDefaultAddress(entity.getUserId(), id);
        }
        return id;
    }

    @Override
    public void deleteAddress(String userId, Long addressId) {
        userCenterRepository.deleteAddress(userId, addressId);
    }

    @Override
    public void setDefaultAddress(String userId, Long addressId) {
        UserAddressEntity address = userCenterRepository.queryAddress(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new IllegalArgumentException("地址不存在或不属于当前用户");
        }
        userCenterRepository.setDefaultAddress(userId, addressId);
    }

    @Override
    public UserAddressEntity resolveOrderAddress(String userId, Long addressId) {
        if (addressId != null) {
            UserAddressEntity address = userCenterRepository.queryAddress(addressId);
            if (address == null || !userId.equals(address.getUserId())) {
                return null;
            }
            return address;
        }
        return userCenterRepository.queryDefaultAddress(userId);
    }

    @Override
    public List<String> listFavoriteGoodsIds(String userId) {
        return userCenterRepository.listFavoriteGoodsIds(userId);
    }

    @Override
    public boolean isFavorite(String userId, String goodsId) {
        return userCenterRepository.isFavorite(userId, goodsId);
    }

    @Override
    public void addFavorite(String userId, String goodsId) {
        if (userCenterRepository.isFavorite(userId, goodsId)) {
            return;
        }
        if (userCenterRepository.countFavorites(userId) >= FAVORITE_LIMIT) {
            throw new IllegalStateException("收藏已满5条，请先取消部分收藏");
        }
        userCenterRepository.addFavorite(userId, goodsId);
    }

    @Override
    public void removeFavorite(String userId, String goodsId) {
        userCenterRepository.removeFavorite(userId, goodsId);
    }

    @Override
    public List<String> listBrowseGoodsIds(String userId) {
        return userCenterRepository.listBrowseGoodsIds(userId);
    }

    @Override
    public void recordBrowse(String userId, String goodsId) {
        userCenterRepository.recordBrowse(userId, goodsId);
    }

    private void validateAddress(UserAddressEntity entity) {
        if (StringUtils.isAnyBlank(entity.getUserId(), entity.getReceiverName(), entity.getReceiverPhone(),
                entity.getProvince(), entity.getCity(), entity.getDistrict(), entity.getDetailAddress())) {
            throw new IllegalArgumentException("地址信息不完整");
        }
    }

}
