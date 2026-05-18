const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
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
