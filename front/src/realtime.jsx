import { createContext, useContext, useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuth } from './auth'

const RealtimeContext = createContext(null)

export function RealtimeProvider({ children }) {
  const { user } = useAuth()
  const clientRef = useRef(null)
  const ready = useRef(false)
  const waiters = useRef([])

  useEffect(() => {
    const token = sessionStorage.getItem('syntra_token')
    if (!user || !token) return undefined
    const client = new Client({
      webSocketFactory: () => new SockJS(`/ws?token=${encodeURIComponent(token)}`),
      reconnectDelay: 5000,
      onConnect: () => {
        ready.current = true
        client.publish({ destination: '/app/presence.heartbeat', body: '{}' })
        waiters.current.splice(0).forEach((fn) => fn(client))
      },
      onDisconnect: () => { ready.current = false }
    })
    clientRef.current = client
    client.activate()
    const beat = setInterval(() => {
      if (client.connected) client.publish({ destination: '/app/presence.heartbeat', body: '{}' })
    }, 20000)
    return () => {
      clearInterval(beat)
      ready.current = false
      client.deactivate()
    }
  }, [user])

  function withClient(fn) {
    const client = clientRef.current
    if (client?.connected) fn(client)
    else waiters.current.push(fn)
  }

  const api = {
    subscribe(destination, handler) {
      let sub = null
      let dead = false
      withClient((client) => {
        if (dead) return
        sub = client.subscribe(destination, (frame) => handler(JSON.parse(frame.body)))
      })
      return () => {
        dead = true
        sub?.unsubscribe()
      }
    },
    sendChat(roomId, content) {
      withClient((client) => client.publish({
        destination: '/app/chat.send',
        body: JSON.stringify({ roomId, content })
      }))
    },
    typing(roomId, typing) {
      withClient((client) => client.publish({
        destination: '/app/chat.typing',
        body: JSON.stringify({ roomId, typing })
      }))
    }
  }

  return <RealtimeContext.Provider value={api}>{children}</RealtimeContext.Provider>
}

export function useRealtime() {
  return useContext(RealtimeContext)
}
