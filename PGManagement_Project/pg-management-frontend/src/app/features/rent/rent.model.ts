export type RentStatus = 'PENDING' | 'PAID' | 'OVERDUE';

export type PaymentMode = 'CASH' | 'UPI' | 'BANK_TRANSFER' | 'OTHER';

/** UI-normalized rent record mapped from the backend RentResponse DTO. */
export interface RentRecord {
  id: number;
  tenantId: number;
  tenantName: string;
  pgId?: number;
  pgName?: string;
  roomNumber?: string;
  bedNumber?: string;
  rentMonth: string;
  amount: number;
  paidAmount: number;
  periodStart: string;
  periodEnd: string;
  dueDate: string;
  paidDate?: string;
  status: RentStatus;
  paymentMode?: PaymentMode;
  note?: string;
}

export interface RentCreateRequest {
  rentAmount: number;
  periodStart: string;
  periodEnd: string;
  dueDate: string;
  paymentMode?: PaymentMode;
  paymentNote?: string;
}

export interface RecordPaymentRequest {
  paidDate: string;
  paymentMode: PaymentMode;
  paymentNote?: string;
}

export interface RentSummary {
  totalRecords: number;
  expected: number;
  collected: number;
  outstanding: number;
  overdue: number;
}
