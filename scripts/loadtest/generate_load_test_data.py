#!/usr/bin/env python3
"""ec-api 負荷試験用のテストデータ(CSV)を生成する。

外部ライブラリ(Faker/numpy等)には依存せず、標準ライブラリのみで動作する。
RDS等の踏み台環境に Python3 だけあれば実行できることを優先した。

生成対象と件数目安は scripts/loadtest/README.md を参照。
出力した CSV は load_into_db.sh で `psql \\copy` を使って投入する想定。
"""

from __future__ import annotations

import argparse
import csv
import itertools
import os
import random
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

# 平文 "password123" の bcrypt(strength=12, BCryptPasswordEncoder)ハッシュ。
# `new BCryptPasswordEncoder(12).encode("password123")` で生成し、.matches() で実際に
# 検証済み。開発用シード(R__init_data.sql)のコメントには同じ平文とあるハッシュ値が
# 実際には一致しない(検証の結果判明した別の既知不具合)ため、それとは異なる値を使う。
# 生成した顧客全員が同じパスワードでログインできるようにし、負荷試験シナリオ側の
# 認証情報管理を単純化する。
PASSWORD_HASH = "$2a$12$mOAKqoGd3QdlgTlnNkp99uhvyNDkVuQ4m1dVOpIcBWWa01pK4aHCm"

JST = timezone(timedelta(hours=9))

LAST_NAMES = [
    "山田", "鈴木", "佐藤", "田中", "伊藤", "渡辺", "加藤", "吉田", "山本", "中村",
    "小林", "斎藤", "松本", "井上", "木村", "林", "清水", "山崎", "森", "阿部",
    "池田", "橋本", "山口", "石川", "前田", "藤田", "後藤", "岡田", "長谷川", "村上",
    "近藤", "石井", "西村", "坂本", "遠藤", "青木", "福田", "藤井", "西田", "宮本",
    "岩崎", "中島", "小川", "杉本", "原田", "竹内", "中野", "川口", "上田", "太田",
]
FIRST_NAMES = [
    "太郎", "愛子", "結衣", "健一", "彩", "智子", "翔太", "美咲", "大輔", "陽菜",
    "拓也", "さくら", "直樹", "由美", "健太", "真理子", "亮", "香織", "誠", "麻衣",
]

PREFECTURE_CITY = [
    ("東京都", "千代田区"), ("東京都", "新宿区"), ("神奈川県", "横浜市"),
    ("大阪府", "大阪市"), ("愛知県", "名古屋市"), ("福岡県", "福岡市"),
    ("北海道", "札幌市"), ("宮城県", "仙台市"), ("広島県", "広島市"),
    ("埼玉県", "さいたま市"), ("千葉県", "千葉市"), ("兵庫県", "神戸市"),
]

