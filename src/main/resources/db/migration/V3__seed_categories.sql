-- ============================================================
-- V3: Seed Categories for Divya Gems
-- 10 parent categories + 52 subcategories
-- Slugs are lowercase-hyphenated; display_order sets nav sort.
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- Helper: temp table to hold parent UUIDs for FK use
-- ──────────────────────────────────────────────────────────────
DO $$
DECLARE
    -- Parent category UUIDs
    v_crystals       UUID;
    v_pyramids       UUID;
    v_vastu          UUID;
    v_accessories    UUID;
    v_meditation     UUID;
    v_energy         UUID;
    v_rudraksha      UUID;
    v_yantras        UUID;
    v_incense        UUID;
    v_books          UUID;
BEGIN

-- ══════════════════════════════════════════════════════════════
-- PARENT CATEGORIES
-- ══════════════════════════════════════════════════════════════

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Crystals & Gemstones', 'crystals-gemstones',
        'Natural crystals and gemstones for healing, meditation, and spiritual growth.',
        true, 1)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_crystals FROM categories WHERE slug = 'crystals-gemstones';

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Pyramids', 'pyramids',
        'Sacred pyramid structures for energy amplification, Vastu correction, and manifestation.',
        true, 2)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_pyramids FROM categories WHERE slug = 'pyramids';

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Vastu Products', 'vastu-products',
        'Authentic Vastu Shastra products to harmonise home and workspace energies.',
        true, 3)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_vastu FROM categories WHERE slug = 'vastu-products';

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Spiritual Accessories', 'spiritual-accessories',
        'Malas, bracelets, pendants, and wearable spiritual items crafted with intention.',
        true, 4)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_accessories FROM categories WHERE slug = 'spiritual-accessories';

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Meditation Tools', 'meditation-tools',
        'Everything you need for a powerful meditation practice — singing bowls to cushions.',
        true, 5)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_meditation FROM categories WHERE slug = 'meditation-tools';

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Energy Products', 'energy-products',
        'Orgonite, orgone pyramids, EMF protection, and energy-balancing tools.',
        true, 6)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_energy FROM categories WHERE slug = 'energy-products';

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Rudraksha', 'rudraksha',
        'Authentic Rudraksha beads and malas — 1 to 21 mukhi — directly from Nepal and Java.',
        true, 7)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_rudraksha FROM categories WHERE slug = 'rudraksha';

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Yantras', 'yantras',
        'Sacred geometric yantras for wealth, protection, health, and spiritual advancement.',
        true, 8)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_yantras FROM categories WHERE slug = 'yantras';

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Incense & Aromatherapy', 'incense-aromatherapy',
        'Premium incense sticks, dhoop cones, essential oils, and aromatherapy diffusers.',
        true, 9)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_incense FROM categories WHERE slug = 'incense-aromatherapy';

INSERT INTO categories (name, slug, description, is_active, display_order)
VALUES ('Books & Oracle Cards', 'books-oracle-cards',
        'Spiritual books, tarot decks, oracle cards, and learning resources.',
        true, 10)
ON CONFLICT (slug) DO NOTHING;
SELECT id INTO v_books FROM categories WHERE slug = 'books-oracle-cards';

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Crystals & Gemstones
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Raw & Natural Crystals', 'raw-natural-crystals',
     'Unpolished, raw crystal specimens in their natural form.', v_crystals, true, 1),
    ('Tumbled Stones', 'tumbled-stones',
     'Smooth, polished tumbled stones — perfect for carrying or placing on the body.', v_crystals, true, 2),
    ('Crystal Clusters & Geodes', 'crystal-clusters-geodes',
     'Stunning crystal clusters and geodes for energy and decor.', v_crystals, true, 3),
    ('Healing Wands', 'healing-wands',
     'Crystal wands for energy healing, chakra work, and intention-setting.', v_crystals, true, 4),
    ('Crystal Spheres & Balls', 'crystal-spheres',
     'Polished crystal spheres for scrying, meditation, and energy diffusion.', v_crystals, true, 5),
    ('Crystal Palm Stones', 'crystal-palm-stones',
     'Smooth, oval palm stones ideal for meditation and anxiety relief.', v_crystals, true, 6),
    ('Gemstone Towers & Points', 'gemstone-towers-points',
     'Crystal towers and generator points for directing and amplifying energy.', v_crystals, true, 7),
    ('Crystal Sets & Kits', 'crystal-sets-kits',
     'Curated crystal sets for chakra healing, manifesting, and protection.', v_crystals, true, 8)
