
export type TenantStatus = 'ACTIVE' | 'INACTIVE';

export interface Tenant {
  id?: number;

  // Personal information
  fullName: string;
  mobileNumber: string;
  email?: string;
  aadhaarNumber?: string;
  gender?: string;
  joiningDate: string;

  // Rent details
  monthlyRent: number;
  securityDeposit: number;

  // Tenant status
  status: TenantStatus;

  // PG and room details
  pgId?: number;
  pgName?: string;
  roomId?: number;
  roomNumber?: string;
  bedId?: number;
  bedNumber?: string | number;

  // Additional information
  emergencyContact?: string;
  emergencyContactName?: string;
}

export interface TenantSummary {
  total: number;
  active: number;
  inactive: number;
  monthlyRent: number;
}
