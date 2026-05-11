import { useState } from "react";
import type { SyncStatus } from "../types";
import { useGet, triggerSync } from "../hooks/useApi";
import Spinner from "../components/Spinner";

function fmtDate(raw: string | null) {
  if (!raw) return "—";
  return new Date(raw).toLocaleString("he-IL");
}

export default function SyncPage() {
  const { data, loading, error, reload } = useGet<SyncStatus>("/sync/status");
  const [triggering, setTriggering] = useState(false);

  const handleTrigger = async () => {
    setTriggering(true);
    await triggerSync();
    setTimeout(() => {
      reload();
      setTriggering(false);
    }, 2000);
  };

  if (loading) return <Spinner />;
  if (error) return <p className="error">שגיאה: {error}</p>;
  if (!data) return null;

  return (
    <div className="page">
      <div className="sync-header">
        <h2>סטטוס סנכרון</h2>
        <button
          className="btn-primary"
          onClick={handleTrigger}
          disabled={triggering}
        >
          {triggering ? "מסנכרן..." : "סנכרן עכשיו"}
        </button>
      </div>

      <h3>רשתות</h3>
      <table className="sync-table">
        <thead>
          <tr>
            <th>רשת</th>
            <th>Chain ID</th>
            <th>סנכרון אחרון</th>
          </tr>
        </thead>
        <tbody>
          {data.chains.map((c) => (
            <tr key={c.id}>
              <td>{c.display_name}</td>
              <td className="mono">{c.chain_id}</td>
              <td>{fmtDate(c.last_synced)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3>לוג סנכרונים אחרונים</h3>
      <table className="sync-table">
        <thead>
          <tr>
            <th>רשת ID</th>
            <th>קובץ</th>
            <th>סוג</th>
            <th>רשומות</th>
            <th>תאריך</th>
            <th>סטטוס</th>
          </tr>
        </thead>
        <tbody>
          {data.recent_logs.map((log) => (
            <tr key={log.id} className={log.status === "error" ? "row-error" : ""}>
              <td>{log.chain_id}</td>
              <td className="mono file-name" title={log.file_name}>
                {log.file_name.slice(0, 40)}
              </td>
              <td>{log.file_type ?? "—"}</td>
              <td>{log.records_count}</td>
              <td>{fmtDate(log.downloaded_at)}</td>
              <td>
                <span className={`badge badge-${log.status}`}>{log.status}</span>
                {log.error_message && (
                  <span className="error-msg" title={log.error_message}> ⚠</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
