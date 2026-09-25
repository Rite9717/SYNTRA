import { useEffect, useState } from 'react'
import api from '../api'

export default function Admin() {
  const [stats, setStats] = useState(null)
  const [incidents, setIncidents] = useState([])
  const [metrics, setMetrics] = useState([])

  function load() {
    api.get('/api/admin/dashboard/stats').then((res) => setStats(res.data))
    api.get('/api/admin/incidents').then((res) => setIncidents(res.data))
    api.get('/api/admin/metrics/recent').then((res) => setMetrics(res.data))
  }

  useEffect(() => { load() }, [])

  const cards = stats ? [
    ['People', stats.totalUsers],
    ['Active', stats.activeUsers],
    ['Mail', stats.totalMail],
    ['Mail today', stats.mailToday],
    ['Unread', stats.unreadMail],
    ['Chat', stats.chatMessages],
    ['Open incidents', stats.openIncidents],
    ['Online', stats.onlineUsers]
  ] : []

  return (
    <div className="p-8">
      <div className="flex justify-between items-end mb-6">
        <div>
          <h1 className="text-3xl">Operations</h1>
          <p className="text-[#9a958a]">Health, delivery rate, and incidents.</p>
        </div>
        {stats?.grafanaUrl && <a className="text-[#e0a15a]" href={stats.grafanaUrl} target="_blank" rel="noreferrer">Open Grafana</a>}
      </div>
      <div className="grid grid-cols-4 gap-3 mb-6">
        {cards.map(([label, value]) => (
          <div key={label} className="rounded-2xl border border-[#2c313c] bg-[#181b22] p-4">
            <div className="text-xs text-[#9a958a]">{label}</div>
            <div className="text-3xl mt-1">{value}</div>
          </div>
        ))}
      </div>
      <div className="grid grid-cols-2 gap-4">
        <section className="rounded-2xl border border-[#2c313c] p-4">
          <h2 className="mb-3">Platform health</h2>
          {stats?.health?.map((item) => (
            <div key={item.name} className="flex justify-between py-2 border-b border-[#2c313c] text-sm">
              <span>{item.name}</span>
              <span className={item.status === 'UP' ? 'text-[#7d9a78]' : 'text-[#d4655d]'}>{item.status}</span>
            </div>
          ))}
        </section>
        <section className="rounded-2xl border border-[#2c313c] p-4">
          <h2 className="mb-3">Incidents</h2>
          {incidents.length === 0 && <p className="text-sm text-[#9a958a]">No incidents. A drop in message rate or a burst of failed logins will open one.</p>}
          {incidents.map((incident) => (
            <div key={incident.id} className="mb-3 text-sm">
              <div className="flex justify-between"><b>{incident.title}</b><span>{incident.status}</span></div>
              <p className="text-[#9a958a]">{incident.detail}</p>
              {incident.status !== 'RESOLVED' && (
                <button className="text-[#e0a15a]" onClick={() => api.put(`/api/admin/incidents/${incident.id}/resolve`).then(load)}>Resolve</button>
              )}
            </div>
          ))}
        </section>
      </div>
      <section className="mt-4 rounded-2xl border border-[#2c313c] p-4">
        <h2 className="mb-3">Recent snapshots</h2>
        <div className="flex gap-2 items-end h-24">
          {metrics.filter((item) => item.name === 'message_rate').slice(0, 24).reverse().map((item, index) => (
            <div key={index} title={`${item.value}`} className="bg-[#e0a15a] w-3" style={{ height: `${Math.max(8, item.value * 12)}px` }} />
          ))}
        </div>
      </section>
    </div>
  )
}
