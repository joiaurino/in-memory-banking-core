# In-Memory Banking Core (Java 25)

A high-performance, zero-framework in-memory banking ledger and authentication engine built in pure **Java 25**. Designed as a practical showcase of **data structure selection**, **algorithmic complexity optimization ($O(1)$ / $O(\log N)$)**, and **clean, minimal design patterns**.

---

## Architecture Overview

```text
+-------------------------------------------------------------------+
|                        CLI Presentation Layer                     |
|         BankingCliRunner  |  ConsoleIO  |  MessageResolver        |
+---------------------------------+---------------------------------+
                                  |
                 +----------------+----------------+
                 v                                 v
+---------------------------------+ +-------------------------------+
|      Authentication Layer       | |     Core Financial Engine     |
|         AuthClientImpl          | |        BankingCoreImpl        |
|  - CPF + Password (SHA-256)     | |  - Command Dispatcher         |
|  - Session Tokens (TTL 10m)     | |  - Lazy Cashback Settlement   |
+----------------+----------------+ +---------------+---------------+
                 |                                  |
                 v                                  v
+---------------------------------+ +-------------------------------+
|       In-Memory Auth Store      | |     Transaction Strategies    |
|  - Map<String, ClientData>      | |  - CreateAccountStrategy      |
|  - Map<String, AuthSession>     | |  - Deposit / Withdraw         |
+---------------------------------+ |  - TransferStrategy (Atomic)  |
                                    |  - PayWithCashBackStrategy    |
                                    +---------------+---------------+
                                                    |
                                                    v
                                    +-------------------------------+
                                    |    In-Memory Ledger Store     |
                                    |  - Map<UUID, Deque<Tx>>       |
                                    |  - PriorityQueue<Tx> (Heap)   |
                                    |  - Map<UUID, Tx> (Cashback)   |
                                    +-------------------------------+
```

---

## Data Structures & Time Complexity

Every data structure in the core engine was chosen to match its exact access and mutation pattern:

| Component | Data Structure | Operation | Complexity | Rationale |
| :--- | :--- | :--- | :---: | :--- |
| **Account Registry** | `HashMap<UUID, Deque<Transaction>>` | Account lookup | $O(1)$ | Direct constant-time access to any account's ledger. |
| **Account Ledger** | `ArrayDeque<Transaction>` (LIFO) | Append tx / Current balance | $O(1)$ | `push()` appends newest entry at head; `peek()` reads latest balance without scanning. |
| **Point-in-Time Balance** | `ArrayDeque<Transaction>` | `getBalanceAt(timestamp)` | $O(K)$ | Scans newest-to-oldest and short-circuits on the first entry $\le$ target timestamp. |
| **Bounded History** | `ArrayDeque<Transaction>` + `takeWhile` | `findTransactions(...)` | $O(K)$ | Exploits chronological LIFO order to stop scanning once entries precede `from`. |
| **Cashback Scheduler** | `PriorityQueue<Transaction>` (Min-Heap) | Schedule / Settle due items | $O(\log N)$ | Orders pending cashbacks by `scheduledDate`; settles expired entries via `peek()`/`poll()`. |
| **Cashback Status Index** | `HashMap<UUID, Transaction>` | `getCashbackStatus(...)` | $O(1)$ | Secondary index avoiding $O(N)$ linear scans across the heap or ledger. |
| **Client & Session Store** | `HashMap<String, ClientData>` / `AuthSession` | Login & Token validation | $O(1)$ | Constant-time lookup by normalized CPF on login and by session token on operations. |

---

## Design Patterns & Engineering Highlights

* **Command Pattern (`sealed interface TransactionCommand`):** Immutable Java `record` commands (`CreateAccountCommand`, `DepositCommand`, `WithdrawCommand`, `TransferCommand`, `CashbackPaymentCommand`) encapsulate intent and timestamps.
* **Strategy + Template Method (`ITransactionStrategy` / `AbstractTransactionStrategy`):** Eliminates conditional branching in the dispatcher while centralizing balance mutation invariants (`incrementBalance`, `decrementBalance`, non-negative balance enforcement).
* **Atomic Transfers:** `TransferStrategy` performs explicit LIFO rollback (`Deque.pop()`) on the source ledger if crediting the target account fails.
* **Security & Cryptography:**
    * Passwords are salted with 16-byte `SecureRandom` values, hashed with `SHA-256`, and stored in self-contained `salt:hash` format.
    * Credential verification uses `MessageDigest.isEqual(...)` (constant-time comparison) to prevent timing attacks.
    * Session tokens are 256-bit URL-safe Base64 strings with automatic expiration and lazy eviction.
* **Internationalized CLI (`ResourceBundle`):** Decoupled presentation layer supporting parameterized messages (`MessageFormat`) and authenticated session state.

---

## Project Structure

```text
src/
├── main/
│   ├── java/br/com/banking/core/
│   │   ├── BankingCoreApplication.java      # Entry point
│   │   ├── cli/                             # Interactive terminal runner, I/O, i18n resolver
│   │   ├── domain/                          # Immutable records & sealed commands
│   │   ├── enums/                           # TransactionType & TransactionStatus
│   │   ├── service/                         # BankingCoreImpl & AuthClientImpl
│   │   └── strategy/                        # Transaction execution strategies
│   └── resources/
│       └── messages.properties              # Externalized CLI prompts and messages
└── test/
    └── java/br/com/banking/core/            # JUnit 5 unit & end-to-end CLI test suites
```

---

## Running & Testing

### Prerequisites
* **Java 25** (with preview features enabled if applicable)
* **Maven 3.9+**

### Run Test Suite
```bash
mvn test
```

### Run Interactive CLI
```bash
mvn compile exec:java -Dexec.mainClass="br.com.banking.core.BankingCoreApplication"
```