-- ============================================================
-- Divya Gems E-Commerce — Full Schema Initialization
-- V1: All core tables
-- ============================================================

-- ──────────────────────────────────────────
-- ROLES
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(30) NOT NULL UNIQUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

-- ──────────────────────────────────────────
-- USERS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(200) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    phone               VARCHAR(15),
    is_email_verified   BOOLEAN     NOT NULL DEFAULT false,
    is_active           BOOLEAN     NOT NULL DEFAULT true,
    email_verify_token  VARCHAR(255),
    reset_password_token VARCHAR(255),
    reset_token_expiry  TIMESTAMP,
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_user_email      ON users (email);
CREATE INDEX IF NOT EXISTS idx_user_is_active  ON users (is_active);

-- ──────────────────────────────────────────
-- USER_ROLES (join table)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ──────────────────────────────────────────
-- REFRESH TOKENS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP   NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

-- ──────────────────────────────────────────
-- ADDRESSES
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS addresses (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id         UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    address_type    VARCHAR(20) NOT NULL DEFAULT 'HOME',
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(15) NOT NULL,
    address_line1   VARCHAR(255) NOT NULL,
    address_line2   VARCHAR(255),
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    pin_code        VARCHAR(10)  NOT NULL,
    country         VARCHAR(100) NOT NULL DEFAULT 'India',
    is_default      BOOLEAN     NOT NULL DEFAULT false,
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150)
);

-- ──────────────────────────────────────────
-- CATEGORIES
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    slug            VARCHAR(200) NOT NULL UNIQUE,
    description     TEXT,
    image_url       VARCHAR(500),
    parent_id       UUID        REFERENCES categories (id) ON DELETE SET NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT true,
    display_order   INT         NOT NULL DEFAULT 0,
    meta_title      VARCHAR(200),
    meta_description VARCHAR(500),
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_category_slug      ON categories (slug);
CREATE INDEX IF NOT EXISTS idx_category_is_active ON categories (is_active);
CREATE INDEX IF NOT EXISTS idx_category_parent_id ON categories (parent_id);

