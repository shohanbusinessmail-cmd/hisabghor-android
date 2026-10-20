# হিসাবঘর — হিসাবরক্ষণ নিয়ম (Accounting Rules)

This document is the authoritative definition of how every financial event is
recorded. The implementation (`PostingFactory`) and the unit tests both follow
it exactly. Amounts are integer **poisha** (১ টাকা = ১০০ পয়সা), stored as `Long`.
Floating point is never used for stored money.

## 1. Double-entry postings

Every financial event creates one `transactions` row and two or more
`postings` (journal lines). **The signed sum of a transaction's postings is
always exactly 0.** Sign convention:

- Positive posting = debit → increases **assets** and **expenses**.
- Negative posting = credit → increases **liabilities** and **income**.

Ledger types: `ACCOUNT` (owned money accounts), `INCOME_CATEGORY`,
`EXPENSE_CATEGORY`, `SHOP_LIABILITY`, `LOAN_LIABILITY`, `RECEIVABLE`, `EQUITY`.

### Derived balances (never stored, always computed)
| Value | Formula |
| --- | --- |
| Account balance | `SUM(postings of ACCOUNT)` |
| Income (period) | `−SUM(INCOME_CATEGORY postings in period)` |
| Expense (period) | `SUM(EXPENSE_CATEGORY postings in period)` |
| Shop due | `−SUM(SHOP_LIABILITY postings of shop)` |
| Loan principal outstanding | `−SUM(LOAN_LIABILITY postings of loan)` |
| Receivable outstanding | `SUM(RECEIVABLE postings of agreement)` |
| Net worth | accounts + receivables + manual assets − loans − shop dues − manual liabilities |

Voided transactions are always excluded.

## 2. Event-by-event treatment

| Event | Postings | Income/Expense effect |
| --- | --- | --- |
| Opening balance (+৳X in cash) | Dr ACCOUNT X, Cr EQUITY X | none |
| Income ৳X into account | Dr ACCOUNT X, Cr INCOME_CATEGORY X | income +X |
| Expense ৳X from account | Dr EXPENSE_CATEGORY X, Cr ACCOUNT X | expense +X |
| Expense refund ৳X | Dr ACCOUNT X, Cr EXPENSE_CATEGORY X | expense −X (not income) |
| Transfer ৳X A→B (+fee ৳F) | Dr B X, Dr EXPENSE_CATEGORY(fee) F, Cr A (X+F) | only the fee is an expense |
| Shop cash purchase | same as expense, `shopId` set | expense +X |
| Shop credit purchase ৳T, paid now ৳P | Dr EXPENSE_CATEGORY T, Cr ACCOUNT P, Cr SHOP_LIABILITY (T−P) | expense +T once |
| Shop payment ৳P | Dr SHOP_LIABILITY P, Cr ACCOUNT P | none (expense already counted) |
| Borrow / loan disbursement ৳D | Dr ACCOUNT D, Cr LOAN_LIABILITY D | **not income** |
| Loan repayment: principal ৳Pr, interest ৳I, fee ৳F | Dr LOAN_LIABILITY Pr, Dr EXPENSE_CATEGORY(interest) I, Dr EXPENSE_CATEGORY(fee) F, Cr ACCOUNT (Pr+I+F) | interest & fee are expenses; principal is not |
| Lend to person ৳X | Dr RECEIVABLE X, Cr ACCOUNT X | **not an expense** |
| Lending returned: principal ৳Pr, interest ৳I | Dr ACCOUNT (Pr+I), Cr RECEIVABLE Pr, Cr INCOME_CATEGORY I | only interest is income |
| Savings deposit ৳X cash→savings account | Dr ACCOUNT(savings) X, Cr ACCOUNT(cash) X | none (internal transfer) |
| Savings profit received ৳X | Dr ACCOUNT(savings) X, Cr INCOME_CATEGORY X | income +X |
| Pre-existing shop due ৳X (opening) | Dr EQUITY X, Cr SHOP_LIABILITY X | none |
| Pre-existing loan ৳X (opening) | Dr EQUITY X, Cr LOAN_LIABILITY X | none |
| Pre-existing receivable ৳X (opening) | Dr RECEIVABLE X, Cr EQUITY X | none |
| Balance adjustment ±৳X | Dr/Cr ACCOUNT vs EQUITY | none (labelled adjustment) |

## 3. Loans & installments

- The loan liability ledger always tracks **principal outstanding only**.
- Interest is recognised **when paid** (or as part of a scheduled installment
  split) — never capitalised silently.
- NGO loans: flat allocation. Total interest = total contractual − principal,
  spread evenly across installments; the last installment absorbs rounding
  remainder. "Remaining contractual amount" = Σ unpaid installment amounts.
- Bank amortizing loans: EMI = P·r·(1+r)ⁿ / ((1+r)ⁿ − 1), r = annual rate ÷
  periods-per-year. Each period interest = round(outstanding × r) (HALF_UP to
  poisha); principal = EMI − interest; final installment adjusts. Zero-rate:
  EMI = P ÷ n with last-installment remainder.
- A repayment is allocated to installments oldest-first, interest before
  principal within each installment. Overpayment beyond the schedule reduces
  principal directly and shrinks later installments from the end.
- Calculated schedules are **estimates**. The app labels them as such and never
  claims to show a bank's official balance. Users can edit installment rows to
  match a lender statement without changing recorded payments.

## 4. Itemized purchases

Quantity is stored as thousandths (`quantityMilli`, so ১.৫ কেজি = 1500).
Line total = `round(quantityMilli × unitPricePoisha ÷ 1000) − discount`.
Transaction amount = Σ line totals + extra charges − overall discount.
The expense posting always equals the transaction amount, so items, payments
and outstanding balance reconcile exactly.

## 5. Editing, voiding, rounding

- Edit = void original + insert corrected transaction (linked), atomically,
  with an audit event. Balances therefore change exactly once.
- All multiplication/division on money uses checked `Long` math or
  `BigDecimal` with explicit `HALF_UP` rounding to poisha.
- Interest rates are stored as integer basis points (১২.৫০% = 1250 bp).

## 6. Worked acceptance examples (all covered by automated tests)

1. Credit purchase ৳1,000, pay ৳300 now → shop due **৳700**, expense ৳1,000.
2. Pay shop another ৳200 → shop due **৳500**, no new expense.
3. Transfer ৳500 cash→bKash → income/expense unchanged, total balance same.
4. Move ৳1,000 cash→savings → no expense, total assets unchanged.
5. Borrow ৳10,000 → cash +৳10,000, liability ৳10,000, income unchanged.
6. Lend ৳2,000 → receivable ৳2,000, expense unchanged.
7. Repay ৳1,000 principal + ৳100 interest → liability −৳1,000, expense +৳100.
8. Editing any of the above updates every derived figure exactly once.
