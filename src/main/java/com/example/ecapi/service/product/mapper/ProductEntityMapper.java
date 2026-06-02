package com.example.ecapi.service.product.mapper;

import com.example.ecapi.entity.Product;
import com.example.ecapi.service.product.dto.CreateProduct;
import com.example.ecapi.service.product.dto.ProductResult;
import com.example.ecapi.service.product.dto.UpdateProduct;
import java.util.List;
import org.mapstruct.*;

/** Entity <-> Service DTO のマッパー（MapStruct） */
@Mapper(componentModel = "spring")
public interface ProductEntityMapper {

    // Product entity -> ProductResult (service DTO)
    ProductResult toProductResult(Product product);

    List<ProductResult> toProductResultList(List<Product> products);

    // CreateProduct (service DTO) -> Product entity
    Product toProduct(CreateProduct dto);

    /**
     * 既存エンティティを UpdateProduct の内容で更新します。 UpdateProduct の null のフィールドは無視され、エンティティの対応するフィールドは変更されません。
     * version フィールドも含まれます。
     *
     * @param dto 更新情報を含む DTO
     * @param entity 更新対象のエンティティ
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProductFromUpdate(UpdateProduct dto, @MappingTarget Product entity);
}
