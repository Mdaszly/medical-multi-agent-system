impor;
t http from 'http';

console.log('正在检查后端服务是否启动...');
console.log('尝试连接: http://localhost:8080/v3/api-docs\n');

http.get('http://localhost:8080/v3/api-docs', (res) => {
  console.log(`✓ 后端服务已启动！状态码: ${res.statusCode}`);
  console.log(`✓ Swagger UI 地址: http://localhost:8080/swagger-ui.html`);
  console.log(`✓ OpenAPI 规范地址: http://localhost:8080/v3/api-docs`);
  console.log('\n现在可以运行: npm run openapi2ts');
}).on('error', (err) => {
  console.error('✗ 后端服务未启动或连接失败');
  console.error('错误信息:', err.message);
  console.log('\n请先启动后端服务（Java 项目），然后再运行此命令。');
});
