-- ============================================================
-- Divya Gems E-Commerce — Full Schema Initialization
-- V1: All core tables — entity-accurate
-- PostgreSQL 14+
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ──────────────────────────────────────────────────────────────
-- ROLES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(30)  NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

-- ──────────────────────────────────────────────────────────────
-- USERS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    first_name                  VARCHAR(100) NOT NULL,
    last_name                   VARCHAR(100) NOT NULL,
    email                       VARCHAR(150) NOT NULL UNIQUE,
    password                    VARCHAR(255) NOT NULL,
    phone                       VARCHAR(15),
    alternate_phone             VARCHAR(15),
    profile_image_url           VARCHAR(500),
    is_email_verified           BOOLEAN      NOT NULL DEFAULT false,
    is_active                   BOOLEAN      NOT NULL DEFAULT true,
    -- Email verification
    email_verification_token    VARCHAR(255),
    email_verification_expiry   TIMESTAMPTZ,
    -- Password reset
    password_reset_token        VARCHAR(255),
    password_reset_expiry       TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by                  VARCHAR(150),
    updated_by                  VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_user_email              ON users (email);
CREATE INDEX IF NOT EXISTS idx_user_is_active          ON users (is_active);
CREATE INDEX IF NOT EXISTS idx_user_verify_token       ON users (email_verification_token)
    WHERE email_verification_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_reset_token        ON users (password_reset_token)
    WHERE password_reset_token IS NOT NULL;

-- ──────────────────────────────────────────────────────────────
-- USER_ROLES  (join table)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles (role_id);

-- ──────────────────────────────────────────────────────────────
-- REFRESH_TOKENS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token   ON refresh_tokens (token);

