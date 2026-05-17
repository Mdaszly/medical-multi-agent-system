export const format = {
  date(date: Date | string | number, formatStr: string = 'YYYY-MM-DD HH:mm:ss'): string {
    const d = new Date(date)
    
    if (isNaN(d.getTime())) {
      return ''
    }
    
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    const seconds = String(d.getSeconds()).padStart(2, '0')
    
    return formatStr
      .replace('YYYY', String(year))
      .replace('MM', month)
      .replace('DD', day)
      .replace('HH', hours)
      .replace('mm', minutes)
      .replace('ss', seconds)
  },

  money(amount: number | string, symbol: string = '¥'): string {
    const num = Number(amount)
    if (isNaN(num)) {
      return `${symbol}0.00`
    }
    return `${symbol}${num.toFixed(2)}`
  },

  appointmentStatus(status: number): string {
    const statusMap: Record<number, string> = {
      0: '待就诊',
      1: '已完成',
      2: '已取消',
      3: '已过期'
    }
    return statusMap[status] || '未知'
  }
}
