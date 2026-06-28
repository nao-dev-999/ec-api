# 1. パッケージ構成

> [← インデックスに戻る](../コーディング規約.md)

---

```
com.example.ecapi
├── config/           # Bean定義・設定クラス（Security, Redis, MVC, AOP等）
├── constant/         # Enum・定数
├── controller/
│   ├── admin/        # 管理者向けエンドポイント
│   │   ├── dto/      # リクエスト/レスポンスDTO
│   │   └── mapper/   # DTO ↔ ServiceDTO 変換
│   └── customer/     # 顧客向けエンドポイント
│       ├── dto/
│       └── mapper/
├── entity/           # JPAエンティティ
├── exception/        # 例外クラス・GlobalExceptionHandler
├── helper/           # ユーティリティ（MessageHelper等）
├── repository/       # Spring Data JPAリポジトリ
└── service/
    ├── auth/
    ├── order/
    │   ├── dto/      # サービス層の入出力オブジェクト
    │   └── mapper/   # Entity ↔ ServiceDTO 変換
    └── product/
        ├── dto/
        └── mapper/
```

**原則:**

- `controller/dto/` はHTTP層の関心事（バリデーション・シリアライズ）のみを持つ
- `service/dto/` はビジネスロジックの入出力を表現する。HTTPに依存しない
- `entity/` はJPA管理下のオブジェクト。SpringやHTTPに依存しない
- `config/` に入るのは `@Bean` / `@Configuration` / `@Aspect` のみ。ビジネスロジックを置かない