-- ──────────────────────────────────────────
-- TAGS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tags (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(150) NOT NULL UNIQUE,
    tag_type    VARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

-- ──────────────────────────────────────────
-- PRODUCTS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name                VARCHAR(300) NOT NULL,
    slug                VARCHAR(350) NOT NULL UNIQUE,
    sku                 VARCHAR(100) NOT NULL UNIQUE,
    short_description   VARCHAR(500),
    description         TEXT,
    benefits            TEXT,
    usage_instructions  TEXT,
    price               NUMERIC(12,2) NOT NULL,
    sale_price          NUMERIC(12,2),
    stock_quantity      INT         NOT NULL DEFAULT 0,
    low_stock_threshold INT         NOT NULL DEFAULT 5,
    weight              DOUBLE PRECISION,
    weight_unit         VARCHAR(20)  DEFAULT 'grams',
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    is_featured         BOOLEAN     NOT NULL DEFAULT false,
    is_new_arrival      BOOLEAN     NOT NULL DEFAULT false,
    average_rating      DOUBLE PRECISION DEFAULT 0.0,
    total_reviews       INT         NOT NULL DEFAULT 0,
    meta_title          VARCHAR(200),
    meta_description    VARCHAR(500),
    meta_keywords       VARCHAR(300),
    category_id         UUID        NOT NULL REFERENCES categories (id),
    sub_category_id     UUID        REFERENCES categories (id),
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_product_slug        ON products (slug);
CREATE INDEX IF NOT EXISTS idx_product_sku         ON products (sku);
CREATE INDEX IF NOT EXISTS idx_product_status      ON products (status);
CREATE INDEX IF NOT EXISTS idx_product_is_featured ON products (is_featured);
CREATE INDEX IF NOT EXISTS idx_product_category_id ON products (category_id);

-- ──────────────────────────────────────────
-- PRODUCT_TAGS (join table)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_tags (
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    tag_id     UUID NOT NULL REFERENCES tags    (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, tag_id)
);

-- ──────────────────────────────────────────
-- PRODUCT_IMAGES
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_images (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id  UUID        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    image_url   VARCHAR(500) NOT NULL,
    alt_text    VARCHAR(300),
    is_primary  BOOLEAN     NOT NULL DEFAULT false,
    display_order INT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

-- ──────────────────────────────────────────
-- PRODUCT_VARIANTS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_variants (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id      UUID        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    sku             VARCHAR(100) NOT NULL UNIQUE,
    color_name      VARCHAR(100),
    color_hex_code  VARCHAR(10),
    price           NUMERIC(12,2),
    sale_price      NUMERIC(12,2),
    stock_quantity  INT         NOT NULL DEFAULT 0,
    image_url       VARCHAR(500),
    is_active       BOOLEAN     NOT NULL DEFAULT true,
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_variant_sku ON product_variants (sku);

-- ──────────────────────────────────────────
-- PRODUCT_VARIANT_IMAGES
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_variant_images (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    variant_id  UUID        NOT NULL REFERENCES product_variants (id) ON DELETE CASCADE,
    image_url   VARCHAR(500) NOT NULL,
    alt_text    VARCHAR(300),
    display_order INT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

-- ──────────────────────────────────────────
-- REVIEWS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id  UUID        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users   (id) ON DELETE CASCADE,
    rating      INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title       VARCHAR(200),
    body        TEXT,
    is_approved BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

-- ──────────────────────────────────────────
-- CARTS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS carts (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        REFERENCES users (id) ON DELETE CASCADE,
    session_id  VARCHAR(255),
    is_active   BOOLEAN     NOT NULL DEFAULT true,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_cart_session_id ON carts (session_id);
CREATE INDEX IF NOT EXISTS idx_cart_user_id    ON carts (user_id);

-- ──────────────────────────────────────────
-- CART_ITEMS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cart_items (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    cart_id         UUID        NOT NULL REFERENCES carts           (id) ON DELETE CASCADE,
    product_id      UUID        NOT NULL REFERENCES products        (id) ON DELETE CASCADE,
    variant_id      UUID        REFERENCES product_variants (id) ON DELETE SET NULL,
    quantity        INT         NOT NULL,
    price_at_add_time NUMERIC(12,2) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150),
    CONSTRAINT uq_cart_product_variant UNIQUE (cart_id, product_id, variant_id)
);

-- ──────────────────────────────────────────
-- COUPONS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupons (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code                    VARCHAR(50) NOT NULL UNIQUE,
    description             VARCHAR(500),
    discount_type           VARCHAR(20) NOT NULL,
    discount_value          NUMERIC(12,2) NOT NULL,
    minimum_order_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    maximum_discount_amount NUMERIC(12,2),
    usage_limit             INT         NOT NULL DEFAULT 0,
    used_count              INT         NOT NULL DEFAULT 0,
    valid_from              TIMESTAMP   NOT NULL,
    valid_until             TIMESTAMP   NOT NULL,
    is_active               BOOLEAN     NOT NULL DEFAULT true,
    is_one_time_per_user    BOOLEAN     NOT NULL DEFAULT false,
    created_at              TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP   NOT NULL DEFAULT now(),
    created_by              VARCHAR(150),
    updated_by              VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_coupon_code      ON coupons (code);
CREATE INDEX IF NOT EXISTS idx_coupon_is_active ON coupons (is_active);

-- ──────────────────────────────────────────
-- ORDERS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id                      UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id                VARCHAR(30)   NOT NULL UNIQUE,
    user_id                 UUID          NOT NULL REFERENCES users (id),
    order_status            VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    payment_status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    payment_method          VARCHAR(20)   NOT NULL,

    -- Financial
    subtotal                NUMERIC(12,2) NOT NULL,
    shipping_charge         NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount         NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_amount              NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount            NUMERIC(12,2) NOT NULL,
    coupon_code             VARCHAR(50),
    coupon_discount         NUMERIC(12,2) NOT NULL DEFAULT 0,

    -- Shipping address snapshot
    shipping_first_name     VARCHAR(100),
    shipping_last_name      VARCHAR(100),
    shipping_phone          VARCHAR(15),
    shipping_address_line1  VARCHAR(255),
    shipping_address_line2  VARCHAR(255),
    shipping_city           VARCHAR(100),
    shipping_state          VARCHAR(100),
    shipping_pin_code       VARCHAR(10),
    shipping_country        VARCHAR(100),

    -- Billing address snapshot
    billing_first_name      VARCHAR(100),
    billing_last_name       VARCHAR(100),
    billing_phone           VARCHAR(15),
    billing_address_line1   VARCHAR(255),
    billing_address_line2   VARCHAR(255),
    billing_city            VARCHAR(100),
    billing_state           VARCHAR(100),
    billing_pin_code        VARCHAR(10),
    billing_country         VARCHAR(100),

    -- Razorpay
    razorpay_order_id       VARCHAR(100),
    razorpay_payment_id     VARCHAR(100),
    razorpay_signature      VARCHAR(255),

    -- Shipping / tracking
    tracking_number         VARCHAR(100),
    shipping_provider       VARCHAR(100),
    estimated_delivery      DATE,
    delivered_at            TIMESTAMP,
    cancelled_at            TIMESTAMP,

    -- Notes
    notes                   TEXT,
    admin_notes             TEXT,

    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    created_by              VARCHAR(150),
    updated_by              VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_order_order_id       ON orders (order_id);
CREATE INDEX IF NOT EXISTS idx_order_user_id        ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_order_status         ON orders (order_status);
CREATE INDEX IF NOT EXISTS idx_order_payment_status ON orders (payment_status);

-- ──────────────────────────────────────────
-- ORDER_ITEMS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id                UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id          UUID        NOT NULL REFERENCES orders          (id) ON DELETE CASCADE,
    product_id        UUID        NOT NULL REFERENCES products        (id),
    variant_id        UUID        REFERENCES product_variants (id),
    product_name      VARCHAR(300) NOT NULL,
    variant_name      VARCHAR(200),
    sku               VARCHAR(100) NOT NULL,
    quantity          INT         NOT NULL,
    unit_price        NUMERIC(12,2) NOT NULL,
    total_price       NUMERIC(12,2) NOT NULL,
    product_image_url VARCHAR(500),
    created_at        TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT now(),
    created_by        VARCHAR(150),
    updated_by        VARCHAR(150)
);

-- ──────────────────────────────────────────
-- ORDER_STATUS_HISTORY
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_status_history (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id    UUID        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status      VARCHAR(30) NOT NULL,
    comment     VARCHAR(500),
    changed_by  VARCHAR(150),
    changed_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_osh_order_id ON order_status_history (order_id);

-- ──────────────────────────────────────────
-- PAYMENTS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id         UUID          NOT NULL UNIQUE REFERENCES orders (id),
    transaction_id   VARCHAR(150)  UNIQUE,
    gateway_name     VARCHAR(30)   NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    currency         VARCHAR(10)   NOT NULL DEFAULT 'INR',
    status           VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    gateway_response TEXT,
    paid_at          TIMESTAMP,
    refunded_at      TIMESTAMP,
    refund_amount    NUMERIC(12,2),
    created_at       TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT now(),
    created_by       VARCHAR(150),
    updated_by       VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_id ON payments (transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_id       ON payments (order_id);

-- ──────────────────────────────────────────
-- NOTIFICATIONS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(50) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     TEXT,
    is_read     BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

-- ──────────────────────────────────────────
-- WISHLIST_ITEMS
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS wishlist_items (
    id          UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users    (id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150),
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id)
);

-- ──────────────────────────────────────────
-- ORDER_MONTHLY_COUNTERS (for OrderIdGenerator)
-- ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_monthly_counters (
    year_month_key  VARCHAR(7)  NOT NULL PRIMARY KEY,   -- e.g. "202601"
    counter         BIGINT      NOT NULL DEFAULT 0
);

-- ──────────────────────────────────────────
-- Seed default roles
-- ──────────────────────────────────────────
INSERT INTO roles (name) VALUES ('ROLE_USER')  ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
