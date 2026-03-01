config.resolve = config.resolve || {};
config.resolve.fallback = {
  ...(config.resolve.fallback || {}),
  path: false,
  fs: false,
  crypto: false,
};