# category テーブルの行(=検索/一覧のカテゴリ軸)であり、同時に商品名生成の名詞プールでもある。
# 値(重み)は「その名詞がどれだけ商品名に出現しやすいか」を表し、検索ヒット件数の粗密を作る。
CATEGORY_NOUNS: dict[str, list[tuple[str, int]]] = {
    "パソコン": [("ノートPC", 10), ("デスクトップPC", 6), ("ミニPC", 4), ("タブレットPC", 4), ("2in1PC", 3)],
    "PC周辺機器": [("マウス", 12), ("キーボード", 10), ("モニター", 9), ("USBハブ", 6),
                   ("ドッキングステーション", 3), ("ウェブカメラ", 5)],
    "オーディオ": [("ヘッドセット", 8), ("イヤホン", 10), ("スピーカー", 7), ("サウンドバー", 3), ("マイク", 4)],
    "ストレージ": [("外付けSSD", 7), ("外付けHDD", 5), ("microSDカード", 8), ("USBメモリ", 8), ("NAS", 3)],
    "ネットワーク機器": [("無線LANルーター", 6), ("メッシュWi-Fiルーター", 3), ("Wi-Fi中継機", 4),
                         ("LANケーブル", 6), ("スイッチングハブ", 3)],
    "モバイル": [("スマートフォン", 9), ("タブレット", 7), ("スマートウォッチ", 6),
                 ("フィットネストラッカー", 4), ("モバイルバッテリー", 8)],
    "スマートホーム": [("スマートリモコン", 4), ("スマートプラグ", 4), ("スマート電球", 4),
                       ("ネットワークカメラ", 5), ("ドアベルカメラ", 3)],
    "カメラ": [("デジタル一眼カメラ", 4), ("ミラーレスカメラ", 4), ("コンパクトデジタルカメラ", 4),
               ("アクションカメラ", 4), ("交換レンズ", 3), ("三脚", 4)],
    "プリンターOA機器": [("家庭用プリンター", 5), ("ビジネス用プリンター", 3), ("ラベルプリンター", 3),
                         ("シュレッダー", 3), ("スキャナー", 3)],
    "オフィス家具": [("電動昇降デスク", 3), ("オフィスチェア", 4), ("モニターアーム", 4), ("ノートPCスタンド", 4)],
    "PCパーツ": [("PCケース", 3), ("電源ユニット", 3), ("グラフィックボード", 4), ("CPUクーラー", 4),
                 ("メモリ", 5), ("内蔵SSD", 5), ("マザーボード", 3)],
    "ゲーミング": [("ゲーミングマウス", 5), ("ゲーミングキーボード", 5), ("ゲーミングモニター", 4),
                   ("ゲームパッド", 5), ("ゲーミングチェア", 3), ("ゲーミングヘッドセット", 4)],
    "ケーブル充電": [("USB充電器", 7), ("充電ケーブル", 8), ("電源タップ", 5), ("変換アダプタ", 5)],
    "生活家電": [("掃除機", 6), ("空気清浄機", 5), ("加湿器", 5), ("扇風機", 5), ("電気ケトル", 4)],
    "キッチン家電": [("電子レンジ", 4), ("炊飯器", 4), ("コーヒーメーカー", 5), ("トースター", 4), ("ミキサー", 4)],
}

# 商品名に付く形容詞。"ワイヤレス" だけ重みを大きくし、検索負荷試験用の「大量ヒットキーワード」にする。
ADJECTIVES: list[tuple[str, int]] = [
    ("", 20),
    ("ワイヤレス", 16),
    ("軽量", 8),
    ("高性能", 8),
    ("コンパクト", 7),
    ("業務用", 5),
    ("家庭用", 5),
    ("防水", 4),
    ("静音", 4),
    ("大容量", 4),
    ("薄型", 4),
    ("プレミアム", 2),
    ("エントリーモデル", 2),
]

VARIANTS: list[str] = [
    "", "", "", "Pro", "Lite", "Plus", "MAX", "2", "3",
    "13インチ", "15インチ", "27インチ", "ブラック", "ホワイト", "シルバー",
]

DESCRIPTION_FEATURES = [
    "長時間駆動のバッテリーを搭載", "耐久性に優れた設計", "初心者にも扱いやすい操作性",
    "在宅ワークにも最適", "コンパクトで持ち運びやすい", "静音設計で夜間でも使いやすい",
    "省スペースに設置できる", "高い費用対効果が魅力", "最新モデルとの互換性を確保",
]

# 検索負荷試験で「0件ヒット」を確認するために予約しておくキーワード(商品名・説明文には絶対に出現させない)。
ZERO_HIT_KEYWORD = "ZZZ-LOADTEST-NOHIT"


@dataclass
class Args:
    out_dir: str
    categories: int
    products: int
    customers: int
    orders: int
    cart_items: int
    years: int
    zipf_s: float
    review_pool_size: int
    review_sample_rate: float
    seed: int


