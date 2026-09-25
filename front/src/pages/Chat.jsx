import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api, { errorText } from '../api'
import { useRealtime } from '../realtime'

export default function Chat() {
  const { roomId } = useParams()
  const navigate = useNavigate()
  const realtime = useRealtime()
  const [rooms, setRooms] = useState([])
  const [people, setPeople] = useState([])
  const [messages, setMessages] = useState([])
  const [text, setText] = useState('')
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [typing, setTyping] = useState('')
  const [groupName, setGroupName] = useState('')
  const [error, setError] = useState('')
  const scroller = useRef(null)

  async function refreshRooms() {
    const { data } = await api.get('/api/rooms')
    setRooms(data)
  }

  useEffect(() => {
    refreshRooms().catch((err) => setError(errorText(err, 'Could not load rooms')))
    api.get('/api/users').then((res) => setPeople(res.data)).catch(() => {})
  }, [])

  useEffect(() => {
    if (!roomId) return
    api.get(`/api/rooms/${roomId}/messages`).then((res) => setMessages(res.data)).catch((err) => setError(errorText(err, 'Could not load history')))
    api.post(`/api/rooms/${roomId}/read`).catch(() => {})
    const offMessage = realtime.subscribe(`/topic/rooms/${roomId}`, (event) => {
      setMessages((current) => current.some((item) => item.id === event.id) ? current : [...current, event])
    })
    const offTyping = realtime.subscribe(`/topic/rooms/${roomId}/typing`, (event) => {
      if (!event.typing) return
      setTyping(event.username)
      setTimeout(() => setTyping(''), 1500)
    })
    return () => { offMessage?.(); offTyping?.() }
  }, [roomId, realtime])

  useEffect(() => {
    scroller.current?.scrollTo(0, scroller.current.scrollHeight)
  }, [messages])

  function send(event) {
    event.preventDefault()
    if (!text.trim() || !roomId) return
    realtime.sendChat(Number(roomId), text.trim())
    setText('')
  }

  async function direct(userId) {
    const { data } = await api.post('/api/rooms/direct', { userId })
    await refreshRooms()
    navigate(`/chat/${data.id}`)
  }

  async function createGroup(event) {
    event.preventDefault()
    const { data } = await api.post('/api/rooms', { name: groupName, memberIds: [] })
    setGroupName('')
    await refreshRooms()
    navigate(`/chat/${data.id}`)
  }

  async function ask(event) {
    event.preventDefault()
    const { data } = await api.post('/api/ai/ask', { roomId: Number(roomId), question })
    setAnswer(data.text)
  }

  async function summarize() {
    const { data } = await api.post(`/api/rooms/${roomId}/summarize`)
    setAnswer(data.text)
  }

  const active = rooms.find((room) => String(room.id) === String(roomId))

  return (
    <div className="grid grid-cols-[280px_1fr] h-screen">
      <aside className="border-r border-[#2c313c] p-4 overflow-auto">
        <form onSubmit={createGroup} className="flex gap-2 mb-3">
          <input value={groupName} onChange={(e) => setGroupName(e.target.value)} placeholder="New group" className="flex-1 rounded-lg bg-[#181b22] border border-[#2c313c] px-2 py-1 text-sm" />
          <button className="text-sm text-[#e0a15a]">Add</button>
        </form>
        {rooms.map((room) => (
          <button key={room.id} onClick={() => navigate(`/chat/${room.id}`)} className={`block w-full text-left rounded-lg px-3 py-2 mb-1 ${String(room.id) === String(roomId) ? 'bg-[#2a241c]' : 'hover:bg-[#181b22]'}`}>
            <div className="flex justify-between"><span>{room.name}</span>{room.unread > 0 && <span className="text-[#e0a15a] text-xs">{room.unread}</span>}</div>
            <div className="text-xs text-[#9a958a] truncate">{room.type === 'DIRECT' ? 'Direct' : 'Group'} · {room.lastMessage}</div>
          </button>
        ))}
        <p className="text-xs text-[#9a958a] mt-4 mb-2">Start a direct chat</p>
        {people.map((person) => (
          <button key={person.id} onClick={() => direct(person.id)} className="block w-full text-left text-sm px-3 py-1">
            <span className={person.online ? 'text-[#7d9a78]' : 'text-[#9a958a]'}>●</span> {person.username}
          </button>
        ))}
      </aside>
      <section className="flex flex-col">
        {!roomId && <p className="p-8 text-[#9a958a]">Pick a room or a person.</p>}
        {roomId && (
          <>
            <header className="border-b border-[#2c313c] px-5 py-3 flex justify-between">
              <div>
                <div className="text-lg">{active?.name || 'Room'}</div>
                <div className="text-xs text-[#9a958a]">{active?.members?.map((member) => member.username).join(', ')} {typing && `· ${typing} is typing`}</div>
              </div>
              <button onClick={summarize} className="text-sm text-[#e0a15a]">Summarize</button>
            </header>
            {error && <p className="px-5 py-2 text-[#d4655d] text-sm">{error}</p>}
            <div ref={scroller} className="flex-1 overflow-auto px-5 py-4 space-y-3">
              {messages.map((message) => (
                <div key={message.id || message.timestamp}>
                  <span className="text-xs text-[#e0a15a]">{message.senderUsername}</span>
                  <p>{message.content}</p>
                </div>
              ))}
            </div>
            {answer && <p className="mx-5 mb-2 rounded-xl bg-[#2a241c] p-3 text-sm">{answer}</p>}
            <form onSubmit={ask} className="px-5 flex gap-2 mb-2">
              <input value={question} onChange={(e) => setQuestion(e.target.value)} placeholder="Ask this room" className="flex-1 rounded-lg bg-[#181b22] border border-[#2c313c] px-3 py-2 text-sm" />
              <button className="text-sm text-[#e0a15a]">Ask</button>
            </form>
            <form onSubmit={send} className="px-5 pb-4 flex gap-2">
              <input value={text} onChange={(e) => { setText(e.target.value); realtime.typing(Number(roomId), true) }}
                placeholder="Message" className="flex-1 rounded-lg bg-[#181b22] border border-[#2c313c] px-3 py-2" />
              <button className="rounded-lg bg-[#e0a15a] text-[#1a140c] px-4">Send</button>
            </form>
          </>
        )}
      </section>
    </div>
  )
}
