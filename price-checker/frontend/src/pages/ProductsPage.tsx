import { useState, useCallback, useEffect, useRef } from "react";
import type { ProductSummary, ProductDetail, Chain } from "../types";
import Spinner from "../components/Spinner";

const BASE = "/api";

function formatPrice(p: number | null) {
  if (p == null) return "—";
  return `₪${p.toFixed(2)}`;
}

function PriceCompare({ product }: { product: ProductDetail }) {
  const sorted = [...product.prices].sort((a, b) => a.price - b.price);
  const min = sorted[0]?.price ?? 0;

  return (
    <div className="price-compare">
      <h3>{product.name}</h3>
      <p className="item-meta">
        {product.manufacturer_name && <span>{product.manufacturer_name} · </span>}
        <span>{product.item_code}</span>
        {product.unit_qty && <span> · {product.unit_qty}</span>}
      </p>

      {sorted.length === 0 ? (
        <p>אין מחירים זמינים</p>
      ) : (
        <table className="prices-table">
          <thead>
            <tr>
              <th>רשת</th>
              <th>עיר</th>
              <th>מחיר</th>
              <th>מחיר ליחידה</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((p) => (
              <tr key={p.chain_id} className={p.price === min ? "cheapest" : ""}>
                <td>{p.chain_display_name}</td>
                <td>{p.store_city ?? "—"}</td>
                <td className="price-cell">₪{p.price.toFixed(2)}</td>
                <td>{p.unit_measure_price ? `₪${p.unit_measure_price.toFixed(2)}` : "—"}</td>
                <td>{p.price === min ? "✓ הזול ביותר" : ""}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default function ProductsPage({ chains }: { chains: Chain[] }) {
  const [query, setQuery] = useState("");
  const [chainFilter, setChainFilter] = useState<number | "">("");
  const [cityFilter, setCityFilter] = useState("");
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<ProductDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const search = useCallback((q: string, chain: number | "", city: string) => {
    setLoading(true);
    const params = new URLSearchParams({ limit: "200" });
    if (q) params.set("q", q);
    if (chain) params.set("chain_id", String(chain));
    if (city) params.set("city", city);
    fetch(`${BASE}/products?${params}`)
      .then((r) => r.json())
      .then(setProducts)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => search(query, chainFilter, cityFilter), 300);
  }, [query, chainFilter, cityFilter, search]);

  const openProduct = (item_code: string) => {
    setDetailLoading(true);
    setSelected(null);
    fetch(`${BASE}/products/${item_code}`)
      .then((r) => r.json())
      .then(setSelected)
      .finally(() => setDetailLoading(false));
  };

  return (
    <div className="page">
      <div className="search-bar">
        <input
          type="text"
          placeholder="חפש מוצר לפי שם או ברקוד..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="search-input"
        />
        <select
          value={chainFilter}
          onChange={(e) => setChainFilter(e.target.value ? Number(e.target.value) : "")}
          className="chain-select"
        >
          <option value="">כל הרשתות</option>
          {chains.map((c) => (
            <option key={c.id} value={c.id}>
              {c.display_name}
            </option>
          ))}
        </select>
        <input
          type="text"
          placeholder="סנן לפי עיר..."
          value={cityFilter}
          onChange={(e) => setCityFilter(e.target.value)}
          className="search-input"
        />
      </div>

      <div className="layout">
        <div className="product-list">
          {loading ? (
            <Spinner />
          ) : products.length === 0 ? (
            <p className="empty">לא נמצאו מוצרים</p>
          ) : (
            products.map((p) => (
              <div
                key={p.id}
                className={`product-card ${selected?.item_code === p.item_code ? "active" : ""}`}
                onClick={() => openProduct(p.item_code)}
              >
                <div className="product-name">{p.name}</div>
                {p.manufacturer_name && (
                  <div className="product-mfr">{p.manufacturer_name}</div>
                )}
                <div className="product-prices">
                  <span className="min-price">{formatPrice(p.min_price)}</span>
                  {p.max_price !== p.min_price && (
                    <span className="max-price"> – {formatPrice(p.max_price)}</span>
                  )}
                  <span className="chain-count"> · {p.num_chains} רשתות</span>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="detail-panel">
          {detailLoading && <Spinner />}
          {selected && !detailLoading && <PriceCompare product={selected} />}
          {!selected && !detailLoading && (
            <div className="detail-placeholder">בחר מוצר להשוואת מחירים</div>
          )}
        </div>
      </div>
    </div>
  );
}
