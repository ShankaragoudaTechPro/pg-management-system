export type BedStatus = 'AVAILABLE' | 'OCCUPIED' | 'INACTIVE';

export interface Bed {
  id?: number;
  bedNumber: string | number;
  status: BedStatus;
  tenantId?: number;
  tenantName?: string;
}

export interface Room {
  id?: number;
  pgId?: number;
  roomNumber: string;
  floorNumber: number;
  sharingType: 'SINGLE' | 'DOUBLE' | 'TRIPLE' | 'FOUR_SHARING';
  totalBeds: number;
  status: 'ACTIVE' | 'INACTIVE';
  beds?: Bed[];
}

export interface RoomSummary {
  totalRooms: number;
  totalBeds: number;
  occupiedBeds: number;
  availableBeds: number;
  maintenanceBeds: number;
}