ON CONFLICT (slug) DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Pyramids
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Crystal Pyramids', 'crystal-pyramids',
     'Pyramids crafted from natural gemstones — Amethyst, Rose Quartz, Black Tourmaline.', v_pyramids, true, 1),
    ('Orgone Pyramids', 'orgone-pyramids',
     'Resin-cast orgone energy pyramids for EMF protection and energy balancing.', v_pyramids, true, 2),
    ('Copper Pyramids', 'copper-pyramids',
     'Pure copper pyramids for Vastu correction and energy amplification.', v_pyramids, true, 3),
    ('Brass & Metal Pyramids', 'brass-metal-pyramids',
     'Decorative and functional Vastu pyramids in brass, bronze, and mixed metals.', v_pyramids, true, 4),
    ('Pyramid Sets', 'pyramid-sets',
     'Multi-pyramid Vastu sets for comprehensive space energisation.', v_pyramids, true, 5)
ON CONFLICT (slug) DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Vastu Products
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Vastu Yantras', 'vastu-yantras',
     'Energised yantras designed specifically for Vastu Shastra correction.', v_vastu, true, 1),
    ('Vastu Tortoise & Animals', 'vastu-tortoise-animals',
     'Feng Shui and Vastu animal figurines — tortoise, dragon, elephant, fish.', v_vastu, true, 2),
    ('Wind Chimes', 'wind-chimes',
     'Metal and crystal wind chimes to attract positive chi and remove negative energy.', v_vastu, true, 3),
    ('Vastu Mirrors & Bagua', 'vastu-mirrors-bagua',
     'Bagua mirrors and convex/concave Vastu mirrors for deflecting negative energy.', v_vastu, true, 4),
    ('House Warming & Puja Items', 'house-warming-puja-items',
     'Auspicious items for griha pravesh, pujas, and home blessings.', v_vastu, true, 5)
ON CONFLICT (slug) DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Spiritual Accessories
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Gemstone Bracelets', 'gemstone-bracelets',
     'Elastic and knotted gemstone bracelets for healing and style.', v_accessories, true, 1),
    ('Crystal Pendants & Necklaces', 'crystal-pendants-necklaces',
     'Sterling silver and gold-plated crystal pendants and necklaces.', v_accessories, true, 2),
    ('Mala Beads & Prayer Beads', 'mala-beads',
     '108-bead crystal, Rudraksha, and Tulsi malas for japa and meditation.', v_accessories, true, 3),
    ('Rings', 'spiritual-rings',
     'Astrological and spiritual gemstone rings for all fingers.', v_accessories, true, 4),
    ('Earrings', 'crystal-earrings',
     'Gemstone stud and dangle earrings.', v_accessories, true, 5),
    ('Anklets', 'crystal-anklets',
     'Crystal and silver anklets for positive energy flow.', v_accessories, true, 6)
ON CONFLICT (slug) DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Meditation Tools
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Singing Bowls', 'singing-bowls',
     'Tibetan singing bowls and crystal singing bowls for sound healing.', v_meditation, true, 1),
    ('Meditation Cushions & Mats', 'meditation-cushions-mats',
     'Zafu cushions, zabuton mats, and meditation shawls.', v_meditation, true, 2),
    ('Mantra & Singing Bell Sets', 'mantra-bell-sets',
     'Tingsha cymbals, dorje bells, and hand bells for clearing energy.', v_meditation, true, 3),
    ('Meditation Accessories', 'meditation-accessories',
     'Eye masks, timers, incense holders, and altar items for meditation.', v_meditation, true, 4),
    ('Chakra Sets', 'chakra-sets',
     'Complete 7-chakra crystal sets with guide cards and pouches.', v_meditation, true, 5)
