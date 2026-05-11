export interface Chain {
  id: number;
  chain_id: string;
  name: string;
  display_name: string;
  last_synced: string | null;
}

export interface ProductSummary {
  id: number;
  item_code: string;
  name: string;
  manufacturer_name: string | null;
  min_price: number | null;
  max_price: number | null;
  num_chains: number;
}

export interface PriceEntry {
  chain_id: number;
  chain_name: string;
  chain_display_name: string;
  price: number;
  unit_measure_price: number | null;
  allow_discount: boolean;
  updated_at: string | null;
}

export interface ProductDetail {
  id: number;
  item_code: string;
  name: string;
  manufacturer_name: string | null;
  unit_qty: string | null;
  quantity: number | null;
  is_weighted: boolean;
  unit_of_measure: string | null;
  prices: PriceEntry[];
}

export interface SyncLog {
  id: number;
  chain_id: number;
  file_name: string;
  file_type: string | null;
  downloaded_at: string | null;
  records_count: number;
  status: string;
  error_message: string | null;
}

export interface SyncStatus {
  chains: Chain[];
  recent_logs: SyncLog[];
}
