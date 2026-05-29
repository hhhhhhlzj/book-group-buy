package com.shuxiang.groupbuy.trigger.http;

import com.shuxiang.groupbuy.api.dto.user.*;
import com.shuxiang.groupbuy.api.response.Response;
import com.shuxiang.groupbuy.domain.usercenter.model.entity.UserAddressEntity;
import com.shuxiang.groupbuy.domain.usercenter.service.IUserCenterService;
import com.shuxiang.groupbuy.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/user/")
public class UserCenterController {

    @Resource
    private IUserCenterService userCenterService;

    @PostMapping("address/list")
    public Response<AddressResponseDTO> listAddress(@RequestBody UserIdRequestDTO request) {
        try {
            List<UserAddressEntity> list = userCenterService.listAddress(request.getUserId());
            AddressResponseDTO dto = new AddressResponseDTO();
            dto.setList(list.stream().map(this::toAddressItem).collect(Collectors.toList()));
            return success(dto);
        } catch (Exception e) {
            log.error("listAddress failed userId={}", request.getUserId(), e);
            return error();
        }
    }

    @PostMapping("address/save")
    public Response<Long> saveAddress(@RequestBody AddressSaveRequestDTO request) {
        try {
            UserAddressEntity entity = UserAddressEntity.builder()
                    .id(request.getId())
                    .userId(request.getUserId())
                    .receiverName(request.getReceiverName())
                    .receiverPhone(request.getReceiverPhone())
                    .province(request.getProvince())
                    .city(request.getCity())
                    .district(request.getDistrict())
                    .detailAddress(request.getDetailAddress())
                    .defaultAddress(Boolean.TRUE.equals(request.getIsDefault()))
                    .build();
            Long id = userCenterService.saveAddress(entity);
            return success(id);
        } catch (IllegalArgumentException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("saveAddress failed", e);
            return error();
        }
    }

    @PostMapping("address/delete")
    public Response<Boolean> deleteAddress(@RequestBody AddressIdRequestDTO request) {
        try {
            userCenterService.deleteAddress(request.getUserId(), request.getAddressId());
            return success(true);
        } catch (Exception e) {
            log.error("deleteAddress failed", e);
            return error();
        }
    }

    @PostMapping("address/set_default")
    public Response<Boolean> setDefaultAddress(@RequestBody AddressIdRequestDTO request) {
        try {
            userCenterService.setDefaultAddress(request.getUserId(), request.getAddressId());
            return success(true);
        } catch (IllegalArgumentException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("setDefaultAddress failed", e);
            return error();
        }
    }

    @PostMapping("favorite/list")
    public Response<GoodsIdListResponseDTO> listFavorites(@RequestBody UserIdRequestDTO request) {
        try {
            GoodsIdListResponseDTO dto = new GoodsIdListResponseDTO();
            dto.setGoodsIds(userCenterService.listFavoriteGoodsIds(request.getUserId()));
            return success(dto);
        } catch (Exception e) {
            log.error("listFavorites failed", e);
            return error();
        }
    }

    @PostMapping("favorite/add")
    public Response<Boolean> addFavorite(@RequestBody GoodsIdRequestDTO request) {
        try {
            userCenterService.addFavorite(request.getUserId(), request.getGoodsId());
            return success(true);
        } catch (IllegalStateException e) {
            return illegal(e.getMessage());
        } catch (Exception e) {
            log.error("addFavorite failed", e);
            return error();
        }
    }

    @PostMapping("favorite/remove")
    public Response<Boolean> removeFavorite(@RequestBody GoodsIdRequestDTO request) {
        try {
            userCenterService.removeFavorite(request.getUserId(), request.getGoodsId());
            return success(true);
        } catch (Exception e) {
            log.error("removeFavorite failed", e);
            return error();
        }
    }

    @PostMapping("favorite/status")
    public Response<FavoriteStatusResponseDTO> favoriteStatus(@RequestBody GoodsIdRequestDTO request) {
        try {
            FavoriteStatusResponseDTO dto = new FavoriteStatusResponseDTO();
            dto.setFavorited(userCenterService.isFavorite(request.getUserId(), request.getGoodsId()));
            return success(dto);
        } catch (Exception e) {
            log.error("favoriteStatus failed", e);
            return error();
        }
    }

    @PostMapping("browse/list")
    public Response<GoodsIdListResponseDTO> listBrowse(@RequestBody UserIdRequestDTO request) {
        try {
            GoodsIdListResponseDTO dto = new GoodsIdListResponseDTO();
            dto.setGoodsIds(userCenterService.listBrowseGoodsIds(request.getUserId()));
            return success(dto);
        } catch (Exception e) {
            log.error("listBrowse failed", e);
            return error();
        }
    }

    @PostMapping("browse/record")
    public Response<Boolean> recordBrowse(@RequestBody GoodsIdRequestDTO request) {
        try {
            if (StringUtils.isNotBlank(request.getUserId()) && StringUtils.isNotBlank(request.getGoodsId())) {
                userCenterService.recordBrowse(request.getUserId(), request.getGoodsId());
            }
            return success(true);
        } catch (Exception e) {
            log.error("recordBrowse failed", e);
            return error();
        }
    }

    private AddressResponseDTO.AddressItem toAddressItem(UserAddressEntity entity) {
        AddressResponseDTO.AddressItem item = new AddressResponseDTO.AddressItem();
        item.setId(entity.getId());
        item.setReceiverName(entity.getReceiverName());
        item.setReceiverPhone(entity.getReceiverPhone());
        item.setProvince(entity.getProvince());
        item.setCity(entity.getCity());
        item.setDistrict(entity.getDistrict());
        item.setDetailAddress(entity.getDetailAddress());
        item.setIsDefault(entity.isDefaultAddress());
        return item;
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(Constants.ResponseCode.SUCCESS.getCode())
                .info(Constants.ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

    private <T> Response<T> error() {
        return Response.<T>builder()
                .code(Constants.ResponseCode.UN_ERROR.getCode())
                .info(Constants.ResponseCode.UN_ERROR.getInfo())
                .build();
    }

    private <T> Response<T> illegal(String message) {
        return Response.<T>builder()
                .code(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode())
                .info(StringUtils.defaultIfBlank(message, Constants.ResponseCode.ILLEGAL_PARAMETER.getInfo()))
                .build();
    }

}
