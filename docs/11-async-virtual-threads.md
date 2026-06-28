# 11. 非同期処理と仮想スレッド（Java 25 / Spring Boot 4.1）

> [← インデックスに戻る](../コーディング規約.md)

---

## Java 25 仮想スレッド（Virtual Threads）の活用と制限

### 原則

Spring Boot 4.1 において `spring.threads.virtual.enabled=true` を有効化し、I/Oバウンドな処理におけるスレッド生成コストを削減する。ただし、仮想スレッドがOSのキャリアスレッドに固定され、スループットが全損する「ピン留め（Pinned）」現象を回避するためのコーディング制約を徹底する。

### 必須要件

1. **`synchronized` の禁止（重い処理の周辺）:** 外部API呼び出し、データベースアクセス、ファイルI/O、または `Thread.sleep()` などのブロック処理を伴うメソッドや、その周囲を囲むブロックでの `synchronized` キーワードの使用を禁止する
2. **`ReentrantLock` への移行:** 排他制御（ロック）が必要な場合は、仮想スレッドのアンマウント（退避）を妨げない `java.util.concurrent.locks.ReentrantLock` を使用する

### 実装例：排他制御の安全な移行

```java
@Component
public class VirtualThreadSafeLocker {

    private final ReentrantLock lock = new ReentrantLock();

    // ❌ 仮想スレッド環境では synchronized メソッドによるブロック処理は「ピン留め」を引き起こすため禁止
    // public synchronized String badLockedOperation() { ... }

    // ✅ ReentrantLock を使用した仮想スレッドセーフなロック処理
    public <T> T executeWithLock(Supplier<T> action) {
        lock.lock();
        try {
            // この内部で重い I/O や外部 API 呼び出しが発生しても、
            // 仮想スレッドは正しくアンマウントされ、キャリアスレッドを解放できる
            return action.get();
        } finally {
            lock.unlock(); // 確実にアンロック
        }
    }
}
```
