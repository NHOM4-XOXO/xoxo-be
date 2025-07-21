ALTER TABLE users
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN gender ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT 'OTHER',
    ADD COLUMN avatar_url TEXT,
    ADD COLUMN cover_url TEXT,
    ADD COLUMN bio TEXT;