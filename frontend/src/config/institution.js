// All institution-specific text lives here, so it can be updated in one place
// (for example for the next semester) without touching the pages.

export const INSTITUTION = {
  name: 'IIT Madras',
  office: 'Office of Global Engagement',
  officeShort: 'OGE',
  programme: 'International Semester Exchange Programme',
  semester: 'July–November 2026',
};

/** Shown in the top bar and on the login page. */
export const APP_TITLE = `${INSTITUTION.office} (${INSTITUTION.officeShort}), ${INSTITUTION.name}`;

/** Hostel allotment for international students. */
export const HOSTELS = [
  { name: 'Tamiraparani Hostel', forWhom: 'Male international students' },
  { name: 'Sabarmati Hostel', forWhom: 'Female international students' },
];

export const HOSTEL_OFFICE_HOURS = [
  { days: 'Monday – Friday', hours: '10:00 AM – 6:00 PM' },
  { days: 'Saturday', hours: '10:30 AM – 1:00 PM' },
];

export const LINKS = {
  biodataForm: 'https://ikollege.iitm.ac.in/iitmhostel/studentRegistration',
  selfDeclaration: 'https://drive.google.com/file/d/12nHAzLFJMCPFki6-V0J96BXka4_bg0b7/view',
  solemnAffirmation: 'https://drive.google.com/file/d/1br-VamYmC9J4QhiH59nuHctbYmNrY1-J/view',
  hostelManagementMap: 'https://maps.app.goo.gl/nMPtqozebMNyzLR76',
};

/** Where hostel fees are paid. */
export const FEE_OFFICE = {
  name: 'Temporary Accommodation Section, Office of Hostel Management',
  room: 'Ground Floor, Room No. 105',
};
