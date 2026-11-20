# In-Memory Banking Core

A pure Java 25, zero-framework in-memory banking ledger engine designed around deterministic `LocalDateTime` progression, immutable `Transaction` records, `BigDecimal` financial precision, and time-delayed cashback scheduling.

## Key Features

- **Account & Ledger Management:** Account creation (`UUID`), deposits, withdrawals, and atomic peer-to-peer transfers backed by a LIFO `Deque<Transaction>` ledger per account (newest transactions at the head).
- **Point-in-Time Balance & Statement Filtering:** Retrieves historical balances (`getBalanceAt`) and filters account statements (`findTransactions`) by `TransactionType` and optional `LocalDateTime` date windows.
- **Event-Driven Cashback Scheduling:** Supports payments with a configurable time-delayed 2% cashback (`RoundingMode.DOWN`) scheduled via a `PriorityQueue<Transaction>` min-heap.
- **Interactive CLI & Unit Tests:** Includes a terminal-based interactive runner (`BankingCoreApplication`) and a JUnit 5 test suite covering business rules and edge cases.

## Architecture & Data Structures

| Component | Structure | Purpose & Complexity |
| :--- | :--- | :--- |
| **Account Ledger** | `Map<UUID, Deque<Transaction>>` | $O(1)$ account lookup; LIFO stack (`push`/`peek`) gives $O(1)$ current balance access and reverse-chronological scans for historical snapshots and statement queries. |
| **Cashback Scheduler** | `PriorityQueue<Transaction>` | Min-heap ordered by `scheduledDate` ($O(\log N)$ insert/poll), settling delayed 2% cashbacks in exact chronological order without background threads. |
| **Domain Model** | `Transaction` (Java `record`) | Immutable ledger entry encapsulating `transactionId`, `accountId`, `amount`, running `balance`, timestamps (`transactionDate`, `scheduledDate`), `TransactionType`, and `TransactionStatus`. |

## Getting Started

### Prerequisites
- **Java 25 (LTS)**
- **Apache Maven 3.9+**

### Running Tests & CLI
```bash
git clone [https://github.com/joiaurino/in-memory-banking-core.git](https://github.com/joiaurino/in-memory-banking-core.git)
cd in-memory-banking-core

# Run the JUnit 5 test suite
mvn test

# Compile and run the interactive CLI
mvn compile exec:java -Dexec.mainClass="br.com.banking.core.BankingCoreApplication"