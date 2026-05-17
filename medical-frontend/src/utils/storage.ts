export const storage = {
  set(key: string, value: any): void {
    try {
      const serializedValue = JSON.stringify(value)
      localStorage.setItem(key, serializedValue)
    } catch (error) {
      console.error('存储失败:', error)
    }
  },

  get(key: string): any {
    try {
      const serializedValue = localStorage.getItem(key)
      if (serializedValue === null) {
        return null
      }
      return JSON.parse(serializedValue)
    } catch (error) {
      console.error('读取失败:', error)
      return null
    }
  },

  remove(key: string): void {
    try {
      localStorage.removeItem(key)
    } catch (error) {
      console.error('删除失败:', error)
    }
  },

  clear(): void {
    try {
      localStorage.clear()
    } catch (error) {
      console.error('清空失败:', error)
    }
  }
}
