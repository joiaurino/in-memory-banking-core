# In-Memory Banking Core

A pure Java 25, zero-framework in-memory banking ledger engine designed around deterministic `LocalDateTime` progression, immutable `Transaction` records, `BigDecimal` financial precision, and clean architectural patterns (`Command`, `Strategy`, and `Template Method`).

## Key Features

- **Account & Ledger Management:** Account creation (`UUID`), deposits, withdrawals, and atomic peer-to-peer transfers backed by an append-only LIFO `Deque<Transaction>` ledger per account.
- **Polymorphic Command Dispatch:** Extensible transaction pipeline built with Java `sealed` interfaces/records (`TransactionCommand`), `ITransactionStrategy`, and `AbstractTransactionStrategy`.
- **Point-in-Time Balance & Statement Filtering:** Retrieves historical balances (`getBalanceAt`) and filters account statements (`findTransactions`) by `TransactionType` and optional `LocalDateTime` windows.
- **Event-Driven Cashback Scheduling:** Supports payments with a configurable time-delayed 2% cashback (`RoundingMode.DOWN`) scheduled via a `PriorityQueue<Transaction>` min-heap.
- **Decoupled Interactive CLI & Unit Tests:** Features a terminal-based interactive runner (`BankingCliRunner`, `ConsoleIO`, `MessageResolver`) with externalized `ResourceBundle` messages and a comprehensive JUnit 5 test suite.

## Architecture & Design Patterns

| Layer / Component | Pattern / Structure | Purpose & Complexity |
| :--- | :--- | :--- |
| **Account Ledger** | `Map<UUID, Deque<Transaction>>` | $O(1)$ account lookup; LIFO stack (`push`/`peek`) gives $O(1)$ current balance access and reverse-chronological scans for historical snapshots. |
| **Cashback Scheduler** | `PriorityQueue<Transaction>` | Min-heap ordered by `scheduledDate` ($O(\log N)$ insert/poll), settling delayed 2% cashbacks in exact chronological order without background threads. |
| **Write Operations** | `Command` + `Strategy` + `Template Method` | `sealed interface TransactionCommand` dispatches to `ITransactionStrategy` implementations; `AbstractTransactionStrategy` centralizes balance validation and ledger mutations. |
| **Domain Model** | `Transaction` (Java `record`) | Immutable ledger entry encapsulating `transactionId`, `accountId`, `amount`, running `balance`, timestamps, `TransactionType`, and `TransactionStatus`. |
| **CLI Presentation** | `BankingCliRunner` + `ConsoleIO` | Isolates terminal I/O and localized formatting (`MessageResolver`) from core domain logic. |

## Project Structure

```text
src/main/java/br/com/banking/core/
├── BankingCoreApplication.java          # Entry point
├── cli/                                 # Interactive terminal runner, I/O & message bundle
├── domain/                              # Immutable Transaction record & sealed TransactionCommand
├── enums/                               # TransactionType & TransactionStatus
├── service/                             # IBankingCore & BankingCoreImpl (command dispatcher & queries)
└── strategy/                            # AbstractTransactionStrategy & concrete transaction rules
```

## Getting Started

### Prerequisites
- **Java 25 (LTS)**
- **Apache Maven 3.9+**

### Clone the Repository
```bash
git clone https://github.com/joiaurino/in-memory-banking-core.git
cd in-memory-banking-core
```

### Run the JUnit 5 Test Suite
```bash
mvn test
```

### Package Executable JAR
```bash
mvn package
```

### Run the Interactive CLI
```bash
mvn compile exec:java -Dexec.mainClass="br.com.banking.core.BankingCoreApplication"
```