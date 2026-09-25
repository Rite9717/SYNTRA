import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'

export default function Notifications() {
  const [items, setItems] = useState([])

  function load() {
    api.get('/api/notifications').then((res) => setItems(res.data))
  }

  useEffect(() => { load() }, [])

  return (
    <div className="max-w-2xl p-8">
      <div className="flex justify-between mb-4">
        <h1 className="text-3xl">Alerts</h1>
        <button className="text-sm text-[#e0a15a]" onClick={() => api.put('/api/notifications/read-all').then(load)}>Mark all read</button>
      </div>
      {items.map((item) => (
        <Link key={item.id} to={item.link || '/notifications'} onClick={() => api.put(`/api/notifications/${item.id}/read`)}
          className={`block rounded-xl border border-[#2c313c] px-4 py-3 mb-2 ${item.read ? 'opacity-60' : 'bg-[#181b22]'}`}>
          <div className="text-sm text-[#e0a15a]">{item.title}</div>
          <div>{item.body}</div>
        </Link>
      ))}
      {items.length === 0 && <p className="text-[#9a958a]">No alerts yet.</p>}
    </div>
  )
}
