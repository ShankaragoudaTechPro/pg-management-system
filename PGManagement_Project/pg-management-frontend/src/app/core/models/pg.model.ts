export interface PgProperty {
  id: number;
  name: string;
  type: string;
  description?: string | null;
  status: string;
}
