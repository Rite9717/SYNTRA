import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api'
import { useAuth } from '../auth'

export default function Dashboard() {
  const { user } = useAuth()
  const [unread, setUnread] = useState(0)
  const [rooms, setRooms] = useState([])
  const [online, setOnline] = useState([])

  useEffect(() => {
    api.get('/api/messages/unread-count').then((res) => setUnread(res.data.count)).catch(() => {})
    api.get('/api/rooms').then((res) => setRooms(res.data.slice(0, 4))).catch(() => {})
    api.get('/api/presence').then((res) => setOnline(res.data.online || [])).catch(() => {})
  }, [])

  return (
    <div className="p-8">
      <h1 className="text-3xl">Good to see you, {user?.firstName || user?.username}</h1>
      <p className="text-[#9a958a] mt-1 mb-6">Mail stays private. Chat is direct or in a named room.</p>
      <div className="grid grid-cols-3 gap-4 mb-8">
        <Link to="/inbox" className="rounded-2xl border border-[#2c313c] bg-[#181b22] p-5">
          <div className="text-[#9a958a] text-sm">Unread mail</div>
          <div className="text-4xl text-[#e0a15a] mt-2">{unread}</div>
        </Link>
        <Link to="/chat" className="rounded-2xl border border-[#2c313c] bg-[#181b22] p-5">
          <div className="text-[#9a958a] text-sm">Rooms</div>
          <div className="text-4xl mt-2">{rooms.length}</div>
        </Link>
        <div className="rounded-2xl border border-[#2c313c] bg-[#181b22] p-5">
          <div className="text-[#9a958a] text-sm">Online now</div>
          <div className="mt-3 text-sm text-[#7d9a78]">{online.length ? online.join(', ') : 'Just you, once the socket connects'}</div>
        </div>
      </div>
      <div className="rounded-2xl border border-[#2c313c] bg-[#181b22]">
        {rooms.map((room) => (
          <Link key={room.id} to={`/chat/${room.id}`} className="flex justify-between px-5 py-4 border-b border-[#2c313c] last:border-0">
            <span>{room.name}</span>
            <span className="text-[#9a958a] text-sm">{room.unread ? `${room.unread} new` : room.lastMessage}</span>
          </Link>
        ))}
        {rooms.length === 0 && <p className="p-5 text-[#9a958a]">No rooms yet. Open Chat to start one.</p>}
      </div>
    </div>
  )
}