def parse_args() -> Args:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--out-dir", default="scripts/loadtest/out")
    # CATEGORY_NOUNS の商品名詞をそのままカテゴリ名としても使うため、既定値はその総数(78)に合わせている。
    # これより大きい値を指定すると "その他カテゴリNNN" という穴埋め名で不足分を補う。
    p.add_argument("--categories", type=int, default=78)
    p.add_argument("--products", type=int, default=100_000)
    p.add_argument("--customers", type=int, default=150_000)
    p.add_argument("--orders", type=int, default=550_000)
    p.add_argument("--cart-items", type=int, default=8_000)
    p.add_argument("--years", type=int, default=5, help="データを分散させる過去年数")
    p.add_argument("--zipf-s", type=float, default=0.9, help="商品人気度の偏り指数(大きいほど偏る)")
    p.add_argument("--review-pool-size", type=int, default=400_000,
                    help="レビュー候補として保持する(顧客,商品)ペアのリザーバーサイズ")
    p.add_argument("--review-sample-rate", type=float, default=0.35,
                    help="候補プールのうちレビュー化する割合")
    p.add_argument("--seed", type=int, default=42)
    ns = p.parse_args()
    return Args(
        out_dir=ns.out_dir,
        categories=ns.categories,
        products=ns.products,
        customers=ns.customers,
        orders=ns.orders,
        cart_items=ns.cart_items,
        years=ns.years,
        zipf_s=ns.zipf_s,
        review_pool_size=ns.review_pool_size,
        review_sample_rate=ns.review_sample_rate,
        seed=ns.seed,
    )


def fmt_ts(dt: datetime) -> str:
    return dt.astimezone(JST).strftime("%Y-%m-%d %H:%M:%S")


def random_ts(rng: random.Random, start: datetime, end: datetime) -> datetime:
    delta = end - start
    seconds = rng.random() * delta.total_seconds()
    return start + timedelta(seconds=seconds)


def weighted_growth_ts(rng: random.Random, start: datetime, end: datetime) -> datetime:
    """事業成長を模して直近ほど発生確率が高くなるタイムスタンプ(ベータ分布風)を返す。"""
    delta = end - start
    # べき乗を使うと 0-1 の一様乱数を「後半に寄せる」ことができる(t^(1/2.2) は右寄り)。
    t = rng.random() ** (1 / 2.2)
    return start + timedelta(seconds=t * delta.total_seconds())


class RowIdCounter:
    def __init__(self, start: int = 1) -> None:
        self._next = start

    def next(self) -> int:
        v = self._next
        self._next += 1
        return v


def build_categories(rng: random.Random, count: int) -> list[tuple[int, str]]:
    all_nouns: list[str] = []
    for nouns in CATEGORY_NOUNS.values():
        all_nouns.extend(name for name, _ in nouns)
    # カテゴリ名は一意制約があるため、不足分は連番サフィックスで補う。
    names: list[str] = list(dict.fromkeys(all_nouns))
    idx = 1
    while len(names) < count:
        names.append(f"その他カテゴリ{idx:03d}")
        idx += 1
    names = names[:count]
    return [(i + 1, name) for i, name in enumerate(names)]


def weighted_noun_pool() -> list[tuple[str, str, int]]:
    """(major, noun, weight) のフラットなリストを返す。"""
    pool = []
    for major, nouns in CATEGORY_NOUNS.items():
        for noun, weight in nouns:
            pool.append((major, noun, weight))
    return pool


def build_zipf_weights(n: int, s: float) -> list[float]:
    return [1.0 / ((rank + 1) ** s) for rank in range(n)]


def write_categories(path: str, categories: list[tuple[int, str]], now: datetime) -> None:
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["id", "name", "created_at", "updated_at", "version", "is_deleted"])
        for cid, name in categories:
            w.writerow([cid, name, fmt_ts(now), fmt_ts(now), 0, "f"])


