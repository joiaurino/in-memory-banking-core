# Changelog

All notable changes to this project will be documented in this file.

## [3.0.0](https://github.com/joiaurino/in-memory-banking-core/compare/v3.0.1...v3.0.0) (2026-11-26)


### ⚠ BREAKING CHANGES

* Refactoring and adding tests
* auth client
* Removing comments
* Add tests and fix bug
* Strategy + Template Method + Command

### Features

* Add tests and fix bug ([a57267f](https://github.com/joiaurino/in-memory-banking-core/commit/a57267fcb865c62951848307c761aadc3e9b1646))
* auth client ([832d7c4](https://github.com/joiaurino/in-memory-banking-core/commit/832d7c41c9060ac7876103f58121e369252cc608))
* create version ([6d1d5e9](https://github.com/joiaurino/in-memory-banking-core/commit/6d1d5e9a3c6e3f93da7435652a1ed9ff8c97bdae))
* create version ([9e1c460](https://github.com/joiaurino/in-memory-banking-core/commit/9e1c460504fff983559c44768d3328b032ea4485))
* **develop:** fixing readme ([3b8b0b9](https://github.com/joiaurino/in-memory-banking-core/commit/3b8b0b904139386a25d6f50bc3457a3a9a4e1b4e))
* **develop:** MVP - fase 1 - menu ([0f6126e](https://github.com/joiaurino/in-memory-banking-core/commit/0f6126ed3379308c0bdbd4fd13132aa9ac82f0b1))
* **develop:** MVP - level 1 - building ([5473e07](https://github.com/joiaurino/in-memory-banking-core/commit/5473e07cd87fa80487e7169c98763f60b487a529))
* **develop:** update readme ([58a2814](https://github.com/joiaurino/in-memory-banking-core/commit/58a28146a8eea02fcc29a4a0828af3bb14c63447))
* **develop:** update README ([dc84608](https://github.com/joiaurino/in-memory-banking-core/commit/dc84608ef41b8f7a1608f465ac734fb6e07a5940))
* Refactoring and adding tests ([88a4d3e](https://github.com/joiaurino/in-memory-banking-core/commit/88a4d3eee04f39a83d32b924fc10cdf62bbd1c6a))
* Removing comments ([9270565](https://github.com/joiaurino/in-memory-banking-core/commit/9270565e08752cc9484bd285330e2199b7372738))
* Strategy + Template Method + Command ([73d3859](https://github.com/joiaurino/in-memory-banking-core/commit/73d38598b63b1dec2903a42efdcb133d7fb3dd44))


### Bug Fixes

* adding validation and fixing names ([1cd8a33](https://github.com/joiaurino/in-memory-banking-core/commit/1cd8a3311b4b349cb0340c14bf424e95fed22052))
* **develop:** MVP - level 1 - building ([c3ddb39](https://github.com/joiaurino/in-memory-banking-core/commit/c3ddb39e6e148f5159eade3dddc856c340dfecd3))
* GithubAction ([eec144e](https://github.com/joiaurino/in-memory-banking-core/commit/eec144e336a0f746b3a8f9f4b3bd07d257a76df0))

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
