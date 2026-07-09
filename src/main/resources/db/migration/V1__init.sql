create table obligations (
    id                uuid primary key,
    title             varchar(255)  not null,
    amount            numeric(19, 2) not null check (amount > 0),
    currency          varchar(3)    not null,
    category          varchar(20)   not null check (category in ('SUBSCRIPTION', 'WARRANTY', 'BILL', 'INSURANCE')),
    recurrence        varchar(20)   check (recurrence in ('MONTHLY', 'QUARTERLY', 'YEARLY')),
    next_payment_date date          not null,
    status            varchar(20)   not null check (status in ('ACTIVE', 'CANCELLED', 'EXPIRED')),
    created_at        timestamptz   not null,
    updated_at        timestamptz   not null
);

-- Сортировка списка и окно /upcoming.
create index idx_obligations_next_payment_date on obligations (next_payment_date);

-- Частичный индекс ровно под ленивое истечение: bulk-UPDATE сканирует только
-- активные разовые обязательства и не деградирует в seq scan.
create index idx_obligations_expiry_candidates on obligations (next_payment_date)
    where status = 'ACTIVE' and recurrence is null;

create table payments (
    id            uuid primary key,
    obligation_id uuid          not null references obligations (id) on delete cascade,
    amount        numeric(19, 2) not null,
    currency      varchar(3)    not null,
    paid_at       timestamptz   not null
);

create index idx_payments_obligation_id on payments (obligation_id);
