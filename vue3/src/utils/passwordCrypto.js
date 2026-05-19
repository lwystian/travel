import { getPasswordPublicKey } from '@/api/auth'

let cachedKey = null

const base64ToArrayBuffer = (base64) => {
  const binary = window.atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes.buffer
}

const arrayBufferToBase64 = (buffer) => {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  bytes.forEach(byte => {
    binary += String.fromCharCode(byte)
  })
  return window.btoa(binary)
}

const getCryptoKey = async () => {
  const data = await getPasswordPublicKey()
  if (data?.enabled === false) {
    return null
  }
  if (!window.crypto?.subtle) {
    throw new Error('当前浏览器不支持安全加密，请更换现代浏览器或使用 HTTPS 访问')
  }
  if (cachedKey) {
    return cachedKey
  }
  cachedKey = await window.crypto.subtle.importKey(
    'spki',
    base64ToArrayBuffer(data.publicKey),
    {
      name: 'RSA-OAEP',
      hash: 'SHA-256'
    },
    false,
    ['encrypt']
  )
  return cachedKey
}

export async function encryptPassword(password) {
  const key = await getCryptoKey()
  if (!key) {
    return password
  }
  const encrypted = await window.crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    key,
    new TextEncoder().encode(password)
  )
  return arrayBufferToBase64(encrypted)
}