def generate_products(
    rng: random.Random,
    args: Args,
    start: datetime,
    end: datetime,
    out_path: str,
) -> tuple[list[float], dict[str, int]]:
    """商品CSVを書き出し、(人気度重みリスト, キーワード出現数) を返す。"""
    noun_pool = weighted_noun_pool()
    noun_pairs = [(m, n) for m, n, _ in noun_pool]
    noun_weights = [w for _, _, w in noun_pool]
    adj_names = [a for a, _ in ADJECTIVES]
    adj_weights = [w for _, w in ADJECTIVES]

    zipf_weights = build_zipf_weights(args.products, args.zipf_s)
    keyword_counts: dict[str, int] = {}

    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow([
            "id", "name", "description", "price", "stock",
            "created_at", "updated_at", "version", "is_deleted", "image_url",
        ])
        for i in range(1, args.products + 1):
            _major, noun = rng.choices(noun_pairs, weights=noun_weights, k=1)[0]
            adjective = rng.choices(adj_names, weights=adj_weights, k=1)[0]
            variant = rng.choice(VARIANTS)
            code = f"EC-{i:07d}"
            name_parts = [p for p in (adjective, noun, variant) if p]
            name = " ".join(name_parts) + f" [{code}]"

            feature = rng.choice(DESCRIPTION_FEATURES)
            description = f"{noun}。{feature}。型番{code}。"

            for kw in (adjective, noun):
                if kw:
                    keyword_counts[kw] = keyword_counts.get(kw, 0) + 1

            price = round(rng.lognormvariate(9.0, 0.9) / 10) * 10
            price = max(500, min(price, 500_000))
            stock = max(0, int(rng.gauss(40, 30)))
            # 5% は在庫切れ、2% は低在庫アラート境界(閾値10)ちょうどにして admin/low-stock の
            # 境界値テストにも使えるようにする。
            roll = rng.random()
            if roll < 0.05:
                stock = 0
            elif roll < 0.07:
                stock = 10

            created = start + timedelta(
                seconds=(i - 1) / args.products * (end - start).total_seconds()
            )
            is_deleted = "t" if rng.random() < 0.03 else "f"
            image_url = f"https://example.com/images/products/{i}.jpg" if rng.random() < 0.7 else ""

            w.writerow([
                i, name, description, f"{price}.00", stock,
                fmt_ts(created), fmt_ts(created), 0, is_deleted, image_url,
            ])

            if i % 20_000 == 0:
                print(f"  product: {i}/{args.products}")

    return zipf_weights, keyword_counts


def write_product_categories(
    rng: random.Random, out_path: str, product_count: int, categories: list[tuple[int, str]]
) -> None:
    category_ids = [cid for cid, _ in categories]
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["product_id", "category_id"])
        for pid in range(1, product_count + 1):
            k = rng.choices([1, 2, 3], weights=[60, 30, 10], k=1)[0]
            for cid in rng.sample(category_ids, k=min(k, len(category_ids))):
                w.writerow([pid, cid])


def generate_customers(
    rng: random.Random, args: Args, start: datetime, end: datetime, out_path: str
) -> None:
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow([
            "id", "email", "password", "last_name", "first_name",
            "phone_number", "created_at", "updated_at", "version", "is_deleted",
        ])
        for i in range(1, args.customers + 1):
            last = rng.choice(LAST_NAMES)
            first = rng.choice(FIRST_NAMES)
            email = f"loadtest-customer-{i:07d}@example.com"
            phone = f"090-{rng.randint(1000, 9999)}-{rng.randint(1000, 9999)}"
            created = weighted_growth_ts(rng, start, end)
            is_deleted = "t" if rng.random() < 0.01 else "f"
            w.writerow([
                i, email, PASSWORD_HASH, last, first, phone,
                fmt_ts(created), fmt_ts(created), 0, is_deleted,
            ])
            if i % 30_000 == 0:
                print(f"  customer: {i}/{args.customers}")


def pick_order_status(rng: random.Random, ordered_at: datetime, now: datetime) -> str:
    age_days = (now - ordered_at).total_seconds() / 86400
    if age_days > 14:
        return rng.choices(["DELIVERED", "CANCELLED"], weights=[92, 8], k=1)[0]
    return rng.choices(
        ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"],
        weights=[25, 20, 20, 25, 10],
        k=1,
    )[0]


