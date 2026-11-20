# In-Memory Banking Core

A pure Java 25, zero-framework in-memory banking ledger engine designed around deterministic timestamp progression, immutable transaction history, and time-delayed cashback processing.

## Key Features

- **Account & Ledger Management:** Account creation, deposits, debits, and atomic peer-to-peer transfers backed by an append-only `Deque<Transaction>` history per account.
- **Top Spenders Ranking:** Ranks active accounts by outgoing transaction volume with deterministic lexicographical tie-breaking.
- **Event-Driven Cashback Scheduling:** Supports scheduled payments with a 24-hour (`86,400,000 ms`) delayed 2% cashback processed lazily via a `PriorityQueue` min-heap on every state transition.
- **Account Merging & Historical Queries:** Merges accounts while preserving historical balance lookups (`getBalance` at any past `timeAt` timestamp) and migrating pending/completed payment ownership.

## Architecture & Data Structures

| Component | Structure | Purpose & Complexity |
| :--- | :--- | :--- |
| **Account Ledger** | `Map<String, Deque<Transaction>>` | $O(1)$ account lookup; LIFO stack gives $O(1)$ current balance access (`peek()`) and chronological reverse scan for historical snapshots. |
| **Cashback Scheduler** | `PriorityQueue<Payment>` | Min-heap ordered by `scheduledTime` ($O(\log N)$ insert/poll), ensuring delayed cashbacks settle in exact chronological order without background threads. |
| **Payment Registry** | `Map<String, Payment>` | $O(1)$ payment status lookup (`IN_PROGRESS` vs. `CASHBACK_RECEIVED`) and ownership updates during account merges. |
| **Closed Accounts** | `Map<String, Deque<Transaction>>` | Preserves pre-merge audit trails so historical `getBalance` queries remain accurate after an account is merged. |

## Getting Started

### Prerequisites
- **Java 25 (LTS)**
- Maven or Gradle (optional, for running JUnit 5 test suites)

### Running Locally
```bash
git clone [https://github.com/joiaurino/in-memory-banking-core.git](https://github.com/joiaurino/in-memory-banking-core.git)
cd in-memory-banking-core
javac -d out src/**/*.java
