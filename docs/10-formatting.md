# 10. フォーマット

> [← インデックスに戻る](../コーディング規約.md)

---

## Spotless / Google Java Format

```kotlin
// build.gradle.kts
spotless {
    java {
        googleJavaFormat()
        importOrder()
        removeUnusedImports()
    }
}
```

**import 順序（Google Java Format 準拠）:**

1. 通常 import をアルファベット順で1グループ
2. 空行
3. static import をアルファベット順で1グループ

```java
// ✅ 正しい順序
import com.example.ecapi.entity.Product;
import java.time.Instant;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.BDDMockito.given;
```

---

## その他

- インデントは4スペース（Spotlessが強制）
- 行末の空白は不可（Spotlessが強制）
- 1行の最大文字数は100文字（Google Java Format デフォルト）
- `var` は型が明らかな場合に使用する（`var list = new ArrayList<String>()` は可、`var x = someMethod()` は型が不明瞭なら避ける）