-- ──────────────────────────────────────────────────────────────
-- ADDRESSES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS addresses (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    address_type    VARCHAR(20)  NOT NULL DEFAULT 'HOME'
                        CHECK (address_type IN ('HOME', 'WORK', 'OTHER')),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    address_line1   VARCHAR(255) NOT NULL,
    address_line2   VARCHAR(255),
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    pin_code        VARCHAR(10)  NOT NULL,
    country         VARCHAR(100) NOT NULL DEFAULT 'India',
    is_default      BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(150),
    updated_by      VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_address_user_id ON addresses (user_id);

-- ──────────────────────────────────────────────────────────────
-- CATEGORIES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    slug             VARCHAR(200) NOT NULL UNIQUE,
    description      TEXT,
    image_url        VARCHAR(500),
    parent_id        UUID         REFERENCES categories (id) ON DELETE SET NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    display_order    INT          NOT NULL DEFAULT 0,
    meta_title       VARCHAR(200),
    meta_description VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       VARCHAR(150),
    updated_by       VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_category_slug      ON categories (slug);
CREATE INDEX IF NOT EXISTS idx_category_is_active ON categories (is_active);
CREATE INDEX IF NOT EXISTS idx_category_parent_id ON categories (parent_id);

-- ──────────────────────────────────────────────────────────────
-- TAGS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tags (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    slug       VARCHAR(150) NOT NULL UNIQUE,
    tag_type   VARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by VARCHAR(150),
    updated_by VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_tag_slug     ON tags (slug);
CREATE INDEX IF NOT EXISTS idx_tag_tag_type ON tags (tag_type);

-- ──────────────────────────────────────────────────────────────
-- PRODUCTS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name                VARCHAR(300)  NOT NULL,
    slug                VARCHAR(350)  NOT NULL UNIQUE,
    sku                 VARCHAR(100)  NOT NULL UNIQUE,
    short_description   VARCHAR(500),
    description         TEXT,
    benefits            TEXT,
    usage_instructions  TEXT,
    price               NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    sale_price          NUMERIC(12,2)           CHECK (sale_price >= 0),
    stock_quantity      INT           NOT NULL DEFAULT 0  CHECK (stock_quantity >= 0),
    low_stock_threshold INT           NOT NULL DEFAULT 5  CHECK (low_stock_threshold >= 0),
    weight              DOUBLE PRECISION,
    weight_unit         VARCHAR(20)   DEFAULT 'grams',
    status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('ACTIVE','INACTIVE','DRAFT','OUT_OF_STOCK')),
    is_featured         BOOLEAN       NOT NULL DEFAULT false,
    is_new_arrival      BOOLEAN       NOT NULL DEFAULT false,
    average_rating      DOUBLE PRECISION        DEFAULT 0.0
                            CHECK (average_rating >= 0 AND average_rating <= 5),
    total_reviews       INT           NOT NULL DEFAULT 0  CHECK (total_reviews >= 0),
    meta_title          VARCHAR(200),
    meta_description    VARCHAR(500),
    meta_keywords       VARCHAR(300),
    category_id         UUID          NOT NULL REFERENCES categories (id),
    sub_category_id     UUID          REFERENCES categories (id),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_product_slug           ON products (slug);
CREATE INDEX IF NOT EXISTS idx_product_sku            ON products (sku);
CREATE INDEX IF NOT EXISTS idx_product_status         ON products (status);
CREATE INDEX IF NOT EXISTS idx_product_is_featured    ON products (is_featured);
CREATE INDEX IF NOT EXISTS idx_product_is_new_arrival ON products (is_new_arrival);
CREATE INDEX IF NOT EXISTS idx_product_category_id    ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_product_sub_category   ON products (sub_category_id);
CREATE INDEX IF NOT EXISTS idx_product_price          ON products (price);
CREATE INDEX IF NOT EXISTS idx_product_avg_rating     ON products (average_rating DESC);
-- Full-text search index on name + description
CREATE INDEX IF NOT EXISTS idx_product_fts ON products
    USING gin(to_tsvector('english', coalesce(name,'') || ' ' || coalesce(description,'')));

-- ──────────────────────────────────────────────────────────────
-- PRODUCT_TAGS  (join table)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_tags (
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    tag_id     UUID NOT NULL REFERENCES tags     (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_product_tags_tag_id ON product_tags (tag_id);

-- ──────────────────────────────────────────────────────────────
-- PRODUCT_IMAGES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_images (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id    UUID         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    image_url     VARCHAR(500) NOT NULL,
    alt_text      VARCHAR(300),
    is_primary    BOOLEAN      NOT NULL DEFAULT false,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(150),
    updated_by    VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images (product_id);

-- ──────────────────────────────────────────────────────────────
-- PRODUCT_VARIANTS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_variants (
    id             UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id     UUID          NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    sku            VARCHAR(100)  NOT NULL UNIQUE,
    color_name     VARCHAR(100),
    color_hex_code VARCHAR(10),
    price          NUMERIC(12,2)          CHECK (price >= 0),
    sale_price     NUMERIC(12,2)          CHECK (sale_price >= 0),
    stock_quantity INT           NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    image_url      VARCHAR(500),
    is_active      BOOLEAN       NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     VARCHAR(150),
    updated_by     VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_variant_sku        ON product_variants (sku);
CREATE INDEX IF NOT EXISTS idx_variant_product_id ON product_variants (product_id);

-- ──────────────────────────────────────────────────────────────
-- PRODUCT_VARIANT_IMAGES
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_variant_images (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    variant_id    UUID         NOT NULL REFERENCES product_variants (id) ON DELETE CASCADE,
    image_url     VARCHAR(500) NOT NULL,
    alt_text      VARCHAR(300),
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(150),
    updated_by    VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_variant_images_variant_id ON product_variant_images (variant_id);

-- ──────────────────────────────────────────────────────────────
-- REVIEWS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id           UUID        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    user_id              UUID        NOT NULL REFERENCES users    (id) ON DELETE CASCADE,
    rating               INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title                VARCHAR(255),
    body                 TEXT,
    is_verified_purchase BOOLEAN     NOT NULL DEFAULT false,
    is_approved          BOOLEAN     NOT NULL DEFAULT false,
    helpful_count        INT         NOT NULL DEFAULT 0 CHECK (helpful_count >= 0),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           VARCHAR(150),
    updated_by           VARCHAR(150),
    -- One review per user per product
    CONSTRAINT uq_review_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_review_product_id  ON reviews (product_id);
CREATE INDEX IF NOT EXISTS idx_review_user_id     ON reviews (user_id);
CREATE INDEX IF NOT EXISTS idx_review_is_approved ON reviews (is_approved);
CREATE INDEX IF NOT EXISTS idx_review_rating      ON reviews (rating);

-- ──────────────────────────────────────────────────────────────
-- CARTS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS carts (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID         REFERENCES users (id) ON DELETE CASCADE,
    session_id VARCHAR(255),
    is_active  BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by VARCHAR(150),
    updated_by VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_cart_user_id    ON carts (user_id);
CREATE INDEX IF NOT EXISTS idx_cart_session_id ON carts (session_id);

-- ──────────────────────────────────────────────────────────────
-- CART_ITEMS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cart_items (
    id                 UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    cart_id            UUID          NOT NULL REFERENCES carts            (id) ON DELETE CASCADE,
    product_id         UUID          NOT NULL REFERENCES products         (id) ON DELETE CASCADE,
    variant_id         UUID          REFERENCES product_variants (id) ON DELETE SET NULL,
    quantity           INT           NOT NULL CHECK (quantity > 0),
    price_at_add_time  NUMERIC(12,2) NOT NULL CHECK (price_at_add_time >= 0),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         VARCHAR(150),
    updated_by         VARCHAR(150),
    CONSTRAINT uq_cart_product_variant UNIQUE (cart_id, product_id, variant_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id    ON cart_items (cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id ON cart_items (product_id);

-- ──────────────────────────────────────────────────────────────
-- COUPONS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupons (
    id                      UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code                    VARCHAR(50)   NOT NULL UNIQUE,
    description             VARCHAR(500),
    discount_type           VARCHAR(20)   NOT NULL CHECK (discount_type IN ('PERCENTAGE','FLAT')),
    discount_value          NUMERIC(12,2) NOT NULL CHECK (discount_value > 0),
    minimum_order_amount    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (minimum_order_amount >= 0),
    maximum_discount_amount NUMERIC(12,2)            CHECK (maximum_discount_amount > 0),
    usage_limit             INT           NOT NULL DEFAULT 0  CHECK (usage_limit >= 0),
    used_count              INT           NOT NULL DEFAULT 0  CHECK (used_count >= 0),
    valid_from              TIMESTAMPTZ   NOT NULL,
    valid_until             TIMESTAMPTZ   NOT NULL,
    is_active               BOOLEAN       NOT NULL DEFAULT true,
    is_one_time_per_user    BOOLEAN       NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by              VARCHAR(150),
    updated_by              VARCHAR(150),
    CONSTRAINT chk_coupon_dates CHECK (valid_until > valid_from)
);

CREATE INDEX IF NOT EXISTS idx_coupon_code      ON coupons (code);
CREATE INDEX IF NOT EXISTS idx_coupon_is_active ON coupons (is_active);
CREATE INDEX IF NOT EXISTS idx_coupon_validity  ON coupons (valid_from, valid_until);

-- ──────────────────────────────────────────────────────────────
-- ORDERS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id              UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id        VARCHAR(30)   NOT NULL UNIQUE,           -- e.g. DG-202601-00001
    user_id         UUID          NOT NULL REFERENCES users (id),
    order_status    VARCHAR(30)   NOT NULL DEFAULT 'PENDING'
                        CHECK (order_status IN (
                            'PENDING','CONFIRMED','PROCESSING','SHIPPED',
                            'OUT_FOR_DELIVERY','DELIVERED','CANCELLED',
                            'REFUND_INITIATED','REFUNDED','RETURN_REQUESTED','RETURNED'
                        )),
    payment_status  VARCHAR(30)   NOT NULL DEFAULT 'PENDING'
                        CHECK (payment_status IN ('PENDING','PAID','FAILED','REFUNDED','PARTIALLY_REFUNDED')),
    payment_method  VARCHAR(20)   NOT NULL
                        CHECK (payment_method IN ('ONLINE','COD')),

    -- Financials
    subtotal           NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    shipping_charge    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (shipping_charge >= 0),
    discount_amount    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    coupon_code        VARCHAR(50),
    coupon_discount    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (coupon_discount >= 0),
    tax_amount         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    total_amount       NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),

    -- Shipping address snapshot
    shipping_first_name    VARCHAR(100),
    shipping_last_name     VARCHAR(100),
    shipping_phone         VARCHAR(15),
    shipping_address_line1 VARCHAR(255),
    shipping_address_line2 VARCHAR(255),
    shipping_city          VARCHAR(100),
    shipping_state         VARCHAR(100),
    shipping_pin_code      VARCHAR(10),
    shipping_country       VARCHAR(100) DEFAULT 'India',

    -- Billing address snapshot
    billing_first_name    VARCHAR(100),
    billing_last_name     VARCHAR(100),
    billing_phone         VARCHAR(15),
    billing_address_line1 VARCHAR(255),
    billing_address_line2 VARCHAR(255),
    billing_city          VARCHAR(100),
    billing_state         VARCHAR(100),
    billing_pin_code      VARCHAR(10),
    billing_country       VARCHAR(100) DEFAULT 'India',

    -- Razorpay
    razorpay_order_id   VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    razorpay_signature  VARCHAR(500),

    -- Fulfilment
    tracking_number     VARCHAR(100),
    shipping_provider   VARCHAR(100),
    estimated_delivery  DATE,
    delivered_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,

    -- Notes
    notes       TEXT,
    admin_notes TEXT,

    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_order_order_id           ON orders (order_id);
CREATE INDEX IF NOT EXISTS idx_order_user_id            ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_order_status             ON orders (order_status);
CREATE INDEX IF NOT EXISTS idx_order_payment_status     ON orders (payment_status);
CREATE INDEX IF NOT EXISTS idx_order_created_at         ON orders (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_razorpay_order_id  ON orders (razorpay_order_id)
    WHERE razorpay_order_id IS NOT NULL;

-- ──────────────────────────────────────────────────────────────
-- ORDER_ITEMS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id                UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id          UUID          NOT NULL REFERENCES orders           (id) ON DELETE CASCADE,
    product_id        UUID          NOT NULL REFERENCES products         (id),
    variant_id        UUID          REFERENCES product_variants (id),
    product_name      VARCHAR(300)  NOT NULL,
    variant_name      VARCHAR(200),
    sku               VARCHAR(100)  NOT NULL,
    quantity          INT           NOT NULL CHECK (quantity > 0),
    unit_price        NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    total_price       NUMERIC(12,2) NOT NULL CHECK (total_price >= 0),
    product_image_url VARCHAR(500),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        VARCHAR(150),
    updated_by        VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id   ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items (product_id);

-- ──────────────────────────────────────────────────────────────
-- ORDER_STATUS_HISTORY
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_status_history (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id   UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status     VARCHAR(30)  NOT NULL,
    comment    VARCHAR(500),
    changed_by VARCHAR(150),
    changed_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by VARCHAR(150),
    updated_by VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_osh_order_id ON order_status_history (order_id);

-- ──────────────────────────────────────────────────────────────
-- PAYMENTS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id         UUID          NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    transaction_id   VARCHAR(150)  UNIQUE,
    gateway_name     VARCHAR(30)   NOT NULL DEFAULT 'RAZORPAY',
    amount           NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(10)   NOT NULL DEFAULT 'INR',
    status           VARCHAR(30)   NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    gateway_response TEXT,
    paid_at          TIMESTAMPTZ,
    refunded_at      TIMESTAMPTZ,
    refund_amount    NUMERIC(12,2)          CHECK (refund_amount >= 0),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       VARCHAR(150),
    updated_by       VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_payment_order_id       ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payment_transaction_id ON payments (transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_status         ON payments (status);

-- ──────────────────────────────────────────────────────────────
-- WISHLIST_ITEMS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS wishlist_items (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users           (id) ON DELETE CASCADE,
    product_id UUID        NOT NULL REFERENCES products        (id) ON DELETE CASCADE,
    variant_id UUID        REFERENCES product_variants (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(150),
    updated_by VARCHAR(150),
    -- One wishlist entry per user+product+variant combination
    CONSTRAINT uq_wishlist_user_product_variant UNIQUE (user_id, product_id, variant_id)
);

CREATE INDEX IF NOT EXISTS idx_wishlist_user_id    ON wishlist_items (user_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_product_id ON wishlist_items (product_id);

-- PostgreSQL UNIQUE treats NULL != NULL, so the composite constraint above allows
-- multiple rows with the same (user_id, product_id, NULL variant_id).
-- This partial index enforces single-entry when no variant is specified.
CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlist_no_variant
    ON wishlist_items (user_id, product_id)
    WHERE variant_id IS NULL;

-- ──────────────────────────────────────────────────────────────
-- NOTIFICATIONS
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(30)  NOT NULL
                     CHECK (type IN (
                         'ORDER_PLACED','ORDER_SHIPPED','ORDER_DELIVERED',
                         'ORDER_CANCELLED','PAYMENT_SUCCESS','PAYMENT_FAILED',
                         'COUPON_APPLIED','GENERAL'
                     )),
    title        VARCHAR(255) NOT NULL,
    message      TEXT         NOT NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT false,
    redirect_url VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(150),
    updated_by   VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS idx_notification_user_id ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notifications (is_read);
CREATE INDEX IF NOT EXISTS idx_notification_created ON notifications (user_id, created_at DESC);

-- ──────────────────────────────────────────────────────────────
-- ORDER_MONTHLY_COUNTERS  (for thread-safe order ID generation)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_monthly_counters (
    year_month_key VARCHAR(7) NOT NULL PRIMARY KEY,  -- e.g. "202601"
    counter        BIGINT     NOT NULL DEFAULT 0
);
