import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { errorText } from '../api'

export default function Compose() {
  const navigate = useNavigate()
  const [users, setUsers] = useState([])
  const [form, setForm] = useState({ receiverId: '', subject: '', body: '' })
  const [file, setFile] = useState(null)
  const [instruction, setInstruction] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.get('/api/users').then((res) => setUsers(res.data)).catch(() => {})
  }, [])

  function set(name, value) {
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function draft() {
    const { data } = await api.post('/api/ai/draft', { subject: form.subject, instruction, context: '' })
    set('body', data.text)
  }

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const { data } = await api.post('/api/messages', { ...form, receiverId: Number(form.receiverId) })
      if (file) {
        const body = new FormData()
        body.append('file', file)
        await api.post(`/api/messages/${data.id}/attachments`, body)
      }
      navigate(`/inbox/${data.id}`)
    } catch (err) {
      setError(errorText(err, 'Could not send'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="max-w-2xl p-8">
      <h1 className="text-3xl mb-6">New message</h1>
      {error && <p className="mb-3 text-[#d4655d]">{error}</p>}
      <select required value={form.receiverId} onChange={(e) => set('receiverId', e.target.value)}
        className="w-full mb-3 rounded-lg bg-[#181b22] border border-[#2c313c] px-3 py-2">
        <option value="">Recipient</option>
        {users.map((user) => <option key={user.id} value={user.id}>{user.username}</option>)}
      </select>
      <input required placeholder="Subject" value={form.subject} onChange={(e) => set('subject', e.target.value)}
        className="w-full mb-3 rounded-lg bg-[#181b22] border border-[#2c313c] px-3 py-2" />
      <textarea required rows={8} placeholder="Write the message" value={form.body} onChange={(e) => set('body', e.target.value)}
        className="w-full mb-3 rounded-lg bg-[#181b22] border border-[#2c313c] px-3 py-2" />
      <div className="flex gap-2 mb-4">
        <input value={instruction} onChange={(e) => setInstruction(e.target.value)} placeholder="Ask the agent for a draft"
          className="flex-1 rounded-lg bg-[#181b22] border border-[#2c313c] px-3 py-2" />
        <button type="button" onClick={draft} className="rounded-lg border border-[#e0a15a] text-[#e0a15a] px-3">Draft</button>
      </div>
      <input type="file" onChange={(e) => setFile(e.target.files?.[0] || null)} className="mb-4 block text-sm" />
      <button disabled={busy} className="rounded-lg bg-[#e0a15a] text-[#1a140c] px-4 py-2">{busy ? 'Sending…' : 'Send'}</button>
    </form>
  )
}
