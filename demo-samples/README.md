# SonarQube vs GitHub Copilot 比較デモ

`OrderService.java` には、意図的に3種類・計7個の問題を仕込んであります。
SonarQubeとGitHub Copilot（PRレビュー機能）の両方にこのコードをかけて、
検出結果を比較するためのデモ用ファイルです。

## 使い方

1. `feature/demo-comparison` のようなブランチを切り、このファイルを配置してPRを作成
2. SonarCloud（nao-dev-999）で解析させ、Issue一覧を確認
3. 同じPRにGitHub Copilotのコードレビューを走らせ、コメントを確認
4. 検出できた項目・できなかった項目を突き合わせる

## 仕込んである問題一覧（ネタバレ）

| # | 種類 | 場所 | SonarQubeでの想定検出 | Copilotでの想定検出 |
|---|---|---|---|---|
| A-1 | 未使用フィールド | `unusedDebugFlag` | ◎ Code Smellとして検出 | △ 検出することもあるが不安定 |
| B | SQLインジェクション | `searchOrdersByCustomerName` | ◎ Vulnerabilityとして確実に検出 | ○ 検出しやすいが指摘の深さはSonarQubeに劣る傾向 |
| A-2 | リソースリーク | `countPendingOrders` | ◎ Bugとして検出（try-with-resources推奨） | △ 検出することもある |
| A-3 | 循環的複雑度過多 | `classifyOrder` | ◎ Code Smell（Cognitive Complexity超過） | × ルールとしては指摘しにくい（が「読みにくい」旨のコメントはつく可能性） |
| C-1 | 計算順序の業務ロジック誤り | `calculateFinalPrice` | × 構文的に正しいため検出困難 | ◎ 「割引と税金の順序が意図と逆では」と指摘しやすい |
| C-2 | 命名と実装の不一致 | `isOrderCancellable` | × 検出困難 | ◎ メソッド名と戻り値の矛盾を指摘しやすい |
| C-3 | ファイル横断の仕様不整合 | `applyItemDiscount` / `recalculateOrderTotal` | × 検出困難（呼び出し元との意味的整合性は見ない） | ○ コンテキストが十分ならば指摘できる可能性（PRの差分範囲に依存） |

## 発表での使い方の例

- 「◎」が多い行＝ルールベース（SonarQube）の得意領域
- 「◎」がCopilot側に偏る行＝意図理解が必要な領域
- 実際の検出結果（スクリーンショット）をスライドに貼ると説得力が増します
- Copilotの検出結果はPRの差分範囲・プロンプトの文脈量に左右されるため、
  「毎回必ずこの通りになるとは限らない」旨を発表内で一言添えると誠実です

## 注意

- このコードは **デモ・学習目的専用** です。本番コードにマージしないでください
- SQLインジェクションのサンプル（`searchOrdersByCustomerName`）は
  実際にDBに接続するとリスクがあるため、解析のみに使用し実行はしないでください
