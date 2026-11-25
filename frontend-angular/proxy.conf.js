// Die REST-API läuft im Spring-Boot-Backend. Der Dev-Server leitet /api dorthin weiter,
// damit kein CORS im Backend nötig ist. Ziel per API_TARGET überschreibbar.
module.exports = {
  '/api': {
    target: process.env.API_TARGET || 'http://localhost:8080',
    changeOrigin: true,
  },
};
