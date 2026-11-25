create table customers (
    id      uuid primary key,
    name    varchar(200) not null,
    email   varchar(254) not null unique,
    city    varchar(100),
    active  boolean      not null
);

create table products (
    id       uuid primary key,
    sku      varchar(32)    not null unique,
    name     varchar(200)   not null,
    price    numeric(19, 2) not null,
    currency varchar(3)     not null,
    active   boolean        not null
);

create table orders (
    id          uuid primary key,
    customer_id uuid        not null references customers (id),
    status      varchar(20) not null,
    created_at  timestamp(6) with time zone not null,
    placed_at   timestamp(6) with time zone
);

create index idx_orders_customer on orders (customer_id);

create table order_lines (
    order_id     uuid           not null references orders (id) on delete cascade,
    position     integer        not null,
    product_id   uuid           not null,
    product_name varchar(200)   not null,
    unit_price   numeric(19, 2) not null,
    currency     varchar(3)     not null,
    quantity     integer        not null,
    primary key (order_id, position)
);
