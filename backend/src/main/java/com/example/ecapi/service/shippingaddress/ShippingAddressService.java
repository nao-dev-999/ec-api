package com.example.ecapi.service.shippingaddress;

import com.example.ecapi.entity.ShippingAddress;
import com.example.ecapi.exception.ShippingAddressNotFoundException;
import com.example.ecapi.repository.ShippingAddressRepository;
import com.example.ecapi.service.shippingaddress.dto.CreateShippingAddress;
import com.example.ecapi.service.shippingaddress.dto.ShippingAddressResult;
import com.example.ecapi.service.shippingaddress.dto.UpdateShippingAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingAddressService {

    private final ShippingAddressRepository shippingAddressRepository;

    public List<ShippingAddressResult> list(Long customerId) {
        return shippingAddressRepository
                .findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    /**
     * @throws ShippingAddressNotFoundException 自分が所有する住所でない、または存在しない場合
     */
    public ShippingAddressResult get(Long id, Long customerId) {
        return shippingAddressRepository
                .findByIdAndCustomerId(id, customerId)
                .map(this::toResult)
                .orElseThrow(() -> new ShippingAddressNotFoundException(id));
    }

    /** 顧客にとって初めての住所登録の場合、自動的にデフォルトとする。 */
    @Transactional
    public ShippingAddressResult create(CreateShippingAddress dto) {
        boolean isFirstAddress = !shippingAddressRepository.existsByCustomerId(dto.customerId());
        boolean makeDefault = dto.isDefault() || isFirstAddress;
        if (makeDefault) {
            clearExistingDefault(dto.customerId());
        }

        ShippingAddress address = new ShippingAddress();
        address.setCustomerId(dto.customerId());
        address.setRecipientName(dto.recipientName());
        address.setPostalCode(dto.postalCode());
        address.setPrefecture(dto.prefecture());
        address.setCity(dto.city());
        address.setAddressLine1(dto.addressLine1());
        address.setAddressLine2(dto.addressLine2());
        address.setPhoneNumber(dto.phoneNumber());
        address.setDefault(makeDefault);
        ShippingAddress saved = shippingAddressRepository.save(address);
        log.info("ShippingAddress created id={} customerId={}", saved.getId(), dto.customerId());
        return toResult(saved);
    }

    /**
     * @throws ShippingAddressNotFoundException 自分が所有する住所でない、または存在しない場合
     */
    @Transactional
    public ShippingAddressResult update(UpdateShippingAddress dto) {
        ShippingAddress address =
                shippingAddressRepository
                        .findByIdAndCustomerId(dto.id(), dto.customerId())
                        .orElseThrow(() -> new ShippingAddressNotFoundException(dto.id()));

        if (dto.recipientName() != null) address.setRecipientName(dto.recipientName());
        if (dto.postalCode() != null) address.setPostalCode(dto.postalCode());
        if (dto.prefecture() != null) address.setPrefecture(dto.prefecture());
        if (dto.city() != null) address.setCity(dto.city());
        if (dto.addressLine1() != null) address.setAddressLine1(dto.addressLine1());
        if (dto.addressLine2() != null) address.setAddressLine2(dto.addressLine2());
        if (dto.phoneNumber() != null) address.setPhoneNumber(dto.phoneNumber());
        if (Boolean.TRUE.equals(dto.isDefault()) && !address.isDefault()) {
            clearExistingDefault(dto.customerId());
            address.setDefault(true);
        } else if (Boolean.FALSE.equals(dto.isDefault())) {
            address.setDefault(false);
        }
        address.setVersion(dto.version());
        log.info("ShippingAddress updated id={} customerId={}", dto.id(), dto.customerId());
        return toResult(shippingAddressRepository.save(address));
    }

    /**
     * @throws ShippingAddressNotFoundException 自分が所有する住所でない、または存在しない場合
     */
    @Transactional
    public void delete(Long id, Long customerId) {
        ShippingAddress address =
                shippingAddressRepository
                        .findByIdAndCustomerId(id, customerId)
                        .orElseThrow(() -> new ShippingAddressNotFoundException(id));
        shippingAddressRepository.delete(address);
        log.info("ShippingAddress deleted id={} customerId={}", id, customerId);
    }

    private void clearExistingDefault(Long customerId) {
        List<ShippingAddress> currentDefaults =
                shippingAddressRepository.findAllByCustomerIdAndIsDefaultTrue(customerId);
        currentDefaults.forEach(a -> a.setDefault(false));
        shippingAddressRepository.saveAll(currentDefaults);
    }

    private ShippingAddressResult toResult(ShippingAddress address) {
        return new ShippingAddressResult(
                address.getId(),
                address.getRecipientName(),
                address.getPostalCode(),
                address.getPrefecture(),
                address.getCity(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getPhoneNumber(),
                address.isDefault(),
                LocalDateTime.ofInstant(address.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(address.getUpdatedAt(), ZoneId.systemDefault()),
                address.getVersion());
    }
}
