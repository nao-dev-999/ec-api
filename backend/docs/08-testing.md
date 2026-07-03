# 8. テスト

> [← インデックスに戻る](../コーディング規約.md)

---

## Controller テスト

```java
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProductService productService;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createProduct_shouldReturn201() throws Exception {
        // given
        var result = new ProductResult(1L, "商品A", 1000, 10);
        given(productService.createProduct(any())).willReturn(result);

        // when / then
        mockMvc.perform(post("/api/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"商品A","price":1000,"stock":10}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L));
    }
}
```

---

## Service テスト

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks ProductService productService;
    @Mock ProductRepository productRepository;
    @Mock ProductEntityMapper mapper;
    @Mock MessageHelper messageHelper;

    @Test
    void getProduct_whenNotFound_shouldThrow() {
        given(productRepository.findByIdAndDeletedFalse(99L)).willReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class,
            () -> productService.getProduct(99L));
    }
}
```

---

## 原則

- Controller テストは `@WebMvcTest`（Spring MVC層のみ起動）
- Service テストは `@ExtendWith(MockitoExtension.class)`（Spring context 不要）
- `@SpringBootTest` は統合テスト（E2E）に限定する
- テストメソッド名: `{メソッド名}_{条件}_{期待結果}` （例: `getProduct_whenNotFound_shouldThrow`）

---

## JaCoCo

```kotlin
// build.gradle.kts
jacoco {
    toolVersion = "0.8.14"  // Java 25 対応（0.8.12 は非対応）
}
```
