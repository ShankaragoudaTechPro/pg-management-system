export interface Tenant {
  id: number;
  name: string;
  mobile: string;
  email?: string | null;
  gender: string;
  joiningDate: string;
  emergencyContact?: string | null;
  emergencyContactName?: string | null;

  pgId: number;
  pgName?: string | null;

  roomId: number;
  roomNumber?: string | null;

  bedId: number;
  bedNumber?: number | null;

  monthlyRent: number;
  securityDeposit: number;

  active: boolean;

  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateTenantRequest {
  name: string;
  mobile: string;
  email?: string | null;
  gender: string;
  joiningDate: string;
  emergencyContact?: string | null;
  emergencyContactName?: string | null;

  pgId: number;
  roomId: number;
  bedId: number;

  monthlyRent: number;
  securityDeposit: number;
}

export interface TenantPage {
  content: Tenant[];

  totalElements: number;
  totalPages: number;
  size: number;
  number: number;

  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}