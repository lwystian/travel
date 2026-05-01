const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8081,
    proxy: {
      '^/api': {
        target: 'http://localhost:1236',
        changeOrigin: true
      },
      '^/img': {
        target: 'http://localhost:1236',
        changeOrigin: true
      }
    },
    client: {
      overlay: false
    }
  }
})