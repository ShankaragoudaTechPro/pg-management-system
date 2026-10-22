export interface OwnerProfile {
  id: number;
  name: string;
  email: string;
  mobile: string;
  status: string;
  createdAt: string | null;
  updatedAt?: string | null;
}

export interface OwnerProfileUpdate {
  name: string;
  email: string;
  mobile: string;
}

export interface UpdatePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}
