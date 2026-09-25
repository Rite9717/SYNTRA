import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api, { errorText } from '../api'

const boxes = [
  ['inbox', 'Inbox'],
  ['sent', 'Sent'],
  ['starred', 'Starred'],
  ['archived', 'Archive']
]

export default function Inbox() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [box, setBox] = useState('inbox')
  const [folders, setFolders] = useState([])
  const [folderId, setFolderId] = useState(null)
  const [query, setQuery] = useState('')
  const [messages, setMessages] = useState([])
  const [selected, setSelected] = useState(null)
  const [thread, setThread] = useState([])
  const [reply, setReply] = useState('')
  const [summary, setSummary] = useState('')
  const [error, setError] = useState('')

  async function loadList(nextBox = box, nextFolder = folderId, q = query) {
    const path = q.trim().length >= 2
      ? `/api/messages/search?q=${encodeURIComponent(q.trim())}`
      : nextFolder
        ? `/api/messages/folder/${nextFolder}`
        : `/api/messages/${nextBox}`
    const { data } = await api.get(path)
    setMessages(data)
  }

  useEffect(() => {
    api.get('/api/folders').then((res) => setFolders(res.data)).catch(() => {})
  }, [])

  useEffect(() => {
    loadList().catch((err) => setError(errorText(err, 'Could not load mail')))
  }, [box, folderId])

  useEffect(() => {
    if (!id) return
    api.get(`/api/messages/${id}`).then((res) => {
      setSelected(res.data)
      return api.get(`/api/messages/${id}/thread`)
    }).then((res) => setThread(res.data)).catch((err) => setError(errorText(err, 'Could not open message')))
  }, [id])

  async function open(message) {
    navigate(`/inbox/${message.id}`)
  }

  async function sendReply(event) {
    event.preventDefault()
    const { data } = await api.post(`/api/messages/${selected.id}/reply`, { body: reply })
    setReply('')
    navigate(`/inbox/${data.id}`)
    loadList()
  }

  async function summarize() {
    const { data } = await api.post('/api/ai/summarize', { kind: 'mail', id: selected.id })
    setSummary(data.text)
  }

  async function download(file) {
    const res = await api.get(`/api/attachments/${file.id}`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const link = document.createElement('a')
    link.href = url
    link.download = file.originalName
    link.click()
  }

  async function addFolder() {
    const name = window.prompt('Folder name')
    if (!name) return
    const { data } = await api.post('/api/folders', { name })
    setFolders((current) => [...current, data])
  }

  return (
    <div className="grid grid-cols-[200px_320px_1fr] h-screen">
      <aside className="border-r border-[#2c313c] p-4">
        {boxes.map(([key, label]) => (
          <button key={key} className={`block w-full text-left rounded-lg px-3 py-2 text-sm ${box === key && !folderId ? 'bg-[#2a241c] text-[#e0a15a]' : ''}`}
            onClick={() => { setBox(key); setFolderId(null); setQuery('') }}>{label}</button>
        ))}
        <div className="mt-4 text-xs text-[#9a958a] flex justify-between">Folders <button onClick={addFolder}>+</button></div>
        {folders.map((folder) => (
          <button key={folder.id} className="block w-full text-left rounded-lg px-3 py-2 text-sm"
            onClick={() => { setFolderId(folder.id); setQuery('') }}>{folder.name}</button>
        ))}
      </aside>
      <section className="border-r border-[#2c313c]">
        <input value={query} onChange={(e) => setQuery(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && loadList()}
          placeholder="Search mail" className="m-3 w-[calc(100%-1.5rem)] rounded-lg bg-[#181b22] border border-[#2c313c] px-3 py-2 text-sm" />
        {error && <p className="px-3 text-sm text-[#d4655d]">{error}</p>}
        {messages.map((message) => (
          <button key={message.id} onClick={() => open(message)} className="block w-full text-left px-4 py-3 border-b border-[#2c313c] hover:bg-[#181b22]">
            <div className="flex justify-between text-sm"><span>{message.senderUsername}</span><span className="text-[#e0a15a] text-xs">{message.priority}</span></div>
            <div className={message.read ? 'text-[#9a958a]' : ''}>{message.subject}</div>
          </button>
        ))}
      </section>
      <section className="p-6 overflow-auto">
        {!selected && <p className="text-[#9a958a]">Select a message.</p>}
        {selected && (
          <>
            <div className="flex gap-2 mb-4">
              <button className="text-sm border border-[#2c313c] rounded-lg px-3 py-1" onClick={() => api.put(`/api/messages/${selected.id}/star`).then(() => loadList())}>Star</button>
              <button className="text-sm border border-[#2c313c] rounded-lg px-3 py-1" onClick={() => api.put(`/api/messages/${selected.id}/archive`).then(() => loadList())}>Archive</button>
              <button className="text-sm border border-[#2c313c] rounded-lg px-3 py-1" onClick={summarize}>Summarize</button>
              <button className="text-sm border border-[#2c313c] rounded-lg px-3 py-1" onClick={() => api.delete(`/api/messages/${selected.id}`).then(() => { setSelected(null); loadList() })}>Delete</button>
            </div>
            <h1 className="text-2xl">{selected.subject}</h1>
            <p className="text-sm text-[#9a958a] mb-4">{selected.senderName} to {selected.receiverName}</p>
            {summary && <p className="mb-4 rounded-xl bg-[#2a241c] p-4 text-sm">{summary}</p>}
            {thread.map((item) => (
              <article key={item.id} className="mb-4 border-b border-[#2c313c] pb-4">
                <p className="whitespace-pre-wrap">{item.body}</p>
                {item.attachments?.map((file) => (
                  <button key={file.id} className="mt-2 text-sm text-[#e0a15a]" onClick={() => download(file)}>{file.originalName}</button>
                ))}
              </article>
            ))}
            <form onSubmit={sendReply} className="mt-4">
              <textarea value={reply} onChange={(e) => setReply(e.target.value)} required rows={4}
                className="w-full rounded-xl bg-[#181b22] border border-[#2c313c] p-3" placeholder="Reply" />
              <button className="mt-2 rounded-lg bg-[#e0a15a] text-[#1a140c] px-4 py-2">Send reply</button>
            </form>
          </>
        )}
      </section>
    </div>
  )
}
