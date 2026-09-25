import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import api from '../api'
import { useAuth } from '../auth'
import { useRealtime } from '../realtime'

const links = [
  ['/dashboard', 'Home'],
  ['/inbox', 'Inbox'],
  ['/compose', 'Compose'],
  ['/chat', 'Chat'],
  ['/notifications', 'Alerts']
]

export default function Shell() {
  const { user, logout, isAdmin } = useAuth()
  const realtime = useRealtime()
  const navigate = useNavigate()
  const [mailUnread, setMailUnread] = useState(0)
  const [alerts, setAlerts] = useState(0)

  useEffect(() => {
    let stop = false
    async function load() {
      try {
        const [mail, notes] = await Promise.all([
          api.get('/api/messages/unread-count'),
          api.get('/api/notifications/unread-count')
        ])
        if (!stop) {
          setMailUnread(mail.data.count)
          setAlerts(notes.data.count)
        }
      } catch { /* shell badges are optional */ }
    }
    load()
    const timer = setInterval(load, 15000)
    const off = realtime?.subscribe('/user/queue/notifications', () => {
      setAlerts((count) => count + 1)
    })
    return () => { stop = true; clearInterval(timer); off?.() }
  }, [realtime])

  return (
    <div className="min-h-screen grid grid-cols-[240px_1fr]">
      <aside className="border-r border-[#2c313c] bg-[#181b22] p-5 flex flex-col">
        <div className="wordmark text-3xl text-[#e0a15a]">Syntra</div>
        <p className="text-xs text-[#9a958a] mt-1 mb-8">Internal desk</p>
        <nav className="flex flex-col gap-1">
          {links.map(([to, label]) => (
            <NavLink key={to} to={to} className={({ isActive }) =>
              `rounded-lg px-3 py-2 text-sm ${isActive ? 'bg-[#2a241c] text-[#e0a15a]' : 'text-[#d7d2c8] hover:bg-[#232833]'}`}>
              {label}
              {to === '/inbox' && mailUnread > 0 && <span className="float-right text-[#e0a15a]">{mailUnread}</span>}
              {to === '/notifications' && alerts > 0 && <span className="float-right text-[#e0a15a]">{alerts}</span>}
            </NavLink>
          ))}
          {isAdmin && (
            <>
              <NavLink to="/admin" className="rounded-lg px-3 py-2 text-sm text-[#d7d2c8] hover:bg-[#232833]">Operations</NavLink>
              <NavLink to="/admin/users" className="rounded-lg px-3 py-2 text-sm text-[#d7d2c8] hover:bg-[#232833]">People</NavLink>
            </>
          )}
        </nav>
        <div className="mt-auto text-sm">
          <div className="text-[#eceae4]">{user?.firstName || user?.username}</div>
          <button className="text-[#9a958a] mt-2" onClick={() => { logout(); navigate('/login') }}>Sign out</button>
        </div>
      </aside>
      <main className="min-h-screen bg-[#12141a]">
        <Outlet />
      </main>
    </div>
  )
}
