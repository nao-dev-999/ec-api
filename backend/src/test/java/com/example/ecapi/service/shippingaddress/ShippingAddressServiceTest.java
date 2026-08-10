package com.example.ecapi.service.shippingaddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.entity.ShippingAddress;
import com.example.ecapi.exception.ShippingAddressNotFoundException;
import com.example.ecapi.repository.ShippingAddressRepository;
import com.example.ecapi.service.shippingaddress.dto.CreateShippingAddress;
import com.example.ecapi.service.shippingaddress.dto.ShippingAddressResult;
import com.example.ecapi.service.shippingaddress.dto.UpdateShippingAddress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShippingAddressServiceTest {

    @Mock private ShippingAddressRepository shippingAddressRepository;

    @InjectMocks private ShippingAddressService shippingAddressService;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long ADDRESS_ID = 10L;

    private ShippingAddress address;

    @BeforeEach
    void setUp() {
        address = new ShippingAddress();
        address.setId(ADDRESS_ID);
        address.setCustomerId(CUSTOMER_ID);
        address.setRecipientName("Test Recipient");
        address.setPostalCode("100-0001");
        address.setPrefecture("東京都");
        address.setCity("千代田区");
        address.setAddressLine1("1-1-1");
        address.setPhoneNumber("090-1111-2222");
        address.setDefault(true);
        address.setVersion(0);
        ReflectionTestUtils.setField(address, "createdAt", Instant.now());
        ReflectionTestUtils.setField(address, "updatedAt", Instant.now());
    }

    @Nested
    @DisplayName("list")
    class ListTest {

        @Test
        @DisplayName("顧客の配送先住所一覧を取得できること")
        void shouldReturnList() {
            when(shippingAddressRepository.findAllByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID))
                    .thenReturn(List.of(address));

            List<ShippingAddressResult> result = shippingAddressService.list(CUSTOMER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo(ADDRESS_ID);
        }

        @Test
        @DisplayName("登録がない場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoAddresses() {
            when(shippingAddressRepository.findAllByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID))
                    .thenReturn(List.of());

            List<ShippingAddressResult> result = shippingAddressService.list(CUSTOMER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("get")
    class GetTest {

        @Test
        @DisplayName("自分の配送先住所を取得できること")
        void shouldReturnAddress() {
            when(shippingAddressRepository.findByIdAndCustomerId(ADDRESS_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(address));

            ShippingAddressResult result = shippingAddressService.get(ADDRESS_ID, CUSTOMER_ID);

            assertThat(result.id()).isEqualTo(ADDRESS_ID);
            assertThat(result.recipientName()).isEqualTo("Test Recipient");
        }

        @Test
        @DisplayName("存在しない、または他顧客の住所の場合、ShippingAddressNotFoundException をスローすること")
        void shouldThrowExceptionWhenNotFound() {
            when(shippingAddressRepository.findByIdAndCustomerId(ADDRESS_ID, CUSTOMER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> shippingAddressService.get(ADDRESS_ID, CUSTOMER_ID))
                    .isInstanceOf(ShippingAddressNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("初めての住所登録の場合、isDefaultの指定に関わらず自動的にデフォルトになること")
        void shouldMakeFirstAddressDefault() {
            CreateShippingAddress dto =
                    new CreateShippingAddress(
                            CUSTOMER_ID,
                            "Test Recipient",
                            "100-0001",
                            "東京都",
                            "千代田区",
                            "1-1-1",
                            null,
                            "090-1111-2222",
                            false);
            when(shippingAddressRepository.existsByCustomerId(CUSTOMER_ID)).thenReturn(false);
            when(shippingAddressRepository.findAllByCustomerIdAndIsDefaultTrue(CUSTOMER_ID))
                    .thenReturn(List.of());
            when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(address);

            ShippingAddressResult result = shippingAddressService.create(dto);

            assertThat(result.isDefault()).isTrue();
        }

        @Test
        @DisplayName("2件目以降でisDefault=trueを指定した場合、既存のデフォルトが解除されること")
        void shouldClearExistingDefaultWhenExplicitlySetAsDefault() {
            CreateShippingAddress dto =
                    new CreateShippingAddress(
                            CUSTOMER_ID,
                            "New Recipient",
                            "100-0002",
                            "東京都",
                            "港区",
                            "2-2-2",
                            null,
                            "090-3333-4444",
                            true);
            ShippingAddress existingDefault = new ShippingAddress();
            existingDefault.setId(1L);
            existingDefault.setCustomerId(CUSTOMER_ID);
            existingDefault.setDefault(true);
            when(shippingAddressRepository.existsByCustomerId(CUSTOMER_ID)).thenReturn(true);
            when(shippingAddressRepository.findAllByCustomerIdAndIsDefaultTrue(CUSTOMER_ID))
                    .thenReturn(List.of(existingDefault));
            when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(address);

            shippingAddressService.create(dto);

            assertThat(existingDefault.isDefault()).isFalse();
            verify(shippingAddressRepository).saveAll(List.of(existingDefault));
        }

        @Test
        @DisplayName("2件目以降でisDefault=falseを指定した場合、デフォルトにならないこと")
        void shouldNotBeDefaultWhenNotFirstAndNotExplicit() {
            CreateShippingAddress dto =
                    new CreateShippingAddress(
                            CUSTOMER_ID,
                            "New Recipient",
                            "100-0002",
                            "東京都",
                            "港区",
                            "2-2-2",
                            null,
                            "090-3333-4444",
                            false);
            when(shippingAddressRepository.existsByCustomerId(CUSTOMER_ID)).thenReturn(true);
            org.mockito.ArgumentCaptor<ShippingAddress> captor =
                    org.mockito.ArgumentCaptor.forClass(ShippingAddress.class);
            when(shippingAddressRepository.save(captor.capture())).thenReturn(address);

            shippingAddressService.create(dto);

            assertThat(captor.getValue().isDefault()).isFalse();
            verify(shippingAddressRepository, never()).findAllByCustomerIdAndIsDefaultTrue(any());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("指定したフィールドのみ更新されること")
        void shouldUpdateOnlySpecifiedFields() {
            UpdateShippingAddress dto =
                    new UpdateShippingAddress(
                            ADDRESS_ID,
                            CUSTOMER_ID,
                            "Updated Recipient",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            0);
            when(shippingAddressRepository.findByIdAndCustomerId(ADDRESS_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(address));
            when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(address);

            ShippingAddressResult result = shippingAddressService.update(dto);

            assertThat(result.recipientName()).isEqualTo("Updated Recipient");
            assertThat(address.getPostalCode()).isEqualTo("100-0001");
        }

        @Test
        @DisplayName("isDefault=trueに更新した場合、既存のデフォルトが解除されること")
        void shouldClearExistingDefaultWhenSetAsDefault() {
            address.setDefault(false);
            ShippingAddress existingDefault = new ShippingAddress();
            existingDefault.setId(2L);
            existingDefault.setCustomerId(CUSTOMER_ID);
            existingDefault.setDefault(true);
            UpdateShippingAddress dto =
                    new UpdateShippingAddress(
                            ADDRESS_ID,
                            CUSTOMER_ID,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            true,
                            0);
            when(shippingAddressRepository.findByIdAndCustomerId(ADDRESS_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(address));
            when(shippingAddressRepository.findAllByCustomerIdAndIsDefaultTrue(CUSTOMER_ID))
                    .thenReturn(List.of(existingDefault));
            when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(address);

            ShippingAddressResult result = shippingAddressService.update(dto);

            assertThat(result.isDefault()).isTrue();
            assertThat(existingDefault.isDefault()).isFalse();
        }

        @Test
        @DisplayName("isDefault=falseに更新した場合、デフォルトが解除されること")
        void shouldUnsetDefault() {
            UpdateShippingAddress dto =
                    new UpdateShippingAddress(
                            ADDRESS_ID,
                            CUSTOMER_ID,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false,
                            0);
            when(shippingAddressRepository.findByIdAndCustomerId(ADDRESS_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(address));
            when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(address);

            ShippingAddressResult result = shippingAddressService.update(dto);

            assertThat(result.isDefault()).isFalse();
            verify(shippingAddressRepository, never()).findAllByCustomerIdAndIsDefaultTrue(any());
        }

        @Test
        @DisplayName("すでにデフォルトの住所をisDefault=trueで更新しても、既存デフォルトの解除処理は呼ばれないこと")
        void shouldNotClearDefaultWhenAlreadyDefault() {
            UpdateShippingAddress dto =
                    new UpdateShippingAddress(
                            ADDRESS_ID,
                            CUSTOMER_ID,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            true,
                            0);
            when(shippingAddressRepository.findByIdAndCustomerId(ADDRESS_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(address));
            when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(address);

            shippingAddressService.update(dto);

            verify(shippingAddressRepository, never()).findAllByCustomerIdAndIsDefaultTrue(any());
        }

        @Test
        @DisplayName("存在しない、または他顧客の住所の場合、ShippingAddressNotFoundException をスローすること")
        void shouldThrowExceptionWhenNotFound() {
            UpdateShippingAddress dto =
                    new UpdateShippingAddress(
                            ADDRESS_ID,
                            CUSTOMER_ID,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            0);
            when(shippingAddressRepository.findByIdAndCustomerId(ADDRESS_ID, CUSTOMER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> shippingAddressService.update(dto))
                    .isInstanceOf(ShippingAddressNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTest {

        @Test
        @DisplayName("自分の配送先住所を削除できること")
        void shouldDeleteAddress() {
            when(shippingAddressRepository.findByIdAndCustomerId(ADDRESS_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(address));

            shippingAddressService.delete(ADDRESS_ID, CUSTOMER_ID);

            verify(shippingAddressRepository, times(1)).delete(address);
        }

        @Test
        @DisplayName("存在しない、または他顧客の住所の場合、ShippingAddressNotFoundException をスローすること")
        void shouldThrowExceptionWhenNotFound() {
            when(shippingAddressRepository.findByIdAndCustomerId(ADDRESS_ID, CUSTOMER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> shippingAddressService.delete(ADDRESS_ID, CUSTOMER_ID))
                    .isInstanceOf(ShippingAddressNotFoundException.class);
            verify(shippingAddressRepository, never()).delete(any());
        }
    }
}
