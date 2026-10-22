export interface DashboardResponse {
  pgId: number;
  pgName: string;
  totalRooms: number;
  totalBeds: number;
  occupiedBeds: number;
  availableBeds: number;
  activeTenants: number;
  occupancyPercentage: number;
  rentDue: number;
  rentOverdue: number;
  rentCollected: number;
}
