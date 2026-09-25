import { useEffect, useState } from 'react'
import api, { errorText } from '../api'

const empty = { username: '', email: '', password: '', firstName: '', lastName: '', role: 'USER' }

export default function Users() {
  const [users, setUsers] = useState([])
  const [keyword, setKeyword] = useState('')
  const [form, setForm] = useState(empty)
  const [error, setError] = useState('')

  function load(q = keyword) {
    api.get('/api/admin/users', { params: { keyword: q } }).then((res) => setUsers(res.data))
  }

  useEffect(() => { load('') }, [])

  async function create(event) {
    event.preventDefault()
    setError('')
    try {
      await api.post('/api/admin/users', form)
      setForm(empty)
      load('')
    } catch (err) {
      setError(errorText(err, 'Could not create the account'))
    }
  }

  return (
    <div className="p-8 grid grid-cols-[1fr_320px] gap-6">
      <section>
        <h1 className="text-3xl mb-4">People</h1>
        <input value={keyword} onChange={(e) => { setKeyword(e.target.value); load(e.target.value) }} placeholder="Search"
          className="mb-4 w-full rounded-lg bg-[#181b22] border border-[#2c313c] px-3 py-2" />
        <table className="w-full text-sm">
          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="border-b border-[#2c313c]">
                <td className="py-3">{user.username}<div className="text-[#9a958a]">{user.email}</div></td>
                <td>{user.roles?.join(', ')}</td>
                <td>{user.active ? 'Active' : 'Disabled'}</td>
                <td><button className="text-[#e0a15a]" onClick={() => api.put(`/api/admin/users/${user.id}/toggle-status`).then(() => load())}>Toggle</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      <form onSubmit={create} className="rounded-2xl border border-[#2c313c] p-4 h-fit">
        <h2 className="mb-3">Create account</h2>
        {error && <p className="text-sm text-[#d4655d] mb-2">{error}</p>}
        {['username', 'email', 'password', 'firstName', 'lastName'].map((field) => (
          <input key={field} required={field !== 'lastName'} type={field === 'password' ? 'password' : 'text'} placeholder={field}
            value={form[field]} onChange={(e) => setForm({ ...form, [field]: e.target.value })}
            className="w-full mb-2 rounded-lg bg-[#12141a] border border-[#2c313c] px-3 py-2" />
        ))}
        <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}
          className="w-full mb-3 rounded-lg bg-[#12141a] border border-[#2c313c] px-3 py-2">
          <option value="USER">User</option>
          <option value="ADMIN">Admin</option>
        </select>
        <button className="w-full rounded-lg bg-[#e0a15a] text-[#1a140c] py-2">Create</button>
      </form>
    </div>
  )
}