def generate_orders_and_details_and_reviews(
    rng: random.Random,
    args: Args,
    product_weights: list[float],
    product_prices: list[int],
    start: datetime,
    end: datetime,
    now: datetime,
    orders_path: str,
    details_path: str,
    reviews_path: str,
) -> None:
    product_ids = list(range(1, args.products + 1))
    # 商品人気度に応じた累積重み。random.choices の cum_weights は一度作れば
    # 大量サンプリングを O(log n) で回せる。
    product_cum_weights = list(itertools.accumulate(product_weights))

    # 顧客側にも軽い偏り(一部が優良顧客)を持たせ、注文履歴ページングの深さに差を作る。
    customer_weights = [1.0 / (r ** 0.5) for r in range(1, args.customers + 1)]
    rng.shuffle(customer_weights)  # 重みをID順から切り離す(常連顧客がID順に偏らないように)
    customer_cum_weights = list(itertools.accumulate(customer_weights))
    customer_ids = list(range(1, args.customers + 1))

    order_detail_id = RowIdCounter()
    review_pool: list[tuple[int, int, str]] = []  # (customer_id, product_id, order_detail_created_at)
    seen_review_count = 0

    with open(orders_path, "w", newline="", encoding="utf-8") as of, \
            open(details_path, "w", newline="", encoding="utf-8") as df:
        ow = csv.writer(of)
        dw = csv.writer(df)
        ow.writerow([
            "id", "customer_id", "status", "ordered_at", "total_amount",
            "created_at", "updated_at", "version", "is_deleted", "coupon_code", "discount_amount",
            "shipping_recipient_name", "shipping_postal_code", "shipping_prefecture",
            "shipping_city", "shipping_address_line1", "shipping_phone_number",
        ])
        dw.writerow([
            "id", "customer_order_id", "product_id", "quantity", "unit_price", "subtotal",
            "created_at", "updated_at", "version",
        ])

        for order_id in range(1, args.orders + 1):
            customer_id = rng.choices(customer_ids, cum_weights=customer_cum_weights, k=1)[0]
            ordered_at = weighted_growth_ts(rng, start, end)
            status = pick_order_status(rng, ordered_at, now)

            item_count = rng.choices([1, 2, 3, 4, 5], weights=[40, 30, 15, 10, 5], k=1)[0]
            picked_products = rng.choices(product_ids, cum_weights=product_cum_weights, k=item_count)
            total = 0
            for product_id in picked_products:
                qty = rng.choices([1, 2, 3], weights=[70, 20, 10], k=1)[0]
                unit_price = product_prices[product_id - 1]
                subtotal = unit_price * qty
                total += subtotal
                did = order_detail_id.next()
                dw.writerow([
                    did, order_id, product_id, qty, f"{unit_price}.00", f"{subtotal}.00",
                    fmt_ts(ordered_at), fmt_ts(ordered_at), 0,
                ])

                # リザーバーサンプリング: プール未満なら追加、以降は確率 capacity/seen で置換。
                seen_review_count += 1
                candidate = (customer_id, product_id, fmt_ts(ordered_at))
                if len(review_pool) < args.review_pool_size:
                    review_pool.append(candidate)
                else:
                    j = rng.randint(0, seen_review_count - 1)
                    if j < args.review_pool_size:
                        review_pool[j] = candidate

            prefecture, city = rng.choice(PREFECTURE_CITY)
            recipient = f"{rng.choice(LAST_NAMES)} {rng.choice(FIRST_NAMES)}"
            postal = f"{rng.randint(100,999)}-{rng.randint(1000,9999)}"
            phone = f"090-{rng.randint(1000,9999)}-{rng.randint(1000,9999)}"

            ow.writerow([
                order_id, customer_id, status, fmt_ts(ordered_at), f"{total}.00",
                fmt_ts(ordered_at), fmt_ts(ordered_at), 0, "f", "", "0.00",
                recipient, postal, prefecture, city, "1-2-3", phone,
            ])

            if order_id % 50_000 == 0:
                print(f"  order: {order_id}/{args.orders}")

    write_reviews(rng, args, review_pool, reviews_path, now)


def write_reviews(
    rng: random.Random,
    args: Args,
    review_pool: list[tuple[int, int, str]],
    out_path: str,
    now: datetime,
) -> None:
    rng.shuffle(review_pool)
    target = int(len(review_pool) * args.review_sample_rate)
    seen_pairs: set[tuple[int, int]] = set()
    comments = [
        "期待通りの品質でした。", "コストパフォーマンスが良いです。", "普通に使えています。",
        "梱包が丁寧でした。", "また購入したいと思います。", "思ったより良かったです。",
        "", "", "",  # コメント無しレビューも一定割合含める
    ]
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow([
            "id", "customer_id", "product_id", "rating", "comment",
            "created_at", "updated_at", "version",
        ])
        review_id = 0
        for customer_id, product_id, order_ts in review_pool:
            if len(seen_pairs) >= target:
                break
            key = (customer_id, product_id)
            if key in seen_pairs:
                continue
            seen_pairs.add(key)
            review_id += 1
            rating = rng.choices([5, 4, 3, 2, 1], weights=[40, 30, 15, 10, 5], k=1)[0]
            comment = rng.choice(comments)
            w.writerow([
                review_id, customer_id, product_id, rating, comment, order_ts, order_ts, 0,
            ])
        print(f"  review: {review_id} rows (pool={len(review_pool)}, target={target})")


