### To set a CSP header set the CSP_HEADER_VALUE env variable. For example:

CSP_HEADER_VALUE=connect-src 'self' localhost; font-src 'self' localhost; frame-src 'self' localhost; script-src 'self' localhost 'unsafe-inline'; style-src 'self' localhost 'unsafe-inline'; frame-ancestors 'self' localhost; img-src 'self' data: localhost; manifest-src 'self' localhost; media-src 'self' localhost; object-src 'self' localhost; worker-src 'self';

## mở nhất để sử dụng trong môi trường phát triển, giúp giảm thiểu các hạn chế trong quá trình phát triển ứng dụng:
CSP_HEADER_VALUE=default-src * 'unsafe-inline' 'unsafe-eval' data: blob:; connect-src * 'unsafe-inline' 'unsafe-eval' data: blob:; font-src * 'unsafe-inline' 'unsafe-eval' data: blob:; frame-src * 'unsafe-inline' 'unsafe-eval' data: blob:; img-src * 'unsafe-inline' 'unsafe-eval' data: blob:; media-src * 'unsafe-inline' 'unsafe-eval' data: blob:; object-src * 'unsafe-inline' 'unsafe-eval' data: blob:; script-src * 'unsafe-inline' 'unsafe-eval' data: blob:; style-src * 'unsafe-inline' 'unsafe-eval' data: blob:; worker-src * 'unsafe-inline' 'unsafe-eval' data: blob:; frame-ancestors * 'unsafe-inline' 'unsafe-eval' data: blob:;
