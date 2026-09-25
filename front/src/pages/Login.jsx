import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { errorText } from '../api'
import { useAuth } from '../auth'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('aisha')
  const [password, setPassword] = useState('User@123')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const { data } = await api.post('/api/auth/login', { username, password })
      login(data)
      navigate(data.roles?.includes('ROLE_ADMIN') ? '/admin' : '/dashboard')
    } catch (err) {
      setError(errorText(err, 'Could not sign in'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen grid place-items-center bg-[#12141a]">
      <form onSubmit={submit} className="w-[420px] rounded-2xl border border-[#2c313c] bg-[#181b22] p-8">
        <div className="wordmark text-4xl text-[#e0a15a]">Syntra</div>
        <p className="text-[#9a958a] mt-2 mb-6">Private mail and rooms for your organization. Accounts are created by an admin.</p>
        {error && <p className="mb-3 text-sm text-[#d4655d]">{error}</p>}
        <label className="block text-sm mb-3">Username
          <input className="mt-1 w-full rounded-lg bg-[#12141a] border border-[#2c313c] px-3 py-2" value={username} onChange={(e) => setUsername(e.target.value)} />
        </label>
        <label className="block text-sm mb-5">Password
          <input type="password" className="mt-1 w-full rounded-lg bg-[#12141a] border border-[#2c313c] px-3 py-2" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        <button disabled={busy} className="w-full rounded-lg bg-[#e0a15a] text-[#1a140c] font-semibold py-2">{busy ? 'Signing in…' : 'Enter'}</button>
        <p className="text-xs text-[#9a958a] mt-4">Demo: aisha / User@123 or admin / Admin@123</p>
      </form>
    </div>
  )
}
