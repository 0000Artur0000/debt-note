alter table obligations
    add column version bigint,
    add column billing_anchor_day smallint;

update obligations
set version = 0,
    billing_anchor_day = case
        when recurrence is null then null
        else extract(day from next_payment_date)::smallint
    end;

update obligations
set title = btrim(title),
    currency = upper(btrim(currency));

update payments
set currency = upper(btrim(currency));

alter table obligations
    alter column version set default 0,
    alter column version set not null,
    add constraint chk_obligations_version_non_negative check (version >= 0),
    add constraint chk_obligations_billing_anchor check (
        (recurrence is null and billing_anchor_day is null)
        or (recurrence is not null and billing_anchor_day between 1 and 31)
    ),
    add constraint chk_obligations_title_normalized check (
        title = btrim(title) and char_length(title) between 1 and 255
    ),
    add constraint chk_obligations_currency_normalized check (
        currency = upper(currency) and currency ~ '^[A-Z]{3}$'
    );

alter table payments
    add constraint chk_payments_amount_positive check (amount > 0),
    add constraint chk_payments_currency_normalized check (
        currency = upper(currency) and currency ~ '^[A-Z]{3}$'
    );

create index idx_obligations_active_payment_date
    on obligations (next_payment_date, id)
    where status = 'ACTIVE';

create index idx_obligations_active_title_lower
    on obligations (lower(title))
    where status = 'ACTIVE';