ON CONFLICT (slug) DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Energy Products
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Orgonite', 'orgonite',
     'Orgonite pendants, discs, and devices for EMF protection and energy balancing.', v_energy, true, 1),
    ('EMF Protection', 'emf-protection',
     'Shungite, black tourmaline, and orgone devices for EMF shielding.', v_energy, true, 2),
    ('Energy Plates & Coasters', 'energy-plates-coasters',
     'Copper and crystal energy charging plates.', v_energy, true, 3),
    ('Feng Shui Items', 'feng-shui-items',
     'Laughing Buddha, lucky cats, wealth bowls, and other Feng Shui enhancers.', v_energy, true, 4)
ON CONFLICT (slug) DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Rudraksha
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Nepal Rudraksha', 'nepal-rudraksha',
     'Large, premium Rudraksha beads from Nepal — 1 to 14 mukhi.', v_rudraksha, true, 1),
    ('Java Rudraksha', 'java-rudraksha',
     'Smaller, rounder Java Rudraksha beads — 1 to 21 mukhi.', v_rudraksha, true, 2),
    ('Rudraksha Malas', 'rudraksha-malas',
     '108+1 bead Rudraksha malas in various mukhi combinations.', v_rudraksha, true, 3),
    ('Rudraksha Bracelets', 'rudraksha-bracelets',
     'Wearable Rudraksha bracelets in single and mixed mukhi.', v_rudraksha, true, 4),
    ('Certified Rudraksha', 'certified-rudraksha',
     'Lab-certified authentic Rudraksha beads with authenticity certificates.', v_rudraksha, true, 5)
ON CONFLICT (slug) DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Yantras
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Sri Yantra', 'sri-yantra',
     'The mother of all yantras — for abundance, prosperity, and divine feminine energy.', v_yantras, true, 1),
    ('Kuber Yantra', 'kuber-yantra',
     'Yantra of Lord Kuber for wealth attraction and financial prosperity.', v_yantras, true, 2),
    ('Navagraha Yantra', 'navagraha-yantra',
     'Nine-planet yantra for astrological balance and removing planetary doshas.', v_yantras, true, 3),
    ('Pyramid Yantra', 'pyramid-yantra',
     'Yantra combined with pyramid geometry for amplified energy.', v_yantras, true, 4),
    ('Vastu Yantra', 'vastu-yantra-category',
     'Yantras designed for Vastu Shastra correction and space energisation.', v_yantras, true, 5),
    ('All Yantras', 'all-yantras',
     'Full collection of energised brass, copper, and silver yantras.', v_yantras, true, 6)
ON CONFLICT (slug) DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Incense & Aromatherapy
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Incense Sticks', 'incense-sticks',
     'Premium masala and charcoal incense sticks — Nag Champa, Sandalwood, Palo Santo.', v_incense, true, 1),
    ('Dhoop Cones & Cakes', 'dhoop-cones-cakes',
     'Resin-based dhoop cones and cakes for deep, long-lasting fragrance.', v_incense, true, 2),
    ('Palo Santo & Sage', 'palo-santo-sage',
     'Authentic Palo Santo sticks and white sage bundles for smudging.', v_incense, true, 3),
    ('Essential Oils', 'essential-oils',
     '100% pure essential oils for aromatherapy and energy healing.', v_incense, true, 4),
    ('Diffusers & Burners', 'diffusers-burners',
     'Electric diffusers, terracotta burners, and ceramic oil warmers.', v_incense, true, 5)
ON CONFLICT (slug) DO NOTHING;

-- ══════════════════════════════════════════════════════════════
-- SUBCATEGORIES — Books & Oracle Cards
-- ══════════════════════════════════════════════════════════════
INSERT INTO categories (name, slug, description, parent_id, is_active, display_order) VALUES
    ('Tarot Decks', 'tarot-decks',
     'Classic and modern tarot decks with guidebooks.', v_books, true, 1),
    ('Oracle Cards', 'oracle-cards',
     'Angel, crystal, chakra, and affirmation oracle card decks.', v_books, true, 2),
    ('Astrology Books', 'astrology-books',
     'Vedic and Western astrology books for all levels.', v_books, true, 3),
    ('Crystal & Healing Books', 'crystal-healing-books',
     'Books on crystal therapy, chakra healing, and Reiki.', v_books, true, 4),
    ('Spiritual Growth Books', 'spiritual-growth-books',
     'Books on mindfulness, meditation, and spiritual awakening.', v_books, true, 5)
ON CONFLICT (slug) DO NOTHING;

END $$;
