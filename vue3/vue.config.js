const { defineConfig } = require('@vue/cli-service')
const proxyTarget = process.env.VUE_APP_PROXY_TARGET || 'http://localhost:3001'
module.exports = defineConfig({
  transpileDependencies: true,
  productionSourceMap: false,
  chainWebpack: config => {
    if (process.env.NODE_ENV === 'production') {
      config.optimization.minimizer('terser').tap(args => {
        args[0].terserOptions.compress = {
          ...args[0].terserOptions.compress,
          drop_console: true,
          drop_debugger: true
        }
        return args
      })
    }
  },
  devServer: {
    port: 8081,
    proxy: {
      '^/api': {
        target: proxyTarget,
        changeOrigin: true
      },
      '^/img': {
        target: proxyTarget,
        changeOrigin: true
      }
    },
    client: {
      overlay: false
    }
  }
})
