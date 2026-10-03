export const INDIAN_STATES: string[] = [
  // 28 States
  "Andhra Pradesh",
  "Arunachal Pradesh",
  "Assam",
  "Bihar",
  "Chhattisgarh",
  "Goa",
  "Gujarat",
  "Haryana",
  "Himachal Pradesh",
  "Jharkhand",
  "Karnataka",
  "Kerala",
  "Madhya Pradesh",
  "Maharashtra",
  "Manipur",
  "Meghalaya",
  "Mizoram",
  "Nagaland",
  "Odisha",
  "Punjab",
  "Rajasthan",
  "Sikkim",
  "Tamil Nadu",
  "Telangana",
  "Tripura",
  "Uttar Pradesh",
  "Uttarakhand",
  "West Bengal",
  // 8 Union Territories
  "Andaman and Nicobar Islands",
  "Chandigarh",
  "Dadra and Nagar Haveli and Daman and Diu",
  "Delhi",
  "Jammu and Kashmir",
  "Ladakh",
  "Lakshadweep",
  "Puducherry"
];

export const normalizeIndianState = (state: string): string => {
  return state
    .toLowerCase()
    .replace(/&/g, 'and')
    .replace(/[^a-z0-9]/g, '')
    .replace('nctofdelhi', 'delhi')
    .replace('nationalcapitalterritoryofdelhi', 'delhi')
    .replace('pondicherry', 'puducherry')
    .replace('uttaranchal', 'uttarakhand')
    .replace('orissa', 'odisha');
};

export const matchIndianState = (s1: string, s2: string): boolean => {
  if (!s1 || !s2) return false;
  const n1 = normalizeIndianState(s1);
  const n2 = normalizeIndianState(s2);
  return n1 === n2 || n1.includes(n2) || n2.includes(n1);
};
