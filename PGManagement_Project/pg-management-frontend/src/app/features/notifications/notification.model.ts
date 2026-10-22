export type NotificationType =
  | 'RENT'
  | 'PAYMENT'
  | 'TENANT'
  | 'SYSTEM'
  | 'GENERAL';

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  createdAt: string;
  read: boolean;
  tenantName?: string;
  roomNumber?: string;
  actionUrl?: string;
}