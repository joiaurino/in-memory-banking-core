# Changelog

All notable changes to this project will be documented in this file.

## [3.0.0] - 2026-11-26

### Added
- **Authentication & Session Management (`IAuthClient` / `AuthClientImpl`):**
    - Client registration and login using normalized 11-digit CPF + password (1:1 relationship with `accountId`).
    - Salted `SHA-256` password hashing stored in self-contained `salt:hash` credential strings.
    - Constant-time hash comparison via `MessageDigest.isEqual` to mitigate timing attacks.
    - 256-bit `SecureRandom` session tokens (`AuthSession`) with 10-minute TTL and lazy eviction on validation.
- **Authenticated CLI Workflow (`BankingCliRunner`):**
    - Dynamic menu switching between unauthenticated (`Register`, `Login`) and authenticated banking operations.
    - Automatic session validation and `accountId` resolution without requiring manual UUID entry on personal transactions.
- **Secondary Index for Cashback Lookups:**
    - Added `Map<UUID, Transaction> cashbackIndex` in `BankingCoreImpl` to achieve $O(1)$ status queries (`PENDING` / `CONCLUDED`).
- **Test Coverage:**
    - Added `AuthClientImplTest` covering registration, duplicate CPF rejection, password policy, session expiration/logout, and account mismatch protection.
    - Updated `BankingCliRunnerTest` for end-to-end authenticated CLI flows.

### Changed
- **Atomic Transfers (`TransferStrategy`):** Added explicit LIFO rollback (`pop()`) on the source account ledger if the target credit step fails.
- **Chronological Query Optimization (`BankingCoreImpl`):** Applied `takeWhile` on `findTransactions` to short-circuit scans on LIFO `Deque` history when reaching entries older than `from`.
- **Pure Statement Queries:** Decoupled `getStatement` from implicit clock-based mutations.

## [2.0.0] - 2026-11-18

### Added
- Interactive CLI layer (`BankingCliRunner`, `ConsoleIO`, `MessageResolver`) with externalized `messages.properties`.
- Scheduled 2% cashback payments (`CashbackPaymentCommand` / `PayWithCashBackStrategy`) backed by a `PriorityQueue` Min-Heap.
- Point-in-time historical balance queries (`getBalanceAt`) and transaction filtering (`findTransactions`).

## [1.0.0] - 2026-11-10

### Added
- Initial pure Java in-memory banking ledger (`BankingCoreImpl`) using `Map<UUID, Deque<Transaction>>`.
- Core transaction commands and strategies (`CreateAccount`, `Deposit`, `Withdraw`, `Transfer`).