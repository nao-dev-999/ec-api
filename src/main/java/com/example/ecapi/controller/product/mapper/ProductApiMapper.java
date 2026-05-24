package com.example.ecapi.controller.product.mapper;

import com.example.ecapi.controller.product.dto.CreateProductRequest;
import com.example.ecapi.controller.product.dto.ProductResponse;
import com.example.ecapi.controller.product.dto.UpdateProductRequest;
import com.example.ecapi.service.product.dto.CreateProduct;
import com.example.ecapi.service.product.dto.ProductResult;
import com.example.ecapi.service.product.dto.UpdateProduct;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductApiMapper {

    CreateProduct toCreateProduct(CreateProductRequest request);

    // UpdateProductRequest から UpdateProduct へのマッピングに version を含める
    UpdateProduct toUpdateProduct(UpdateProductRequest request);

    ProductResponse toProductResponse(ProductResult result);

    List<ProductResponse> toProductResponseList(List<ProductResult> results);
}
