const isProduction = process.env.NODE_ENV === 'production'
const noop = () => {}
const nativeConsole = {
  debug: console.debug.bind(console),
  info: console.info.bind(console),
  warn: console.warn.bind(console),
  error: console.error.bind(console),
  log: console.log.bind(console)
}

const normalize = (args) => args.map(item => {
  if (item instanceof Error) {
    return {
      name: item.name,
      message: item.message,
      stack: item.stack
    }
  }
  return item
})

const write = (level, args) => {
  if (isProduction && level !== 'error') {
    return
  }

  const writer = nativeConsole[level] || nativeConsole.log
  writer(`[${level}]`, ...normalize(args))
}

const logger = {
  debug: isProduction ? noop : (...args) => write('debug', args),
  info: isProduction ? noop : (...args) => write('info', args),
  warn: (...args) => write('warn', args),
  error: (...args) => write('error', args)
}

export default logger
