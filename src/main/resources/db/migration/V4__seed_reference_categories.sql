-- =====================================================================================
-- V4: Seed reference categories observed in the existing UX/UI, so the API is immediately
-- usable by the frontend team without waiting on Back Office admin tooling.
-- =====================================================================================
INSERT INTO categories (name, slug, description, display_order, is_active, created_by, updated_by)
VALUES
    ('ព័ត៌មានជាតិ', 'national-news', 'National News', 10, TRUE, 'system', 'system'),
    ('សកម្មភាពថ្នាក់ដឹកនាំ', 'leadership-activities', 'Director General / Leadership Activities', 20, TRUE, 'system', 'system'),
    ('ព័ត៌មានអន្តរជាតិ', 'international-news', 'International News', 30, TRUE, 'system', 'system'),
    ('សេចក្តីជូនដំណឹង', 'announcements', 'Public Announcements', 40, TRUE, 'system', 'system');
