-- V5: IIT Madras, Office of Global Engagement (OGE) document checklist for the
-- International Semester Exchange Programme (July-November 2026).
--
-- V1-V4 have already run on existing databases, so they must never be edited.
-- Changes always go into a NEW migration file like this one.

-- Optional link to a form the student has to download / fill in.
ALTER TABLE document_types ADD COLUMN form_url VARCHAR(500);

-- Reuse the rows created in V3 (keeps any files students already uploaded).
UPDATE document_types SET name = 'IIT Madras offer letter',
       description = 'Offer / admission letter issued by IIT Madras (also hand a copy to the Hostel Manager)',
       sort_order = 10
 WHERE code = 'ADMISSION_LETTER';

UPDATE document_types SET name = 'Passport / citizenship proof',
       description = 'Scan of the passport photo and address pages. Bring the original and a hard copy on arrival.',
       sort_order = 20
 WHERE code = 'PASSPORT';

UPDATE document_types SET name = 'Visa details',
       description = 'Indian student visa / e-visa page showing visa number, type and validity',
       sort_order = 30
 WHERE code = 'VISA';

UPDATE document_types SET name = 'Passport-size colour photograph',
       description = 'Recent colour photo, the same one pasted on the BIODATA form',
       sort_order = 90
 WHERE code = 'PHOTO';

UPDATE document_types SET name = 'Health insurance',
       description = 'Proof of health / travel insurance valid for the whole stay in India',
       sort_order = 110
 WHERE code = 'HEALTH_INSURANCE';

UPDATE document_types SET name = 'Learning agreement',
       description = 'Course / learning agreement approved by your home university',
       sort_order = 120
 WHERE code = 'LEARNING_AGREEMENT';

UPDATE document_types SET sort_order = 130 WHERE code = 'DEGREE_CERTIFICATE';

-- New documents from the OGE hostel allotment instructions.
INSERT INTO document_types (code, name, description, student_category, sort_order, form_url) VALUES
    ('BONAFIDE_LETTER', 'Bonafide letter',
     'Bonafide / enrolment certificate from your home university', NULL, 40, NULL),
    ('BIODATA_FORM', 'Signed BIODATA form',
     'Register on iKollege, download the form, paste your photo, sign it and upload a scan. Bring the signed hard copy.',
     NULL, 50, 'https://ikollege.iitm.ac.in/iitmhostel/studentRegistration'),
    ('SELF_DECLARATION', 'Self-Declaration Addendum',
     'Download, print, sign and upload the scanned form. Hand the signed copy to the Hostel Manager.',
     NULL, 60, 'https://drive.google.com/file/d/12nHAzLFJMCPFki6-V0J96BXka4_bg0b7/view'),
    ('SOLEMN_AFFIRMATION', 'Solemn Affirmation of Conformance to Rules and Regulations',
     'Download, print, sign and upload the scanned form. Hand the signed copy to the Hostel Manager.',
     NULL, 70, 'https://drive.google.com/file/d/1br-VamYmC9J4QhiH59nuHctbYmNrY1-J/view'),
    ('BANK_DETAILS', 'Bank details proof',
     'Bank statement, passbook front page or cancelled cheque showing account holder name and account number',
     NULL, 80, NULL),
    ('NOMINATION_LETTER', 'Exchange nomination letter',
     'Nomination letter from your home university for the IIT Madras exchange semester', 'EXCHANGE', 115, NULL);