def write_cart_items(rng: random.Random, args: Args, now: datetime, out_path: str) -> None:
    pairs: set[tuple[int, int]] = set()
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow([
            "id", "customer_id", "product_id", "quantity",
            "created_at", "updated_at", "version",
        ])
        cart_id = 0
        attempts = 0
        while cart_id < args.cart_items and attempts < args.cart_items * 20:
            attempts += 1
            customer_id = rng.randint(1, args.customers)
            product_id = rng.randint(1, args.products)
            key = (customer_id, product_id)
            if key in pairs:
                continue
            pairs.add(key)
            cart_id += 1
            qty = rng.randint(1, 3)
            created = now - timedelta(minutes=rng.randint(1, 60 * 24 * 14))
            w.writerow([cart_id, customer_id, product_id, qty, fmt_ts(created), fmt_ts(created), 0])


def write_summary(out_dir: str, args: Args, keyword_counts: dict[str, int]) -> None:
    top_common = sorted(keyword_counts.items(), key=lambda kv: -kv[1])[:3]
    rare = sorted(keyword_counts.items(), key=lambda kv: kv[1])[:3]
    lines = [
        "# 負荷試験データ生成サマリー",
        "",
        f"- products: {args.products}",
        f"- customers: {args.customers}",
        f"- orders: {args.orders}",
        f"- categories: {args.categories}",
        f"- cart_items(target): {args.cart_items}",
        "",
        "## 検索キーワードのヒット件数目安(name/description の部分一致検索用)",
        "",
        "| キーワード | 出現商品数 |",
        "|---|---|",
    ]
    for kw, cnt in top_common:
        lines.append(f"| {kw} | {cnt}(大量ヒット) |")
    for kw, cnt in rare:
        lines.append(f"| {kw} | {cnt}(少数ヒット) |")
    lines.append(f"| {ZERO_HIT_KEYWORD} | 0(意図的に未使用の語) |")
    lines.append("")
    lines.append("商品コード([EC-0000001] 等、商品名の末尾に必ず付与)で検索すると必ず1件だけヒットする。")
    with open(os.path.join(out_dir, "SUMMARY.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


def main() -> None:
    args = parse_args()
    os.makedirs(args.out_dir, exist_ok=True)
    rng = random.Random(args.seed)

    now = datetime.now(tz=JST)
    start = now - timedelta(days=365 * args.years)

    print(f"out_dir={args.out_dir}")

    categories = build_categories(rng, args.categories)
    write_categories(os.path.join(args.out_dir, "category.csv"), categories, now)
    print(f"categories: {len(categories)}")

    zipf_weights, keyword_counts = generate_products(
        rng, args, start, now, os.path.join(args.out_dir, "product.csv")
    )

    write_product_categories(
        rng, os.path.join(args.out_dir, "product_category.csv"), args.products, categories
    )

    generate_customers(rng, args, start, now, os.path.join(args.out_dir, "customer.csv"))

    # unit_price のスナップショット用に商品価格を読み直す(生成時と同じ乱数列を再現するのは
    # コストが高いため、CSV から読み戻して整合させる)。
    product_prices: list[int] = []
    with open(os.path.join(args.out_dir, "product.csv"), newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            product_prices.append(int(float(row["price"])))

    generate_orders_and_details_and_reviews(
        rng, args, zipf_weights, product_prices, start, now, now,
        os.path.join(args.out_dir, "customer_order.csv"),
        os.path.join(args.out_dir, "customer_order_detail.csv"),
        os.path.join(args.out_dir, "review.csv"),
    )

    write_cart_items(rng, args, now, os.path.join(args.out_dir, "cart_item.csv"))

    write_summary(args.out_dir, args, keyword_counts)

    print("done.")


if __name__ == "__main__":
    main()
