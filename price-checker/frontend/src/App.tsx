import { useState } from "react";
import { useGet } from "./hooks/useApi";
import type { Chain } from "./types";
import ProductsPage from "./pages/ProductsPage";
import SyncPage from "./pages/SyncPage";
import "./index.css";

type Tab = "products" | "sync";

export default function App() {
  const [tab, setTab] = useState<Tab>("products");
  const { data: chains } = useGet<Chain[]>("/chains");

  return (
    <div className="app">
      <header className="app-header">
        <h1 className="app-title">🛒 בודק מחירי GZ</h1>
        <nav className="app-nav">
          <button
            className={`nav-btn ${tab === "products" ? "active" : ""}`}
            onClick={() => setTab("products")}
          >
            מוצרים ומחירים
          </button>
          <button
            className={`nav-btn ${tab === "sync" ? "active" : ""}`}
            onClick={() => setTab("sync")}
          >
            סטטוס סנכרון
          </button>
        </nav>
      </header>

      <main>
        {tab === "products" && <ProductsPage chains={chains ?? []} />}
        {tab === "sync" && <SyncPage />}
      </main>
    </div>
  );
}
