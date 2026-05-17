interface CacheItem<T = any> {
  data: T
  expireTime: number
}

export const cache = {
  set<T>(key: string, data: T, expireSeconds: number = 300): void {
    const cacheItem: CacheItem<T> = {
      data,
      expireTime: Date.now() + expireSeconds * 1000
    }
    try {
      localStorage.setItem(`cache_${key}`, JSON.stringify(cacheItem))
    } catch (error) {
      console.error('缓存失败:', error)
    }
  },

  get<T>(key: string): T | null {
    try {
      const cacheStr = localStorage.getItem(`cache_${key}`)
      if (!cacheStr) {
        return null
      }
      
      const cacheItem: CacheItem<T> = JSON.parse(cacheStr)
      
      if (Date.now() > cacheItem.expireTime) {
        this.remove(key)
        return null
      }
      
      return cacheItem.data
    } catch (error) {
      console.error('读取缓存失败:', error)
      return null
    }
  },

  remove(key: string): void {
    try {
      localStorage.removeItem(`cache_${key}`)
    } catch (error) {
      console.error('删除缓存失败:', error)
    }
  },

  clearAll(): void {
    try {
      const keys = Object.keys(localStorage)
      keys.forEach(key => {
        if (key.startsWith('cache_')) {
          localStorage.removeItem(key)
        }
      })
    } catch (error) {
      console.error('清空缓存失败:', error)
    }
  }
}
