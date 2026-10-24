export function downloadResponse(response, fallbackName = '下载文件') {
  const blob = new Blob([response.data])
  const disposition = response.headers?.['content-disposition'] || ''
  let filename = fallbackName
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  const normalMatch = disposition.match(/filename="?([^";]+)"?/i)
  if (utf8Match) {
    filename = decodeURIComponent(utf8Match[1])
  } else if (normalMatch) {
    filename = decodeURIComponent(normalMatch[1])
  }
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export function openBlobInNewTab(response) {
  const blob = new Blob([response.data], { type: response.headers['content-type'] })
  const url = window.URL.createObjectURL(blob)
  return url
}
